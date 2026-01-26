package ilp.ilp_cw.ilp_1_2.aStarPathFinding;

import java.math.BigDecimal;
import java.math.MathContext;

import ilp.ilp_cw.ilp_1_2.model.Position;
import ilp.ilp_cw.ilp_1_2.util.MathUtils;

public class PositionScorer implements Scorer<PositionNode> {
    @Override
    public BigDecimal computeCost(PositionNode from, PositionNode to) {
        Position p1 = from.getPosition();
        Position p2 = to.getPosition();
        if (p1 == null || p2 == null
                || p1.getLatitude() == null || p1.getLongitude() == null
                || p2.getLatitude() == null || p2.getLongitude() == null) {
            throw new IllegalArgumentException("Positions must not be null and must have coordinates");
        }
        BigDecimal latDiff = p1.getLatitude().subtract(p2.getLatitude());
        BigDecimal lonDiff = p1.getLongitude().subtract(p2.getLongitude());
        BigDecimal distance = MathUtils.sqrt(latDiff.pow(2).add(lonDiff.pow(2)), MathContext.DECIMAL64);
        return distance;
    }
}

