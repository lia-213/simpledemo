package ilp.ilp_cw.ilp_1_2.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class DeliveryPathReturnStructure {
    @JsonProperty("totalCost")
    private Float totalCost;
    @JsonProperty("totalMoves")
    private Integer totalMoves;
    @JsonProperty("dronePaths")
    private List<DronePath> dronePaths;

    public DeliveryPathReturnStructure(Float totalCost, Integer totalMoves, List<DronePath> dronePaths) {
        this.totalCost = totalCost;
        this.totalMoves = totalMoves;
        this.dronePaths = dronePaths;
    }
}
