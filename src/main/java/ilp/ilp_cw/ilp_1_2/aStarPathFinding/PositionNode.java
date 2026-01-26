package ilp.ilp_cw.ilp_1_2.aStarPathFinding;

import lombok.Data;

import java.util.Objects;

import ilp.ilp_cw.ilp_1_2.model.Position;

/**
 * Wrapper around Position that implements GraphNode for A* pathfinding.
 * Each PositionNode represents a reachable position in the graph.
 */
@Data
public class PositionNode implements GraphNode {
    private final String id;
    private final Position position;

    public PositionNode(String id, Position position) {
        this.id = id;
        this.position = position;
    }

    @Override
    public String id() {
        return id;
    }

    public Position getPosition() {
        return position;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PositionNode that = (PositionNode) o;

        // Treat positions that are "close" as equal to prevent getting stuck
        // (e.g., 1.11111 and 1.11112 are considered the same point)
        if (position == null || that.position == null) {
            return Objects.equals(position, that.position);
        }

        // Use Position.isCloseTo() to check if positions are within 0.00015 distance
        // This matches the isCloseTo spec and prevents closed set from treating
        // nearly-identical positions as different
        return position.isCloseTo(that.position);
    }

    @Override
    public int hashCode() {
        // Hash based on rounded position coordinates to ensure close positions have same hash
        // This allows the HashMap/HashSet to bucket nearby positions together
        if (position == null) return Objects.hash((Object) null);

        // Round to 4 decimal places for bucketing (0.00015 ≈ 0.0002)
        double lngRounded = Math.round(position.getLongitude().doubleValue() * 10000.0) / 10000.0;
        double latRounded = Math.round(position.getLatitude().doubleValue() * 10000.0) / 10000.0;

        return Objects.hash(lngRounded, latRounded);
    }
}

