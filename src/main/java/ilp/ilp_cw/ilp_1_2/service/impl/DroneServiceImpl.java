package ilp.ilp_cw.ilp_1_2.service.impl;

import ilp.ilp_cw.ilp_1_2.dto.*;
import ilp.ilp_cw.ilp_1_2.service.DroneIlpClient;
import ilp.ilp_cw.ilp_1_2.service.DroneService;
import ilp.ilp_cw.ilp_1_2.util.DistanceUtils;
import ilp.ilp_cw.ilp_1_2.model.*;
import ilp.ilp_cw.ilp_1_2.aStarPathFinding.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Default implementation of {@link DroneService} containing the business logic
 * for distance, closeness, next-position computation and region containment checks.
 * <p>
 * Methods validate inputs and return {@link ResponseEntity} objects with either
 * 200 OK and the computed result, or 400 Bad Request for invalid inputs.
 */
@Service
public class DroneServiceImpl implements DroneService {
    private static final Logger log = LoggerFactory.getLogger(DroneServiceImpl.class);

    private final DroneIlpClient droneIlpClient;

    @Autowired
    public DroneServiceImpl(DroneIlpClient droneIlpClient) {
        this.droneIlpClient = droneIlpClient;
    }

    /**
     * Helper methods to retrieve fresh data from ILP service on each call.
     * <p>
     * Per instructor clarification: "you should reach out to the ILP-service (which is passed to you)
     * on each request and retrieve the necessary information"
     * <p>
     * Per instructor clarification: "you were never supposed to have any kind of persistence layer.
     * All operations are in-memory only"
     * <p>
     * These methods do NOT cache data - they fetch fresh from the ILP REST API each time.
     * Protected to allow subclasses to override for testing (e.g., mock data sources).
     */
    protected List<Drone> fetchAllDrones() {
        return droneIlpClient.fetchAllDrones();
    }

    protected List<DroneForServicePoint> fetchAllDronesForServicePoints() {
        return droneIlpClient.fetchAllDronesForServicePoints();
    }

    protected List<ServicePoint> fetchAllServicePoints() {
        return droneIlpClient.fetchAllServicePoints();
    }

    protected List<RestrictedArea> fetchAllRestrictedAreas() {
        return droneIlpClient.fetchAllRestrictedAreas();
    }

    /**
     * Returns the student's UID.
     */
    @Override
    public String getUID() {
        return "s2141930";
    }

    /**
     * Compute the Euclidean distance between two positions.
     * <p>
     * Validates presence of coordinates and returns 400 if any coordinate is missing or an error occurs.
     */
    @Override
    public ResponseEntity<BigDecimal> distanceTo(PositionsDto positionsDto) {
        // Basic DTO null checks
        if (positionsDto == null
                || positionsDto.getPosition1() == null
                || positionsDto.getPosition2() == null) {
            return ResponseEntity.badRequest().build();
        }

        Position dtoPos1 = positionsDto.getPosition1();
        Position dtoPos2 = positionsDto.getPosition2();

        // Make sure lng/lat are present
        if (dtoPos1.getLongitude() == null || dtoPos1.getLatitude() == null
                || dtoPos2.getLongitude() == null || dtoPos2.getLatitude() == null) {
            return ResponseEntity.badRequest().build();
        }

        // Build domain Positions
        Position p1 = new Position(dtoPos1.getLongitude(), dtoPos1.getLatitude());
        Position p2 = new Position(dtoPos2.getLongitude(), dtoPos2.getLatitude());

        // Validate coordinate ranges: reject positions with out-of-range latitude/longitude
        if (!p1.isValidPosition() || !p2.isValidPosition()) {
            return ResponseEntity.badRequest().build();
        }

        try {
            BigDecimal distance = DistanceUtils.euclideanDistance(p1, p2);
            return ResponseEntity.ok(distance);
        } catch (IllegalArgumentException e) {
            // DistanceUtils guards against bad coordinates as well
            return ResponseEntity.badRequest().build();
        }
    }


    /**
     * Returns whether two positions are closer than 0.00015 units.
     * <p>
     * Calls {@link #distanceTo(PositionsDto)} and maps errors to 400 Bad Request.
     */
    @Override
    public ResponseEntity<Boolean> isCloseTo(PositionsDto positionsDto) {
        ResponseEntity<BigDecimal> response = distanceTo(positionsDto);

        if (response == null || response.getStatusCode() != HttpStatus.OK || response.getBody() == null)
            return ResponseEntity.badRequest().build();

        return ResponseEntity.ok(response.getBody().compareTo(new BigDecimal("0.00015")) < 0);
    }

    /**
     * Compute the next position given a start and an angle in degrees.
     * <p>
     * Validates the request, angle range and that the angle is a multiple of 22.5 degrees
     * (within a tiny tolerance). Uses trigonometry and small-step constants to compute
     * a new latitude/longitude and returns 400 on invalid input.
     */
    @Override
    public ResponseEntity<Position> computeNextPosition(DroneMoveRqstDto req) {
        if (req == null || req.getStartPosition() == null) return ResponseEntity.badRequest().build();
        Position start = req.getStartPosition();
        BigDecimal angle = req.getAngle();

        if (angle == null || angle.compareTo(BigDecimal.ZERO) < 0 || angle.compareTo(new BigDecimal("360")) >= 0)
            return ResponseEntity.badRequest().build();

        BigDecimal scaled = angle.divide(new BigDecimal("22.5"), MathContext.DECIMAL64);
        BigDecimal diff = scaled.subtract(scaled.setScale(0, RoundingMode.HALF_UP)).abs();
        if (diff.compareTo(new BigDecimal("1e-9")) > 0) return ResponseEntity.badRequest().build();

        if (!start.isValidPosition()) return ResponseEntity.badRequest().build();

        double rad = Math.toRadians(angle.doubleValue());
        BigDecimal step = new BigDecimal("0.00015");
        MathContext mc = MathContext.DECIMAL64;

        BigDecimal newLat = step.multiply(BigDecimal.valueOf(Math.cos(rad)), mc).add(start.getLatitude());
        BigDecimal newLng = step.multiply(BigDecimal.valueOf(Math.sin(rad)), mc)
                .divide(BigDecimal.valueOf(Math.cos(Math.toRadians(start.getLatitude().doubleValue()))), mc)
                .add(start.getLongitude());

        return ResponseEntity.ok(new Position(newLng, newLat));
    }

    /**
     * Determine whether a position is inside the provided region.
     * <p>
     * Validates inputs and returns 400 for invalid wrapper, invalid position or invalid region definition.
     */
    @Override
    public ResponseEntity<Boolean> isInRegion(LocationRegionDto wrapper) {
        if (wrapper == null || wrapper.getCurrPosition() == null || wrapper.getRegion() == null)
            return ResponseEntity.badRequest().build();

        if (!wrapper.getCurrPosition().isValidPosition() || wrapper.getRegion().isNotValidRegion())
            return ResponseEntity.badRequest().build();

        return ResponseEntity.ok(wrapper.getRegion().contains(wrapper.getCurrPosition()));
    }

    @Override
    public ResponseEntity<List<Integer>> dronesWithCooling(Boolean state) {
        // CW2 spec: Always return 200, use empty list for invalid input
        if (state == null) return ResponseEntity.ok(Collections.emptyList());

        List<Integer> ids = droneIlpClient.findDroneIdsByCooling(state);
        return ResponseEntity.ok(ids);
    }

    @Override
    public ResponseEntity<List<Integer>> dronesWithHeating(Boolean heating) {
        // CW2 spec: Always return 200, use empty list for invalid input
        if (heating == null) return ResponseEntity.ok(Collections.emptyList());

        List<Integer> ids = droneIlpClient.findDroneIdsByHeating(heating);
        return ResponseEntity.ok(ids);
    }

