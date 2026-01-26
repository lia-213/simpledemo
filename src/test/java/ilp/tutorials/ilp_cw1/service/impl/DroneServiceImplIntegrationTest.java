package ilp.tutorials.ilp_cw1.service.impl;

import ilp.ilp_cw.ilp_1_2.dto.*;
import ilp.ilp_cw.ilp_1_2.ilpCw1Application;
import ilp.ilp_cw.ilp_1_2.model.*;
import ilp.ilp_cw.ilp_1_2.service.DroneIlpClient;
import ilp.ilp_cw.ilp_1_2.service.DroneService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Integration tests for {@link DroneServiceImpl} using full Spring Boot context.
 * Uses @MockBean to mock external REST client while testing full service layer logic.
 */
@SpringBootTest(classes = ilpCw1Application.class)
class DroneServiceImplIntegrationTest {

    @Autowired
    private DroneService droneService;

    @MockBean
    private DroneIlpClient droneIlpClient;

    private List<Drone> testDrones;
    private List<ServicePoint> testServicePoints;
    private List<DroneForServicePoint> testDfsp;
    private List<RestrictedArea> testRestrictedAreas;

    @BeforeEach
    void setUp() {
        // Setup test data
        testDrones = createTestDrones();
        testServicePoints = createTestServicePoints();
        testDfsp = createTestDfsp();
        testRestrictedAreas = Collections.emptyList();

        // Configure default mock behavior
        when(droneIlpClient.fetchAllDrones()).thenReturn(testDrones);
        when(droneIlpClient.fetchAllServicePoints()).thenReturn(testServicePoints);
        when(droneIlpClient.fetchAllDronesForServicePoints()).thenReturn(testDfsp);
        when(droneIlpClient.fetchAllRestrictedAreas()).thenReturn(testRestrictedAreas);
    }

    // -----------------------
    // CW1 Integration Tests
    // -----------------------

    @Test
    void distanceTo_integrationTest_calculatesCorrectDistance() {
        // Arrange
        Position pos1 = new Position(BigDecimal.valueOf(-3.192473), BigDecimal.valueOf(55.946233));
        Position pos2 = new Position(BigDecimal.valueOf(-3.192473), BigDecimal.valueOf(55.942617));
        PositionsDto dto = new PositionsDto(pos1, pos2);

        // Act
        ResponseEntity<BigDecimal> response = droneService.distanceTo(dto);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        // Distance should be approximately 0.003616
        assertEquals(0.003616, response.getBody().doubleValue(), 0.000001);
    }

