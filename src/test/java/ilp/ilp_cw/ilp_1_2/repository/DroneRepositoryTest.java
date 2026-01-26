package ilp.ilp_cw.ilp_1_2.repository;

import ilp.ilp_cw.ilp_1_2.IlpConfig;
import ilp.ilp_cw.ilp_1_2.model.Drone;
import ilp.ilp_cw.ilp_1_2.model.Capability;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive test suite for DroneRepository.
 * Tests REST API interactions for fetching drone data.
 */
@ExtendWith(MockitoExtension.class)
class DroneRepositoryTest {

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private DroneRepository droneRepository;

    private static final String BASE_URL = "https://ilp-rest.azurewebsites.net/";
    private Drone[] sampleDrones;

    @BeforeEach
    void setUp() {
        // Create sample drones with different capabilities
        Capability capabilityWithCooling = new Capability(
            true,   // cooling
            false,  // heating
            100.0f,  // capacity
            500,    // maxMoves
            0.5f,    // costPerMove
            10.0f,   // costInitial
            5.0f     // costFinal
        );

        Capability capabilityWithHeating = new Capability(
            false,  // cooling
            true,   // heating
            150.0f,  // capacity
            600,    // maxMoves
            0.6f,    // costPerMove
            12.0f,   // costInitial
            6.0f     // costFinal
        );

        Capability capabilityWithBoth = new Capability(
            true,   // cooling
            true,   // heating
            200.0f,  // capacity
            700,    // maxMoves
            0.7f,    // costPerMove
            15.0f,   // costInitial
            7.0f     // costFinal
        );

        Capability capabilityWithNeither = new Capability(
            false,  // cooling
            false,  // heating
            80.0f,   // capacity
            400,    // maxMoves
            0.4f,    // costPerMove
            8.0f,    // costInitial
            4.0f     // costFinal
        );

        sampleDrones = new Drone[]{
            new Drone("CoolingDrone", "1", capabilityWithCooling),
            new Drone("HeatingDrone", "2", capabilityWithHeating),
            new Drone("BothDrone", "3", capabilityWithBoth),
            new Drone("NeitherDrone", "4", capabilityWithNeither)
        };
    }

    // ==================== fetchAllDronesFromAPI Tests ====================

    @Test
    void testFetchAllDronesFromAPI_Success() {
        try (MockedStatic<IlpConfig> mockedConfig = mockStatic(IlpConfig.class)) {
            // Arrange
            mockedConfig.when(IlpConfig::getCurrentIlpEndpoint).thenReturn(BASE_URL);
            when(restTemplate.getForObject(BASE_URL + "drones", Drone[].class))
                .thenReturn(sampleDrones);

            // Act
            List<Drone> result = droneRepository.fetchAllDronesFromAPI();

            // Assert
            assertNotNull(result);
            assertEquals(4, result.size());
            assertEquals("CoolingDrone", result.get(0).getName());
            assertEquals("HeatingDrone", result.get(1).getName());
            assertEquals("BothDrone", result.get(2).getName());
            assertEquals("NeitherDrone", result.get(3).getName());

            verify(restTemplate).getForObject(BASE_URL + "drones", Drone[].class);
            mockedConfig.verify(IlpConfig::getCurrentIlpEndpoint, times(1));
        }
    }

    @Test
    void testFetchAllDronesFromAPI_NullResponse() {
        try (MockedStatic<IlpConfig> mockedConfig = mockStatic(IlpConfig.class)) {
            // Arrange
            mockedConfig.when(IlpConfig::getCurrentIlpEndpoint).thenReturn(BASE_URL);
            when(restTemplate.getForObject(BASE_URL + "drones", Drone[].class))
                .thenReturn(null);

            // Act
            List<Drone> result = droneRepository.fetchAllDronesFromAPI();

            // Assert
            assertNotNull(result);
            assertTrue(result.isEmpty());
            verify(restTemplate).getForObject(BASE_URL + "drones", Drone[].class);
        }
    }