    @Override
    public ResponseEntity<Drone> getDrone(int id) {
        if (id <= 0) return ResponseEntity.status(HttpStatus.NOT_FOUND).build();

        List<Drone> drones = fetchAllDrones();
        if (drones == null) return ResponseEntity.badRequest().build();

        for (Drone drone : drones) {
            if (drone.getId() == id) {
                return ResponseEntity.ok(drone);
            }
        }

        return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    }

    @Override
    public ResponseEntity<List<Integer>> getSuitableDrones(List<MedDispatchRecDto> medDispatchRequests) {
        if (medDispatchRequests == null || medDispatchRequests.isEmpty())
            return ResponseEntity.ok(Collections.emptyList());
        List<Drone> dronesSnapshot = fetchAllDrones();
        if (dronesSnapshot == null || dronesSnapshot.isEmpty())
            return ResponseEntity.ok(Collections.emptyList());
        List<DroneForServicePoint> dfspSnapshot = fetchAllDronesForServicePoints();
        List<ServicePoint> servicePointsSnapshot = fetchAllServicePoints();
        // Convert DTOs to models ONCE
        List<MedDispatchRec> medDispatchModels = medDispatchRequests.stream().map(this::convertToModel).toList();
        List<Integer> suitableDrones = findSuitableDronesInternal(
                medDispatchModels,
                dronesSnapshot,
                dfspSnapshot,
                servicePointsSnapshot
        );
        return ResponseEntity.ok(suitableDrones);
    }

    /**
     * Internal method to find suitable drones using pre-fetched data.
     * <p>
     * Per instructor: "Each POST is one call and starts a new sequence."
     * This method accepts pre-fetched data to ensure consistency when called
     * from within other endpoints (e.g., calcDeliveryPath calling getSuitableDrones logic).
     * <p>
     * <b>maxCost Handling (Heuristic Approach):</b>
     * Per instructor: "Don't overdo the optimization as there are no marks for 'the best path'"
     * - This method checks maxCost using INDIVIDUAL delivery estimates (heuristic)
     * - Actual pro-rata cost validation happens in calcDeliveryPath
     * - This is acceptable per instructor: "All which is tested is validity of the paths"
     * <p>
     * Protected to allow subclasses to override matching logic.
     *
     * @param medDispatchModels     List of delivery requests (model)
     * @param dronesSnapshot        Pre-fetched drone data
     * @param dfspSnapshot          Pre-fetched drone-for-service-point data
     * @param servicePointsSnapshot Pre-fetched service point data
     * @return List of drone IDs that can handle ALL deliveries (AND semantics)
     */
    protected List<Integer> findSuitableDronesInternal(
            List<MedDispatchRec> medDispatchModels,
            List<Drone> dronesSnapshot,
            List<DroneForServicePoint> dfspSnapshot,
            List<ServicePoint> servicePointsSnapshot) {

        List<Integer> suitableDrones = new ArrayList<>();

        if (medDispatchModels == null || medDispatchModels.isEmpty()) {
            return suitableDrones;
        }

        // Check each drone against ALL deliveries (AND semantics)
        for (Drone drone : dronesSnapshot) {
            boolean thisDroneWorks = true;
            for (MedDispatchRec medDispatchRec : medDispatchModels) {
                if (!drone.matchMedDispatchRec(medDispatchRec, dfspSnapshot, servicePointsSnapshot)) {
                    thisDroneWorks = false;
                    break;
                }
            }
            if (thisDroneWorks) {
                suitableDrones.add(drone.getId());
            }
        }

        return suitableDrones;
    }

    private MedDispatchRec convertToModel(MedDispatchRecDto dto) {
        boolean heating, cooling;
        float capacity, maxCost;

        if (dto == null) return null;
        if (dto.getRequirements().getHeating() != null) {
            heating = dto.getRequirements().getHeating();
        } else {
            heating = false;
        }

        if (dto.getRequirements().getCooling() != null) {
            cooling = dto.getRequirements().getCooling();
        } else {
            cooling = false;
        }

        capacity = dto.getRequirements().getCapacity();

        if (dto.getRequirements().getMaxCost() != null) {
            maxCost = dto.getRequirements().getMaxCost();
        } else {
            maxCost = Float.POSITIVE_INFINITY;
        }

        return new MedDispatchRec(
                dto.getId(),
                dto.getDate(),
                dto.getTime(),
                new Requirements(
                        heating,
                        cooling,
                        capacity,
                        maxCost
                ),
                new Position(
                        dto.getDelivery().getLongitude(),
                        dto.getDelivery().getLatitude()
                )
        );
    }

    /**
     * Order deliveries for a given drone starting from the first service point
     * using a greedy nearest-next heuristic based on Euclidean distance.
     * <p>
     * Per instructor clarification (Nov 23, 2025): "There is no rule how you deliver your stuff"
     * Any permutation of the input deliveries is acceptable. This method reorders them
     * to minimize travel distance using a greedy approach.
     */
    private List<MedDispatchRec> orderDeliveriesForDrone(List<MedDispatchRec> medDispatchModels,
                                                         List<ServicePoint> servicePointsSnapshot) {
        if (medDispatchModels == null || medDispatchModels.isEmpty()) return medDispatchModels;
        if (servicePointsSnapshot == null || servicePointsSnapshot.isEmpty()) return medDispatchModels;

        // For now, pick the first service point as starting point
        Position currentPos = servicePointsSnapshot.get(0).position();
        if (currentPos == null) return medDispatchModels;

        List<MedDispatchRec> ordered = new ArrayList<>();
        Set<Integer> usedIds = new HashSet<>();

        while (ordered.size() < medDispatchModels.size()) {
            MedDispatchRec best = null;
            BigDecimal bestDist = null;

            for (MedDispatchRec rec : medDispatchModels) {
                if (rec == null || rec.delivery() == null) continue;
                if (usedIds.contains(rec.id())) continue;

                BigDecimal d = DistanceUtils.euclideanDistance(currentPos, rec.delivery());
                if (best == null || d.compareTo(bestDist) < 0) {
                    best = rec;
                    bestDist = d;
                }
            }

            if (best == null) break; // nothing else reachable / valid

            ordered.add(best);
            usedIds.add(best.id());
            currentPos = best.delivery();
        }

        // Fallback: if something went wrong, return original
        return ordered.isEmpty() ? medDispatchModels : ordered;
    }

