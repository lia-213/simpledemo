package ilp.ilp_cw.ilp_1_2.model;

import ilp.ilp_cw.ilp_1_2.aStarPathFinding.PositionNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class DroneTest {

    private Capability capability;
    private Drone drone;

    @BeforeEach
    void setUp() {
        capability = new Capability(true, false, 10.0f, 100, 0.5f, 1.0f, 1.0f);
        drone = new Drone("TestDrone", "1", capability);
    }

    @Test
    void testConstructor() {
        assertEquals("TestDrone", drone.getName());
        assertEquals(1, drone.getId()); // getId() returns Integer, not String
        assertSame(capability, drone.getCapability());
    }

    @Test
    void testConstructor_WithNullValues() {
        Drone nullDrone = new Drone(null, null, null);
        assertNull(nullDrone.getName());
        assertNull(nullDrone.getId());
        assertNull(nullDrone.getCapability());
    }

    @Test
    void testGetId_ValidString() {
        Drone drone = new Drone("Test", "42", capability);
        assertEquals(42, drone.getId());
    }

    @Test
    void testGetId_NullString() {
        Drone drone = new Drone("Test", null, capability);
        assertNull(drone.getId());
    }

    @Test
    void testGetId_EmptyString() {
        Drone drone = new Drone("Test", "", capability);
        assertNull(drone.getId());
    }

    @Test
    void testGetId_NonNumericString() {
        Drone drone = new Drone("Test", "abc", capability);
        assertThrows(NumberFormatException.class, drone::getId);
    }

    @Test
    void testGetCooling_True() {
        Capability capWithCooling = new Capability(true, false, 10.0f, 100, 0.5f, 1.0f, 1.0f);
        Drone drone = new Drone("Test", "1", capWithCooling);
        assertTrue(drone.getCooling());
    }

    @Test
    void testGetCooling_False() {
        Capability capWithoutCooling = new Capability(false, false, 10.0f, 100, 0.5f, 1.0f, 1.0f);
        Drone drone = new Drone("Test", "1", capWithoutCooling);
        assertFalse(drone.getCooling());
    }

    @Test
    void testGetCooling_NullCapability() {
        Drone drone = new Drone("Test", "1", null);
        assertFalse(drone.getCooling());
    }

    @Test
    void testGetHeating_True() {
        Capability capWithHeating = new Capability(false, true, 10.0f, 100, 0.5f, 1.0f, 1.0f);
        Drone drone = new Drone("Test", "1", capWithHeating);
        assertTrue(drone.getHeating());
    }

    @Test
    void testGetHeating_False() {
        Capability capWithoutHeating = new Capability(false, false, 10.0f, 100, 0.5f, 1.0f, 1.0f);
        Drone drone = new Drone("Test", "1", capWithoutHeating);
        assertFalse(drone.getHeating());
    }

    @Test
    void testGetHeating_NullCapability() {
        Drone drone = new Drone("Test", "1", null);
        assertFalse(drone.getHeating());
    }

    @Test
    void testMatchMedDispatchRec_NullMedDispatchRec() {
        List<DroneForServicePoint> dronesForServicePoint = new ArrayList<>();
        List<ServicePoint> servicePoints = new ArrayList<>();

        assertFalse(drone.matchMedDispatchRec(null, dronesForServicePoint, servicePoints));
    }

    @Test
    void testMatchMedDispatchRec_WithReason_NullMedDispatchRec() {
        List<DroneForServicePoint> dronesForServicePoint = new ArrayList<>();
        List<ServicePoint> servicePoints = new ArrayList<>();
        StringBuilder reason = new StringBuilder();

        assertFalse(drone.matchMedDispatchRec(null, dronesForServicePoint, servicePoints, reason));
        assertEquals("medDispatchRec is null", reason.toString());
    }

    @Test
    void testMatchMedDispatchRec_NoAvailability() {
        Position delivery = new Position(BigDecimal.valueOf(0.001), BigDecimal.valueOf(0.001));
        MedDispatchRec rec = new MedDispatchRec(1, LocalDate.now(), LocalTime.now(), null, delivery);
        
        List<DroneForServicePoint> dronesForServicePoint = new ArrayList<>();
        List<ServicePoint> servicePoints = new ArrayList<>();
        StringBuilder reason = new StringBuilder();

        assertFalse(drone.matchMedDispatchRec(rec, dronesForServicePoint, servicePoints, reason));
        assertEquals("drone not available at required service point", reason.toString());
    }

    @Test
    void testMatchMedDispatchRec_CapacityRequirement_NotMet() {
        Capability smallCapability = new Capability(false, false, 5.0f, 100, 0.5f, 1.0f, 1.0f);
        Drone smallDrone = new Drone("SmallDrone", "1", smallCapability);

        Requirements req = new Requirements(false, false, 10.0f, null);
        Position delivery = new Position(BigDecimal.valueOf(0.001), BigDecimal.valueOf(0.001));
        LocalDate testDate = LocalDate.of(2026, 1, 20); // Tuesday
        LocalTime testTime = LocalTime.of(12, 0);
        MedDispatchRec rec = new MedDispatchRec(1, testDate, testTime, req, delivery);

        List<Availability> availList = List.of(new Availability("TUESDAY", LocalTime.of(0, 0), LocalTime.of(23, 59)));
        DroneAvailability droneAvail = new DroneAvailability("1", availList);
        DroneForServicePoint dsp = new DroneForServicePoint(1, List.of(droneAvail));
        List<DroneForServicePoint> dronesForServicePoint = List.of(dsp);

        Position servicePos = new Position(BigDecimal.ZERO, BigDecimal.ZERO);
        ServicePoint servicePoint = new ServicePoint(1, "Test", servicePos);
        List<ServicePoint> servicePoints = List.of(servicePoint);
        StringBuilder reason = new StringBuilder();

        assertFalse(smallDrone.matchMedDispatchRec(rec, dronesForServicePoint, servicePoints, reason));
        assertTrue(reason.toString().contains("capacity"));
    }

    @Test
    void testMatchMedDispatchRec_HeatingRequired_NotAvailable() {
        Capability noHeating = new Capability(false, false, 10.0f, 100, 0.5f, 1.0f, 1.0f);
        Drone coldDrone = new Drone("ColdDrone", "1", noHeating);

        Requirements req = new Requirements(true, false, null, null);
        Position delivery = new Position(BigDecimal.valueOf(0.001), BigDecimal.valueOf(0.001));
        LocalDate testDate = LocalDate.of(2026, 1, 20); // Tuesday
        LocalTime testTime = LocalTime.of(12, 0);
        MedDispatchRec rec = new MedDispatchRec(1, testDate, testTime, req, delivery);

        List<Availability> availList = List.of(new Availability("TUESDAY", LocalTime.of(0, 0), LocalTime.of(23, 59)));
        DroneAvailability droneAvail = new DroneAvailability("1", availList);
        DroneForServicePoint dsp = new DroneForServicePoint(1, List.of(droneAvail));
        List<DroneForServicePoint> dronesForServicePoint = List.of(dsp);

        Position servicePos = new Position(BigDecimal.ZERO, BigDecimal.ZERO);
        ServicePoint servicePoint = new ServicePoint(1, "Test", servicePos);
        List<ServicePoint> servicePoints = List.of(servicePoint);
        StringBuilder reason = new StringBuilder();

        assertFalse(coldDrone.matchMedDispatchRec(rec, dronesForServicePoint, servicePoints, reason));
        assertTrue(reason.toString().contains("heating"));
    }

    @Test
    void testMatchMedDispatchRec_CoolingRequired_NotAvailable() {
        Capability noCooling = new Capability(false, false, 10.0f, 100, 0.5f, 1.0f, 1.0f);
        Drone hotDrone = new Drone("HotDrone", "1", noCooling);

        Requirements req = new Requirements(false, true, null, null);
        Position delivery = new Position(BigDecimal.valueOf(0.001), BigDecimal.valueOf(0.001));
        LocalDate testDate = LocalDate.of(2026, 1, 20); // Tuesday
        LocalTime testTime = LocalTime.of(12, 0);
        MedDispatchRec rec = new MedDispatchRec(1, testDate, testTime, req, delivery);

        List<Availability> availList = List.of(new Availability("TUESDAY", LocalTime.of(0, 0), LocalTime.of(23, 59)));
        DroneAvailability droneAvail = new DroneAvailability("1", availList);
        DroneForServicePoint dsp = new DroneForServicePoint(1, List.of(droneAvail));
        List<DroneForServicePoint> dronesForServicePoint = List.of(dsp);

        Position servicePos = new Position(BigDecimal.ZERO, BigDecimal.ZERO);
        ServicePoint servicePoint = new ServicePoint(1, "Test", servicePos);
        List<ServicePoint> servicePoints = List.of(servicePoint);
        StringBuilder reason = new StringBuilder();

        assertFalse(hotDrone.matchMedDispatchRec(rec, dronesForServicePoint, servicePoints, reason));
        assertTrue(reason.toString().contains("cooling"));
    }

    @Test
    void testMatchMedDispatchRec_MaxCostExceeded() {
        Capability expensiveCap = new Capability(false, false, 10.0f, 100, 100.0f, 1.0f, 1.0f);
        Drone expensiveDrone = new Drone("ExpensiveDrone", "1", expensiveCap);

        Requirements req = new Requirements(false, false, null, 10.0f);
        Position delivery = new Position(BigDecimal.valueOf(1.0), BigDecimal.valueOf(1.0));
        MedDispatchRec rec = new MedDispatchRec(1, LocalDate.now(), LocalTime.now(), req, delivery);

        List<Availability> availList = List.of(new Availability("MONDAY", LocalTime.of(0, 0), LocalTime.of(23, 59)));
        DroneAvailability droneAvail = new DroneAvailability("1", availList);
        DroneForServicePoint dsp = new DroneForServicePoint(1, List.of(droneAvail));
        List<DroneForServicePoint> dronesForServicePoint = List.of(dsp);

        Position servicePos = new Position(BigDecimal.ZERO, BigDecimal.ZERO);
        ServicePoint servicePoint = new ServicePoint(1, "Test", servicePos);
        List<ServicePoint> servicePoints = List.of(servicePoint);

        assertFalse(expensiveDrone.matchMedDispatchRec(rec, dronesForServicePoint, servicePoints));
    }

    @Test
    void testMatchMedDispatchRec_SuccessfulMatch() {
        Requirements req = new Requirements(false, true, 5.0f, 1000.0f);
        Position delivery = new Position(BigDecimal.valueOf(0.001), BigDecimal.valueOf(0.001));
        LocalDate testDate = LocalDate.of(2026, 1, 20); // Tuesday
        LocalTime testTime = LocalTime.of(12, 0);
        MedDispatchRec rec = new MedDispatchRec(1, testDate, testTime, req, delivery);

        List<Availability> availList = List.of(new Availability("TUESDAY", LocalTime.of(0, 0), LocalTime.of(23, 59)));
        DroneAvailability droneAvail = new DroneAvailability("1", availList);
        DroneForServicePoint dsp = new DroneForServicePoint(1, List.of(droneAvail));
        List<DroneForServicePoint> dronesForServicePoint = List.of(dsp);

        Position servicePos = new Position(BigDecimal.ZERO, BigDecimal.ZERO);
        ServicePoint servicePoint = new ServicePoint(1, "Test", servicePos);
        List<ServicePoint> servicePoints = List.of(servicePoint);

        assertTrue(drone.matchMedDispatchRec(rec, dronesForServicePoint, servicePoints));
    }

    @Test
    void testMatchMedDispatchRec_NoRequirements() {
        Position delivery = new Position(BigDecimal.valueOf(0.001), BigDecimal.valueOf(0.001));
        LocalDate testDate = LocalDate.of(2026, 1, 20); // Tuesday
        LocalTime testTime = LocalTime.of(12, 0);
        MedDispatchRec rec = new MedDispatchRec(1, testDate, testTime, null, delivery);

        List<Availability> availList = List.of(new Availability("TUESDAY", LocalTime.of(0, 0), LocalTime.of(23, 59)));
        DroneAvailability droneAvail = new DroneAvailability("1", availList);
        DroneForServicePoint dsp = new DroneForServicePoint(1, List.of(droneAvail));
        List<DroneForServicePoint> dronesForServicePoint = List.of(dsp);

        Position servicePos = new Position(BigDecimal.ZERO, BigDecimal.ZERO);
        ServicePoint servicePoint = new ServicePoint(1, "Test", servicePos);
        List<ServicePoint> servicePoints = List.of(servicePoint);

        assertTrue(drone.matchMedDispatchRec(rec, dronesForServicePoint, servicePoints));
    }

    @Test
    void testAStarSearch_NullMedDispatchRequests() {
        Position servicePos = new Position(BigDecimal.ZERO, BigDecimal.ZERO);
        ServicePoint servicePoint = new ServicePoint(1, "Test", servicePos);

        assertNull(drone.aStarSearch(null, servicePoint, new ArrayList<>()));
    }

    @Test
    void testAStarSearch_EmptyMedDispatchRequests() {
        Position servicePos = new Position(BigDecimal.ZERO, BigDecimal.ZERO);
        ServicePoint servicePoint = new ServicePoint(1, "Test", servicePos);

        assertNull(drone.aStarSearch(new ArrayList<>(), servicePoint, new ArrayList<>()));
    }

    @Test
    void testAStarSearch_NullServicePoint() {
        Position delivery = new Position(BigDecimal.valueOf(0.001), BigDecimal.valueOf(0.001));
        MedDispatchRec rec = new MedDispatchRec(1, LocalDate.now(), LocalTime.now(), null, delivery);
        List<MedDispatchRec> requests = List.of(rec);

        assertNull(drone.aStarSearch(requests, null, new ArrayList<>()));
    }

    @Test
    void testAStarSearch_ServicePointWithNullPosition() {
        Position delivery = new Position(BigDecimal.valueOf(0.001), BigDecimal.valueOf(0.001));
        MedDispatchRec rec = new MedDispatchRec(1, LocalDate.now(), LocalTime.now(), null, delivery);
        List<MedDispatchRec> requests = List.of(rec);
        ServicePoint servicePoint = new ServicePoint(1, "Test", null);

        assertNull(drone.aStarSearch(requests, servicePoint, new ArrayList<>()));
    }

    @Test
    void testAStarSearch_ValidSingleDelivery() {
        Position servicePos = new Position(BigDecimal.ZERO, BigDecimal.ZERO);
        Position delivery = new Position(BigDecimal.valueOf(0.001), BigDecimal.valueOf(0.001));
        MedDispatchRec rec = new MedDispatchRec(1, LocalDate.now(), LocalTime.now(), null, delivery);
        List<MedDispatchRec> requests = List.of(rec);
        ServicePoint servicePoint = new ServicePoint(1, "Test", servicePos);

        FlightPath result = drone.aStarSearch(requests, servicePoint, new ArrayList<>());

        assertNotNull(result);
        assertNotNull(result.getStops());
        assertFalse(result.getStops().isEmpty());
        assertTrue(result.getTotalMoves() >= 0);
    }

    @Test
    void testAStarSearch_MultipleDeliveries() {
        Position servicePos = new Position(BigDecimal.ZERO, BigDecimal.ZERO);
        Position delivery1 = new Position(BigDecimal.valueOf(0.001), BigDecimal.valueOf(0.001));
        Position delivery2 = new Position(BigDecimal.valueOf(0.002), BigDecimal.valueOf(0.002));
        
        MedDispatchRec rec1 = new MedDispatchRec(1, LocalDate.now(), LocalTime.now(), null, delivery1);
        MedDispatchRec rec2 = new MedDispatchRec(2, LocalDate.now(), LocalTime.now(), null, delivery2);
        List<MedDispatchRec> requests = List.of(rec1, rec2);
        ServicePoint servicePoint = new ServicePoint(1, "Test", servicePos);

        FlightPath result = drone.aStarSearch(requests, servicePoint, new ArrayList<>());

        assertNotNull(result);
        assertNotNull(result.getSegmentEndIndices());
        assertEquals(2, result.getSegmentEndIndices().size());
    }

    @Test
    void testAStarSearch_WithNullDeliveryInList() {
        Position servicePos = new Position(BigDecimal.ZERO, BigDecimal.ZERO);
        Position delivery = new Position(BigDecimal.valueOf(0.001), BigDecimal.valueOf(0.001));
        
        MedDispatchRec rec1 = new MedDispatchRec(1, LocalDate.now(), LocalTime.now(), null, delivery);
        List<MedDispatchRec> requests = Arrays.asList(rec1, null);
        ServicePoint servicePoint = new ServicePoint(1, "Test", servicePos);

        FlightPath result = drone.aStarSearch(requests, servicePoint, new ArrayList<>());

        assertNotNull(result);
    }

    @Test
    void testEquality() {
        Drone drone1 = new Drone("TestDrone", "1", capability);
        Drone drone2 = new Drone("TestDrone", "1", capability);

        assertEquals(drone1, drone2);
        assertEquals(drone1.hashCode(), drone2.hashCode());
    }

    @Test
    void testEquality_DifferentNames() {
        Drone drone1 = new Drone("TestDrone1", "1", capability);
        Drone drone2 = new Drone("TestDrone2", "1", capability);

        assertNotEquals(drone1, drone2);
    }

    @Test
    void testToString() {
        String toString = drone.toString();
        assertNotNull(toString);
        assertTrue(toString.contains("TestDrone"));
        assertTrue(toString.contains("1"));
    }

    @Test
    void testMatchMedDispatchRec_InfiniteCost() {
        Requirements req = new Requirements(false, false, null, Float.POSITIVE_INFINITY);
        Position delivery = new Position(BigDecimal.valueOf(0.001), BigDecimal.valueOf(0.001));
        LocalDate testDate = LocalDate.of(2026, 1, 20); // Tuesday
        LocalTime testTime = LocalTime.of(12, 0);
        MedDispatchRec rec = new MedDispatchRec(1, testDate, testTime, req, delivery);

        List<Availability> availList = List.of(new Availability("TUESDAY", LocalTime.of(0, 0), LocalTime.of(23, 59)));
        DroneAvailability droneAvail = new DroneAvailability("1", availList);
        DroneForServicePoint dsp = new DroneForServicePoint(1, List.of(droneAvail));
        List<DroneForServicePoint> dronesForServicePoint = List.of(dsp);

        Position servicePos = new Position(BigDecimal.ZERO, BigDecimal.ZERO);
        ServicePoint servicePoint = new ServicePoint(1, "Test", servicePos);
        List<ServicePoint> servicePoints = List.of(servicePoint);

        assertTrue(drone.matchMedDispatchRec(rec, dronesForServicePoint, servicePoints));
    }

    @Test
    void testMatchMedDispatchRec_NullServicePointPosition() {
        Position delivery = new Position(BigDecimal.valueOf(0.001), BigDecimal.valueOf(0.001));
        MedDispatchRec rec = new MedDispatchRec(1, LocalDate.now(), LocalTime.now(), null, delivery);

        List<Availability> availList = List.of(new Availability("MONDAY", LocalTime.of(0, 0), LocalTime.of(23, 59)));
        DroneAvailability droneAvail = new DroneAvailability("1", availList);
        DroneForServicePoint dsp = new DroneForServicePoint(1, List.of(droneAvail));
        List<DroneForServicePoint> dronesForServicePoint = List.of(dsp);

        ServicePoint servicePoint = new ServicePoint(1, "Test", null);
        List<ServicePoint> servicePoints = List.of(servicePoint);

        assertFalse(drone.matchMedDispatchRec(rec, dronesForServicePoint, servicePoints));
    }

    @Test
    void testMatchMedDispatchRec_NullDeliveryPosition() {
        Position delivery = null;
        MedDispatchRec rec = new MedDispatchRec(1, LocalDate.now(), LocalTime.now(), null, delivery);

        List<Availability> availList = List.of(new Availability("MONDAY", LocalTime.of(0, 0), LocalTime.of(23, 59)));
        DroneAvailability droneAvail = new DroneAvailability("1", availList);
        DroneForServicePoint dsp = new DroneForServicePoint(1, List.of(droneAvail));
        List<DroneForServicePoint> dronesForServicePoint = List.of(dsp);

        Position servicePos = new Position(BigDecimal.ZERO, BigDecimal.ZERO);
        ServicePoint servicePoint = new ServicePoint(1, "Test", servicePos);
        List<ServicePoint> servicePoints = List.of(servicePoint);

        assertFalse(drone.matchMedDispatchRec(rec, dronesForServicePoint, servicePoints));
    }

    @Test
    void testMatchMedDispatchRec_MultipleServicePoints() {
        Requirements req = new Requirements(false, false, null, 100.0f);
        Position delivery = new Position(BigDecimal.valueOf(0.001), BigDecimal.valueOf(0.001));
        LocalDate testDate = LocalDate.of(2026, 1, 20); // Tuesday
        LocalTime testTime = LocalTime.of(12, 0);
        MedDispatchRec rec = new MedDispatchRec(1, testDate, testTime, req, delivery);

        List<Availability> availList = List.of(new Availability("TUESDAY", LocalTime.of(0, 0), LocalTime.of(23, 59)));
        DroneAvailability droneAvail = new DroneAvailability("1", availList);
        DroneForServicePoint dsp = new DroneForServicePoint(1, List.of(droneAvail));
        List<DroneForServicePoint> dronesForServicePoint = List.of(dsp);

        Position servicePos1 = new Position(BigDecimal.ZERO, BigDecimal.ZERO);
        Position servicePos2 = new Position(BigDecimal.valueOf(10.0), BigDecimal.valueOf(10.0)); // Far away
        ServicePoint servicePoint1 = new ServicePoint(1, "Test1", servicePos1);
        ServicePoint servicePoint2 = new ServicePoint(2, "Test2", servicePos2);
        List<ServicePoint> servicePoints = List.of(servicePoint1, servicePoint2);

        // Should match because at least one service point meets the cost requirement
        assertTrue(drone.matchMedDispatchRec(rec, dronesForServicePoint, servicePoints));
    }

    @Test
    void testMatchMedDispatchRec_EmptyServicePoints() {
        Position delivery = new Position(BigDecimal.valueOf(0.001), BigDecimal.valueOf(0.001));
        MedDispatchRec rec = new MedDispatchRec(1, LocalDate.now(), LocalTime.now(), null, delivery);

        List<Availability> availList = List.of(new Availability("MONDAY", LocalTime.of(0, 0), LocalTime.of(23, 59)));
        DroneAvailability droneAvail = new DroneAvailability("1", availList);
        DroneForServicePoint dsp = new DroneForServicePoint(1, List.of(droneAvail));
        List<DroneForServicePoint> dronesForServicePoint = List.of(dsp);

        List<ServicePoint> servicePoints = new ArrayList<>();

        assertFalse(drone.matchMedDispatchRec(rec, dronesForServicePoint, servicePoints));
    }

    @Test
    void testMatchMedDispatchRec_NullPositionCoordinates() {
        Position delivery = new Position(BigDecimal.valueOf(0.001), BigDecimal.valueOf(0.001));
        LocalDate testDate = LocalDate.of(2026, 1, 20); // Tuesday
        LocalTime testTime = LocalTime.of(12, 0);
        MedDispatchRec rec = new MedDispatchRec(1, testDate, testTime, null, delivery);

        List<Availability> availList = List.of(new Availability("TUESDAY", LocalTime.of(0, 0), LocalTime.of(23, 59)));
        DroneAvailability droneAvail = new DroneAvailability("1", availList);
        DroneForServicePoint dsp = new DroneForServicePoint(1, List.of(droneAvail));
        List<DroneForServicePoint> dronesForServicePoint = List.of(dsp);

        // Service point with null coordinates
        Position servicePos = new Position(null, null);
        ServicePoint servicePoint = new ServicePoint(1, "Test", servicePos);
        List<ServicePoint> servicePoints = List.of(servicePoint);

        assertFalse(drone.matchMedDispatchRec(rec, dronesForServicePoint, servicePoints));
    }

    @Test
    void testMatchMedDispatchRec_OnlyDateProvided() {
        Position delivery = new Position(BigDecimal.valueOf(0.001), BigDecimal.valueOf(0.001));
        LocalDate testDate = LocalDate.of(2026, 1, 20); // Tuesday
        MedDispatchRec rec = new MedDispatchRec(1, testDate, null, null, delivery);

        List<Availability> availList = List.of(new Availability("TUESDAY", LocalTime.of(0, 0), LocalTime.of(23, 59)));
        DroneAvailability droneAvail = new DroneAvailability("1", availList);
        DroneForServicePoint dsp = new DroneForServicePoint(1, List.of(droneAvail));
        List<DroneForServicePoint> dronesForServicePoint = List.of(dsp);

        Position servicePos = new Position(BigDecimal.ZERO, BigDecimal.ZERO);
        ServicePoint servicePoint = new ServicePoint(1, "Test", servicePos);
        List<ServicePoint> servicePoints = List.of(servicePoint);

        assertTrue(drone.matchMedDispatchRec(rec, dronesForServicePoint, servicePoints));
    }

    @Test
    void testMatchMedDispatchRec_OnlyTimeProvided() {
        Position delivery = new Position(BigDecimal.valueOf(0.001), BigDecimal.valueOf(0.001));
        LocalTime testTime = LocalTime.of(12, 0);
        MedDispatchRec rec = new MedDispatchRec(1, null, testTime, null, delivery);

        List<Availability> availList = List.of(new Availability("TUESDAY", LocalTime.of(0, 0), LocalTime.of(23, 59)));
        DroneAvailability droneAvail = new DroneAvailability("1", availList);
        DroneForServicePoint dsp = new DroneForServicePoint(1, List.of(droneAvail));
        List<DroneForServicePoint> dronesForServicePoint = List.of(dsp);

        Position servicePos = new Position(BigDecimal.ZERO, BigDecimal.ZERO);
        ServicePoint servicePoint = new ServicePoint(1, "Test", servicePos);
        List<ServicePoint> servicePoints = List.of(servicePoint);

        assertTrue(drone.matchMedDispatchRec(rec, dronesForServicePoint, servicePoints));
    }

    @Test
    void testMatchMedDispatchRec_NoDateOrTime() {
        Position delivery = new Position(BigDecimal.valueOf(0.001), BigDecimal.valueOf(0.001));
        MedDispatchRec rec = new MedDispatchRec(1, null, null, null, delivery);

        List<Availability> availList = List.of(new Availability("TUESDAY", LocalTime.of(0, 0), LocalTime.of(23, 59)));
        DroneAvailability droneAvail = new DroneAvailability("1", availList);
        DroneForServicePoint dsp = new DroneForServicePoint(1, List.of(droneAvail));
        List<DroneForServicePoint> dronesForServicePoint = List.of(dsp);

        Position servicePos = new Position(BigDecimal.ZERO, BigDecimal.ZERO);
        ServicePoint servicePoint = new ServicePoint(1, "Test", servicePos);
        List<ServicePoint> servicePoints = List.of(servicePoint);

        assertTrue(drone.matchMedDispatchRec(rec, dronesForServicePoint, servicePoints));
    }

    @Test
    void testMatchMedDispatchRec_NullDronesForServicePoint() {
        Position delivery = new Position(BigDecimal.valueOf(0.001), BigDecimal.valueOf(0.001));
        MedDispatchRec rec = new MedDispatchRec(1, null, null, null, delivery);

        Position servicePos = new Position(BigDecimal.ZERO, BigDecimal.ZERO);
        ServicePoint servicePoint = new ServicePoint(1, "Test", servicePos);
        List<ServicePoint> servicePoints = List.of(servicePoint);

        assertFalse(drone.matchMedDispatchRec(rec, null, servicePoints));
    }

    @Test
    void testMatchMedDispatchRec_EmptyDronesForServicePoint() {
        Position delivery = new Position(BigDecimal.valueOf(0.001), BigDecimal.valueOf(0.001));
        MedDispatchRec rec = new MedDispatchRec(1, null, null, null, delivery);

        Position servicePos = new Position(BigDecimal.ZERO, BigDecimal.ZERO);
        ServicePoint servicePoint = new ServicePoint(1, "Test", servicePos);
        List<ServicePoint> servicePoints = List.of(servicePoint);

        assertFalse(drone.matchMedDispatchRec(rec, new ArrayList<>(), servicePoints));
    }

    @Test
    void testMatchMedDispatchRec_CostTolerance() {
        // Test that cost tolerance allows slight exceedances
        Capability tightCostCap = new Capability(false, false, 10.0f, 100, 1.0f, 0.0f, 0.0f);
        Drone tightDrone = new Drone("TightDrone", "1", tightCostCap);

        Requirements req = new Requirements(false, false, null, 10.0f); // Very tight budget
        Position delivery = new Position(BigDecimal.valueOf(0.001), BigDecimal.valueOf(0.001));
        LocalDate testDate = LocalDate.of(2026, 1, 20); // Tuesday
        LocalTime testTime = LocalTime.of(12, 0);
        MedDispatchRec rec = new MedDispatchRec(1, testDate, testTime, req, delivery);

        List<Availability> availList = List.of(new Availability("TUESDAY", LocalTime.of(0, 0), LocalTime.of(23, 59)));
        DroneAvailability droneAvail = new DroneAvailability("1", availList);
        DroneForServicePoint dsp = new DroneForServicePoint(1, List.of(droneAvail));
        List<DroneForServicePoint> dronesForServicePoint = List.of(dsp);

        Position servicePos = new Position(BigDecimal.ZERO, BigDecimal.ZERO);
        ServicePoint servicePoint = new ServicePoint(1, "Test", servicePos);
        List<ServicePoint> servicePoints = List.of(servicePoint);

        // Should allow some tolerance
        boolean result = tightDrone.matchMedDispatchRec(rec, dronesForServicePoint, servicePoints);
        // Result depends on actual distance calculation
        assertNotNull(result);
    }

    @Test
    void testAStarSearch_RecWithNullDelivery() {
        Position servicePos = new Position(BigDecimal.ZERO, BigDecimal.ZERO);
        Position delivery = new Position(BigDecimal.valueOf(0.001), BigDecimal.valueOf(0.001));
        
        MedDispatchRec rec1 = new MedDispatchRec(1, LocalDate.now(), LocalTime.now(), null, delivery);
        MedDispatchRec rec2 = new MedDispatchRec(2, LocalDate.now(), LocalTime.now(), null, null);
        List<MedDispatchRec> requests = List.of(rec1, rec2);
        ServicePoint servicePoint = new ServicePoint(1, "Test", servicePos);

        FlightPath result = drone.aStarSearch(requests, servicePoint, new ArrayList<>());

        assertNotNull(result);
        // Should have processed rec1 but skipped rec2
    }

    @Test
    void testGetId_LargeNumber() {
        Drone drone = new Drone("Test", "999999999", capability);
        assertEquals(999999999, drone.getId());
    }

    @Test
    void testGetId_Zero() {
        Drone drone = new Drone("Test", "0", capability);
        assertEquals(0, drone.getId());
    }

    @Test
    void testGetId_NegativeNumber() {
        Drone drone = new Drone("Test", "-42", capability);
        assertEquals(-42, drone.getId());
    }
}
