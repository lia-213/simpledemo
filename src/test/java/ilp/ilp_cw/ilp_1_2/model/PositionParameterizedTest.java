package ilp.ilp_cw.ilp_1_2.model;

import ilp.ilp_cw.ilp_1_2.util.DistanceUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.math.BigDecimal;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Parameterized tests for Position class.
 * Tests distance calculations, boundary conditions, and edge cases.
 */
class PositionParameterizedTest {

    @ParameterizedTest
    @DisplayName("Calculate distance between various position pairs")
    @CsvSource({
        "0.0,  0.0,  0.0,  0.0,  0.0",           // Same position
        "0.0,  0.0,  1.0,  0.0,  1.0",           // 1 degree longitude
        "0.0,  0.0,  0.0,  1.0,  1.0",           // 1 degree latitude
        "3.0,  4.0,  0.0,  0.0,  5.0",           // 3-4-5 triangle
        "-3.0, -4.0, 0.0,  0.0,  5.0",           // Negative coordinates
        "0.0,  0.0,  3.0,  4.0,  5.0"            // Reversed 3-4-5 triangle
    })
    void testDistanceTo_VariousPositions(double lng1, double lat1, double lng2, double lat2, double expectedDistance) {
        Position pos1 = new Position(BigDecimal.valueOf(lng1), BigDecimal.valueOf(lat1));
        Position pos2 = new Position(BigDecimal.valueOf(lng2), BigDecimal.valueOf(lat2));
        
        BigDecimal distance = DistanceUtils.euclideanDistance(pos1, pos2);
        assertEquals(expectedDistance, distance.doubleValue(), 0.0001, 
            "Distance from (" + lng1 + "," + lat1 + ") to (" + lng2 + "," + lat2 + ") should be " + expectedDistance);
    }

    @ParameterizedTest
    @DisplayName("Test position equality with various coordinates")
    @MethodSource("positionEqualityProvider")
    void testEquals_VariousPositions(Position pos1, Position pos2, boolean shouldBeEqual) {
        assertEquals(shouldBeEqual, pos1.equals(pos2));
        if (shouldBeEqual) {
            assertEquals(pos1.hashCode(), pos2.hashCode(), "Equal positions should have same hashCode");
        }
    }

    static Stream<Object[]> positionEqualityProvider() {
        Position p1 = new Position(BigDecimal.valueOf(1.5), BigDecimal.valueOf(2.5));
        Position p2 = new Position(BigDecimal.valueOf(1.5), BigDecimal.valueOf(2.5));
        Position p3 = new Position(BigDecimal.valueOf(1.5), BigDecimal.valueOf(3.5));
        Position p4 = new Position(BigDecimal.valueOf(2.5), BigDecimal.valueOf(2.5));
        Position p5 = new Position(BigDecimal.ZERO, BigDecimal.ZERO);
        Position p6 = new Position(BigDecimal.ZERO, BigDecimal.ZERO);
        
        return Stream.of(
            new Object[]{p1, p2, true},   // Same coordinates
            new Object[]{p1, p3, false},  // Different latitude
            new Object[]{p1, p4, false},  // Different longitude
            new Object[]{p5, p6, true},   // Both at origin
            new Object[]{p1, p1, true}    // Same instance
        );
    }

    @ParameterizedTest
    @DisplayName("Test closeness within epsilon threshold")
    @CsvSource({
        "0.0,    0.0,    0.00001,  0.00001,  true",   // Within epsilon
        "0.0,    0.0,    0.00015,  0.00001,  false",  // Outside epsilon (exactly on boundary)
        "55.944, -3.188, 55.9441,  -3.1881,  true",   // Edinburgh coordinates close
        "55.944, -3.188, 55.945,   -3.189,   false",  // Edinburgh coordinates not close
        "0.0,    0.0,    0.0,      0.0,      true"    // Exact same position
    })
    void testIsCloseTo_VariousDistances(double lng1, double lat1, double lng2, double lat2, boolean expectedClose) {
        Position pos1 = new Position(BigDecimal.valueOf(lng1), BigDecimal.valueOf(lat1));
        Position pos2 = new Position(BigDecimal.valueOf(lng2), BigDecimal.valueOf(lat2));
        
        assertEquals(expectedClose, pos1.isCloseTo(pos2));
    }

    @ParameterizedTest
    @DisplayName("Test coordinate getters with various precision values")
    @MethodSource("coordinateProvider")
    void testCoordinateGetters(double lng, double lat) {
        Position pos = new Position(BigDecimal.valueOf(lng), BigDecimal.valueOf(lat));
        
        assertEquals(lng, pos.getLongitude().doubleValue(), 0.0000001);
        assertEquals(lat, pos.getLatitude().doubleValue(), 0.0000001);
    }

    static Stream<Object[]> coordinateProvider() {
        return Stream.of(
            new Object[]{0.0, 0.0},
            new Object[]{-180.0, -90.0},
            new Object[]{180.0, 90.0},
            new Object[]{55.944425, -3.188267},     // Edinburgh
            new Object[]{-0.127758, 51.507351},     // London
            new Object[]{-74.005941, 40.712784},    // New York
            new Object[]{0.000001, 0.000001}        // Very small values
        );
    }

    @ParameterizedTest
    @DisplayName("Test position construction with extreme values")
    @CsvSource({
        "180,    90",     // Max valid
        "-180,   -90",    // Min valid
        "0,      0",      // Origin
        "179.9,  89.9",   // Near max
        "-179.9, -89.9"   // Near min
    })
    void testConstructor_ExtremeValues(double lng, double lat) {
        Position pos = new Position(BigDecimal.valueOf(lng), BigDecimal.valueOf(lat));
        
        assertNotNull(pos);
        assertEquals(lng, pos.getLongitude().doubleValue(), 0.0001);
        assertEquals(lat, pos.getLatitude().doubleValue(), 0.0001);
    }

    @ParameterizedTest
    @DisplayName("Test isValidPosition with various coordinates")
    @CsvSource({
        "0.0,    0.0,    true",     // Origin
        "180.0,  90.0,   true",     // Max valid
        "-180.0, -90.0,  true",     // Min valid
        "181.0,  0.0,    false",    // Longitude too high
        "-181.0, 0.0,    false",    // Longitude too low
        "0.0,    91.0,   false",    // Latitude too high
        "0.0,    -91.0,  false"     // Latitude too low
    })
    void testIsValidPosition_VariousCoordinates(double lng, double lat, boolean expectedValid) {
        Position pos = new Position(BigDecimal.valueOf(lng), BigDecimal.valueOf(lat));
        assertEquals(expectedValid, pos.isValidPosition());
    }

    @ParameterizedTest
    @DisplayName("Test null coordinate handling")
    @MethodSource("nullCoordinateProvider")
    void testNullCoordinates_Invalid(Position pos) {
        assertFalse(pos.isValidPosition(), "Position with null coordinates should be invalid");
    }

    static Stream<Position> nullCoordinateProvider() {
        return Stream.of(
            new Position(null, BigDecimal.ZERO),
            new Position(BigDecimal.ZERO, null),
            new Position(null, null)
        );
    }
}
