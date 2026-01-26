package ilp.ilp_cw.ilp_1_2.aStarPathFinding;

import ilp.ilp_cw.ilp_1_2.model.Position;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive test suite for PositionScorer.
 * Tests all branches including null checks and distance calculations.
 */
class PositionScorerTest {

    private PositionScorer scorer;
    private Position validPosition1;
    private Position validPosition2;
    private PositionNode validNode1;
    private PositionNode validNode2;

    @BeforeEach
    void setUp() {
        scorer = new PositionScorer();

        // Create valid positions
        validPosition1 = new Position();
        validPosition1.setLatitude(BigDecimal.valueOf(55.9445));
        validPosition1.setLongitude(BigDecimal.valueOf(-3.1878));

        validPosition2 = new Position();
        validPosition2.setLatitude(BigDecimal.valueOf(55.9450));
        validPosition2.setLongitude(BigDecimal.valueOf(-3.1900));

        // Create valid nodes
        validNode1 = new PositionNode("node1", validPosition1);
        validNode2 = new PositionNode("node2", validPosition2);
    }

    // ==================== Successful Computation Tests ====================

    @Test
    void testComputeCost_ValidPositions_ReturnsDistance() {
        // Act
        BigDecimal cost = scorer.computeCost(validNode1, validNode2);

        // Assert
        assertNotNull(cost);
        assertTrue(cost.compareTo(BigDecimal.ZERO) > 0, "Distance should be positive");
    }

    @Test
    void testComputeCost_SamePosition_ReturnsZero() {
        // Arrange
        Position samePosition = new Position();
        samePosition.setLatitude(BigDecimal.valueOf(55.9445));
        samePosition.setLongitude(BigDecimal.valueOf(-3.1878));

        PositionNode node1 = new PositionNode("node1", samePosition);
        PositionNode node2 = new PositionNode("node2", samePosition);

        // Act
        BigDecimal cost = scorer.computeCost(node1, node2);

        // Assert
        assertNotNull(cost);
        assertEquals(0, cost.compareTo(BigDecimal.ZERO), "Distance between same position should be zero");
    }

    @Test
    void testComputeCost_DistanceCalculation_IsSymmetric() {
        // Act
        BigDecimal cost1to2 = scorer.computeCost(validNode1, validNode2);
        BigDecimal cost2to1 = scorer.computeCost(validNode2, validNode1);

        // Assert
        assertEquals(cost1to2, cost2to1, "Distance should be symmetric");
    }

    @Test
    void testComputeCost_KnownDistance_CalculatesCorrectly() {
        // Arrange - Create positions with known Euclidean distance
        Position p1 = new Position();
        p1.setLatitude(BigDecimal.valueOf(0));
        p1.setLongitude(BigDecimal.valueOf(0));

        Position p2 = new Position();
        p2.setLatitude(BigDecimal.valueOf(3));
        p2.setLongitude(BigDecimal.valueOf(4));

        PositionNode n1 = new PositionNode("n1", p1);
        PositionNode n2 = new PositionNode("n2", p2);

        // Act - Distance should be sqrt(3^2 + 4^2) = 5
        BigDecimal cost = scorer.computeCost(n1, n2);

        // Assert
        assertEquals(0, cost.compareTo(BigDecimal.valueOf(5)), 
            "Distance for 3-4-5 triangle should be 5");
    }

    // ==================== Null Position Tests ====================

