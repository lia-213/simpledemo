package ilp.ilp_cw.ilp_1_2.aStarPathFinding;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class RouteFinderTest {

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

        @Override
        public String toString() {
            return "Node(" + id + ")";
        }
    }

    // Test implementation of Graph
    static class TestGraph implements Graph<TestNode> {
        private final Map<TestNode, Set<TestNode>> connections = new HashMap<>();

        void addConnection(TestNode from, TestNode to) {
            connections.computeIfAbsent(from, k -> new HashSet<>()).add(to);
        }

        @Override
        public Set<TestNode> getConnections(TestNode node) {
            return connections.getOrDefault(node, Collections.emptySet());
        }
    }

    // Simple scorer that returns distance of 1 for any connection
    static class UnitScorer implements Scorer<TestNode> {
        @Override
        public BigDecimal computeCost(TestNode from, TestNode to) {
            if (from == null || to == null) {
                return new BigDecimal("999999"); // Large value instead of infinity
            }
            return BigDecimal.ONE;
        }
    }

    // Heuristic scorer that returns 0 (Dijkstra behavior)
    static class ZeroHeuristicScorer implements Scorer<TestNode> {
        @Override
        public BigDecimal computeCost(TestNode from, TestNode to) {
            if (from == null || to == null) {
                return new BigDecimal("999999"); // Large value instead of infinity
            }
            return BigDecimal.ZERO;
        }
    }

    // Custom scorer for weighted graphs
    static class WeightedScorer implements Scorer<TestNode> {
        private final Map<String, BigDecimal> weights = new HashMap<>();

        void setWeight(String fromId, String toId, BigDecimal weight) {
            weights.put(fromId + "->" + toId, weight);
        }

        @Override
        public BigDecimal computeCost(TestNode from, TestNode to) {
            if (from == null || to == null) {
                return new BigDecimal("999999"); // Large value instead of infinity
            }
            return weights.getOrDefault(from.id() + "->" + to.id(), BigDecimal.ONE);
        }
    }

    private TestGraph graph;
    private TestNode nodeA;
    private TestNode nodeB;
    private TestNode nodeC;
    private TestNode nodeD;
    private TestNode nodeE;

    @BeforeEach
    void setUp() {
        graph = new TestGraph();
        nodeA = new TestNode("A");
        nodeB = new TestNode("B");
        nodeC = new TestNode("C");
        nodeD = new TestNode("D");
        nodeE = new TestNode("E");
    }

    @Test
    void testFindRoute_SimpleDirectPath() {
        // A -> B
        graph.addConnection(nodeA, nodeB);

        RouteFinder<TestNode> finder = new RouteFinder<>(
                graph,
                new UnitScorer(),
                new ZeroHeuristicScorer()
        );

        List<TestNode> route = finder.findRoute(nodeA, nodeB);

        assertNotNull(route);
        assertEquals(2, route.size());
        assertEquals(nodeA, route.get(0));
        assertEquals(nodeB, route.get(1));
    }

    @Test
    void testFindRoute_StartAndGoalAreSame() {
        // A -> A (self loop or just checking same node)
        RouteFinder<TestNode> finder = new RouteFinder<>(
                graph,
                new UnitScorer(),
                new ZeroHeuristicScorer()
        );

        List<TestNode> route = finder.findRoute(nodeA, nodeA);

        assertNotNull(route);
        assertEquals(1, route.size());
        assertEquals(nodeA, route.get(0));
    }

    @Test
    void testFindRoute_LinearPath() {
        // A -> B -> C
        graph.addConnection(nodeA, nodeB);
        graph.addConnection(nodeB, nodeC);

        RouteFinder<TestNode> finder = new RouteFinder<>(
                graph,
                new UnitScorer(),
                new ZeroHeuristicScorer()
        );

        List<TestNode> route = finder.findRoute(nodeA, nodeC);

        assertNotNull(route);
        assertEquals(3, route.size());
        assertEquals(nodeA, route.get(0));
        assertEquals(nodeB, route.get(1));
        assertEquals(nodeC, route.get(2));
    }

    @Test
    void testFindRoute_NoRouteExists() {
        // A -> B, C -> D (disconnected)
        graph.addConnection(nodeA, nodeB);
        graph.addConnection(nodeC, nodeD);

        RouteFinder<TestNode> finder = new RouteFinder<>(
                graph,
                new UnitScorer(),
                new ZeroHeuristicScorer()
        );

        assertThrows(IllegalStateException.class, () -> {
            finder.findRoute(nodeA, nodeD);
        });
    }

    @Test
    void testFindRoute_NoRouteExists_Message() {
        graph.addConnection(nodeA, nodeB);

        RouteFinder<TestNode> finder = new RouteFinder<>(
                graph,
                new UnitScorer(),
                new ZeroHeuristicScorer()
        );

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            finder.findRoute(nodeA, nodeC);
        });

        assertEquals("No route found", exception.getMessage());
    }

    @Test
    void testFindRoute_ChoosesShortestPath() {
        // A -> B -> D (cost 2)
        // A -> C -> D (cost 2)
        // A -> D (cost 1) - should choose this
        graph.addConnection(nodeA, nodeB);
        graph.addConnection(nodeB, nodeD);
        graph.addConnection(nodeA, nodeC);
        graph.addConnection(nodeC, nodeD);
        graph.addConnection(nodeA, nodeD);

        RouteFinder<TestNode> finder = new RouteFinder<>(
                graph,
                new UnitScorer(),
                new ZeroHeuristicScorer()
        );

        List<TestNode> route = finder.findRoute(nodeA, nodeD);

        assertNotNull(route);
        assertEquals(2, route.size());
        assertEquals(nodeA, route.get(0));
        assertEquals(nodeD, route.get(1));
    }

    @Test
    void testFindRoute_WeightedGraph() {
        // A -> B (cost 10)
        // A -> C (cost 1)
        // C -> D (cost 1)
        // B -> D (cost 1)
        // Shortest: A -> C -> D (cost 2)
        graph.addConnection(nodeA, nodeB);
        graph.addConnection(nodeA, nodeC);
        graph.addConnection(nodeC, nodeD);
        graph.addConnection(nodeB, nodeD);

        WeightedScorer scorer = new WeightedScorer();
        scorer.setWeight("A", "B", BigDecimal.TEN);
        scorer.setWeight("A", "C", BigDecimal.ONE);
        scorer.setWeight("C", "D", BigDecimal.ONE);
        scorer.setWeight("B", "D", BigDecimal.ONE);

        RouteFinder<TestNode> finder = new RouteFinder<>(
                graph,
                scorer,
                new ZeroHeuristicScorer()
        );

        List<TestNode> route = finder.findRoute(nodeA, nodeD);

        assertNotNull(route);
        assertEquals(3, route.size());
        assertEquals(nodeA, route.get(0));
        assertEquals(nodeC, route.get(1));
        assertEquals(nodeD, route.get(2));
    }

    @Test
    void testFindRoute_ComplexGraph() {
        // A -> B, A -> C
        // B -> D
        // C -> D, C -> E
        // D -> E
        graph.addConnection(nodeA, nodeB);
        graph.addConnection(nodeA, nodeC);
        graph.addConnection(nodeB, nodeD);
        graph.addConnection(nodeC, nodeD);
        graph.addConnection(nodeC, nodeE);
        graph.addConnection(nodeD, nodeE);

        RouteFinder<TestNode> finder = new RouteFinder<>(
                graph,
                new UnitScorer(),
                new ZeroHeuristicScorer()
        );

        List<TestNode> route = finder.findRoute(nodeA, nodeE);

        assertNotNull(route);
        // Shortest path should be A -> C -> E (length 3)
        assertEquals(3, route.size());
        assertEquals(nodeA, route.get(0));
        assertEquals(nodeC, route.get(1));
        assertEquals(nodeE, route.get(2));
    }

    @Test
    void testFindRoute_WithCycles() {
        // A -> B -> C -> A (cycle)
        // B -> D
        graph.addConnection(nodeA, nodeB);
        graph.addConnection(nodeB, nodeC);
        graph.addConnection(nodeC, nodeA);
        graph.addConnection(nodeB, nodeD);

        RouteFinder<TestNode> finder = new RouteFinder<>(
                graph,
                new UnitScorer(),
                new ZeroHeuristicScorer()
        );

        List<TestNode> route = finder.findRoute(nodeA, nodeD);

        assertNotNull(route);
        assertEquals(3, route.size());
        assertEquals(nodeA, route.get(0));
        assertEquals(nodeB, route.get(1));
        assertEquals(nodeD, route.get(2));
    }

    @Test
    void testFindRoute_PreventsInfiniteLoop() {
        // A <-> B (bidirectional)
        // B -> C
        graph.addConnection(nodeA, nodeB);
        graph.addConnection(nodeB, nodeA);
        graph.addConnection(nodeB, nodeC);

        RouteFinder<TestNode> finder = new RouteFinder<>(
                graph,
                new UnitScorer(),
                new ZeroHeuristicScorer()
        );

        List<TestNode> route = finder.findRoute(nodeA, nodeC);

        assertNotNull(route);
        assertEquals(3, route.size());
        assertEquals(nodeA, route.get(0));
        assertEquals(nodeB, route.get(1));
        assertEquals(nodeC, route.get(2));
    }

    @Test
    void testFindRoute_HeuristicGuideSearch() {
        // Graph: A -> B -> C -> D
        // Also: A -> E (wrong direction)
        graph.addConnection(nodeA, nodeB);
        graph.addConnection(nodeB, nodeC);
        graph.addConnection(nodeC, nodeD);
        graph.addConnection(nodeA, nodeE);

        // Heuristic that favors the correct direction
        Scorer<TestNode> heuristic = (from, to) -> {
            if (to.id().equals("D")) {
                // Penalize going to E
                if (from.id().equals("E")) {
                    return BigDecimal.TEN;
                }
            }
            return BigDecimal.ZERO;
        };

        RouteFinder<TestNode> finder = new RouteFinder<>(
                graph,
                new UnitScorer(),
                heuristic
        );

        List<TestNode> route = finder.findRoute(nodeA, nodeD);

        assertNotNull(route);
        assertEquals(4, route.size());
        assertEquals(nodeA, route.get(0));
        assertEquals(nodeD, route.get(3));
    }

    @Test
    void testFindRoute_EmptyOpenSetScenario() {
        // Single node with no connections
        graph = new TestGraph();

        RouteFinder<TestNode> finder = new RouteFinder<>(
                graph,
                new UnitScorer(),
                new ZeroHeuristicScorer()
        );

        // Starting node has no connections to reach target
        assertThrows(IllegalStateException.class, () -> {
            finder.findRoute(nodeA, nodeB);
        });
    }

    @Test
    void testFindRoute_MultipleCallsIndependent() {
        // A -> B -> C
        graph.addConnection(nodeA, nodeB);
        graph.addConnection(nodeB, nodeC);

        RouteFinder<TestNode> finder = new RouteFinder<>(
                graph,
                new UnitScorer(),
                new ZeroHeuristicScorer()
        );

        // First call
        List<TestNode> route1 = finder.findRoute(nodeA, nodeB);
        assertEquals(2, route1.size());

        // Second call should be independent
        List<TestNode> route2 = finder.findRoute(nodeA, nodeC);
        assertEquals(3, route2.size());

        // First route should still be valid
        assertEquals(2, route1.size());
    }

    @Test
    void testFindRoute_UpdatesBetterPath() {
        // A -> B -> D (cost 2)
        // A -> C (cost 5)
        // C -> B (cost 1) 
        // So A -> B costs 2 directly, but A -> C -> B would cost 6
        // Should choose direct path
        graph.addConnection(nodeA, nodeB);
        graph.addConnection(nodeB, nodeD);
        graph.addConnection(nodeA, nodeC);
        graph.addConnection(nodeC, nodeB);

        WeightedScorer scorer = new WeightedScorer();
        scorer.setWeight("A", "B", BigDecimal.valueOf(2));
        scorer.setWeight("B", "D", BigDecimal.ONE);
        scorer.setWeight("A", "C", BigDecimal.valueOf(5));
        scorer.setWeight("C", "B", BigDecimal.ONE);

        RouteFinder<TestNode> finder = new RouteFinder<>(
                graph,
                scorer,
                new ZeroHeuristicScorer()
        );

        List<TestNode> route = finder.findRoute(nodeA, nodeD);

        // Should be A -> B -> D
        assertEquals(3, route.size());
        assertEquals(nodeA, route.get(0));
        assertEquals(nodeB, route.get(1));
        assertEquals(nodeD, route.get(2));
    }

    @Test
    void testFindRoute_LargeGraph() {
        // Create a grid-like graph with 10 nodes
        TestNode[] nodes = new TestNode[10];
        for (int i = 0; i < 10; i++) {
            nodes[i] = new TestNode("N" + i);
        }

        // Linear connections: N0 -> N1 -> N2 -> ... -> N9
        for (int i = 0; i < 9; i++) {
            graph.addConnection(nodes[i], nodes[i + 1]);
        }

        RouteFinder<TestNode> finder = new RouteFinder<>(
                graph,
                new UnitScorer(),
                new ZeroHeuristicScorer()
        );

        List<TestNode> route = finder.findRoute(nodes[0], nodes[9]);

        assertNotNull(route);
        assertEquals(10, route.size());
        assertEquals(nodes[0], route.get(0));
        assertEquals(nodes[9], route.get(9));
    }

    @Test
    void testFindRoute_ClosedSetPreventsRevisit() {
        // A -> B -> C
        // A -> D -> B (more expensive to reach B)
        // B should only be explored once via the cheaper path
        graph.addConnection(nodeA, nodeB);
        graph.addConnection(nodeB, nodeC);
        graph.addConnection(nodeA, nodeD);
        graph.addConnection(nodeD, nodeB);

        WeightedScorer scorer = new WeightedScorer();
        scorer.setWeight("A", "B", BigDecimal.ONE);
        scorer.setWeight("B", "C", BigDecimal.ONE);
        scorer.setWeight("A", "D", BigDecimal.valueOf(5));
        scorer.setWeight("D", "B", BigDecimal.ONE);

        RouteFinder<TestNode> finder = new RouteFinder<>(
                graph,
                scorer,
                new ZeroHeuristicScorer()
        );

        List<TestNode> route = finder.findRoute(nodeA, nodeC);

        // Should use A -> B -> C (not A -> D -> B -> C)
        assertEquals(3, route.size());
        assertEquals(nodeA, route.get(0));
        assertEquals(nodeB, route.get(1));
        assertEquals(nodeC, route.get(2));
    }

    @Test
    void testFindRoute_WithNullNextInOpenSet() {
        // Edge case: openSet.poll() could theoretically return null
        // In practice, this won't happen with our implementation,
        // but the code handles it with the null check
        
        // Standard case to ensure no NPE
        graph.addConnection(nodeA, nodeB);

        RouteFinder<TestNode> finder = new RouteFinder<>(
                graph,
                new UnitScorer(),
                new ZeroHeuristicScorer()
        );

        List<TestNode> route = finder.findRoute(nodeA, nodeB);
        assertNotNull(route);
        assertEquals(2, route.size());
    }

    @Test
    void testFindRoute_BidirectionalGraph() {
        // A <-> B <-> C (all bidirectional)
        graph.addConnection(nodeA, nodeB);
        graph.addConnection(nodeB, nodeA);
        graph.addConnection(nodeB, nodeC);
        graph.addConnection(nodeC, nodeB);

        RouteFinder<TestNode> finder = new RouteFinder<>(
                graph,
                new UnitScorer(),
                new ZeroHeuristicScorer()
        );

        List<TestNode> route = finder.findRoute(nodeC, nodeA);

        assertNotNull(route);
        assertEquals(3, route.size());
        assertEquals(nodeC, route.get(0));
        assertEquals(nodeB, route.get(1));
        assertEquals(nodeA, route.get(2));
    }

    @Test
    void testFindRoute_ComplexCostCalculation() {
        // A -> B, A -> C, B -> D, C -> D
        graph.addConnection(nodeA, nodeB);
        graph.addConnection(nodeA, nodeC);
        graph.addConnection(nodeB, nodeD);
        graph.addConnection(nodeC, nodeD);

        WeightedScorer nextScorer = new WeightedScorer();
        nextScorer.setWeight("A", "B", BigDecimal.valueOf(3));
        nextScorer.setWeight("A", "C", BigDecimal.valueOf(1));
        nextScorer.setWeight("B", "D", BigDecimal.valueOf(1));
        nextScorer.setWeight("C", "D", BigDecimal.valueOf(2));

        WeightedScorer targetScorer = new WeightedScorer();
        targetScorer.setWeight("B", "D", BigDecimal.valueOf(1));
        targetScorer.setWeight("C", "D", BigDecimal.valueOf(2));

        RouteFinder<TestNode> finder = new RouteFinder<>(
                graph,
                nextScorer,
                targetScorer
        );

        List<TestNode> route = finder.findRoute(nodeA, nodeD);

        assertNotNull(route);
        // A -> C -> D (cost 3) vs A -> B -> D (cost 4)
        assertEquals(3, route.size());
        assertEquals(nodeA, route.get(0));
        assertEquals(nodeC, route.get(1));
        assertEquals(nodeD, route.get(2));
    }

    @Test
    void testFindRoute_HighlyConnectedGraph() {
        // Create a graph with multiple paths and redundant edges
        // to ensure the closed set is properly used
        // A -> B, A -> C, A -> D
        // B -> C, B -> D
        // C -> D, C -> E
        // D -> E
        graph.addConnection(nodeA, nodeB);
        graph.addConnection(nodeA, nodeC);
        graph.addConnection(nodeA, nodeD);
        graph.addConnection(nodeB, nodeC);
        graph.addConnection(nodeB, nodeD);
        graph.addConnection(nodeC, nodeD);
        graph.addConnection(nodeC, nodeE);
        graph.addConnection(nodeD, nodeE);

        RouteFinder<TestNode> finder = new RouteFinder<>(
                graph,
                new UnitScorer(),
                new ZeroHeuristicScorer()
        );

        List<TestNode> route = finder.findRoute(nodeA, nodeE);

        assertNotNull(route);
        // Should find a path (likely A -> C -> E or similar)
        assertEquals(nodeA, route.get(0));
        assertEquals(nodeE, route.get(route.size() - 1));
        assertTrue(route.size() >= 2 && route.size() <= 4);
    }
}
