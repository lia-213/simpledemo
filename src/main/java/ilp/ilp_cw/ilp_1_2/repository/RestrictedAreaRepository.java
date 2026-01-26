package ilp.ilp_cw.ilp_1_2.repository;

import ilp.ilp_cw.ilp_1_2.model.RestrictedArea;
import ilp.ilp_cw.ilp_1_2.IlpConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Repository
public class RestrictedAreaRepository {
    private final RestTemplate restTemplate;

    @Autowired
    public RestrictedAreaRepository(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public List<RestrictedArea> fetchAllRestrictedAreasFromAPI() {
        String url = IlpConfig.getCurrentIlpEndpoint() + "restricted-areas";
        RestrictedArea[] restrictedAreas = this.restTemplate.getForObject(url, RestrictedArea[].class);
        return restrictedAreas != null ? List.of(restrictedAreas) : List.of();

    }
}
