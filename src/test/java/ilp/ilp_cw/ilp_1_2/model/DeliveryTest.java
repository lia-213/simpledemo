package ilp.ilp_cw.ilp_1_2.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DeliveryTest {

    @Test
    void testConstructor_WithValidParameters() {
        FlightPath flightPath = new FlightPath();
        Delivery delivery = new Delivery(1, flightPath);

        assertEquals(1, delivery.getDeliveryId());
        assertSame(flightPath, delivery.getFlightPath());
    }

    @Test
    void testConstructor_WithNullFlightPath() {
        Delivery delivery = new Delivery(1, null);

        assertEquals(1, delivery.getDeliveryId());
        assertNull(delivery.getFlightPath());
    }

    @Test
    void testConstructor_WithNullDeliveryId() {
        FlightPath flightPath = new FlightPath();
        Delivery delivery = new Delivery(null, flightPath);

        assertNull(delivery.getDeliveryId());
        assertSame(flightPath, delivery.getFlightPath());
    }

    @Test
    void testConstructor_WithBothNull() {
        Delivery delivery = new Delivery(null, null);

        assertNull(delivery.getDeliveryId());
        assertNull(delivery.getFlightPath());
    }

    @Test
    void testGetDeliveryId() {
        Delivery delivery = new Delivery(42, new FlightPath());

        assertEquals(42, delivery.getDeliveryId());
    }

    @Test
    void testGetFlightPath() {
        FlightPath flightPath = new FlightPath();
        Delivery delivery = new Delivery(1, flightPath);

        assertSame(flightPath, delivery.getFlightPath());
    }

    @Test
    void testSetFlightPath() {
        FlightPath originalPath = new FlightPath();
        Delivery delivery = new Delivery(1, originalPath);

        FlightPath newPath = new FlightPath();
        delivery.setFlightPath(newPath);

        assertSame(newPath, delivery.getFlightPath());
        assertNotSame(originalPath, delivery.getFlightPath());
    }

    @Test
    void testSetFlightPath_ToNull() {
        FlightPath flightPath = new FlightPath();
        Delivery delivery = new Delivery(1, flightPath);

        delivery.setFlightPath(null);

        assertNull(delivery.getFlightPath());
    }

    @Test
    void testSetFlightPath_FromNull() {
        Delivery delivery = new Delivery(1, null);

        FlightPath newPath = new FlightPath();
        delivery.setFlightPath(newPath);

        assertSame(newPath, delivery.getFlightPath());
    }

    @Test
    void testDeliveryId_IsImmutable() {
        FlightPath flightPath = new FlightPath();
        Delivery delivery = new Delivery(100, flightPath);

        assertEquals(100, delivery.getDeliveryId());
        // Verify deliveryId is final by checking it cannot be changed
        // (This is enforced at compile-time due to the final modifier)
    }

    @Test
    void testDeliveryId_WithZero() {
        Delivery delivery = new Delivery(0, new FlightPath());

        assertEquals(0, delivery.getDeliveryId());
    }

    @Test
    void testDeliveryId_WithNegativeValue() {
        Delivery delivery = new Delivery(-1, new FlightPath());

        assertEquals(-1, delivery.getDeliveryId());
    }

    @Test
    void testDeliveryId_WithLargeValue() {
        Delivery delivery = new Delivery(Integer.MAX_VALUE, new FlightPath());

        assertEquals(Integer.MAX_VALUE, delivery.getDeliveryId());
    }

    @Test
    void testFlightPath_WithStops() {
        FlightPath flightPath = new FlightPath();
        Position pos1 = new Position(BigDecimal.ZERO, BigDecimal.ZERO);
        Position pos2 = new Position(BigDecimal.ONE, BigDecimal.ONE);
        flightPath.getStops().add(pos1);
        flightPath.getStops().add(pos2);

        Delivery delivery = new Delivery(1, flightPath);

        assertEquals(2, delivery.getFlightPath().getStops().size());
        assertTrue(delivery.getFlightPath().getStops().contains(pos1));
        assertTrue(delivery.getFlightPath().getStops().contains(pos2));
    }

    @Test
    void testFlightPath_ModifyAfterConstruction() {
        FlightPath flightPath = new FlightPath();
        Delivery delivery = new Delivery(1, flightPath);

        Position newPosition = new Position(BigDecimal.valueOf(2.0), BigDecimal.valueOf(2.0));
        delivery.getFlightPath().getStops().add(newPosition);

        assertTrue(delivery.getFlightPath().getStops().contains(newPosition));
    }

    @Test
    void testEquality_SameValues() {
        FlightPath flightPath = new FlightPath();
        Delivery delivery1 = new Delivery(1, flightPath);
        Delivery delivery2 = new Delivery(1, flightPath);

        assertEquals(delivery1, delivery2);
        assertEquals(delivery1.hashCode(), delivery2.hashCode());
    }

    @Test
    void testEquality_DifferentDeliveryIds() {
        FlightPath flightPath = new FlightPath();
        Delivery delivery1 = new Delivery(1, flightPath);
        Delivery delivery2 = new Delivery(2, flightPath);

        assertNotEquals(delivery1, delivery2);
    }

    @Test
    void testEquality_DifferentFlightPaths() {
        Delivery delivery1 = new Delivery(1, new FlightPath());
        Delivery delivery2 = new Delivery(1, new FlightPath());

        // Different FlightPath instances with same content are equal
        assertEquals(delivery1, delivery2);
    }

    @Test
    void testEquality_SameReference() {
        FlightPath flightPath = new FlightPath();
        Delivery delivery = new Delivery(1, flightPath);

        assertEquals(delivery, delivery);
        assertEquals(delivery.hashCode(), delivery.hashCode());
    }

    @Test
    void testToString() {
        FlightPath flightPath = new FlightPath();
        Delivery delivery = new Delivery(123, flightPath);

        String toString = delivery.toString();
        assertNotNull(toString);
        assertTrue(toString.contains("123"));
    }

    @Test
    void testToString_WithNullValues() {
        Delivery delivery = new Delivery(null, null);

        String toString = delivery.toString();
        assertNotNull(toString);
        assertTrue(toString.contains("null"));
    }

    @Test
    void testFlightPath_WithTotalMoves() {
        FlightPath flightPath = new FlightPath();
        flightPath.setTotalMoves(10);
        Delivery delivery = new Delivery(1, flightPath);

        assertEquals(10, delivery.getFlightPath().getTotalMoves());
    }

    @Test
    void testFlightPath_WithTotalCost() {
        FlightPath flightPath = new FlightPath();
        flightPath.setTotalCost(250.5f);
        Delivery delivery = new Delivery(1, flightPath);

        assertEquals(250.5f, delivery.getFlightPath().getTotalCost());
    }

    @Test
    void testFlightPath_WithSegmentEndIndices() {
        FlightPath flightPath = new FlightPath();
        flightPath.getSegmentEndIndices().add(5);
        flightPath.getSegmentEndIndices().add(10);
        Delivery delivery = new Delivery(1, flightPath);

        List<Integer> indices = delivery.getFlightPath().getSegmentEndIndices();
        assertEquals(2, indices.size());
        assertEquals(5, indices.get(0));
        assertEquals(10, indices.get(1));
    }

    @Test
    void testMultipleDeliveries_SeparateInstances() {
        FlightPath path1 = new FlightPath();
        FlightPath path2 = new FlightPath();
        
        Delivery delivery1 = new Delivery(1, path1);
        Delivery delivery2 = new Delivery(2, path2);

        assertNotSame(delivery1, delivery2);
        assertNotSame(delivery1.getFlightPath(), delivery2.getFlightPath());
    }

    @Test
    void testDeliveryId_BoundaryValues() {
        Delivery minDelivery = new Delivery(Integer.MIN_VALUE, new FlightPath());
        Delivery maxDelivery = new Delivery(Integer.MAX_VALUE, new FlightPath());

        assertEquals(Integer.MIN_VALUE, minDelivery.getDeliveryId());
        assertEquals(Integer.MAX_VALUE, maxDelivery.getDeliveryId());
    }
}
