package ilp.ilp_cw.ilp_1_2.service;

import ilp.ilp_cw.ilp_1_2.model.Drone;
import ilp.ilp_cw.ilp_1_2.model.DroneForServicePoint;
import ilp.ilp_cw.ilp_1_2.model.RestrictedArea;
import ilp.ilp_cw.ilp_1_2.model.ServicePoint;

import java.util.List;

public interface DroneIlpClient {
    List<Drone> fetchAllDrones();

    List<DroneForServicePoint> fetchAllDronesForServicePoints();

    List<ServicePoint> fetchAllServicePoints();

    List<RestrictedArea> fetchAllRestrictedAreas();

    List<Integer> findDroneIdsByCooling(Boolean cooling);

    List<Integer> findDroneIdsByHeating(Boolean heating);
}

