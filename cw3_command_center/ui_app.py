"""
Streamlit Command Center UI for MedSupplyDrones.
Natural language interface for dispatch planning and what-if analysis.
"""

import streamlit as st
import requests
import json
import os
from datetime import datetime
from dotenv import load_dotenv
from llm_parser import LLMParser
from typing import Optional
import pydeck as pdk
import pandas as pd

# Load environment variables
load_dotenv()

# Configuration
API_BASE = os.getenv("DISPATCH_API_URL", "http://localhost:8000")
st.set_page_config(
    page_title="MedSupplyDrones Command Center",
    page_icon="🚁",
    layout="wide"
)


def init_session_state():
    """Initialize session state variables."""
    if 'llm_parser' not in st.session_state:
        try:
            st.session_state.llm_parser = LLMParser()
            st.session_state.llm_enabled = True
        except ValueError as e:
            st.session_state.llm_enabled = False
            st.warning(f"LLM disabled: {e}. Set GEMINI_API_KEY to enable natural language parsing.")

    if 'last_scenario' not in st.session_state:
        st.session_state.last_scenario = None
    if 'last_plan' not in st.session_state:
        st.session_state.last_plan = None

    # Session-based scenario queue for hypothetical planning
    if 'scenario_queue' not in st.session_state:
        st.session_state.scenario_queue = []
    if 'queue_counter' not in st.session_state:
        st.session_state.queue_counter = 1

    # (simulation session removed) previously tracked drone busy windows
    if 'saved_geojsons' not in st.session_state:
        st.session_state.saved_geojsons = []


def add_to_scenario_queue(user_text: str, parsed_scenario: dict):
    """Add a parsed scenario to the session queue."""
    queue_item = {
        'id': st.session_state.queue_counter,
        'user_text': user_text,
        'scenario': parsed_scenario,
        'timestamp': datetime.now().strftime("%H:%M:%S")
    }
    st.session_state.scenario_queue.append(queue_item)
    st.session_state.queue_counter += 1
    return queue_item['id']


def clear_scenario_queue():
    """Clear the scenario queue."""
    st.session_state.scenario_queue = []
    st.session_state.queue_counter = 1


def merge_queued_scenarios(strategy: str = "min_cost"):
    """Merge all queued scenarios into a single combined scenario for planning."""
    if not st.session_state.scenario_queue:
        return None

    all_dispatches = []
    dispatch_id = 1

    for item in st.session_state.scenario_queue:
        scenario = item['scenario']
        for dispatch in scenario.get('dispatches', []):
            # Reassign sequential IDs
            dispatch_copy = dispatch.copy()
            dispatch_copy['id'] = dispatch_id
            all_dispatches.append(dispatch_copy)
            dispatch_id += 1

    return {
        'dispatches': all_dispatches,
        'strategy': strategy
    }


def display_header():
    """Display application header."""
    st.title("🚁 MedSupplyDrones Command Center")
    st.markdown("*Natural-language dispatch planning and what-if analysis*")
    st.divider()


def get_fleet_summary():
    """Fetch fleet summary from API."""
    try:
        response = requests.get(f"{API_BASE}/summary/fleet", timeout=10)
        response.raise_for_status()
        return response.json()
    except Exception as e:
        st.error(f"Failed to fetch fleet summary: {e}")
        return None


def _extract_delivered_ids(drone_path: dict):
    """Robustly extract delivered dispatch IDs from a dronePath entry.

    Handles many shapes returned by CW2 / planning endpoints:
    - keys like `deliveredIds`, `deliveries`, `delivered`, `delivered_ids`
    - lists of ints, lists of dicts with `id`/`dispatchId`/`deliveryId`
    - CSV strings like "1,2,3"
    - waypoint-level metadata fields in `flightPath`
    Returns a list of unique ints (preserves order), or empty list.
    """
    if not drone_path or not isinstance(drone_path, dict):
        return []

    candidate_keys = [
        'deliveredIds', 'deliveries', 'delivered', 'delivered_ids',
        'deliveredIdsList', 'deliveredList', 'deliveriesIds'
    ]

    def _norm(val):
        ids = []
        if isinstance(val, list):
            for item in val:
                if isinstance(item, dict):
                    # common id field names
                    for f in ('id', 'dispatchId', 'deliveryId', 'dispatch_id'):
                        if f in item and item.get(f) is not None:
                            try:
                                ids.append(int(item[f]))
                            except Exception:
                                pass
                            break
                    else:
                        # fallback: pick first numeric-like value
                        for v in item.values():
                            if isinstance(v, int):
                                ids.append(int(v))
                                break
                            if isinstance(v, str) and v.isdigit():
                                ids.append(int(v))
                                break
                elif isinstance(item, int):
                    ids.append(item)
                elif isinstance(item, str):
                    if ',' in item:
                        for p in item.split(','):
                            s = p.strip()
                            if s.isdigit():
                                ids.append(int(s))
                    elif item.isdigit():
                        ids.append(int(item))
        elif isinstance(val, int):
            ids.append(val)
        elif isinstance(val, str):
            if ',' in val:
                for p in val.split(','):
                    s = p.strip()
                    if s.isdigit():
                        ids.append(int(s))
            elif val.isdigit():
                ids.append(int(val))
        return ids

    found = []
    for k in candidate_keys:
        if k in drone_path and drone_path.get(k) is not None:
            found = _norm(drone_path.get(k))
            if found:
                break

    # If not found, inspect flightPath waypoints and their metadata
    if not found:
        fp = drone_path.get('flightPath') or []
        for wp in fp:
            if not isinstance(wp, dict):
                continue
            for f in ('deliveredId', 'dispatchId', 'deliveryId', 'delivered'):
                if f in wp and wp.get(f) is not None:
                    try:
                        found.extend(_norm(wp.get(f)))
                    except Exception:
                        pass
            meta = wp.get('meta') or wp.get('metadata') or {}
            if isinstance(meta, dict):
                for f in ('deliveredId', 'dispatchId', 'deliveryId'):
                    if f in meta and meta.get(f) is not None:
                        try:
                            found.extend(_norm(meta.get(f)))
                        except Exception:
                            pass

    # Deduplicate while preserving order
    seen = set()
    out = []
    for i in found:
        try:
            ii = int(i)
        except Exception:
            continue
        if ii not in seen:
            seen.add(ii)
            out.append(ii)
    return out


def _normalize_drone_for_table(d: dict):
    """Return a flat dict with selected columns for table display.

    Columns: name, id, capacity, cooling, costFinal, costInitial, costPerMove, heating, maxMoves
    """
    if not isinstance(d, dict):
        return {'name': d, 'id': d, 'capacity': None, 'cooling': None, 'costFinal': None, 'costInitial': None, 'costPerMove': None, 'heating': None, 'maxMoves': None}

    name = d.get('name') or d.get('displayName') or d.get('id')
    did = d.get('id') or d.get('droneId') or (name if isinstance(name, (str, int)) else None)

    # capability may be dict or JSON string
    cap = d.get('capability') or d.get('capabilities') or {}
    if isinstance(cap, str):
        try:
            cap = json.loads(cap)
        except Exception:
            cap = {}

    # costs may live at top-level or under capability
    cost_final = d.get('costFinal') or d.get('cost_final') or (cap.get('costFinal') if isinstance(cap, dict) else None)
    cost_initial = d.get('costInitial') or d.get('cost_initial') or (cap.get('costInitial') if isinstance(cap, dict) else None)
    cost_per_move = d.get('costPerMove') or d.get('cost_per_move') or (cap.get('costPerMove') if isinstance(cap, dict) else None)

    return {
        'name': name,
        'id': did,
        'capacity': cap.get('capacity') if isinstance(cap, dict) else None,
        'cooling': cap.get('cooling') if isinstance(cap, dict) else None,
        'costFinal': cost_final,
        'costInitial': cost_initial,
        'costPerMove': cost_per_move,
        'heating': cap.get('heating') if isinstance(cap, dict) else None,
        'maxMoves': cap.get('maxMoves') or cap.get('max_moves') or cap.get('maxMove') if isinstance(cap, dict) else None,
    }


def create_plan(scenario_json: dict):
    """Send plan request to API."""
    try:
        response = requests.post(
            f"{API_BASE}/plan",
            json=scenario_json,
            timeout=30
        )
        try:
            response.raise_for_status()
        except requests.exceptions.HTTPError:
            # surface response body for easier debugging in the UI
            try:
                body = response.text
            except Exception:
                body = '<unreadable response body>'
            st.error(f"Planning failed: HTTP {response.status_code} - {body}")
            return None
        try:
            return response.json()
        except Exception:
            st.error("Planning failed: could not decode JSON response from API")
            return None
    except Exception as e:
        st.error(f"Planning failed: {e}")
        return None


def create_plan_debug(scenario_json: dict):
    """Send plan request to API and show raw response for debugging."""
    try:
        resp = requests.post(f"{API_BASE}/plan", json=scenario_json, timeout=30)
    except Exception as e:
        st.error(f"Planning request failed: {e}")
        return None

    # Show debug info in an expander so users can see raw response
    with st.expander("Debug: /plan response", expanded=True):
        st.write(f"Status: {resp.status_code}")
        try:
            st.write(resp.text)
        except Exception:
            st.write("<could not read response body>")

    if not resp.ok:
        st.error(f"Planning failed: HTTP {resp.status_code}")
        return None

    try:
        return resp.json()
    except Exception:
        st.error("Planning failed: could not decode JSON response from API")
        return None


def what_if_compare(dispatches: list, strategy_a: str, strategy_b: str):
    """Run what-if comparison."""
    try:
        request = {
            "dispatches": dispatches,
            "strategyA": strategy_a,
            "strategyB": strategy_b
        }
        response = requests.post(
            f"{API_BASE}/what-if",
            json=request,
            timeout=30
        )
        response.raise_for_status()
        return response.json()
    except Exception as e:
        st.error(f"What-if analysis failed: {e}")
        return None


def calculate_haversine_distance(lat1: float, lng1: float, lat2: float, lng2: float) -> float:
    """
    Calculate the great circle distance in meters between two points on Earth.
    Uses the Haversine formula.
    """
    from math import radians, cos, sin, asin, sqrt

    # Convert decimal degrees to radians
    lng1, lat1, lng2, lat2 = map(radians, [lng1, lat1, lng2, lat2])

    # Haversine formula
    dlng = lng2 - lng1
    dlat = lat2 - lat1
    a = sin(dlat / 2) ** 2 + cos(lat1) * cos(lat2) * sin(dlng / 2) ** 2
    c = 2 * asin(sqrt(a))
    r = 6371000  # Radius of Earth in meters
    return c * r


