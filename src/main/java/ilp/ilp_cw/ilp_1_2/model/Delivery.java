package ilp.ilp_cw.ilp_1_2.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class Delivery {
    @JsonProperty("deliveryId")
    private final Integer deliveryId;
    @JsonProperty("flightPath")
    private FlightPath flightPath;

    public Delivery(Integer deliveryId, FlightPath flightPath) {
        this.deliveryId = deliveryId;
        this.flightPath = flightPath;
    }
}
