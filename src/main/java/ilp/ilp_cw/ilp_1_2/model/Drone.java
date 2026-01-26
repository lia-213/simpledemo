package ilp.ilp_cw.ilp_1_2.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import ilp.ilp_cw.ilp_1_2.aStarPathFinding.*;
import lombok.Data;

import java.util.List;

import static java.awt.geom.Point2D.distance;

@Data
public class Drone {
    @JsonProperty("name")
    private final String name;
    @JsonProperty("id")
    private final String id;
    @JsonProperty("capability")
    private final Capability capability;

    private static final float COST_TOLERANCE = 0.1f; // allow small rounding tolerance when comparing costs

    @JsonCreator
    public Drone(String name, String id, Capability capability) {
        this.name = name;
        this.id = id;
        this.capability = capability;
    }

    @JsonIgnore
    public Integer getId() {
        return (id == null || id.isEmpty()) ? null : Integer.parseInt(id);
    }

    @JsonIgnore
    public boolean getCooling() {
        return capability != null && capability.cooling();
    }

    @JsonIgnore
    public boolean getHeating() {
        return capability != null && capability.heating();
    }

    @JsonIgnore
    public boolean matchMedDispatchRec(MedDispatchRec medDispatchRec, List<DroneForServicePoint> dronesForServicePoint, List<ServicePoint> servicePointsSnapshot) {
        if (medDispatchRec == null) return false;

        if (!droneMatchesAvailability(medDispatchRec, dronesForServicePoint)) {
            return false;
        }

        if (medDispatchRec.requirements() != null) {
            Requirements req = medDispatchRec.requirements();

            if (req.getCapacity() != null) {
                if (doesNotMatchAttribute("capacity", ">=", String.valueOf(req.getCapacity()))) {
                    return false;
                }
            }

            if (req.getHeating() != null && req.getHeating()) {
                if (doesNotMatchAttribute("heating", "=", "true")) {
                    return false;
                }
            }

            if (req.getCooling() != null && req.getCooling()) {
                if (doesNotMatchAttribute("cooling", "=", "true")) {
                    return false;
                }
            }
        }

        Requirements req = medDispatchRec.requirements();
        Float maxCost = (req != null) ? req.getMaxCost() : null;

        boolean canMeetCostForAtLeastOneServicePoint = false;

        for (ServicePoint sp : servicePointsSnapshot) {
            Position servicePoint = sp.position();
            if (servicePoint == null) continue;

            Position dispatchPoint = medDispatchRec.delivery();
            if (dispatchPoint == null) continue;

            if (servicePoint.getLongitude() != null && servicePoint.getLatitude() != null
                    && dispatchPoint.getLongitude() != null && dispatchPoint.getLatitude() != null) {
                double servicePointX = servicePoint.getLongitude().doubleValue();
                double servicePointY = servicePoint.getLatitude().doubleValue();
                double dispatchPointX = dispatchPoint.getLongitude().doubleValue();
                double dispatchPointY = dispatchPoint.getLatitude().doubleValue();

                float costPerMove = capability.costPerMove();
                // For preliminary suitability we check moves-related cost only. The routing
                // step applies take-off/landing costs once per trip.
                double oneWayDistance = distance(servicePointX, servicePointY, dispatchPointX, dispatchPointY);
                double roundTripDistance = oneWayDistance * 2;
                int estimatedMoves = (int) Math.ceil(roundTripDistance / 0.00015);
                float estimatedMovesCost = (estimatedMoves * costPerMove);
                boolean withinMaxCost = true;
                if (maxCost != null && !maxCost.isInfinite()) {
                    float tolerance = Math.max(COST_TOLERANCE, costPerMove);
                    withinMaxCost = estimatedMovesCost <= (maxCost + tolerance);
                }
                if (withinMaxCost) {
                    canMeetCostForAtLeastOneServicePoint = true;
                    break; // Found valid service point
                }
            }
        }

        return canMeetCostForAtLeastOneServicePoint;
    }

