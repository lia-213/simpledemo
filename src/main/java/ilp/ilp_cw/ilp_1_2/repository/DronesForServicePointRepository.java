package ilp.ilp_cw.ilp_1_2.repository;

import ilp.ilp_cw.ilp_1_2.model.DroneForServicePoint;
import ilp.ilp_cw.ilp_1_2.IlpConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Repository
public class DronesForServicePointRepository {
    private final RestTemplate restTemplate;

    @Autowired
    public DronesForServicePointRepository(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public List<DroneForServicePoint> fetchAllDronesForServicePointsFromAPI() {
        String url = IlpConfig.getCurrentIlpEndpoint() + "drones-for-service-points";
        DroneForServicePoint[] drones = this.restTemplate.getForObject(url, DroneForServicePoint[].class);
        return drones != null ? List.of(drones) : List.of();

    }
}