    @Test
    void testFetchAllDronesFromAPI_EmptyArray() {
        try (MockedStatic<IlpConfig> mockedConfig = mockStatic(IlpConfig.class)) {
            // Arrange
            mockedConfig.when(IlpConfig::getCurrentIlpEndpoint).thenReturn(BASE_URL);
            when(restTemplate.getForObject(BASE_URL + "drones", Drone[].class))
                .thenReturn(new Drone[0]);

            // Act
            List<Drone> result = droneRepository.fetchAllDronesFromAPI();

            // Assert
            assertNotNull(result);
            assertTrue(result.isEmpty());
            verify(restTemplate).getForObject(BASE_URL + "drones", Drone[].class);
        }
    }

    @Test
    void testFetchAllDronesFromAPI_SingleDrone() {
        try (MockedStatic<IlpConfig> mockedConfig = mockStatic(IlpConfig.class)) {
            // Arrange
            Drone[] singleDrone = new Drone[]{sampleDrones[0]};
            mockedConfig.when(IlpConfig::getCurrentIlpEndpoint).thenReturn(BASE_URL);
            when(restTemplate.getForObject(BASE_URL + "drones", Drone[].class))
                .thenReturn(singleDrone);

            // Act
            List<Drone> result = droneRepository.fetchAllDronesFromAPI();

            // Assert
            assertNotNull(result);
            assertEquals(1, result.size());
            assertEquals("CoolingDrone", result.get(0).getName());
        }
    }

    @Test
    void testFetchAllDronesFromAPI_UsesDynamicEndpoint() {
        try (MockedStatic<IlpConfig> mockedConfig = mockStatic(IlpConfig.class)) {
            // Arrange
            String customEndpoint = "https://custom-endpoint.com/";
            mockedConfig.when(IlpConfig::getCurrentIlpEndpoint).thenReturn(customEndpoint);
            when(restTemplate.getForObject(customEndpoint + "drones", Drone[].class))
                .thenReturn(sampleDrones);

            // Act
            List<Drone> result = droneRepository.fetchAllDronesFromAPI();

            // Assert
            assertNotNull(result);
            assertEquals(4, result.size());
            verify(restTemplate).getForObject(customEndpoint + "drones", Drone[].class);
        }
    }

    // ==================== findIdsByCooling Tests ====================

    @Test
    void testFindIdsByCooling_True() {
        try (MockedStatic<IlpConfig> mockedConfig = mockStatic(IlpConfig.class)) {
            // Arrange
            mockedConfig.when(IlpConfig::getCurrentIlpEndpoint).thenReturn(BASE_URL);
            when(restTemplate.getForObject(BASE_URL + "drones", Drone[].class))
                .thenReturn(sampleDrones);

            // Act
            List<Integer> result = droneRepository.findIdsByCooling(true);

            // Assert
            assertNotNull(result);
            assertEquals(2, result.size());
            assertTrue(result.contains(1)); // ID "1" (CoolingDrone)
            assertTrue(result.contains(3)); // ID "3" (BothDrone)
        }
    }

    @Test
    void testFindIdsByCooling_False() {
        try (MockedStatic<IlpConfig> mockedConfig = mockStatic(IlpConfig.class)) {
            // Arrange
            mockedConfig.when(IlpConfig::getCurrentIlpEndpoint).thenReturn(BASE_URL);
            when(restTemplate.getForObject(BASE_URL + "drones", Drone[].class))
                .thenReturn(sampleDrones);

            // Act
            List<Integer> result = droneRepository.findIdsByCooling(false);

            // Assert
            assertNotNull(result);
            assertEquals(2, result.size());
            assertTrue(result.contains(2)); // ID "2" (HeatingDrone)
            assertTrue(result.contains(4)); // ID "4" (NeitherDrone)
        }
    }

    @Test
    void testFindIdsByCooling_Null() {
        try (MockedStatic<IlpConfig> mockedConfig = mockStatic(IlpConfig.class)) {
            // Arrange
            mockedConfig.when(IlpConfig::getCurrentIlpEndpoint).thenReturn(BASE_URL);
            when(restTemplate.getForObject(BASE_URL + "drones", Drone[].class))
                .thenReturn(sampleDrones);

            // Act - null behaves like false, returns drones WITHOUT cooling
            List<Integer> result = droneRepository.findIdsByCooling(null);

            // Assert
            assertNotNull(result);
            assertEquals(2, result.size());
            assertTrue(result.contains(2)); // HeatingDrone (no cooling)
            assertTrue(result.contains(4)); // NeitherDrone (no cooling)
        }
    }

