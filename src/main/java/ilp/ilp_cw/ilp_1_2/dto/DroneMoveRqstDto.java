package ilp.ilp_cw.ilp_1_2.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import ilp.ilp_cw.ilp_1_2.model.Position;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO for requests that ask the service to compute the drone's next position.
 * <p>
 * Contains a starting {@link Position} and an {@link BigDecimal} angle in degrees.
 * Angle must be in the range [0, 360) and is validated by the service.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DroneMoveRqstDto {

    @JsonProperty("start")
    private Position startPosition;

    @JsonProperty("angle")
    private BigDecimal angle;
}
