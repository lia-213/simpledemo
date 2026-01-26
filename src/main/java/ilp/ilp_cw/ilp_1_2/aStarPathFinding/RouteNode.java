package ilp.ilp_cw.ilp_1_2.aStarPathFinding;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class RouteNode<T extends GraphNode> implements Comparable<RouteNode<T>> {
    private final T current;
    private T previous;
    private BigDecimal routeScore;
    private BigDecimal estimatedScore;

    RouteNode(T current) {
        this(current, null,
                new BigDecimal("999999999"),
                new BigDecimal("999999999"));
    }

    RouteNode(T current, T previous, BigDecimal routeScore, BigDecimal estimatedScore) {
        this.current = current;
        this.previous = previous;
        this.routeScore = routeScore;
        this.estimatedScore = estimatedScore;
    }

    @Override
    public int compareTo(RouteNode<T> other) {
        return this.estimatedScore.compareTo(other.estimatedScore);
    }
}
