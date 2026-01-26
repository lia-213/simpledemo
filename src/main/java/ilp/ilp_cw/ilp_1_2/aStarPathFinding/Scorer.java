package ilp.ilp_cw.ilp_1_2.aStarPathFinding;

import java.math.BigDecimal;

public interface Scorer<T extends GraphNode> {
    BigDecimal computeCost(T from, T to);
}
