package ilp.ilp_cw.ilp_1_2.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;
import java.time.LocalTime;

@JsonIgnoreProperties(ignoreUnknown = true)
public record MedDispatchRec(
        @JsonProperty("id") Integer id,
        @JsonProperty("date") LocalDate date,
        @JsonProperty("time") LocalTime time,
        @JsonProperty("requirements") Requirements requirements,
        @JsonProperty("delivery") Position delivery
) {
}
