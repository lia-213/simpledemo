package ilp.ilp_cw.ilp_1_2.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import ilp.ilp_cw.ilp_1_2.aStarPathFinding.GraphNode;
import ilp.ilp_cw.ilp_1_2.util.DistanceUtils;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Position implements GraphNode {

    @JsonProperty("lng")
    private BigDecimal longitude;

    @JsonProperty("lat")
    private BigDecimal latitude;

    @JsonIgnore
    public boolean isValidPosition() {
        if (latitude == null || longitude == null) return false;
        return latitude.compareTo(BigDecimal.valueOf(-90)) >= 0 &&
                latitude.compareTo(BigDecimal.valueOf(90)) <= 0 &&
                longitude.compareTo(BigDecimal.valueOf(-180)) >= 0 &&
                longitude.compareTo(BigDecimal.valueOf(180)) <= 0;
    }

    @JsonIgnore
    public boolean isCloseTo(Position other) {
        if (other == null || !this.isValidPosition() || !other.isValidPosition()) {
            return false;
        }
        try {
            BigDecimal distance = DistanceUtils.euclideanDistance(this, other);
            return distance.compareTo(new BigDecimal("0.00015")) < 0;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    @Override
    public String id() {
        return "";
    }
}
