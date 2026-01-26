package ilp.tutorials.ilp_cw1.model;

import ilp.ilp_cw.ilp_1_2.model.Position;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link Position} validation logic.
 * <p>
 * Covers valid/invalid ranges and null checks for latitude and longitude.
 */
class PositionTest {

    // Helper to create Position
    private Position pos(double lng, double lat) {
        return new Position(BigDecimal.valueOf(lng), BigDecimal.valueOf(lat));
    }

    // --- Valid positions ---
    @Test
    void isValidPosition_validLatLng_returnsTrue() {
        Position p = pos(0, 0);
        assertTrue(p.isValidPosition());

        p = pos(-180, -90);
        assertTrue(p.isValidPosition());

        p = pos(180, 90);
        assertTrue(p.isValidPosition());

        p = pos(45.123, -12.456);
        assertTrue(p.isValidPosition());
    }

    // --- Invalid positions ---
    @Test
    void isValidPosition_latTooLow_returnsFalse() {
        Position p = pos(0, -90.00001);
        assertFalse(p.isValidPosition());
    }

    @Test
    void isValidPosition_latTooHigh_returnsFalse() {
        Position p = pos(0, 90.00001);
        assertFalse(p.isValidPosition());
    }

    @Test
    void isValidPosition_lngTooLow_returnsFalse() {
        Position p = pos(-180.00001, 0);
        assertFalse(p.isValidPosition());
    }

    @Test
    void isValidPosition_lngTooHigh_returnsFalse() {
        Position p = pos(180.00001, 0);
        assertFalse(p.isValidPosition());
    }

    // --- Null checks ---
    @Test
    void isValidPosition_nullLatitude_returnsFalse() {
        Position p = new Position(BigDecimal.ZERO, null);
        assertFalse(p.isValidPosition());
    }

    @Test
    void isValidPosition_nullLongitude_returnsFalse() {
        Position p = new Position(null, BigDecimal.ZERO);
        assertFalse(p.isValidPosition());
    }

    @Test
    void isValidPosition_nullLatitudeAndLongitude_returnsFalse() {
        Position p = new Position(null, null);
        assertFalse(p.isValidPosition());
    }
}
