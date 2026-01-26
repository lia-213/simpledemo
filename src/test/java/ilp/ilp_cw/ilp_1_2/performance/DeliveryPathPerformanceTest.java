package ilp.ilp_cw.ilp_1_2.performance;

import ilp.ilp_cw.ilp_1_2.dto.MedDispatchRecDto;
import ilp.ilp_cw.ilp_1_2.model.*;
import ilp.ilp_cw.ilp_1_2.service.DroneIlpClient;
import ilp.ilp_cw.ilp_1_2.service.impl.DroneServiceImpl;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTimeout;

public class DeliveryPathPerformanceTest {

    // Simple stub implementation of the ILP client used by the service
    private static class StubDroneIlpClient implements DroneIlpClient {
        @Override
        public List<Drone> fetchAllDrones() {
            Capability cap = new Capability(false, false, 10f, 10000, 0.1f, 1f, 1f);
            Drone d = new Drone("drone1", "1", cap);
            List<Drone> list = new ArrayList<>();
            list.add(d);
            return list;
        }

        @Override
        public List<DroneForServicePoint> fetchAllDronesForServicePoints() {
            Availability avail = new Availability("MONDAY", LocalTime.of(0,0), LocalTime.of(23,59));
            DroneAvailability da = new DroneAvailability("1", List.of(avail));
            DroneForServicePoint dfsp = new DroneForServicePoint(1, List.of(da));
            return List.of(dfsp);
        }

        @Override
        public List<ServicePoint> fetchAllServicePoints() {
            Position p = new Position(BigDecimal.valueOf(0.0), BigDecimal.valueOf(0.0));
            ServicePoint sp = new ServicePoint(1, "SP1", p);
            return List.of(sp);
        }

        @Override
        public List<RestrictedArea> fetchAllRestrictedAreas() {
            return List.of();
        }

        @Override
        public List<Integer> findDroneIdsByCooling(Boolean cooling) {
            return List.of(1);
        }

        @Override
        public List<Integer> findDroneIdsByHeating(Boolean heating) {
            return List.of(1);
        }
    }

    @Test
    public void calcDeliveryPath_completesWithin30Seconds() {
        DroneIlpClient stub = new StubDroneIlpClient();
        DroneServiceImpl service = new DroneServiceImpl(stub);

        // Create a moderate workload: 50 deliveries spread around the origin
        List<MedDispatchRecDto> requests = new ArrayList<>();
        for (int i = 1; i <= 50; i++) {
            MedDispatchRecDto dto = new MedDispatchRecDto();
            dto.setId(i);
            Requirements req = new Requirements(false, false, 1.0f, Float.POSITIVE_INFINITY);
            dto.setRequirements(req);
            Position dest = new Position(BigDecimal.valueOf(0.001 * i), BigDecimal.valueOf(0.001 * i));
            dto.setDelivery(dest);
            requests.add(dto);
        }

        // Assert the call completes within 30 seconds
        assertTimeout(Duration.ofSeconds(30), () -> {
            service.calcDeliveryPath(requests, "greedy");
        }, "calcDeliveryPath should complete within 30 seconds");
    }
}
