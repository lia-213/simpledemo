package ilp.ilp_cw.ilp_1_2.repository;

import ilp.ilp_cw.ilp_1_2.model.ServicePoint;
import ilp.ilp_cw.ilp_1_2.IlpConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Repository
public class ServicePointsRepository {
    private final RestTemplate restTemplate;

    @Autowired
    public ServicePointsRepository(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public List<ServicePoint> fetchAllServicePointsFromAPI() {
        String url = IlpConfig.getCurrentIlpEndpoint() + "service-points";
        ServicePoint[] servicePoints = this.restTemplate.getForObject(url, ServicePoint[].class);
        return servicePoints != null ? List.of(servicePoints) : List.of();

    }
}