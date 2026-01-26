package ilp.ilp_cw.ilp_1_2.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import ilp.ilp_cw.ilp_1_2.dto.DroneMoveRqstDto;
import ilp.ilp_cw.ilp_1_2.dto.GeoJsonLineStringDto;
import ilp.ilp_cw.ilp_1_2.dto.LocationRegionDto;
import ilp.ilp_cw.ilp_1_2.dto.MedDispatchRecDto;
import ilp.ilp_cw.ilp_1_2.dto.QueryAttributeDto;
import ilp.ilp_cw.ilp_1_2.dto.PositionsDto;
import ilp.ilp_cw.ilp_1_2.model.DeliveryPathReturnStructure;
import ilp.ilp_cw.ilp_1_2.model.Drone;
import ilp.ilp_cw.ilp_1_2.model.Position;
import ilp.ilp_cw.ilp_1_2.service.DroneService;

import java.math.BigDecimal;
import java.util.List;

/**
 * REST controller exposing drone-related endpoints used by the coursework API.
 * <p>
 * Delegates business logic to {@link DroneService} and maps incoming JSON bodies
 * to DTO objects.
 */


@RestController
@RequestMapping("/api/v1")
public class DroneController {

    private final DroneService droneService;

    public DroneController(DroneService droneService) {
        this.droneService = droneService;
    }

    /**
     * Returns the student UID for the application.
     *
     * @return UID string
     */
    @GetMapping("/uid")
    public String uid() {
        return droneService.getUID();
    }

