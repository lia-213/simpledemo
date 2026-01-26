package ilp.ilp_cw.ilp_1_2.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class RestrictedArea {
    @JsonProperty("vertices")
    private final List<Position> positions;

    /**
     * Unique restricted area ID (guaranteed unique by ILP service)
     */
    private final Integer id;

    private String name;
}
