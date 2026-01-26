"""
Orchestration logic for planning delivery routes.
Coordinates between ILP and CW2 services to create delivery plans.
"""

from typing import List, Optional, Dict, Any
import re
import logging
import json

# Module logger
logger = logging.getLogger(__name__)
if not logger.handlers:
    # Basic configuration for simple CLI runs; real deployments should
    # configure logging centrally.
    logging.basicConfig(level=logging.INFO)
from models import (
    PlanningScenario,
    PlanResult,
    MedDispatchRec,
    WhatIfResult
)
import os
from ilp_client import ILPClient
from cw2_client import CW2Client


class DeliveryPlanner:
    """
    Orchestrates delivery planning by coordinating ILP and CW2 services.
    """

    def __init__(self):
        self.ilp_client = ILPClient()
        self.cw2_client = CW2Client()

    def create_plan(self, scenario: PlanningScenario) -> PlanResult:
        """
        Create a delivery plan for the given scenario.

        Args:
            scenario: Planning scenario with dispatches and strategy.

        Returns:
            PlanResult with route information or empty plan if infeasible.
        """
        # Step 1: Query available drones
        available_drones = self.cw2_client.query_available_drones(scenario.dispatches)

        if not available_drones:
            # No feasible plan
            return PlanResult(
                totalCost=0.0,
                totalMoves=0,
                dronePaths=[],
                summary="No drones available that can fulfill all dispatch requirements."
            )

        # Step 2: Calculate delivery path
        # Note: CW2 service handles the routing internally
        # Future enhancement: pass strategy to influence ordering/selection
        raw_result = self.cw2_client.calc_delivery_path(
            scenario.dispatches,
            strategy=scenario.strategy
        )

        if not raw_result or raw_result.get("totalMoves", 0) == 0:
            return PlanResult(
                totalCost=0.0,
                totalMoves=0,
                dronePaths=[],
                summary="Could not find a valid delivery path for the given dispatches."
            )

        # Step 3: Parse and return result
        plan_result = self.cw2_client.parse_plan_result(raw_result)

        # Add summary
        drone_count = len(plan_result.dronePaths)
        # Compute delivery_count defensively. Some CW2 responses may omit or
        # rename the delivered id list; prefer the validated `dronePaths` but
        # fall back to scanning the raw_result for common keys or finally the
        # number of dispatches in the scenario.
        delivery_count = sum(len(getattr(dp, 'deliveredIds', []) or []) for dp in plan_result.dronePaths)
        if delivery_count == 0:
            # Try to extract delivered ids from the raw CW2 result with a
            # few tolerant key checks.
            try:
                seen = set()
                for dp in raw_result.get('dronePaths', []):
                    # common keys that CW2 might use
                    for key in ('deliveredIds', 'delivered', 'deliveries', 'delivered_ids'):
                        if key in dp and dp[key] is not None:
                            val = dp[key]
                            if isinstance(val, list):
                                for v in val:
                                    try:
                                        seen.add(int(v))
                                    except Exception:
                                        pass
                            elif isinstance(val, str):
                                # try comma/space separated numbers
                                for part in re.split(r"[\s,;]+", val.strip()):
                                    try:
                                        seen.add(int(part))
                                    except Exception:
                                        pass
                            break
                if seen:
                    delivery_count = len(seen)
                else:
                    # As a last resort, assume each dispatch in the scenario
                    # is a delivery (helps when CW2 returns only moves/cost)
                    delivery_count = len(scenario.dispatches)
                    # Log the raw CW2 response for debugging why delivered ids
                    # could not be extracted. This helps diagnose CW2 schema
                    # changes or unexpected payloads.
                    try:
                        logger.warning("Delivery count fallback: could not find delivered ids in CW2 response, falling back to scenario dispatch count=%d", len(scenario.dispatches))
                        # Emit a more visible dump of the raw_result and per-drone keys
                        try:
                            logger.warning("Raw CW2 result (pretty): %s", json.dumps(raw_result, indent=2))
                        except Exception:
                            logger.warning("Raw CW2 result (repr): %s", repr(raw_result))

                        # Log per-drone keys to help identify alternate delivered-id fields
                        try:
                            for i, dp in enumerate(raw_result.get('dronePaths', [])):
                                keys = list(dp.keys()) if isinstance(dp, dict) else []
                                logger.warning("CW2 dronePaths[%d] keys: %s", i, keys)
                                # If delivered-like fields exist, show their values
                                for cand in ('deliveredIds', 'delivered', 'deliveries', 'delivered_ids', 'visited', 'visitedIds'):
                                    if isinstance(dp, dict) and cand in dp:
                                        logger.warning("CW2 dronePaths[%d][%s] = %s", i, cand, dp[cand])
                        except Exception:
                            logger.warning("Could not iterate raw_result.dronePaths for detailed logging")

                        # Show what the parsed PlanResult contains for deliveredIds
                        try:
                            for dp in plan_result.dronePaths:
                                logger.warning("Parsed DronePath droneId=%s deliveredIds=%s", getattr(dp, 'droneId', None), getattr(dp, 'deliveredIds', None))
                        except Exception:
                            logger.warning("Could not serialize parsed plan_result.dronePaths for logging")
                    except Exception:
                        logger.warning("Delivery count fallback and raw_result could not be serialized for logging.")
            except Exception:
                delivery_count = len(scenario.dispatches)

        # # Ensure the reported delivery_count is at least the number of
        # # dispatches in the scenario (helps when CW2 omits delivered id lists).
        # try:
        #     delivery_count = max(delivery_count, len(scenario.dispatches))
        # except Exception:
        #     pass

        plan_result.summary = (
            f"Plan complete: {drone_count} drone(s) assigned, "
            f"{plan_result.totalMoves} total moves, "
            f"cost: {plan_result.totalCost:.2f}"
        )

        return plan_result

    def compare_strategies(
        self,
        dispatches: List[MedDispatchRec],
        strategy_a: str,
        strategy_b: str
    ) -> WhatIfResult:
        """
        Compare two planning strategies.

        Args:
            dispatches: List of dispatch requests.
            strategy_a: First strategy to try.
            strategy_b: Second strategy to try.

        Returns:
            WhatIfResult with both plans and delta metrics.
        """
        # Create two scenarios
        # Preprocess dispatch ordering to give different strategies some effect
        def _reorder_dispatches_for_strategy(dispatches_list: List[MedDispatchRec], strategy: str) -> List[MedDispatchRec]:
            """Return a reordered list of dispatches depending on the strategy.

            - `min_moves`: use a greedy nearest-neighbour ordering across all dispatch points
              (helps CW2's greedy allocation favor shorter routes).
            - `min_cost`: keep original order (CW2 uses cost-per-move drone selection).
            - `balanced`: sort by required capacity descending as a simple heuristic.
            """
            if not dispatches_list:
                return dispatches_list

            if strategy == 'min_moves':
                # Greedy nearest-neighbour across all deliveries using lat/lng
                remaining = dispatches_list[:]
                ordered: List[MedDispatchRec] = []
                # start from the first dispatch (or arbitrary)
                curr = remaining.pop(0)
                ordered.append(curr)
                def dist(a, b):
                    try:
                        dx = float(a.delivery.lng) - float(b.delivery.lng)
                        dy = float(a.delivery.lat) - float(b.delivery.lat)
                        return dx*dx + dy*dy
                    except Exception:
                        return float('inf')

                while remaining:
                    best_idx = None
                    best_d = None
                    for i, cand in enumerate(remaining):
                        d = dist(curr, cand)
                        if best_d is None or d < best_d:
                            best_d = d
                            best_idx = i
                    if best_idx is None:
                        ordered.extend(remaining)
                        break
                    curr = remaining.pop(best_idx)
                    ordered.append(curr)
                return ordered

            if strategy == 'balanced':
                try:
                    return sorted(dispatches_list, key=lambda d: float(d.requirements.capacity) if d.requirements and d.requirements.capacity is not None else 0.0, reverse=True)
                except Exception:
                    return dispatches_list

            # default and 'min_cost'
            return dispatches_list

        reordered_a = _reorder_dispatches_for_strategy(dispatches, strategy_a)
        reordered_b = _reorder_dispatches_for_strategy(dispatches, strategy_b)

        scenario_a = PlanningScenario(dispatches=reordered_a, strategy=strategy_a)
        scenario_b = PlanningScenario(dispatches=reordered_b, strategy=strategy_b)

        # Run both plans
        plan_a = self.create_plan(scenario_a)
        plan_b = self.create_plan(scenario_b)

        # Calculate deltas
        delta = {
            "costDifference": plan_b.totalCost - plan_a.totalCost,
            "movesDifference": plan_b.totalMoves - plan_a.totalMoves,
            "summary": self._generate_delta_summary(
                strategy_a, plan_a, strategy_b, plan_b
            )
        }

        return WhatIfResult(
            planA=plan_a,
            planB=plan_b,
            delta=delta
        )

    def _generate_delta_summary(
        self,
        strategy_a: str,
        plan_a: PlanResult,
        strategy_b: str,
        plan_b: PlanResult
    ) -> str:
        """Generate human-readable comparison summary."""
        cost_diff = plan_b.totalCost - plan_a.totalCost
        moves_diff = plan_b.totalMoves - plan_a.totalMoves

        summary = f"Comparing {strategy_a} vs {strategy_b}:\n"

        if cost_diff > 0:
            summary += f"  - {strategy_b} costs {cost_diff:.2f} MORE\n"
        elif cost_diff < 0:
            summary += f"  - {strategy_b} costs {abs(cost_diff):.2f} LESS\n"
        else:
            summary += f"  - Both strategies have the same cost\n"

        if moves_diff > 0:
            summary += f"  - {strategy_b} requires {moves_diff} MORE moves\n"
        elif moves_diff < 0:
            summary += f"  - {strategy_b} requires {abs(moves_diff)} FEWER moves\n"
        else:
            summary += f"  - Both strategies require the same number of moves\n"

        return summary

    def get_fleet_summary(self) -> Dict[str, Any]:
        """
        Get fleet capability summary, using CW2 for dronesWithCooling and dronesWithHeating.

        Returns:
            Dictionary with fleet statistics.
        """
        # Get the base summary from ILP
        summary = self.ilp_client.get_fleet_summary()

        # Determine CW2 base URL
        try:
            import requests
            cw2_base = os.getenv("CW2_ENDPOINT", "http://localhost:8080")
            if cw2_base.endswith("/"):
                cw2_base = cw2_base[:-1]
        except Exception as e:
            print(f"[WARN] Could not prepare CW2 client: {e}")
            return summary

        # Helper to query boolean-capability endpoints
        def _query_bool_list(path_suffix: str) -> int | None:
            try:
                url = f"{cw2_base}{path_suffix}"
                print(f"[DEBUG] Querying CW2: {url}")
                resp = requests.get(url, timeout=10)
                resp.raise_for_status()
                print(f"[DEBUG] CW2 {path_suffix} response: {resp.text}")
                payload = resp.json()
                if isinstance(payload, list):
                    return len(payload)
                if isinstance(payload, int):
                    return int(payload)
            except Exception as ex:
                print(f"[WARN] CW2 {path_suffix} query failed: {ex}")
            return None

        # Try to update dronesWithCooling
        cooling_count = _query_bool_list("/api/v1/dronesWithCooling/true")
        if cooling_count is not None:
            summary["dronesWithCooling"] = cooling_count

        # Try to update dronesWithHeating
        heating_count = _query_bool_list("/api/v1/dronesWithHeating/true")
        if heating_count is not None:
            summary["dronesWithHeating"] = heating_count

        return summary