    /**
     * Computes the Euclidean distance between two positions provided in the request body.
     *
     * @param positionsDto wrapper containing position1 and position2
     * @return 200 with the distance as {@link BigDecimal} when successful, 400 otherwise
     */
    @PostMapping("/distanceTo")
    public ResponseEntity<BigDecimal> distanceTo(@RequestBody PositionsDto positionsDto) {
        try {
            return droneService.distanceTo(positionsDto);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Checks whether two positions are closer than the configured threshold.
     *
     * @param positionsDto wrapper containing position1 and position2
     * @return 200 with true/false when successful, 400 otherwise
     */
    @PostMapping("/isCloseTo")
    public ResponseEntity<Boolean> isCloseTo(@RequestBody PositionsDto positionsDto) {
        try {
            return droneService.isCloseTo(positionsDto);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Computes the next position for a drone given a start and an angle.
     *
     * @param droneMoveRqstDto request DTO containing start position and angle in degrees
     * @return 200 with the computed {@link Position} when successful, 400 otherwise
     */
    @PostMapping("/nextPosition")
    public ResponseEntity<Position> nextPosition(@RequestBody DroneMoveRqstDto droneMoveRqstDto) {
        try {
            return droneService.computeNextPosition(droneMoveRqstDto);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Determines whether a supplied position lies inside the supplied region.
     *
     * @param locationRegionDto wrapper containing the current position and the region
     * @return 200 with true/false when successful, 400 otherwise
     */
    @PostMapping("/isInRegion")
    public ResponseEntity<Boolean> isInRegion(@RequestBody LocationRegionDto locationRegionDto) {
        try {
            return droneService.isInRegion(locationRegionDto);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // ===========================
    // CW2: Static Queries
    // ===========================

    /**
     * Returns a list of drone IDs that have the specified cooling capability.
     * CW2 spec: Always returns 200 (service layer handles invalid input with empty list)
     * <p>
     * Per instructor clarification: "you can define the @GetMapping() and in there have a
     * placeholder which is Boolean -> then anything which can converted to a Boolean will do..."
     * Using Boolean (wrapper) allows Spring to handle case-insensitive conversion:
     * "true", "True", "TRUE", "false", "False", "FALSE" all work.
     * <p>
     * Per instructor clarification: "as this is part of the path it would be mandatory to specify it"
     * Calling /dronesWithCooling/ without state will return 404 (no endpoint match).
     * The automarker will always provide a state value.
     *
     * @param state true if cooling is required, false otherwise (case-insensitive, REQUIRED)
     * @return 200 with list of drone IDs (empty list if invalid input)
     */
    @GetMapping("/dronesWithCooling/{cooling}")
    public ResponseEntity<List<Integer>> dronesWithCooling(@PathVariable("cooling") Boolean state) {
        return droneService.dronesWithCooling(state);
    }

    @GetMapping("/dronesWithHeating/{heating}")
    public ResponseEntity<List<Integer>> dronesWithHeating(@PathVariable("heating") Boolean state) {
        return droneService.dronesWithHeating(state);
    }

    /**
     * Returns the JSON object for a single drone with the given id.
     * Note: Returns 404 if the drone ID does not exist (CW2 spec exception).
     * Returns 200 with empty list for invalid ID format (NumberFormatException).
     *
     * @param id the drone ID to retrieve
     * @return 200 with Drone details, 404 if not found
     */
    @GetMapping("/droneDetails/{id}")
    public ResponseEntity<Drone> droneDetails(@PathVariable String id) {
        try {
            return droneService.getDrone(Integer.parseInt(id));
        } catch (NumberFormatException e) {
            // Invalid ID format - could return 404 or handle as "not found"
            // Service expects integer, so delegate to service with invalid value
            return droneService.getDrone(-1); // Will return 400 from service for invalid ID
        }
    }

    // ===========================
    // CW2: Dynamic Queries
    // ===========================

    /**
     * Query drones by a single attribute using path parameters.
     * CW2 spec: Always returns 200 (service layer handles invalid input with empty list)
     *
     * @param attributeName  the attribute to query (e.g., "capacity", "cooling")
     * @param attributeValue the value to match
     * @return 200 with list of matching drone IDs (empty list if no matches)
     */
    @GetMapping("/queryAsPath/{attribute-name}/{attribute-value}")
    public ResponseEntity<List<Integer>> queryAsPath(@PathVariable("attribute-name") String attributeName,
                                                     @PathVariable("attribute-value") String attributeValue) {
        return droneService.queryAsPath(attributeName, attributeValue);
    }

    /**
     * Query drones by multiple attributes using a POST body with AND semantics.
     * CW2 spec: Always returns 200 (service layer handles invalid input with empty list)
     *
     * @param query list of query attribute DTOs
     * @return 200 with list of matching drone IDs (empty list if no matches)
     */
    @PostMapping("/query")
    public ResponseEntity<List<String>> query(@RequestBody List<QueryAttributeDto> query) {
        return droneService.query(query);
    }

    // ===========================
    // CW2: Drone Availability Queries
    // ===========================

    /**
     * Query which drones are available and suitable for a set of medical dispatch requests.
     * Returns drone IDs that can fulfill ALL dispatch requirements (AND semantics).
     * CW2 spec: Always returns 200 (service layer handles invalid input with empty list)
     *
     * @param medDispatchRequests list of medical dispatch request DTOs
     * @return 200 with list of suitable drone IDs (empty list if no matches or invalid input)
     */
    @PostMapping("/queryAvailableDrones")
    public ResponseEntity<List<Integer>> queryAvailableDrones(@RequestBody List<MedDispatchRecDto> medDispatchRequests) {
        return droneService.getSuitableDrones(medDispatchRequests);
    }

    // ===========================
    // CW2: Calculate Delivery Path (A* Pathfinding)
    // ===========================

    /**
     * Calculate delivery paths for medical dispatches using A* pathfinding.
     * Returns the complete delivery plan with drone assignments, flight paths, total cost, and total moves.
     * Enforces maxMoves constraints and respects no-fly zones.
     * CW2 spec: Always returns 200 (service layer handles failures with empty structure)
     *
     * @param medDispatchRequests list of medical dispatch request DTOs
     * @return 200 with delivery path structure (empty structure if no valid path)
     */
    @PostMapping("/calcDeliveryPath")
    public ResponseEntity<DeliveryPathReturnStructure> calcDeliveryPath(
            @RequestBody List<MedDispatchRecDto> medDispatchRequests,
            @RequestParam(value = "strategy", required = false) String strategy
    ) {
        return droneService.calcDeliveryPath(medDispatchRequests, strategy);
    }

    /**
     * Calculate delivery path as GeoJSON for visualization.
     * Guarantees all deliveries can be made with a single drone in a single sequence.
     * Returns a valid GeoJSON LineString structure viewable at https://geojson.io
     * CW2 spec: Always returns 200 (service layer handles failures with empty GeoJSON)
     *
     * @param medDispatchRequests list of medical dispatch request DTOs
     * @return 200 with GeoJSON structure (empty FeatureCollection if no valid path)
     */
    @PostMapping("/calcDeliveryPathAsGeoJson")
    public ResponseEntity<GeoJsonLineStringDto> calcDeliveryPathAsGeoJson(@RequestBody List<MedDispatchRecDto> medDispatchRequests) {
        return droneService.calcDeliveryPathAsGeoJson(medDispatchRequests);
    }

    /**
     * Calculate multi-drone delivery paths as GeoJSON for visualization.
     * Returns a FeatureCollection containing one LineString per assigned drone.
     * Accepts optional strategy query param forwarded to the planner.
     */
    @PostMapping("/calcMultiDroneDeliveryPathAsGeoJson")
    public ResponseEntity<GeoJsonLineStringDto> calcMultiDroneDeliveryPathAsGeoJson(
            @RequestBody List<MedDispatchRecDto> medDispatchRequests,
            @RequestParam(value = "strategy", required = false) String strategy
    ) {
        return droneService.calcMultiDroneDeliveryPathAsGeoJson(medDispatchRequests, strategy);
    }

}
