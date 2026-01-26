"""
LLM-powered intent parser for natural language dispatch requests.
Converts user text into structured planning scenarios.
"""

import os
import json
import requests
from typing import Optional, Dict, Any
from models import PlanningScenario, MedDispatchRec, Requirements, Position
from datetime import datetime, date, time, timedelta
import re

try:
    import dateparser
    HAS_DATEPARSER = True
except Exception:
    HAS_DATEPARSER = False


def _resolve_relative_date(text: str, ref: datetime) -> str | None:
    """Resolve simple relative date phrases to an ISO date string.

    Supported forms:
      - today, tomorrow
      - this <weekday>, next <weekday> (weekday names)
      - in N days

    Returns ISO date string (YYYY-MM-DD) or None if not recognized.
    Uses `ref` (a datetime) as the reference point (UTC by default in parser).
    """
    if not text or not isinstance(text, str):
        return None
    s = text.strip().lower()

    if s in ("today", "tonight"):
        return ref.date().isoformat()
    if s == "tomorrow":
        return (ref.date() + timedelta(days=1)).isoformat()

    m = re.match(r"in\s+(\d+)\s+days?", s)
    if m:
        days = int(m.group(1))
        return (ref.date() + timedelta(days=days)).isoformat()

    # Weekday handling
    weekdays = {
        'monday': 0, 'tuesday': 1, 'wednesday': 2, 'thursday': 3,
        'friday': 4, 'saturday': 5, 'sunday': 6
    }

    parts = s.split()
    if len(parts) == 1 and parts[0] in weekdays:
        # e.g. 'saturday' -> nearest upcoming weekday (including today)
        target = weekdays[parts[0]]
        today_idx = ref.weekday()
        days_ahead = (target - today_idx) % 7
        return (ref.date() + timedelta(days=days_ahead)).isoformat()

    if len(parts) == 2 and parts[1] in weekdays and parts[0] in ("this", "next"):
        target = weekdays[parts[1]]
        today_idx = ref.weekday()
        days_ahead = (target - today_idx) % 7
        if parts[0] == 'next':
            if days_ahead == 0:
                days_ahead = 7
        # 'this' returns same-week upcoming day (if today is past that day, it will wrap to next occurrence)
        return (ref.date() + timedelta(days=days_ahead)).isoformat()

    return None


def _resolve_holiday(text: str, ref: datetime) -> str | None:
    """Resolve common holiday names to ISO date strings.

    Recognises phrases such as 'christmas', 'christmas day', 'new year's day', 'new years day', 'boxing day'.
    Returns YYYY-MM-DD for the same year unless the date has already passed relative to `ref`,
    in which case returns the next year's date.
    """
    if not text or not isinstance(text, str):
        return None
    s = text.strip().lower()

    # Map keywords to (month, day)
    holidays = {
        'christmas': (12, 25),
        'christmas day': (12, 25),
        'xmas': (12, 25),
        'christmas eve': (12, 24),
        "new year's day": (1, 1),
        'new years day': (1, 1),
        'new year': (1, 1),
        "new year's eve": (12, 31),
        'new years eve': (12, 31),
        'boxing day': (12, 26)
    }

    for key, (m, d) in holidays.items():
        if key in s:
            year = ref.year
            try:
                candidate = date(year, m, d)
            except Exception:
                return None
            if candidate < ref.date():
                # holiday already passed this year -> next year
                try:
                    candidate = date(year + 1, m, d)
                except Exception:
                    return None
            return candidate.isoformat()
    return None


def _user_specified_maxcost(user_text: str) -> bool:
    """Return True if the user's text explicitly mentions a max cost/budget with a numeric value."""
    if not user_text or not isinstance(user_text, str):
        return False
    s = user_text.lower()
    # quick checks: phrases and presence of digits
    if any(k in s for k in ("max cost", "maxcost", "max-cost", "budget", "max budget", "no more than", "at most", "no more than")):
        # ensure there's a numeric token somewhere nearby
        if re.search(r"\d+", s):
            return True
        return False
    # Also detect patterns like 'max 30', '<= 30', 'under 30', 'less than 30'
    if re.search(r"\b(max|<=|under|less than|no more than)\b.*\d+", s):
        return True
    return False


def _user_has_relative_date_phrase(user_text: str) -> bool:
    """Detect whether the user's original text contains relative date phrases or weekday names.

    Returns True if phrases like 'next', 'this', 'tomorrow', 'in N days', or weekday names appear.
    """
    if not user_text or not isinstance(user_text, str):
        return False
    s = user_text.lower()
    # common relative tokens
    rel_tokens = [
        'today', 'tonight', 'tomorrow', 'this ', 'next ', 'in ', 'after', 'before',
        'monday', 'tuesday', 'wednesday', 'thursday', 'friday', 'saturday', 'sunday'
    ]
    for t in rel_tokens:
        if t in s:
            # ignore the generic 'in ' presence unless followed by a digit
            if t == 'in ':
                if re.search(r'in\s+\d+\s+days?', s):
                    return True
                continue
            return True
    return False


