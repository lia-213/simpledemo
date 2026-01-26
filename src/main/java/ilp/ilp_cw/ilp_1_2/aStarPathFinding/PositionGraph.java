package ilp.ilp_cw.ilp_1_2.aStarPathFinding;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ilp.ilp_cw.ilp_1_2.model.Position;
import ilp.ilp_cw.ilp_1_2.model.RestrictedArea;

/**
 * Lazy neighbor-generation graph for PositionNode.
 * Neighbors are generated using the same step width and angle constraints
 * as DroneServiceImpl.computeNextPosition, while respecting no-fly areas.
 */
public class PositionGraph implements Graph<PositionNode> {

    private static final BigDecimal STEP = new BigDecimal("0.00015");
    private static final BigDecimal FULL_CIRCLE = new BigDecimal("360");
    private static final BigDecimal ANGLE_INCREMENT = new BigDecimal("22.5");

    private final List<RestrictedArea> restrictedAreas;

    public PositionGraph(List<RestrictedArea> restrictedAreas) {
        this.restrictedAreas = restrictedAreas;
    }

    @Override
    public Set<PositionNode> getConnections(PositionNode node) {
        Set<PositionNode> neighbors = new HashSet<>();
        Position start = node.getPosition();
        if (start == null || !start.isValidPosition()) {
            return neighbors;
        }

        // Reject starting nodes that lie inside any restricted area
        if (isInsideAnyRestrictedArea(start)) {
            return neighbors; // treat as unreachable
        }

        // Generate neighbors for all allowed directions (0, 22.5, ..., 337.5)
        for (BigDecimal angle = BigDecimal.ZERO;
             angle.compareTo(FULL_CIRCLE) < 0;
             angle = angle.add(ANGLE_INCREMENT)) {

            Position nextPos = computeNextPosition(start, angle);
            if (nextPos == null || !nextPos.isValidPosition()) {
                continue;
            }

            // Apply no-fly constraints to this potential edge (start -> nextPos)
            if (isInsideAnyRestrictedArea(nextPos)) {
                continue; // endpoint inside restricted area
            }
            if (segmentCrossesAnyRestrictedArea(start, nextPos)) {
                continue; // segment intersects a restricted polygon boundary
            }

            String id = node.id() + "->" + angle.toPlainString();
            neighbors.add(new PositionNode(id, nextPos));
        }
        return neighbors;
    }

    /**
     * Compute next position from a given start and angle, mirroring
     * the logic from DroneServiceImpl.computeNextPosition but without
     * ResponseEntity and validation wrappers.
     */
    private Position computeNextPosition(Position start, BigDecimal angle) {
        try {
            double rad = Math.toRadians(angle.doubleValue());
            MathContext mc = MathContext.DECIMAL64;

            BigDecimal newLat = STEP.multiply(BigDecimal.valueOf(Math.cos(rad)), mc)
                    .add(start.getLatitude());
            BigDecimal newLng = STEP.multiply(BigDecimal.valueOf(Math.sin(rad)), mc)
                    .divide(BigDecimal.valueOf(Math.cos(Math.toRadians(start.getLatitude().doubleValue()))), mc)
                    .add(start.getLongitude());

            return new Position(newLng, newLat);
        } catch (Exception e) {
            return null;
        }
    }

    // ========================= No-fly helpers =========================

    private boolean isInsideAnyRestrictedArea(Position p) {
        if (restrictedAreas == null || restrictedAreas.isEmpty() || p == null) {
            return false;
        }
        for (RestrictedArea area : restrictedAreas) {
            if (area == null || area.getPositions() == null) continue;
            if (pointInPolygon(p, area.getPositions())) {
                return true;
            }
        }
        return false;
    }

    private boolean segmentCrossesAnyRestrictedArea(Position from, Position to) {
        if (restrictedAreas == null || restrictedAreas.isEmpty() || from == null || to == null) {
            return false;
        }
        // Simple approximation: sample points along the segment and check if any lies inside a restricted area
        final int samples = 5; // small number for approximation
        for (int i = 1; i < samples; i++) {
            double t = (double) i / samples;
            BigDecimal lng = from.getLongitude().add(
                    to.getLongitude().subtract(from.getLongitude()).multiply(BigDecimal.valueOf(t))
            );
            BigDecimal lat = from.getLatitude().add(
                    to.getLatitude().subtract(from.getLatitude()).multiply(BigDecimal.valueOf(t))
            );
            Position sample = new Position(lng, lat);
            if (isInsideAnyRestrictedArea(sample)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Basic point-in-polygon test using ray casting over the polygon defined by a list of Positions.
     * Assumes the polygon is defined by a sequence of positions (closed or open).
     */
    private boolean pointInPolygon(Position p, List<Position> vertices) {
        if (p == null || vertices == null || vertices.isEmpty() || vertices.size() < 3) {
            return false;
        }
//        if (polygonDto.get() != null) vertices.add(polygonDto.getPosition1());
//        if (polygonDto.getPosition2() != null) vertices.add(polygonDto.getPosition2());
//        if (polygonDto.getPosition3() != null) vertices.add(polygonDto.getPosition3());
//        if (polygonDto.getPosition4() != null) vertices.add(polygonDto.getPosition4());
//        if (vertices.size() < 3) return false; // not a polygon

        boolean inside = false;
        int n = vertices.size();
        double x = p.getLongitude().doubleValue();
        double y = p.getLatitude().doubleValue();

        for (int i = 0, j = n - 1; i < n; j = i++) {
            Position vi = vertices.get(i);
            Position vj = vertices.get(j);
            double xi = vi.getLongitude().doubleValue();
            double yi = vi.getLatitude().doubleValue();
            double xj = vj.getLongitude().doubleValue();
            double yj = vj.getLatitude().doubleValue();

            boolean intersect = ((yi > y) != (yj > y)) &&
                    (x < (xj - xi) * (y - yi) / (yj - yi + 1e-12) + xi);
            if (intersect) inside = !inside;
        }
        return inside;
    }
}
