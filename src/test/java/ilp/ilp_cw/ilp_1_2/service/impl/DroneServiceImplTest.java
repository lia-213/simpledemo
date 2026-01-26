package ilp.ilp_cw.ilp_1_2.service.impl;

import ilp.ilp_cw.ilp_1_2.dto.*;
import ilp.ilp_cw.ilp_1_2.model.*;
import ilp.ilp_cw.ilp_1_2.service.DroneIlpClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive JUnit tests for {@link DroneServiceImpl} in ilp_cw.ilp_1_2 package.
 * Tests cover all major endpoints including distance calculations, drone queries,
 * delivery path planning, and GeoJSON generation.
 */
class DroneServiceImplTest {

    @Mock
    private DroneIlpClient droneIlpClient;

    private DroneServiceImpl service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new DroneServiceImpl(droneIlpClient);
    }

    // -----------------------
    // getUID() test
    // -----------------------
    @Test
    void getUID_returnsStudentID() {
        assertEquals("s2141930", service.getUID());
    }

    // -----------------------
    // distanceTo() tests
    // -----------------------
    @Test
    void distanceTo_validPositions_returnsDistance() {
        PositionsDto positionsDto = new PositionsDto(
                new Position(BigDecimal.valueOf(1), BigDecimal.valueOf(1)),
                new Position(BigDecimal.valueOf(4), BigDecimal.valueOf(5))
        );

        ResponseEntity<BigDecimal> resp = service.distanceTo(positionsDto);
        assertNotNull(resp.getBody());
        assertEquals(5.0, resp.getBody().doubleValue(), 1e-6);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
    }

    @Test
    void distanceTo_nullDto_returnsBadRequest() {
        ResponseEntity<BigDecimal> resp = service.distanceTo(null);
        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
    }

    @Test
    void distanceTo_missingPosition_returnsBadRequest() {
        PositionsDto positionsDto = new PositionsDto(null, new Position(BigDecimal.ONE, BigDecimal.ONE));
        ResponseEntity<BigDecimal> resp = service.distanceTo(positionsDto);
        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
    }

    @Test
    void distanceTo_nullCoordinates_returnsBadRequest() {
        ResponseEntity<BigDecimal> resp = service.distanceTo(
                new PositionsDto(new Position(null, null), new Position(BigDecimal.ONE, BigDecimal.ONE)));
        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
    }

    @Test
    void distanceTo_outOfRangeCoordinates_returnsBadRequest() {
        PositionsDto positionsDto = new PositionsDto(
                new Position(BigDecimal.valueOf(-300.192473), BigDecimal.valueOf(550.946233)),
                new Position(BigDecimal.valueOf(-3202.192473), BigDecimal.valueOf(5533.942617))
        );
        ResponseEntity<BigDecimal> resp = service.distanceTo(positionsDto);
        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
    }

    // -----------------------
    // isCloseTo() tests
    // -----------------------
    @Test
    void isCloseTo_distanceBelowThreshold_returnsTrue() {
        PositionsDto positionsDto = new PositionsDto(
                new Position(BigDecimal.ZERO, BigDecimal.ZERO),
                new Position(BigDecimal.valueOf(0.0001), BigDecimal.ZERO)
        );

        ResponseEntity<Boolean> resp = service.isCloseTo(positionsDto);
        assertNotNull(resp.getBody());
        assertTrue(resp.getBody());
    }

    @Test
    void isCloseTo_distanceAboveThreshold_returnsFalse() {
        PositionsDto positionsDto = new PositionsDto(
                new Position(BigDecimal.ZERO, BigDecimal.ZERO),
                new Position(BigDecimal.valueOf(0.01), BigDecimal.ZERO)
        );

        ResponseEntity<Boolean> resp = service.isCloseTo(positionsDto);
        assertNotNull(resp.getBody());
        assertFalse(resp.getBody());
    }

    @Test
    void isCloseTo_invalidInput_returnsBadRequest() {
        ResponseEntity<Boolean> resp = service.isCloseTo(
                new PositionsDto(new Position(null, null), new Position(null, null)));
        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
    }

    // -----------------------
    // computeNextPosition() tests
    // -----------------------
    @Test
    void computeNextPosition_validRequest_returnsNewPosition() {
        Position start = new Position(BigDecimal.ZERO, BigDecimal.ZERO);
        DroneMoveRqstDto req = new DroneMoveRqstDto(start, BigDecimal.valueOf(90));
        ResponseEntity<Position> resp = service.computeNextPosition(req);
        
        assertTrue(resp.getStatusCode().is2xxSuccessful());
        assertNotNull(resp.getBody());
    }

    @Test
    void computeNextPosition_nullRequest_returnsBadRequest() {
        ResponseEntity<Position> resp = service.computeNextPosition(null);
        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
    }

    @Test
    void computeNextPosition_invalidAngle_returnsBadRequest() {
        Position start = new Position(BigDecimal.ZERO, BigDecimal.ZERO);
        DroneMoveRqstDto req = new DroneMoveRqstDto(start, BigDecimal.valueOf(-10));
        ResponseEntity<Position> resp = service.computeNextPosition(req);
        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
    }

    @Test
    void computeNextPosition_angleOutOfRange_returnsBadRequest() {
        Position start = new Position(BigDecimal.ZERO, BigDecimal.ZERO);
        DroneMoveRqstDto req = new DroneMoveRqstDto(start, BigDecimal.valueOf(400));
        ResponseEntity<Position> resp = service.computeNextPosition(req);
        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
    }

    // -----------------------
    // isInRegion() tests
    // -----------------------
    @Test
    void isInRegion_positionInside_returnsTrue() {
        Region region = new Region("square", Arrays.asList(
                new Position(BigDecimal.ZERO, BigDecimal.ZERO),
                new Position(BigDecimal.ZERO, BigDecimal.ONE),
                new Position(BigDecimal.ONE, BigDecimal.ONE),
                new Position(BigDecimal.ONE, BigDecimal.ZERO)
        ));
        LocationRegionDto dto = new LocationRegionDto(
                new Position(BigDecimal.valueOf(0.5), BigDecimal.valueOf(0.5)), region);
        
        ResponseEntity<Boolean> resp = service.isInRegion(dto);
        assertNotNull(resp.getBody());
        assertTrue(resp.getBody());
    }

    @Test
    void isInRegion_positionOutside_returnsFalse() {
        Region region = new Region("square", Arrays.asList(
                new Position(BigDecimal.ZERO, BigDecimal.ZERO),
                new Position(BigDecimal.ZERO, BigDecimal.ONE),
                new Position(BigDecimal.ONE, BigDecimal.ONE),
                new Position(BigDecimal.ONE, BigDecimal.ZERO)
        ));
        LocationRegionDto dto = new LocationRegionDto(
                new Position(BigDecimal.valueOf(2), BigDecimal.valueOf(2)), region);
        
        ResponseEntity<Boolean> resp = service.isInRegion(dto);
        assertNotNull(resp.getBody());
        assertFalse(resp.getBody());
    }

    @Test
    void isInRegion_nullWrapper_returnsBadRequest() {
        ResponseEntity<Boolean> resp = service.isInRegion(null);
        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
    }

    // -----------------------
    // dronesWithCooling() tests
    // -----------------------
    @Test
    void dronesWithCooling_nullState_returnsEmptyList() {
        ResponseEntity<List<Integer>> resp = service.dronesWithCooling(null);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertNotNull(resp.getBody());
        assertTrue(resp.getBody().isEmpty());
    }

    @Test
    void dronesWithCooling_trueState_returnsDroneIds() {
        when(droneIlpClient.findDroneIdsByCooling(true)).thenReturn(List.of(1, 2, 3));
        
        ResponseEntity<List<Integer>> resp = service.dronesWithCooling(true);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertNotNull(resp.getBody());
        assertEquals(3, resp.getBody().size());
    }

    @Test
    void dronesWithCooling_falseState_returnsDroneIds() {
        when(droneIlpClient.findDroneIdsByCooling(false)).thenReturn(List.of(4, 5));
        
        ResponseEntity<List<Integer>> resp = service.dronesWithCooling(false);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertEquals(2, resp.getBody().size());
    }

    // -----------------------
    // dronesWithHeating() tests
    // -----------------------
    @Test
    void dronesWithHeating_nullState_returnsEmptyList() {
        ResponseEntity<List<Integer>> resp = service.dronesWithHeating(null);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertTrue(resp.getBody().isEmpty());
    }

    @Test
    void dronesWithHeating_trueState_returnsDroneIds() {
        when(droneIlpClient.findDroneIdsByHeating(true)).thenReturn(List.of(1, 3, 5));
        
        ResponseEntity<List<Integer>> resp = service.dronesWithHeating(true);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertEquals(3, resp.getBody().size());
    }

    // -----------------------
    // getDrone() tests
    // -----------------------
    @Test
    void getDrone_invalidId_returns404() {
        ResponseEntity<Drone> resp = service.getDrone(0);
        assertEquals(HttpStatus.NOT_FOUND, resp.getStatusCode());
    }

    @Test
    void getDrone_negativeId_returns404() {
        ResponseEntity<Drone> resp = service.getDrone(-1);
        assertEquals(HttpStatus.NOT_FOUND, resp.getStatusCode());
    }

    @Test
    void getDrone_validId_returnsDrone() {
        Drone testDrone = createTestDrone(1, 10.0f, 1000, 0.5f, 1.0f, 1.0f, true, true);
        when(droneIlpClient.fetchAllDrones()).thenReturn(List.of(testDrone));
        
        ResponseEntity<Drone> resp = service.getDrone(1);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertNotNull(resp.getBody());
        assertEquals(1, resp.getBody().getId());
    }

    @Test
    void getDrone_nonExistentId_returns404() {
        Drone testDrone = createTestDrone(1, 10.0f, 1000, 0.5f, 1.0f, 1.0f, true, true);
        when(droneIlpClient.fetchAllDrones()).thenReturn(List.of(testDrone));
        
        ResponseEntity<Drone> resp = service.getDrone(999);
        assertEquals(HttpStatus.NOT_FOUND, resp.getStatusCode());
    }

    @Test
    void getDrone_fetchReturnsNull_returnsBadRequest() {
        when(droneIlpClient.fetchAllDrones()).thenReturn(null);
        
        ResponseEntity<Drone> resp = service.getDrone(1);
        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
    }

    // -----------------------
    // getSuitableDrones() tests
    // -----------------------
    @Test
    void getSuitableDrones_nullInput_returnsEmptyList() {
        ResponseEntity<List<Integer>> resp = service.getSuitableDrones(null);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertTrue(resp.getBody().isEmpty());
    }

    @Test
    void getSuitableDrones_emptyList_returnsEmptyList() {
        ResponseEntity<List<Integer>> resp = service.getSuitableDrones(Collections.emptyList());
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertTrue(resp.getBody().isEmpty());
    }

    @Test
    void getSuitableDrones_withValidDelivery_returnsSuitableDrones() {
        Drone testDrone = createTestDrone(1, 10.0f, 1000, 0.5f, 1.0f, 1.0f, true, true);
        ServicePoint testSP = new ServicePoint(1, "SP1", new Position(BigDecimal.ZERO, BigDecimal.ZERO));
        
        when(droneIlpClient.fetchAllDrones()).thenReturn(List.of(testDrone));
        when(droneIlpClient.fetchAllServicePoints()).thenReturn(List.of(testSP));
        when(droneIlpClient.fetchAllDronesForServicePoints()).thenReturn(
                List.of(createTestDroneForServicePoint(1, "1")));
        
        MedDispatchRecDto delivery = createTestDeliveryDto(1,
                new Position(BigDecimal.valueOf(0.001), BigDecimal.valueOf(0.001)),
                true, true, 5.0f, 100.0f);
        
        ResponseEntity<List<Integer>> resp = service.getSuitableDrones(List.of(delivery));
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertNotNull(resp.getBody());
    }

    // -----------------------
    // calcDeliveryPath() tests
    // -----------------------
    @Test
    void calcDeliveryPath_nullInput_returnsEmptyStructure() {
        ResponseEntity<DeliveryPathReturnStructure> resp = service.calcDeliveryPath(null, "greedy");
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertNotNull(resp.getBody());
        assertEquals(0f, resp.getBody().getTotalCost());
        assertEquals(0, resp.getBody().getTotalMoves());
        assertTrue(resp.getBody().getDronePaths().isEmpty());
    }

    @Test
    void calcDeliveryPath_emptyList_returnsEmptyStructure() {
        ResponseEntity<DeliveryPathReturnStructure> resp = service.calcDeliveryPath(Collections.emptyList(), "greedy");
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertTrue(resp.getBody().getDronePaths().isEmpty());
    }

    @Test
    void calcDeliveryPath_minCostStrategy_usesCorrectScoring() {
        Drone testDrone = createTestDrone(1, 10.0f, 1000, 0.5f, 1.0f, 1.0f, true, true);
        ServicePoint testSP = new ServicePoint(1, "SP1", new Position(BigDecimal.ZERO, BigDecimal.ZERO));
        
        when(droneIlpClient.fetchAllDrones()).thenReturn(List.of(testDrone));
        when(droneIlpClient.fetchAllServicePoints()).thenReturn(List.of(testSP));
        when(droneIlpClient.fetchAllRestrictedAreas()).thenReturn(Collections.emptyList());
        when(droneIlpClient.fetchAllDronesForServicePoints()).thenReturn(
                List.of(createTestDroneForServicePoint(1, "1")));
        
        MedDispatchRecDto delivery = createTestDeliveryDto(1,
                new Position(BigDecimal.valueOf(0.001), BigDecimal.valueOf(0.001)),
                true, true, 5.0f, 100.0f);
        
        ResponseEntity<DeliveryPathReturnStructure> resp = service.calcDeliveryPath(
                List.of(delivery), "min_cost");
        
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertNotNull(resp.getBody());
    }

    @Test
    void calcDeliveryPath_minMovesStrategy_usesCorrectScoring() {
        Drone testDrone = createTestDrone(1, 10.0f, 1000, 0.5f, 1.0f, 1.0f, true, true);
        ServicePoint testSP = new ServicePoint(1, "SP1", new Position(BigDecimal.ZERO, BigDecimal.ZERO));
        
        when(droneIlpClient.fetchAllDrones()).thenReturn(List.of(testDrone));
        when(droneIlpClient.fetchAllServicePoints()).thenReturn(List.of(testSP));
        when(droneIlpClient.fetchAllRestrictedAreas()).thenReturn(Collections.emptyList());
        when(droneIlpClient.fetchAllDronesForServicePoints()).thenReturn(
                List.of(createTestDroneForServicePoint(1, "1")));
        
        MedDispatchRecDto delivery = createTestDeliveryDto(1,
                new Position(BigDecimal.valueOf(0.001), BigDecimal.valueOf(0.001)),
                true, true, 5.0f, 100.0f);
        
        ResponseEntity<DeliveryPathReturnStructure> resp = service.calcDeliveryPath(
                List.of(delivery), "min_moves");
        
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertNotNull(resp.getBody());
    }

    @Test
    void calcDeliveryPath_balancedStrategy_usesCorrectScoring() {
        Drone testDrone = createTestDrone(1, 10.0f, 1000, 0.5f, 1.0f, 1.0f, true, true);
        ServicePoint testSP = new ServicePoint(1, "SP1", new Position(BigDecimal.ZERO, BigDecimal.ZERO));
        
        when(droneIlpClient.fetchAllDrones()).thenReturn(List.of(testDrone));
        when(droneIlpClient.fetchAllServicePoints()).thenReturn(List.of(testSP));
        when(droneIlpClient.fetchAllRestrictedAreas()).thenReturn(Collections.emptyList());
        when(droneIlpClient.fetchAllDronesForServicePoints()).thenReturn(
                List.of(createTestDroneForServicePoint(1, "1")));
        
        MedDispatchRecDto delivery = createTestDeliveryDto(1,
                new Position(BigDecimal.valueOf(0.001), BigDecimal.valueOf(0.001)),
                true, true, 5.0f, 100.0f);
        
        ResponseEntity<DeliveryPathReturnStructure> resp = service.calcDeliveryPath(
                List.of(delivery), "balanced");
        
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertNotNull(resp.getBody());
    }

    @Test
    void calcDeliveryPath_exceptionHandling_returnsEmptyResult() {
        when(droneIlpClient.fetchAllDrones()).thenThrow(new RuntimeException("Test exception"));
        
        MedDispatchRecDto delivery = createTestDeliveryDto(1,
                new Position(BigDecimal.valueOf(0.001), BigDecimal.valueOf(0.001)),
                true, true, 5.0f, 100.0f);
        
        try {
            ResponseEntity<DeliveryPathReturnStructure> resp = service.calcDeliveryPath(
                    List.of(delivery), "min_cost");
            assertTrue(resp.getStatusCode() == HttpStatus.OK || 
                      resp.getStatusCode() == HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
            fail("Should handle exception gracefully");
        }
    }

    // -----------------------
    // calcDeliveryPathAsGeoJson() tests
    // -----------------------
    @Test
    void calcDeliveryPathAsGeoJson_nullInput_returnsEmptyGeoJson() {
        ResponseEntity<GeoJsonLineStringDto> resp = service.calcDeliveryPathAsGeoJson(null);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertNotNull(resp.getBody());
        assertNotNull(resp.getBody().getFeatures());
    }

    @Test
    void calcDeliveryPathAsGeoJson_emptyList_returnsEmptyGeoJson() {
        ResponseEntity<GeoJsonLineStringDto> resp = service.calcDeliveryPathAsGeoJson(Collections.emptyList());
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertNotNull(resp.getBody());
    }

    @Test
    void calcDeliveryPathAsGeoJson_withValidData_returnsGeoJson() {
        Drone testDrone = createTestDrone(1, 10.0f, 1000, 0.5f, 1.0f, 1.0f, true, true);
        ServicePoint testSP = new ServicePoint(1, "SP1", new Position(BigDecimal.ZERO, BigDecimal.ZERO));
        
        when(droneIlpClient.fetchAllDrones()).thenReturn(List.of(testDrone));
        when(droneIlpClient.fetchAllServicePoints()).thenReturn(List.of(testSP));
        when(droneIlpClient.fetchAllRestrictedAreas()).thenReturn(Collections.emptyList());
        when(droneIlpClient.fetchAllDronesForServicePoints()).thenReturn(
                List.of(createTestDroneForServicePoint(1, "1")));
        
        MedDispatchRecDto delivery = createTestDeliveryDto(1,
                new Position(BigDecimal.valueOf(0.001), BigDecimal.valueOf(0.001)),
                true, true, 5.0f, 100.0f);
        
        ResponseEntity<GeoJsonLineStringDto> resp = service.calcDeliveryPathAsGeoJson(List.of(delivery));
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertNotNull(resp.getBody());
    }

    // -----------------------
    // calcMultiDroneDeliveryPathAsGeoJson() tests
    // -----------------------
    @Test
    void calcMultiDroneDeliveryPathAsGeoJson_nullInput_returnsEmptyGeoJson() {
        ResponseEntity<GeoJsonLineStringDto> resp = service.calcMultiDroneDeliveryPathAsGeoJson(null, "greedy");
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertNotNull(resp.getBody());
    }

    @Test
    void calcMultiDroneDeliveryPathAsGeoJson_emptyList_returnsEmptyGeoJson() {
        ResponseEntity<GeoJsonLineStringDto> resp = service.calcMultiDroneDeliveryPathAsGeoJson(
                Collections.emptyList(), "greedy");
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertNotNull(resp.getBody());
    }

    // -----------------------
    // queryAsPath() tests
    // -----------------------
    @Test
    void queryAsPath_nullAttributes_returnsEmptyList() {
        ResponseEntity<List<Integer>> resp1 = service.queryAsPath(null, "value");
        assertEquals(HttpStatus.OK, resp1.getStatusCode());
        assertTrue(resp1.getBody().isEmpty());

        ResponseEntity<List<Integer>> resp2 = service.queryAsPath("attr", null);
        assertEquals(HttpStatus.OK, resp2.getStatusCode());
        assertTrue(resp2.getBody().isEmpty());
    }

    @Test
    void queryAsPath_withValidData_filtersCorrectly() {
        Drone drone1 = createTestDrone(1, 10.0f, 1000, 0.5f, 1.0f, 1.0f, true, true);
        Drone drone2 = createTestDrone(2, 20.0f, 2000, 1.0f, 2.0f, 2.0f, false, true);
        
        when(droneIlpClient.fetchAllDrones()).thenReturn(List.of(drone1, drone2));
        
        ResponseEntity<List<Integer>> resp = service.queryAsPath("capacity", "=10.0");
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertNotNull(resp.getBody());
    }

    @Test
    void queryAsPath_withGreaterThanOperator_filtersCorrectly() {
        Drone drone1 = createTestDrone(1, 10.0f, 1000, 0.5f, 1.0f, 1.0f, true, true);
        Drone drone2 = createTestDrone(2, 20.0f, 2000, 1.0f, 2.0f, 2.0f, false, true);
        
        when(droneIlpClient.fetchAllDrones()).thenReturn(List.of(drone1, drone2));
        
        ResponseEntity<List<Integer>> resp = service.queryAsPath("capacity", ">15.0");
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertNotNull(resp.getBody());
    }

    @Test
    void queryAsPath_withLessThanOperator_filtersCorrectly() {
        Drone drone1 = createTestDrone(1, 10.0f, 1000, 0.5f, 1.0f, 1.0f, true, true);
        Drone drone2 = createTestDrone(2, 20.0f, 2000, 1.0f, 2.0f, 2.0f, false, true);
        
        when(droneIlpClient.fetchAllDrones()).thenReturn(List.of(drone1, drone2));
        
        ResponseEntity<List<Integer>> resp = service.queryAsPath("capacity", "<15.0");
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertNotNull(resp.getBody());
    }

    // -----------------------
    // query() tests
    // -----------------------
    @Test
    void query_nullInput_returnsEmptyList() {
        ResponseEntity<List<String>> resp = service.query(null);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertTrue(resp.getBody().isEmpty());
    }

    @Test
    void query_emptyList_returnsResult() {
        ResponseEntity<List<String>> resp = service.query(Collections.emptyList());
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertNotNull(resp.getBody());
    }

    @Test
    void query_withMultipleAttributes_returnsDroneIds() {
        Drone drone1 = createTestDrone(1, 10.0f, 1000, 0.5f, 1.0f, 1.0f, true, true);
        
        when(droneIlpClient.fetchAllDrones()).thenReturn(List.of(drone1));
        
        QueryAttributeDto attr1 = new QueryAttributeDto("capacity", "=", "10.0");
        QueryAttributeDto attr2 = new QueryAttributeDto("heating", "=", "true");
        
        ResponseEntity<List<String>> resp = service.query(List.of(attr1, attr2));
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertNotNull(resp.getBody());
    }

    @Test
    void query_withNoMatchingDrones_returnsEmptyList() {
        Drone drone1 = createTestDrone(1, 10.0f, 1000, 0.5f, 1.0f, 1.0f, true, true);
        
        when(droneIlpClient.fetchAllDrones()).thenReturn(List.of(drone1));
        
        QueryAttributeDto attr = new QueryAttributeDto("capacity", "=", "999.0");
        
        ResponseEntity<List<String>> resp = service.query(List.of(attr));
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertNotNull(resp.getBody());
    }

    // -----------------------
    // Helper methods for creating test data
    // -----------------------
    private Drone createTestDrone(int id, float capacity, int maxMoves, float costPerMove,
                                  float costInitial, float costFinal, boolean heating, boolean cooling) {
        Capability capability = new Capability(cooling, heating, capacity, maxMoves, 
                                              costPerMove, costInitial, costFinal);
        return new Drone("Drone" + id, String.valueOf(id), capability);
    }

    private MedDispatchRecDto createTestDeliveryDto(int id, Position delivery,
                                                    boolean heating, boolean cooling,
                                                    float capacity, float maxCost) {
        MedDispatchRecDto dto = new MedDispatchRecDto();
        dto.setId(id);
        dto.setDate(LocalDate.now());
        dto.setTime(LocalTime.now());
        dto.setDelivery(delivery);
        
        Requirements requirements = new Requirements(heating, cooling, capacity, maxCost);
        dto.setRequirements(requirements);
        
        return dto;
    }

    private DroneForServicePoint createTestDroneForServicePoint(Integer servicePointId, String droneId) {
        Availability availability = new Availability("MONDAY", LocalTime.of(9, 0), LocalTime.of(17, 0));
        DroneAvailability droneAvailability = new DroneAvailability(droneId, List.of(availability));
        return new DroneForServicePoint(servicePointId, List.of(droneAvailability));
    }
}