    @Test
    void testComputeCost_FromPositionIsNull_ThrowsException() {
        // Arrange
        PositionNode nullNode = new PositionNode("null", null);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> scorer.computeCost(nullNode, validNode2)
        );
        assertTrue(exception.getMessage().contains("Positions must not be null"));
    }

    @Test
    void testComputeCost_ToPositionIsNull_ThrowsException() {
        // Arrange
        PositionNode nullNode = new PositionNode("null", null);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> scorer.computeCost(validNode1, nullNode)
        );
        assertTrue(exception.getMessage().contains("Positions must not be null"));
    }

    @Test
    void testComputeCost_BothPositionsNull_ThrowsException() {
        // Arrange
        PositionNode nullNode1 = new PositionNode("null1", null);
        PositionNode nullNode2 = new PositionNode("null2", null);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> scorer.computeCost(nullNode1, nullNode2)
        );
        assertTrue(exception.getMessage().contains("Positions must not be null"));
    }

    // ==================== Null Latitude Tests ====================

    @Test
    void testComputeCost_FromLatitudeIsNull_ThrowsException() {
        // Arrange
        Position positionWithNullLat = new Position();
        positionWithNullLat.setLatitude(null);
        positionWithNullLat.setLongitude(BigDecimal.valueOf(-3.1878));
        PositionNode nodeWithNullLat = new PositionNode("nullLat", positionWithNullLat);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> scorer.computeCost(nodeWithNullLat, validNode2)
        );
        assertTrue(exception.getMessage().contains("must have coordinates"));
    }

    @Test
    void testComputeCost_ToLatitudeIsNull_ThrowsException() {
        // Arrange
        Position positionWithNullLat = new Position();
        positionWithNullLat.setLatitude(null);
        positionWithNullLat.setLongitude(BigDecimal.valueOf(-3.1878));
        PositionNode nodeWithNullLat = new PositionNode("nullLat", positionWithNullLat);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> scorer.computeCost(validNode1, nodeWithNullLat)
        );
        assertTrue(exception.getMessage().contains("must have coordinates"));
    }

    // ==================== Null Longitude Tests ====================

    @Test
    void testComputeCost_FromLongitudeIsNull_ThrowsException() {
        // Arrange
        Position positionWithNullLon = new Position();
        positionWithNullLon.setLatitude(BigDecimal.valueOf(55.9445));
        positionWithNullLon.setLongitude(null);
        PositionNode nodeWithNullLon = new PositionNode("nullLon", positionWithNullLon);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> scorer.computeCost(nodeWithNullLon, validNode2)
        );
        assertTrue(exception.getMessage().contains("must have coordinates"));
    }

    @Test
    void testComputeCost_ToLongitudeIsNull_ThrowsException() {
        // Arrange
        Position positionWithNullLon = new Position();
        positionWithNullLon.setLatitude(BigDecimal.valueOf(55.9445));
        positionWithNullLon.setLongitude(null);
        PositionNode nodeWithNullLon = new PositionNode("nullLon", positionWithNullLon);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> scorer.computeCost(validNode1, nodeWithNullLon)
        );
        assertTrue(exception.getMessage().contains("must have coordinates"));
    }

    // ==================== Combined Null Tests ====================

    @Test
    void testComputeCost_FromBothCoordinatesNull_ThrowsException() {
        // Arrange
        Position positionWithNullCoords = new Position();
        positionWithNullCoords.setLatitude(null);
        positionWithNullCoords.setLongitude(null);
        PositionNode nodeWithNullCoords = new PositionNode("nullCoords", positionWithNullCoords);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> scorer.computeCost(nodeWithNullCoords, validNode2)
        );
        assertTrue(exception.getMessage().contains("must have coordinates"));
    }

    @Test
    void testComputeCost_ToBothCoordinatesNull_ThrowsException() {
        // Arrange
        Position positionWithNullCoords = new Position();
        positionWithNullCoords.setLatitude(null);
        positionWithNullCoords.setLongitude(null);
        PositionNode nodeWithNullCoords = new PositionNode("nullCoords", positionWithNullCoords);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> scorer.computeCost(validNode1, nodeWithNullCoords)
        );
        assertTrue(exception.getMessage().contains("must have coordinates"));
    }

    // ==================== Edge Case Tests ====================

    @Test
    void testComputeCost_VerySmallDistance_ReturnsAccurateResult() {
        // Arrange - Positions very close together
        Position p1 = new Position();
        p1.setLatitude(BigDecimal.valueOf(55.9445));
        p1.setLongitude(BigDecimal.valueOf(-3.1878));

        Position p2 = new Position();
        p2.setLatitude(BigDecimal.valueOf(55.9445001));
        p2.setLongitude(BigDecimal.valueOf(-3.1878001));

        PositionNode n1 = new PositionNode("n1", p1);
        PositionNode n2 = new PositionNode("n2", p2);

        // Act
        BigDecimal cost = scorer.computeCost(n1, n2);

        // Assert
        assertNotNull(cost);
        assertTrue(cost.compareTo(BigDecimal.ZERO) > 0, "Should have non-zero distance");
        assertTrue(cost.compareTo(BigDecimal.valueOf(0.001)) < 0, "Distance should be very small");
    }

    @Test
    void testComputeCost_LargeDistance_ReturnsAccurateResult() {
        // Arrange - Positions far apart
        Position p1 = new Position();
        p1.setLatitude(BigDecimal.valueOf(0));
        p1.setLongitude(BigDecimal.valueOf(0));

        Position p2 = new Position();
        p2.setLatitude(BigDecimal.valueOf(100));
        p2.setLongitude(BigDecimal.valueOf(100));

        PositionNode n1 = new PositionNode("n1", p1);
        PositionNode n2 = new PositionNode("n2", p2);

        // Act
        BigDecimal cost = scorer.computeCost(n1, n2);

        // Assert
        assertNotNull(cost);
        assertTrue(cost.compareTo(BigDecimal.valueOf(100)) > 0, "Distance should be > 100");
    }

    @Test
    void testComputeCost_NegativeCoordinates_WorksCorrectly() {
        // Arrange - Positions with negative coordinates
        Position p1 = new Position();
        p1.setLatitude(BigDecimal.valueOf(-55.9445));
        p1.setLongitude(BigDecimal.valueOf(-3.1878));

        Position p2 = new Position();
        p2.setLatitude(BigDecimal.valueOf(-55.9450));
        p2.setLongitude(BigDecimal.valueOf(-3.1900));

        PositionNode n1 = new PositionNode("n1", p1);
        PositionNode n2 = new PositionNode("n2", p2);

        // Act
        BigDecimal cost = scorer.computeCost(n1, n2);

        // Assert
        assertNotNull(cost);
        assertTrue(cost.compareTo(BigDecimal.ZERO) > 0, "Distance should be positive even with negative coordinates");
    }

    @Test
    void testComputeCost_CrossingOrigin_WorksCorrectly() {
        // Arrange - Positions on opposite sides of origin
        Position p1 = new Position();
        p1.setLatitude(BigDecimal.valueOf(-10));
        p1.setLongitude(BigDecimal.valueOf(-10));

        Position p2 = new Position();
        p2.setLatitude(BigDecimal.valueOf(10));
        p2.setLongitude(BigDecimal.valueOf(10));

        PositionNode n1 = new PositionNode("n1", p1);
        PositionNode n2 = new PositionNode("n2", p2);

        // Act
        BigDecimal cost = scorer.computeCost(n1, n2);

        // Assert
        assertNotNull(cost);
        // Distance should be sqrt(20^2 + 20^2) = sqrt(800) ≈ 28.28
        assertTrue(cost.compareTo(BigDecimal.valueOf(28)) > 0);
        assertTrue(cost.compareTo(BigDecimal.valueOf(29)) < 0);
    }
}