def get_known_location_proximity(lat: float, lng: float) -> tuple:
    """
    Check proximity to known locations and return nearest match with distance.
    Returns (location_name, distance_meters) or (None, None) if no close match.
    """
    known_locations = {
        'Appleton Tower': {'lng': -3.1873, 'lat': 55.9445},
        'Royal Infirmary': {'lng': -3.1883, 'lat': 55.9217},
        'Western General': {'lng': -3.2416, 'lat': 55.9642},
        "St John's Hospital": {'lng': -3.5064, 'lat': 55.9348},
        'Sick Kids Hospital': {'lng': -3.2029, 'lat': 55.9233}
    }

    nearest_name = None
    nearest_dist = float('inf')

    for name, coords in known_locations.items():
        dist = calculate_haversine_distance(lat, lng, coords['lat'], coords['lng'])
        if dist < nearest_dist:
            nearest_dist = dist
            nearest_name = name

    # Only return if within 500m (reasonable proximity)
    if nearest_dist < 500:
        return nearest_name, nearest_dist
    return None, None


def extract_move_count_from_drone_path(drone_path: dict) -> int:
    """
    Extract the number of moves from a drone path.
    Tries multiple fields: moves, moveCount, or length of flightPath.
    """
    # Try explicit move count fields first
    if 'moves' in drone_path and drone_path['moves'] is not None:
        return int(drone_path['moves'])
    if 'moveCount' in drone_path and drone_path['moveCount'] is not None:
        return int(drone_path['moveCount'])

    # Fall back to counting flight path waypoints
    flight_path = drone_path.get('flightPath', [])
    return len(flight_path) if flight_path else 0


# Simulation session has been removed from this UI. No registration occurs.


def load_restricted_areas() -> dict:
    """
    Load restricted areas GeoJSON from the project source directory.

    Returns:
        GeoJSON FeatureCollection of restricted areas or None if not found
    """
    try:
        # Path to restricted areas file (in src directory)
        restricted_path = os.path.join(os.path.dirname(__file__), '..', 'src', 'restricted areas map.geojson')

        if os.path.exists(restricted_path):
            with open(restricted_path, 'r', encoding='utf-8') as f:
                return json.load(f)
        return None
    except Exception as e:
        return None


def fetch_geojson_for_dispatches(dispatches: list, cw2_base_url: str = None) -> dict:
    """
    Fetch GeoJSON representation of drone paths for given dispatches.

    Args:
        dispatches: List of dispatch dictionaries
        cw2_base_url: Base URL for CW2 backend (default from env or localhost:8080)

    Returns:
        GeoJSON FeatureCollection or None if request fails
    """
    if cw2_base_url is None:
        cw2_base_url = os.getenv("CW2_BASE_URL", "http://localhost:8080")

    try:
        # Use multi-drone GeoJSON endpoint (one LineString per assigned drone)
        url = f"{cw2_base_url}/api/v1/calcMultiDroneDeliveryPathAsGeoJson"
        # include optional strategy if provided in session (keep backward-compatible)
        params = {}
        strategy = st.session_state.get('last_scenario', {}).get('strategy') if 'last_scenario' in st.session_state and st.session_state.last_scenario else None
        if strategy:
            url = f"{url}?strategy={strategy}"
        response = requests.post(url, json=dispatches, timeout=30)
        response.raise_for_status()
        return response.json()
    except Exception as e:
        st.error(f"Failed to fetch GeoJSON: {e}")
        return None


def save_geojson_to_disk(geojson: dict, label: str = None) -> Optional[str]:
    """
    Save a GeoJSON dict to disk under `data/geojsons` with a timestamped filename.
    Returns the file path or None on failure.
    """
    try:
        base_dir = os.path.join(os.path.dirname(__file__), '..', 'data', 'geojsons')
        os.makedirs(base_dir, exist_ok=True)
        ts = datetime.utcnow().strftime('%Y%m%dT%H%M%SZ')
        safe_label = (label or 'geo').replace(' ', '_')
        fname = f"{safe_label}_{ts}.geojson"
        fpath = os.path.join(base_dir, fname)
        with open(fpath, 'w', encoding='utf-8') as f:
            json.dump(geojson, f, ensure_ascii=False, indent=2)
        return fpath
    except Exception:
        return None