def _extract_time_from_text(user_text: str) -> time | None:
    """Try to extract an explicit time (hour[:min] with optional am/pm) from user text.

    Returns a `time` object or None if not found.
    Supports forms like '3pm', '15:30', 'after 3pm', 'any time after 3pm'.
    """
    if not user_text or not isinstance(user_text, str):
        return None
    s = user_text.lower()
    # Common numeric patterns: '3pm', '3 pm', '15:30', 'after 3pm'
    m = re.search(r"(\d{1,2})(?::(\d{2}))?\s*(am|pm)?", s)
    if m:
        # Only accept numeric matches as times when they are explicit times (am/pm or colon),
        # or when preceded by a time-word like 'at', 'after', 'before', 'around', 'by', 'until'.
        pre = s[max(0, m.start() - 12):m.start()]
        mer = m.group(3)
        has_colon = bool(m.group(2))
        if mer or has_colon or re.search(r"(at|after|before|around|by|until|past)\s*$", pre):
            hour = int(m.group(1))
            minute = int(m.group(2)) if m.group(2) else 0
            if mer:
                mer = mer.lower()
                if mer == 'pm' and hour < 12:
                    hour = (hour % 12) + 12
                if mer == 'am' and hour == 12:
                    hour = 0
            # clamp hour
            if 0 <= hour <= 23:
                try:
                    return time(hour=hour, minute=minute)
                except Exception:
                    return None
        # otherwise, this numeric match is probably not a time (e.g., capacity '5L')
    # If no numeric time, look for textual cues like 'before midday', 'before noon', 'morning', 'afternoon'
    if re.search(r"before\s+(noon|midday|12)\b", s):
        return time(11, 59)
    if re.search(r"\bbefore\s+12\b", s):
        return time(11, 59)
    if re.search(r"\b(noon|midday)\b", s):
        return time(12, 0)
    # phrases like 'after noon' or 'after midday' should map to just after midday
    if re.search(r"after\s+(noon|midday|12)\b", s):
        return time(12, 1)
    if re.search(r"\bmorning\b", s):
        return time(9, 0)
    if re.search(r"\bafternoon\b", s):
        return time(15, 0)
    if re.search(r"\bevening\b", s):
        return time(18, 0)
    # fallback
    return None

# Require Google Gemini
try:
    import google.generativeai as genai
    HAS_GEMINI = True
except ImportError:
    HAS_GEMINI = False


