package ilp.ilp_cw.ilp_1_2.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DronePathTest {

    @Test
    void testConstructor_WithValidParameters() {
        List<Delivery> deliveries = new ArrayList<>();
        DronePath dronePath = new DronePath(1, deliveries);

        assertEquals(1, dronePath.getDroneId());
        assertSame(deliveries, dronePath.getDeliveries());
    }

    @Test
    void testConstructor_WithNullDeliveries() {
        DronePath dronePath = new DronePath(1, null);

        assertEquals(1, dronePath.getDroneId());
        assertNull(dronePath.getDeliveries());
    }

    @Test
    void testConstructor_WithNullDroneId() {
        List<Delivery> deliveries = new ArrayList<>();
        DronePath dronePath = new DronePath(null, deliveries);

        assertNull(dronePath.getDroneId());
        assertSame(deliveries, dronePath.getDeliveries());
    }

    @Test
    void testConstructor_WithBothNull() {
        DronePath dronePath = new DronePath(null, null);

        assertNull(dronePath.getDroneId());
        assertNull(dronePath.getDeliveries());
    }

    @Test
    void testGetDroneId() {
        DronePath dronePath = new DronePath(42, new ArrayList<>());

        assertEquals(42, dronePath.getDroneId());
    }

    @Test
    void testGetDeliveries() {
        List<Delivery> deliveries = new ArrayList<>();
        DronePath dronePath = new DronePath(1, deliveries);

        assertSame(deliveries, dronePath.getDeliveries());
    }

    @Test
    void testSetDeliveries() {
        List<Delivery> originalDeliveries = new ArrayList<>();
        DronePath dronePath = new DronePath(1, originalDeliveries);

        List<Delivery> newDeliveries = new ArrayList<>();
        dronePath.setDeliveries(newDeliveries);

        assertSame(newDeliveries, dronePath.getDeliveries());
        assertNotSame(originalDeliveries, dronePath.getDeliveries());
    }

    @Test
    void testSetDeliveries_ToNull() {
        List<Delivery> deliveries = new ArrayList<>();
        DronePath dronePath = new DronePath(1, deliveries);

        dronePath.setDeliveries(null);

        assertNull(dronePath.getDeliveries());
    }

    @Test
    void testSetDeliveries_FromNull() {
        DronePath dronePath = new DronePath(1, null);

        List<Delivery> newDeliveries = new ArrayList<>();
        dronePath.setDeliveries(newDeliveries);

        assertSame(newDeliveries, dronePath.getDeliveries());
    }

    @Test
    void testDroneId_IsImmutable() {
        DronePath dronePath = new DronePath(100, new ArrayList<>());

        assertEquals(100, dronePath.getDroneId());
        // Verify droneId is final by checking it cannot be changed
        // (This is enforced at compile-time due to the final modifier)
    }

    @Test
    void testDroneId_WithZero() {
        DronePath dronePath = new DronePath(0, new ArrayList<>());

        assertEquals(0, dronePath.getDroneId());
    }

    @Test
    void testDroneId_WithNegativeValue() {
        DronePath dronePath = new DronePath(-1, new ArrayList<>());

        assertEquals(-1, dronePath.getDroneId());
    }

    @Test
    void testDroneId_WithLargeValue() {
        DronePath dronePath = new DronePath(Integer.MAX_VALUE, new ArrayList<>());

        assertEquals(Integer.MAX_VALUE, dronePath.getDroneId());
    }

    @Test
    void testDeliveries_EmptyList() {
        List<Delivery> deliveries = new ArrayList<>();
        DronePath dronePath = new DronePath(1, deliveries);

        assertTrue(dronePath.getDeliveries().isEmpty());
        assertEquals(0, dronePath.getDeliveries().size());
    }

    @Test
    void testDeliveries_SingleDelivery() {
        Delivery delivery = new Delivery(1, new FlightPath());
        List<Delivery> deliveries = new ArrayList<>();
        deliveries.add(delivery);

        DronePath dronePath = new DronePath(1, deliveries);

        assertEquals(1, dronePath.getDeliveries().size());
        assertTrue(dronePath.getDeliveries().contains(delivery));
    }

    @Test
    void testDeliveries_MultipleDeliveries() {
        Delivery delivery1 = new Delivery(1, new FlightPath());
        Delivery delivery2 = new Delivery(2, new FlightPath());
        Delivery delivery3 = new Delivery(3, new FlightPath());
        
        List<Delivery> deliveries = new ArrayList<>(Arrays.asList(delivery1, delivery2, delivery3));
        DronePath dronePath = new DronePath(1, deliveries);

        assertEquals(3, dronePath.getDeliveries().size());
        assertTrue(dronePath.getDeliveries().contains(delivery1));
        assertTrue(dronePath.getDeliveries().contains(delivery2));
        assertTrue(dronePath.getDeliveries().contains(delivery3));
    }

    @Test
    void testDeliveries_AddAfterConstruction() {
        List<Delivery> deliveries = new ArrayList<>();
        DronePath dronePath = new DronePath(1, deliveries);

        Delivery newDelivery = new Delivery(1, new FlightPath());
        dronePath.getDeliveries().add(newDelivery);

        assertEquals(1, dronePath.getDeliveries().size());
        assertTrue(dronePath.getDeliveries().contains(newDelivery));
    }

    @Test
    void testDeliveries_RemoveAfterConstruction() {
        Delivery delivery1 = new Delivery(1, new FlightPath());
        Delivery delivery2 = new Delivery(2, new FlightPath());
        List<Delivery> deliveries = new ArrayList<>(Arrays.asList(delivery1, delivery2));
        DronePath dronePath = new DronePath(1, deliveries);

        dronePath.getDeliveries().remove(delivery1);

        assertEquals(1, dronePath.getDeliveries().size());
        assertFalse(dronePath.getDeliveries().contains(delivery1));
        assertTrue(dronePath.getDeliveries().contains(delivery2));
    }

    @Test
    void testDeliveries_ClearAfterConstruction() {
        Delivery delivery = new Delivery(1, new FlightPath());
        List<Delivery> deliveries = new ArrayList<>(List.of(delivery));
        DronePath dronePath = new DronePath(1, deliveries);

        dronePath.getDeliveries().clear();

        assertTrue(dronePath.getDeliveries().isEmpty());
    }

    @Test
    void testEquality_SameValues() {
        List<Delivery> deliveries = new ArrayList<>();
        DronePath dronePath1 = new DronePath(1, deliveries);
        DronePath dronePath2 = new DronePath(1, deliveries);

        assertEquals(dronePath1, dronePath2);
        assertEquals(dronePath1.hashCode(), dronePath2.hashCode());
    }

    @Test
    void testEquality_DifferentDroneIds() {
        List<Delivery> deliveries = new ArrayList<>();
        DronePath dronePath1 = new DronePath(1, deliveries);
        DronePath dronePath2 = new DronePath(2, deliveries);

        assertNotEquals(dronePath1, dronePath2);
    }

    @Test
    void testEquality_DifferentDeliveryLists() {
        DronePath dronePath1 = new DronePath(1, new ArrayList<>());
        DronePath dronePath2 = new DronePath(1, new ArrayList<>());

        // Different ArrayList instances with same content are equal
        assertEquals(dronePath1, dronePath2);
    }

    @Test
    void testEquality_SameReference() {
        List<Delivery> deliveries = new ArrayList<>();
        DronePath dronePath = new DronePath(1, deliveries);

        assertEquals(dronePath, dronePath);
        assertEquals(dronePath.hashCode(), dronePath.hashCode());
    }

    @Test
    void testToString() {
        List<Delivery> deliveries = new ArrayList<>();
        DronePath dronePath = new DronePath(123, deliveries);

        String toString = dronePath.toString();
        assertNotNull(toString);
        assertTrue(toString.contains("123"));
    }

    @Test
    void testToString_WithNullValues() {
        DronePath dronePath = new DronePath(null, null);

        String toString = dronePath.toString();
        assertNotNull(toString);
        assertTrue(toString.contains("null"));
    }

    @Test
    void testDeliveries_WithComplexFlightPaths() {
        FlightPath path1 = new FlightPath();
        path1.getStops().add(new Position(BigDecimal.ZERO, BigDecimal.ZERO));
        path1.getStops().add(new Position(BigDecimal.ONE, BigDecimal.ONE));
        path1.setTotalMoves(5);

        FlightPath path2 = new FlightPath();
        path2.getStops().add(new Position(BigDecimal.valueOf(2.0), BigDecimal.valueOf(2.0)));
        path2.setTotalCost(100.5f);

        Delivery delivery1 = new Delivery(1, path1);
        Delivery delivery2 = new Delivery(2, path2);

        List<Delivery> deliveries = new ArrayList<>(Arrays.asList(delivery1, delivery2));
        DronePath dronePath = new DronePath(1, deliveries);

        assertEquals(2, dronePath.getDeliveries().size());
        assertEquals(5, dronePath.getDeliveries().get(0).getFlightPath().getTotalMoves());
        assertEquals(100.5f, dronePath.getDeliveries().get(1).getFlightPath().getTotalCost());
    }

    @Test
    void testMultipleDronePaths_SeparateInstances() {
        List<Delivery> deliveries1 = new ArrayList<>();
        List<Delivery> deliveries2 = new ArrayList<>();
        
        DronePath dronePath1 = new DronePath(1, deliveries1);
        DronePath dronePath2 = new DronePath(2, deliveries2);

        assertNotSame(dronePath1, dronePath2);
        assertNotSame(dronePath1.getDeliveries(), dronePath2.getDeliveries());
    }

    @Test
    void testDroneId_BoundaryValues() {
        DronePath minDronePath = new DronePath(Integer.MIN_VALUE, new ArrayList<>());
        DronePath maxDronePath = new DronePath(Integer.MAX_VALUE, new ArrayList<>());

        assertEquals(Integer.MIN_VALUE, minDronePath.getDroneId());
        assertEquals(Integer.MAX_VALUE, maxDronePath.getDroneId());
    }

    @Test
    void testDeliveries_LargeNumberOfDeliveries() {
        List<Delivery> deliveries = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            deliveries.add(new Delivery(i, new FlightPath()));
        }

        DronePath dronePath = new DronePath(1, deliveries);

        assertEquals(1000, dronePath.getDeliveries().size());
    }

    @Test
    void testDeliveries_OrderPreserved() {
        Delivery delivery1 = new Delivery(1, new FlightPath());
        Delivery delivery2 = new Delivery(2, new FlightPath());
        Delivery delivery3 = new Delivery(3, new FlightPath());
        
        List<Delivery> deliveries = new ArrayList<>(Arrays.asList(delivery1, delivery2, delivery3));
        DronePath dronePath = new DronePath(1, deliveries);

        assertEquals(delivery1, dronePath.getDeliveries().get(0));
        assertEquals(delivery2, dronePath.getDeliveries().get(1));
        assertEquals(delivery3, dronePath.getDeliveries().get(2));
    }
}
