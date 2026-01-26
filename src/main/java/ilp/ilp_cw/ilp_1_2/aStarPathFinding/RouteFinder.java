package ilp.ilp_cw.ilp_1_2.aStarPathFinding;

import java.math.BigDecimal;
import java.util.*;

public class RouteFinder<T extends GraphNode> {
    private final Graph<T> graph;
    private final Scorer<T> nextNodeScorer;
    private final Scorer<T> targetScorer;
    private final Queue<RouteNode<T>> openSet = new PriorityQueue<>();
    private final Map<T, RouteNode<T>> allNodes = new HashMap<>();
    private final Set<T> closedSet = new HashSet<>(); // Track already-explored nodes

    public RouteFinder(Graph<T> graph, Scorer<T> nextNodeScorer, Scorer<T> targetScorer) {
        this.graph = graph;
        this.nextNodeScorer = nextNodeScorer;
        this.targetScorer = targetScorer;
    }

    public List<T> findRoute(T from, T to) {
        openSet.clear();
        allNodes.clear();
        closedSet.clear(); // Reset closed set for each search

        // start node: routeScore = 0, estimatedScore = heuristic(from, to)
        RouteNode<T> start = new RouteNode<>(
                from,
                null,
                BigDecimal.ZERO,
                nextNodeScorer.computeCost(from, to).add(targetScorer.computeCost(from, to))
        );
        openSet.add(start);
        allNodes.put(from, start);

        while (!openSet.isEmpty()) {
            RouteNode<T> next = openSet.poll();
            if (next == null) break;

            // Skip if already explored (avoid infinite loops and redundant work)
            if (closedSet.contains(next.getCurrent())) {
                continue;
            }
            closedSet.add(next.getCurrent()); // Mark as explored

            // goal reached -> reconstruct path
            if (next.getCurrent().equals(to)) {
                List<T> route = new ArrayList<>();
                RouteNode<T> current = next;
                while (current != null) {
                    route.add(0, current.getCurrent());
                    T prev = current.getPrevious();
                    current = (prev == null) ? null : allNodes.get(prev);
                }
                return route;
            }

            // expand neighbours via graph.getConnections
            for (T connection : graph.getConnections(next.getCurrent())) {
                RouteNode<T> nextNode = allNodes.getOrDefault(
                        connection,
                        new RouteNode<>(
                                connection,
                                null,
                                new BigDecimal("999999999"),
                                new BigDecimal("999999999"))
                );
                allNodes.put(connection, nextNode);

                BigDecimal newScore = next.getRouteScore()
                        .add(nextNodeScorer.computeCost(next.getCurrent(), connection));

                if (newScore.compareTo(nextNode.getRouteScore()) < 0) {
                    nextNode.setPrevious(next.getCurrent());
                    nextNode.setRouteScore(newScore);
                    BigDecimal estimated = newScore.add(targetScorer.computeCost(connection, to));
                    nextNode.setEstimatedScore(estimated);
                    openSet.add(nextNode);
                }
            }
        }

        throw new IllegalStateException("No route found");
    }
}
