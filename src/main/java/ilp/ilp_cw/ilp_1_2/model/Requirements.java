package ilp.ilp_cw.ilp_1_2.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Requirements {

    private Boolean heating;
    private Boolean cooling;
    private Float capacity;
    private Float maxCost;

    public Float getCapacity() {
        return capacity;
    }

    public Boolean getCooling() {
        return cooling;
    }

    public Boolean getHeating() {
        return heating;
    }

    public Float getMaxCost() {
        return maxCost;
    }
}