def display_drone_path_map(geojson: dict, plan: dict = None):
    """
    Display an interactive map of drone flight paths using pydeck.

    Args:
        geojson: GeoJSON FeatureCollection containing drone paths
        plan: Optional plan dict for additional context
    """
    if not geojson or not geojson.get('features'):
        st.warning("No path data available for visualization")
        return

    try:
        # Extract all coordinates from GeoJSON features and build one path per feature
        all_coords = []
        path_data = []

        for feature in geojson['features']:
            if feature.get('type') != 'Feature':
                continue
            geometry = feature.get('geometry', {})
            properties = feature.get('properties', {})

            if geometry.get('type') != 'LineString':
                continue
            coords = geometry.get('coordinates') or []
            if not coords:
                continue

            # collect coords for centering (as [lat, lng])
            for c in coords:
                all_coords.append([c[1], c[0]])

            # full path as list of [lng, lat]
            full_path = [[p[0], p[1]] for p in coords]

            drone_id = properties.get('droneId') or properties.get('drone_id') or properties.get('drone') or properties.get('id')
            drone_display = str(drone_id) if drone_id is not None else 'unknown'
            color = _get_drone_color(drone_id)

            # Extract delivered IDs robustly from feature properties (use existing helper)
            try:
                delivered_ids = _extract_delivered_ids(properties)
            except Exception:
                delivered_ids = []

            deliveries_str = ','.join(map(str, delivered_ids)) if delivered_ids else 'none'

            # Extract dispatch IDs from plan data (similar to Drone Assignments section)
            dispatch_ids = []
            if plan and plan.get('dronePaths'):
                for drone_path in plan['dronePaths']:
                    if drone_path.get('droneId') == drone_id:
                        # First, try to get from deliveredIds at the drone path level
                        delivered_ids_direct = drone_path.get('deliveredIds') or drone_path.get('deliveredId')
                        if delivered_ids_direct:
                            if isinstance(delivered_ids_direct, list):
                                for did in delivered_ids_direct:
                                    try:
                                        dispatch_ids.append(int(did))
                                    except (ValueError, TypeError):
                                        pass
                            else:
                                try:
                                    dispatch_ids.append(int(delivered_ids_direct))
                                except (ValueError, TypeError):
                                    pass

                        # Also check flightPath waypoints for dispatch IDs
                        if drone_path.get('flightPath'):
                            for wp in drone_path['flightPath']:
                                if isinstance(wp, dict):
                                    meta = wp.get('meta') or wp.get('metadata') or {}
                                    dispatch_id = meta.get('deliveredId') or meta.get('dispatchId')
                                    if dispatch_id is not None:
                                        try:
                                            dispatch_ids.append(int(dispatch_id))
                                        except (ValueError, TypeError):
                                            pass
                        break

            # Deduplicate dispatch IDs while preserving order
            seen_dispatch = set()
            unique_dispatch_ids = []
            for did in dispatch_ids:
                if did not in seen_dispatch:
                    seen_dispatch.add(did)
                    unique_dispatch_ids.append(did)

            dispatch_display = ','.join(map(str, unique_dispatch_ids)) if unique_dispatch_ids else 'none'

            path_data.append({
                'path': full_path,
                'drone_id': drone_id,
                'drone_display': drone_display,
                'dispatch_id': unique_dispatch_ids[0] if unique_dispatch_ids else None,
                'dispatch_display': dispatch_display,
                'deliveries': deliveries_str,
                'color': color
            })
            # capture start point as potential service point
            try:
                start_lng, start_lat = coords[0][0], coords[0][1]
                # record start as tuple for later dedupe
                if 'start_points' not in locals():
                    start_points = []
                start_points.append({'lat': start_lat, 'lng': start_lng})
            except Exception:
                pass

        if not all_coords:
            st.warning("No valid coordinates found in GeoJSON")
            return

        # Calculate center and zoom
        lats = [c[0] for c in all_coords]
        lngs = [c[1] for c in all_coords]
        center_lat = sum(lats) / len(lats)
        center_lng = sum(lngs) / len(lngs)

        # Create DataFrame for path layer
        df_paths = pd.DataFrame(path_data)

        # Create pydeck layers
        path_layer = pdk.Layer(
            'PathLayer',
            data=df_paths,
            get_path='path',
            get_color='color',
            width_scale=20,
            width_min_pixels=2,
            pickable=True,
            auto_highlight=True
        )

        # Extract delivery points (endpoints)
        delivery_points = []
        seen_dp = set()
        for feature in geojson['features']:
            if feature.get('type') != 'Feature':
                continue
            geometry = feature.get('geometry', {})
            properties = feature.get('properties', {})
            if geometry.get('type') != 'LineString':
                continue
            coords = geometry.get('coordinates') or []
            if not coords:
                continue
            lng, lat = coords[-1][0], coords[-1][1]
            key = f"{lat:.6f}|{lng:.6f}" if isinstance(lat, float) and isinstance(lng, float) else f"{lat}|{lng}"
            if key in seen_dp:
                continue
            seen_dp.add(key)
            delivery_points.append({
                'lat': lat,
                'lng': lng,
                'drone_id': properties.get('droneId', 'unknown'),
                'dispatch_id': properties.get('dispatchId', properties.get('deliveredId', 'unknown'))
            })

        df_points = pd.DataFrame(delivery_points) if delivery_points else pd.DataFrame()

        layers = []

        # Load and add restricted areas layer (first, so it appears underneath)
        restricted_areas = load_restricted_areas()
        if restricted_areas and restricted_areas.get('features'):
            # Extract polygon features
            restricted_polygons = []
            for feature in restricted_areas['features']:
                if feature.get('geometry', {}).get('type') == 'Polygon':
                    # Extract polygon coordinates
                    coords = feature['geometry']['coordinates'][0]  # First ring (outer boundary)
                    # Convert to [lng, lat] format for pydeck
                    polygon_coords = [[lng, lat] for lng, lat in coords]

                    restricted_polygons.append({
                        'polygon': polygon_coords,
                        'name': feature.get('properties', {}).get('name', 'Restricted Area')
                    })

            if restricted_polygons:
                df_restricted = pd.DataFrame(restricted_polygons)

                # Create PolygonLayer for restricted areas
                restricted_layer = pdk.Layer(
                    'PolygonLayer',
                    data=df_restricted,
                    get_polygon='polygon',
                    get_fill_color='[255, 100, 100, 80]',  # Semi-transparent red fill
                    get_line_color='[200, 0, 0, 200]',     # Dark red outline
                    line_width_min_pixels=2,
                    pickable=True,
                    auto_highlight=True
                )
                layers.append(restricted_layer)

        # Add path layer
        layers.append(path_layer)

        # Add text labels for each path at its midpoint (dispatch id / drone id)
        text_labels = []
        for feature in geojson['features']:
            try:
                if feature.get('type') != 'Feature':
                    continue
                geom = feature.get('geometry', {})
                props = feature.get('properties', {})
                if geom.get('type') != 'LineString':
                    continue
                coords = geom.get('coordinates') or []
                if not coords:
                    continue
                mid = coords[len(coords) // 2]
                    # Delivery point text labels removed per user request; only point markers are shown.
            except Exception:
                continue

        df_text = pd.DataFrame(text_labels) if text_labels else pd.DataFrame()
        if not df_text.empty:
            text_layer = pdk.Layer(
                "TextLayer",
                data=df_text,
                get_position="coordinates",
                get_text="text",
                get_size=10,
                get_color=[255, 255, 255],
                get_angle=0,
                get_text_anchor='middle',
                get_alignment_baseline='center',
                get_background_color=[0, 0, 0, 180],
                background=True,
                background_padding=[3, 1, 3, 1]
            )
            layers.append(text_layer)

        # Add delivery point markers (cyan/blue with white center)
        if not df_points.empty:
            # Outer cyan circle
            point_layer_outer = pdk.Layer(
                'ScatterplotLayer',
                data=df_points,
                get_position='[lng, lat]',
                get_color='[0, 200, 255, 240]',  # Bright cyan/blue
                get_radius=80,
                pickable=True,
                auto_highlight=True
            )
            layers.append(point_layer_outer)

            # Inner white circle for contrast
            point_layer_inner = pdk.Layer(
                'ScatterplotLayer',
                data=df_points,
                get_position='[lng, lat]',
                get_color='[255, 255, 255, 255]',  # White center
                get_radius=35,
                pickable=True,
                auto_highlight=True
            )
            layers.append(point_layer_inner)

        # Service points: Load from restricted areas GeoJSON (Appleton Tower, Ocean Terminal)
        service_points = []

        # First, load fixed service points from restricted areas map
        restricted_areas = load_restricted_areas()
        if restricted_areas and restricted_areas.get('features'):
            for feature in restricted_areas['features']:
                if feature.get('geometry', {}).get('type') == 'Point':
                    coords = feature['geometry'].get('coordinates', [])
                    props = feature.get('properties', {})
                    if len(coords) >= 2:
                        service_points.append({
                            'id': props.get('id'),
                            'name': props.get('name', 'Service Point'),
                            'lat': float(coords[1]),
                            'lng': float(coords[0])
                        })

        # Also try to load from fleet_data in session_state (if available)
        fleet = st.session_state.get('fleet_data') or {}
        sp_candidates = fleet.get('servicePoints') or fleet.get('service_points') or fleet.get('servicePointList') or []
        if isinstance(sp_candidates, list) and sp_candidates:
            for sp in sp_candidates:
                try:
                    lat = sp.get('lat') or sp.get('latitude')
                    lng = sp.get('lng') or sp.get('lon') or sp.get('longitude')
                    sid = sp.get('id') or sp.get('servicePointId') or sp.get('name')
                    if lat is None or lng is None:
                        continue
                    # Avoid duplicates
                    already_exists = any(
                        abs(float(existing.get('lat', 0)) - float(lat)) < 1e-6 and
                        abs(float(existing.get('lng', 0)) - float(lng)) < 1e-6
                        for existing in service_points
                    )
                    if not already_exists:
                        service_points.append({'id': sid, 'name': str(sid), 'lat': float(lat), 'lng': float(lng)})
                except Exception:
                    continue

        # fallback: try plan object for service points
        if not service_points and plan:
            sp_plan = plan.get('servicePoints') or plan.get('service_points') or []
            if isinstance(sp_plan, list):
                for sp in sp_plan:
                    try:
                        lat = sp.get('lat') or sp.get('latitude')
                        lng = sp.get('lng') or sp.get('lon') or sp.get('longitude')
                        sid = sp.get('id') or sp.get('servicePointId') or sp.get('name')
                        if lat is None or lng is None:
                            continue
                        service_points.append({'id': sid, 'lat': float(lat), 'lng': float(lng)})
                    except Exception:
                        continue
        
            # also include start points extracted from geojson features as service points (if not already present)
            try:
                if 'start_points' in locals():
                    for sp in start_points:
                        try:
                            lat = float(sp.get('lat'))
                            lng = float(sp.get('lng'))
                            # avoid duplicates by coordinate
                            already = False
                            for exist in service_points:
                                try:
                                    if abs(float(exist['lat']) - lat) < 1e-6 and abs(float(exist['lng']) - lng) < 1e-6:
                                        already = True
                                        break
                                except Exception:
                                    continue
                            if not already:
                                service_points.append({'id': None, 'lat': lat, 'lng': lng})
                        except Exception:
                            continue
            except Exception:
                pass
        df_service = pd.DataFrame(service_points) if service_points else pd.DataFrame()
        if not df_service.empty:
            # Service points: Orange circle only, no text labels
            service_layer = pdk.Layer(
                'ScatterplotLayer',
                data=df_service,
                get_position='[lng, lat]',
                get_color='[255, 140, 0, 255]',  # Bright orange
                get_radius=70,  # Slightly larger for prominence
                pickable=True,
                auto_highlight=True,
                get_line_color=[0, 0, 0, 255],  # Black outline
                line_width_min_pixels=2,
                stroked=True
            )
            layers.append(service_layer)

        # Create view state
        view_state = pdk.ViewState(
            latitude=center_lat,
            longitude=center_lng,
            zoom=11,
            pitch=45,
            bearing=0
        )

        # Render map
        deck = pdk.Deck(
            layers=layers,
            initial_view_state=view_state,
            tooltip={
                'html': '<b>Drone {drone_display}</b><br/>Dispatch/delivery no.: {dispatch_display}',
                'style': {'color': 'white'}
            },
            map_style='https://basemaps.cartocdn.com/gl/positron-gl-style/style.json'
        )

        st.pydeck_chart(deck)

        # Add legend
        with st.expander("🗺️ Map Legend", expanded=True):
            st.markdown("""
            - **Colored Lines with Labels**: Drone flight paths showing dispatch IDs and drone numbers
            - **Cyan/Blue Circles with White Centers**: Delivery destinations
            - **Orange Circles with Black Outline**: Service points (Appleton Tower, Ocean Terminal)
            - **Pink/Red Shaded Areas**: Restricted no-fly zones (drones avoid these areas)
            - **Interactive**: Hover over paths and areas to see details
            """)

            if plan:
                st.write(f"**Total Drones**: {len(plan.get('dronePaths', []))}")
                st.write(f"**Total Cost**: £{plan.get('totalCost', 0):.2f}")
                st.write(f"**Total Moves**: {plan.get('totalMoves', 0)}")

    except Exception as e:
        st.error(f"Map visualization error: {e}")
        # Fallback to simple coordinate display
        st.write("GeoJSON data:")
        st.json(geojson)


def _get_drone_color(drone_id):
    """Get a unique color for each drone ID."""
    # Color palette for up to 10 drones
    colors = [
        [0, 119, 182, 200],   # Blue
        [255, 127, 0, 200],   # Orange
        [44, 160, 44, 200],   # Green
        [214, 39, 40, 200],   # Red
        [148, 103, 189, 200], # Purple
        [140, 86, 75, 200],   # Brown
        [227, 119, 194, 200], # Pink
        [127, 127, 127, 200], # Gray
        [188, 189, 34, 200],  # Olive
        [23, 190, 207, 200]   # Cyan
    ]

    try:
        # Accept numeric strings as ints too
        if isinstance(drone_id, str) and drone_id.isdigit():
            idx = int(drone_id)
            return colors[idx % len(colors)]
        if isinstance(drone_id, int):
            return colors[drone_id % len(colors)]
        # Fallback: hash the string representation to choose a color
        key = str(drone_id)
        h = 0
        for ch in key:
            h = (h * 31 + ord(ch)) & 0xFFFFFFFF
        return colors[h % len(colors)]
    except Exception:
        return colors[0]


def display_plan_result(plan):
    """Display plan results in a nice format."""
    if not plan or not plan.get("dronePaths"):
        st.warning("No plan generated. Check dispatch requirements.")
        return

    # Compute drone and delivery counts for a client-side summary (defensive)
    drone_count = len(plan.get('dronePaths', []))

    # Metrics
    # Compute delivery count defensively: prefer backend deliveredIds, else
    # fall back to the parsed scenario dispatch count stored in session state.
    delivered_count = 0
    try:
        delivered_count = sum(len(_extract_delivered_ids(dp)) for dp in plan.get('dronePaths', []))
    except Exception:
        delivered_count = 0

    # Fallback to session scenario dispatch count when available
    try:
        if delivered_count == 0 and 'last_scenario' in st.session_state and st.session_state.last_scenario:
            parsed = st.session_state.last_scenario
            if isinstance(parsed, dict) and 'dispatches' in parsed:
                delivered_count = len(parsed['dispatches'])
    except Exception:
        pass

    # Synthesize a client-side summary so the UI reflects parsed dispatch count
    try:
        summary_str = (
            f"Plan complete: {drone_count} drone(s) assigned, "
            f"{delivered_count} delivery(ies), "
            f"{plan.get('totalMoves', 0)} total moves, "
            f"cost: {plan.get('totalCost', 0.0):.2f}"
        )
    except Exception:
        summary_str = plan.get("summary", "Plan generated successfully")

    st.success(summary_str)

    col1, col2, col3, col4 = st.columns(4)
    with col1:
        st.metric("Total Cost", f"£{plan['totalCost']:.2f}")
    with col2:
        st.metric("Total Moves", plan['totalMoves'])
    with col3:
        st.metric("Drones Used", len(plan['dronePaths']))
    with col4:
        st.metric("Deliveries", delivered_count)

    # Map Visualization
    st.subheader("🗺️ Flight Path Visualization")

    # Try to fetch and display the map
    if 'last_scenario' in st.session_state and st.session_state.last_scenario:
        dispatches = st.session_state.last_scenario.get('dispatches', [])
        if dispatches:
            # Split layout: map wide on the left, requested dispatches JSON on the right
            map_col, json_col = st.columns([3, 1])

            with map_col:
                with st.spinner("Loading map..."):
                    geojson = fetch_geojson_for_dispatches(dispatches)
                    if geojson:
                        # Debug: show raw GeoJSON to help diagnose single-drone vs multi-drone responses
                        with st.expander("Debug: raw GeoJSON response", expanded=True):
                            try:
                                st.write(f"Features: {len(geojson.get('features', []))}")
                                # list droneIds found in features
                                drone_ids = [f.get('properties', {}).get('droneId') or f.get('properties', {}).get('drone_id') for f in geojson.get('features', [])]
                                st.write(f"Drone IDs in GeoJSON: {drone_ids}")
                                st.json(geojson)
                            except Exception:
                                st.write(geojson)

                        # Save GeoJSON to disk for later inspection and history
                        saved_path = save_geojson_to_disk(geojson, label='plan')
                        entry = {'path': saved_path or '<unsaved>', 'timestamp': datetime.utcnow().isoformat(), 'label': os.path.basename(saved_path) if saved_path else 'unsaved'}
                        st.session_state.saved_geojsons.insert(0, entry)

                        # Render the interactive map
                        display_drone_path_map(geojson, plan)

                        # Provide quick access to saved geojson files
                        if st.session_state.saved_geojsons:
                            with st.expander('Saved GeoJSONs', expanded=False):
                                for s in st.session_state.saved_geojsons[:10]:
                                    try:
                                        st.write(f"{s.get('timestamp')}: {s.get('label')} — {s.get('path')}")
                                    except Exception:
                                        st.write(s)
                    else:
                        st.info("Map visualization unavailable (GeoJSON endpoint not responding)")

            with json_col:
                st.subheader("Requested Dispatches")
                # Show a human-friendly list rather than raw JSON so it's easier to inspect
                try:
                    if not dispatches:
                        st.write("No dispatches")
                    else:
                        for idx, d in enumerate(dispatches, start=1):
                            # Prefer canonical id fields, but show as 'Delivery id' for clarity
                            did = d.get('id') or d.get('deliveryId') or d.get('dispatchId') or None

                            # Try to summarize location/amount/cooling for quick scanning
                            delivery = d.get('delivery') if isinstance(d.get('delivery'), dict) else d.get('delivery') or {}
                            addr = delivery.get('address') or delivery.get('addr') or delivery.get('location') or None
                            lat = delivery.get('lat') or delivery.get('latitude') or None
                            lng = delivery.get('lng') or delivery.get('lon') or delivery.get('longitude') or None
                            vol = delivery.get('volume') or delivery.get('qty') or d.get('volume') or None
                            cooling = delivery.get('cooling') if isinstance(delivery, dict) else d.get('cooling')

                            with st.container():
                                st.markdown(f"**Delivery id:** {did if did is not None else '(unknown)'}")
                                # One-line summary
                                summary_parts = []
                                if addr:
                                    summary_parts.append(f"Address: `{addr}`")
                                elif lat is not None and lng is not None:
                                    try:
                                        summary_parts.append(f"Location: `{float(lat):.5f}, {float(lng):.5f}`")
                                    except Exception:
                                        summary_parts.append(f"Location: `{lat}, {lng}`")
                                if vol:
                                    summary_parts.append(f"Volume: {vol}")
                                if cooling is not None:
                                    summary_parts.append(f"Cooling: {cooling}")
                                if summary_parts:
                                    st.markdown("- " + " • ".join(summary_parts))
                                # Provide a compact view of the rest of the dispatch for inspection
                                try:
                                    st.code(json.dumps(d, indent=2, ensure_ascii=False), language='json')
                                except Exception:
                                    st.write(d)
                                # Small spacer between dispatches
                                st.write("")
                except Exception:
                    # Fallback: show raw JSON if structured rendering fails
                    try:
                        st.code(json.dumps(dispatches, indent=2, ensure_ascii=False), language='json')
                    except Exception:
                        st.write(dispatches)
        else:
            st.info("No dispatches available for map visualization")
    else:
        st.info("Map visualization requires scenario data")

    st.markdown("---")

    # Drone assignment details
    st.subheader("Drone Assignments")
    for drone_path in plan['dronePaths']:
        with st.expander(f"Drone {drone_path['droneId']}"):
            # Show deliveries or '(none)'
            delivered = _extract_delivered_ids(drone_path)
            if delivered:
                st.write(f"**Deliveries ({len(delivered)}):** {', '.join(map(str, delivered))}")
            else:
                st.write("**Deliveries:** (none)")
            # st.write(f"**Waypoints:** {len(drone_path.get('flightPath') or [])}")

            # Fetch drone capability from API to show true maxMoves.
            # Try the Python proxy first, then fall back to the Java CW2 service directly
            try:
                drone_info = None
                # Try Python proxy (/cw2/drone/{id}) first
                try:
                    resp = requests.get(f"{API_BASE}/cw2/drone/{drone_path['droneId']}", timeout=5)
                except Exception:
                    resp = None

                if resp is not None and resp.ok:
                    try:
                        drone_info = resp.json()
                    except Exception:
                        drone_info = None
                else:
                    # Proxy failed or returned non-200: try the Java CW2 backend directly
                    cw2_base = os.getenv("CW2_ENDPOINT", "http://localhost:8080")
                    if cw2_base.endswith("/"):
                        cw2_base = cw2_base[:-1]
                    try:
                        resp2 = requests.get(f"{cw2_base}/api/v1/droneDetails/{drone_path['droneId']}", timeout=5)
                        if resp2.ok:
                            try:
                                drone_info = resp2.json()
                            except Exception:
                                drone_info = None
                    except Exception:
                        drone_info = None

                if drone_info:
                    cap = drone_info.get('capability') or {}
                    max_moves = cap.get('maxMoves')
                    st.write(f"**Capability - maxMoves:** {max_moves}")
                else:
                    st.write("**Capability - maxMoves:** (unavailable)")
            except Exception as e:
                st.write("**Capability - maxMoves:** (error fetching)")

            # Show delivery destination proximity to known locations
            if drone_path.get('flightPath'):
                # Extract delivery waypoints (those with deliveredId in metadata)
                delivery_waypoints = []
                for wp in drone_path['flightPath']:
                    if isinstance(wp, dict):
                        meta = wp.get('meta') or wp.get('metadata') or {}
                        if meta.get('deliveredId') or meta.get('dispatchId'):
                            pos = wp.get('position', {})
                            if pos.get('lat') and pos.get('lng'):
                                delivery_waypoints.append((
                                    meta.get('deliveredId') or meta.get('dispatchId'),
                                    pos['lat'],
                                    pos['lng']
                                ))

                if delivery_waypoints:
                    st.write("**Delivery Locations:**")
                    for dispatch_id, lat, lng in delivery_waypoints:
                        location_name, distance = get_known_location_proximity(lat, lng)
                        if location_name:
                            st.write(f"  • Dispatch #{dispatch_id}: **{location_name}** ({distance:.0f}m away)")
                        else:
                            st.write(f"  • Dispatch #{dispatch_id}: ({lat:.4f}, {lng:.4f})")


def main():
    """Main application."""
    init_session_state()
    display_header()

    # Sidebar for mode selection
    with st.sidebar:
        st.header("Navigation")
        mode = st.radio(
            "Select Mode",
            ["Natural Language Planning", "Fleet Query", "What-If Analysis", "Manual JSON"]
        )

        st.divider()
        st.subheader("Quick Info")
        st.info("""
        **Frequently Dispatched (from/to) Locations:**
        - Appleton Tower
        - Royal Infirmary
        - Western General
        - Sick Kids Hospital
        """)

    # Main content area
    if mode == "Natural Language Planning":
        st.header("Natural Language Planning")

        # Add info banner about Hypothetical Scenario Planner
        st.info("💡 **Hypothetical Scenario Planner**: Stack multiple delivery requests to simulate fleet performance without committing to permanent scheduling. Perfect for risk-free optimization!")

        # Show scenario queue if it has items
        if st.session_state.scenario_queue:
            st.subheader(f"📋 Queued Scenarios ({len(st.session_state.scenario_queue)})")

            queue_cols = st.columns([3, 1])
            with queue_cols[0]:
                for item in st.session_state.scenario_queue:
                    with st.expander(f"#{item['id']}: {item['user_text'][:60]}... (added at {item['timestamp']})"):
                        st.json(item['scenario'])

            with queue_cols[1]:
                if st.button("Clear Queue", key="clear_queue"):
                    clear_scenario_queue()
                    st.rerun()

                st.write("")  # spacing

                # Strategy selector for combined planning
                queue_strategy = st.selectbox(
                    "Strategy for Queue",
                    ["min_cost", "min_moves", "balanced"],
                    key="queue_strategy"
                )

                if st.button("Plan All Queued", type="primary", key="plan_queue"):
                    combined = merge_queued_scenarios(strategy=queue_strategy)
                    if combined:
                        with st.spinner("Creating combined delivery plan..."):
                            plan = create_plan(combined)
                        if plan:
                            st.session_state.last_plan = plan
                            st.session_state.last_scenario = combined
                            st.success(f"✅ Planned {len(combined['dispatches'])} deliveries from {len(st.session_state.scenario_queue)} scenarios!")
                            # Clear queue after successful planning
                            clear_scenario_queue()
                            st.rerun()

            st.markdown("---")

        # If a plan was previously created and stored in session, show it prominently
        if 'last_plan' in st.session_state and st.session_state.last_plan:
            st.subheader("Last Generated Plan")
            display_plan_result(st.session_state.last_plan)
            st.markdown("---")

        # (Clearing cached last plan removed — avoid runtime errors in older Streamlit versions)

        if not st.session_state.llm_enabled:
            st.error("Natural language parsing requires GEMINI_API_KEY. Please set it in .env file.")
            return

        st.subheader("➕ Add New Scenario")
        st.write("Describe your delivery scenario in plain English:")

        # Example prompts
        with st.expander("Example Prompts"):
            st.code("""
Examples:
1. "Deliver 3 liters of cooled insulin to Royal Infirmary as cheaply as possible"
2. "Send 5L of heated medicine to Western General, minimize moves"
3. "Urgent delivery: 2L to Sick Kids Hospital with cooling, max cost 30"
            """)

        # Text input
        user_text = st.text_area(
            "Scenario Description",
            height=100,
            placeholder="E.g., Deliver 3L cooled medicine to Royal Infirmary, minimize cost"
        )

        st.warning(
                    "Some dispatches will be parsed from place names or non-exact inputs "
                    "(e.g., 'Appleton Tower'). Geocoding by place names may be approximate (and so, might be a few km/miles off) — if "
                    "precision matters, provide exact latitude/longitude coordinates or verify the locations before creating the plan."
                )
        
        if st.button("Generate Plan", type="primary"):
            if not user_text:
                st.warning("Please enter a scenario description")
                return
            with st.spinner("Parsing your request..."):
                scenario = st.session_state.llm_parser.parse_to_scenario(user_text)

            if not scenario:
                st.error("Failed to parse your request. Please try rephrasing.")
                return

            # Store parsed scenario temporarily and ask for confirmation before planning
            parsed_json = scenario.model_dump(exclude_none=True)
            st.session_state.parsed_scenario = parsed_json
            st.session_state.parsed_user_text = user_text
            st.session_state.confirm_needed = True
            st.session_state.parsed_warnings = scenario._warnings if hasattr(scenario, "_warnings") else []

        # Show confirmation UI if needed (outside button handler so it persists across reruns)
        if st.session_state.get('confirm_needed', False):
            # Show parsed scenario and any warnings
            st.success("Scenario parsed successfully! Review and confirm before planning.")
            with st.expander("Parsed Scenario (JSON)", expanded=True):
                st.json(st.session_state.parsed_scenario)

            # Disclaimer when dispatches use place names or non-exact location inputs
            def _is_exact_coord(d: dict) -> bool:
                try:
                    delv = d.get('delivery', {}) if isinstance(d.get('delivery'), dict) else {}
                    lng = delv.get('lng') or delv.get('lon') or delv.get('longitude')
                    lat = delv.get('lat') or delv.get('latitude')
                    if lng is None or lat is None:
                        return False
                    try:
                        float(lng); float(lat)
                        return True
                    except Exception:
                        return False
                except Exception:
                    return False
            
            # non_exact = [
            #         d for d in st.session_state.parsed_scenario.get('dispatches', [])
            #         if not _is_exact_coord(d)
            #     ]

            # if non_exact:
            #     st.warning(
            #         "Some dispatches were parsed from place names or non-exact inputs "
            #         "(e.g., 'Appleton Tower'). Geocoding may be approximate — if "
            #         "precision matters, provide exact latitude/longitude coordinates "
            #         "or verify the locations before creating the plan."
            #     )
            

            if st.session_state.get('parsed_warnings'):
                import re
                # Hide noisy/opaque datetime-adjust messages created by the parser
                visible_warnings = [
                    w for w in st.session_state.parsed_warnings
                    if not re.search(r"datetime adjusted|adjusted from", str(w), re.IGNORECASE)
                ]
                if visible_warnings:
                    st.markdown("**Parser adjustments:**")
                    for w in visible_warnings:
                        st.warning(w)

            # Confirmation UI
            st.markdown("---")
            st.info("**Choose an option:** Add this scenario to the queue for batch planning, or plan it immediately.")
            st.write("**Original text:**", st.session_state.parsed_user_text)

            col1, col2, col3 = st.columns(3)
            with col1:
                if st.button("➕ Add to Queue", key="add_to_queue"):
                    # Add to queue and clear confirmation state
                    add_to_scenario_queue(
                        st.session_state.parsed_user_text,
                        st.session_state.parsed_scenario
                    )
                    st.session_state.confirm_needed = False
                    st.rerun()

            with col2:
                if st.button("🚀 Plan Now", type="primary", key="confirm_create"):
                    # send to planning endpoint
                    with st.spinner("Creating delivery plan..."):
                        plan = create_plan_debug(st.session_state.parsed_scenario)
                    if plan:
                        st.session_state.last_plan = plan
                        st.session_state.last_scenario = st.session_state.parsed_scenario
                        st.session_state.confirm_needed = False
                        st.rerun()
                    else:
                        st.error("Plan creation failed. See messages above for details.")

            with col3:
                if st.button("✏️ Edit", key="edit_desc"):
                    st.session_state.confirm_needed = False
                    st.rerun()

    elif mode == "Fleet Query":
        st.header("Fleet Capabilities Query")

        if st.button("Refresh Fleet Data"):
            with st.spinner("Fetching fleet data..."):
                fleet = get_fleet_summary()
                if fleet:
                    st.session_state.fleet_data = fleet

        if 'fleet_data' in st.session_state:
            fleet = st.session_state.fleet_data

            # Display metrics
            col1, col2, col3, col4 = st.columns(4)
            with col1:
                st.metric("Total Drones", fleet['totalDrones'])
            with col2:
                st.metric("With Cooling", fleet['dronesWithCooling'])
            with col3:
                st.metric("With Heating", fleet['dronesWithHeating'])
            with col4:
                st.metric("Service Points", len(fleet.get('servicePointCounts', {})))

            # Capacity distribution
            st.subheader("Capacity Distribution")
            st.bar_chart(fleet['capacityDistribution'])

            # Service point assignments
            st.subheader("Drones per Service Point")
            st.json(fleet['servicePointCounts'])

            # Natural language query
            if st.session_state.llm_enabled:
                st.divider()
                st.subheader("Ask a Question")
                question = st.text_input(
                    "Question",
                    placeholder="E.g., How many drones can handle cooled deliveries?"
                )
                if st.button("Ask") and question:
                    with st.spinner("Thinking..."):
                        q = question.lower()
                        handled = False
                        # Small debug expander to show how the question was normalized
                        with st.expander('Debug: Ask parsing', expanded=False):
                            st.write({'normalized_question': q})

                        # Quick detection: handle explicit 'show details for drone X' queries first
                        try:
                            import re as _re2
                            early_drone_match = _re2.search(r"drone\s*#?\s*(\d+)", q)
                        except Exception:
                            early_drone_match = None

                        if early_drone_match and ("detail" in q or "show" in q or "info" in q or "capacity" in q or "maxmove" in q or "cost" in q):
                            # handle drone detail request immediately
                            did = early_drone_match.group(1)
                            # fetch drones from fleet summary or API (same logic as below)
                            drones = None
                            for k in ("drones", "droneList", "drone_list", "droneRecords", "droneRecordsList", "drones_list", "fleet"):
                                v = fleet.get(k)
                                if isinstance(v, list):
                                    drones = v
                                    break

                            if drones is None:
                                try:
                                    resp = requests.get(f"{API_BASE}/drones", timeout=6)
                                    if resp.ok:
                                        j = resp.json()
                                        if isinstance(j, list):
                                            drones = j
                                except Exception:
                                    drones = None

                            if drones is None:
                                try:
                                    ilp_base = os.getenv('ILPENDPOINT') or None
                                    if ilp_base:
                                        if ilp_base.endswith('/'):
                                            ilp_base = ilp_base[:-1]
                                        resp2 = requests.get(f"{ilp_base}/drones", timeout=6)
                                        if resp2.ok:
                                            j2 = resp2.json()
                                            if isinstance(j2, list):
                                                drones = j2
                                except Exception:
                                    drones = None

                            found = None
                            if isinstance(drones, list):
                                for d in drones:
                                    try:
                                        if d is None:
                                            continue
                                        for key in ('id', 'droneId', 'name'):
                                            val = d.get(key) if isinstance(d, dict) else None
                                            if val is None:
                                                continue
                                            if str(val) == str(did):
                                                found = d
                                                break
                                            if key == 'name' and isinstance(val, str) and (str(did) in val or f"drone {did}" in val.lower()):
                                                found = d
                                                break
                                        if found:
                                            break
                                    except Exception:
                                        continue

                            if found:
                                out = {
                                    'id': found.get('id'),
                                    'name': found.get('name') or found.get('id'),
                                }
                                cap = found.get('capability') or {}
                                if isinstance(cap, str):
                                    try:
                                        cap = json.loads(cap)
                                    except Exception:
                                        cap = {}
                                out.update({
                                    'capacity': cap.get('capacity'),
                                    'maxMoves': cap.get('maxMoves') or cap.get('max_moves') or cap.get('maxMove'),
                                    'cooling': cap.get('cooling'),
                                    'heating': cap.get('heating'),
                                })
                                out.update({
                                    'costInitial': found.get('costInitial') or cap.get('costInitial') or found.get('cost_initial') or cap.get('cost_initial'),
                                    'costFinal': found.get('costFinal') or cap.get('costFinal') or found.get('cost_final') or cap.get('cost_final'),
                                    'costPerMove': found.get('costPerMove') or cap.get('costPerMove') or found.get('cost_per_move') or cap.get('cost_per_move'),
                                })
                                st.subheader(f"Drone {did} details")
                                try:
                                    st.table([out])
                                except Exception:
                                    st.json(out)
                                handled = True

                        # Service point count queries: e.g. 'how many drones are assigned to service point 1?'
                        try:
                            import re
                            sp_match = re.search(r"service\s*point[s]?\s*(?:#|no\.?|number)?\s*(\d+)", q)
                            if not sp_match:
                                sp_match = re.search(r"service\s*point[s]?\s*(\d+)", q)
                        except Exception:
                            sp_match = None

                        if sp_match:
                            sp_id = sp_match.group(1)

                            # 1) Try explicit assignment mapping in the fleet summary
                            assignments = None
                            for key in ('servicePointAssignments', 'service_point_assignments', 'servicePointToDrones', 'servicePointMapping', 'servicePointDroneIds', 'servicePointToDroneIds', 'servicePointAssignmentsMap'):
                                v = fleet.get(key)
                                if isinstance(v, dict):
                                    assignments = v
                                    break

                            if assignments:
                                lst = assignments.get(sp_id) or assignments.get(int(sp_id) if sp_id.isdigit() else None)
                                if lst is None:
                                    for k, val in assignments.items():
                                        try:
                                            if str(k) == str(sp_id):
                                                lst = val
                                                break
                                        except Exception:
                                            continue

                                # Normalize lst into full drone records if possible
                                records = []
                                if isinstance(lst, list) and lst:
                                    # If elements are already dicts with capability, use them directly
                                    simple = True
                                    for item in lst:
                                        if not isinstance(item, dict):
                                            simple = False
                                            break
                                    if simple:
                                        records = lst
                                    else:
                                        # Need to resolve ids/names to full drone records
                                        # Try to get detailed drone list from fleet or API
                                        drones = None
                                        for k in ("drones", "droneList", "drone_list", "droneRecords", "drones_list"):
                                            v = fleet.get(k)
                                            if isinstance(v, list):
                                                drones = v
                                                break
                                        if drones is None:
                                            try:
                                                resp = requests.get(f"{API_BASE}/drones", timeout=6)
                                                if resp.ok:
                                                    j = resp.json()
                                                    if isinstance(j, list):
                                                        drones = j
                                            except Exception:
                                                drones = None

                                        if isinstance(drones, list):
                                            for item in lst:
                                                for d in drones:
                                                    try:
                                                        if str(d.get('id')) == str(item) or str(d.get('name')) == str(item):
                                                            records.append(d)
                                                            break
                                                    except Exception:
                                                        continue

                                if records:
                                    st.write(f"{len(records)} drones assigned to service point {sp_id}")
                                    # display normalized table of selected columns
                                    try:
                                        rows = [_normalize_drone_for_table(r) for r in records]
                                        st.table(rows)
                                    except Exception:
                                        st.json(records)
                                    handled = True
                            if handled:
                                # already answered via explicit mapping
                                pass
                            else:
                                # Try extracting assignments from fleet['servicePoints'] or similar structures
                                sp_list_try = None
                                for k in ('servicePoints', 'service_points', 'servicePointList', 'service_point_list'):
                                    v = fleet.get(k)
                                    if isinstance(v, list):
                                        sp_list_try = v
                                        break
                                if sp_list_try:
                                    lst = None
                                    for sp in sp_list_try:
                                        try:
                                            sid = sp.get('id') or sp.get('servicePointId') or sp.get('name')
                                            if sid is None:
                                                continue
                                            if str(sid) == str(sp_id):
                                                # look for drone id lists under common fields
                                                for f in ('drones', 'droneIds', 'drone_ids', 'assignedDrones', 'assignedDroneIds', 'members'):
                                                    candidate = sp.get(f)
                                                    if candidate:
                                                        lst = candidate
                                                        break
                                                # if still not found, maybe the service point has a nested 'assignment' dict
                                                if lst is None and isinstance(sp.get('assignment'), dict):
                                                    for f in ('droneIds', 'drones'):
                                                        candidate = sp['assignment'].get(f)
                                                        if candidate:
                                                            lst = candidate
                                                            break
                                                break
                                        except Exception:
                                            continue
                                    if lst:
                                        # Normalize and resolve to records similar to earlier logic
                                        records = []
                                        # If elements are dicts, assume they are full records
                                        simple = True
                                        for item in lst:
                                            if not isinstance(item, dict):
                                                simple = False
                                                break
                                        if simple:
                                            records = lst
                                        else:
                                            # resolve ids/names via fleet['drones'] or API
                                            drones = None
                                            for k in ("drones", "droneList", "drone_list", "droneRecords", "drones_list"):
                                                v = fleet.get(k)
                                                if isinstance(v, list):
                                                    drones = v
                                                    break
                                            if drones is None:
                                                try:
                                                    resp = requests.get(f"{API_BASE}/drones", timeout=6)
                                                    if resp.ok:
                                                        j = resp.json()
                                                        if isinstance(j, list):
                                                            drones = j
                                                except Exception:
                                                    drones = None
                                            if isinstance(drones, list):
                                                for item in lst:
                                                    for d in drones:
                                                        try:
                                                            if str(d.get('id')) == str(item) or str(d.get('name')) == str(item):
                                                                records.append(d)
                                                                break
                                                        except Exception:
                                                            continue
                                        if records:
                                            st.write(f"{len(records)} drones assigned to service point {sp_id} (from fleet.servicePoints)")
                                            try:
                                                rows = [_normalize_drone_for_table(r) for r in records]
                                                st.table(rows)
                                            except Exception:
                                                st.json(records)
                                            handled = True

                                # continue to the next fallback if not handled
                                
                                # 2) Try to infer from detailed drone records (in fleet or via API)
                                drones = None
                                for k in ("drones", "droneList", "drone_list", "droneRecords", "droneRecordsList", "drones_list", "fleet"):
                                    v = fleet.get(k)
                                    if isinstance(v, list):
                                        drones = v
                                        break

                                # try fetching from API_BASE /drones if not present
                                if drones is None:
                                    try:
                                        resp = requests.get(f"{API_BASE}/drones", timeout=6)
                                        if resp.ok:
                                            j = resp.json()
                                            if isinstance(j, list):
                                                drones = j
                                    except Exception:
                                        drones = None

                                # try ILP endpoint as last resort
                                if drones is None:
                                    try:
                                        ilp_base = os.getenv('ILPENDPOINT') or None
                                        if ilp_base:
                                            if ilp_base.endswith('/'):
                                                ilp_base = ilp_base[:-1]
                                            resp2 = requests.get(f"{ilp_base}/drones", timeout=6)
                                            if resp2.ok:
                                                j2 = resp2.json()
                                                if isinstance(j2, list):
                                                    drones = j2
                                    except Exception:
                                        drones = None

                                if isinstance(drones, list):
                                    assigned = []
                                    for d in drones:
                                        # check common fields that might indicate assigned service point
                                        for f in ('servicePoint', 'service_point', 'assignedServicePoint', 'servicePointId', 'service_point_id', 'servicePointAssignment', 'assigned_service_point'):
                                            val = d.get(f)
                                            if val is None:
                                                continue
                                            # val may be dict or scalar
                                            if isinstance(val, dict):
                                                candidate = val.get('id') or val.get('name') or val.get('servicePointId')
                                            else:
                                                candidate = val
                                            try:
                                                if str(candidate) == str(sp_id):
                                                    assigned.append(str(d.get('name') or d.get('id')))
                                                    break
                                            except Exception:
                                                continue

                                    if assigned:
                                        st.info(f"{len(assigned)} drones assigned to service point {sp_id}: {', '.join(assigned)}")
                                    else:
                                        # fallback to counts in fleet summary
                                        sp_counts = fleet.get('servicePointCounts') or fleet.get('service_point_counts') or {}
                                        cnt = None
                                        if isinstance(sp_counts, dict):
                                            if sp_id in sp_counts:
                                                cnt = sp_counts.get(sp_id)
                                            else:
                                                for k2, v2 in sp_counts.items():
                                                    try:
                                                        if str(k2) == str(sp_id):
                                                            cnt = v2
                                                            break
                                                    except Exception:
                                                        continue

                                        # Try to fetch detailed drones and attempt to list those with matching service-point fields
                                        # Try several endpoints to fetch detailed drone records and collect debug info
                                        drones_for_table = None
                                        tried = []
                                        endpoints = [f"{API_BASE}/drones", f"{API_BASE}/fleet/drones", f"{API_BASE}/summary/drones"]
                                        for ep in endpoints:
                                            try:
                                                r = requests.get(ep, timeout=6)
                                                tried.append({'url': ep, 'status': r.status_code})
                                                if r.ok:
                                                    try:
                                                        j = r.json()
                                                    except Exception:
                                                        j = None
                                                    if isinstance(j, list) and j:
                                                        drones_for_table = j
                                                        break
                                            except Exception as e:
                                                tried.append({'url': ep, 'error': str(e)})

                                        # First, try dedicated service-point -> drones endpoints (they return a list of {servicePointId, drones})
                                        if drones_for_table is None:
                                            sp_eps = [
                                                f"{API_BASE}/drones-for-service-points",
                                                f"{API_BASE}/service-point-drones",
                                                f"{API_BASE}/servicePoints/drones",
                                                f"{API_BASE}/servicePointDrones",
                                            ]
                                            for spe in sp_eps:
                                                try:
                                                    r_sp = requests.get(spe, timeout=6)
                                                    tried.append({'url': spe, 'status': r_sp.status_code})
                                                    if r_sp.ok:
                                                        try:
                                                            spj = r_sp.json()
                                                        except Exception:
                                                            spj = None
                                                        if isinstance(spj, list) and spj:
                                                            # be permissive: look for dict entries with a servicePointId/servicePoint and a drones list
                                                            matched = None
                                                            for ent in spj:
                                                                if not isinstance(ent, dict):
                                                                    continue
                                                                if not ('servicePointId' in ent or 'servicePoint' in ent or 'servicePointId' in ent):
                                                                    continue
                                                                if not ('drones' in ent or 'droneIds' in ent or 'dronesList' in ent or isinstance(ent.get('drones'), list)):
                                                                    # still accept if ent has a 'drones' key even if empty
                                                                    pass
                                                                sid = ent.get('servicePointId') or ent.get('servicePoint') or ent.get('id')
                                                                try:
                                                                    if str(sid) == str(sp_id):
                                                                        matched = ent
                                                                        break
                                                                except Exception:
                                                                    continue

                                                            if matched is not None:
                                                                # normalize drones list
                                                                dlist = matched.get('drones') or matched.get('droneIds') or matched.get('dronesList') or []
                                                                if isinstance(dlist, list) and dlist:
                                                                    # Try to fetch detailed drones to enrich records
                                                                    detailed = None
                                                                    try:
                                                                        resp_all2 = requests.get(f"{API_BASE}/drones", timeout=6)
                                                                        tried.append({'url': f"{API_BASE}/drones", 'status': resp_all2.status_code})
                                                                        if resp_all2.ok:
                                                                            jall = resp_all2.json()
                                                                            if isinstance(jall, list):
                                                                                detailed = jall
                                                                    except Exception as e:
                                                                        tried.append({'url': f"{API_BASE}/drones", 'error': str(e)})

                                                                    records = []
                                                                    if detailed:
                                                                        for item in dlist:
                                                                            did = None
                                                                            if isinstance(item, dict):
                                                                                did = item.get('id') or item.get('droneId') or item.get('name')
                                                                            else:
                                                                                did = item
                                                                            for d in detailed:
                                                                                try:
                                                                                    if str(d.get('id')) == str(did) or str(d.get('name')) == str(did):
                                                                                        records.append(d)
                                                                                        break
                                                                                except Exception:
                                                                                    continue
                                                                    else:
                                                                        # no detailed list available, show the dlist as-is
                                                                        records = dlist

                                                                    if records:
                                                                        st.info(f"{len(records)} drones assigned to service point {sp_id} (from {spe})")
                                                                        try:
                                                                            rows = [_normalize_drone_for_table(r) for r in records]
                                                                            st.table(rows)
                                                                        except Exception:
                                                                            st.json(records)
                                                                        handled = True
                                                                        # set drones_for_table to a marker to avoid full-list fallback
                                                                        drones_for_table = []
                                                                        break
                                                except Exception as e:
                                                    tried.append({'url': spe, 'error': str(e)})
                                            # If not found on local API, try same service-point endpoints on ILP endpoint (if configured)
                                            if drones_for_table is None:
                                                ilp_base = os.getenv('ILPENDPOINT') or None
                                                if ilp_base:
                                                    if ilp_base.endswith('/'):
                                                        ilp_base = ilp_base[:-1]
                                                    for spe in [f"{ilp_base}/drones-for-service-points", f"{ilp_base}/service-point-drones", f"{ilp_base}/servicePoints/drones", f"{ilp_base}/servicePointDrones"]:
                                                        try:
                                                            r_sp = requests.get(spe, timeout=6)
                                                            tried.append({'url': spe, 'status': r_sp.status_code})
                                                            if r_sp.ok:
                                                                try:
                                                                    spj = r_sp.json()
                                                                except Exception:
                                                                    spj = None
                                                                if isinstance(spj, list) and spj:
                                                                    # find matching service point by servicePointId or servicePointId numeric
                                                                    matched = None
                                                                    for ent in spj:
                                                                        if not isinstance(ent, dict):
                                                                            continue
                                                                        sid = ent.get('servicePointId') or ent.get('servicePoint') or ent.get('servicePointId') or ent.get('id')
                                                                        try:
                                                                            if str(sid) == str(sp_id):
                                                                                matched = ent
                                                                                break
                                                                        except Exception:
                                                                            continue
                                                                    if matched is not None:
                                                                        dlist = matched.get('drones') or matched.get('droneIds') or matched.get('dronesList') or []
                                                                        # attempt to enrich with detailed ILP drones list
                                                                        records = []
                                                                        detailed = None
                                                                        try:
                                                                            resp_all2 = requests.get(f"{ilp_base}/drones", timeout=6)
                                                                            tried.append({'url': f"{ilp_base}/drones", 'status': resp_all2.status_code})
                                                                            if resp_all2.ok:
                                                                                jall = resp_all2.json()
                                                                                if isinstance(jall, list):
                                                                                    detailed = jall
                                                                        except Exception as e:
                                                                            tried.append({'url': f"{ilp_base}/drones", 'error': str(e)})

                                                                        if detailed:
                                                                            for item in dlist:
                                                                                did = None
                                                                                if isinstance(item, dict):
                                                                                    did = item.get('id') or item.get('droneId') or item.get('name')
                                                                                else:
                                                                                    did = item
                                                                                for d in detailed:
                                                                                    try:
                                                                                        if str(d.get('id')) == str(did) or str(d.get('name')) == str(did):
                                                                                            records.append(d)
                                                                                            break
                                                                                    except Exception:
                                                                                        continue
                                                                        else:
                                                                            records = dlist

                                                                        if records:
                                                                            st.info(f"{len(records)} drones assigned to service point {sp_id} (from {spe})")
                                                                            try:
                                                                                rows = [_normalize_drone_for_table(r) for r in records]
                                                                                st.table(rows)
                                                                            except Exception:
                                                                                st.json(records)
                                                                            handled = True
                                                                            drones_for_table = []
                                                                            break
                                                        except Exception as e:
                                                            tried.append({'url': spe, 'error': str(e)})

                                        # Also try ILP endpoint as last resort (generic /drones)
                                        if drones_for_table is None:
                                            try:
                                                ilp_base = os.getenv('ILPENDPOINT') or None
                                                if ilp_base:
                                                    if ilp_base.endswith('/'):
                                                        ilp_base = ilp_base[:-1]
                                                    r2 = requests.get(f"{ilp_base}/drones", timeout=6)
                                                    tried.append({'url': f"{ilp_base}/drones", 'status': r2.status_code if r2 is not None else 'err'})
                                                    if r2.ok:
                                                        try:
                                                            j2 = r2.json()
                                                        except Exception:
                                                            j2 = None
                                                        if isinstance(j2, list) and j2:
                                                            drones_for_table = j2
                                            except Exception as e:
                                                tried.append({'url': f"{ilp_base}/drones" if 'ilp_base' in locals() else 'ilp', 'error': str(e)})

                                        # Show debug expander with endpoints tried
                                        with st.expander('Debug: drone data fetch attempts', expanded=False):
                                            st.write(tried)

                                        # If we have drone records, try to infer assignments and show them; otherwise show full table
                                        if drones_for_table:
                                            inferred = []
                                            for d in drones_for_table:
                                                for f in ('servicePoint', 'service_point', 'assignedServicePoint', 'servicePointId', 'service_point_id'):
                                                    val = d.get(f)
                                                    if val is None:
                                                        continue
                                                    candidate = None
                                                    if isinstance(val, dict):
                                                        candidate = val.get('id') or val.get('name')
                                                    else:
                                                        candidate = val
                                                    try:
                                                        if str(candidate) == str(sp_id):
                                                            inferred.append(d)
                                                            break
                                                    except Exception:
                                                        continue

                                            if inferred:
                                                st.info(f"{len(inferred)} drones assigned to service point {sp_id} (inferred from drone records)")
                                                try:
                                                    rows = [_normalize_drone_for_table(r) for r in inferred]
                                                    st.table(rows)
                                                except Exception:
                                                    st.json(inferred)
                                            else:
                                                # Show full drone table and a note that assignment info wasn't present
                                                try:
                                                    st.info(f"No explicit assignment records found for service point {sp_id}. Showing full drone list for inspection (assignment fields missing). Count reported: {cnt if cnt is not None else 'unknown'}")
                                                    st.table(drones_for_table)
                                                except Exception:
                                                    st.info(f"No explicit assignment records found for service point {sp_id}. Count reported: {cnt if cnt is not None else 'unknown'}")
                                                    st.json(drones_for_table)
                                        else:
                                            if cnt is not None:
                                                st.info(f"{cnt} drones assigned to service point {sp_id}")
                                            else:
                                                st.info(f"No data for service point {sp_id}")
                                    handled = True

                                # Detect requests for a specific drone's details, e.g. "show details for drone 7"
                                try:
                                    import re as _re
                                    drone_match = _re.search(r"drone\s*#?\s*(\d+)", q)
                                except Exception:
                                    drone_match = None

                                if not handled and drone_match and ("detail" in q or "show" in q or "info" in q or "capacity" in q or "maxmove" in q or "cost" in q):
                                    with st.expander('Debug: drone lookup', expanded=False):
                                        st.write({'drone_match': bool(drone_match), 'drone_match_group': drone_match.group(1) if drone_match else None})
                                    did = drone_match.group(1)
                                    # try to find detailed drone record in fleet summary first
                                    drones = None
                                    for k in ("drones", "droneList", "drone_list", "droneRecords", "droneRecordsList", "drones_list", "fleet"):
                                        v = fleet.get(k)
                                        if isinstance(v, list):
                                            drones = v
                                            break

                                    # fallback to API_BASE /drones
                                    if drones is None:
                                        try:
                                            resp = requests.get(f"{API_BASE}/drones", timeout=6)
                                            if resp.ok:
                                                j = resp.json()
                                                if isinstance(j, list):
                                                    drones = j
                                        except Exception:
                                            drones = None

                                    # fallback to ILP /drones
                                    if drones is None:
                                        try:
                                            ilp_base = os.getenv('ILPENDPOINT') or None
                                            if ilp_base:
                                                if ilp_base.endswith('/'):
                                                    ilp_base = ilp_base[:-1]
                                                resp2 = requests.get(f"{ilp_base}/drones", timeout=6)
                                                if resp2.ok:
                                                    j2 = resp2.json()
                                                    if isinstance(j2, list):
                                                        drones = j2
                                        except Exception:
                                            drones = None

                                    found = None
                                    detailed_sample = None
                                    if isinstance(drones, list):
                                        detailed_sample = drones[:5]
                                        for d in drones:
                                            try:
                                                # match by id, name, droneId, or presence of the id in the name
                                                if d is None:
                                                    continue
                                                for key in ('id', 'droneId', 'name'):
                                                    val = d.get(key) if isinstance(d, dict) else None
                                                    if val is None:
                                                        continue
                                                    if str(val) == str(did):
                                                        found = d
                                                        break
                                                    # name like 'Drone 7'
                                                    if key == 'name' and isinstance(val, str) and (str(did) in val or f"drone {did}" in val.lower()):
                                                        found = d
                                                        break
                                                if found:
                                                    break
                                            except Exception:
                                                continue

                                    if found:
                                        # Select relevant fields to show
                                        out = {
                                            'id': found.get('id'),
                                            'name': found.get('name') or found.get('id'),
                                        }
                                        cap = found.get('capability') or {}
                                        # capability may be JSON string in some APIs
                                        if isinstance(cap, str):
                                            try:
                                                cap = json.loads(cap)
                                            except Exception:
                                                cap = {}
                                        out.update({
                                            'capacity': cap.get('capacity'),
                                            'maxMoves': cap.get('maxMoves') or cap.get('max_moves') or cap.get('maxMove'),
                                            'cooling': cap.get('cooling'),
                                            'heating': cap.get('heating'),
                                        })
                                        # costs may be top-level or inside capability
                                        out.update({
                                            'costInitial': found.get('costInitial') or cap.get('costInitial') or found.get('cost_initial') or cap.get('cost_initial'),
                                            'costFinal': found.get('costFinal') or cap.get('costFinal') or found.get('cost_final') or cap.get('cost_final'),
                                            'costPerMove': found.get('costPerMove') or cap.get('costPerMove') or found.get('cost_per_move') or cap.get('cost_per_move'),
                                        })
                                        st.subheader(f"Drone {did} details")
                                        try:
                                            st.table([out])
                                        except Exception:
                                            st.json(out)
                                        handled = True
                                    else:
                                        # Not found — show helpful debug info so we can adapt to the exact payload
                                        st.info(f"No detailed record found for drone {did} in fleet summary or API responses.")
                                        # Build a local probe of common drone endpoints so we can inspect responses
                                        tried_endpoints = []
                                        sample_drones = None
                                        # Probe local API
                                        try:
                                            r_local = requests.get(f"{API_BASE}/drones", timeout=6)
                                            tried_endpoints.append({'url': f"{API_BASE}/drones", 'status': r_local.status_code})
                                            if r_local.ok:
                                                try:
                                                    jloc = r_local.json()
                                                    if isinstance(jloc, list) and jloc:
                                                        sample_drones = jloc[:5]
                                                except Exception:
                                                    pass
                                        except Exception as e:
                                            tried_endpoints.append({'url': f"{API_BASE}/drones", 'error': str(e)})

                                        # Probe ILP endpoint if configured
                                        try:
                                            ilp_base = os.getenv('ILPENDPOINT') or None
                                            if ilp_base:
                                                if ilp_base.endswith('/'):
                                                    ilp_base = ilp_base[:-1]
                                                r_ilp = requests.get(f"{ilp_base}/drones", timeout=6)
                                                tried_endpoints.append({'url': f"{ilp_base}/drones", 'status': r_ilp.status_code})
                                                if r_ilp.ok:
                                                    try:
                                                        jilp = r_ilp.json()
                                                        if isinstance(jilp, list) and jilp and sample_drones is None:
                                                            sample_drones = jilp[:5]
                                                    except Exception:
                                                        pass
                                        except Exception as e:
                                            tried_endpoints.append({'url': f"{ilp_base}/drones" if 'ilp_base' in locals() else 'ilp', 'error': str(e)})

                                        with st.expander('Debug: drone detail lookup', expanded=True):
                                            st.write({'tried_endpoints': tried_endpoints})
                                            if sample_drones is not None:
                                                st.write({'sample_drones': sample_drones})
                                            else:
                                                st.write('No drone lists were available from tested endpoints')

                                import re

                        # Helper to parse comparison phrases into (op, value)
                        def _parse_threshold(text, unit_words=None):
                            # unit_words: list of unit suffixes to accept (e.g., ['l','litre']) or None
                            text = text.lower()
                            # direct operator forms: >=, <=, >, <, =
                            m = re.search(r"(>=|<=|>|<|=)\s*(\d+(?:\.\d+)?)\s*(%s)?\b" % ("|".join(unit_words) if unit_words else ""), text) if unit_words else re.search(r"(>=|<=|>|<|=)\s*(\d+(?:\.\d+)?)\b", text)
                            if m:
                                op = m.group(1)
                                val = float(m.group(2))
                                return op, val

                            # suffix-plus like '10l+' or '10+'
                            m2 = re.search(r"(\d+(?:\.\d+)?)\s*(%s)?\s*\+\b" % ("|".join(unit_words) if unit_words else ""), text)
                            if m2:
                                return '>=', float(m2.group(1))

                            # word forms
                            gt = re.search(r"(?:more than|greater than|above|over)\s*(\d+(?:\.\d+)?)\s*(%s)?\b" % ("|".join(unit_words) if unit_words else ""), text)
                            if gt:
                                return '>', float(gt.group(1))
                            gte = re.search(r"(?:at least|no less than|>=|not less than|minimum of)\s*(\d+(?:\.\d+)?)\s*(%s)?\b" % ("|".join(unit_words) if unit_words else ""), text)
                            if gte:
                                return '>=', float(gte.group(1))
                            lt = re.search(r"(?:less than|below|under)\s*(\d+(?:\.\d+)?)\s*(%s)?\b" % ("|".join(unit_words) if unit_words else ""), text)
                            if lt:
                                return '<', float(lt.group(1))
                            lte = re.search(r"(?:at most|no more than|<=|not more than|max of)\s*(\d+(?:\.\d+)?)\s*(%s)?\b" % ("|".join(unit_words) if unit_words else ""), text)
                            if lte:
                                return '<=', float(lte.group(1))

                            # phrases like 'capacity of more than 10' or 'capacity >= 10'
                            cap_phrase = re.search(r"capacity\s*(?:of)?\s*(?:more than|greater than|at least|>=|>|<=|<|no more than|at most)\s*(\d+(?:\.\d+)?)\b", text)
                            if cap_phrase:
                                # choose operator heuristically
                                if 'at least' in text or '>= ' in text or 'minimum' in text:
                                    return '>=', float(cap_phrase.group(1))
                                if 'more than' in text or 'greater than' in text or '>' in text:
                                    return '>', float(cap_phrase.group(1))
                                if 'less than' in text or 'below' in text or '<' in text:
                                    return '<', float(cap_phrase.group(1))
                                if 'no more than' in text or 'at most' in text or '<=' in text:
                                    return '<=', float(cap_phrase.group(1))
                                return '>=', float(cap_phrase.group(1))

                            return None, None

                        # parse moves threshold (accept comparators and word forms)
                        mv_op, mv_val = _parse_threshold(q, unit_words=['moves', 'move'])
                        # If user wrote '2000 moves' without comparator, treat as '>= 2000'
                        if mv_op is None:
                            mv_match = re.search(r"(\d{2,6})\s*(?:moves|move)\b", q)
                            if mv_match:
                                mv_op, mv_val = '>=', float(mv_match.group(1))

                        # cooling/heating flags
                        wants_cool = ("cool" in q or "cooling" in q)
                        wants_heat = ("heat" in q or "heating" in q or "heated" in q)

                        # parse capacity threshold (supports 'l' and word forms)
                        cap_op, cap_val = _parse_threshold(q, unit_words=['l', 'litre', 'litres', 'liter', 'liters'])

                        if (mv_val is not None) or wants_cool or wants_heat or (cap_val is not None):
                            # find detailed drone list under common keys
                            drones = None
                            for k in ("drones", "droneList", "drone_list", "droneRecords", "droneRecordsList", "drones_list", "fleet"):
                                v = fleet.get(k)
                                if isinstance(v, list):
                                    drones = v
                                    break

                            # try fetching from API_BASE /drones
                            if drones is None:
                                try:
                                    resp = requests.get(f"{API_BASE}/drones", timeout=6)
                                    if resp.ok:
                                        j = resp.json()
                                        if isinstance(j, list):
                                            drones = j
                                except Exception:
                                    drones = None

                            # try ILP endpoint
                            if drones is None:
                                try:
                                    ilp_base = os.getenv('ILPENDPOINT') or None
                                    if ilp_base:
                                        if ilp_base.endswith('/'):
                                            ilp_base = ilp_base[:-1]
                                        resp2 = requests.get(f"{ilp_base}/drones", timeout=6)
                                        if resp2.ok:
                                            j2 = resp2.json()
                                            if isinstance(j2, list):
                                                drones = j2
                                except Exception:
                                    drones = None

                            if isinstance(drones, list):
                                filtered = []
                                for d in drones:
                                    cap = d.get('capability') or {}
                                    ok = True
                                    # apply numeric moves filter if requested (respect comparator)
                                    if mv_val is not None:
                                        try:
                                            maxm = cap.get('maxMoves')
                                            if maxm is None:
                                                ok = False
                                            else:
                                                mm = int(maxm)
                                                if mv_op in (None, '>='):
                                                    ok = ok and (mm >= int(mv_val))
                                                elif mv_op == '>':
                                                    ok = ok and (mm > int(mv_val))
                                                elif mv_op == '<=':
                                                    ok = ok and (mm <= int(mv_val))
                                                elif mv_op == '<':
                                                    ok = ok and (mm < int(mv_val))
                                                elif mv_op == '=':
                                                    ok = ok and (mm == int(mv_val))
                                                else:
                                                    ok = ok and (mm >= int(mv_val))
                                        except Exception:
                                            ok = False
                                    # apply capability filters
                                    if wants_cool:
                                        ok = ok and bool(cap.get('cooling'))
                                    if wants_heat:
                                        ok = ok and bool(cap.get('heating'))
                                    # apply capacity filter if requested
                                    if cap_val is not None:
                                        try:
                                            capc = cap.get('capacity')
                                            if capc is None:
                                                ok = False
                                            else:
                                                capc_f = float(capc)
                                                if cap_op in (None, '=', ''):
                                                    ok = ok and (capc_f == float(cap_val))
                                                elif cap_op == '>=':
                                                    ok = ok and (capc_f >= float(cap_val))
                                                elif cap_op == '>':
                                                    ok = ok and (capc_f > float(cap_val))
                                                elif cap_op == '<=':
                                                    ok = ok and (capc_f <= float(cap_val))
                                                elif cap_op == '<':
                                                    ok = ok and (capc_f < float(cap_val))
                                        except Exception:
                                            ok = False
                                    if ok:
                                        name = d.get('name') or d.get('id')
                                        filtered.append(str(name))

                                if filtered:
                                    # human-friendly output
                                    if mv_val is not None:
                                        op_str = mv_op if mv_op is not None else '>='
                                        st.info(f"{len(filtered)} drones matching {op_str}{int(mv_val)} moves: {', '.join(filtered)}")
                                    else:
                                        quals = []
                                        if wants_cool:
                                            quals.append('cooling')
                                        if wants_heat:
                                            quals.append('heating')
                                        qstr = ' & '.join(quals) if quals else 'matching'
                                        st.info(f"{len(filtered)} drones with {qstr}: {', '.join(filtered)}")
                                else:
                                    st.info("No matching drones found (detailed records present).")
                                handled = True

                        # fallback to LLM/aggregate answer if not handled
                        if not handled:
                            answer = st.session_state.llm_parser.answer_fleet_question(
                                question,
                                fleet
                            )
                            st.info(answer)

    elif mode == "What-If Analysis":
        st.header("What-If Strategy Comparison")

        st.write("Compare different planning strategies for the same dispatches.")

        # Use last scenario if available
        if st.session_state.last_scenario:
            st.info("Using dispatches from last generated plan")
            dispatches = st.session_state.last_scenario.get('dispatches', [])
        else:
            st.warning("Generate a plan first or manually enter JSON below")
            dispatches_json = st.text_area(
                "Dispatches JSON",
                height=200,
                placeholder='[{"id": 1, "requirements": {"capacity": 3.0, "cooling": true}, "delivery": {"lng": -3.1883, "lat": 55.9217}}]'
            )
            if dispatches_json:
                try:
                    dispatches = json.loads(dispatches_json)
                except json.JSONDecodeError:
                    st.error("Invalid JSON")
                    return
            else:
                return

        col1, col2 = st.columns(2)
        with col1:
            strategy_a = st.selectbox("Strategy A", ["min_cost", "min_moves", "balanced"], key="sa")
        with col2:
            strategy_b = st.selectbox("Strategy B", ["min_cost", "min_moves", "balanced"], index=1, key="sb")

        if st.button("Compare Strategies", type="primary"):
            with st.spinner("Running comparison..."):
                result = what_if_compare(dispatches, strategy_a, strategy_b)

            if result:
                st.success("Comparison complete!")

                # Display delta summary
                st.subheader("Comparison Summary")
                st.text(result['delta']['summary'])

                # Side by side metrics
                col1, col2 = st.columns(2)

                with col1:
                    st.subheader(f"Plan A: {strategy_a}")
                    st.metric("Cost", f"£{result['planA']['totalCost']:.2f}")
                    st.metric("Moves", result['planA']['totalMoves'])
                    st.metric("Drones", len(result['planA']['dronePaths']))

                with col2:
                    st.subheader(f"Plan B: {strategy_b}")
                    cost_delta = result['delta']['costDifference']
                    moves_delta = result['delta']['movesDifference']
                    st.metric(
                        "Cost",
                        f"£{result['planB']['totalCost']:.2f}",
                        delta=f"{cost_delta:+.2f}"
                    )
                    st.metric(
                        "Moves",
                        result['planB']['totalMoves'],
                        delta=f"{moves_delta:+d}"
                    )
                    st.metric("Drones", len(result['planB']['dronePaths']))

    elif mode == "Manual JSON":
        st.header("Manual JSON Input")

        st.write("Enter a planning scenario in JSON format:")

        scenario_json = st.text_area(
            "Scenario JSON",
            height=300,
            value="""{
  "dispatches": [
    {
      "id": 1,
      "date": "2025-01-15",
      "time": "14:00:00",
      "requirements": {
        "capacity": 3.0,
        "cooling": true,
        "heating": false,
        "maxCost": 50.0
      },
      "delivery": {
        "lng": -3.1883,
        "lat": 55.9217
      }
    }
  ],
  "strategy": "min_cost"
}"""
        )

        if st.button("Create Plan", type="primary"):
            try:
                scenario = json.loads(scenario_json)
                with st.spinner("Creating plan..."):
                    plan = create_plan(scenario)
                if plan:
                    display_plan_result(plan)
            except json.JSONDecodeError as e:
                st.error(f"Invalid JSON: {e}")


if __name__ == "__main__":
    main()