    @Test
    void testFindIdsByCooling_NoDronesMatch() {
        try (MockedStatic<IlpConfig> mockedConfig = mockStatic(IlpConfig.class)) {
            // Arrange - Only drones without cooling
            Capability noCooling = new Capability(false, false, 100.0f, 500, 0.5f, 10.0f, 5.0f);
            Drone[] noCoolingDrones = new Drone[]{
                new Drone("Drone1", "1", noCooling)
            };

            mockedConfig.when(IlpConfig::getCurrentIlpEndpoint).thenReturn(BASE_URL);
            when(restTemplate.getForObject(BASE_URL + "drones", Drone[].class))
                .thenReturn(noCoolingDrones);

            // Act
            List<Integer> result = droneRepository.findIdsByCooling(true);

            // Assert
            assertNotNull(result);
            assertTrue(result.isEmpty());
        }
    }

    @Test
    void testFindIdsByCooling_EmptyDroneList() {
        try (MockedStatic<IlpConfig> mockedConfig = mockStatic(IlpConfig.class)) {
            // Arrange
            mockedConfig.when(IlpConfig::getCurrentIlpEndpoint).thenReturn(BASE_URL);
            when(restTemplate.getForObject(BASE_URL + "drones", Drone[].class))
                .thenReturn(new Drone[0]);

            // Act
            List<Integer> result = droneRepository.findIdsByCooling(true);

            // Assert
            assertNotNull(result);
            assertTrue(result.isEmpty());
        }
    }

    @Test
    void testFindIdsByCooling_NullResponse() {
        try (MockedStatic<IlpConfig> mockedConfig = mockStatic(IlpConfig.class)) {
            // Arrange
            mockedConfig.when(IlpConfig::getCurrentIlpEndpoint).thenReturn(BASE_URL);
            when(restTemplate.getForObject(BASE_URL + "drones", Drone[].class))
                .thenReturn(null);

            // Act
            List<Integer> result = droneRepository.findIdsByCooling(true);

            // Assert
            assertNotNull(result);
            assertTrue(result.isEmpty());
        }
    }

    // ==================== findIdsByHeating Tests ====================

    @Test
    void testFindIdsByHeating_True() {
        try (MockedStatic<IlpConfig> mockedConfig = mockStatic(IlpConfig.class)) {
            // Arrange
            mockedConfig.when(IlpConfig::getCurrentIlpEndpoint).thenReturn(BASE_URL);
            when(restTemplate.getForObject(BASE_URL + "drones", Drone[].class))
                .thenReturn(sampleDrones);

            // Act
            List<Integer> result = droneRepository.findIdsByHeating(true);

            // Assert
            assertNotNull(result);
            assertEquals(2, result.size());
            assertTrue(result.contains(2)); // ID "2" (HeatingDrone)
            assertTrue(result.contains(3)); // ID "3" (BothDrone)
        }
    }

    @Test
    void testFindIdsByHeating_False() {
        try (MockedStatic<IlpConfig> mockedConfig = mockStatic(IlpConfig.class)) {
            // Arrange
            mockedConfig.when(IlpConfig::getCurrentIlpEndpoint).thenReturn(BASE_URL);
            when(restTemplate.getForObject(BASE_URL + "drones", Drone[].class))
                .thenReturn(sampleDrones);

            // Act
            List<Integer> result = droneRepository.findIdsByHeating(false);

            // Assert
            assertNotNull(result);
            assertEquals(2, result.size());
            assertTrue(result.contains(1)); // ID "1" (CoolingDrone)
            assertTrue(result.contains(4)); // ID "4" (NeitherDrone)
        }
    }

    @Test
    void testFindIdsByHeating_Null() {
        try (MockedStatic<IlpConfig> mockedConfig = mockStatic(IlpConfig.class)) {
            // Arrange
            mockedConfig.when(IlpConfig::getCurrentIlpEndpoint).thenReturn(BASE_URL);
            when(restTemplate.getForObject(BASE_URL + "drones", Drone[].class))
                .thenReturn(sampleDrones);

            // Act - null behaves like false, returns drones WITHOUT heating
            List<Integer> result = droneRepository.findIdsByHeating(null);

            // Assert
            assertNotNull(result);
            assertEquals(2, result.size());
            assertTrue(result.contains(1)); // CoolingDrone (no heating)
            assertTrue(result.contains(4)); // NeitherDrone (no heating)
        }
    }

