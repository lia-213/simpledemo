package ilp.ilp_cw.ilp_1_2.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class DronePath {
    @JsonProperty("droneId")
    private final Integer droneId;
    @JsonProperty("deliveries")
    private List<Delivery> deliveries;

    public DronePath(Integer droneID, List<Delivery> deliveries) {
        this.droneId = droneID;
        this.deliveries = deliveries;
    }
}
