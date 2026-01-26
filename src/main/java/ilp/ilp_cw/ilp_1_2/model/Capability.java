package ilp.ilp_cw.ilp_1_2.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record Capability(
        @JsonProperty("cooling") boolean cooling,
        @JsonProperty("heating") boolean heating,
        @JsonProperty("capacity") float capacity,
        @JsonProperty("maxMoves") int maxMoves,
        @JsonProperty("costPerMove") float costPerMove,
        @JsonProperty("costInitial") float costInitial,
        @JsonProperty("costFinal") float costFinal
) {
}
