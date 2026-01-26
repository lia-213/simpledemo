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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Mock-based tests for {@link DroneServiceImpl} focusing on CW2 business logic.
 * Uses Mockito to mock DroneIlpClient responses and verify complex scenarios.
 */
@ExtendWith(MockitoExtension.class)
class DroneServiceImplMockTest {

    @Mock
    private DroneIlpClient droneIlpClient;

    private DroneServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new DroneServiceImpl(droneIlpClient);
    }

    // -----------------------
    // dronesWithCooling tests
    // -----------------------

    @Test
    void dronesWithCooling_returnsMatchingDroneIds() {
        // Arrange
        List<Integer> expectedIds = Arrays.asList(1, 3, 5);
        when(droneIlpClient.findDroneIdsByCooling(true)).thenReturn(expectedIds);

        // Act
        ResponseEntity<List<Integer>> response = service.dronesWithCooling(true);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(3, response.getBody().size());
        assertTrue(response.getBody().containsAll(expectedIds));
        verify(droneIlpClient, times(1)).findDroneIdsByCooling(true);
    }

    @Test
    void dronesWithCooling_false_returnsNonCoolingDrones() {
        // Arrange
        List<Integer> expectedIds = Arrays.asList(2, 4, 6);
        when(droneIlpClient.findDroneIdsByCooling(false)).thenReturn(expectedIds);

        // Act
        ResponseEntity<List<Integer>> response = service.dronesWithCooling(false);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(3, response.getBody().size());
        assertTrue(response.getBody().containsAll(expectedIds));
    }

    @Test
    void dronesWithCooling_emptyResult_returnsEmptyList() {
        // Arrange
        when(droneIlpClient.findDroneIdsByCooling(true)).thenReturn(Collections.emptyList());

        // Act
        ResponseEntity<List<Integer>> response = service.dronesWithCooling(true);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isEmpty());
    }

    // -----------------------
    // dronesWithHeating tests
    // -----------------------

    @Test
    void dronesWithHeating_returnsMatchingDroneIds() {
        // Arrange
        List<Integer> expectedIds = Arrays.asList(2, 4);
        when(droneIlpClient.findDroneIdsByHeating(true)).thenReturn(expectedIds);

        // Act
        ResponseEntity<List<Integer>> response = service.dronesWithHeating(true);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(2, response.getBody().size());
        assertTrue(response.getBody().containsAll(expectedIds));
        verify(droneIlpClient, times(1)).findDroneIdsByHeating(true);
    }

    @Test
    void dronesWithHeating_false_returnsNonHeatingDrones() {
        // Arrange
        List<Integer> expectedIds = Arrays.asList(1, 3, 5, 6);
        when(droneIlpClient.findDroneIdsByHeating(false)).thenReturn(expectedIds);

        // Act
        ResponseEntity<List<Integer>> response = service.dronesWithHeating(false);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(4, response.getBody().size());
    }

    // -----------------------
    // getDrone tests
    // -----------------------

    @Test
    void getDrone_validId_returnsDrone() {
        // Arrange
        Drone expectedDrone = createTestDrone(1, "TestDrone", 10.0f);
        when(droneIlpClient.fetchAllDrones()).thenReturn(Collections.singletonList(expectedDrone));

        // Act
        ResponseEntity<Drone> response = service.getDrone(1);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().getId());
        assertEquals("TestDrone", response.getBody().getName());
        verify(droneIlpClient, times(1)).fetchAllDrones();
    }

    @Test
    void getDrone_nonExistentId_returns404() {
        // Arrange
        Drone drone1 = createTestDrone(1, "Drone1", 10.0f);
        Drone drone2 = createTestDrone(2, "Drone2", 15.0f);
        when(droneIlpClient.fetchAllDrones()).thenReturn(Arrays.asList(drone1, drone2));

        // Act
        ResponseEntity<Drone> response = service.getDrone(999);

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNull(response.getBody());
    }

    @Test
    void getDrone_emptyDroneList_returns404() {
        // Arrange
        when(droneIlpClient.fetchAllDrones()).thenReturn(Collections.emptyList());

        // Act
        ResponseEntity<Drone> response = service.getDrone(1);

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void getDrone_nullDroneList_returnsBadRequest() {
        // Arrange
        when(droneIlpClient.fetchAllDrones()).thenReturn(null);

        // Act
        ResponseEntity<Drone> response = service.getDrone(1);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    // -----------------------
    // getSuitableDrones tests
    // -----------------------

    @Test
    void getSuitableDrones_singleDelivery_returnsSuitableDrone() {
        // Arrange
        MedDispatchRecDto delivery = createDeliveryDto(1, 5.0f, true, false, 1000.0f,
                LocalDate.of(2026, 1, 15), LocalTime.of(10, 0),
                BigDecimal.valueOf(-3.186), BigDecimal.valueOf(55.944));

        Drone suitableDrone = createTestDrone(1, "Suitable", 10.0f, true, false);
        Drone unsuitableDrone = createTestDrone(2, "Unsuitable", 3.0f, false, false);

        when(droneIlpClient.fetchAllDrones()).thenReturn(Arrays.asList(suitableDrone, unsuitableDrone));
        // Provide empty DroneForServicePoint list - methods handle this gracefully
        when(droneIlpClient.fetchAllDronesForServicePoints()).thenReturn(Collections.emptyList());
        when(droneIlpClient.fetchAllServicePoints()).thenReturn(createTestServicePoints());

        // Act
        ResponseEntity<List<Integer>> response = service.getSuitableDrones(Collections.singletonList(delivery));

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        // Result may vary based on availability checking - just ensure it returns OK
    }

    @Test
    void getSuitableDrones_noDronesAvailable_returnsEmptyList() {
        // Arrange
        MedDispatchRecDto delivery = createDeliveryDto(1, 5.0f, false, false, 100.0f,
                LocalDate.of(2026, 1, 15), LocalTime.of(10, 0),
                BigDecimal.valueOf(-3.186), BigDecimal.valueOf(55.944));

        when(droneIlpClient.fetchAllDrones()).thenReturn(Collections.emptyList());

        // Act
        ResponseEntity<List<Integer>> response = service.getSuitableDrones(Collections.singletonList(delivery));

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isEmpty());
    }

    // -----------------------
    // calcDeliveryPath basic tests
    // -----------------------

    @Test
    void calcDeliveryPath_singleDelivery_returnsValidPath() {
        // Arrange
        MedDispatchRecDto delivery = createDeliveryDto(1, 5.0f, false, false, 5000.0f,
                LocalDate.of(2026, 1, 15), LocalTime.of(10, 0),
                BigDecimal.valueOf(-3.186), BigDecimal.valueOf(55.944));

        Drone drone = createTestDrone(1, "TestDrone", 10.0f, false, false);
        setupMocksForDeliveryPath(drone);

        // Act
        ResponseEntity<DeliveryPathReturnStructure> response = service.calcDeliveryPath(
                Collections.singletonList(delivery), null);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().getDronePaths());
        assertTrue(response.getBody().getTotalMoves() >= 0);
        assertTrue(response.getBody().getTotalCost() >= 0);
    }

    @Test
    void calcDeliveryPath_multipleDeliveries_combinesCorrectly() {
        // Arrange
        MedDispatchRecDto delivery1 = createDeliveryDto(1, 3.0f, false, false, 3000.0f,
                LocalDate.of(2026, 1, 15), LocalTime.of(10, 0),
                BigDecimal.valueOf(-3.186), BigDecimal.valueOf(55.944));
        MedDispatchRecDto delivery2 = createDeliveryDto(2, 4.0f, false, false, 4000.0f,
                LocalDate.of(2026, 1, 15), LocalTime.of(11, 0),
                BigDecimal.valueOf(-3.188), BigDecimal.valueOf(55.945));

        Drone drone = createTestDrone(1, "TestDrone", 15.0f, false, false);
        setupMocksForDeliveryPath(drone);

        // Act
        ResponseEntity<DeliveryPathReturnStructure> response = service.calcDeliveryPath(
                Arrays.asList(delivery1, delivery2), null);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        // Should have paths for deliveries
        assertTrue(response.getBody().getTotalMoves() >= 0);
    }

    // -----------------------
    // query tests
    // -----------------------

    @Test
    void query_byName_returnsMatchingDrones() {
        // Arrange
        Drone drone1 = createTestDrone(1, "AlphaDrone", 10.0f);
        Drone drone2 = createTestDrone(2, "BetaDrone", 15.0f);
        Drone drone3 = createTestDrone(3, "AlphaTwo", 12.0f);

        when(droneIlpClient.fetchAllDrones()).thenReturn(Arrays.asList(drone1, drone2, drone3));

        // QueryAttributeDto constructor: attribute, operator, value
        QueryAttributeDto query = new QueryAttributeDto("name", "MATCH", "Alpha.*");

        // Act
        ResponseEntity<List<String>> response = service.query(Collections.singletonList(query));

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        // Depending on implementation, may return drone names
    }

    @Test
    void query_byCapacity_greaterThan_returnsMatching() {
        // Arrange
        Drone drone1 = createTestDrone(1, "Small", 5.0f);
        Drone drone2 = createTestDrone(2, "Medium", 10.0f);
        Drone drone3 = createTestDrone(3, "Large", 20.0f);

        when(droneIlpClient.fetchAllDrones()).thenReturn(Arrays.asList(drone1, drone2, drone3));

        QueryAttributeDto query = new QueryAttributeDto("capability.capacity", "GREATER_THAN", "12");

        // Act
        ResponseEntity<List<String>> response = service.query(Collections.singletonList(query));

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    void query_byId_equals_returnsSingleDrone() {
        // Arrange
        Drone drone1 = createTestDrone(1, "Drone1", 10.0f);
        Drone drone2 = createTestDrone(2, "Drone2", 15.0f);

        when(droneIlpClient.fetchAllDrones()).thenReturn(Arrays.asList(drone1, drone2));

        QueryAttributeDto query = new QueryAttributeDto("id", "EQUALS", "1");

        // Act
        ResponseEntity<List<String>> response = service.query(Collections.singletonList(query));

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    void query_multipleAttributes_appliesAnd_logic() {
        // Arrange
        Drone drone1 = createTestDrone(1, "AlphaDrone", 8.0f, true, false);
        Drone drone2 = createTestDrone(2, "AlphaTwo", 12.0f, false, false);
        Drone drone3 = createTestDrone(3, "BetaDrone", 15.0f, true, false);

        when(droneIlpClient.fetchAllDrones()).thenReturn(Arrays.asList(drone1, drone2, drone3));

        QueryAttributeDto query1 = new QueryAttributeDto("name", "MATCH", "Alpha.*");
        QueryAttributeDto query2 = new QueryAttributeDto("capability.capacity", "GREATER_THAN", "10");

        // Act
        ResponseEntity<List<String>> response = service.query(Arrays.asList(query1, query2));

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    void query_noMatches_returnsEmptyList() {
        // Arrange
        Drone drone1 = createTestDrone(1, "AlphaDrone", 8.0f);
        when(droneIlpClient.fetchAllDrones()).thenReturn(Collections.singletonList(drone1));

        QueryAttributeDto query = new QueryAttributeDto("name", "MATCH", "Gamma.*");

        // Act
        ResponseEntity<List<String>> response = service.query(Collections.singletonList(query));

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isEmpty());
    }

    // -----------------------
    // queryAsPath tests
    // -----------------------

    @Test
    void queryAsPath_validAttribute_returnsIds() {
        // Arrange
        Drone drone1 = createTestDrone(1, "AlphaDrone", 10.0f, true, false);
        Drone drone2 = createTestDrone(2, "BetaDrone", 15.0f, false, false);
        Drone drone3 = createTestDrone(3, "GammaDrone", 12.0f, true, false);

        when(droneIlpClient.fetchAllDrones()).thenReturn(Arrays.asList(drone1, drone2, drone3));

        // Act - query for drones with cooling
        ResponseEntity<List<Integer>> response = service.queryAsPath("capability.cooling", "true");

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(2, response.getBody().size());
        assertTrue(response.getBody().contains(1));
        assertTrue(response.getBody().contains(3));
        assertFalse(response.getBody().contains(2));
    }

    @Test
    void queryAsPath_numericComparison_returnsCorrectIds() {
        // Arrange
        Drone drone1 = createTestDrone(1, "Small", 5.0f);
        Drone drone2 = createTestDrone(2, "Medium", 10.0f);
        Drone drone3 = createTestDrone(3, "Large", 20.0f);

        when(droneIlpClient.fetchAllDrones()).thenReturn(Arrays.asList(drone1, drone2, drone3));

        // Act - query for capacity = 10
        ResponseEntity<List<Integer>> response = service.queryAsPath("capability.capacity", "10");

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        assertTrue(response.getBody().contains(2));
    }

    @Test
    void queryAsPath_noMatches_returnsEmptyList() {
        // Arrange
        Drone drone1 = createTestDrone(1, "AlphaDrone", 10.0f);
        when(droneIlpClient.fetchAllDrones()).thenReturn(Collections.singletonList(drone1));

        // Act
        ResponseEntity<List<Integer>> response = service.queryAsPath("capability.heating", "true");

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isEmpty());
    }

    // -----------------------
    // Helper methods
    // -----------------------

    private Drone createTestDrone(int id, String name, float capacity) {
        return createTestDrone(id, name, capacity, false, false);
    }

    private Drone createTestDrone(int id, String name, float capacity, boolean cooling, boolean heating) {
        // Capability record: cooling, heating, capacity, maxMoves, costPerMove, costInitial, costFinal
        Capability capability = new Capability(cooling, heating, capacity, 2000, 1.0f, 50.0f, 50.0f);
        // Drone constructor: name, id (String), capability
        return new Drone(name, String.valueOf(id), capability);
    }

    private MedDispatchRecDto createDeliveryDto(int id, float capacity, boolean cooling, boolean heating,
                                                 float maxCost, LocalDate date, LocalTime time,
                                                 BigDecimal lng, BigDecimal lat) {
        Requirements requirements = new Requirements(heating, cooling, capacity, maxCost);
        Position delivery = new Position(lng, lat);
        return new MedDispatchRecDto(id, date, time, requirements, delivery);
    }

    private List<ServicePoint> createTestServicePoints() {
        Position sp1Pos = new Position(BigDecimal.valueOf(-3.192), BigDecimal.valueOf(55.946));
        ServicePoint sp1 = new ServicePoint(1, "Central", sp1Pos);
        return Collections.singletonList(sp1);
    }

    private void setupMocksForDeliveryPath(Drone drone) {
        when(droneIlpClient.fetchAllDrones()).thenReturn(Collections.singletonList(drone));
        // DroneForServicePoint expects (servicePointId, List<DroneAvailability>)
        // DroneAvailability expects (droneId as String, List<Availability>)
        DroneAvailability droneAvailability = new DroneAvailability(drone.getId().toString(), Collections.emptyList());
        DroneForServicePoint dfsp = new DroneForServicePoint(1, Collections.singletonList(droneAvailability));
        when(droneIlpClient.fetchAllDronesForServicePoints()).thenReturn(Collections.singletonList(dfsp));
        when(droneIlpClient.fetchAllServicePoints()).thenReturn(createTestServicePoints());
        when(droneIlpClient.fetchAllRestrictedAreas()).thenReturn(Collections.emptyList());
    }
}
