package ilp.ilp_cw.ilp_1_2.service.impl;

import ilp.ilp_cw.ilp_1_2.model.Drone;
import ilp.ilp_cw.ilp_1_2.model.DroneForServicePoint;
import ilp.ilp_cw.ilp_1_2.model.RestrictedArea;
import ilp.ilp_cw.ilp_1_2.model.ServicePoint;
import ilp.ilp_cw.ilp_1_2.model.Capability;
import ilp.ilp_cw.ilp_1_2.model.Position;
import ilp.ilp_cw.ilp_1_2.model.Region;
import ilp.ilp_cw.ilp_1_2.model.DroneAvailability;
import ilp.ilp_cw.ilp_1_2.model.Availability;
import ilp.ilp_cw.ilp_1_2.repository.DroneRepository;
import ilp.ilp_cw.ilp_1_2.repository.DronesForServicePointRepository;
import ilp.ilp_cw.ilp_1_2.repository.RestrictedAreaRepository;
import ilp.ilp_cw.ilp_1_2.repository.ServicePointsRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive JUnit tests for {@link DroneIlpClientImpl}.
 * Tests all repository interactions and data fetching methods.
 */
class DroneIlpClientImplTest {

    @Mock
    private DroneRepository droneRepository;

    @Mock
    private DronesForServicePointRepository dronesForServicePointRepository;

    @Mock
    private ServicePointsRepository servicePointsRepository;

    @Mock
    private RestrictedAreaRepository restrictedAreaRepository;