    @Test
    void testFindIdsByHeating_NoDronesMatch() {
        try (MockedStatic<IlpConfig> mockedConfig = mockStatic(IlpConfig.class)) {
            // Arrange - Only drones without heating
            Capability noHeating = new Capability(false, false, 100.0f, 500, 0.5f, 10.0f, 5.0f);
            Drone[] noHeatingDrones = new Drone[]{
                new Drone("Drone1", "1", noHeating)
            };

            mockedConfig.when(IlpConfig::getCurrentIlpEndpoint).thenReturn(BASE_URL);
            when(restTemplate.getForObject(BASE_URL + "drones", Drone[].class))
                .thenReturn(noHeatingDrones);

            // Act
            List<Integer> result = droneRepository.findIdsByHeating(true);

            // Assert
            assertNotNull(result);
            assertTrue(result.isEmpty());
        }
    }

    @Test
    void testFindIdsByHeating_EmptyDroneList() {
        try (MockedStatic<IlpConfig> mockedConfig = mockStatic(IlpConfig.class)) {
            // Arrange
            mockedConfig.when(IlpConfig::getCurrentIlpEndpoint).thenReturn(BASE_URL);
            when(restTemplate.getForObject(BASE_URL + "drones", Drone[].class))
                .thenReturn(new Drone[0]);

            // Act
            List<Integer> result = droneRepository.findIdsByHeating(true);

            // Assert
            assertNotNull(result);
            assertTrue(result.isEmpty());
        }
    }

    @Test
    void testFindIdsByHeating_NullResponse() {
        try (MockedStatic<IlpConfig> mockedConfig = mockStatic(IlpConfig.class)) {
            // Arrange
            mockedConfig.when(IlpConfig::getCurrentIlpEndpoint).thenReturn(BASE_URL);
            when(restTemplate.getForObject(BASE_URL + "drones", Drone[].class))
                .thenReturn(null);

            // Act
            List<Integer> result = droneRepository.findIdsByHeating(true);

            // Assert
            assertNotNull(result);
            assertTrue(result.isEmpty());
        }
    }

    // ==================== Integration Tests ====================

    @Test
    void testFilterConsistency_CoolingAndHeating() {
        try (MockedStatic<IlpConfig> mockedConfig = mockStatic(IlpConfig.class)) {
            // Arrange
            mockedConfig.when(IlpConfig::getCurrentIlpEndpoint).thenReturn(BASE_URL);
            when(restTemplate.getForObject(BASE_URL + "drones", Drone[].class))
                .thenReturn(sampleDrones);

            // Act
            List<Integer> coolingIds = droneRepository.findIdsByCooling(true);
            List<Integer> heatingIds = droneRepository.findIdsByHeating(true);

            // Assert - Drone 3 (BothDrone) should appear in both lists
            assertTrue(coolingIds.contains(3));
            assertTrue(heatingIds.contains(3));

            // Drone 1 only has cooling
            assertTrue(coolingIds.contains(1));
            assertFalse(heatingIds.contains(1));

            // Drone 2 only has heating
            assertFalse(coolingIds.contains(2));
            assertTrue(heatingIds.contains(2));

            // Drone 4 has neither
            assertFalse(coolingIds.contains(4));
            assertFalse(heatingIds.contains(4));
        }
    }

    @Test
    void testMultipleCallsUseDynamicEndpoint() {
        try (MockedStatic<IlpConfig> mockedConfig = mockStatic(IlpConfig.class)) {
            // Arrange
            mockedConfig.when(IlpConfig::getCurrentIlpEndpoint).thenReturn(BASE_URL);
            when(restTemplate.getForObject(BASE_URL + "drones", Drone[].class))
                .thenReturn(sampleDrones);

            // Act - Multiple calls
            droneRepository.fetchAllDronesFromAPI();
            droneRepository.findIdsByCooling(true);
            droneRepository.findIdsByHeating(true);

            // Assert - IlpConfig should be called for each method invocation
            mockedConfig.verify(IlpConfig::getCurrentIlpEndpoint, atLeast(3));
        }
    }
}
