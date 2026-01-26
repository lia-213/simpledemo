"""
FastAPI application for MedSupplyDrones Dispatch Engine.
Provides REST endpoints for planning, fleet queries, and what-if analysis.
"""

from fastapi import FastAPI, HTTPException
from typing import Optional
from fastapi.middleware.cors import CORSMiddleware
from dotenv import load_dotenv
import os

from models import (
    PlanningScenario,
    PlanResult,
    WhatIfRequest,
    WhatIfResult,
    FleetSummary
)
from planner import DeliveryPlanner

# Load environment variables
load_dotenv()

# Create FastAPI app
app = FastAPI(
    title="MedSupplyDrones Dispatch Engine",
    description="Natural-language command center for drone dispatch planning",
    version="1.0.0"
)

# Add CORS middleware for web UI access
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Initialize planner
planner = DeliveryPlanner()


@app.get("/")
async def root():
    """Health check and API information."""
    return {
        "service": "MedSupplyDrones Dispatch Engine",
        "version": "1.0.0",
        "status": "operational",
        "pid": os.getpid(),
        "endpoints": {
            "plan": "POST /plan - Create delivery plan",
            "fleet": "GET /summary/fleet - Get fleet summary",
            "whatif": "POST /what-if - Compare strategies"
        }
    }


@app.post("/plan", response_model=PlanResult)
async def create_plan(scenario: PlanningScenario):
    """
    Create a delivery plan for the given scenario.

    Args:
        scenario: Planning scenario with dispatches and strategy preference.

    Returns:
        PlanResult with routes, costs, and move counts.

    Raises:
        HTTPException: If planning fails due to invalid input.
    """
    try:
        result = planner.create_plan(scenario)
        return result
    except Exception as e:
        raise HTTPException(
            status_code=500,
            detail=f"Planning failed: {str(e)}"
        )


@app.get("/summary/fleet")
async def get_fleet_summary():
    """
    Get aggregated fleet capabilities summary.

    Returns:
        FleetSummary with drone counts, capabilities, and service point assignments.
    """
    try:
        summary = planner.get_fleet_summary()
        return summary
    except Exception as e:
        raise HTTPException(
            status_code=500,
            detail=f"Failed to retrieve fleet summary: {str(e)}"
        )


@app.get("/summary/availability")
async def get_availability(day: Optional[str] = None, after: Optional[str] = None, before: Optional[str] = None):
    """
    Get computed weekday availability for drones assigned to service points.

    Returns:
        JSON with `perServicePoint` and `overall` mappings of day -> list of drone ids.
    """
    try:
        availability = planner.ilp_client.compute_weekday_availability(day=day, after_time=after, before_time=before)
        return availability
    except Exception as e:
        raise HTTPException(
            status_code=500,
            detail=f"Failed to compute availability: {str(e)}"
        )


@app.get("/summary/capacity")
async def get_capacity_summary(lt: Optional[float] = None, list_ids: Optional[bool] = False):
    """
    Query capacity-based counts.

    Query params:
      - lt: optional float threshold (liters). When provided, returns count (and optionally ids) of drones with capacity < lt.
      - list_ids: if true, include matching drone ids in the response.
    """
    try:
        if lt is None:
            raise HTTPException(status_code=400, detail="Please provide query parameter 'lt' (capacity less-than threshold in liters).")

        ids = planner.ilp_client.drones_with_capacity_less_than(lt)
        resp = {"threshold": float(lt), "count": len(ids)}
        if list_ids:
            resp["ids"] = ids
        return resp
    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Failed to compute capacity summary: {str(e)}")


@app.post("/what-if", response_model=WhatIfResult)
async def what_if_analysis(request: WhatIfRequest):
    """
    Compare two planning strategies for the same dispatches.

    Args:
        request: What-if request with dispatches and two strategies to compare.

    Returns:
        WhatIfResult with both plans and delta metrics.

    Raises:
        HTTPException: If analysis fails.
    """
    try:
        result = planner.compare_strategies(
            request.dispatches,
            request.strategyA,
            request.strategyB
        )
        return result
    except Exception as e:
        raise HTTPException(
            status_code=500,
            detail=f"What-if analysis failed: {str(e)}"
        )


@app.get("/cw2/drone/{drone_id}")
async def get_cw2_drone(drone_id: int):
    """
    Proxy endpoint to retrieve CW2 drone details for UI inspection.
    """
    try:
        # Use planner's CW2 client
        details = planner.cw2_client.get_drone_details(drone_id)
        if details is None:
            raise HTTPException(status_code=404, detail=f"Drone {drone_id} not found")
        return details
    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Failed to fetch drone details: {e}")


if __name__ == "__main__":
    import uvicorn
    port = int(os.getenv("PORT", 8000))
    uvicorn.run(app, host="0.0.0.0", port=port)
