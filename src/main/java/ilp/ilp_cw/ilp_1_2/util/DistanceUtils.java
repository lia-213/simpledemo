package ilp.ilp_cw.ilp_1_2.util;

import java.math.BigDecimal;
import java.math.MathContext;

import ilp.ilp_cw.ilp_1_2.model.Position;

public class DistanceUtils {
    private DistanceUtils() {
    }

    public static BigDecimal euclideanDistance(Position p1, Position p2) {

        if (p1.getLatitude() == null || p1.getLongitude() == null
                || p2.getLatitude() == null || p2.getLongitude() == null) {
            throw new IllegalArgumentException("Positions must not be null and must have coordinates");
        }

        BigDecimal latDiff = p1.getLatitude().subtract(p2.getLatitude());
        BigDecimal lonDiff = p1.getLongitude().subtract(p2.getLongitude());
        return MathUtils.sqrt(latDiff.pow(2).add(lonDiff.pow(2)), MathContext.DECIMAL64);
    }
}