    public ResponseEntity<DeliveryPathReturnStructure> calcDeliveryPath(List<MedDispatchRecDto> medDispatchRequests, String strategy) {
        log.debug("calcDeliveryPath called with {} requests", medDispatchRequests != null ? medDispatchRequests.size() : 0);
        if (medDispatchRequests == null || medDispatchRequests.isEmpty()) {
            log.debug("No medDispatchRequests provided, returning empty result");
            return ResponseEntity.ok(new DeliveryPathReturnStructure(0f, 0, Collections.emptyList()));
        }
        try {
            List<Drone> dronesSnapshot = fetchAllDrones();
            List<ServicePoint> servicePointsSnapshot = fetchAllServicePoints();
            List<RestrictedArea> restrictedAreasSnapshot = fetchAllRestrictedAreas();
            List<DroneForServicePoint> dfspSnapshot = fetchAllDronesForServicePoints();
            log.debug("Fetched {} drones, {} service points, {} restricted areas, {} drone-for-service-point mappings",
                    dronesSnapshot != null ? dronesSnapshot.size() : 0,
                    servicePointsSnapshot != null ? servicePointsSnapshot.size() : 0,
                    restrictedAreasSnapshot != null ? restrictedAreasSnapshot.size() : 0,
                    dfspSnapshot != null ? dfspSnapshot.size() : 0);

            if (dronesSnapshot == null || dronesSnapshot.isEmpty() ||
                servicePointsSnapshot == null || servicePointsSnapshot.isEmpty()) {
                return ResponseEntity.ok(new DeliveryPathReturnStructure(0f, 0, Collections.emptyList()));
            }

            List<MedDispatchRec> medDispatchModels = medDispatchRequests.stream()
                    .map(this::convertToModel)
                    .toList();
            log.debug("Converted medDispatchRequests to {} MedDispatchRec models", medDispatchModels.size());

            // Patch: assign default date if null to avoid groupingBy NPE
            List<MedDispatchRec> medDispatchModelsWithDate = medDispatchModels.stream()
                    .map(rec -> rec == null || rec.date() != null ? rec :
                            new MedDispatchRec(rec.id(), LocalDate.now(), rec.time(), rec.requirements(), rec.delivery()))
                    .toList();
            Map<LocalDate, List<MedDispatchRec>> deliveriesByDate = medDispatchModelsWithDate.stream()
                    .collect(Collectors.groupingBy(MedDispatchRec::date, HashMap::new, Collectors.toList()));

            List<DronePath> dronePaths = new ArrayList<>();
            float totalCost = 0f;
            int totalMoves = 0;

            // Process each date separately
            for (Map.Entry<LocalDate, List<MedDispatchRec>> dateEntry : deliveriesByDate.entrySet()) {
                log.debug("Processing date {} with {} deliveries", dateEntry.getKey(), dateEntry.getValue().size());
                List<MedDispatchRec> deliveriesForDate = dateEntry.getValue();
                List<MedDispatchRec> unassigned = new ArrayList<>(deliveriesForDate);

                // Keep allocating until all deliveries are assigned or no progress can be made
                while (!unassigned.isEmpty()) {
                    // Use findSuitableDronesInternal to get drones that can handle at least one delivery
                    // We check each delivery individually to find which drones can take it
                    Drone bestDrone = null;
                    float bestScore = Float.MAX_VALUE;

                    for (Drone drone : dronesSnapshot) {
                        // Check if drone can handle at least one unassigned delivery
                        // Use richer match that returns a reason string to aid debugging
                        boolean canHandleAny = false;
                        for (MedDispatchRec rec : unassigned) {
                            StringBuilder reason = new StringBuilder();
                            boolean ok = drone.matchMedDispatchRec(rec, dfspSnapshot, servicePointsSnapshot, reason);
                            if (ok) {
                                canHandleAny = true;
                                break;
                            } else {
                                log.debug("Drone {} cannot handle delivery {}: {}", drone.getId(), rec.id(), reason.toString());
                            }
                        }

                        if (!canHandleAny) continue;

                        // Score selection varies by strategy
                        float score = Float.MAX_VALUE;
                        String strat = strategy == null ? "min_cost" : strategy.trim();
                        try {
                            // Find the origin service point for this candidate drone (used for move estimates)
                            ServicePoint candidateOrigin = findOriginServicePointForDrone(drone.getId(), dfspSnapshot, servicePointsSnapshot);
                            if ("min_moves".equalsIgnoreCase(strat)) {
                                // Estimate total moves for this drone to cover deliveries it can handle
                                int est = 0;
                                for (MedDispatchRec rec : unassigned) {
                                    StringBuilder r2 = new StringBuilder();
                                    if (drone.matchMedDispatchRec(rec, dfspSnapshot, servicePointsSnapshot, r2)) {
                                        if (candidateOrigin != null && candidateOrigin.position() != null && rec.delivery() != null) {
                                            est += estimateMovesForDistance(candidateOrigin.position(), rec.delivery());
                                        }
                                    }
                                }
                                score = (float) est;
                            } else if ("balanced".equalsIgnoreCase(strat)) {
                                // Combine cost and estimated moves with simple weighting
                                int est = 0;
                                for (MedDispatchRec rec : unassigned) {
                                    StringBuilder r2 = new StringBuilder();
                                    if (drone.matchMedDispatchRec(rec, dfspSnapshot, servicePointsSnapshot, r2)) {
                                        if (candidateOrigin != null && candidateOrigin.position() != null && rec.delivery() != null) {
                                            est += estimateMovesForDistance(candidateOrigin.position(), rec.delivery());
                                        }
                                    }
                                }
                                float costPerMove = drone.getCapability() != null ? drone.getCapability().costPerMove() : 1.0f;
                                score = costPerMove * 100.0f + est; // weight cost higher
                            } else {
                                // default: min_cost
                                score = drone.getCapability() != null ? drone.getCapability().costPerMove() : 1.0f;
                            }
                        } catch (Exception e) {
                            // Fallback to cost per move if anything goes wrong
                            try {
                                score = drone.getCapability() != null ? drone.getCapability().costPerMove() : 1.0f;
                            } catch (Exception ex) {
                                score = 1.0f;
                            }
                        }
                        if (score < bestScore) {
                            bestScore = score;
                            bestDrone = drone;
                        }
                    }

                    if (bestDrone == null) {
                        log.warn("No suitable drone found for remaining {} deliveries", unassigned.size());
                        break;
                    }

                    int droneId = bestDrone.getId();
                    float droneCapacity = bestDrone.getCapability().capacity();
                    int maxMoves = bestDrone.getCapability().maxMoves();
                    float costPerMove = bestDrone.getCapability().costPerMove();
                    float costInitial = bestDrone.getCapability().costInitial();
                    float costFinal = bestDrone.getCapability().costFinal();

                    // Find service point for this drone
                    ServicePoint originSP = findOriginServicePointForDrone(droneId, dfspSnapshot, servicePointsSnapshot);
                    if (originSP == null || originSP.position() == null) {
                        log.warn("No service point found for drone {}", droneId);
                        // Remove this drone from consideration and try again
                        final int removeDroneId = droneId;
                        dronesSnapshot = dronesSnapshot.stream()
                                .filter(d -> !d.getId().equals(removeDroneId))
                                .collect(Collectors.toList());
                        continue;
                    }

                    // Get deliveries this drone can handle using matchMedDispatchRec (same as getSuitableDrones)
                    final Drone selectedDrone = bestDrone;
                    List<MedDispatchRec> droneCanHandle = new ArrayList<>();
                    for (MedDispatchRec rec : unassigned) {
                        StringBuilder reason = new StringBuilder();
                        if (selectedDrone.matchMedDispatchRec(rec, dfspSnapshot, servicePointsSnapshot, reason)) {
                            droneCanHandle.add(rec);
                        } else {
                            log.debug("Selected drone {} cannot handle delivery {}: {}", selectedDrone.getId(), rec.id(), reason.toString());
                        }
                    }

                    if (droneCanHandle.isEmpty()) {
                        // This drone can't handle any remaining deliveries, remove it
                        final int removeDroneId = droneId;
                        dronesSnapshot = dronesSnapshot.stream()
                                .filter(d -> !d.getId().equals(removeDroneId))
                                .collect(Collectors.toList());
                        continue;
                    }

                    // Select a capacity-feasible subset for this trip (greedy packing)
                    List<MedDispatchRec> tripDeliveries = selectDeliveriesForTrip(droneCanHandle, droneCapacity, originSP);
                    if (tripDeliveries == null || tripDeliveries.isEmpty()) {
                        final int removeDroneId = droneId;
                        dronesSnapshot = dronesSnapshot.stream()
                                .filter(d -> !d.getId().equals(removeDroneId))
                                .collect(Collectors.toList());
                        continue;
                    }

                    // Order deliveries for this drone starting from the origin service point
                    List<MedDispatchRec> orderedTripDeliveries = greedyOrderDeliveries(tripDeliveries, originSP.position());

                    // Use multi-stop A* from Drone.aStarSearch to compute full path
                    FlightPath fullPath = selectedDrone.aStarSearch(orderedTripDeliveries, originSP, restrictedAreasSnapshot);
                    if (fullPath == null || fullPath.getStops() == null || fullPath.getStops().isEmpty()) {
                        log.debug("aStarSearch returned no path for drone {}", droneId);
                        final int removeDroneId = droneId;
                        dronesSnapshot = dronesSnapshot.stream()
                                .filter(d -> !d.getId().equals(removeDroneId))
                                .collect(Collectors.toList());
                        continue;
                    }

                    int fullMoves = fullPath.getTotalMoves() != null ? fullPath.getTotalMoves() : 0;
                    if (maxMoves > 0 && fullMoves > maxMoves) {
                        log.debug("Full multi-stop path for drone {} exceeds maxMoves ({} > {}), rejecting", droneId, fullMoves, maxMoves);
                        final int removeDroneId = droneId;
                        dronesSnapshot = dronesSnapshot.stream()
                                .filter(d -> !d.getId().equals(removeDroneId))
                                .collect(Collectors.toList());
                        continue;
                    }

                    List<Delivery> droneDeliveries = new ArrayList<>();
                    List<Position> allStops = fullPath.getStops();
                    List<Integer> segmentEnds = fullPath.getSegmentEndIndices();

                    if (segmentEnds == null || segmentEnds.isEmpty()) {
                        log.debug("Multi-stop path for drone {} has no segment end indices, rejecting", droneId);
                        final int removeDroneId = droneId;
                        dronesSnapshot = dronesSnapshot.stream()
                                .filter(d -> !d.getId().equals(removeDroneId))
                                .collect(Collectors.toList());
                        continue;
                    }

                    if (segmentEnds.size() != orderedTripDeliveries.size()) {
                        log.debug("Segment end count {} does not match deliveries {} for drone {}", segmentEnds.size(), orderedTripDeliveries.size(), droneId);
                    }

                    // Build one Delivery per requested delivery using segmentEndIndices
                    for (int i = 0; i < segmentEnds.size() && i < orderedTripDeliveries.size(); i++) {
                        int endIdx = segmentEnds.get(i);
                        int startIdx = (i == 0) ? 0 : segmentEnds.get(i - 1);
                        if (startIdx < 0) startIdx = 0;
                        if (endIdx >= allStops.size()) endIdx = allStops.size() - 1;

                        FlightPath fp = new FlightPath();
                        fp.getStops().addAll(allStops.subList(startIdx, endIdx + 1));
                        fp.setTotalMoves(Math.max(0, fp.getStops().size() - 1));

                        Integer deliveryId = orderedTripDeliveries.get(i).id();
                        droneDeliveries.add(new Delivery(deliveryId, fp));
                    }

                    // Optional: create separate return segment as a Delivery with null ID
                    if (!segmentEnds.isEmpty() && !allStops.isEmpty()) {
                        int lastSegEnd = segmentEnds.get(segmentEnds.size() - 1);
                        if (lastSegEnd < allStops.size() - 1) {
                            int startIdx = lastSegEnd;
                            int endIdx = allStops.size() - 1;
                            FlightPath returnFp = new FlightPath();
                            returnFp.getStops().addAll(allStops.subList(startIdx, endIdx + 1));
                            returnFp.setTotalMoves(Math.max(0, returnFp.getStops().size() - 1));
                            droneDeliveries.add(new Delivery(null, returnFp));
                        }
                    }

                    if (!droneDeliveries.isEmpty()) {
                        DronePath dronePath = new DronePath(droneId, droneDeliveries);
                        dronePaths.add(dronePath);

                        int droneMoves = droneDeliveries.stream()
                                .mapToInt(d -> d.getFlightPath().getTotalMoves() != null ? d.getFlightPath().getTotalMoves() : 0)
                                .sum();

                        // Compute drone cost from final paths only: initial + moves * costPerMove + final
                        float droneCost = costInitial + droneMoves * costPerMove + costFinal;
                        totalCost += droneCost;
                        totalMoves += droneMoves;

                        Set<Integer> assignedIds = orderedTripDeliveries.stream()
                                .map(MedDispatchRec::id)
                                .collect(Collectors.toSet());
                        unassigned.removeIf(r -> assignedIds.contains(r.id()));

                        log.debug("Drone {} completed {} deliveries, {} moves, cost {}",
                                droneId, droneDeliveries.size(), droneMoves, droneCost);
                    } else {
                        final int removeDroneId = droneId;
                        dronesSnapshot = dronesSnapshot.stream()
                                .filter(d -> !d.getId().equals(removeDroneId))
                                .collect(Collectors.toList());
                    }
                }

                if (!unassigned.isEmpty()) {
                    log.warn("Deliveries could not be assigned to any drone: {}",
                            unassigned.stream().map(MedDispatchRec::id).toList());
                }
            }

            if (dronePaths.isEmpty()) {
                log.debug("No deliveries assigned to any drone, returning empty result");
                return ResponseEntity.ok(new DeliveryPathReturnStructure(0f, 0, Collections.emptyList()));
            }

            // Validate that total cost doesn't exceed combined maxCost budget
            float totalMaxCostBudget = 0f;
            boolean hasInfiniteBudget = false;
            for (MedDispatchRec rec : medDispatchModels) {
                if (rec == null || rec.requirements() == null) continue;
                Float maxCost = rec.requirements().getMaxCost();
                if (maxCost == null || Float.isInfinite(maxCost)) {
                    hasInfiniteBudget = true;
                    break;
                }
                totalMaxCostBudget += maxCost;
            }

            if (!hasInfiniteBudget && totalCost > totalMaxCostBudget) {
                log.warn("Total cost {} exceeds combined maxCost budget {}, returning empty result",
                        totalCost, totalMaxCostBudget);
                return ResponseEntity.ok(new DeliveryPathReturnStructure(0f, 0, Collections.emptyList()));
            }

            DeliveryPathReturnStructure deliveryPathReturnStructure =
                    new DeliveryPathReturnStructure(totalCost, totalMoves, dronePaths);
            log.debug("Returning deliveryPathReturnStructure: totalCost={}, totalMoves={}, dronePaths={}",
                    totalCost, totalMoves, dronePaths.size());
            return ResponseEntity.ok(deliveryPathReturnStructure);
        } catch (Exception e) {
            log.error("Exception in calcDeliveryPath: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new DeliveryPathReturnStructure(0f, 0, Collections.emptyList()));
        }
    }

    /**
     * Order deliveries using greedy nearest-neighbor heuristic.
     * Starts from given position and always picks closest unvisited delivery.
     */
    private List<MedDispatchRec> greedyOrderDeliveries(List<MedDispatchRec> deliveries, Position startPos) {
        if (deliveries == null || deliveries.isEmpty()) return deliveries;

        List<MedDispatchRec> ordered = new ArrayList<>();
        Set<Integer> visited = new HashSet<>();
        Position currentPos = startPos;

        while (ordered.size() < deliveries.size()) {
            MedDispatchRec nearest = null;
            double nearestDist = Double.MAX_VALUE;

            for (MedDispatchRec rec : deliveries) {
                if (visited.contains(rec.id())) continue;
                if (rec.delivery() == null) continue;

                double dist = DistanceUtils.euclideanDistance(currentPos, rec.delivery()).doubleValue();
                if (dist < nearestDist) {
                    nearestDist = dist;
                    nearest = rec;
                }
            }

            if (nearest == null) break;

            ordered.add(nearest);
            visited.add(nearest.id());
            currentPos = nearest.delivery();
        }

        return ordered;
    }

    /**
     * Calculate flight path between two positions avoiding restricted areas.
     * Uses A* search algorithm with straight-line Euclidean fallback.
     */
    private FlightPath calculateSingleFlightPath(Position from, Position to, List<RestrictedArea> restrictedAreas) {
        if (from == null || to == null) return null;

        // Try A* search first
        try {
            Graph<PositionNode> graph =
                    new PositionGraph(restrictedAreas);
            PositionScorer scorer =
                    new PositionScorer();
            RouteFinder<PositionNode> finder =
                    new RouteFinder<>(graph, scorer, scorer);

            PositionNode startNode =
                    new PositionNode("START", from);
            PositionNode goalNode =
                    new PositionNode("GOAL", to);

            List<PositionNode> pathNodes = finder.findRoute(startNode, goalNode);

            FlightPath flightPath = new FlightPath();
            for (PositionNode node : pathNodes) {
                flightPath.getStops().add(node.getPosition());
            }
            flightPath.setTotalMoves(Math.max(0, flightPath.getStops().size() - 1));

            return flightPath;
        } catch (Exception e) {
            log.warn("A* search failed from {} to {}: {}, using Euclidean fallback", from, to, e.getMessage());
        }

        // Fallback: straight-line path using Euclidean distance
        return calculateStraightLinePath(from, to);
    }

    /**
     * Calculate a straight-line flight path using discrete moves along valid angles.
     * This is a fallback when A* search fails. Avoids restricted areas.
     */
    private FlightPath calculateStraightLinePath(Position from, Position to) {
        if (from == null || to == null) return null;

        // Fetch restricted areas for validation
        List<RestrictedArea> restrictedAreas = fetchAllRestrictedAreas();

        FlightPath flightPath = new FlightPath();
        flightPath.getStops().add(from);

        Position current = from;
        int maxIterations = 10000; // Safety limit
        int iterations = 0;

        while (iterations < maxIterations) {
            iterations++;

            // Check if we're close enough to destination (within one step)
            double distToGoal = DistanceUtils.euclideanDistance(current, to).doubleValue();
            if (distToGoal < 0.00015) {
                // Close enough, add destination if not in restricted area
                if (!isPositionInRestrictedArea(to, restrictedAreas)) {
                    flightPath.getStops().add(to);
                }
                break;
            }

            // Find the best angle to move towards the goal
            double targetAngle = Math.toDegrees(Math.atan2(
                    to.getLongitude().doubleValue() - current.getLongitude().doubleValue(),
                    to.getLatitude().doubleValue() - current.getLatitude().doubleValue()
            ));
            if (targetAngle < 0) targetAngle += 360;

            // Try angles starting from the ideal one, then alternating left/right
            Position next = null;
            double[] angleOffsets = {0, 22.5, -22.5, 45, -45, 67.5, -67.5, 90, -90, 112.5, -112.5, 135, -135, 157.5, -157.5, 180};

            for (double offset : angleOffsets) {
                double tryAngle = targetAngle + offset;
                if (tryAngle < 0) tryAngle += 360;
                if (tryAngle >= 360) tryAngle -= 360;

                // Snap to nearest valid angle (multiples of 22.5)
                double snappedAngle = Math.round(tryAngle / 22.5) * 22.5;
                if (snappedAngle >= 360) snappedAngle = 0;

                // Compute next position
                Position candidate = computeNextPositionInternal(current, snappedAngle);
                if (candidate == null) continue;

                // Check if this position is in a restricted area or path crosses restricted area
                if (isPositionInRestrictedArea(candidate, restrictedAreas)) continue;
                if (doesPathCrossRestrictedArea(current, candidate, restrictedAreas)) continue;

                next = candidate;
                break;
            }

            if (next == null) {
                log.error("Failed to find valid next position avoiding restricted areas");
                break;
            }

            flightPath.getStops().add(next);
            current = next;
        }

        if (iterations >= maxIterations) {
            log.warn("Straight-line path exceeded max iterations");
        }

        flightPath.setTotalMoves(Math.max(0, flightPath.getStops().size() - 1));
        return flightPath;
    }

    /**
     * Check if a position is inside any restricted area.
     */
    private boolean isPositionInRestrictedArea(Position pos, List<RestrictedArea> restrictedAreas) {
        if (pos == null || restrictedAreas == null || restrictedAreas.isEmpty()) return false;

        for (RestrictedArea area : restrictedAreas) {
            if (area == null || area.getPositions() == null || area.getPositions().size() < 3) continue;
            if (pointInPolygon(pos, area.getPositions())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check if the path between two positions crosses any restricted area.
     */
    private boolean doesPathCrossRestrictedArea(Position from, Position to, List<RestrictedArea> restrictedAreas) {
        if (from == null || to == null || restrictedAreas == null || restrictedAreas.isEmpty()) return false;

        // Sample points along the path
        final int samples = 5;
        for (int i = 1; i < samples; i++) {
            double t = (double) i / samples;
            BigDecimal lng = from.getLongitude().add(
                    to.getLongitude().subtract(from.getLongitude()).multiply(BigDecimal.valueOf(t))
            );
            BigDecimal lat = from.getLatitude().add(
                    to.getLatitude().subtract(from.getLatitude()).multiply(BigDecimal.valueOf(t))
            );
            Position sample = new Position(lng, lat);
            if (isPositionInRestrictedArea(sample, restrictedAreas)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Point-in-polygon test using ray casting algorithm.
     */
    private boolean pointInPolygon(Position p, List<Position> vertices) {
        if (p == null || vertices == null || vertices.size() < 3) return false;

        boolean inside = false;
        int n = vertices.size();
        double x = p.getLongitude().doubleValue();
        double y = p.getLatitude().doubleValue();

        for (int i = 0, j = n - 1; i < n; j = i++) {
            Position vi = vertices.get(i);
            Position vj = vertices.get(j);
            double xi = vi.getLongitude().doubleValue();
            double yi = vi.getLatitude().doubleValue();
            double xj = vj.getLongitude().doubleValue();
            double yj = vj.getLatitude().doubleValue();

            boolean intersect = ((yi > y) != (yj > y)) &&
                    (x < (xj - xi) * (y - yi) / (yj - yi + 1e-12) + xi);
            if (intersect) inside = !inside;
        }
        return inside;
    }

    /**
     * Internal method to compute next position for a given angle.
     * Mirrors the logic from computeNextPosition but without ResponseEntity wrapper.
     */
    private Position computeNextPositionInternal(Position start, double angleDegrees) {
        if (start == null) return null;

        try {
            double rad = Math.toRadians(angleDegrees);
            BigDecimal step = new BigDecimal("0.00015");
            MathContext mc = MathContext.DECIMAL64;

            BigDecimal newLat = step.multiply(BigDecimal.valueOf(Math.cos(rad)), mc)
                    .add(start.getLatitude());
            BigDecimal newLng = step.multiply(BigDecimal.valueOf(Math.sin(rad)), mc)
                    .divide(BigDecimal.valueOf(Math.cos(Math.toRadians(start.getLatitude().doubleValue()))), mc)
                    .add(start.getLongitude());

            return new Position(newLng, newLat);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Estimate number of moves needed between two positions (straight-line heuristic).
     */
    private int estimateMovesForDistance(Position from, Position to) {
        if (from == null || to == null) return Integer.MAX_VALUE;
        double distance = DistanceUtils.euclideanDistance(from, to).doubleValue();
        return (int) Math.ceil(distance / 0.00015);
    }

    @Override
    public ResponseEntity<GeoJsonLineStringDto> calcDeliveryPathAsGeoJson(List<MedDispatchRecDto> medDispatchRequests) {
        // Always return 200, use empty GeoJSON for invalid/no-match scenarios
        GeoJsonLineStringDto emptyGeoJson = new GeoJsonLineStringDto(List.of());

        // Validate input
        if (medDispatchRequests == null || medDispatchRequests.isEmpty()) {
            return ResponseEntity.ok(emptyGeoJson);
        }

        // Fetch all data once (single snapshot)
        List<Drone> dronesSnapshot = fetchAllDrones();
        List<ServicePoint> servicePointsSnapshot = fetchAllServicePoints();
        List<RestrictedArea> restrictedAreasSnapshot = fetchAllRestrictedAreas();
        List<DroneForServicePoint> dfspSnapshot = fetchAllDronesForServicePoints();

        // Convert DTOs to models ONCE
        List<MedDispatchRec> medDispatchModels = medDispatchRequests.stream().map(this::convertToModel).toList();

        // Find suitable drones using the SAME data snapshot
        List<Integer> suitableDroneIDs = findSuitableDronesInternal(
                medDispatchModels,
                dronesSnapshot,
                dfspSnapshot,
                servicePointsSnapshot
        );

        if (suitableDroneIDs == null || suitableDroneIDs.isEmpty()) {
            return ResponseEntity.ok(emptyGeoJson);
        }
        if (dronesSnapshot == null || dronesSnapshot.isEmpty() ||
                servicePointsSnapshot == null || servicePointsSnapshot.isEmpty()) {
            return ResponseEntity.ok(emptyGeoJson);
        }

        // Use the first suitable drone
        Integer firstDroneId = suitableDroneIDs.get(0);
        Drone selectedDrone = dronesSnapshot.stream()
                .filter(d -> d.getId() != null && d.getId().equals(firstDroneId))
                .findFirst().orElse(null);
        if (selectedDrone == null) {
            return ResponseEntity.ok(emptyGeoJson);
        }

        // Find origin service point
        ServicePoint originServicePoint = findOriginServicePointForDrone(selectedDrone.getId(), dfspSnapshot, servicePointsSnapshot);
        if (originServicePoint == null || originServicePoint.position() == null) {
            return ResponseEntity.ok(emptyGeoJson);
        }

        // Order deliveries for this drone (greedy nearest-neighbor)
        List<MedDispatchRec> orderedDeliveries = orderDeliveriesForDrone(medDispatchModels, servicePointsSnapshot);

        // Build the full path with trip-level tracking so we can "recharge" at the service point.
        List<Position> allStops = new ArrayList<>();
        Position originPos = originServicePoint.position();
        allStops.add(originPos);

        // Drone capability values
        float droneCapacity = selectedDrone.getCapability() != null ? selectedDrone.getCapability().capacity() : Float.POSITIVE_INFINITY;
        int maxMoves = selectedDrone.getCapability() != null ? selectedDrone.getCapability().maxMoves() : Integer.MAX_VALUE;
        // float costPerMove = selectedDrone.getCapability() != null ? selectedDrone.getCapability().costPerMove() : 0f;
        // float costInitial = selectedDrone.getCapability() != null ? selectedDrone.getCapability().costInitial() : 0f;
        // float costFinal = selectedDrone.getCapability() != null ? selectedDrone.getCapability().costFinal() : 0f;

        int remainingMoves = maxMoves;
        float currentLoad = 0f;
        Position currentPos = originPos;
        // int tripCount = 0; // number of departures from origin

        try {
        for (MedDispatchRec delivery : orderedDeliveries) {
            if (delivery == null || delivery.delivery() == null) continue;

            float deliveryWeight = delivery.requirements() != null ? delivery.requirements().getCapacity() : 0f;

            // If current load would exceed capacity, return to origin to unload and recharge
            if (currentLoad + deliveryWeight > droneCapacity && currentLoad > 0) {
                FlightPath returnPath = calculateSingleFlightPath(currentPos, originPos, restrictedAreasSnapshot);
                if (returnPath == null) {
                    return ResponseEntity.ok(emptyGeoJson);
                }
                int returnMoves = returnPath.getTotalMoves() != null ? returnPath.getTotalMoves() : Integer.MAX_VALUE;
                if (returnMoves > remainingMoves) {
                    // Can't get back to base with remaining moves => invalid
                    return ResponseEntity.ok(emptyGeoJson);
                }

                // Append return stops (skip duplicate)
                List<Position> stops = returnPath.getStops();
                if (stops != null && !stops.isEmpty()) {
                    if (!Objects.equals(allStops.get(allStops.size() - 1), stops.get(0))) {
                        allStops.addAll(stops);
                    } else {
                        allStops.addAll(stops.subList(1, stops.size()));
                    }
                }

                // Account for cost (kept externally if needed) and recharge
                remainingMoves = maxMoves;
                currentLoad = 0f;
                currentPos = originPos;
            }

            // If we're at origin and about to depart, increment trip count
            // if (Objects.equals(currentPos, originPos)) {
            //     tripCount++;
            // }

            // Calculate path from current position to delivery
            FlightPath toDeliveryPath = calculateSingleFlightPath(currentPos, delivery.delivery(), restrictedAreasSnapshot);
            if (toDeliveryPath == null || toDeliveryPath.getStops() == null || toDeliveryPath.getStops().isEmpty()) {
                return ResponseEntity.ok(emptyGeoJson);
            }

            int movesToDelivery = toDeliveryPath.getTotalMoves() != null ? toDeliveryPath.getTotalMoves() : Integer.MAX_VALUE;

            // Estimate return moves after delivery to ensure trip feasibility
            int estimatedReturnMoves = estimateMovesForDistance(delivery.delivery(), originPos);

            if (movesToDelivery + estimatedReturnMoves > remainingMoves) {
                // Not enough moves left: if we're not at origin, try returning first then retry
                if (!Objects.equals(currentPos, originPos)) {
                    FlightPath returnPath = calculateSingleFlightPath(currentPos, originPos, restrictedAreasSnapshot);
                    if (returnPath == null) return ResponseEntity.ok(emptyGeoJson);
                    int returnMoves = returnPath.getTotalMoves() != null ? returnPath.getTotalMoves() : Integer.MAX_VALUE;
                    if (returnMoves > remainingMoves) return ResponseEntity.ok(emptyGeoJson);

                    // Append return stops
                    List<Position> rStops = returnPath.getStops();
                    if (rStops != null && !rStops.isEmpty()) {
                        if (!Objects.equals(allStops.get(allStops.size() - 1), rStops.get(0))) {
                            allStops.addAll(rStops);
                        } else {
                            allStops.addAll(rStops.subList(1, rStops.size()));
                        }
                    }

                    // Recharge
                    remainingMoves = maxMoves;
                    currentLoad = 0f;
                    currentPos = originPos;

                    // departing from origin for this delivery
                    // tripCount++;

                    // Recompute path from origin
                    toDeliveryPath = calculateSingleFlightPath(currentPos, delivery.delivery(), restrictedAreasSnapshot);
                    if (toDeliveryPath == null || toDeliveryPath.getStops() == null || toDeliveryPath.getStops().isEmpty()) {
                        return ResponseEntity.ok(emptyGeoJson);
                    }
                    movesToDelivery = toDeliveryPath.getTotalMoves() != null ? toDeliveryPath.getTotalMoves() : Integer.MAX_VALUE;
                    estimatedReturnMoves = estimateMovesForDistance(delivery.delivery(), originPos);
                    if (movesToDelivery + estimatedReturnMoves > remainingMoves) return ResponseEntity.ok(emptyGeoJson);
                } else {
                    // We're at origin already but don't have enough moves - fail
                    return ResponseEntity.ok(emptyGeoJson);
                }
            }

            // Append to-delivery stops (skip duplicate start)
            List<Position> dStops = toDeliveryPath.getStops();
            if (!dStops.isEmpty()) {
                if (!Objects.equals(allStops.get(allStops.size() - 1), dStops.get(0))) {
                    allStops.addAll(dStops);
                } else {
                    allStops.addAll(dStops.subList(1, dStops.size()));
                }
            }

            // Add hover (duplicate position) to indicate delivery
            if (!dStops.isEmpty()) {
                allStops.add(dStops.get(dStops.size() - 1));
            }

            // Update tracking
            remainingMoves -= movesToDelivery;
            currentLoad += deliveryWeight;
            currentPos = delivery.delivery();
        }
        } catch (Exception e) {
            log.error("Exception while building GeoJSON path: {}", e.getMessage(), e);
            return ResponseEntity.ok(emptyGeoJson);
        }

        // Final return to base if needed
        Position last = allStops.get(allStops.size() - 1);
        if (!last.equals(originPos)) {
            FlightPath returnPath = calculateSingleFlightPath(last, originPos, restrictedAreasSnapshot);
            if (returnPath == null || returnPath.getStops() == null || returnPath.getStops().isEmpty()) {
                return ResponseEntity.ok(emptyGeoJson);
            }
            int returnMoves = returnPath.getTotalMoves() != null ? returnPath.getTotalMoves() : Integer.MAX_VALUE;
            if (returnMoves > remainingMoves) {
                // Not enough moves to return
                return ResponseEntity.ok(emptyGeoJson);
            }

            List<Position> stops = returnPath.getStops();
            if (!Objects.equals(allStops.get(allStops.size() - 1), stops.get(0))) {
                allStops.addAll(stops);
            } else {
                allStops.addAll(stops.subList(1, stops.size()));
            }

            // Recharge after final return (semantic)
            remainingMoves = maxMoves;
        }

        if (allStops.size() < 2) {
            return ResponseEntity.ok(emptyGeoJson);
        }

        // Build coordinates for GeoJsonLineStringDto
        List<List<Number>> coordinates = new ArrayList<>();
        for (Position p : allStops) {
            coordinates.add(List.of(p.getLongitude(), p.getLatitude()));
        }
        GeoJsonLineStringDto.Geometry geometry = new GeoJsonLineStringDto.Geometry(coordinates);
        GeoJsonLineStringDto.Properties properties = new GeoJsonLineStringDto.Properties(selectedDrone.getId(), allStops.size() - 1);
        GeoJsonLineStringDto.Feature feature = new GeoJsonLineStringDto.Feature(properties, geometry);
        GeoJsonLineStringDto geoJsonDto = new GeoJsonLineStringDto(List.of(feature));

        return ResponseEntity.ok(geoJsonDto);
    }

    @Override
    public ResponseEntity<GeoJsonLineStringDto> calcMultiDroneDeliveryPathAsGeoJson(List<MedDispatchRecDto> medDispatchRequests, String strategy) {
        // Use existing planner to compute assignments, then convert DronePaths -> one LineString feature per drone
        GeoJsonLineStringDto emptyGeoJson = new GeoJsonLineStringDto(List.of());

        try {
            ResponseEntity<DeliveryPathReturnStructure> planResp = calcDeliveryPath(medDispatchRequests, strategy);
            if (planResp == null || planResp.getStatusCodeValue() != 200 || planResp.getBody() == null) {
                return ResponseEntity.ok(emptyGeoJson);
            }

            DeliveryPathReturnStructure plan = planResp.getBody();
            if (plan.getDronePaths() == null || plan.getDronePaths().isEmpty()) {
                return ResponseEntity.ok(emptyGeoJson);
            }

            List<GeoJsonLineStringDto.Feature> features = new ArrayList<>();

            for (DronePath dp : plan.getDronePaths()) {
                if (dp == null || dp.getDeliveries() == null || dp.getDeliveries().isEmpty()) continue;

                List<List<Number>> coordinates = new ArrayList<>();
                int totalMoves = 0;

                for (Delivery del : dp.getDeliveries()) {
                    if (del == null || del.getFlightPath() == null || del.getFlightPath().getStops() == null) continue;
                    // append each stop as [lng, lat]
                    for (Position p : del.getFlightPath().getStops()) {
                        if (p == null) continue;
                        coordinates.add(List.of(p.getLongitude(), p.getLatitude()));
                    }
                    Integer fm = del.getFlightPath().getTotalMoves();
                    if (fm != null) totalMoves += fm;
                }

                if (coordinates.size() < 2) continue;

                GeoJsonLineStringDto.Geometry geometry = new GeoJsonLineStringDto.Geometry(coordinates);
                GeoJsonLineStringDto.Properties properties = new GeoJsonLineStringDto.Properties(dp.getDroneId(), totalMoves);
                GeoJsonLineStringDto.Feature feature = new GeoJsonLineStringDto.Feature(properties, geometry);
                features.add(feature);
            }

            if (features.isEmpty()) return ResponseEntity.ok(emptyGeoJson);

            GeoJsonLineStringDto out = new GeoJsonLineStringDto(features);
            return ResponseEntity.ok(out);
        } catch (Exception e) {
            log.error("Exception in calcMultiDroneDeliveryPathAsGeoJson: {}", e.getMessage(), e);
            return ResponseEntity.ok(emptyGeoJson);
        }
    }

    /**
     * Resolve the origin ServicePoint for a given drone based on DroneForServicePoint mappings.
     * If multiple service points host the same drone, return the first one. If none found,
     * fall back to the first service point in the snapshot (for robustness).
     */
    private ServicePoint findOriginServicePointForDrone(Integer droneId,
                                                        List<DroneForServicePoint> dfspSnapshot,
                                                        List<ServicePoint> servicePointsSnapshot) {
        if (droneId == null || dfspSnapshot == null || servicePointsSnapshot == null
                || dfspSnapshot.isEmpty() || servicePointsSnapshot.isEmpty()) {
            return servicePointsSnapshot == null || servicePointsSnapshot.isEmpty()
                    ? null
                    : servicePointsSnapshot.get(0);
        }

        // Convert droneId to String for comparison with DroneAvailability.id() (which is String from API)
        String droneIdStr = droneId.toString();

        for (DroneForServicePoint dfsp : dfspSnapshot) {
            if (dfsp == null) continue;
            List<DroneAvailability> dronesAtSp = dfsp.drones();
            if (dronesAtSp == null) continue;

            // Check if this service point has the drone we're looking for
            boolean present = dronesAtSp.stream()
                    .anyMatch(da -> da != null && Objects.equals(da.id(), droneIdStr));

            if (present) {
                Integer spId = dfsp.servicePointId();
                if (spId == null) continue;

                // Find the matching ServicePoint by ID
                ServicePoint matchingSp = servicePointsSnapshot.stream()
                        .filter(sp -> sp != null && sp.id() != null && sp.id().equals(spId))
                        .findFirst()
                        .orElse(null);

                if (matchingSp != null) {
                    return matchingSp;
                }
                // If spId doesn't match any ServicePoint, continue to next DroneForServicePoint
            }
        }

        // Fallback: use first service point if no drone-specific match found
        return servicePointsSnapshot.get(0);
    }

    @Override
    public ResponseEntity<List<Integer>> queryAsPath(String attributeName, String attributeValue) {
        // CW2 spec: Always return 200, use empty list for invalid input
        if (attributeName == null || attributeValue == null) return ResponseEntity.ok(Collections.emptyList());

        // Fetch drones ONCE at the start
        List<Drone> dronesSnapshot = fetchAllDrones();
        if (dronesSnapshot == null || dronesSnapshot.isEmpty()) return ResponseEntity.ok(Collections.emptyList());

        List<Integer> result = new ArrayList<>();

        for (Drone drone : dronesSnapshot) {
            try {
                Object val = extractAttributeValue(drone, attributeName);
                if (val == null) continue;
                if (matchesAsStringOrNumber(val, "=", attributeValue)) {
                    result.add(drone.getId());
                }
            } catch (Exception e) {
                // ignore and continue
            }
        }
        return ResponseEntity.ok(result);
    }

    @Override
    public ResponseEntity<List<String>> query(List<QueryAttributeDto> queryAttributes) {
        // CW2 spec: Always return 200, use empty list for invalid input
        if (queryAttributes == null) return ResponseEntity.ok(Collections.emptyList());

        // Fetch drones ONCE at the start
        List<Drone> dronesSnapshot = fetchAllDrones();
        if (dronesSnapshot == null || dronesSnapshot.isEmpty()) return ResponseEntity.ok(Collections.emptyList());

        List<String> result = new ArrayList<>();

        outer:
        for (Drone drone : dronesSnapshot) {
            for (QueryAttributeDto qa : queryAttributes) {
                if (qa == null || qa.getAttribute() == null || qa.getOperator() == null || qa.getValue() == null) {
                    // Skip this drone if query attribute is invalid, continue to next drone
                    continue outer;
                }
                Object val;
                try {
                    val = extractAttributeValue(drone, qa.getAttribute().trim());
                } catch (Exception e) {
                    // treat as non-matching
                    continue outer;
                }
                if (val == null) {
                    continue outer; // attribute missing -> no match
                }

                boolean ok = matchesAsStringOrNumber(val, qa.getOperator(), qa.getValue());
                if (!ok) continue outer; // AND semantics: all must match
            }
            result.add(drone.getId().toString());
        }

        return ResponseEntity.ok(result);
    }

    // Helper to extract nested attributes like "capability.capacity" or top-level names like "name" or "id"
    private Object extractAttributeValue(Drone drone, String attributeName) {
        if (attributeName == null || drone == null) return null;
        // allow either dot notation or simple name; also support path-style input like 'capacity' which may be inside capability
        // first try top-level getter
        attributeName = attributeName
            .replaceAll("\\s", "").toLowerCase(); 
            
        switch (attributeName) {
            case "name":
                return drone.getName();
            case "id":
                return drone.getId();
            default:
                // try capability.<attr>
                Capability cap = drone.getCapability();
                if (cap != null) {
                    switch (attributeName) {
                        case "heating":
                            return cap.heating();
                        case "cooling":
                            return cap.cooling();
                        case "capacity":
                            return cap.capacity();
                        case "maxMoves":
                        case "maxmoves":
                        case "max_moves":
                            return cap.maxMoves();
                        case "costPerMove":
                        case "costpermove":
                            return cap.costPerMove();
                        case "costInitial":
                        case "costinitial":
                            return cap.costInitial();
                        case "costFinal":
                        case "costfinal":
                            return cap.costFinal();
                    }
                }
                // if attribute contains dot like capability.capacity
                if (attributeName.contains(".")) {
                    String[] parts = attributeName.split("\\.");
                    if (parts.length == 2 && "capability".equals(parts[0])) {
                        return extractAttributeValue(drone, parts[1]);
                    }
                }
                break;
        }
        return null;
    }

    /**
     * Match values using string or numerical comparison.
     * Protected to allow subclasses to implement custom comparison operators.
     * <p>
     * Per instructor clarification: "It never said you should compare strings...
     * You have to treat the input as strings and then map accordingly
     * (in this case string -> double)."
     * <p>
     * This means: Parse expectedStr as the same type as actualVal before comparing.
     * Example: If actualVal is 8.0 (double) and expectedStr is "8", parse "8" as double
     * and compare 8.0 == 8.0 (TRUE), not "8.0" == "8" (FALSE).
     */
    protected boolean matchesAsStringOrNumber(Object actualVal, String operator, String expectedStr) {
        if (actualVal == null || operator == null || expectedStr == null) return false;

        // Normalize operator: remove ALL invisible characters
        operator = operator
                .replaceAll("\\s", "")
                .replaceAll("\\p{C}", "");

        // Boolean values
        if (actualVal instanceof Boolean act) {
            boolean exp = Boolean.parseBoolean(expectedStr.trim());
            return switch (operator) {
                case "="  -> act == exp;
                case "!=" -> act != exp;
                default   -> false;
            };
        }

        // Numeric values
        if (actualVal instanceof Number num) {
            double act = num.doubleValue();

            System.out.println("DEBUG: comparing act=" + act + " operator=" + operator + " exp=" + expectedStr);

            double exp;
            final double EPS = 1e-9;

            try {
                exp = Double.parseDouble(expectedStr.trim());
            } catch (NumberFormatException e) {
                return false;
            }

            return switch (operator) {
                case "="  -> Math.abs(act - exp) < EPS;
                case "!=" -> Math.abs(act - exp) >= EPS;
                case "<"  -> act < exp && Math.abs(act - exp) >= EPS;
                case ">"  -> act > exp && Math.abs(act - exp) >= EPS;
                default   -> false;
            };
        }

        return false;
    }


    /**
     * Select deliveries for a single trip based on capacity constraint.
     * Uses greedy packing strategy: adds deliveries until capacity is reached.
     * <p>
     * Multiple trips per day are supported: SP → A → SP → B → SP is valid
     * (same drone, same day, multiple trips).
     *
     * @param available     List of available deliveries to choose from
     * @param droneCapacity Maximum capacity of the drone
     * @param servicePoint  Starting service point (for proximity ordering)
     * @return List of deliveries selected for this trip (fits in capacity)
     */
    private List<MedDispatchRec> selectDeliveriesForTrip(
            List<MedDispatchRec> available,
            float droneCapacity,
            ServicePoint servicePoint
    ) {
        if (available == null || available.isEmpty()) {
            return Collections.emptyList();
        }

        List<MedDispatchRec> selected = new ArrayList<>();
        float currentLoad = 0.0f;

        // Sort by proximity to service point (greedy nearest-first)
        List<MedDispatchRec> sorted = new ArrayList<>(available);
        Position spPos = servicePoint != null ? servicePoint.position() : null;

        if (spPos != null) {
            sorted.sort(Comparator.comparingDouble(d -> {
                Position dPos = d.delivery();
                if (dPos == null) return Double.MAX_VALUE;
                // Use existing distanceTo method
                ResponseEntity<BigDecimal> distResponse = distanceTo(new PositionsDto(spPos, dPos));
                if (distResponse == null || distResponse.getBody() == null) return Double.MAX_VALUE;
                return distResponse.getBody().doubleValue();
            }));
        }

        // Greedy packing: add deliveries until capacity full
        for (MedDispatchRec delivery : sorted) {
            if (delivery.requirements() == null) continue;

            Float weight = delivery.requirements().getCapacity();
            if (weight == null) continue;

            // Check if this delivery fits in remaining capacity
            if (currentLoad + weight <= droneCapacity) {
                selected.add(delivery);
                currentLoad += weight;

                // Stop if at capacity (100% full)
                if (currentLoad >= droneCapacity) {
                    break;
                }
            }
        }

        return selected;
    }

}
