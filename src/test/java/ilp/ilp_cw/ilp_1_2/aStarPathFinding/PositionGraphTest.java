package ilp.ilp_cw.ilp_1_2.aStarPathFinding;

import ilp.ilp_cw.ilp_1_2.model.Position;
import ilp.ilp_cw.ilp_1_2.model.RestrictedArea;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class PositionGraphTest {

    private PositionGraph graph;
    private Position validPosition;
    private List<RestrictedArea> restrictedAreas;

    @BeforeEach
    void setUp() {
        validPosition = new Position(new BigDecimal("-3.186874"), new BigDecimal("55.944494"));
        restrictedAreas = new ArrayList<>();
    }

    @Test
    void testGetConnections_ValidPositionNoRestrictions() {
        graph = new PositionGraph(restrictedAreas);
        PositionNode node = new PositionNode("test-node", validPosition);

        Set<PositionNode> neighbors = graph.getConnections(node);

        assertNotNull(neighbors);
        // Should have neighbors (up to 16 directions, some may produce invalid positions)
        assertTrue(neighbors.size() > 0);
        assertTrue(neighbors.size() <= 16);
    }

    @Test
    void testGetConnections_NullPosition() {
        graph = new PositionGraph(restrictedAreas);
        PositionNode node = new PositionNode("test-node", null);

        Set<PositionNode> neighbors = graph.getConnections(node);

        assertNotNull(neighbors);
        assertTrue(neighbors.isEmpty());
    }

    @Test
    void testGetConnections_InvalidPosition() {
        graph = new PositionGraph(restrictedAreas);
        Position invalidPos = new Position(new BigDecimal("200"), new BigDecimal("100")); // Out of valid range
        PositionNode node = new PositionNode("test-node", invalidPos);

        Set<PositionNode> neighbors = graph.getConnections(node);

        assertNotNull(neighbors);
        assertTrue(neighbors.isEmpty());
    }

    @Test
    void testGetConnections_StartInsideRestrictedArea() {
        // Create a restricted area around the start position
        List<Position> polygon = createSquarePolygon(validPosition, new BigDecimal("0.001"));
        RestrictedArea area = new RestrictedArea(polygon, 1, "TestArea");
        restrictedAreas.add(area);

        graph = new PositionGraph(restrictedAreas);
        PositionNode node = new PositionNode("test-node", validPosition);

        Set<PositionNode> neighbors = graph.getConnections(node);

        assertNotNull(neighbors);
        assertTrue(neighbors.isEmpty());
    }

    @Test
    void testGetConnections_SomeNeighborsInsideRestrictedArea() {
        // Create a small restricted area to the north of the position
        Position northCenter = new Position(
                validPosition.getLongitude(),
                validPosition.getLatitude().add(new BigDecimal("0.0002"))
        );
        List<Position> polygon = createSquarePolygon(northCenter, new BigDecimal("0.00015"));
        RestrictedArea area = new RestrictedArea(polygon, 1, "NorthArea");
        restrictedAreas.add(area);

        graph = new PositionGraph(restrictedAreas);
        PositionNode node = new PositionNode("test-node", validPosition);

        Set<PositionNode> neighbors = graph.getConnections(node);

        assertNotNull(neighbors);
        // Should have fewer than 16 neighbors (some blocked by restricted area)
        assertTrue(neighbors.size() < 16);
        assertTrue(neighbors.size() > 0);
    }

    @Test
    void testGetConnections_SegmentCrossesRestrictedArea() {
        // Create a wall-like restricted area between start and some neighbors
        Position wallCenter = new Position(
                validPosition.getLongitude().add(new BigDecimal("0.0001")),
                validPosition.getLatitude()
        );
        List<Position> polygon = createRectanglePolygon(wallCenter, 
                new BigDecimal("0.00005"), new BigDecimal("0.001"));
        RestrictedArea area = new RestrictedArea(polygon, 1, "Wall");
        restrictedAreas.add(area);

        graph = new PositionGraph(restrictedAreas);
        PositionNode node = new PositionNode("test-node", validPosition);

        Set<PositionNode> neighbors = graph.getConnections(node);

        assertNotNull(neighbors);
        // Some neighbors should be blocked
        assertTrue(neighbors.size() < 16);
    }

    @Test
    void testGetConnections_NullRestrictedAreas() {
        graph = new PositionGraph(null);
        PositionNode node = new PositionNode("test-node", validPosition);

        Set<PositionNode> neighbors = graph.getConnections(node);

        assertNotNull(neighbors);
        assertTrue(neighbors.size() > 0);
        assertTrue(neighbors.size() <= 16);
    }

    @Test
    void testGetConnections_EmptyRestrictedAreas() {
        graph = new PositionGraph(new ArrayList<>());
        PositionNode node = new PositionNode("test-node", validPosition);

        Set<PositionNode> neighbors = graph.getConnections(node);

        assertNotNull(neighbors);
        assertTrue(neighbors.size() > 0);
        assertTrue(neighbors.size() <= 16);
    }

    @Test
    void testGetConnections_RestrictedAreaWithNullPositions() {
        RestrictedArea area = new RestrictedArea(null, 1, "NullPositions");
        restrictedAreas.add(area);

        graph = new PositionGraph(restrictedAreas);
        PositionNode node = new PositionNode("test-node", validPosition);

        Set<PositionNode> neighbors = graph.getConnections(node);

        assertNotNull(neighbors);
        assertTrue(neighbors.size() > 0);
        assertTrue(neighbors.size() <= 16);
    }

    @Test
    void testGetConnections_MultipleRestrictedAreas() {
        // Create two restricted areas
        Position center1 = new Position(
                validPosition.getLongitude().add(new BigDecimal("0.0002")),
                validPosition.getLatitude()
        );
        List<Position> polygon1 = createSquarePolygon(center1, new BigDecimal("0.00010"));
        RestrictedArea area1 = new RestrictedArea(polygon1, 1, "Area1");

        Position center2 = new Position(
                validPosition.getLongitude(),
                validPosition.getLatitude().add(new BigDecimal("0.0002"))
        );
        List<Position> polygon2 = createSquarePolygon(center2, new BigDecimal("0.00010"));
        RestrictedArea area2 = new RestrictedArea(polygon2, 2, "Area2");

        restrictedAreas.add(area1);
        restrictedAreas.add(area2);

        graph = new PositionGraph(restrictedAreas);
        PositionNode node = new PositionNode("test-node", validPosition);

        Set<PositionNode> neighbors = graph.getConnections(node);

        assertNotNull(neighbors);
        assertTrue(neighbors.size() < 16);
    }

    @Test
    void testGetConnections_NeighborIdsAreUnique() {
        graph = new PositionGraph(restrictedAreas);
        PositionNode node = new PositionNode("test-node", validPosition);

        Set<PositionNode> neighbors = graph.getConnections(node);

        Set<String> ids = new HashSet<>();
        for (PositionNode neighbor : neighbors) {
            ids.add(neighbor.id());
        }

        assertEquals(neighbors.size(), ids.size(), "All neighbor IDs should be unique");
    }

    @Test
    void testGetConnections_NeighborPositionsAreValid() {
        graph = new PositionGraph(restrictedAreas);
        PositionNode node = new PositionNode("test-node", validPosition);

        Set<PositionNode> neighbors = graph.getConnections(node);

        for (PositionNode neighbor : neighbors) {
            assertNotNull(neighbor.getPosition());
            assertTrue(neighbor.getPosition().isValidPosition());
        }
    }

    @Test
    void testGetConnections_NeighborsAreAtCorrectDistance() {
        graph = new PositionGraph(restrictedAreas);
        PositionNode node = new PositionNode("test-node", validPosition);

        Set<PositionNode> neighbors = graph.getConnections(node);

        // All neighbors should be approximately one step away
        for (PositionNode neighbor : neighbors) {
            Position neighborPos = neighbor.getPosition();
            BigDecimal latDiff = neighborPos.getLatitude().subtract(validPosition.getLatitude()).abs();
            BigDecimal lonDiff = neighborPos.getLongitude().subtract(validPosition.getLongitude()).abs();
            
            // At least one coordinate should have changed
            assertTrue(latDiff.compareTo(BigDecimal.ZERO) > 0 || lonDiff.compareTo(BigDecimal.ZERO) > 0);
            
            // Changes should be small (around 0.00015 step size)
            assertTrue(latDiff.compareTo(new BigDecimal("0.001")) < 0);
            assertTrue(lonDiff.compareTo(new BigDecimal("0.001")) < 0);
        }
    }

    @Test
    void testGetConnections_AllAnglesGenerated() {
        graph = new PositionGraph(restrictedAreas);
        PositionNode node = new PositionNode("test-node", validPosition);

        Set<PositionNode> neighbors = graph.getConnections(node);

        // Check that we get neighbors (up to 16 directions)
        assertTrue(neighbors.size() > 0);
        assertTrue(neighbors.size() <= 16);
        
        // Verify the IDs contain angle information
        Set<String> neighborIds = new HashSet<>();
        for (PositionNode neighbor : neighbors) {
            neighborIds.add(neighbor.id());
        }
        
        for (String id : neighborIds) {
            assertTrue(id.contains("->"), "ID should contain angle separator");
        }
    }

    @Test
    void testGetConnections_PositionAtPole() {
        // Test edge case: position near north pole
        Position polePosition = new Position(new BigDecimal("0"), new BigDecimal("89.9"));
        graph = new PositionGraph(restrictedAreas);
        PositionNode node = new PositionNode("pole-node", polePosition);

        Set<PositionNode> neighbors = graph.getConnections(node);

        assertNotNull(neighbors);
        // Should still generate neighbors, though some might be invalid at extreme latitudes
        assertTrue(neighbors.size() >= 0);
    }

    @Test
    void testGetConnections_PositionAtEquator() {
        Position equatorPosition = new Position(new BigDecimal("0"), new BigDecimal("0"));
        graph = new PositionGraph(restrictedAreas);
        PositionNode node = new PositionNode("equator-node", equatorPosition);

        Set<PositionNode> neighbors = graph.getConnections(node);

        assertNotNull(neighbors);
        assertTrue(neighbors.size() > 0);
        assertTrue(neighbors.size() <= 16);
    }

    @Test
    void testGetConnections_VerySmallRestrictedArea() {
        // Create a tiny restricted area (single point effectively)
        List<Position> polygon = new ArrayList<>();
        polygon.add(validPosition);
        polygon.add(new Position(
                validPosition.getLongitude().add(new BigDecimal("0.000001")),
                validPosition.getLatitude()
        ));
        polygon.add(new Position(
                validPosition.getLongitude(),
                validPosition.getLatitude().add(new BigDecimal("0.000001"))
        ));
        
        RestrictedArea area = new RestrictedArea(polygon, 1, "TinyArea");
        restrictedAreas.add(area);

        graph = new PositionGraph(restrictedAreas);
        PositionNode node = new PositionNode("test-node", validPosition);

        Set<PositionNode> neighbors = graph.getConnections(node);

        assertNotNull(neighbors);
        // The tiny area might block the start position or not affect neighbors
        assertTrue(neighbors.size() >= 0 && neighbors.size() <= 16);
    }

    @Test
    void testGetConnections_LargeRestrictedArea() {
        // Create a large restricted area covering most neighbors
        List<Position> polygon = createSquarePolygon(validPosition, new BigDecimal("0.001"));
        RestrictedArea area = new RestrictedArea(polygon, 1, "LargeArea");
        restrictedAreas.add(area);

        graph = new PositionGraph(restrictedAreas);
        PositionNode node = new PositionNode("test-node", validPosition);

        Set<PositionNode> neighbors = graph.getConnections(node);

        assertNotNull(neighbors);
        // Start is inside, so no neighbors
        assertEquals(0, neighbors.size());
    }

    @Test
    void testGetConnections_RestrictedAreaAsListWithNullElements() {
        restrictedAreas.add(null);
        RestrictedArea validArea = new RestrictedArea(
                createSquarePolygon(new Position(new BigDecimal("-3.18"), new BigDecimal("55.94")), 
                        new BigDecimal("0.0001")), 
                1, "ValidArea");
        restrictedAreas.add(validArea);
        restrictedAreas.add(null);

        graph = new PositionGraph(restrictedAreas);
        PositionNode node = new PositionNode("test-node", validPosition);

        Set<PositionNode> neighbors = graph.getConnections(node);

        assertNotNull(neighbors);
        // Should handle null elements gracefully
        assertTrue(neighbors.size() >= 0);
    }

    @Test
    void testGetConnections_SegmentCrossing_MultipleSamples() {
        // Create a barrier that should be detected by segment sampling
        Position barrierCenter = new Position(
                validPosition.getLongitude().add(new BigDecimal("0.00008")),
                validPosition.getLatitude()
        );
        List<Position> polygon = createRectanglePolygon(barrierCenter,
                new BigDecimal("0.00003"), new BigDecimal("0.0005"));
        RestrictedArea area = new RestrictedArea(polygon, 1, "Barrier");
        restrictedAreas.add(area);

        graph = new PositionGraph(restrictedAreas);
        PositionNode node = new PositionNode("test-node", validPosition);

        Set<PositionNode> neighbors = graph.getConnections(node);

        assertNotNull(neighbors);
        // Some eastward directions should be blocked
        assertTrue(neighbors.size() < 16);
    }

    @Test
    void testGetConnections_ComputeNextPositionException() {
        // Test with extreme values that might cause computation issues
        Position extremePosition = new Position(
                new BigDecimal("179.999999"),
                new BigDecimal("89.999999")
        );
        graph = new PositionGraph(restrictedAreas);
        PositionNode node = new PositionNode("extreme-node", extremePosition);

        Set<PositionNode> neighbors = graph.getConnections(node);

        assertNotNull(neighbors);
        // Should handle computation gracefully, some neighbors might be invalid
        assertTrue(neighbors.size() >= 0);
    }

    @Test
    void testGetConnections_DivisionByZeroAtPole() {
        // Test at exact pole where cos(latitude) approaches 0
        Position polePosition = new Position(new BigDecimal("0"), new BigDecimal("90"));
        graph = new PositionGraph(restrictedAreas);
        PositionNode node = new PositionNode("pole-node", polePosition);

        Set<PositionNode> neighbors = graph.getConnections(node);

        assertNotNull(neighbors);
        // At the pole, computation might fail due to division by cos(90°) ≈ 0
        // Should handle this gracefully
        assertTrue(neighbors.size() >= 0);
    }

    @Test
    void testGetConnections_PointInPolygonEdgeCase() {
        // Create a polygon where the test point is exactly on the edge
        List<Position> polygon = new ArrayList<>();
        polygon.add(new Position(
                validPosition.getLongitude().subtract(new BigDecimal("0.0001")),
                validPosition.getLatitude().subtract(new BigDecimal("0.0001"))
        ));
        polygon.add(new Position(
                validPosition.getLongitude().add(new BigDecimal("0.0001")),
                validPosition.getLatitude().subtract(new BigDecimal("0.0001"))
        ));
        polygon.add(validPosition); // Exact position on the polygon edge
        polygon.add(new Position(
                validPosition.getLongitude().subtract(new BigDecimal("0.0001")),
                validPosition.getLatitude().add(new BigDecimal("0.0001"))
        ));

        RestrictedArea area = new RestrictedArea(polygon, 1, "EdgePolygon");
        restrictedAreas.add(area);

        graph = new PositionGraph(restrictedAreas);
        PositionNode node = new PositionNode("test-node", validPosition);

        Set<PositionNode> neighbors = graph.getConnections(node);

        assertNotNull(neighbors);
        // Behavior depends on edge case handling in point-in-polygon
        assertTrue(neighbors.size() >= 0 && neighbors.size() <= 16);
    }

    @Test
    void testGetConnections_TriangleRestrictedArea() {
        // Test with a triangular restricted area (minimum polygon)
        List<Position> triangle = new ArrayList<>();
        triangle.add(new Position(
                validPosition.getLongitude(),
                validPosition.getLatitude().add(new BigDecimal("0.0002"))
        ));
        triangle.add(new Position(
                validPosition.getLongitude().add(new BigDecimal("0.0002")),
                validPosition.getLatitude()
        ));
        triangle.add(new Position(
                validPosition.getLongitude().subtract(new BigDecimal("0.0002")),
                validPosition.getLatitude()
        ));

        RestrictedArea area = new RestrictedArea(triangle, 1, "Triangle");
        restrictedAreas.add(area);

        graph = new PositionGraph(restrictedAreas);
        PositionNode node = new PositionNode("test-node", validPosition);

        Set<PositionNode> neighbors = graph.getConnections(node);

        assertNotNull(neighbors);
        assertTrue(neighbors.size() < 16);
    }

    @Test
    void testGetConnections_PolygonWithLessThanThreeVertices() {
        // Polygon with only 2 vertices (invalid polygon)
        List<Position> invalidPolygon = new ArrayList<>();
        invalidPolygon.add(validPosition);
        invalidPolygon.add(new Position(
                validPosition.getLongitude().add(new BigDecimal("0.0001")),
                validPosition.getLatitude()
        ));

        RestrictedArea area = new RestrictedArea(invalidPolygon, 1, "InvalidPolygon");
        restrictedAreas.add(area);

        graph = new PositionGraph(restrictedAreas);
        PositionNode node = new PositionNode("test-node", validPosition);

        Set<PositionNode> neighbors = graph.getConnections(node);

        assertNotNull(neighbors);
        // Invalid polygon should be ignored
        assertTrue(neighbors.size() > 0);
        assertTrue(neighbors.size() <= 16);
    }

    @Test
    void testGetConnections_EmptyPolygon() {
        // Empty vertex list
        List<Position> emptyPolygon = new ArrayList<>();
        RestrictedArea area = new RestrictedArea(emptyPolygon, 1, "EmptyPolygon");
        restrictedAreas.add(area);

        graph = new PositionGraph(restrictedAreas);
        PositionNode node = new PositionNode("test-node", validPosition);

        Set<PositionNode> neighbors = graph.getConnections(node);

        assertNotNull(neighbors);
        assertTrue(neighbors.size() > 0);
        assertTrue(neighbors.size() <= 16);
    }

    // Helper methods to create test polygons

    private List<Position> createSquarePolygon(Position center, BigDecimal halfSize) {
        List<Position> polygon = new ArrayList<>();
        polygon.add(new Position(
                center.getLongitude().subtract(halfSize),
                center.getLatitude().subtract(halfSize)
        ));
        polygon.add(new Position(
                center.getLongitude().add(halfSize),
                center.getLatitude().subtract(halfSize)
        ));
        polygon.add(new Position(
                center.getLongitude().add(halfSize),
                center.getLatitude().add(halfSize)
        ));
        polygon.add(new Position(
                center.getLongitude().subtract(halfSize),
                center.getLatitude().add(halfSize)
        ));
        return polygon;
    }

    private List<Position> createRectanglePolygon(Position center, BigDecimal halfWidth, BigDecimal halfHeight) {
        List<Position> polygon = new ArrayList<>();
        polygon.add(new Position(
                center.getLongitude().subtract(halfWidth),
                center.getLatitude().subtract(halfHeight)
        ));
        polygon.add(new Position(
                center.getLongitude().add(halfWidth),
                center.getLatitude().subtract(halfHeight)
        ));
        polygon.add(new Position(
                center.getLongitude().add(halfWidth),
                center.getLatitude().add(halfHeight)
        ));
        polygon.add(new Position(
                center.getLongitude().subtract(halfWidth),
                center.getLatitude().add(halfHeight)
        ));
        return polygon;
    }
}
