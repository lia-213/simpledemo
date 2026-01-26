package ilp.ilp_cw.ilp_1_2.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import ilp.ilp_cw.ilp_1_2.model.Position;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO that wraps two {@link ilp.tutorials.ilp_cw1.model.Position} objects for endpoints that compare or
 * compute relationships between two locations (e.g. distance or closeness checks).
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PositionsDto {
    @JsonProperty("position1")
    private Position position1;
    @JsonProperty("position2")
    private Position position2;
}
