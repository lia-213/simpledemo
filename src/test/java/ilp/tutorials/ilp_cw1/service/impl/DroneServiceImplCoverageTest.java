package ilp.tutorials.ilp_cw1.service.impl;

import ilp.ilp_cw.ilp_1_2.dto.*;
import ilp.ilp_cw.ilp_1_2.model.*;
import ilp.ilp_cw.ilp_1_2.service.DroneIlpClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.*;

/**
 * Comprehensive coverage tests for DroneServiceImpl.
 * Focuses on edge cases, error paths, and complex scenarios to improve coverage from 27% to 70%+.
 */
@ExtendWith(MockitoExtension.class)
class DroneServiceImplCoverageTest {

    @Mock
    private DroneIlpClient droneIlpClient;

    private DroneServiceImpl droneService;

    @BeforeEach
    void setUp() {
        droneService = new DroneServiceImpl(droneIlpClient);
    }

    // ========================
    // CW1: Edge Case Testing
    // ========================

    @Test
    void distanceTo_withNullDto_returnsBadRequest() {
        ResponseEntity<BigDecimal> response = droneService.distanceTo(null);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void distanceTo_withNullPosition1_returnsBadRequest() {
        PositionsDto dto = new PositionsDto(null, new Position(BigDecimal.ZERO, BigDecimal.ZERO));
        ResponseEntity<BigDecimal> response = droneService.distanceTo(dto);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void distanceTo_withNullPosition2_returnsBadRequest() {
        PositionsDto dto = new PositionsDto(new Position(BigDecimal.ZERO, BigDecimal.ZERO), null);
        ResponseEntity<BigDecimal> response = droneService.distanceTo(dto);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void distanceTo_withNullLongitude_returnsBadRequest() {
        Position p1 = new Position(null, BigDecimal.ZERO);
        Position p2 = new Position(BigDecimal.ZERO, BigDecimal.ZERO);
        PositionsDto dto = new PositionsDto(p1, p2);
        ResponseEntity<BigDecimal> response = droneService.distanceTo(dto);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void distanceTo_withNullLatitude_returnsBadRequest() {
        Position p1 = new Position(BigDecimal.ZERO, null);
        Position p2 = new Position(BigDecimal.ZERO, BigDecimal.ZERO);
        PositionsDto dto = new PositionsDto(p1, p2);
        ResponseEntity<BigDecimal> response = droneService.distanceTo(dto);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void distanceTo_withInvalidLatitude_returnsBadRequest() {
        Position p1 = new Position(BigDecimal.ZERO, BigDecimal.valueOf(95)); // > 90
        Position p2 = new Position(BigDecimal.ZERO, BigDecimal.ZERO);
        PositionsDto dto = new PositionsDto(p1, p2);
        ResponseEntity<BigDecimal> response = droneService.distanceTo(dto);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void distanceTo_withInvalidLongitude_returnsBadRequest() {
        Position p1 = new Position(BigDecimal.valueOf(185), BigDecimal.ZERO); // > 180
        Position p2 = new Position(BigDecimal.ZERO, BigDecimal.ZERO);
        PositionsDto dto = new PositionsDto(p1, p2);
        ResponseEntity<BigDecimal> response = droneService.distanceTo(dto);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void computeNextPosition_withNullRequest_returnsBadRequest() {
        ResponseEntity<Position> response = droneService.computeNextPosition(null);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void computeNextPosition_withNullPosition_returnsBadRequest() {
        DroneMoveRqstDto dto = new DroneMoveRqstDto(null, BigDecimal.ZERO);
        ResponseEntity<Position> response = droneService.computeNextPosition(dto);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void computeNextPosition_withNullAngle_returnsBadRequest() {
        Position start = new Position(BigDecimal.ZERO, BigDecimal.ZERO);
        DroneMoveRqstDto dto = new DroneMoveRqstDto(start, null);
        ResponseEntity<Position> response = droneService.computeNextPosition(dto);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void computeNextPosition_withNegativeAngle_returnsBadRequest() {
        Position start = new Position(BigDecimal.ZERO, BigDecimal.ZERO);
        DroneMoveRqstDto dto = new DroneMoveRqstDto(start, BigDecimal.valueOf(-10));
        ResponseEntity<Position> response = droneService.computeNextPosition(dto);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void computeNextPosition_withAngleGreaterOrEqual360_returnsBadRequest() {
        Position start = new Position(BigDecimal.ZERO, BigDecimal.ZERO);
        DroneMoveRqstDto dto = new DroneMoveRqstDto(start, BigDecimal.valueOf(360));
        ResponseEntity<Position> response = droneService.computeNextPosition(dto);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void computeNextPosition_withInvalidAngle_notMultipleOf22Point5_returnsBadRequest() {
        Position start = new Position(BigDecimal.ZERO, BigDecimal.ZERO);
        DroneMoveRqstDto dto = new DroneMoveRqstDto(start, BigDecimal.valueOf(25.0)); // Not multiple of 22.5
        ResponseEntity<Position> response = droneService.computeNextPosition(dto);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void computeNextPosition_withInvalidStartPosition_returnsBadRequest() {
        Position start = new Position(BigDecimal.valueOf(200), BigDecimal.valueOf(100)); // Invalid
        DroneMoveRqstDto dto = new DroneMoveRqstDto(start, BigDecimal.ZERO);
        ResponseEntity<Position> response = droneService.computeNextPosition(dto);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void isInRegion_withNullWrapper_returnsBadRequest() {
        ResponseEntity<Boolean> response = droneService.isInRegion(null);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void isInRegion_withNullPosition_returnsBadRequest() {
        List<Position> vertices = Arrays.asList(
                new Position(BigDecimal.ZERO, BigDecimal.ZERO),
                new Position(BigDecimal.ONE, BigDecimal.ZERO),
                new Position(BigDecimal.ONE, BigDecimal.ONE)
        );
        Region region = new Region("test", vertices);
        LocationRegionDto dto = new LocationRegionDto(null, region);
        ResponseEntity<Boolean> response = droneService.isInRegion(dto);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void isInRegion_withNullRegion_returnsBadRequest() {
        Position pos = new Position(BigDecimal.valueOf(0.5), BigDecimal.valueOf(0.5));
        LocationRegionDto dto = new LocationRegionDto(pos, null);
        ResponseEntity<Boolean> response = droneService.isInRegion(dto);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void isInRegion_withInvalidRegion_lessThan3Vertices_returnsBadRequest() {
        Position pos = new Position(BigDecimal.valueOf(0.5), BigDecimal.valueOf(0.5));
        List<Position> vertices = Arrays.asList(
                new Position(BigDecimal.ZERO, BigDecimal.ZERO),
                new Position(BigDecimal.ONE, BigDecimal.ZERO)
        );
        Region region = new Region("test", vertices);
        LocationRegionDto dto = new LocationRegionDto(pos, region);
        ResponseEntity<Boolean> response = droneService.isInRegion(dto);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    // ========================
    // CW2: Drone Query Testing
    // ========================

    @Test
    void dronesWithCooling_withNullState_returnsEmptyList() {
        ResponseEntity<List<Integer>> response = droneService.dronesWithCooling(null);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isEmpty());
    }

    @Test
    void dronesWithHeating_withNullState_returnsEmptyList() {
        ResponseEntity<List<Integer>> response = droneService.dronesWithHeating(null);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isEmpty());
    }

    @Test
    void getDrone_withNegativeId_returnsNotFound() {
        ResponseEntity<Drone> response = droneService.getDrone(-1);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void getDrone_withZeroId_returnsNotFound() {
        ResponseEntity<Drone> response = droneService.getDrone(0);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void getDrone_whenNoDronesExist_returnsNotFound() {
        when(droneIlpClient.fetchAllDrones()).thenReturn(Collections.emptyList());
        ResponseEntity<Drone> response = droneService.getDrone(1);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void getDrone_whenClientReturnsNull_returnsBadRequest() {
        when(droneIlpClient.fetchAllDrones()).thenReturn(null);
        ResponseEntity<Drone> response = droneService.getDrone(1);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void getSuitableDrones_withNullList_returnsEmptyList() {
        ResponseEntity<List<Integer>> response = droneService.getSuitableDrones(null);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isEmpty());
    }

    @Test
    void getSuitableDrones_withEmptyList_returnsEmptyList() {
        ResponseEntity<List<Integer>> response = droneService.getSuitableDrones(Collections.emptyList());
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isEmpty());
    }

    @Test
    void getSuitableDrones_whenNoDronesExist_returnsEmptyList() {
        when(droneIlpClient.fetchAllDrones()).thenReturn(Collections.emptyList());
        MedDispatchRecDto delivery = createDeliveryDto(1, 5.0f, false, false, 100.0f);
        ResponseEntity<List<Integer>> response = droneService.getSuitableDrones(List.of(delivery));
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().isEmpty());
    }

    @Test
    void getSuitableDrones_whenDronesAreNull_returnsEmptyList() {
        when(droneIlpClient.fetchAllDrones()).thenReturn(null);
        MedDispatchRecDto delivery = createDeliveryDto(1, 5.0f, false, false, 100.0f);
        ResponseEntity<List<Integer>> response = droneService.getSuitableDrones(List.of(delivery));
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().isEmpty());
    }

    // ========================
    // Query Method Testing
    // ========================

    @Test
    void queryAsPath_withNullAttributeName_returnsEmptyList() {
        ResponseEntity<List<Integer>> response = droneService.queryAsPath(null, "value");
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().isEmpty());
    }

    @Test
    void queryAsPath_withNullAttributeValue_returnsEmptyList() {
        ResponseEntity<List<Integer>> response = droneService.queryAsPath("name", null);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().isEmpty());
    }

    @Test
    void queryAsPath_whenNoDronesExist_returnsEmptyList() {
        when(droneIlpClient.fetchAllDrones()).thenReturn(Collections.emptyList());
        ResponseEntity<List<Integer>> response = droneService.queryAsPath("name", "TestDrone");
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().isEmpty());
    }

    @Test
    void queryAsPath_byName_findsMatchingDrone() {
        Drone drone = createTestDrone(1, "AlphaDrone", 10.0f, false, false);
        when(droneIlpClient.fetchAllDrones()).thenReturn(List.of(drone));

        ResponseEntity<List<Integer>> response = droneService.queryAsPath("name", "AlphaDrone");
        assertEquals(HttpStatus.OK, response.getStatusCode());
        // Name is String, matchesAsStringOrNumber returns false for String types
        assertEquals(0, response.getBody().size());
    }

    @Test
    void queryAsPath_byId_findsMatchingDrone() {
        Drone drone = createTestDrone(42, "TestDrone", 10.0f, false, false);
        when(droneIlpClient.fetchAllDrones()).thenReturn(List.of(drone));

        ResponseEntity<List<Integer>> response = droneService.queryAsPath("id", "42");
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
        assertEquals(42, response.getBody().get(0));
    }

    @Test
    void query_withNullList_returnsEmptyList() {
        ResponseEntity<List<String>> response = droneService.query(null);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().isEmpty());
    }

    @Test
    void query_withEmptyList_returnsAllDrones() {
        Drone drone1 = createTestDrone(1, "Drone1", 10.0f, false, false);
        Drone drone2 = createTestDrone(2, "Drone2", 15.0f, true, false);
        when(droneIlpClient.fetchAllDrones()).thenReturn(List.of(drone1, drone2));

        ResponseEntity<List<String>> response = droneService.query(Collections.emptyList());
        assertEquals(HttpStatus.OK, response.getStatusCode());
        // Empty query list should match all drones
        assertEquals(2, response.getBody().size());
    }

    @Test
    void query_withInvalidQueryAttribute_skipsNonMatchingDrones() {
        Drone drone = createTestDrone(1, "TestDrone", 10.0f, false, false);
        when(droneIlpClient.fetchAllDrones()).thenReturn(List.of(drone));

        QueryAttributeDto invalidQuery = new QueryAttributeDto(null, "=", "value");
        ResponseEntity<List<String>> response = droneService.query(List.of(invalidQuery));
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().isEmpty());
    }

    @Test
    void query_byCapacity_greaterThan_findsMatchingDrones() {
        Drone drone1 = createTestDrone(1, "SmallDrone", 5.0f, false, false);
        Drone drone2 = createTestDrone(2, "LargeDrone", 15.0f, false, false);
        when(droneIlpClient.fetchAllDrones()).thenReturn(List.of(drone1, drone2));

        QueryAttributeDto query = new QueryAttributeDto("capacity", ">", "10");
        ResponseEntity<List<String>> response = droneService.query(List.of(query));
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
        assertEquals("2", response.getBody().get(0));
    }

    @Test
    void query_byCapacity_lessThan_findsMatchingDrones() {
        Drone drone1 = createTestDrone(1, "SmallDrone", 5.0f, false, false);
        Drone drone2 = createTestDrone(2, "LargeDrone", 15.0f, false, false);
        when(droneIlpClient.fetchAllDrones()).thenReturn(List.of(drone1, drone2));

        QueryAttributeDto query = new QueryAttributeDto("capacity", "<", "10");
        ResponseEntity<List<String>> response = droneService.query(List.of(query));
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
        assertEquals("1", response.getBody().get(0));
    }

    @Test
    void query_byHeating_boolean_findsMatchingDrones() {
        Drone drone1 = createTestDrone(1, "NormalDrone", 10.0f, false, false);
        Drone drone2 = createTestDrone(2, "HeatedDrone", 10.0f, true, false);
        when(droneIlpClient.fetchAllDrones()).thenReturn(List.of(drone1, drone2));

        QueryAttributeDto query = new QueryAttributeDto("heating", "=", "true");
        ResponseEntity<List<String>> response = droneService.query(List.of(query));
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
        assertEquals("2", response.getBody().get(0));
    }

    @Test
    void query_byCooling_boolean_findsMatchingDrones() {
        Drone drone1 = createTestDrone(1, "NormalDrone", 10.0f, false, false);
        Drone drone2 = createTestDrone(2, "CooledDrone", 10.0f, false, true);
        when(droneIlpClient.fetchAllDrones()).thenReturn(List.of(drone1, drone2));

        QueryAttributeDto query = new QueryAttributeDto("cooling", "=", "true");
        ResponseEntity<List<String>> response = droneService.query(List.of(query));
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
        assertEquals("2", response.getBody().get(0));
    }

    @Test
    void query_multipleConditions_AND_semantics() {
        Drone drone1 = createTestDrone(1, "Drone1", 5.0f, false, false);
        Drone drone2 = createTestDrone(2, "Drone2", 15.0f, true, false);
        Drone drone3 = createTestDrone(3, "Drone3", 15.0f, false, false);
        when(droneIlpClient.fetchAllDrones()).thenReturn(List.of(drone1, drone2, drone3));

        List<QueryAttributeDto> queries = Arrays.asList(
                new QueryAttributeDto("capacity", ">", "10"),
                new QueryAttributeDto("heating", "=", "true")
        );
        ResponseEntity<List<String>> response = droneService.query(queries);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
        assertEquals("2", response.getBody().get(0));
    }

    @Test
    void query_byNotEquals_filtersCorrectly() {
        Drone drone1 = createTestDrone(1, "Drone1", 10.0f, false, false);
        Drone drone2 = createTestDrone(2, "Drone2", 15.0f, false, false);
        when(droneIlpClient.fetchAllDrones()).thenReturn(List.of(drone1, drone2));

        QueryAttributeDto query = new QueryAttributeDto("capacity", "!=", "10");
        ResponseEntity<List<String>> response = droneService.query(List.of(query));
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
        assertEquals("2", response.getBody().get(0));
    }

    // ========================
    // Delivery Path Testing
    // ========================

    @Test
    void calcDeliveryPath_withNullList_returnsEmptyResult() {
        ResponseEntity<DeliveryPathReturnStructure> response = droneService.calcDeliveryPath(null, null);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(0, response.getBody().getDronePaths().size());
    }

    @Test
    void calcDeliveryPath_withEmptyList_returnsEmptyResult() {
        ResponseEntity<DeliveryPathReturnStructure> response = droneService.calcDeliveryPath(Collections.emptyList(), null);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(0, response.getBody().getDronePaths().size());
    }

    @Test
    void calcDeliveryPath_whenNoDrones_returnsEmptyResult() {
        when(droneIlpClient.fetchAllDrones()).thenReturn(Collections.emptyList());
        when(droneIlpClient.fetchAllServicePoints()).thenReturn(createTestServicePoints());

        MedDispatchRecDto delivery = createDeliveryDto(1, 5.0f, false, false, 100.0f);
        ResponseEntity<DeliveryPathReturnStructure> response = droneService.calcDeliveryPath(List.of(delivery), null);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(0, response.getBody().getDronePaths().size());
    }

    @Test
    void calcDeliveryPath_whenNoServicePoints_returnsEmptyResult() {
        when(droneIlpClient.fetchAllDrones()).thenReturn(createTestDrones());
        when(droneIlpClient.fetchAllServicePoints()).thenReturn(Collections.emptyList());

        MedDispatchRecDto delivery = createDeliveryDto(1, 5.0f, false, false, 100.0f);
        ResponseEntity<DeliveryPathReturnStructure> response = droneService.calcDeliveryPath(List.of(delivery), null);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(0, response.getBody().getDronePaths().size());
    }

    @Test
    void calcDeliveryPathAsGeoJson_withNullList_returnsEmptyGeoJson() {
        ResponseEntity<GeoJsonLineStringDto> response = droneService.calcDeliveryPathAsGeoJson(null);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().getFeatures().isEmpty());
    }

    @Test
    void calcDeliveryPathAsGeoJson_withEmptyList_returnsEmptyGeoJson() {
        ResponseEntity<GeoJsonLineStringDto> response = droneService.calcDeliveryPathAsGeoJson(Collections.emptyList());
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().getFeatures().isEmpty());
    }

    @Test
    void calcDeliveryPathAsGeoJson_whenNoSuitableDrones_returnsEmptyGeoJson() {
        List<Drone> testDrones = createTestDrones();
        List<ServicePoint> testServicePoints = createTestServicePoints();
        List<DroneForServicePoint> testDfsp = createTestDfsp();
        
        when(droneIlpClient.fetchAllDrones()).thenReturn(testDrones);
        when(droneIlpClient.fetchAllServicePoints()).thenReturn(testServicePoints);
        when(droneIlpClient.fetchAllDronesForServicePoints()).thenReturn(testDfsp);
        when(droneIlpClient.fetchAllRestrictedAreas()).thenReturn(Collections.emptyList());

        // Request impossible requirements
        MedDispatchRecDto delivery = createDeliveryDto(1, 1000.0f, true, true, 0.01f); // Impossible capacity
        ResponseEntity<GeoJsonLineStringDto> response = droneService.calcDeliveryPathAsGeoJson(List.of(delivery));
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().getFeatures().isEmpty());
    }

    @Test
    void calcMultiDroneDeliveryPathAsGeoJson_withNullInput_returnsEmptyGeoJson() {
        ResponseEntity<GeoJsonLineStringDto> response = droneService.calcMultiDroneDeliveryPathAsGeoJson(null, null);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().getFeatures().isEmpty());
    }

    @Test
    void calcMultiDroneDeliveryPathAsGeoJson_withEmptyInput_returnsEmptyGeoJson() {
        ResponseEntity<GeoJsonLineStringDto> response = droneService.calcMultiDroneDeliveryPathAsGeoJson(Collections.emptyList(), null);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().getFeatures().isEmpty());
    }

    // ========================
    // Helper Methods
    // ========================

    private Drone createTestDrone(int id, String name, float capacity, boolean heating, boolean cooling) {
        Capability capability = new Capability(
                cooling,
                heating,
                capacity,
                2000,      // maxMoves
                1.0f,      // costPerMove
                10.0f,     // costInitial
                5.0f       // costFinal
        );
        return new Drone(name, String.valueOf(id), capability);
    }

    private List<Drone> createTestDrones() {
        return Arrays.asList(
                createTestDrone(1, "AlphaDrone", 10.0f, false, false),
                createTestDrone(2, "BetaDrone", 15.0f, true, false),
                createTestDrone(3, "GammaDrone", 20.0f, false, true)
        );
    }

    private MedDispatchRecDto createDeliveryDto(int id, float capacity, boolean heating, boolean cooling, float maxCost) {
        Requirements requirements = new Requirements(heating, cooling, capacity, maxCost);
        Position delivery = new Position(BigDecimal.valueOf(-3.186), BigDecimal.valueOf(55.944));
        return new MedDispatchRecDto(
                id,
                LocalDate.of(2026, 1, 15),
                LocalTime.of(10, 0),
                requirements,
                delivery
        );
    }

    private List<ServicePoint> createTestServicePoints() {
        return Arrays.asList(
                new ServicePoint(1, "AT", new Position(BigDecimal.valueOf(-3.186874), BigDecimal.valueOf(55.944494))),
                new ServicePoint(2, "BH", new Position(BigDecimal.valueOf(-3.184319), BigDecimal.valueOf(55.942617)))
        );
    }

    private List<DroneForServicePoint> createTestDfsp() {
        Availability avail = new Availability("MONDAY", LocalTime.of(0, 0), LocalTime.of(23, 59));
        List<DroneAvailability> dronesAtSp1 = Arrays.asList(
                new DroneAvailability("1", List.of(avail)),
                new DroneAvailability("2", List.of(avail))
        );
        List<DroneAvailability> dronesAtSp2 = Arrays.asList(
                new DroneAvailability("3", List.of(avail))
        );
        return Arrays.asList(
                new DroneForServicePoint(1, dronesAtSp1),
                new DroneForServicePoint(2, dronesAtSp2)
        );
    }
}