    private DroneIlpClientImpl droneIlpClient;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        droneIlpClient = new DroneIlpClientImpl(
                droneRepository,
                dronesForServicePointRepository,
                servicePointsRepository,
                restrictedAreaRepository
        );
    }

    // -----------------------
    // fetchAllDrones() tests
    // -----------------------
    @Test
    void fetchAllDrones_returnsListFromRepository() {
        // Arrange
        Drone drone1 = createTestDrone(1, 10.0f, 1000, 0.5f, 1.0f, 1.0f, true, true);
        Drone drone2 = createTestDrone(2, 15.0f, 1500, 0.6f, 1.2f, 1.2f, false, true);
        List<Drone> expectedDrones = Arrays.asList(drone1, drone2);
        
        when(droneRepository.fetchAllDronesFromAPI()).thenReturn(expectedDrones);

        // Act
        List<Drone> result = droneIlpClient.fetchAllDrones();

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(expectedDrones, result);
        verify(droneRepository, times(1)).fetchAllDronesFromAPI();
    }

    @Test
    void fetchAllDrones_returnsEmptyListWhenNoData() {
        // Arrange
        when(droneRepository.fetchAllDronesFromAPI()).thenReturn(Collections.emptyList());

        // Act
        List<Drone> result = droneIlpClient.fetchAllDrones();

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(droneRepository, times(1)).fetchAllDronesFromAPI();
    }

    @Test
    void fetchAllDrones_returnsNullWhenRepositoryReturnsNull() {
        // Arrange
        when(droneRepository.fetchAllDronesFromAPI()).thenReturn(null);

        // Act
        List<Drone> result = droneIlpClient.fetchAllDrones();

        // Assert
        assertNull(result);
        verify(droneRepository, times(1)).fetchAllDronesFromAPI();
    }

    @Test
    void fetchAllDrones_callsRepositoryOnlyOnce() {
        // Arrange
        List<Drone> drones = Collections.singletonList(
                createTestDrone(1, 10.0f, 1000, 0.5f, 1.0f, 1.0f, true, true)
        );
        when(droneRepository.fetchAllDronesFromAPI()).thenReturn(drones);

        // Act
        droneIlpClient.fetchAllDrones();

        // Assert
        verify(droneRepository, times(1)).fetchAllDronesFromAPI();
        verifyNoMoreInteractions(droneRepository);
    }

    // -----------------------
    // fetchAllDronesForServicePoints() tests
    // -----------------------
    @Test
    void fetchAllDronesForServicePoints_returnsListFromRepository() {
        // Arrange
        DroneForServicePoint dfsp1 = createTestDroneForServicePoint(1, "1");
        DroneForServicePoint dfsp2 = createTestDroneForServicePoint(2, "2");
        List<DroneForServicePoint> expectedDfsp = Arrays.asList(dfsp1, dfsp2);
        
        when(dronesForServicePointRepository.fetchAllDronesForServicePointsFromAPI())
                .thenReturn(expectedDfsp);

        // Act
        List<DroneForServicePoint> result = droneIlpClient.fetchAllDronesForServicePoints();

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(expectedDfsp, result);
        verify(dronesForServicePointRepository, times(1))
                .fetchAllDronesForServicePointsFromAPI();
    }

    @Test
    void fetchAllDronesForServicePoints_returnsEmptyListWhenNoData() {
        // Arrange
        when(dronesForServicePointRepository.fetchAllDronesForServicePointsFromAPI())
                .thenReturn(Collections.emptyList());

        // Act
        List<DroneForServicePoint> result = droneIlpClient.fetchAllDronesForServicePoints();

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(dronesForServicePointRepository, times(1))
                .fetchAllDronesForServicePointsFromAPI();
    }

    @Test
    void fetchAllDronesForServicePoints_returnsNullWhenRepositoryReturnsNull() {
        // Arrange
        when(dronesForServicePointRepository.fetchAllDronesForServicePointsFromAPI())
                .thenReturn(null);

        // Act
        List<DroneForServicePoint> result = droneIlpClient.fetchAllDronesForServicePoints();

        // Assert
        assertNull(result);
        verify(dronesForServicePointRepository, times(1))
                .fetchAllDronesForServicePointsFromAPI();
    }

    // -----------------------
    // fetchAllServicePoints() tests
    // -----------------------
    @Test
    void fetchAllServicePoints_returnsListFromRepository() {
        // Arrange
        ServicePoint sp1 = new ServicePoint(1, "SP1", 
                new Position(BigDecimal.ZERO, BigDecimal.ZERO));
        ServicePoint sp2 = new ServicePoint(2, "SP2", 
                new Position(BigDecimal.ONE, BigDecimal.ONE));
        List<ServicePoint> expectedServicePoints = Arrays.asList(sp1, sp2);
        
        when(servicePointsRepository.fetchAllServicePointsFromAPI())
                .thenReturn(expectedServicePoints);

        // Act
        List<ServicePoint> result = droneIlpClient.fetchAllServicePoints();

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(expectedServicePoints, result);
        verify(servicePointsRepository, times(1)).fetchAllServicePointsFromAPI();
    }

    @Test
    void fetchAllServicePoints_returnsEmptyListWhenNoData() {
        // Arrange
        when(servicePointsRepository.fetchAllServicePointsFromAPI())
                .thenReturn(Collections.emptyList());

        // Act
        List<ServicePoint> result = droneIlpClient.fetchAllServicePoints();

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(servicePointsRepository, times(1)).fetchAllServicePointsFromAPI();
    }

    @Test
    void fetchAllServicePoints_returnsNullWhenRepositoryReturnsNull() {
        // Arrange
        when(servicePointsRepository.fetchAllServicePointsFromAPI()).thenReturn(null);

        // Act
        List<ServicePoint> result = droneIlpClient.fetchAllServicePoints();

        // Assert
        assertNull(result);
        verify(servicePointsRepository, times(1)).fetchAllServicePointsFromAPI();
    }

    // -----------------------
    // fetchAllRestrictedAreas() tests
    // -----------------------
    @Test
    void fetchAllRestrictedAreas_returnsListFromRepository() {
        // Arrange
        RestrictedArea area1 = createTestRestrictedArea("Area1");
        RestrictedArea area2 = createTestRestrictedArea("Area2");
        List<RestrictedArea> expectedAreas = Arrays.asList(area1, area2);
        
        when(restrictedAreaRepository.fetchAllRestrictedAreasFromAPI())
                .thenReturn(expectedAreas);

        // Act
        List<RestrictedArea> result = droneIlpClient.fetchAllRestrictedAreas();

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(expectedAreas, result);
        verify(restrictedAreaRepository, times(1)).fetchAllRestrictedAreasFromAPI();
    }

    @Test
    void fetchAllRestrictedAreas_returnsEmptyListWhenNoData() {
        // Arrange
        when(restrictedAreaRepository.fetchAllRestrictedAreasFromAPI())
                .thenReturn(Collections.emptyList());

        // Act
        List<RestrictedArea> result = droneIlpClient.fetchAllRestrictedAreas();

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(restrictedAreaRepository, times(1)).fetchAllRestrictedAreasFromAPI();
    }

    @Test
    void fetchAllRestrictedAreas_returnsNullWhenRepositoryReturnsNull() {
        // Arrange
        when(restrictedAreaRepository.fetchAllRestrictedAreasFromAPI()).thenReturn(null);

        // Act
        List<RestrictedArea> result = droneIlpClient.fetchAllRestrictedAreas();

        // Assert
        assertNull(result);
        verify(restrictedAreaRepository, times(1)).fetchAllRestrictedAreasFromAPI();
    }

    // -----------------------
    // findDroneIdsByCooling() tests
    // -----------------------
    @Test
    void findDroneIdsByCooling_withTrue_returnsIdsFromRepository() {
        // Arrange
        List<Integer> expectedIds = Arrays.asList(1, 2, 3);
        when(droneRepository.findIdsByCooling(true)).thenReturn(expectedIds);

        // Act
        List<Integer> result = droneIlpClient.findDroneIdsByCooling(true);

        // Assert
        assertNotNull(result);
        assertEquals(3, result.size());
        assertEquals(expectedIds, result);
        verify(droneRepository, times(1)).findIdsByCooling(true);
    }

    @Test
    void findDroneIdsByCooling_withFalse_returnsIdsFromRepository() {
        // Arrange
        List<Integer> expectedIds = Arrays.asList(4, 5);
        when(droneRepository.findIdsByCooling(false)).thenReturn(expectedIds);

        // Act
        List<Integer> result = droneIlpClient.findDroneIdsByCooling(false);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(expectedIds, result);
        verify(droneRepository, times(1)).findIdsByCooling(false);
    }

    @Test
    void findDroneIdsByCooling_withNull_passesNullToRepository() {
        // Arrange
        when(droneRepository.findIdsByCooling(null)).thenReturn(Collections.emptyList());

        // Act
        List<Integer> result = droneIlpClient.findDroneIdsByCooling(null);

        // Assert
        assertNotNull(result);
        verify(droneRepository, times(1)).findIdsByCooling(null);
    }

    @Test
    void findDroneIdsByCooling_returnsEmptyListWhenNoMatches() {
        // Arrange
        when(droneRepository.findIdsByCooling(true)).thenReturn(Collections.emptyList());

        // Act
        List<Integer> result = droneIlpClient.findDroneIdsByCooling(true);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(droneRepository, times(1)).findIdsByCooling(true);
    }

    // -----------------------
    // findDroneIdsByHeating() tests
    // -----------------------
    @Test
    void findDroneIdsByHeating_withTrue_returnsIdsFromRepository() {
        // Arrange
        List<Integer> expectedIds = Arrays.asList(1, 3, 5);
        when(droneRepository.findIdsByHeating(true)).thenReturn(expectedIds);

        // Act
        List<Integer> result = droneIlpClient.findDroneIdsByHeating(true);

        // Assert
        assertNotNull(result);
        assertEquals(3, result.size());
        assertEquals(expectedIds, result);
        verify(droneRepository, times(1)).findIdsByHeating(true);
    }

    @Test
    void findDroneIdsByHeating_withFalse_returnsIdsFromRepository() {
        // Arrange
        List<Integer> expectedIds = Arrays.asList(2, 4, 6);
        when(droneRepository.findIdsByHeating(false)).thenReturn(expectedIds);

        // Act
        List<Integer> result = droneIlpClient.findDroneIdsByHeating(false);

        // Assert
        assertNotNull(result);
        assertEquals(3, result.size());
        assertEquals(expectedIds, result);
        verify(droneRepository, times(1)).findIdsByHeating(false);
    }

    @Test
    void findDroneIdsByHeating_withNull_passesNullToRepository() {
        // Arrange
        when(droneRepository.findIdsByHeating(null)).thenReturn(Collections.emptyList());

        // Act
        List<Integer> result = droneIlpClient.findDroneIdsByHeating(null);

        // Assert
        assertNotNull(result);
        verify(droneRepository, times(1)).findIdsByHeating(null);
    }

    @Test
    void findDroneIdsByHeating_returnsEmptyListWhenNoMatches() {
        // Arrange
        when(droneRepository.findIdsByHeating(true)).thenReturn(Collections.emptyList());

        // Act
        List<Integer> result = droneIlpClient.findDroneIdsByHeating(true);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(droneRepository, times(1)).findIdsByHeating(true);
    }

    // -----------------------
    // Multiple method interaction tests
    // -----------------------
    @Test
    void multipleCalls_eachCallsRepositoryIndependently() {
        // Arrange
        List<Drone> drones = Collections.singletonList(
                createTestDrone(1, 10.0f, 1000, 0.5f, 1.0f, 1.0f, true, true)
        );
        List<ServicePoint> servicePoints = Collections.singletonList(
                new ServicePoint(1, "SP1", new Position(BigDecimal.ZERO, BigDecimal.ZERO))
        );
        
        when(droneRepository.fetchAllDronesFromAPI()).thenReturn(drones);
        when(servicePointsRepository.fetchAllServicePointsFromAPI()).thenReturn(servicePoints);

        // Act
        droneIlpClient.fetchAllDrones();
        droneIlpClient.fetchAllServicePoints();

        // Assert
        verify(droneRepository, times(1)).fetchAllDronesFromAPI();
        verify(servicePointsRepository, times(1)).fetchAllServicePointsFromAPI();
        verifyNoInteractions(dronesForServicePointRepository);
        verifyNoInteractions(restrictedAreaRepository);
    }

    @Test
    void allFetchMethods_canBeCalledSuccessfully() {
        // Arrange
        when(droneRepository.fetchAllDronesFromAPI()).thenReturn(Collections.emptyList());
        when(dronesForServicePointRepository.fetchAllDronesForServicePointsFromAPI())
                .thenReturn(Collections.emptyList());
        when(servicePointsRepository.fetchAllServicePointsFromAPI())
                .thenReturn(Collections.emptyList());
        when(restrictedAreaRepository.fetchAllRestrictedAreasFromAPI())
                .thenReturn(Collections.emptyList());

        // Act & Assert
        assertNotNull(droneIlpClient.fetchAllDrones());
        assertNotNull(droneIlpClient.fetchAllDronesForServicePoints());
        assertNotNull(droneIlpClient.fetchAllServicePoints());
        assertNotNull(droneIlpClient.fetchAllRestrictedAreas());
    }

    @Test
    void filterMethods_canBeCalledSuccessfully() {
        // Arrange
        when(droneRepository.findIdsByCooling(true)).thenReturn(List.of(1, 2));
        when(droneRepository.findIdsByHeating(true)).thenReturn(List.of(3, 4));

        // Act & Assert
        assertNotNull(droneIlpClient.findDroneIdsByCooling(true));
        assertNotNull(droneIlpClient.findDroneIdsByHeating(true));
        
        verify(droneRepository, times(1)).findIdsByCooling(true);
        verify(droneRepository, times(1)).findIdsByHeating(true);
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

    private DroneForServicePoint createTestDroneForServicePoint(Integer servicePointId, String droneId) {
        Availability availability = new Availability("MONDAY", LocalTime.of(9, 0), LocalTime.of(17, 0));
        DroneAvailability droneAvailability = new DroneAvailability(droneId, List.of(availability));
        return new DroneForServicePoint(servicePointId, List.of(droneAvailability));
    }

    private RestrictedArea createTestRestrictedArea(String name) {
        List<Position> vertices = Arrays.asList(
                new Position(BigDecimal.ZERO, BigDecimal.ZERO),
                new Position(BigDecimal.ZERO, BigDecimal.ONE),
                new Position(BigDecimal.ONE, BigDecimal.ONE),
                new Position(BigDecimal.ONE, BigDecimal.ZERO)
        );
        RestrictedArea area = new RestrictedArea(vertices, 1, name);
        return area;
    }
}
