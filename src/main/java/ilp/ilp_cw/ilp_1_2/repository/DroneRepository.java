package ilp.ilp_cw.ilp_1_2.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.web.client.RestTemplate;

import ilp.ilp_cw.ilp_1_2.model.Drone;
import ilp.ilp_cw.ilp_1_2.IlpConfig;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * REST client wrapper for fetching drone data from the external ILP REST API.
 * <p>
 * Per instructor clarification: "you were never supposed to have any kind of persistence layer.
 * All operations are in-memory only. You should reach out to the ILP-service on each request
 * and retrieve the necessary information."
 * <p>
 * This is NOT a JPA/database repository - it's a REST client that fetches fresh data
 * from the ILP REST API on each call. No caching or persistence.
 */
@Repository
public class DroneRepository {
    private final RestTemplate restTemplate;

    @Autowired
    public DroneRepository(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Fetches ALL drones from the ILP REST API with NO caching.
     * <p>
     * Per spec: "Make sure you retrieve all necessary data for your calculations and decisions
     * every time you start processing an endpoint. During the call in the endpoint data won't change,
     * yet it could between 2 calls."
     * <p>
     * Per instructor clarification (Nov 2025): "this only applies to the URL as this is the endpoint,
     * which could / will change. Make sure you can handle this and have proper defaults"
     * <p>
     * This method makes a fresh HTTP GET request to the ILP service each time it's called.
     * URL is also read fresh from environment variable each time (can change between calls).
     *
     * @return List of all drones from the ILP API
     */
    public List<Drone> fetchAllDronesFromAPI() {
        String url = IlpConfig.getCurrentIlpEndpoint() + "drones";
        Drone[] drones = restTemplate.getForObject(url, Drone[].class);
        return drones != null ? Arrays.asList(drones) : List.of();
    }

    /**
     * Finds drone IDs by cooling capability for the dronesWithCooling endpoint.
     * <p>
     * Per instructor: "You can ignore these flags if not needed"
     * - Drones WITH cooling can deliver packages that DON'T need cooling ✅
     * - Drones WITHOUT cooling CANNOT deliver packages that need cooling ❌
     *
     * @param cooling true = return only drones WITH cooling, false/null = return ALL drones
     * @return List of drone IDs matching the cooling filter
     */
    public List<Integer> findIdsByCooling(Boolean cooling) {
        List<Drone> allDrones = fetchAllDronesFromAPI();
        ArrayList<Integer> dronesToReturn = new ArrayList<>();

        // Filter drones based on cooling capability matching the requested state
        // cooling=true → return drones WITH cooling (cooling=true)
        // cooling=false → return drones WITHOUT cooling (cooling=false)
        for (Drone drone : allDrones) {
            if (drone.getCooling() == Boolean.TRUE.equals(cooling)) {
                dronesToReturn.add(drone.getId());
            }
        }

        return dronesToReturn;
    }

    /**
     * Finds drone IDs by heating capability for the dronesWithHeating endpoint.
     *
     * @param heating true = return only drones WITH heating, false/null = return ALL drones
     * @return List of drone IDs matching the heating filter
     */
    public List<Integer> findIdsByHeating(Boolean heating) {
        List<Drone> allDrones = fetchAllDronesFromAPI();
        ArrayList<Integer> dronesToReturn = new ArrayList<>();

        for (Drone drone : allDrones) {
            if (drone.getHeating() == Boolean.TRUE.equals(heating)) {
                dronesToReturn.add(drone.getId());
            }
        }

        return dronesToReturn;
    }
}