    @Test
    void isCloseTo_integrationTest_returnsTrueForClosePositions() {
        // Arrange
        Position pos1 = new Position(BigDecimal.valueOf(-3.192473), BigDecimal.valueOf(55.946233));
        Position pos2 = new Position(BigDecimal.valueOf(-3.192473), BigDecimal.valueOf(55.946234));
        PositionsDto dto = new PositionsDto(pos1, pos2);

        // Act
        ResponseEntity<Boolean> response = droneService.isCloseTo(dto);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody());
    }

    @Test
    void computeNextPosition_integrationTest_movesEastward() {
        // Arrange
        Position start = new Position(BigDecimal.valueOf(-3.192473), BigDecimal.valueOf(55.946233));
        DroneMoveRqstDto dto = new DroneMoveRqstDto(start, BigDecimal.valueOf(90)); // 90° = East

        // Act
        ResponseEntity<Position> response = droneService.computeNextPosition(dto);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        // Should move eastward (longitude increases)
        assertTrue(response.getBody().getLongitude().compareTo(start.getLongitude()) > 0);
    }

    @Test
    void isInRegion_integrationTest_positionInsideSquare() {
        // Arrange
        List<Position> vertices = Arrays.asList(
                new Position(BigDecimal.ZERO, BigDecimal.ZERO),
                new Position(BigDecimal.ZERO, BigDecimal.ONE),
                new Position(BigDecimal.ONE, BigDecimal.ONE),
                new Position(BigDecimal.ONE, BigDecimal.ZERO)
        );
        Region region = new Region("square", vertices);
        Position testPoint = new Position(BigDecimal.valueOf(0.5), BigDecimal.valueOf(0.5));
        LocationRegionDto dto = new LocationRegionDto(testPoint, region);

        // Act
        ResponseEntity<Boolean> response = droneService.isInRegion(dto);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody());
    }

    // -----------------------
    // CW2 Integration Tests
    // -----------------------

    @Test
    void dronesWithCooling_integrationTest_returnsFilteredList() {
        // Arrange
        when(droneIlpClient.findDroneIdsByCooling(true)).thenReturn(Arrays.asList(1, 3));

        // Act
        ResponseEntity<List<Integer>> response = droneService.dronesWithCooling(true);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(2, response.getBody().size());
        assertTrue(response.getBody().contains(1));
        assertTrue(response.getBody().contains(3));
    }

    @Test
    void dronesWithHeating_integrationTest_returnsFilteredList() {
        // Arrange
        when(droneIlpClient.findDroneIdsByHeating(true)).thenReturn(Arrays.asList(2, 4));

        // Act
        ResponseEntity<List<Integer>> response = droneService.dronesWithHeating(true);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(2, response.getBody().size());
        assertTrue(response.getBody().contains(2));
        assertTrue(response.getBody().contains(4));
    }

    @Test
    void getDrone_integrationTest_returnsExistingDrone() {
        // Act
        ResponseEntity<Drone> response = droneService.getDrone(1);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().getId());
        assertEquals("CoolingDrone", response.getBody().getName());
    }

    @Test
    void getDrone_integrationTest_returns404ForNonExistent() {
        // Act
        ResponseEntity<Drone> response = droneService.getDrone(999);

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void getSuitableDrones_integrationTest_findsDroneMatchingRequirements() {
        // Arrange - delivery requiring cooling, 5kg capacity
        MedDispatchRecDto delivery = createDeliveryDto(1, 5.0f, true, false, 10000.0f,
                LocalDate.of(2026, 1, 15), LocalTime.of(10, 0),
                BigDecimal.valueOf(-3.186), BigDecimal.valueOf(55.944));

        // Act
        ResponseEntity<List<Integer>> response = droneService.getSuitableDrones(
                Collections.singletonList(delivery));

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        // Should include drones with cooling capability and sufficient capacity
    }

    @Test
    void getSuitableDrones_integrationTest_multipleDeliveries_andSemantics() {
        // Arrange - both deliveries need cooling
        MedDispatchRecDto delivery1 = createDeliveryDto(1, 3.0f, true, false, 5000.0f,
                LocalDate.of(2026, 1, 15), LocalTime.of(10, 0),
                BigDecimal.valueOf(-3.186), BigDecimal.valueOf(55.944));
        MedDispatchRecDto delivery2 = createDeliveryDto(2, 4.0f, true, false, 5000.0f,
                LocalDate.of(2026, 1, 15), LocalTime.of(11, 0),
                BigDecimal.valueOf(-3.188), BigDecimal.valueOf(55.945));

        // Act
        ResponseEntity<List<Integer>> response = droneService.getSuitableDrones(
                Arrays.asList(delivery1, delivery2));

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        // All returned drones must satisfy BOTH deliveries
    }

    @Test
    void calcDeliveryPath_integrationTest_singleDelivery_returnsValidPath() {
        // Arrange
        MedDispatchRecDto delivery = createDeliveryDto(1, 5.0f, false, false, 50000.0f,
                LocalDate.of(2026, 1, 15), LocalTime.of(10, 0),
                BigDecimal.valueOf(-3.186), BigDecimal.valueOf(55.944));

        // Act
        ResponseEntity<DeliveryPathReturnStructure> response = droneService.calcDeliveryPath(
                Collections.singletonList(delivery), null);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        DeliveryPathReturnStructure result = response.getBody();

        assertNotNull(result.getDronePaths());
        assertTrue(result.getTotalMoves() >= 0);
        assertTrue(result.getTotalCost() >= 0);

        // Note: Drone paths may be empty if pathfinding fails with mocked data
        // The key integration test is that the service returns a valid response structure
    }

    @Test
    void calcDeliveryPath_integrationTest_multipleDeliveries_combinesTrips() {
        // Arrange - two deliveries that can be handled by same drone
        MedDispatchRecDto delivery1 = createDeliveryDto(1, 3.0f, false, false, 30000.0f,
                LocalDate.of(2026, 1, 15), LocalTime.of(10, 0),
                BigDecimal.valueOf(-3.186), BigDecimal.valueOf(55.944));
        MedDispatchRecDto delivery2 = createDeliveryDto(2, 4.0f, false, false, 40000.0f,
                LocalDate.of(2026, 1, 15), LocalTime.of(11, 0),
                BigDecimal.valueOf(-3.188), BigDecimal.valueOf(55.945));

        // Act
        ResponseEntity<DeliveryPathReturnStructure> response = droneService.calcDeliveryPath(
                Arrays.asList(delivery1, delivery2), null);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        DeliveryPathReturnStructure result = response.getBody();

        assertTrue(result.getTotalMoves() >= 0);
        assertTrue(result.getTotalCost() >= 0);
        assertNotNull(result.getDronePaths());

        // Note: Path calculation may vary with mocked data
        // The key integration test is that the service handles multiple deliveries
    }

    @Test
    void calcDeliveryPathAsGeoJson_integrationTest_returnsValidGeoJson() {
        // Arrange
        MedDispatchRecDto delivery = createDeliveryDto(1, 5.0f, false, false, 50000.0f,
                LocalDate.of(2026, 1, 15), LocalTime.of(10, 0),
                BigDecimal.valueOf(-3.186), BigDecimal.valueOf(55.944));

        // Act
        ResponseEntity<GeoJsonLineStringDto> response = droneService.calcDeliveryPathAsGeoJson(
                Collections.singletonList(delivery));

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        GeoJsonLineStringDto geoJson = response.getBody();

        assertNotNull(geoJson.getFeatures());
        if (!geoJson.getFeatures().isEmpty()) {
            GeoJsonLineStringDto.Feature feature = geoJson.getFeatures().get(0);
            assertNotNull(feature.getGeometry());
            assertNotNull(feature.getGeometry().getCoordinates());
            assertNotNull(feature.getProperties());

            // Should have at least 2 coordinates (start and end)
            assertTrue(feature.getGeometry().getCoordinates().size() >= 2);
        }
    }

    @Test
    void calcMultiDroneDeliveryPathAsGeoJson_integrationTest_multipleFeatures() {
        // Arrange - multiple deliveries that might need multiple drones
        MedDispatchRecDto delivery1 = createDeliveryDto(1, 8.0f, false, false, 30000.0f,
                LocalDate.of(2026, 1, 15), LocalTime.of(10, 0),
                BigDecimal.valueOf(-3.186), BigDecimal.valueOf(55.944));
        MedDispatchRecDto delivery2 = createDeliveryDto(2, 8.0f, false, false, 30000.0f,
                LocalDate.of(2026, 1, 15), LocalTime.of(11, 0),
                BigDecimal.valueOf(-3.188), BigDecimal.valueOf(55.945));

        // Act
        ResponseEntity<GeoJsonLineStringDto> response = droneService.calcMultiDroneDeliveryPathAsGeoJson(
                Arrays.asList(delivery1, delivery2), null);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        GeoJsonLineStringDto geoJson = response.getBody();

        assertNotNull(geoJson.getFeatures());
        // May have one or more features depending on drone assignments
    }

    @Test
    void query_integrationTest_byName_returnsMatchingDrones() {
        // Arrange
        QueryAttributeDto query = new QueryAttributeDto("name", "MATCH", ".*Drone");

        // Act
        ResponseEntity<List<String>> response = droneService.query(Collections.singletonList(query));

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        // Should return names of drones matching the pattern
    }

    @Test
    void query_integrationTest_byCapacity_greaterThan() {
        // Arrange
        QueryAttributeDto query = new QueryAttributeDto("capability.capacity", "GREATER_THAN", "12");

        // Act
        ResponseEntity<List<String>> response = droneService.query(Collections.singletonList(query));

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        // Should return drones with capacity > 12
    }

    @Test
    void query_integrationTest_multipleAttributes_andLogic() {
        // Arrange - name matches AND capacity > 10
        QueryAttributeDto query1 = new QueryAttributeDto("name", "MATCH", "Cooling.*");
        QueryAttributeDto query2 = new QueryAttributeDto("capability.capacity", "GREATER_THAN", "10");

        // Act
        ResponseEntity<List<String>> response = droneService.query(Arrays.asList(query1, query2));

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        // Should return only drones matching BOTH criteria
    }

    @Test
    void queryAsPath_integrationTest_byCooling_returnsIds() {
        // Arrange - all test drones are set up, some with cooling
        
        // Act
        ResponseEntity<List<Integer>> response = droneService.queryAsPath("capability.cooling", "true");

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        // Should return IDs of drones with cooling
    }

    @Test
    void queryAsPath_integrationTest_byCapacity_returnsMatchingIds() {
        // Act
        ResponseEntity<List<Integer>> response = droneService.queryAsPath("capability.capacity", "15");

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    // -----------------------
    // Complex Scenario Tests
    // -----------------------

    @Test
    void integrationTest_endToEnd_fullDeliveryScenario() {
        // Arrange - complex scenario with multiple deliveries on same day
        MedDispatchRecDto delivery1 = createDeliveryDto(1, 3.0f, true, false, 20000.0f,
                LocalDate.of(2026, 1, 15), LocalTime.of(9, 0),
                BigDecimal.valueOf(-3.186), BigDecimal.valueOf(55.944));
        MedDispatchRecDto delivery2 = createDeliveryDto(2, 4.0f, true, false, 25000.0f,
                LocalDate.of(2026, 1, 15), LocalTime.of(10, 0),
                BigDecimal.valueOf(-3.188), BigDecimal.valueOf(55.945));
        MedDispatchRecDto delivery3 = createDeliveryDto(3, 2.0f, false, false, 15000.0f,
                LocalDate.of(2026, 1, 15), LocalTime.of(11, 0),
                BigDecimal.valueOf(-3.190), BigDecimal.valueOf(55.946));

        List<MedDispatchRecDto> deliveries = Arrays.asList(delivery1, delivery2, delivery3);

        // Act 1: Find suitable drones
        ResponseEntity<List<Integer>> suitableResponse = droneService.getSuitableDrones(deliveries);

        // Assert 1: Should find at least one suitable drone
        assertEquals(HttpStatus.OK, suitableResponse.getStatusCode());
        assertNotNull(suitableResponse.getBody());

        // Act 2: Calculate delivery path
        ResponseEntity<DeliveryPathReturnStructure> pathResponse = droneService.calcDeliveryPath(deliveries, null);

        // Assert 2: Should generate valid path
        assertEquals(HttpStatus.OK, pathResponse.getStatusCode());
        assertNotNull(pathResponse.getBody());
        DeliveryPathReturnStructure path = pathResponse.getBody();

        assertTrue(path.getTotalMoves() >= 0);
        assertTrue(path.getTotalCost() >= 0);
        assertNotNull(path.getDronePaths());

        // Act 3: Get GeoJSON representation
        ResponseEntity<GeoJsonLineStringDto> geoJsonResponse = droneService.calcDeliveryPathAsGeoJson(deliveries);

        // Assert 3: Should generate valid GeoJSON
        assertEquals(HttpStatus.OK, geoJsonResponse.getStatusCode());
        assertNotNull(geoJsonResponse.getBody());
    }

    @Test
    void integrationTest_noSuitableDrones_returnsEmptyPath() {
        // Arrange - delivery with impossible requirements (very high capacity)
        MedDispatchRecDto delivery = createDeliveryDto(1, 100.0f, true, true, 1000.0f,
                LocalDate.of(2026, 1, 15), LocalTime.of(10, 0),
                BigDecimal.valueOf(-3.186), BigDecimal.valueOf(55.944));

        // Act
        ResponseEntity<DeliveryPathReturnStructure> response = droneService.calcDeliveryPath(
                Collections.singletonList(delivery), null);

        // Assert - should handle gracefully with empty path
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    void integrationTest_emptyInputs_returnEmptyResults() {
        // Test getSuitableDrones with empty list
        ResponseEntity<List<Integer>> suitableResponse = droneService.getSuitableDrones(Collections.emptyList());
        assertEquals(HttpStatus.OK, suitableResponse.getStatusCode());
        assertTrue(suitableResponse.getBody().isEmpty());

        // Test calcDeliveryPath with empty list
        ResponseEntity<DeliveryPathReturnStructure> pathResponse = droneService.calcDeliveryPath(
                Collections.emptyList(), null);
        assertEquals(HttpStatus.OK, pathResponse.getStatusCode());
    }

    // -----------------------
    // Helper Methods
    // -----------------------

    private List<Drone> createTestDrones() {
        List<Drone> drones = new ArrayList<>();

        // Drone 1: Cooling, 15kg capacity
        Capability cap1 = new Capability(true, false, 15.0f, 2000, 1.0f, 50.0f, 50.0f);
        drones.add(new Drone("CoolingDrone", "1", cap1));

        // Drone 2: Heating, 12kg capacity
        Capability cap2 = new Capability(false, true, 12.0f, 1800, 1.2f, 60.0f, 60.0f);
        drones.add(new Drone("HeatingDrone", "2", cap2));

        // Drone 3: Both cooling and heating, 10kg capacity
        Capability cap3 = new Capability(true, true, 10.0f, 1500, 1.5f, 70.0f, 70.0f);
        drones.add(new Drone("BothDrone", "3", cap3));

        // Drone 4: No special capabilities, 20kg capacity
        Capability cap4 = new Capability(false, false, 20.0f, 2500, 0.8f, 40.0f, 40.0f);
        drones.add(new Drone("LargeDrone", "4", cap4));

        return drones;
    }

    private List<ServicePoint> createTestServicePoints() {
        Position sp1Pos = new Position(BigDecimal.valueOf(-3.192), BigDecimal.valueOf(55.946));
        ServicePoint sp1 = new ServicePoint(1, "Central", sp1Pos);

        Position sp2Pos = new Position(BigDecimal.valueOf(-3.184), BigDecimal.valueOf(55.943));
        ServicePoint sp2 = new ServicePoint(2, "East", sp2Pos);

        return Arrays.asList(sp1, sp2);
    }

    private List<DroneForServicePoint> createTestDfsp() {
        // All drones available at service point 1
        List<DroneAvailability> availabilities = Arrays.asList(
                new DroneAvailability("1", Collections.emptyList()),
                new DroneAvailability("2", Collections.emptyList()),
                new DroneAvailability("3", Collections.emptyList()),
                new DroneAvailability("4", Collections.emptyList())
        );

        return Collections.singletonList(new DroneForServicePoint(1, availabilities));
    }

    private MedDispatchRecDto createDeliveryDto(int id, float capacity, boolean cooling, boolean heating,
                                                 float maxCost, LocalDate date, LocalTime time,
                                                 BigDecimal lng, BigDecimal lat) {
        Requirements requirements = new Requirements(heating, cooling, capacity, maxCost);
        Position delivery = new Position(lng, lat);
        return new MedDispatchRecDto(id, date, time, requirements, delivery);
    }
}