    public boolean matchMedDispatchRec(MedDispatchRec medDispatchRec, List<DroneForServicePoint> dronesForServicePoint, List<ServicePoint> servicePointsSnapshot, StringBuilder reason) {
        if (medDispatchRec == null) {
            if (reason != null) reason.append("medDispatchRec is null");
            return false;
        }
        if (!droneMatchesAvailability(medDispatchRec, dronesForServicePoint)) {
            if (reason != null) reason.append("drone not available at required service point");
            return false;
        }
        if (medDispatchRec.requirements() != null) {
            Requirements req = medDispatchRec.requirements();
            if (req.getCapacity() != null) {
                if (doesNotMatchAttribute("capacity", ">=", String.valueOf(req.getCapacity()))) {
                    if (reason != null)
                        reason.append("capacity ").append(capability.capacity()).append(" < required ").append(req.getCapacity());
                    return false;
                }
            }
            if (req.getHeating() != null && req.getHeating()) {
                if (doesNotMatchAttribute("heating", "=", "true")) {
                    if (reason != null) reason.append("heating required but drone has no heating");
                    return false;
                }
            }
            if (req.getCooling() != null && req.getCooling()) {
                if (doesNotMatchAttribute("cooling", "=", "true")) {
                    if (reason != null) reason.append("cooling required but drone has no cooling");
                    return false;
                }
            }
        }
        Requirements req = medDispatchRec.requirements();
        Float maxCost = (req != null) ? req.getMaxCost() : null;
        boolean canMeetCostForAtLeastOneServicePoint = false;
        for (ServicePoint sp : servicePointsSnapshot) {
            Position servicePoint = sp.position();
            if (servicePoint == null) continue;
            Position dispatchPoint = medDispatchRec.delivery();
            if (dispatchPoint == null) continue;
            if (servicePoint.getLongitude() != null && servicePoint.getLatitude() != null
                    && dispatchPoint.getLongitude() != null && dispatchPoint.getLatitude() != null) {
                double servicePointX = servicePoint.getLongitude().doubleValue();
                double servicePointY = servicePoint.getLatitude().doubleValue();
                double dispatchPointX = dispatchPoint.getLongitude().doubleValue();
                double dispatchPointY = dispatchPoint.getLatitude().doubleValue();
                float costPerMove = capability.costPerMove();
                // For preliminary suitability we check moves-related cost only. The routing
                // step applies take-off/landing costs once per trip.
                double oneWayDistance = distance(servicePointX, servicePointY, dispatchPointX, dispatchPointY);
                double roundTripDistance = oneWayDistance * 2;
                int estimatedMoves = (int) Math.ceil(roundTripDistance / 0.00015);
                float estimatedMovesCost = (estimatedMoves * costPerMove);
                boolean withinMaxCost = true;
                if (maxCost != null && !maxCost.isInfinite()) {
                    float tolerance = Math.max(COST_TOLERANCE, costPerMove);
                    withinMaxCost = estimatedMovesCost <= (maxCost + tolerance);
                }
                if (withinMaxCost) {
                    canMeetCostForAtLeastOneServicePoint = true;
                    break;
                } else {
                    if (reason != null)
                        reason.append("estimatedMovesCost ").append(estimatedMovesCost).append(" > maxCost ").append(maxCost)
                                .append(" (takeoff/landing costs applied once per trip)");
                }
            }
        }
        if (!canMeetCostForAtLeastOneServicePoint) {
            if (reason != null && reason.length() == 0) reason.append("no service point can meet maxCost");
        }
        return canMeetCostForAtLeastOneServicePoint;
    }

    private boolean doesNotMatchAttribute(String attributeName, String operator, String expectedValue) {
        try {
            Object actualVal = extractAttributeValue(attributeName);
            if (actualVal == null) return true;
            return !matchesAsStringOrNumber(actualVal, operator, expectedValue);
        } catch (Exception e) {
            return true;
        }
    }

    private Object extractAttributeValue(String attributeName) {
        if (attributeName == null) return null;

        switch (attributeName) {
            case "name":
                return name;
            case "id":
                return getId();
            default:
                if (capability != null) {
                    switch (attributeName) {
                        case "heating":
                            return capability.heating();
                        case "cooling":
                            return capability.cooling();
                        case "capacity":
                            return capability.capacity();
                        case "maxMoves":
                        case "maxmoves":
                        case "max_moves":
                            return capability.maxMoves();
                        case "costPerMove":
                        case "costpermove":
                            return capability.costPerMove();
                        case "costInitial":
                        case "costinitial":
                            return capability.costInitial();
                        case "costFinal":
                        case "costfinal":
                            return capability.costFinal();
                    }
                }
                break;
        }
        return null;
    }

