package ilp.ilp_cw.ilp_1_2.aStarPathFinding;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class RouteNodeTest {

    // Test implementation of GraphNode
    static class TestNode implements GraphNode {
        private final String id;

        TestNode(String id) {
            this.id = id;
        }

        @Override
        public String id() {
            return id;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            TestNode testNode = (TestNode) o;
            return Objects.equals(id, testNode.id);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id);
        }
    }

    private TestNode nodeA;
    private TestNode nodeB;
    private TestNode nodeC;

    @BeforeEach
    void setUp() {
        nodeA = new TestNode("A");
        nodeB = new TestNode("B");
        nodeC = new TestNode("C");
    }

    @Test
    void testConstructor_WithCurrentOnly() {
        RouteNode<TestNode> node = new RouteNode<>(nodeA);

        assertNotNull(node);
        assertEquals(nodeA, node.getCurrent());
        assertNull(node.getPrevious());
        
        // Should initialize with large values instead of infinity
        assertNotNull(node.getRouteScore());
        assertNotNull(node.getEstimatedScore());
        assertTrue(node.getRouteScore().compareTo(BigDecimal.ZERO) > 0);
        assertTrue(node.getEstimatedScore().compareTo(BigDecimal.ZERO) > 0);
    }

    @Test
    void testConstructor_WithAllParameters() {
        BigDecimal routeScore = new BigDecimal("10.5");
        BigDecimal estimatedScore = new BigDecimal("15.3");

        RouteNode<TestNode> node = new RouteNode<>(nodeA, nodeB, routeScore, estimatedScore);

        assertNotNull(node);
        assertEquals(nodeA, node.getCurrent());
        assertEquals(nodeB, node.getPrevious());
        assertEquals(routeScore, node.getRouteScore());
        assertEquals(estimatedScore, node.getEstimatedScore());
    }

    @Test
    void testConstructor_WithNullPrevious() {
        BigDecimal routeScore = BigDecimal.ZERO;
        BigDecimal estimatedScore = BigDecimal.ONE;

        RouteNode<TestNode> node = new RouteNode<>(nodeA, null, routeScore, estimatedScore);

        assertEquals(nodeA, node.getCurrent());
        assertNull(node.getPrevious());
        assertEquals(routeScore, node.getRouteScore());
        assertEquals(estimatedScore, node.getEstimatedScore());
    }

    @Test
    void testSetPrevious() {
        RouteNode<TestNode> node = new RouteNode<>(nodeA);

        assertNull(node.getPrevious());

        node.setPrevious(nodeB);
        assertEquals(nodeB, node.getPrevious());

        node.setPrevious(nodeC);
        assertEquals(nodeC, node.getPrevious());

        node.setPrevious(null);
        assertNull(node.getPrevious());
    }

    @Test
    void testSetRouteScore() {
        RouteNode<TestNode> node = new RouteNode<>(nodeA);

        BigDecimal newScore = new BigDecimal("25.7");
        node.setRouteScore(newScore);

        assertEquals(newScore, node.getRouteScore());
    }

    @Test
    void testSetEstimatedScore() {
        RouteNode<TestNode> node = new RouteNode<>(nodeA);

        BigDecimal newScore = new BigDecimal("42.3");
        node.setEstimatedScore(newScore);

        assertEquals(newScore, node.getEstimatedScore());
    }

    @Test
    void testCompareTo_LowerEstimatedScore() {
        RouteNode<TestNode> node1 = new RouteNode<>(nodeA, null, 
                new BigDecimal("10"), new BigDecimal("15"));
        RouteNode<TestNode> node2 = new RouteNode<>(nodeB, null, 
                new BigDecimal("20"), new BigDecimal("25"));

        assertTrue(node1.compareTo(node2) < 0);
    }

    @Test
    void testCompareTo_HigherEstimatedScore() {
        RouteNode<TestNode> node1 = new RouteNode<>(nodeA, null, 
                new BigDecimal("10"), new BigDecimal("30"));
        RouteNode<TestNode> node2 = new RouteNode<>(nodeB, null, 
                new BigDecimal("20"), new BigDecimal("25"));

        assertTrue(node1.compareTo(node2) > 0);
    }

    @Test
    void testCompareTo_EqualEstimatedScore() {
        RouteNode<TestNode> node1 = new RouteNode<>(nodeA, null, 
                new BigDecimal("10"), new BigDecimal("25"));
        RouteNode<TestNode> node2 = new RouteNode<>(nodeB, null, 
                new BigDecimal("20"), new BigDecimal("25"));

        assertEquals(0, node1.compareTo(node2));
    }

    @Test
    void testCompareTo_ZeroScores() {
        RouteNode<TestNode> node1 = new RouteNode<>(nodeA, null, 
                BigDecimal.ZERO, BigDecimal.ZERO);
        RouteNode<TestNode> node2 = new RouteNode<>(nodeB, null, 
                BigDecimal.ONE, BigDecimal.ZERO);

        assertEquals(0, node1.compareTo(node2));
    }

    @Test
    void testCompareTo_NegativeScores() {
        RouteNode<TestNode> node1 = new RouteNode<>(nodeA, null, 
                new BigDecimal("-5"), new BigDecimal("-10"));
        RouteNode<TestNode> node2 = new RouteNode<>(nodeB, null, 
                new BigDecimal("5"), new BigDecimal("-5"));

        assertTrue(node1.compareTo(node2) < 0);
    }

    @Test
    void testCompareTo_VeryLargeScores() {
        RouteNode<TestNode> node1 = new RouteNode<>(nodeA, null, 
                new BigDecimal("999999999"), new BigDecimal("999999999"));
        RouteNode<TestNode> node2 = new RouteNode<>(nodeB, null, 
                new BigDecimal("1000000000"), new BigDecimal("1000000000"));

        assertTrue(node1.compareTo(node2) < 0);
    }

    @Test
    void testCompareTo_DecimalPrecision() {
        RouteNode<TestNode> node1 = new RouteNode<>(nodeA, null, 
                new BigDecimal("10.001"), new BigDecimal("15.001"));
        RouteNode<TestNode> node2 = new RouteNode<>(nodeB, null, 
                new BigDecimal("10.002"), new BigDecimal("15.002"));

        assertTrue(node1.compareTo(node2) < 0);
    }

    @Test
    void testCompareTo_SelfComparison() {
        RouteNode<TestNode> node = new RouteNode<>(nodeA, null, 
                new BigDecimal("10"), new BigDecimal("15"));

        assertEquals(0, node.compareTo(node));
    }

    @Test
    void testPriorityQueueOrdering() {
        // Test that RouteNode works correctly in a PriorityQueue
        PriorityQueue<RouteNode<TestNode>> queue = new PriorityQueue<>();

        RouteNode<TestNode> node1 = new RouteNode<>(nodeA, null, 
                new BigDecimal("10"), new BigDecimal("30"));
        RouteNode<TestNode> node2 = new RouteNode<>(nodeB, null, 
                new BigDecimal("5"), new BigDecimal("10"));
        RouteNode<TestNode> node3 = new RouteNode<>(nodeC, null, 
                new BigDecimal("15"), new BigDecimal("20"));

        queue.add(node1);
        queue.add(node2);
        queue.add(node3);

        assertEquals(node2, queue.poll()); // Lowest estimated score
        assertEquals(node3, queue.poll());
        assertEquals(node1, queue.poll()); // Highest estimated score
    }

    @Test
    void testSortingCollection() {
        List<RouteNode<TestNode>> nodes = new ArrayList<>();

        nodes.add(new RouteNode<>(nodeA, null, new BigDecimal("10"), new BigDecimal("50")));
        nodes.add(new RouteNode<>(nodeB, null, new BigDecimal("5"), new BigDecimal("20")));
        nodes.add(new RouteNode<>(nodeC, null, new BigDecimal("15"), new BigDecimal("35")));

        Collections.sort(nodes);

        assertEquals(new BigDecimal("20"), nodes.get(0).getEstimatedScore());
        assertEquals(new BigDecimal("35"), nodes.get(1).getEstimatedScore());
        assertEquals(new BigDecimal("50"), nodes.get(2).getEstimatedScore());
    }

    @Test
    void testUpdateScoresDuringPathfinding() {
        // Simulate A* pathfinding score updates
        RouteNode<TestNode> node = new RouteNode<>(nodeA);

        // Initial state (large values)
        assertTrue(node.getRouteScore().compareTo(new BigDecimal("1000")) > 0);

        // Update as we discover a path to this node
        node.setRouteScore(BigDecimal.TEN);
        node.setEstimatedScore(new BigDecimal("25"));

        assertEquals(BigDecimal.TEN, node.getRouteScore());
        assertEquals(new BigDecimal("25"), node.getEstimatedScore());

        // Update with a better path
        node.setRouteScore(new BigDecimal("8"));
        node.setEstimatedScore(new BigDecimal("23"));

        assertEquals(new BigDecimal("8"), node.getRouteScore());
        assertEquals(new BigDecimal("23"), node.getEstimatedScore());
    }

    @Test
    void testGettersReturnCorrectValues() {
        BigDecimal routeScore = new BigDecimal("12.34");
        BigDecimal estimatedScore = new BigDecimal("56.78");

        RouteNode<TestNode> node = new RouteNode<>(nodeA, nodeB, routeScore, estimatedScore);

        assertEquals(nodeA, node.getCurrent());
        assertEquals(nodeB, node.getPrevious());
        assertEquals(routeScore, node.getRouteScore());
        assertEquals(estimatedScore, node.getEstimatedScore());
    }

    @Test
    void testConstructor_PreservesNodeReferences() {
        RouteNode<TestNode> node = new RouteNode<>(nodeA, nodeB, 
                BigDecimal.ONE, BigDecimal.TEN);

        assertSame(nodeA, node.getCurrent());
        assertSame(nodeB, node.getPrevious());
    }

    @Test
    void testSetPrevious_UpdatesReference() {
        RouteNode<TestNode> node = new RouteNode<>(nodeA);

        node.setPrevious(nodeB);
        assertSame(nodeB, node.getPrevious());

        node.setPrevious(nodeC);
        assertSame(nodeC, node.getPrevious());
    }

    @Test
    void testCompareTo_Transitivity() {
        // Test transitivity: if a < b and b < c, then a < c
        RouteNode<TestNode> nodeSmall = new RouteNode<>(nodeA, null, 
                BigDecimal.ZERO, new BigDecimal("10"));
        RouteNode<TestNode> nodeMedium = new RouteNode<>(nodeB, null, 
                BigDecimal.ZERO, new BigDecimal("20"));
        RouteNode<TestNode> nodeLarge = new RouteNode<>(nodeC, null, 
                BigDecimal.ZERO, new BigDecimal("30"));

        assertTrue(nodeSmall.compareTo(nodeMedium) < 0);
        assertTrue(nodeMedium.compareTo(nodeLarge) < 0);
        assertTrue(nodeSmall.compareTo(nodeLarge) < 0);
    }

    @Test
    void testCompareTo_Reflexivity() {
        // Test reflexivity: a.compareTo(a) == 0
        RouteNode<TestNode> node = new RouteNode<>(nodeA, null, 
                new BigDecimal("5"), new BigDecimal("10"));

        assertEquals(0, node.compareTo(node));
    }

    @Test
    void testCompareTo_Symmetry() {
        // Test symmetry: if a.compareTo(b) < 0, then b.compareTo(a) > 0
        RouteNode<TestNode> node1 = new RouteNode<>(nodeA, null, 
                BigDecimal.ZERO, new BigDecimal("10"));
        RouteNode<TestNode> node2 = new RouteNode<>(nodeB, null, 
                BigDecimal.ZERO, new BigDecimal("20"));

        assertTrue(node1.compareTo(node2) < 0);
        assertTrue(node2.compareTo(node1) > 0);
    }

    @Test
    void testRouteScoreIndependentOfComparison() {
        // compareTo only uses estimatedScore, not routeScore
        RouteNode<TestNode> node1 = new RouteNode<>(nodeA, null, 
                new BigDecimal("100"), new BigDecimal("15"));
        RouteNode<TestNode> node2 = new RouteNode<>(nodeB, null, 
                new BigDecimal("5"), new BigDecimal("25"));

        // node1 has higher routeScore but lower estimatedScore
        assertTrue(node1.compareTo(node2) < 0);
    }

    @Test
    void testEquals_FromLombok() {
        // Test that Lombok @Data generates proper equals
        RouteNode<TestNode> node1 = new RouteNode<>(nodeA, nodeB, 
                new BigDecimal("10"), new BigDecimal("20"));
        RouteNode<TestNode> node2 = new RouteNode<>(nodeA, nodeB, 
                new BigDecimal("10"), new BigDecimal("20"));

        assertEquals(node1, node2);
    }

    @Test
    void testHashCode_FromLombok() {
        // Test that Lombok @Data generates proper hashCode
        RouteNode<TestNode> node1 = new RouteNode<>(nodeA, nodeB, 
                new BigDecimal("10"), new BigDecimal("20"));
        RouteNode<TestNode> node2 = new RouteNode<>(nodeA, nodeB, 
                new BigDecimal("10"), new BigDecimal("20"));

        assertEquals(node1.hashCode(), node2.hashCode());
    }

    @Test
    void testToString_FromLombok() {
        // Test that Lombok @Data generates proper toString
        RouteNode<TestNode> node = new RouteNode<>(nodeA, nodeB, 
                new BigDecimal("10"), new BigDecimal("20"));

        String str = node.toString();
        assertNotNull(str);
        assertTrue(str.contains("RouteNode"));
    }
}
