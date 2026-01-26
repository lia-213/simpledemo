package ilp.ilp_cw.ilp_1_2.util;

import java.math.BigDecimal;
import java.math.MathContext;

/**
 * Small math utility helpers used across the project.
 * <p>
 * Contains a BigDecimal-based square-root helper that uses an initial double-based
 * approximation and one Newton-like correction step. Suitable for small-scale
 * calculations in this coursework context.
 */
public final class MathUtils {

    private MathUtils() {
    } // prevent instantiation

    /**
     * Compute the square root of a {@link BigDecimal} value using a double-based
     * initial approximation and a single correction step with the provided {@link MathContext}.
     *
     * @param value the value to take the square root of
     * @param mc    the math context controlling precision and rounding
     * @return approximate square root as a {@link BigDecimal}
     */
    public static BigDecimal sqrt(BigDecimal value, MathContext mc) {
        double vd = value.doubleValue();
        if (Double.isNaN(vd) || Double.isInfinite(vd) || vd < 0) {
            throw new IllegalArgumentException("Invalid value for sqrt: " + vd);
        }
        if (vd == 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal x = new BigDecimal(Math.sqrt(vd), mc);
        double xd = x.doubleValue();
        if (xd == 0) {
            return x;
        }
        double correction = value.subtract(x.multiply(x)).doubleValue() / (xd * 2.0);
        if (Double.isNaN(correction) || Double.isInfinite(correction)) {
            return x;
        }
        return x.add(new BigDecimal(correction, mc));
    }
}