    private boolean matchesAsStringOrNumber(Object actualVal, String operator, String expectedStr) {
        if (actualVal == null || operator == null || expectedStr == null) return false;
        operator = operator.trim();

        if (actualVal instanceof Boolean) {
            boolean act = (Boolean) actualVal;
            boolean exp = Boolean.parseBoolean(expectedStr.trim().toLowerCase());
            return switch (operator) {
                case "=" -> act == exp;
                case "!=" -> act != exp;
                default -> false;
            };
        }

        if (actualVal instanceof Number) {
            double act = ((Number) actualVal).doubleValue();
            double exp;
            try {
                exp = Double.parseDouble(expectedStr);
            } catch (NumberFormatException e) {
                return false;
            }
            return switch (operator) {
                case "=" -> Double.compare(act, exp) == 0;
                case "!=" -> Double.compare(act, exp) != 0;
                case "<" -> Double.compare(act, exp) < 0;
                case ">" -> Double.compare(act, exp) > 0;
                case "<=" -> Double.compare(act, exp) <= 0;
                case ">=" -> Double.compare(act, exp) >= 0;
                default -> false;
            };
        }

        return false;
    }

    private boolean droneMatchesAvailability(MedDispatchRec medDispatchRec, List<DroneForServicePoint> dronesForServicePoints) {
        Integer droneId = getId();
        if (droneId == null) return false;
        if (dronesForServicePoints == null || dronesForServicePoints.isEmpty()) return false;

        for (DroneForServicePoint dsp : dronesForServicePoints) {
            if (dsp == null) continue;
            List<Availability> avail = dsp.getDroneAvailabilityForSpecificDrone(droneId);
            if (avail == null || avail.isEmpty()) continue;

            if (medDispatchRec.time() != null && medDispatchRec.date() != null) {
                boolean found = avail.stream().anyMatch(a ->
                        a.dateWorks(medDispatchRec.date()) &&
                                a.timeWorks(a.fromTime, a.untilTime, medDispatchRec.time())
                );
                return found;
            } else if (medDispatchRec.time() == null && medDispatchRec.date() != null) {
                return (avail.stream().anyMatch(a ->
                        a.dateWorks(medDispatchRec.date())
                ));
            } else if (medDispatchRec.date() == null && medDispatchRec.time() != null) {
                return (avail.stream().anyMatch(a ->
                        a.timeWorks(a.fromTime, a.untilTime, medDispatchRec.time())
                ));
            } else {
                return true;
            }
        }

        return false;
    }

    @JsonIgnore
    public FlightPath aStarSearch(List<MedDispatchRec> medDispatchRequests,
                                  ServicePoint originServicePoint,
                                  List<RestrictedArea> restrictedAreas) {
        if (medDispatchRequests == null || medDispatchRequests.isEmpty()
                || originServicePoint == null || originServicePoint.position() == null) {
            return null;
        }

        Position servicePointPos = originServicePoint.position();

        Graph<PositionNode> graph = new PositionGraph(restrictedAreas);
        PositionScorer scorer = new PositionScorer();
        RouteFinder<PositionNode> finder = new RouteFinder<>(graph, scorer, scorer);

        FlightPath flightPath = new FlightPath();
        List<Position> stops = flightPath.getStops();
        List<Integer> segmentEndIndices = flightPath.getSegmentEndIndices();

        Position currentPos = servicePointPos;
        stops.add(currentPos);

        for (MedDispatchRec rec : medDispatchRequests) {
            if (rec == null || rec.delivery() == null) continue;

            Position deliveryPos = rec.delivery();
            PositionNode startNode = new PositionNode("SP-SEG", currentPos);
            PositionNode goalNode = new PositionNode("DEL-" + rec.id(), deliveryPos);

            List<PositionNode> pathNodes;
            try {
                pathNodes = finder.findRoute(startNode, goalNode);
            } catch (Exception e) {
                return null;
            }

            boolean first = true;
            for (PositionNode node : pathNodes) {
                Position pos = node.getPosition();
                if (first) {
                    first = false;
                    continue;
                }
                stops.add(pos);
            }
            if (!stops.isEmpty()) {
                stops.add(stops.getLast());
            }
            segmentEndIndices.add(stops.size() - 1);

            currentPos = deliveryPos;
        }
        if (!stops.isEmpty() && !currentPos.equals(servicePointPos)) {
            PositionNode startNode = new PositionNode("RET-START", currentPos);
            PositionNode goalNode = new PositionNode("RET-SP", servicePointPos);
            List<PositionNode> returnNodes;
            try {
                returnNodes = finder.findRoute(startNode, goalNode);
            } catch (Exception e) {
                return null;
            }
            boolean first = true;
            for (PositionNode node : returnNodes) {
                Position pos = node.getPosition();
                if (first) {
                    first = false;
                    continue;
                }
                stops.add(pos);
            }
        }
        flightPath.setTotalMoves(Math.max(0, stops.size() - 1));
        return flightPath;
    }
}
