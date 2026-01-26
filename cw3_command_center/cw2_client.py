"""
Client for interacting with the CW2 service.
Provides methods to query drones and calculate delivery paths.
"""

import os
import requests
from typing import List, Dict, Any, Optional
from models import MedDispatchRec, PlanResult, DronePath, FlightPath, Position
import logging
import re

# Module logger
logger = logging.getLogger(__name__)
if not logger.handlers:
    logging.basicConfig(level=logging.INFO)


class CW2Client:
    """Client for CW2 Drone Service API."""

    def __init__(self, base_url: Optional[str] = None):
        """
        Initialize CW2 client.

        Args:
            base_url: Base URL for CW2 service. Defaults to CW2_ENDPOINT env var.
        """
        self.base_url = base_url or os.getenv("CW2_ENDPOINT", "http://localhost:8080")
        if self.base_url.endswith("/"):
            self.base_url = self.base_url[:-1]
        self.api_base = f"{self.base_url}/api/v1"

    def query_available_drones(self, dispatches: List[MedDispatchRec]) -> List[int]:
        """
        Query which drones are available for a set of dispatch requests.

        Args:
            dispatches: List of medical dispatch records.

        Returns:
            List of drone IDs that can fulfill all requirements.
        """
        try:
            # Convert Pydantic models to dicts
            payload = [d.model_dump(exclude_none=True) for d in dispatches]

            response = requests.post(
                f"{self.api_base}/queryAvailableDrones",
                json=payload,
                timeout=10
            )
            response.raise_for_status()
            return response.json()
        except requests.exceptions.RequestException as e:
            print(f"Error querying available drones from CW2: {e}")
            return []

    def calc_delivery_path(
        self,
        dispatches: List[MedDispatchRec],
        strategy: str = "min_cost"
    ) -> Optional[Dict[str, Any]]:
        """
        Calculate delivery path for dispatch requests.

        Args:
            dispatches: List of medical dispatch records.
            strategy: Planning strategy (currently for future enhancement).

        Returns:
            Dictionary with totalCost, totalMoves, and dronePaths.
        """
        try:
            # Convert Pydantic models to dicts
            payload = [d.model_dump(exclude_none=True) for d in dispatches]

            # Pass the chosen strategy as a query parameter so CW2 can vary behavior
            url = f"{self.api_base}/calcDeliveryPath"
            if strategy:
                url = f"{url}?strategy={strategy}"
            response = requests.post(
                url,
                json=payload,
                timeout=30
            )
            response.raise_for_status()
            return response.json()
        except requests.exceptions.RequestException as e:
            print(f"Error calculating delivery path from CW2: {e}")
            return None

    def calc_delivery_path_as_geojson(
        self,
        dispatches: List[MedDispatchRec]
    ) -> Optional[Dict[str, Any]]:
        """
        Calculate delivery path and return as GeoJSON for visualization.

        Args:
            dispatches: List of medical dispatch records.

        Returns:
            GeoJSON FeatureCollection structure.
        """
        try:
            # Convert Pydantic models to dicts
            payload = [d.model_dump(exclude_none=True) for d in dispatches]

            # Request multi-drone GeoJSON (one LineString per assigned drone)
            url = f"{self.api_base}/calcMultiDroneDeliveryPathAsGeoJson"
            response = requests.post(
                url,
                json=payload,
                timeout=30
            )
            response.raise_for_status()
            return response.json()
        except requests.exceptions.RequestException as e:
            print(f"Error getting GeoJSON from CW2: {e}")
            return None

    def get_drone_details(self, drone_id: int) -> Optional[Dict[str, Any]]:
        """
        Get details for a specific drone.

        Args:
            drone_id: ID of the drone to retrieve.

        Returns:
            Drone details dictionary or None if not found.
        """
        try:
            response = requests.get(
                f"{self.api_base}/droneDetails/{drone_id}",
                timeout=10
            )
            if response.status_code == 404:
                # Log for debugging why a 404 was returned from CW2
                try:
                    print(f"[DEBUG] CW2 GET {self.api_base}/droneDetails/{drone_id} returned 404: {response.text}")
                except Exception:
                    print(f"[DEBUG] CW2 GET {self.api_base}/droneDetails/{drone_id} returned 404 and body could not be read")
                return None
            response.raise_for_status()
            return response.json()
        except requests.exceptions.RequestException as e:
            print(f"Error fetching drone details from CW2: {e}")
            return None

    def parse_plan_result(self, raw_result: Dict[str, Any]) -> PlanResult:
        """
        Parse raw API response into PlanResult model.

        Args:
            raw_result: Raw dictionary from calcDeliveryPath.

        Returns:
            Parsed PlanResult object.
        """
        drone_paths = []
        for dp in raw_result.get("dronePaths", []):
            flight_path = [
                FlightPath(
                    position=Position(**fp["position"]),
                    angle=fp["angle"],
                    ticksSinceStartOfCalculation=fp["ticksSinceStartOfCalculation"]
                )
                for fp in dp.get("flightPath", [])
            ]
            # Attempt to fetch drone capability/details from CW2 and attach to the result
            cap = None
            try:
                details = self.get_drone_details(dp["droneId"])
                if details and isinstance(details, dict):
                    cap = details.get("capability")
            except Exception:
                cap = None

            # Normalize delivered ids from various possible CW2 response shapes
            delivered_raw = None
            for key in ("deliveredIds", "delivered", "deliveries", "delivered_ids", "visited", "visitedIds", "deliveredId"):
                if key in dp:
                    delivered_raw = dp.get(key)
                    if delivered_raw is not None:
                        logger.debug("Found delivered-like key '%s' in CW2 dronePaths entry", key)
                        break

            delivered_ids = []
            try:
                if delivered_raw is None:
                    delivered_ids = []
                elif isinstance(delivered_raw, list):
                    # Could be list of ints or list of dicts
                    if delivered_raw and isinstance(delivered_raw[0], dict):
                        # try to extract id fields from dicts
                        for item in delivered_raw:
                            for cand in ("id", "dispatchId", "dispatch_id", "deliveryId"):
                                if cand in item:
                                    try:
                                        delivered_ids.append(int(item[cand]))
                                        break
                                    except Exception:
                                        pass
                    else:
                        for v in delivered_raw:
                            try:
                                delivered_ids.append(int(v))
                            except Exception:
                                # skip non-intable entries
                                pass
                elif isinstance(delivered_raw, str):
                    # comma/space separated ids
                    parts = re.split(r"[\s,;]+", delivered_raw.strip())
                    for p in parts:
                        try:
                            delivered_ids.append(int(p))
                        except Exception:
                            pass
                elif isinstance(delivered_raw, int):
                    delivered_ids = [delivered_raw]
            except Exception as ex:
                logger.warning("Failed to normalize delivered ids from CW2 entry: %s", ex)

            drone_paths.append(
                DronePath(
                    droneId=dp["droneId"],
                    flightPath=flight_path,
                    deliveredIds=delivered_ids,
                    capability=cap
                )
            )

        return PlanResult(
            totalCost=raw_result.get("totalCost", 0.0),
            totalMoves=raw_result.get("totalMoves", 0),
            dronePaths=drone_paths
        )
