package ilp.tutorials.ilp_cw1.model;

import org.junit.jupiter.api.Test;
import ilp.ilp_cw.ilp_1_2.model.Position;
import ilp.ilp_cw.ilp_1_2.model.Region;
import java.awt.geom.Path2D;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the {@link Region} class.
 * <p>
 * Verifies region validation, polygon construction and point containment for
 * simple and complex polygons, including edge and vertex cases.
 */
class RegionTest {

    // ------------------------------------------------------------
    // Helper methods
    // ------------------------------------------------------------

    private Position pos(double lng, double lat) {
        return new Position(BigDecimal.valueOf(lng), BigDecimal.valueOf(lat));
    }

    private Region createSquareRegion() {
        return new Region("square", Arrays.asList(
                pos(0, 0),
                pos(0, 1),
                pos(1, 1),
                pos(1, 0)
        ));
    }

    private Region createComplexRegion() {
        return new Region("complex region", Arrays.asList(
                pos(-70, -20),
                pos(-40, 60),
                pos(0, 90),
                pos(50, 50),
                pos(80, -10),
                pos(30, -80),
                pos(-30, -60)
        ));
    }

    // ------------------------------------------------------------
    // isNotValidRegion() tests
    // ------------------------------------------------------------

    @Test
    void isNotValidRegion_nullVertices_returnsTrue() {
        Region r = new Region("r", null);
        assertTrue(r.isNotValidRegion());
    }

    @Test
    void isNotValidRegion_fewerThan3Vertices_returnsTrue() {
        Region r = new Region("r", Collections.singletonList(pos(0, 0)));
        assertTrue(r.isNotValidRegion());
    }

    @Test
    void isNotValidRegion_duplicateVertices_returnsTrue() {
        Region r = new Region("r", Arrays.asList(pos(0, 0), pos(0, 0), pos(0, 0)));
        assertTrue(r.isNotValidRegion());
    }

    @Test
    void isNotValidRegion_validSquare_returnsFalse() {
        Region r = createSquareRegion();
        assertFalse(r.isNotValidRegion());
    }

    @Test
    void isNotValidRegion_complexRegion_returnsFalse() {
        Region r = createComplexRegion();
        assertFalse(r.isNotValidRegion());
    }

    // ------------------------------------------------------------
    // makePolygon() tests
    // ------------------------------------------------------------

    @Test
    void makePolygon_withVertices_createsClosedPolygon() {
        Region r = createSquareRegion();
        Path2D.Double polygon = r.makePolygon();
        assertNotNull(polygon);
        // polygon should be closed
        assertFalse(polygon.getPathIterator(null).isDone());
    }

    @Test
    void makePolygon_emptyVertices_returnsEmptyPolygon() {
        Region r = new Region("r", Collections.emptyList());
        Path2D.Double polygon = r.makePolygon();
        assertNotNull(polygon);
        // no points, area should be 0
        assertEquals(0.0, polygon.getBounds2D().getWidth());
        assertEquals(0.0, polygon.getBounds2D().getHeight());
    }

    @Test
    void makePolygon_complexRegion_createsClosedShape() {
        Region r = createComplexRegion();
        Path2D.Double polygon = r.makePolygon();
        assertNotNull(polygon);
        // ensure the path is closed
        assertFalse(polygon.getPathIterator(null).isDone());
    }

    // ------------------------------------------------------------
    // contains(Position) tests — square region
    // ------------------------------------------------------------

    @Test
    void contains_nullPosition_returnsFalse() {
        Region r = createSquareRegion();
        assertFalse(r.contains(null));
    }

    @Test
    void contains_invalidRegion_returnsFalse() {
        Region r = new Region("r", Arrays.asList(pos(0, 0), pos(0, 0))); // invalid region
        assertFalse(r.contains(pos(0.5, 0.5)));
    }

    @Test
    void contains_pointInsideSquare_returnsTrue() {
        Region r = createSquareRegion();
        Position p = pos(0.5, 0.5);
        assertTrue(r.contains(p));
    }

    @Test
    void contains_pointOutsideSquare_returnsFalse() {
        Region r = createSquareRegion();
        Position p = pos(2.0, 2.0);
        assertFalse(r.contains(p));
    }

    @Test
    void contains_pointOnSquareBorder_returnsTrue() {
        Region r = createSquareRegion();
        Position p = pos(0, 0.5); // edge
        assertTrue(r.contains(p));
    }

    @Test
    void contains_justInBorderSquare_returnsTrue() {
        Region r = createSquareRegion();
        Position p = pos(0.99999, 0.99999);
        assertTrue(r.contains(p));
    }

    @Test
    void contains_justOutBorderSquare_returnsFalse() {
        Region r = createSquareRegion();
        Position p = pos(1.00016, 1);
        assertFalse(r.contains(p));
    }

    // ------------------------------------------------------------
    // contains(Position) tests — complex region
    // ------------------------------------------------------------

    @Test
    void contains_pointInsideComplexRegion_returnsTrue() {
        Region r = createComplexRegion();
        // point roughly in the middle of the irregular shape
        Position p = pos(0, 0);
        assertTrue(r.contains(p));
    }

    @Test
    void contains_pointOutsideComplexRegion_returnsFalse() {
        Region r = createComplexRegion();
        // clearly outside the polygon
        Position p = pos(100, 100);
        assertFalse(r.contains(p));
    }

    @Test
    void contains_pointNearComplexEdge_returnsTrue() {
        Region r = createComplexRegion();
        // near one of the edges, within bounds
        Position p = pos(50, 0);
        assertTrue(r.contains(p));
    }

    @Test
    void contains_pointOnComplexVertex_returnsTrue() {
        Region r = createComplexRegion();
        // exactly on a vertex
        Position p = pos(-70, -20);
        assertTrue(r.contains(p));
    }
}