package ilp.ilp_cw.ilp_1_2.service.impl;

import ilp.ilp_cw.ilp_1_2.model.Drone;
import ilp.ilp_cw.ilp_1_2.repository.DroneRepository;
import ilp.ilp_cw.ilp_1_2.repository.DronesForServicePointRepository;
import ilp.ilp_cw.ilp_1_2.repository.RestrictedAreaRepository;
import ilp.ilp_cw.ilp_1_2.repository.ServicePointsRepository;
import ilp.ilp_cw.ilp_1_2.service.DroneIlpClient;
import ilp.ilp_cw.ilp_1_2.model.DroneForServicePoint;
import ilp.ilp_cw.ilp_1_2.model.RestrictedArea;
import ilp.ilp_cw.ilp_1_2.model.ServicePoint;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DroneIlpClientImpl implements DroneIlpClient {

    private final DroneRepository droneRepository;
    private final DronesForServicePointRepository dronesForServicePointRepository;
    private final ServicePointsRepository servicePointsRepository;
    private final RestrictedAreaRepository restrictedAreaRepository;

    @Autowired
    public DroneIlpClientImpl(DroneRepository droneRepository,
                              DronesForServicePointRepository dronesForServicePointRepository,
                              ServicePointsRepository servicePointsRepository,
                              RestrictedAreaRepository restrictedAreaRepository) {
        this.droneRepository = droneRepository;
        this.dronesForServicePointRepository = dronesForServicePointRepository;
        this.servicePointsRepository = servicePointsRepository;
        this.restrictedAreaRepository = restrictedAreaRepository;
    }

    @Override
    public List<Drone> fetchAllDrones() {
        return droneRepository.fetchAllDronesFromAPI();
    }

    @Override
    public List<DroneForServicePoint> fetchAllDronesForServicePoints() {
        return dronesForServicePointRepository.fetchAllDronesForServicePointsFromAPI();
    }

    @Override
    public List<ServicePoint> fetchAllServicePoints() {
        return servicePointsRepository.fetchAllServicePointsFromAPI();
    }

    @Override
    public List<RestrictedArea> fetchAllRestrictedAreas() {
        return restrictedAreaRepository.fetchAllRestrictedAreasFromAPI();
    }

    @Override
    public List<Integer> findDroneIdsByCooling(Boolean cooling) {
        return droneRepository.findIdsByCooling(cooling);
    }

    @Override
    public List<Integer> findDroneIdsByHeating(Boolean heating) {
        return droneRepository.findIdsByHeating(heating);
    }
}

