package ilp.ilp_cw.ilp_1_2.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for dynamic query attributes used by the POST /query endpoint.
 * Example JSON element:
 * { "attribute": "capacity", "operator": "<", "value": "8" }
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class QueryAttributeDto {
    @JsonProperty("attribute")
    private String attribute;

    @JsonProperty("operator")
    private String operator;

    @JsonProperty("value")
    private String value;
}

