package ilp.ilp_cw.ilp_1_2.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class FlightPath {
    @JsonValue
    private final List<Position> stops = new ArrayList<>();

    @JsonIgnore
    private final List<Integer> segmentEndIndices = new ArrayList<>();

    @JsonIgnore
    private Float totalCost;

    @JsonIgnore
    private Integer totalMoves;
}
