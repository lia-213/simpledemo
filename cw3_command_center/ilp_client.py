"""
Client for interacting with the ILP REST service.
Provides methods to fetch drone and service point information.
"""

import os
import requests
from typing import List, Dict, Any, Optional
from models import Drone, ServicePoint, Position


class ILPClient:
    """Client for ILP REST API."""

    def __init__(self, base_url: Optional[str] = None):
        """
        Initialize ILP client.

        Args:
            base_url: Base URL for ILP REST service. Defaults to ILPENDPOINT env var.
        """
        self.base_url = base_url or os.getenv("ILPENDPOINT", "https://ilp-rest-2025-bvh6e9hschfagrgy.ukwest-01.azurewebsites.net/")
        if self.base_url.endswith("/"):
            self.base_url = self.base_url[:-1]

    def get_drones(self) -> List[Dict[str, Any]]:
        """
        Fetch all drones from ILP REST service.

        Returns:
            List of drone dictionaries from the API.
        """
        try:
            response = requests.get(f"{self.base_url}/drones", timeout=10)
            response.raise_for_status()
            return response.json()
        except requests.exceptions.RequestException as e:
            print(f"Error fetching drones from ILP: {e}")
            return []

    def get_service_points(self) -> List[Dict[str, Any]]:
        """
        Fetch all service points from ILP REST service.

        Returns:
            List of service point dictionaries from the API.
        """
        try:
            response = requests.get(f"{self.base_url}/service-points", timeout=10)
            response.raise_for_status()
            return response.json()
        except requests.exceptions.RequestException as e:
            print(f"Error fetching service points from ILP: {e}")
            return []

    def get_restricted_areas(self) -> List[Dict[str, Any]]:
        """
        Fetch restricted/no-fly zones from ILP REST service.

        Returns:
            List of restricted area dictionaries from the API.
        """
        try:
            response = requests.get(f"{self.base_url}/restricted-areas", timeout=10)
            response.raise_for_status()
            return response.json()
        except requests.exceptions.RequestException as e:
            print(f"Error fetching restricted areas from ILP: {e}")
            return []

    def get_drones_for_service_points(self) -> Dict[str, Any]:
        """
        Fetch drone assignments to service points.

        Returns:
            Dictionary mapping service points to drone lists.
        """
        try:
            response = requests.get(f"{self.base_url}/drones-for-service-points", timeout=10)
            response.raise_for_status()
            return response.json()
        except requests.exceptions.RequestException as e:
            print(f"Error fetching drones-for-service-points from ILP: {e}")
            return {}

    def get_fleet_summary(self) -> Dict[str, Any]:
        """
        Aggregate fleet information for quick queries.

        Returns:
            Dictionary with fleet statistics:
            - totalDrones: total number of drones
            - dronesWithCooling: count of drones with cooling
            - dronesWithHeating: count of drones with heating
            - capacityDistribution: histogram of capacity ranges
            - servicePointCounts: drones per service point
        """
        drones = self.get_drones()
        service_point_assignments = self.get_drones_for_service_points()

        total_drones = len(drones)
        # Drones may include a nested `capability` object per the ILP API.
        # Read cooling/heating/capacity from capability when present, otherwise fall back to top-level keys.
        drones_with_cooling = 0
        drones_with_heating = 0
        capacities = []
        for drone in drones:
            cap_obj = drone.get("capability") or {}
            cooling_val = cap_obj.get("cooling", drone.get("cooling", False))
            heating_val = cap_obj.get("heating", drone.get("heating", False))
            capacity_val = cap_obj.get("capacity", drone.get("capacity", 0))

            if cooling_val:
                drones_with_cooling += 1
            if heating_val:
                drones_with_heating += 1

            try:
                # Accept numeric values or strings like '4', '4.0', or '4L'
                if isinstance(capacity_val, (int, float)):
                    capacities.append(float(capacity_val))
                else:
                    s = str(capacity_val).strip()
                    # remove trailing non-digit characters (e.g. 'L')
                    import re
                    m = re.search(r"[-+]?[0-9]*\.?[0-9]+", s)
                    if m:
                        capacities.append(float(m.group(0)))
                    else:
                        capacities.append(0.0)
            except Exception:
                capacities.append(0.0)

        # Capacity distribution (buckets: <5, 5-10, 10-15, 15+)
        capacity_dist = {"<5L": 0, "5-10L": 0, "10-15L": 0, "15+L": 0}
        for cap in capacities:
            if cap < 5:
                capacity_dist["<5L"] += 1
            elif cap < 10:
                capacity_dist["5-10L"] += 1
            elif cap < 15:
                capacity_dist["10-15L"] += 1
            else:
                capacity_dist["15+L"] += 1

        # Debug: show capacities observed
        print("[DEBUG] ILP capacities:", capacities)

        # Service point counts
        sp_counts = {}
        if isinstance(service_point_assignments, list):
            for entry in service_point_assignments:
                sp_id = entry.get("servicePointId", "Unknown")
                drone_list = entry.get("drones", [])
                sp_counts[str(sp_id)] = len(drone_list)

        return {
            "totalDrones": total_drones,
            "dronesWithCooling": drones_with_cooling,
            "dronesWithHeating": drones_with_heating,
            "capacityDistribution": capacity_dist,
            "servicePointCounts": sp_counts
        }

    def compute_weekday_availability(self, day: Optional[str] = None, after_time: Optional[str] = None, before_time: Optional[str] = None) -> Dict[str, Dict[str, Any]]:
        """
        Compute weekday availability from the `drones-for-service-points` payload.

        Returns a dictionary with per-service-point availability and overall aggregation.
        Format:
        {
            "perServicePoint": { "<sp_id>": {"MONDAY": [id,...], ...}, ... },
            "overall": {"MONDAY": [id,...], ...}
        }
        """
        data = self.get_drones_for_service_points()
        days = ["MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY"]

        per_sp: Dict[str, Dict[str, List[str]]] = {}
        overall: Dict[str, List[str]] = {d: [] for d in days}

        # Helper to parse time strings like '16:00' or '16:00:00'
        def _parse_time_str(ts: str):
            if not ts or not isinstance(ts, str):
                return None
            try:
                # accept 'HH:MM' or 'HH:MM:SS'
                from datetime import time as _time
                parts = ts.split(":")
                h = int(parts[0])
                m = int(parts[1]) if len(parts) > 1 else 0
                s = int(parts[2]) if len(parts) > 2 else 0
                return _time(h, m, s)
            except Exception:
                return None

        cutoff_after = None
        cutoff_before = None
        if after_time:
            cutoff_after = _parse_time_str(after_time)
        if before_time:
            cutoff_before = _parse_time_str(before_time)

        if isinstance(data, list):
            for entry in data:
                sp_id = entry.get("servicePointId")
                key = str(sp_id)
                per_sp[key] = {d: [] for d in days}
                drones = entry.get("drones", [])
                for d in drones:
                    drone_id = str(d.get("id"))
                    availability = d.get("availability", [])
                    for slot in availability:
                        slot_day = slot.get("dayOfWeek")
                        if not slot_day:
                            continue
                        slot_day_u = str(slot_day).upper()
                        if slot_day_u not in per_sp[key]:
                            continue

                        # If a time cutoff was requested, check slot 'from' and 'until' fields
                        if cutoff_after is not None or cutoff_before is not None:
                            slot_from = _parse_time_str(slot.get("from") or slot.get("fromTime") or slot.get("from_time"))
                            slot_until = _parse_time_str(slot.get("until") or slot.get("untilTime") or slot.get("until_time"))
                            if slot_from is None or slot_until is None:
                                # can't evaluate this slot for time; skip it
                                continue
                            # If asking for 'after' a cutoff: include if cutoff >= from AND cutoff < until
                            if cutoff_after is not None:
                                if not ((cutoff_after >= slot_from) and (cutoff_after < slot_until)):
                                    continue
                            # If asking for 'before' a cutoff: include if the slot overlaps [00:00, cutoff_before)
                            if cutoff_before is not None:
                                from datetime import time as _time
                                window_start = _time(0, 0, 0)
                                overlap_start = slot_from if slot_from > window_start else window_start
                                overlap_end = slot_until if slot_until < cutoff_before else cutoff_before
                                # overlap exists if overlap_end > overlap_start
                                if not (overlap_end > overlap_start):
                                    continue

                        # Passed cutoff (or no cutoff requested) -> include drone for that day
                        if drone_id not in per_sp[key][slot_day_u]:
                            per_sp[key][slot_day_u].append(drone_id)
                        if drone_id not in overall[slot_day_u]:
                            overall[slot_day_u].append(drone_id)

        # Build counts for convenience
        per_sp_counts: Dict[str, Dict[str, int]] = {}
        overall_counts: Dict[str, int] = {}
        for sp, days_map in per_sp.items():
            per_sp_counts[sp] = {d: len(days_map.get(d, [])) for d in days}
        overall_counts = {d: len(overall.get(d, [])) for d in days}

        result = {
            "perServicePoint": per_sp,
            "overall": overall,
            "perServicePointCounts": per_sp_counts,
            "overallCounts": overall_counts,
        }

        # If a single day was requested, filter the response to only that day
        if day:
            day = str(day).upper()
            if day not in days:
                return {"error": f"Invalid day: {day}. Use MONDAY..SUNDAY"}

            filtered_per_sp = {sp: {day: mapping.get(day, [])} for sp, mapping in per_sp.items()}
            filtered_counts = {sp: {day: per_sp_counts.get(sp, {}).get(day, 0)} for sp in per_sp_counts}
            return {
                "perServicePoint": filtered_per_sp,
                "overall": {day: overall.get(day, [])},
                "perServicePointCounts": filtered_counts,
                "overallCounts": {day: overall_counts.get(day, 0)}
            }

        return result

    def _parse_drone_capacity(self, capacity_val: Any) -> float:
        """Parse a single capacity field into a float (liters).

        Accepts numeric values or strings like '4', '4.0', '4L', '4.5L'.
        Returns 0.0 on parse failure.
        """
        try:
            if isinstance(capacity_val, (int, float)):
                return float(capacity_val)
            s = str(capacity_val).strip()
            import re
            m = re.search(r"[-+]?[0-9]*\.?[0-9]+", s)
            if m:
                return float(m.group(0))
        except Exception:
            pass
        return 0.0

    def drones_with_capacity_less_than(self, threshold: float) -> List[str]:
        """Return list of drone ids whose parsed capacity is strictly less than `threshold` liters."""
        drones = self.get_drones()
        result: List[str] = []
        for drone in drones:
            cap_obj = drone.get("capability") or {}
            capacity_val = cap_obj.get("capacity", drone.get("capacity", 0))
            cap = self._parse_drone_capacity(capacity_val)
            if cap < float(threshold):
                result.append(str(drone.get("id")))
        return result

    def count_drones_with_capacity_less_than(self, threshold: float) -> int:
        """Return the number of drones with capacity < threshold liters."""
        return len(self.drones_with_capacity_less_than(threshold))
