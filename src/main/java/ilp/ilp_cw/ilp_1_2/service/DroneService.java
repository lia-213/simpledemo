package ilp.ilp_cw.ilp_1_2.service;

import org.springframework.http.ResponseEntity;

import ilp.ilp_cw.ilp_1_2.dto.*;
import ilp.ilp_cw.ilp_1_2.model.DeliveryPathReturnStructure;
import ilp.ilp_cw.ilp_1_2.model.Drone;
import ilp.ilp_cw.ilp_1_2.model.Position;

import java.math.BigDecimal;
import java.util.List;

/**
 * Service interface exposing drone-related business operations.
 * <p>
 * Implementations perform validation and return appropriate {@link org.springframework.http.ResponseEntity}
 * instances containing the result or an error status (e.g. 400 Bad Request).
 */

public interface DroneService {

    /**
     * Compute the Euclidean distance between two positions.
     *
     * @param positionsDto wrapper containing position1 and position2
     * @return 200 with the distance as {@link BigDecimal} when successful, or 400 on invalid input
     */
    ResponseEntity<BigDecimal> distanceTo(PositionsDto positionsDto);

    /**
     * Check whether two positions are closer than the configured threshold.
     *
     * @param positionsDto wrapper containing position1 and position2
     * @return 200 with true/false when successful, or 400 on invalid input
     */
    ResponseEntity<Boolean> isCloseTo(PositionsDto positionsDto);

    /**
     * Compute the next position for a drone given a start and an angle in degrees.
     *
     * @param request request DTO containing start position and angle
     * @return 200 with the computed {@link Position} when successful, or 400 on invalid input
     */
    ResponseEntity<Position> computeNextPosition(DroneMoveRqstDto request);

    /**
     * Determine whether a supplied position lies inside a supplied region.
     *
     * @param wrapper wrapper containing the current position and the region
     * @return 200 with true/false when successful, or 400 on invalid input
     */
    ResponseEntity<Boolean> isInRegion(LocationRegionDto wrapper);

    /**
     * Returns the student UID for this implementation.
     *
     * @return a UID string
     */
    String getUID();

    ResponseEntity<List<Integer>> dronesWithCooling(Boolean state);

    ResponseEntity<Drone> getDrone(int id);

    ResponseEntity<List<Integer>> getSuitableDrones(List<MedDispatchRecDto> medDispatchRequests);

    ResponseEntity<DeliveryPathReturnStructure> calcDeliveryPath(List<MedDispatchRecDto> medDispatchRequests, String strategy);

    ResponseEntity<GeoJsonLineStringDto> calcDeliveryPathAsGeoJson(List<MedDispatchRecDto> medDispatchRequests);

    /**
     * Calculate multi-drone delivery paths as GeoJSON for visualization.
     * Returns a FeatureCollection with one LineString feature per assigned drone.
     * @param medDispatchRequests list of medical dispatch request DTOs
     * @param strategy optional strategy parameter forwarded to the planner
     * @return 200 with GeoJSON FeatureCollection (empty collection if no valid paths)
     */
    ResponseEntity<GeoJsonLineStringDto> calcMultiDroneDeliveryPathAsGeoJson(List<MedDispatchRecDto> medDispatchRequests, String strategy);

    ResponseEntity<List<Integer>> queryAsPath(String attributeName, String attributeValue);

    ResponseEntity<List<String>> query(List<QueryAttributeDto> queryAttributes);

    ResponseEntity<List<Integer>> dronesWithHeating(Boolean state);
}
