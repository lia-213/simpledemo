package ilp.ilp_cw.ilp_1_2.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import ilp.ilp_cw.ilp_1_2.model.Position;
import ilp.ilp_cw.ilp_1_2.model.Region;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Wrapper DTO used by endpoints that require a current location and a region.
 * <p>
 * Maps JSON properties "position" and "region" to the current position and
 * target region objects respectively.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LocationRegionDto {
    @JsonProperty("position")
    private Position currPosition;

    @JsonProperty("region")
    private Region region;
}