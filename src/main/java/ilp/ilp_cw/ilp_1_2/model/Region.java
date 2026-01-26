package ilp.ilp_cw.ilp_1_2.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.awt.geom.Path2D;
import java.util.HashSet;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Region {
    @JsonProperty("name")
    private String name;
    @JsonProperty("vertices")
    private List<Position> vertices;

    @JsonIgnore
    public boolean isNotValidRegion() {
        if (vertices == null || vertices.size() < 3) {
            return true;
        }
        HashSet<Position> unique = new HashSet<>(vertices);
        return unique.size() < 3;
    }

    @JsonIgnore
    public Path2D.Double makePolygon() {
        if (vertices == null || vertices.isEmpty()) {
            return new Path2D.Double();
        }
        Path2D.Double polygon = new Path2D.Double();
        polygon.moveTo(vertices.getFirst().getLongitude().doubleValue(), vertices.getFirst().getLatitude().doubleValue());
        for (Position vertex : vertices) {
            polygon.lineTo(vertex.getLongitude().doubleValue(), vertex.getLatitude().doubleValue());
        }
        polygon.closePath();
        return polygon;
    }

    @JsonIgnore
    public boolean contains(Position position) {
        if (position == null || isNotValidRegion()) {
            return false;
        }
        Path2D.Double polygon = makePolygon();
        return polygon.contains(position.getLongitude().doubleValue(), position.getLatitude().doubleValue());
    }
}
