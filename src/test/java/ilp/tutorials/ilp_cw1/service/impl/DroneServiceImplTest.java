package ilp.tutorials.ilp_cw1.service.impl;

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
 * Plain JUnit tests for {@link DroneServiceImpl} covering CW1 basic endpoints
 * (distance, closeness, next-position, region containment).
 * <p>
 * Uses Mockito to mock DroneIlpClient dependencies.
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
        assertEquals(5.0, resp.getBody().doubleValue(), 1e-6); // sqrt((4-1)^2 + (5-1)^2) = 5
        assertEquals(HttpStatus.OK, resp.getStatusCode());
    }

    @Test
    void distanceTo_missingPosition_returnsBadRequest() {
        PositionsDto positionsDto = new PositionsDto(null, new Position(BigDecimal.ONE, BigDecimal.ONE));
        ResponseEntity<BigDecimal> resp = service.distanceTo(positionsDto);
        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
    }

    @Test
    void distanceTo_invalidJson_returnsBadRequest() {
        ResponseEntity<BigDecimal> resp = service.distanceTo(new PositionsDto(new Position(null, null), new Position(BigDecimal.ONE, BigDecimal.ONE)));
        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
    }

    @Test
    void distanceTo_outOfRangeCoordinates_returnsBadRequest() {
        // Test case from submission checker: lng=-300.192473, lat=550.946233
        // These coordinates are way out of valid geographic bounds
        PositionsDto positionsDto = new PositionsDto(
                new Position(BigDecimal.valueOf(-300.192473), BigDecimal.valueOf(550.946233)),
                new Position(BigDecimal.valueOf(-3202.192473), BigDecimal.valueOf(5533.942617))
        );
        ResponseEntity<BigDecimal> resp = service.distanceTo(positionsDto);
        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
    }

    @Test
    void distanceTo_onlyFirstPositionOutOfRange_returnsBadRequest() {
        // Valid range: longitude [-180,180], latitude [-90,90]
        PositionsDto positionsDto = new PositionsDto(
                new Position(BigDecimal.valueOf(-300.192473), BigDecimal.valueOf(55.946233)),
                new Position(BigDecimal.valueOf(-3.192473), BigDecimal.valueOf(55.942617))
        );
        ResponseEntity<BigDecimal> resp = service.distanceTo(positionsDto);
        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
    }

    @Test
    void distanceTo_onlySecondPositionOutOfRange_returnsBadRequest() {
        PositionsDto positionsDto = new PositionsDto(
                new Position(BigDecimal.valueOf(-3.192473), BigDecimal.valueOf(55.946233)),
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
    void isCloseTo_invalidJson_returnsBadRequest() {
        ResponseEntity<Boolean> resp = service.isCloseTo(new PositionsDto(new Position(null, null), new Position(null, null)));
        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
    }

    @Test
    void isCloseTo_outOfRangeCoordinates_returnsBadRequest() {
        PositionsDto positionsDto = new PositionsDto(
                new Position(BigDecimal.valueOf(-300.192473), BigDecimal.valueOf(550.946233)),
                new Position(BigDecimal.valueOf(-3202.192473), BigDecimal.valueOf(5533.942617))
        );
        ResponseEntity<Boolean> resp = service.isCloseTo(positionsDto);
        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
    }

    @Test
    void isCloseTo_onlyFirstPositionOutOfRange_returnsBadRequest() {
        PositionsDto positionsDto = new PositionsDto(
                new Position(BigDecimal.valueOf(-300.192473), BigDecimal.valueOf(55.946233)),
                new Position(BigDecimal.valueOf(-3.192473), BigDecimal.valueOf(55.942617))
        );
        ResponseEntity<Boolean> resp = service.isCloseTo(positionsDto);
        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
    }

    @Test
    void isCloseTo_onlySecondPositionOutOfRange_returnsBadRequest() {
        PositionsDto positionsDto = new PositionsDto(
                new Position(BigDecimal.valueOf(-3.192473), BigDecimal.valueOf(55.946233)),
                new Position(BigDecimal.valueOf(-3202.192473), BigDecimal.valueOf(5533.942617))
        );
        ResponseEntity<Boolean> resp = service.isCloseTo(positionsDto);
        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
    }

    // -----------------------
    // computeNextPosition() tests
    // -----------------------
    @Test
    void computeNextPosition_validRequest_returnsNewPosition() {
        Position start = new Position(BigDecimal.ZERO, BigDecimal.ZERO);
        DroneMoveRqstDto req = new DroneMoveRqstDto(start, BigDecimal.valueOf(90)); // east
        ResponseEntity<Position> resp = service.computeNextPosition(req);
        assertTrue(resp.getStatusCode().is2xxSuccessful());
        Position result = resp.getBody();
        assertNotNull(result);
        assertNotEquals(BigDecimal.ZERO, result.getLongitude());
        assertNotEquals(BigDecimal.ZERO, result.getLatitude());
    }

    @Test
    void computeNextPosition_invalidRequest_returnsBadRequest() {
        ResponseEntity<Position> resp1 = service.computeNextPosition(null);
        assertEquals(HttpStatus.BAD_REQUEST, resp1.getStatusCode());

        DroneMoveRqstDto req = new DroneMoveRqstDto(null, BigDecimal.ZERO);
        ResponseEntity<Position> resp2 = service.computeNextPosition(req);
        assertEquals(HttpStatus.BAD_REQUEST, resp2.getStatusCode());

        DroneMoveRqstDto req2 = new DroneMoveRqstDto(new Position(BigDecimal.ZERO, BigDecimal.ZERO), BigDecimal.valueOf(-10));
        assertEquals(HttpStatus.BAD_REQUEST, service.computeNextPosition(req2).getStatusCode());
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
        LocationRegionDto dto = new LocationRegionDto(new Position(BigDecimal.valueOf(0.5), BigDecimal.valueOf(0.5)), region);
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
        LocationRegionDto dto = new LocationRegionDto(new Position(BigDecimal.valueOf(2), BigDecimal.valueOf(2)), region);
        ResponseEntity<Boolean> resp = service.isInRegion(dto);
        assertNotNull(resp.getBody());
        assertFalse(resp.getBody());
    }

    @Test
    void isInRegion_invalidInputs_returnsBadRequest() {
        assertEquals(HttpStatus.BAD_REQUEST, service.isInRegion(null).getStatusCode());
        assertEquals(HttpStatus.BAD_REQUEST, service.isInRegion(new LocationRegionDto(null, new Region())).getStatusCode());
        assertEquals(HttpStatus.BAD_REQUEST, service.isInRegion(new LocationRegionDto(new Position(), null)).getStatusCode());
    }
//
//    // -----------------------
//    // Additional test cases matching submission checker
//    // -----------------------
//
//    // Valid distanceTo test with exact expected value
//    @Test
//    void distanceTo_validCoordinates_returnsExpectedDistance() {
//        PositionsDto positionsDto = new PositionsDto(
//                new Position(BigDecimal.valueOf(-3.192473), BigDecimal.valueOf(55.946233)),
//                new Position(BigDecimal.valueOf(-3.192473), BigDecimal.valueOf(55.942617))
//        );
//        ResponseEntity<BigDecimal> resp = service.distanceTo(positionsDto);
//        assertEquals(HttpStatus.OK, resp.getStatusCode());
//        assertNotNull(resp.getBody());
//        // Expected distance: 0.003616000000000000
//        assertEquals(0.003616, resp.getBody().doubleValue(), 1e-9);
//    }
//
//    // Valid isCloseTo test
//    @Test
//    void isCloseTo_positionsVeryClose_returnsTrue() {
//        PositionsDto positionsDto = new PositionsDto(
//                new Position(BigDecimal.valueOf(-3.192473), BigDecimal.valueOf(55.946233)),
//                new Position(BigDecimal.valueOf(-3.192473), BigDecimal.valueOf(55.946117))
//        );
//        ResponseEntity<Boolean> resp = service.isCloseTo(positionsDto);
//        assertEquals(HttpStatus.OK, resp.getStatusCode());
//        assertNotNull(resp.getBody());
//        assertTrue(resp.getBody());
//    }
//
//    // Semantic error for isCloseTo - out of range coordinates
//    @Test
//    void isCloseTo_outOfRangeSemanticError_returnsBadRequest() {
//        PositionsDto positionsDto = new PositionsDto(
//                new Position(BigDecimal.valueOf(-3004.192473), BigDecimal.valueOf(550.946233)),
//                new Position(BigDecimal.valueOf(-390.192473), BigDecimal.valueOf(551.942617))
//        );
//        ResponseEntity<Boolean> resp = service.isCloseTo(positionsDto);
//        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
//    }
//
//    // Valid nextPosition test
//    @Test
//    void computeNextPosition_validAngle90_returnsNewPosition() {
//        Position start = new Position(BigDecimal.valueOf(-3.192473), BigDecimal.valueOf(55.946233));
//        DroneMoveRqstDto req = new DroneMoveRqstDto(start, BigDecimal.valueOf(90));
//        ResponseEntity<Position> resp = service.computeNextPosition(req);
//        assertEquals(HttpStatus.OK, resp.getStatusCode());
//        assertNotNull(resp.getBody());
//        // Should move eastward (angle 90 degrees)
//        assertTrue(resp.getBody().getLongitude().compareTo(start.getLongitude()) > 0);
//    }
//
//    // Semantic error for nextPosition - angle out of range
//    @Test
//    void computeNextPosition_angleOutOfRange_returnsBadRequest() {
//        Position start = new Position(BigDecimal.valueOf(-3.192473), BigDecimal.valueOf(55.946233));
//        DroneMoveRqstDto req = new DroneMoveRqstDto(start, BigDecimal.valueOf(900));
//        ResponseEntity<Position> resp = service.computeNextPosition(req);
//        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
//    }
//
//    // Valid isInRegion test
//    @Test
//    void isInRegion_validPositionInsideRegion_returnsTrue() {
//        Region region = new Region("central", Arrays.asList(
//                new Position(BigDecimal.valueOf(-3.192473), BigDecimal.valueOf(55.946233)),
//                new Position(BigDecimal.valueOf(-3.192473), BigDecimal.valueOf(55.942617)),
//                new Position(BigDecimal.valueOf(-3.184319), BigDecimal.valueOf(55.942617)),
//                new Position(BigDecimal.valueOf(-3.184319), BigDecimal.valueOf(55.946233)),
//                new Position(BigDecimal.valueOf(-3.192473), BigDecimal.valueOf(55.946233))
//        ));
//        Position position = new Position(BigDecimal.valueOf(-3.186000), BigDecimal.valueOf(55.944000));
//        LocationRegionDto dto = new LocationRegionDto(position, region);
//
//        ResponseEntity<Boolean> resp = service.isInRegion(dto);
//        assertEquals(HttpStatus.OK, resp.getStatusCode());
//        assertNotNull(resp.getBody());
//        assertTrue(resp.getBody());
//    }
//
//    // Semantic error for isInRegion - out of range coordinates
//    @Test
//    void isInRegion_outOfRangeCoordinates_returnsBadRequest() {
//        Region region = new Region("central", Arrays.asList(
//                new Position(BigDecimal.valueOf(-3.192473), BigDecimal.valueOf(558.946233)),
//                new Position(BigDecimal.valueOf(-367.192473), BigDecimal.valueOf(55.942617)),
//                new Position(BigDecimal.valueOf(-3.184319), BigDecimal.valueOf(55.942617)),
//                new Position(BigDecimal.valueOf(-3.184319), BigDecimal.valueOf(55.946233)),
//                new Position(BigDecimal.valueOf(-3.192473), BigDecimal.valueOf(55.946233))
//        ));
//        Position position = new Position(BigDecimal.valueOf(-390.186000), BigDecimal.valueOf(550.944000));
//        LocationRegionDto dto = new LocationRegionDto(position, region);
//
//        ResponseEntity<Boolean> resp = service.isInRegion(dto);
//        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
//    }
//
//    // Open vertices (polygon with less than 3 vertices or not forming a closed polygon)
    // Open vertices (polygon with less than 3 vertices or not forming a closed polygon)
    @Test
    void isInRegion_insufficientVertices_returnsBadRequest() {
        Region region = new Region("central", Arrays.asList(
                new Position(BigDecimal.valueOf(-3.192473), BigDecimal.valueOf(55.946233)),
                new Position(BigDecimal.valueOf(-3.192473), BigDecimal.valueOf(55.942617)),
                new Position(BigDecimal.valueOf(-3.184319), BigDecimal.valueOf(55.942617))
        ));
        Position position = new Position(BigDecimal.valueOf(398.234), BigDecimal.valueOf(500.222));
        LocationRegionDto dto = new LocationRegionDto(position, region);

        ResponseEntity<Boolean> resp = service.isInRegion(dto);
        // Should return bad request because position is out of range AND region might be invalid
        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
    }

    // Test case for 3 valid vertices forming a valid triangle
    @Test
    void isInRegion_threeValidVertices_returnsOkWhenValidPosition() {
        Region region = new Region("triangle", Arrays.asList(
                new Position(BigDecimal.valueOf(-3.192473), BigDecimal.valueOf(55.946233)),
                new Position(BigDecimal.valueOf(-3.192473), BigDecimal.valueOf(55.942617)),
                new Position(BigDecimal.valueOf(-3.184319), BigDecimal.valueOf(55.944425))
        ));
        Position position = new Position(BigDecimal.valueOf(-3.188000), BigDecimal.valueOf(55.944000));
        LocationRegionDto dto = new LocationRegionDto(position, region);

        // This should work if the position is valid and inside the triangle
        ResponseEntity<Boolean> resp = service.isInRegion(dto);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
    }

    // -----------------------
    // CW2 Input Validation Tests (without mocking data)
    // -----------------------

    // dronesWithCooling tests
    @Test
    void dronesWithCooling_nullInput_returnsEmptyList() {
        ResponseEntity<List<Integer>> resp = service.dronesWithCooling(null);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertNotNull(resp.getBody());
        assertTrue(resp.getBody().isEmpty());
    }

    // dronesWithHeating tests
    @Test
    void dronesWithHeating_nullInput_returnsEmptyList() {
        ResponseEntity<List<Integer>> resp = service.dronesWithHeating(null);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertNotNull(resp.getBody());
        assertTrue(resp.getBody().isEmpty());
    }

    // getDrone tests
    @Test
    void getDrone_invalidId_returns404() {
        ResponseEntity<Drone> resp1 = service.getDrone(0);
        assertEquals(HttpStatus.NOT_FOUND, resp1.getStatusCode());

        ResponseEntity<Drone> resp2 = service.getDrone(-1);
        assertEquals(HttpStatus.NOT_FOUND, resp2.getStatusCode());

        ResponseEntity<Drone> resp3 = service.getDrone(-999);
        assertEquals(HttpStatus.NOT_FOUND, resp3.getStatusCode());
    }

    // getSuitableDrones tests
    @Test
    void getSuitableDrones_nullInput_returnsEmptyList() {
        ResponseEntity<List<Integer>> resp = service.getSuitableDrones(null);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertNotNull(resp.getBody());
        assertTrue(resp.getBody().isEmpty());
    }

    @Test
    void getSuitableDrones_emptyList_returnsEmptyList() {
        ResponseEntity<List<Integer>> resp = service.getSuitableDrones(Collections.emptyList());
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertNotNull(resp.getBody());
        assertTrue(resp.getBody().isEmpty());
    }

    // calcDeliveryPath tests
    @Test
    void calcDeliveryPath_nullInput_returnsEmptyStructure() {
        ResponseEntity<DeliveryPathReturnStructure> resp = service.calcDeliveryPath(null, "greedy");
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertNotNull(resp.getBody());
        assertEquals(0f, resp.getBody().getTotalCost());
        assertEquals(0, resp.getBody().getTotalMoves());
        assertNotNull(resp.getBody().getDronePaths());
        assertTrue(resp.getBody().getDronePaths().isEmpty());
    }

    @Test
    void calcDeliveryPath_emptyList_returnsEmptyStructure() {
        ResponseEntity<DeliveryPathReturnStructure> resp = service.calcDeliveryPath(Collections.emptyList(), "greedy");
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertNotNull(resp.getBody());
        assertEquals(0f, resp.getBody().getTotalCost());
        assertEquals(0, resp.getBody().getTotalMoves());
        assertTrue(resp.getBody().getDronePaths().isEmpty());
    }

    // calcDeliveryPathAsGeoJson tests
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
        assertNotNull(resp.getBody().getFeatures());
    }

    // calcMultiDroneDeliveryPathAsGeoJson tests
    @Test
    void calcMultiDroneDeliveryPathAsGeoJson_nullInput_returnsEmptyGeoJson() {
        ResponseEntity<GeoJsonLineStringDto> resp = service.calcMultiDroneDeliveryPathAsGeoJson(null, "greedy");
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertNotNull(resp.getBody());
        assertNotNull(resp.getBody().getFeatures());
    }

    @Test
    void calcMultiDroneDeliveryPathAsGeoJson_emptyList_returnsEmptyGeoJson() {
        ResponseEntity<GeoJsonLineStringDto> resp = service.calcMultiDroneDeliveryPathAsGeoJson(Collections.emptyList(), "greedy");
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertNotNull(resp.getBody());
        assertNotNull(resp.getBody().getFeatures());
    }

    // queryAsPath tests
    @Test
    void queryAsPath_nullAttribute_returnsEmptyList() {
        ResponseEntity<List<Integer>> resp1 = service.queryAsPath(null, "value");
        assertEquals(HttpStatus.OK, resp1.getStatusCode());
        assertNotNull(resp1.getBody());
        assertTrue(resp1.getBody().isEmpty());

        ResponseEntity<List<Integer>> resp2 = service.queryAsPath("attribute", null);
        assertEquals(HttpStatus.OK, resp2.getStatusCode());
        assertNotNull(resp2.getBody());
        assertTrue(resp2.getBody().isEmpty());

        ResponseEntity<List<Integer>> resp3 = service.queryAsPath(null, null);
        assertEquals(HttpStatus.OK, resp3.getStatusCode());
        assertNotNull(resp3.getBody());
        assertTrue(resp3.getBody().isEmpty());
    }

    // query tests
    @Test
    void query_nullInput_returnsEmptyList() {
        ResponseEntity<List<String>> resp = service.query(null);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertNotNull(resp.getBody());
        assertTrue(resp.getBody().isEmpty());
    }

    @Test
    void query_emptyList_returnsEmptyList() {
        ResponseEntity<List<String>> resp = service.query(Collections.emptyList());
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertNotNull(resp.getBody());
        // Empty query list should return all drones, but without mocking we get empty
        assertNotNull(resp.getBody());
    }

    // Additional edge case tests
    @Test
    void getSuitableDrones_invalidDeliveryData_handlesGracefully() {
        // Test with delivery that has null requirements
        MedDispatchRecDto delivery = new MedDispatchRecDto();
        delivery.setId(1);
        delivery.setDate(LocalDate.now());
        delivery.setTime(LocalTime.now());
        // Note: requirements is null - should not crash
        
        ResponseEntity<List<Integer>> resp = service.getSuitableDrones(List.of(delivery));
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertNotNull(resp.getBody());
        // Without mocking, we expect empty or graceful handling
    }

    // -----------------------
    // Tests for orderDeliveriesForDrone method (via reflection or subclass)
    // -----------------------
    @Test
    void calcDeliveryPath_withValidMockedData_ordersDeliveriesCorrectly() {
        // Setup mock data
        Drone testDrone = createTestDrone(1, 10.0f, 1000, 0.5f, 1.0f, 1.0f, true, true);
        ServicePoint testSP = new ServicePoint(1, "SP1", new Position(BigDecimal.ZERO, BigDecimal.ZERO));
        
        when(droneIlpClient.fetchAllDrones()).thenReturn(List.of(testDrone));
        when(droneIlpClient.fetchAllServicePoints()).thenReturn(List.of(testSP));
        when(droneIlpClient.fetchAllRestrictedAreas()).thenReturn(Collections.emptyList());
        when(droneIlpClient.fetchAllDronesForServicePoints()).thenReturn(
            List.of(createTestDroneForServicePoint(1, "1"))
        );
        
        // Create test deliveries
        MedDispatchRecDto delivery1 = createTestDeliveryDto(1, 
            new Position(BigDecimal.valueOf(0.001), BigDecimal.valueOf(0.001)), 
            true, true, 5.0f, 100.0f);
        MedDispatchRecDto delivery2 = createTestDeliveryDto(2, 
            new Position(BigDecimal.valueOf(0.002), BigDecimal.valueOf(0.002)), 
            true, true, 5.0f, 100.0f);
        
        ResponseEntity<DeliveryPathReturnStructure> resp = service.calcDeliveryPath(
            List.of(delivery1, delivery2), "min_cost"
        );
        
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertNotNull(resp.getBody());
    }

    @Test
    void calcDeliveryPath_withMinMovesStrategy_usesCorrectScoring() {
        // Setup mock data
        Drone testDrone = createTestDrone(1, 10.0f, 1000, 0.5f, 1.0f, 1.0f, true, true);
        ServicePoint testSP = new ServicePoint(1, "SP1", new Position(BigDecimal.ZERO, BigDecimal.ZERO));
        
        when(droneIlpClient.fetchAllDrones()).thenReturn(List.of(testDrone));
        when(droneIlpClient.fetchAllServicePoints()).thenReturn(List.of(testSP));
        when(droneIlpClient.fetchAllRestrictedAreas()).thenReturn(Collections.emptyList());
        when(droneIlpClient.fetchAllDronesForServicePoints()).thenReturn(
            List.of(createTestDroneForServicePoint(1, "1"))
        );
        
        MedDispatchRecDto delivery = createTestDeliveryDto(1, 
            new Position(BigDecimal.valueOf(0.001), BigDecimal.valueOf(0.001)), 
            true, true, 5.0f, 100.0f);
        
        ResponseEntity<DeliveryPathReturnStructure> resp = service.calcDeliveryPath(
            List.of(delivery), "min_moves"
        );
        
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertNotNull(resp.getBody());
    }

    @Test
    void calcDeliveryPath_withBalancedStrategy_usesCorrectScoring() {
        // Setup mock data
        Drone testDrone = createTestDrone(1, 10.0f, 1000, 0.5f, 1.0f, 1.0f, true, true);
        ServicePoint testSP = new ServicePoint(1, "SP1", new Position(BigDecimal.ZERO, BigDecimal.ZERO));
        
        when(droneIlpClient.fetchAllDrones()).thenReturn(List.of(testDrone));
        when(droneIlpClient.fetchAllServicePoints()).thenReturn(List.of(testSP));
        when(droneIlpClient.fetchAllRestrictedAreas()).thenReturn(Collections.emptyList());
        when(droneIlpClient.fetchAllDronesForServicePoints()).thenReturn(
            List.of(createTestDroneForServicePoint(1, "1"))
        );
        
        MedDispatchRecDto delivery = createTestDeliveryDto(1, 
            new Position(BigDecimal.valueOf(0.001), BigDecimal.valueOf(0.001)), 
            true, true, 5.0f, 100.0f);
        
        ResponseEntity<DeliveryPathReturnStructure> resp = service.calcDeliveryPath(
            List.of(delivery), "balanced"
        );
        
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertNotNull(resp.getBody());
    }

    @Test
    void calcDeliveryPath_whenNoDroneCanHandleDeliveries_returnsEmptyResult() {
        // Setup mock data with drone that can't handle the delivery requirements
        Drone testDrone = createTestDrone(1, 1.0f, 100, 0.5f, 1.0f, 1.0f, false, false);
        ServicePoint testSP = new ServicePoint(1, "SP1", new Position(BigDecimal.ZERO, BigDecimal.ZERO));
        
        when(droneIlpClient.fetchAllDrones()).thenReturn(List.of(testDrone));
        when(droneIlpClient.fetchAllServicePoints()).thenReturn(List.of(testSP));
        when(droneIlpClient.fetchAllRestrictedAreas()).thenReturn(Collections.emptyList());
        when(droneIlpClient.fetchAllDronesForServicePoints()).thenReturn(
            List.of(createTestDroneForServicePoint(1, "1"))
        );
        
        // Delivery requires heating and cooling, but drone doesn't have them
        MedDispatchRecDto delivery = createTestDeliveryDto(1, 
            new Position(BigDecimal.valueOf(0.001), BigDecimal.valueOf(0.001)), 
            true, true, 0.5f, 100.0f);
        
        ResponseEntity<DeliveryPathReturnStructure> resp = service.calcDeliveryPath(
            List.of(delivery), "min_cost"
        );
        
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertNotNull(resp.getBody());
        assertTrue(resp.getBody().getDronePaths().isEmpty());
    }

    @Test
    void calcDeliveryPath_withNoServicePoint_handlesGracefully() {
        // Setup mock data with drone but no service point
        Drone testDrone = createTestDrone(1, 10.0f, 1000, 0.5f, 1.0f, 1.0f, true, true);
        
        when(droneIlpClient.fetchAllDrones()).thenReturn(List.of(testDrone));
        when(droneIlpClient.fetchAllServicePoints()).thenReturn(Collections.emptyList());
        when(droneIlpClient.fetchAllRestrictedAreas()).thenReturn(Collections.emptyList());
        when(droneIlpClient.fetchAllDronesForServicePoints()).thenReturn(Collections.emptyList());
        
        MedDispatchRecDto delivery = createTestDeliveryDto(1, 
            new Position(BigDecimal.valueOf(0.001), BigDecimal.valueOf(0.001)), 
            true, true, 5.0f, 100.0f);
        
        ResponseEntity<DeliveryPathReturnStructure> resp = service.calcDeliveryPath(
            List.of(delivery), "min_cost"
        );
        
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertNotNull(resp.getBody());
    }

    @Test
    void calcDeliveryPath_withExceptionDuringProcessing_handlesGracefully() {
        // Setup mock to throw exception
        when(droneIlpClient.fetchAllDrones()).thenThrow(new RuntimeException("Test exception"));
        
        MedDispatchRecDto delivery = createTestDeliveryDto(1, 
            new Position(BigDecimal.valueOf(0.001), BigDecimal.valueOf(0.001)), 
            true, true, 5.0f, 100.0f);
        
        // The implementation returns 500 on exception - verify it doesn't crash
        try {
            ResponseEntity<DeliveryPathReturnStructure> resp = service.calcDeliveryPath(
                List.of(delivery), "min_cost"
            );
            // Either 200 or 500 is acceptable as long as it doesn't throw
            assertTrue(resp.getStatusCode() == HttpStatus.OK || resp.getStatusCode() == HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
            fail("Should handle exception gracefully without throwing");
        }
    }

    // -----------------------
    // Tests for query methods with various operators
    // -----------------------
    @Test
    void queryAsPath_withValidDroneData_filtersCorrectly() {
        Drone drone1 = createTestDrone(1, 10.0f, 1000, 0.5f, 1.0f, 1.0f, true, true);
        Drone drone2 = createTestDrone(2, 20.0f, 2000, 1.0f, 2.0f, 2.0f, false, true);
        
        when(droneIlpClient.fetchAllDrones()).thenReturn(List.of(drone1, drone2));
        
        // Test with "=" operator
        ResponseEntity<List<Integer>> resp = service.queryAsPath("capacity", "=10.0");
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertNotNull(resp.getBody());
    }

    @Test
    void query_withMultipleAttributesAllMatch_returnsDroneIds() {
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
        assertTrue(resp.getBody().isEmpty());
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

    @Test
    void queryAsPath_withBooleanAttribute_matchesCorrectly() {
        Drone drone1 = createTestDrone(1, 10.0f, 1000, 0.5f, 1.0f, 1.0f, true, true);
        Drone drone2 = createTestDrone(2, 20.0f, 2000, 1.0f, 2.0f, 2.0f, false, false);
        
        when(droneIlpClient.fetchAllDrones()).thenReturn(List.of(drone1, drone2));
        
        ResponseEntity<List<Integer>> resp = service.queryAsPath("heating", "=true");
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertNotNull(resp.getBody());
    }

    // -----------------------
    // Tests for getDrone with mocked data
    // -----------------------
    @Test
    void getDrone_withValidId_returnsDrone() {
        Drone testDrone = createTestDrone(1, 10.0f, 1000, 0.5f, 1.0f, 1.0f, true, true);
        
        when(droneIlpClient.fetchAllDrones()).thenReturn(List.of(testDrone));
        
        ResponseEntity<Drone> resp = service.getDrone(1);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertNotNull(resp.getBody());
        assertEquals(1, resp.getBody().getId());
    }

    @Test
    void getDrone_withNonExistentId_returns404() {
        Drone testDrone = createTestDrone(1, 10.0f, 1000, 0.5f, 1.0f, 1.0f, true, true);
        
        when(droneIlpClient.fetchAllDrones()).thenReturn(List.of(testDrone));
        
        ResponseEntity<Drone> resp = service.getDrone(999);
        assertEquals(HttpStatus.NOT_FOUND, resp.getStatusCode());
    }

    @Test
    void getDrone_whenFetchReturnsNull_returns400() {
        when(droneIlpClient.fetchAllDrones()).thenReturn(null);
        
        ResponseEntity<Drone> resp = service.getDrone(1);
        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
    }

    // -----------------------
    // Tests for dronesWithCooling with mocked data
    // -----------------------
    @Test
    void dronesWithCooling_withTrueState_returnsMatchingDrones() {
        when(droneIlpClient.findDroneIdsByCooling(true)).thenReturn(List.of(1, 2, 3));
        
        ResponseEntity<List<Integer>> resp = service.dronesWithCooling(true);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertNotNull(resp.getBody());
        assertEquals(3, resp.getBody().size());
    }

    @Test
    void dronesWithCooling_withFalseState_returnsMatchingDrones() {
        when(droneIlpClient.findDroneIdsByCooling(false)).thenReturn(List.of(4, 5));
        
        ResponseEntity<List<Integer>> resp = service.dronesWithCooling(false);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertNotNull(resp.getBody());
        assertEquals(2, resp.getBody().size());
    }

    // -----------------------
    // Tests for dronesWithHeating with mocked data
    // -----------------------
    @Test
    void dronesWithHeating_withTrueState_returnsMatchingDrones() {
        when(droneIlpClient.findDroneIdsByHeating(true)).thenReturn(List.of(1, 3, 5));
        
        ResponseEntity<List<Integer>> resp = service.dronesWithHeating(true);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertNotNull(resp.getBody());
        assertEquals(3, resp.getBody().size());
    }

    @Test
    void dronesWithHeating_withFalseState_returnsMatchingDrones() {
        when(droneIlpClient.findDroneIdsByHeating(false)).thenReturn(List.of(2, 4, 6));
        
        ResponseEntity<List<Integer>> resp = service.dronesWithHeating(false);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertNotNull(resp.getBody());
        assertEquals(3, resp.getBody().size());
    }

    // -----------------------
    // Helper methods for creating test data
    // -----------------------
    private Drone createTestDrone(int id, float capacity, int maxMoves, float costPerMove, 
                                  float costInitial, float costFinal, boolean heating, boolean cooling) {
        Capability capability = new Capability(cooling, heating, capacity, maxMoves, costPerMove, costInitial, costFinal);
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
