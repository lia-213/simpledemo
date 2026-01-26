package ilp.ilp_cw.ilp_1_2.aStarPathFinding;

import java.util.Set;

/**
 * Generic graph interface for A* that exposes neighbors lazily.
 * Implementations can generate neighbors on the fly (e.g. using nextPosition logic).
 */
public interface Graph<T extends GraphNode> {

    /**
     * Return all neighbors of the given node that are reachable by one legal move.
     */
    Set<T> getConnections(T node);
}
