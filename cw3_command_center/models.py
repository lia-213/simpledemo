

# --- CW2 Dispatch Models and Helper ---
from typing import List, Optional
import requests
from pydantic import BaseModel, Field

class Position(BaseModel):
    lng: float
    lat: float

class Requirements(BaseModel):
    heating: bool | None = None
    cooling: bool | None = None
    capacity: float
    maxCost: float | None = None

class MedDispatchRec(BaseModel):
    id: int
    date: str | None = None
    time: str | None = None  # exact delivery time
    timeAfter: str | None = None  # earliest acceptable time
    timeBefore: str | None = None  # latest acceptable time
    requirements: Requirements
    delivery: Position

# --- PlanningScenario to MedDispatchRec conversion ---
def planning_scenario_to_dispatches(scenario):
    """
    Converts a PlanningScenario to a list of MedDispatchRec for CW2.
    Accepts either a PlanningScenario object or dict.
    """
    if hasattr(scenario, 'dispatches'):
        return scenario.dispatches
    elif isinstance(scenario, dict) and 'dispatches' in scenario:
        return [MedDispatchRec(**d) for d in scenario['dispatches']]
    else:
        raise ValueError("Invalid PlanningScenario input")

# --- Human-readable summary formatter ---
def format_plan_summary(plan_result):
    """
    Formats a human-readable summary from a PlanResult or dict.
    """
    if hasattr(plan_result, 'totalCost'):
        cost = plan_result.totalCost
        moves = plan_result.totalMoves
        drones = len(plan_result.dronePaths)
    else:
        cost = plan_result.get('totalCost')
        moves = plan_result.get('totalMoves')
        drones = len(plan_result.get('dronePaths', []))
    return f"Plan complete: {drones} drone(s) assigned, {moves} total moves, cost: {cost:.2f}"

# --- GeoJSON helper for /calcDeliveryPathAsGeoJson ---
def call_cw2_calc_delivery_path_as_geojson(
    dispatches: List[MedDispatchRec],
    cw2_base_url: str = "http://localhost:8080"
):
    """
    Calls /api/v1/calcDeliveryPathAsGeoJson on CW2 backend.
    Returns GeoJSON FeatureCollection.
    """
    # Call multi-drone GeoJSON endpoint which returns one LineString per drone
    url = f"{cw2_base_url}/api/v1/calcMultiDroneDeliveryPathAsGeoJson"
    payload = [d.dict() for d in dispatches]
    response = requests.post(url, json=payload, timeout=30)
    response.raise_for_status()
    return response.json()
# --- CW2 Dispatch Models and Helper ---

from typing import List, Optional
from pydantic import BaseModel, Field
import requests


class Position(BaseModel):
    lng: float
    lat: float

class Requirements(BaseModel):
    heating: bool | None = None
    cooling: bool | None = None
    capacity: float
    maxCost: float | None = None

class MedDispatchRec(BaseModel):
    id: int
    date: str | None = None
    time: str | None = None  # exact delivery time
    timeAfter: str | None = None  # earliest acceptable time
    timeBefore: str | None = None  # latest acceptable time
    requirements: Requirements
    delivery: Position

def call_cw2_calc_delivery_path(
    dispatches: List[MedDispatchRec],
    cw2_base_url: str = "http://localhost:8080"
):
    """
    Sends a list of MedDispatchRec objects to the CW2 Java backend
    using the official endpoint:
        POST /api/v1/calcDeliveryPath

    Returns:
        dict – the JSON response from CW2
    """

    url = f"{cw2_base_url}/api/v1/calcDeliveryPath"
    payload = [d.dict() for d in dispatches]
    response = requests.post(url, json=payload, timeout=30)
    response.raise_for_status()
    return response.json()
"""
Pydantic models for the MedSupplyDrones Command Center.
These models define the data structures for API requests/responses.
"""

from typing import List, Optional
from pydantic import BaseModel, Field
from datetime import date, time


class Requirements(BaseModel):
    """Medical dispatch requirements matching CW2 Requirements model."""
    heating: Optional[bool] = None
    cooling: Optional[bool] = None
    capacity: float
    maxCost: Optional[float] = None


class Position(BaseModel):
    """Geographic position with longitude and latitude."""
    lng: float
    lat: float


class MedDispatchRec(BaseModel):
    """
    Medical dispatch record matching CW2 MedDispatchRecDto.
    Represents a single delivery request.
    """
    id: int
    date: Optional[str] = None  # ISO date format
    time: Optional[str] = None  # ISO time format (exact delivery time)
    timeAfter: Optional[str] = None  # ISO time format (earliest acceptable time)
    timeBefore: Optional[str] = None  # ISO time format (latest acceptable time)
    requirements: Requirements
    delivery: Position


class DispatchRequest(BaseModel):
    """Simplified dispatch request for LLM parsing."""
    id: str = Field(description="Unique identifier for this dispatch")
    capacity: float = Field(description="Required capacity in liters")
    cooling: bool = Field(default=False, description="Requires cooling capability")
    heating: bool = Field(default=False, description="Requires heating capability")
    maxCost: Optional[float] = Field(default=None, description="Maximum acceptable cost")
    delivery: Optional[Position] = Field(default=None, description="Delivery destination")


class PlanningScenario(BaseModel):
    """
    Planning scenario containing dispatch requests and strategy preferences.
    This is the main input to the /plan endpoint.
    """
    dispatches: List[MedDispatchRec]
    strategy: str = Field(
        default="min_cost",
        description="Planning strategy: 'min_cost', 'min_moves', or 'balanced'"
    )


class FlightPath(BaseModel):
    """Individual flight path point."""
    position: Position
    angle: float
    ticksSinceStartOfCalculation: int


class DronePath(BaseModel):
    """Drone path information from CW2 calcDeliveryPath."""
    droneId: int
    flightPath: List[FlightPath]
    deliveredIds: List[int]
    capability: Optional[dict] = None


class PlanResult(BaseModel):
    """
    Result of a planning operation.
    Matches the structure returned by CW2 calcDeliveryPath.
    """
    totalCost: float
    totalMoves: int
    dronePaths: List[DronePath]
    summary: Optional[str] = None  # Human-readable summary


class WhatIfRequest(BaseModel):
    """Request for what-if analysis comparing two strategies."""
    dispatches: List[MedDispatchRec]
    strategyA: str = Field(default="min_cost", description="First strategy to compare")
    strategyB: str = Field(default="min_moves", description="Second strategy to compare")


class WhatIfResult(BaseModel):
    """Result of what-if analysis comparing two strategies."""
    planA: PlanResult
    planB: PlanResult
    delta: dict = Field(description="Differences between plans")


class FleetSummary(BaseModel):
    """Summary of available fleet capabilities."""
    totalDrones: int
    dronesWithCooling: int
    dronesWithHeating: int
    capacityDistribution: dict
    servicePointCounts: dict


class Drone(BaseModel):
    """Drone information from ILP REST service."""
    id: int
    capacity: float
    cooling: Optional[bool] = None
    heating: Optional[bool] = None
    costPerMove: Optional[float] = None


class ServicePoint(BaseModel):
    """Service point information from ILP REST service."""
    name: str
    position: Position

