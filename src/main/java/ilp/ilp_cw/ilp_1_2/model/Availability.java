package ilp.ilp_cw.ilp_1_2.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Availability {
    @JsonProperty("dayOfWeek")
    String dayOfWeek;
    @JsonProperty("from")
    LocalTime fromTime;
    @JsonProperty("until")
    LocalTime untilTime;

    @JsonIgnore
    public Boolean dateWorks(LocalDate dateToCheck) {
        return dateToCheck.getDayOfWeek().toString().equals(dayOfWeek);
    }

    @JsonIgnore
    public Boolean timeWorks(LocalTime fromTime, LocalTime untilTime, LocalTime timeToCheck) {
        return (timeToCheck.equals(fromTime) || timeToCheck.isAfter(fromTime)) &&
                (timeToCheck.equals(untilTime) || timeToCheck.isBefore(untilTime));
    }

}
