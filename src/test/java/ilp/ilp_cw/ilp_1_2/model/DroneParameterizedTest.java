package ilp.ilp_cw.ilp_1_2.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Parameterized tests for Drone class.
 * Demonstrates efficient testing with multiple input values.
 */
class DroneParameterizedTest {

    private static final Capability DEFAULT_CAPABILITY = 
        new Capability(true, false, 10.0f, 100, 0.5f, 1.0f, 1.0f);

    @ParameterizedTest
    @DisplayName("Parse valid drone IDs")
    @ValueSource(strings = {"1", "42", "999", "12345"})
    void testGetId_ValidNumericStrings(String idString) {
        Drone drone = new Drone("Test", idString, DEFAULT_CAPABILITY);
        Integer expectedId = Integer.parseInt(idString);
        assertEquals(expectedId, drone.getId());
    }

    @ParameterizedTest
    @DisplayName("Handle invalid drone IDs")
    @ValueSource(strings = {"abc", "12.5", "12a", "a12", "-", "12-34"})
    void testGetId_InvalidNumericStrings(String invalidId) {
        Drone drone = new Drone("Test", invalidId, DEFAULT_CAPABILITY);
        assertThrows(NumberFormatException.class, drone::getId);
    }

    @ParameterizedTest
    @DisplayName("Handle null and empty drone IDs")
    @NullAndEmptySource
    void testGetId_NullAndEmptyStrings(String invalidId) {
        Drone drone = new Drone("Test", invalidId, DEFAULT_CAPABILITY);
        assertNull(drone.getId());
    }

    @ParameterizedTest
    @DisplayName("Test cooling capability variations")
    @CsvSource({
        "true,  false, true",   // Has cooling, no heating -> cooling enabled
        "false, false, false",  // No cooling, no heating -> cooling disabled
        "true,  true,  true",   // Both -> cooling enabled
        "false, true,  false"   // No cooling, has heating -> cooling disabled
    })
    void testGetCooling_VariousCapabilities(boolean cooling, boolean heating, boolean expectedCooling) {
        Capability cap = new Capability(cooling, heating, 10.0f, 100, 0.5f, 1.0f, 1.0f);
        Drone drone = new Drone("Test", "1", cap);
        assertEquals(expectedCooling, drone.getCooling());
    }

    @ParameterizedTest
    @DisplayName("Test heating capability variations")
    @CsvSource({
        "false, true,  true",   // No cooling, has heating -> heating enabled
        "false, false, false",  // No cooling, no heating -> heating disabled
        "true,  true,  true",   // Both -> heating enabled
        "true,  false, false"   // Has cooling, no heating -> heating disabled
    })
    void testGetHeating_VariousCapabilities(boolean cooling, boolean heating, boolean expectedHeating) {
        Capability cap = new Capability(cooling, heating, 10.0f, 100, 0.5f, 1.0f, 1.0f);
        Drone drone = new Drone("Test", "1", cap);
        assertEquals(expectedHeating, drone.getHeating());
    }

    @ParameterizedTest
    @DisplayName("Test max capacity from various capabilities")
    @MethodSource("capacityProvider")
    void testGetMaxCapacity_VariousValues(float capacity, float expectedCapacity) {
        Capability cap = new Capability(false, false, capacity, 100, 0.5f, 1.0f, 1.0f);
        Drone drone = new Drone("Test", "1", cap);
        assertEquals(expectedCapacity, drone.getCapability().capacity());
    }

    static Stream<Object[]> capacityProvider() {
        return Stream.of(
            new Object[]{0.0f, 0.0f},
            new Object[]{1.5f, 1.5f},
            new Object[]{10.0f, 10.0f},
            new Object[]{100.5f, 100.5f},
            new Object[]{Float.MAX_VALUE, Float.MAX_VALUE}
        );
    }

    @ParameterizedTest
    @DisplayName("Test max moves from various capabilities")
    @CsvSource({
        "100,   100",
        "1000,  1000",
        "5000,  5000",
        "10000, 10000",
        "0,     0"
    })
    void testGetMaxMoves_VariousValues(int maxMoves, int expectedMaxMoves) {
        Capability cap = new Capability(false, false, 10.0f, maxMoves, 0.5f, 1.0f, 1.0f);
        Drone drone = new Drone("Test", "1", cap);
        assertEquals(expectedMaxMoves, drone.getCapability().maxMoves());
    }

    @ParameterizedTest
    @DisplayName("Test cost per move rates")
    @CsvSource({
        "0.01, 0.01",
        "0.05, 0.05",
        "0.10, 0.10",
        "0.50, 0.50",
        "1.00, 1.00"
    })
    void testGetCostPerMove_VariousValues(float costPerMove, float expected) {
        Capability cap = new Capability(false, false, 10.0f, 100, costPerMove, 1.0f, 1.0f);
        Drone drone = new Drone("Test", "1", cap);
        assertEquals(expected, drone.getCapability().costPerMove());
    }

    @ParameterizedTest
    @DisplayName("Test drone names including special cases")
    @ValueSource(strings = {"Drone1", "Test Drone", "DJI-Phantom", "A", "Very Long Drone Name 12345"})
    void testGetName_VariousNames(String name) {
        Drone drone = new Drone(name, "1", DEFAULT_CAPABILITY);
        assertEquals(name, drone.getName());
    }

    @ParameterizedTest
    @DisplayName("Test null capability handling across methods")
    @MethodSource("nullCapabilityMethodProvider")
    void testNullCapability_VariousMethods(DroneMethodTest methodTest) {
        Drone drone = new Drone("Test", "1", null);
        methodTest.test(drone);
    }

    static Stream<DroneMethodTest> nullCapabilityMethodProvider() {
        return Stream.of(
            drone -> assertFalse(drone.getCooling(), "Cooling should be false for null capability"),
            drone -> assertFalse(drone.getHeating(), "Heating should be false for null capability"),
            drone -> assertNull(drone.getCapability(), "Capability should be null")
        );
    }

    @FunctionalInterface
    interface DroneMethodTest {
        void test(Drone drone);
    }
}