class LLMParser:
    """
    Parses natural language into structured dispatch requests using Google Gemini.
    """

    def __init__(self):
        """Initialize LLM client (Gemini)."""
        # Require Gemini API key and SDK
        gemini_key = os.getenv("GEMINI_API_KEY") or os.getenv("GOOGLE_API_KEY")
        if not gemini_key:
            raise ValueError(
                "No Gemini API key found. Set GEMINI_API_KEY in .env file.\n"
                "Install the SDK with: pip install google-generativeai"
            )

        if not HAS_GEMINI:
            raise ValueError(
                "google-generativeai SDK not installed. Install with: pip install google-generativeai"
            )

        # Configure Gemini
        genai.configure(api_key=gemini_key)
        self.provider = "gemini"
        self.model_name = os.getenv("GEMINI_MODEL", "gemini-2.5-flash")
        # Create the GenerativeModel instance
        self.client = genai.GenerativeModel(model_name=self.model_name)

        # System prompt for parsing dispatch requests
        self.system_prompt = """You are an assistant for the MedSupplyDrones dispatch system.
Your job is to parse natural language requests into structured JSON for medical supply deliveries.

IMPORTANT: You must respond with ONLY valid JSON, no other text or explanation.

The JSON format should match this structure:
{
  "dispatches": [
    {
      "id": 1,
      "date": "2025-01-15",
      "time": "14:00:00",
      "timeAfter": "17:00:00",
      "timeBefore": "20:00:00",
      "requirements": {
        "capacity": 3.0,
        "cooling": true,
        "heating": false,
        "maxCost": 50.0
      },
      "delivery": {
        "lng": -3.1883,
        "lat": 55.9445
      }
    }
  ],
  "strategy": "min_cost"
}

Time constraint fields:
- "time": Use ONLY if an exact time is specified (e.g., "at 3pm")
- "timeAfter": Use for "after X" constraints (e.g., "after 5pm" → "timeAfter": "17:00:00")
- "timeBefore": Use for "before X" constraints (e.g., "before 3pm" → "timeBefore": "15:00:00")
- "timeAfter" + "timeBefore": Use for "between X and Y" constraints (e.g., "between 2pm and 4pm" → "timeAfter": "14:00:00", "timeBefore": "16:00:00")

IMPORTANT: Do NOT set "time" when the user specifies a time constraint like "after", "before", or "between".
Only use "time" for exact delivery times.

Known locations (use these coordinates when mentioned):
- Appleton Tower: {"lng": -3.1873, "lat": 55.9445}
- Royal Infirmary: {"lng": -3.1883, "lat": 55.9217}
- Western General: {"lng": -3.2416, "lat": 55.9642}
- St John's Hospital: {"lng": -3.5064, "lat": 55.9348}
- Sick Kids Hospital: {"lng": -3.2029, "lat": 55.9233}

Strategy options:
- "min_cost" - minimize delivery cost
- "min_moves" - minimize number of moves
- "balanced" - balance cost and moves

Capacity is measured in liters (L).

Example inputs:
"Deliver 3 liters of cooled insulin from Appleton Tower to Royal Infirmary as cheaply as possible"
"Urgent: 5L heated medicine to Western General, minimize moves"

Extract:
1. Delivery requirements (capacity, cooling, heating)
2. Locations (from/to)
3. Optimization preference (cost vs moves)
4. Any cost constraints mentioned
5. Date and time information (exact or relative like 'tomorrow', 'today', 'in 3 days')

IMPORTANT DATE HANDLING:
- For relative dates like 'tomorrow', 'today', 'in X days', 'in X weeks', 'next week', use the ACTUAL date string in the output
- 'today' → use the current date being processed
- 'tomorrow' → add 1 day to current date
- 'in X days' → add X days to current date
- 'in X weeks' → add X*7 days to current date
- 'next week' → add 7 days to current date
- If no date specified, use today's date

Generate sequential IDs starting from 1 for each dispatch.
Default to min_cost strategy if optimization preference unclear."""

    def parse_to_scenario(self, text: str) -> Optional[PlanningScenario]:
        """
        Parse natural language text into a PlanningScenario.

        Args:
            text: Natural language description of dispatch request(s).

        Returns:
            PlanningScenario object or None if parsing fails.
        """
        try:
            # Get current date/time to provide context to LLM
            now = datetime.utcnow()
            current_date = now.strftime("%Y-%m-%d")
            current_time = now.strftime("%H:%M:%S")

            # Call Gemini API
            prompt = f"""{self.system_prompt}

CURRENT DATE: {current_date}
CURRENT TIME: {current_time}

User request: {text}

Respond with ONLY valid JSON, no markdown or explanation."""

            generation_config = genai.GenerationConfig(
                temperature=0.1,
                response_mime_type="application/json"
            )

            response = self.client.generate_content(
                prompt,
                generation_config=generation_config
            )

            # Extract JSON text from response
            json_text = response.text

            # Extract and parse JSON
            scenario_dict = json.loads(json_text)
            # Debug: show raw LLM JSON and parsed dict for troubleshooting
            try:
                print("[DEBUG llm_response]", json_text)
                print("[DEBUG parsed_scenario_dict]", json.dumps(scenario_dict, indent=2))
            except Exception:
                pass


            # Resolve relative date phrases (if LLM emitted human text like "this Saturday")
            now = datetime.utcnow()
            if isinstance(scenario_dict, dict) and 'dispatches' in scenario_dict:
                for d in scenario_dict['dispatches']:
                    try:
                        raw_date = d.get('date')
                        if isinstance(raw_date, str) and re.search('[a-zA-Z]', raw_date):
                            # Try holidays first (e.g., 'christmas day', 'new year's day')
                            try:
                                holiday_res = _resolve_holiday(raw_date, now)
                                if holiday_res:
                                    d['date'] = holiday_res
                                    continue
                            except Exception:
                                pass

                            # Prefer dateparser for natural-language parsing when available.
                            parsed_dt = None
                            if HAS_DATEPARSER:
                                try:
                                    parsed_dt = dateparser.parse(
                                        raw_date,
                                        settings={
                                            'RELATIVE_BASE': now,
                                            'PREFER_DATES_FROM': 'future',
                                        }
                                    )
                                except Exception:
                                    parsed_dt = None

                            if parsed_dt:
                                # dateparser returned a datetime — extract date and optionally time
                                d['date'] = parsed_dt.date().isoformat()
                                # only set time if parser provided a non-midnight time
                                if not (parsed_dt.time().hour == 0 and parsed_dt.time().minute == 0 and parsed_dt.time().second == 0):
                                    d['time'] = parsed_dt.time().isoformat()
                            else:
                                # fallback to simple resolver for phrases like 'this saturday'
                                resolved = _resolve_relative_date(raw_date, now)
                                if resolved:
                                    d['date'] = resolved
                    except Exception:
                        # ignore resolver errors and leave original value
                        pass

            # If the LLM inserted a default maxCost but the user didn't ask for one,
            # treat it as unspecified so downstream matching can use drone-specific budgets.
            try:
                user_wants_maxcost = _user_specified_maxcost(text)
                if isinstance(scenario_dict, dict) and 'dispatches' in scenario_dict and not user_wants_maxcost:
                    for d in scenario_dict['dispatches']:
                        req = d.get('requirements')
                        if isinstance(req, dict) and 'maxCost' in req:
                            # clear LLM-inserted default
                            req['maxCost'] = None
            except Exception:
                pass

            # Validate and create PlanningScenario
            scenario = PlanningScenario(**scenario_dict)

            # --- Fallback geocoding ---
            # If the LLM returned missing coordinates or placeholder zeros, try to
            # resolve a sensible lat/lng. First check a small in-prompt known-locations
            # table, then fall back to Nominatim lookup using the original user text.
            geowarnings = []
            try:
                # small mapping copied from the system prompt (keeps behavior stable)
                known_locations = {
                    'appleton tower': {'lng': -3.1873, 'lat': 55.9445},
                    'royal infirmary': {'lng': -3.1883, 'lat': 55.9217},
                    'western general': {'lng': -3.2416, 'lat': 55.9642},
                    "st john's hospital": {'lng': -3.5064, 'lat': 55.9348},
                    'sick kids hospital': {'lng': -3.2029, 'lat': 55.9233}
                }

                # Helper to attempt to set coords on a dispatch object
                def _set_coords_if_missing(d):
                    try:
                        # d.delivery is a Position object (pydantic) with lng/lat
                        cur_lng = getattr(d.delivery, 'lng', None)
                        cur_lat = getattr(d.delivery, 'lat', None)
                    except Exception:
                        cur_lng = None
                        cur_lat = None

                    missing_or_zero = (
                        cur_lng is None or cur_lat is None or
                        (isinstance(cur_lng, (int, float)) and isinstance(cur_lat, (int, float)) and (float(cur_lng) == 0.0 and float(cur_lat) == 0.0))
                    )
                    if not missing_or_zero:
                        return False

                    # 1) try known locations by looking for their names in the user text
                    ut = text.lower()
                    for name, pos in known_locations.items():
                        if name in ut:
                            try:
                                d.delivery.lng = float(pos['lng'])
                                d.delivery.lat = float(pos['lat'])
                                geowarnings.append(f"Auto-geocoded dispatch id {d.id} from known location '{name}'")
                                return True
                            except Exception:
                                continue

                    # 2) try Nominatim using the full user text as a query
                    try:
                        nom_url = 'https://nominatim.openstreetmap.org/search'
                        params = {'q': text, 'format': 'json', 'limit': 1}
                        headers = {'User-Agent': 'MedSupplyDrones/1.0 (contact: dev@example.com)'}
                        r = requests.get(nom_url, params=params, headers=headers, timeout=8)
                        if r.ok:
                            j = r.json()
                            if isinstance(j, list) and len(j) > 0:
                                first = j[0]
                                lat = first.get('lat')
                                lon = first.get('lon') or first.get('lng')
                                if lat and lon:
                                    try:
                                        d.delivery.lng = float(lon)
                                        d.delivery.lat = float(lat)
                                        geowarnings.append(f"Auto-geocoded dispatch id {d.id} using Nominatim ('{text[:80]}')")
                                        return True
                                    except Exception:
                                        pass
                    except Exception:
                        # network or parsing failure - ignore and continue
                        pass

                    return False

                # Iterate and try to fix each dispatch
                if hasattr(scenario, 'dispatches') and scenario.dispatches:
                    for disp in scenario.dispatches:
                        try:
                            _set_coords_if_missing(disp)
                        except Exception:
                            continue
            except Exception:
                # swallow geocoding errors - geocoding is a best-effort fallback
                geowarnings = []

            # Ensure all dispatch datetimes are not in the past. If a parsed
            # dispatch has missing date/time, or its combined datetime is before
            # now, adjust it forward to be at least a few minutes in the future.
            now = datetime.utcnow()
            min_future = now + timedelta(minutes=5)

            # If the user's original text contained a holiday phrase (e.g. 'christmas day this year')
            # but the LLM output left dispatch dates as today or missing, prefer the holiday date.
            try:
                holiday_from_text = _resolve_holiday(text, now)
                print(f"[DEBUG holiday_from_text] {holiday_from_text}")
                if holiday_from_text:
                    for d in scenario.dispatches:
                        try:
                            # Print current values/types for debugging
                            try:
                                print(f"[DEBUG before_override] id={getattr(d, 'id', None)} date={repr(getattr(d, 'date', None))} (type={type(getattr(d, 'date', None))}) time={repr(getattr(d, 'time', None))} (type={type(getattr(d, 'time', None))})")
                            except Exception:
                                pass

                            # Determine parsed_date if available (handle str, date)
                            parsed_date_val = None
                            try:
                                if not getattr(d, 'date', None):
                                    parsed_date_val = None
                                else:
                                    if isinstance(d.date, str):
                                        try:
                                            parsed_date_val = datetime.fromisoformat(d.date).date()
                                        except Exception:
                                            parsed_date_val = None
                                    elif isinstance(d.date, date):
                                        parsed_date_val = d.date
                                    else:
                                        parsed_date_val = None
                            except Exception:
                                parsed_date_val = None

                            # If the dispatch date is missing, today's date, or in the past,
                            # prefer the holiday date extracted from the user's text.
                            date_needs_override = False
                            if parsed_date_val is None:
                                date_needs_override = True
                            else:
                                if parsed_date_val <= now.date():
                                    date_needs_override = True

                            if date_needs_override:
                                d.date = holiday_from_text

                            # Also set a reasonable default time (midday) instead of midnight
                            # to avoid the datetime being interpreted as "past" on the holiday itself
                            # Determine parsed_time if available (handle str, time)
                            parsed_time_val = None
                            try:
                                if not getattr(d, 'time', None):
                                    parsed_time_val = None
                                else:
                                    if isinstance(d.time, str):
                                        try:
                                            parsed_time_val = time.fromisoformat(d.time)
                                        except Exception:
                                            parsed_time_val = None
                                    elif isinstance(d.time, time):
                                        parsed_time_val = d.time
                                    else:
                                        parsed_time_val = None
                            except Exception:
                                parsed_time_val = None

                            # If time missing or midnight-like, set to midday to avoid 'past' interpretations
                            time_needs_override = False
                            if parsed_time_val is None:
                                time_needs_override = True
                            else:
                                if parsed_time_val.hour == 0:
                                    time_needs_override = True

                            if time_needs_override:
                                d.time = "12:00:00"

                            try:
                                print(f"[DEBUG after_override] id={getattr(d, 'id', None)} date={repr(getattr(d, 'date', None))} (type={type(getattr(d, 'date', None))}) time={repr(getattr(d, 'time', None))} (type={type(getattr(d, 'time', None))})")
                            except Exception:
                                pass
                        except Exception:
                            continue
            except Exception:
                pass

            adjusted = False
            adjustments = []
            # Detect if the user's text contained relative-date phrases (e.g. 'next Wednesday')
            user_has_relative = _user_has_relative_date_phrase(text)
            for d in scenario.dispatches:
                # d.date and d.time may be None or strings
                d_date = d.date
                d_time = d.time

                # Check if time constraints are specified
                has_time_constraints = (
                    getattr(d, 'timeAfter', None) is not None or
                    getattr(d, 'timeBefore', None) is not None
                )

                # If date missing, use today's date (UTC)
                if not d_date:
                    d_date = now.date().isoformat()

                # If time missing and no time constraints, use current time
                # If time constraints are present, don't set a default time
                if not d_time and not has_time_constraints:
                    # round up to next minute
                    t = (now + timedelta(minutes=1)).time().replace(microsecond=0)
                    d_time = t.isoformat()
                elif has_time_constraints and not d_time:
                    # When time constraints are specified, use the timeAfter value as the reference time
                    # for validation purposes (or timeBefore if only that is set)
                    if getattr(d, 'timeAfter', None):
                        d_time = d.timeAfter
                    elif getattr(d, 'timeBefore', None):
                        d_time = d.timeBefore
                    else:
                        # Fallback if neither is set (shouldn't happen)
                        t = (now + timedelta(minutes=1)).time().replace(microsecond=0)
                        d_time = t.isoformat()

                # Parse safely
                try:
                    parsed_date = datetime.fromisoformat(d_date).date() if isinstance(d_date, str) else d_date
                except Exception:
                    parsed_date = now.date()

                try:
                    parsed_time = time.fromisoformat(d_time) if isinstance(d_time, str) else d_time
                except Exception:
                    parsed_time = (now + timedelta(minutes=1)).time().replace(microsecond=0)

                combined = datetime.combine(parsed_date, parsed_time)
                # If user used relative-date wording (e.g. 'next Wednesday'), prefer parsing
                # the original user text to compute the intended future date/time.
                if combined < min_future and user_has_relative:
                    pd = None
                    if HAS_DATEPARSER:
                        try:
                            pd = dateparser.parse(text, settings={
                                'RELATIVE_BASE': now,
                                'PREFER_DATES_FROM': 'future',
                            })
                        except Exception:
                            pd = None

                    if pd:
                        pdn = pd.replace(microsecond=0)
                        if pdn >= min_future:
                            old = combined.isoformat()
                            d.date = pd.date().isoformat()
                            d.time = pd.time().isoformat()
                            adjusted = True
                            adjustments.append(f"dispatch id {d.id} datetime set from LLM absolute {old} to user-relative {pdn.isoformat()}")
                            # update parsed_date/parsed_time/combined for later checks
                            parsed_date = pd.date()
                            parsed_time = pd.time()
                            combined = datetime.combine(parsed_date, parsed_time)
                    else:
                        # fallback: try to extract weekday phrase and time from user text
                        s = text.lower()
                        wd_match = re.search(r"(?:(this|next)\s+)?(monday|tuesday|wednesday|thursday|friday|saturday|sunday)", s)
                        if wd_match:
                            phrase = (wd_match.group(1) + " " + wd_match.group(2)).strip() if wd_match.group(1) else wd_match.group(2)
                            try:
                                resolved = _resolve_relative_date(phrase, now)
                                if resolved:
                                    # extract time if present in text
                                    tobj = _extract_time_from_text(text)
                                    if tobj is None:
                                        # default to MIDDAY (avoid midnight being used for unspecified times)
                                        tobj = time(12, 0)
                                    pd_dt = datetime.combine(datetime.fromisoformat(resolved).date(), tobj)
                                    if pd_dt >= min_future:
                                        old = combined.isoformat()
                                        d.date = pd_dt.date().isoformat()
                                        d.time = pd_dt.time().isoformat()
                                        adjusted = True
                                        adjustments.append(f"dispatch id {d.id} datetime set from LLM absolute {old} to user-relative {pd_dt.isoformat()}")
                                        parsed_date = pd_dt.date()
                                        parsed_time = pd_dt.time()
                                        combined = datetime.combine(parsed_date, parsed_time)
                            except Exception:
                                pass

                # Assume incoming times are in UTC. If you use localtime, change here.
                if combined < min_future:
                    # Bump to at least min_future, rounded UP to the next whole minute.
                    nd = min_future.replace(microsecond=0)
                    if nd.second > 0:
                        nd = (nd + timedelta(minutes=1)).replace(second=0)
                    old = combined.isoformat()
                    d.date = nd.date().isoformat()
                    # Only set time if no time constraints are specified
                    if not has_time_constraints:
                        d.time = nd.time().isoformat()
                    adjusted = True
                    adjustments.append(f"dispatch id {d.id} datetime adjusted from {old} to {nd.isoformat()}")
                else:
                    # keep original (normalized) values
                    d.date = parsed_date.isoformat()
                    # Only set time if no time constraints are specified
                    if not has_time_constraints:
                        d.time = parsed_time.isoformat()

            if adjusted:
                # Attach a lightweight warnings list to the returned scenario so the UI can show what changed.
                try:
                    existing = getattr(scenario, '_warnings', []) or []
                    # merge existing warnings (e.g., geocoding notes) with time-adjustment notes
                    scenario._warnings = existing + adjustments
                except Exception:
                    # If attaching fails, still print a server-side warning
                    print("[WARN] Some parsed dispatch datetimes were in the past; they were adjusted to the near future.")
            else:
                # If we didn't attach adjustments but had geocode warnings, ensure they are present
                try:
                    if geowarnings:
                        existing = getattr(scenario, '_warnings', []) or []
                        scenario._warnings = existing + geowarnings
                except Exception:
                    pass

            return scenario

        except json.JSONDecodeError as e:
            print(f"JSON parsing error: {e}")
            return None
        except Exception as e:
            print(f"LLM parsing error: {e}")
            return None

    def answer_fleet_question(
        self,
        text: str,
        fleet_summary: Dict[str, Any]
    ) -> str:
        """
        Answer questions about fleet capabilities using LLM.

        Args:
            text: Natural language question about fleet.
            fleet_summary: Fleet summary data from /summary/fleet.

        Returns:
            Natural language answer.
        """
        print("[DEBUG] fleet_summary:", json.dumps(fleet_summary, indent=2))

        # Check if question is about weekday availability
        text_lower = text.lower()
        # Quick deterministic answers for common capability counts (avoid LLM when possible)
        try:
            wants_cool = any(k in text_lower for k in ("cool", "cooling", "cooled"))
            wants_heat = any(k in text_lower for k in ("heat", "heating", "heated"))

            # If user asked for both cooling AND heating, try to count drones
            # that have both capabilities using a detailed drone list when available.
            if wants_cool and wants_heat:
                # Look for a detailed drone list inside fleet_summary under common keys
                drones_list = None
                for key in ("drones", "droneList", "drone_list", "fleet", "drones_list"):
                    if key in fleet_summary and isinstance(fleet_summary[key], list):
                        drones_list = fleet_summary[key]
                        break

                both_count = None
                if drones_list is not None:
                    c = 0
                    try:
                        for d in drones_list:
                            cap = d.get('capability') if isinstance(d, dict) else None
                            if cap and cap.get('cooling') and cap.get('heating'):
                                c += 1
                        both_count = c
                    except Exception:
                        both_count = None

                # If no detailed list present, try to fetch full drone records
                # from the ILP REST service to compute an exact count.
                if both_count is None and drones_list is None:
                    try:
                        ilp_base = os.getenv('ILPENDPOINT', None)
                        if not ilp_base:
                            ilp_base = "https://ilp-rest-2025-bvh6e9hschfagrgy.ukwest-01.azurewebsites.net/"
                        if ilp_base.endswith('/'):
                            ilp_base = ilp_base[:-1]
                        try:
                            resp = requests.get(f"{ilp_base}/drones", timeout=10)
                            resp.raise_for_status()
                            ilp_drones = resp.json()
                            c = 0
                            for d in ilp_drones:
                                cap = d.get('capability') if isinstance(d, dict) else None
                                if cap and cap.get('cooling') and cap.get('heating'):
                                    c += 1
                            both_count = c
                        except Exception:
                            both_count = None
                    except Exception:
                        both_count = None

                if both_count is None:
                    # Fallback: use the lower of the two capability counts as an estimate
                    try:
                        both_count = min(int(fleet_summary.get('dronesWithCooling') or 0), int(fleet_summary.get('dronesWithHeating') or 0))
                    except Exception:
                        both_count = fleet_summary.get('dronesWithCooling') or fleet_summary.get('dronesWithHeating') or 0

                return f"{both_count}"

            # Single capability quick answers
            if wants_cool:
                cnt = None
                try:
                    cnt = int(fleet_summary.get('dronesWithCooling') or 0)
                except Exception:
                    cnt = fleet_summary.get('dronesWithCooling')
                return f"{cnt}"
            if wants_heat:
                cnt = None
                try:
                    cnt = int(fleet_summary.get('dronesWithHeating') or 0)
                except Exception:
                    cnt = fleet_summary.get('dronesWithHeating')
                return f"{cnt}"
        except Exception:
            pass
        weekdays = ["monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday"]
        mentioned_day = None
        for day in weekdays:
            if day in text_lower:
                mentioned_day = day.upper()
                break

        # If asking about a specific day, fetch availability data
        availability_data = None
        if mentioned_day or any(keyword in text_lower for keyword in ["available", "availability", "schedule", "day", "weekday"]):
            try:
                api_base = os.getenv("DISPATCH_API_URL", "http://localhost:8000")
                # detect time in question (e.g., 'after 4pm', 'before midday', or 'wednesday morning')
                time_obj = _extract_time_from_text(text)
                modifier = None
                if 'before' in text_lower:
                    modifier = 'before'
                elif 'after' in text_lower:
                    modifier = 'after'

                # detect time-of-day windows like morning/afternoon/evening/night
                tod_windows = {
                    'morning': ('06:00', '12:00'),
                    'afternoon': ('12:00', '17:00'),
                    'evening': ('17:00', '21:00'),
                    'night': ('21:00', '23:00'), 
                    'overnight': ('23:00', '06:00')
                }
                window_start = None
                window_end = None
                window_kw = None
                for kw, (s_t, e_t) in tod_windows.items():
                    if kw in text_lower:
                        window_start, window_end = s_t, e_t
                        window_kw = kw
                        break

                time_param = None
                if time_obj:
                    time_param = time_obj.strftime("%H:%M")

                # Build URL with appropriate query params (support both after and before)
                params = []
                if mentioned_day:
                    params.append(f"day={mentioned_day}")
                # Window takes precedence over single modifiers
                if window_start and window_end:
                    params.append(f"after={window_start}")
                    params.append(f"before={window_end}")
                else:
                    if time_param and modifier == 'after':
                        params.append(f"after={time_param}")
                    if time_param and modifier == 'before':
                        params.append(f"before={time_param}")

                if params:
                    url = f"{api_base}/summary/availability?" + "&".join(params)
                else:
                    url = f"{api_base}/summary/availability"

                resp = requests.get(url, timeout=10)
                resp.raise_for_status()
                availability_data = resp.json()
                print(f"[DEBUG] Fetched availability data for {mentioned_day or 'all days'}")
            except Exception as e:
                print(f"[WARN] Could not fetch availability data: {e}")

            # If we fetched availability and the user asked about a specific day/time,
            # return a deterministic concise answer listing the drone ids rather than calling LLM.
            try:
                if availability_data:
                    # If a specific day was requested, prefer that slice
                    if mentioned_day:
                        day_list = availability_data.get('overall', {}).get(mentioned_day, [])
                    else:
                        # No specific day requested: aggregate all days or fall back to raw availability
                        day_list = availability_data.get('overall', {})

                    # If user asked for the cheapest drone, compute it deterministically
                    if 'cheapest' in text_lower or 'cheapest drone' in text_lower or 'cheapest to deliver' in text_lower:
                        try:
                            # If a specific day was requested, require the day_list to be a list of ids
                            if not mentioned_day:
                                return "Please specify a day (e.g., 'on Monday') to find the cheapest available drone."

                            if not day_list:
                                return f"No drones available on {mentioned_day.title()}."

                            # Fetch full drone records from ILP REST service to read cost fields
                            ilp_base = os.getenv('ILPENDPOINT', None)
                            if not ilp_base:
                                ilp_base = "https://ilp-rest-2025-bvh6e9hschfagrgy.ukwest-01.azurewebsites.net/"
                            if ilp_base.endswith('/'):
                                ilp_base = ilp_base[:-1]

                            try:
                                resp = requests.get(f"{ilp_base}/drones", timeout=10)
                                resp.raise_for_status()
                                ilp_drones = resp.json()
                            except Exception as e:
                                print(f"[WARN] Could not fetch ILP drones for cheapest calculation: {e}")
                                ilp_drones = []

                            # Build mapping id -> capability costs and maxMoves
                            costs = {}
                            for d in ilp_drones:
                                did = str(d.get('id'))
                                cap = d.get('capability') or {}
                                try:
                                    cost_initial = float(cap.get('costInitial', 0) or 0)
                                except Exception:
                                    cost_initial = 0.0
                                try:
                                    cost_per_move = float(cap.get('costPerMove', 0) or 0)
                                except Exception:
                                    cost_per_move = 0.0
                                try:
                                    cost_final = float(cap.get('costFinal', 0) or 0)
                                except Exception:
                                    cost_final = 0.0
                                try:
                                    max_moves = int(cap.get('maxMoves')) if cap.get('maxMoves') is not None else None
                                except Exception:
                                    max_moves = None
                                costs[did] = {
                                    'costInitial': cost_initial,
                                    'costPerMove': cost_per_move,
                                    'costFinal': cost_final,
                                    'maxMoves': max_moves
                                }

                            # Parse required moves from question, e.g. 'more than 1500 moves' or '1500 moves'
                            moves_required = 500  # default
                            m = re.search(r"more than\s*(\d{1,7})\s*moves?", text.lower())
                            if not m:
                                m = re.search(r"(\d{1,7})\s*moves?", text.lower())
                            if m:
                                try:
                                    moves_required = int(m.group(1))
                                except Exception:
                                    moves_required = 500  # default if parsing fails

                            # Filter available drones and compute total cost for the required moves
                            available_ids = [str(x) for x in day_list]
                            candidates = []
                            for did in available_ids:
                                info = costs.get(did)
                                if not info:
                                    continue
                                # If moves_required provided, ensure drone capability can handle it
                                if moves_required is not None:
                                    maxm = info.get('maxMoves')
                                    if maxm is not None and maxm < moves_required:
                                        # skip drones that cannot handle required moves
                                        continue
                                    # compute total cost using moves_required
                                    total = info['costInitial'] + info['costPerMove'] * float(moves_required) + info['costFinal']
                                else:
                                    # default to a single-move metric if moves unspecified
                                    total = info['costInitial'] + info['costPerMove'] * 1 + info['costFinal']
                                candidates.append((did, total, info))

                            if not candidates:
                                if moves_required is not None:
                                    return f"No available drones on {mentioned_day.title()} can handle {moves_required} moves."
                                return f"Drones available on {mentioned_day.title()}: {', '.join(available_ids)} (no cost data available to pick the cheapest)."

                            # choose min total cost
                            best = min(candidates, key=lambda it: it[1])
                            best_id, best_total, best_info = best
                            if moves_required is None:
                                moves_required = 1
                            # If a time window was requested, include it in the phrasing (e.g., 'on Tuesday morning')
                            if window_kw:
                                return (
                                    f"Cheapest available on {mentioned_day.title()} {window_kw}: Drone {best_id} — price for a delivery with {moves_required} moves: {best_total:.2f} "
                                    f"(costInitial={best_info['costInitial']:.2f}, costPerMove={best_info['costPerMove']:.2f}, costFinal={best_info['costFinal']:.2f})"
                                )
                            return (
                                f"Cheapest available on {mentioned_day.title()}: Drone {best_id} — price for a delivery with {moves_required} moves: {best_total:.2f} "
                                f"(costInitial={best_info['costInitial']:.2f}, costPerMove={best_info['costPerMove']:.2f}, costFinal={best_info['costFinal']:.2f})"
                            )
                        except Exception as e:
                            print(f"[WARN] cheapest calculation failed: {e}")
                            # fall through to LLM fallback below

                    # If a time window or explicit time was present in the question, produce a direct answer
                    if mentioned_day:
                        if window_start and window_end:
                            if not day_list:
                                return f"No drones available on {mentioned_day.title()} during the {kw}."
                            return f"Drones available on {mentioned_day.title()} during the {kw}: {', '.join(day_list)}."
                        if time_obj:
                            if modifier == 'before':
                                if not day_list:
                                    return f"No drones available before {time_obj.strftime('%H:%M')} on {mentioned_day.title()}."
                                return f"Drones available before {time_obj.strftime('%H:%M')} on {mentioned_day.title()}: {', '.join(day_list)}."
                            else:
                                if not day_list:
                                    return f"No drones available after {time_obj.strftime('%H:%M')} on {mentioned_day.title()}."
                                return f"Drones available after {time_obj.strftime('%H:%M')} on {mentioned_day.title()}: {', '.join(day_list)}."

                # Fall back to LLM-generated answer using the availability data as context
                data_context = f"Fleet Summary:\n{json.dumps(fleet_summary, indent=2)}"
                data_context += f"\n\nWeekday Availability:\n{json.dumps(availability_data, indent=2)}" if availability_data else data_context

                prompt = f"""Based on this fleet data, answer the question concisely:

{data_context}

Question: {text}

Provide a brief, helpful answer."""

                system_context = "You are a helpful assistant for the MedSupplyDrones fleet. Answer questions clearly and concisely."
                full_prompt = f"{system_context}\n\n{prompt}"

                generation_config = genai.GenerationConfig(temperature=0.3)

                response = self.client.generate_content(
                    full_prompt,
                    generation_config=generation_config
                )

                return response.text

            except Exception as e:
                return f"Error answering question: {str(e)}"

    def classify_intent(self, text: str) -> str:
        """
        Classify user intent: 'plan' for route planning, 'query' for fleet questions.

        Args:
            text: User input text.

        Returns:
            'plan' or 'query'
        """
        # Simple keyword-based classification (can be enhanced with LLM)
        text_lower = text.lower()

        # Planning keywords
        plan_keywords = [
            "deliver", "delivery", "dispatch", "send", "transport",
            "route", "plan", "schedule", "ship"
        ]

        # Query keywords
        query_keywords = [
            "how many", "what drones", "which drones", "available",
            "capacity", "capabilities", "can do", "able to"
        ]

        plan_score = sum(1 for kw in plan_keywords if kw in text_lower)
        query_score = sum(1 for kw in query_keywords if kw in text_lower)

        if plan_score > query_score:
            return "plan"
        elif query_score > 0:
            return "query"
        else:
            return "plan"  # Default to planning
