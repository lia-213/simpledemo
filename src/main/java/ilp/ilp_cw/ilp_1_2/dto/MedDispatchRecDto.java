package ilp.ilp_cw.ilp_1_2.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import ilp.ilp_cw.ilp_1_2.model.Position;
import ilp.ilp_cw.ilp_1_2.model.Requirements;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * DTO for medical dispatch records from API requests.
 * <p>
 * Per CW2 spec and instructor clarification (Piazza, 2 weeks ago):
 * <ul>
 *   <li><b>Required fields</b>: id, requirements.capacity, delivery (destination)</li>
 *   <li><b>Optional fields</b>: date, time, requirements.heating, requirements.cooling, requirements.maxCost</li>
 * </ul>
 * <p>
 * Instructor's note: "you will never receive a 'false' / 'wrong' MedDispatchRecord - which means a record
 * which cannot be used. For the usage you will always need id and capacity + destination."
 * <p>
 * In practice: "I will not use this feature and always provide a date / time to make your planning easier."
 * Therefore, while date/time are technically optional, they will always be present in actual test cases.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MedDispatchRecDto {

    @JsonProperty("id")
    @NotNull
    private Integer id;

    /**
     * Date for the delivery. Optional per spec, but always provided in practice.
     */
    @JsonProperty("date")
    private LocalDate date;

    /**
     * Time for the delivery. Optional per spec, but always provided in practice.
     */
    @JsonProperty("time")
    private LocalTime time;

    /**
     * Requirements including capacity (required), heating, cooling, maxCost (all optional).
     */
    @JsonProperty("requirements")
    @NotNull
    private Requirements requirements;

    /**
     * Delivery destination. Required (added to spec later).
     */
    @JsonProperty("delivery")
    @NotNull
    private Position delivery;


}
