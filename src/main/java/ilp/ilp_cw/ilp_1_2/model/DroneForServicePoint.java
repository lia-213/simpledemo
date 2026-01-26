package ilp.ilp_cw.ilp_1_2.model;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

public record DroneForServicePoint(Integer servicePointId, List<DroneAvailability> drones) {
    public List<Availability> getDroneAvailabilityForSpecificDrone(Integer droneId) {
        if (droneId == null || drones == null) {
            return Collections.emptyList();
        }
        String droneIdStr = droneId.toString();
        return getDroneAvailabilityForSpecificDrone(droneIdStr);
    }

    public List<Availability> getDroneAvailabilityForSpecificDrone(String droneId) {
        if (droneId == null || drones == null) {
            return Collections.emptyList();
        }
        return drones.stream()
                .filter(d -> Objects.equals(droneId, d.id()))
                .findFirst()
                .map(DroneAvailability::availability)
                .orElse(Collections.emptyList());
    }
}
