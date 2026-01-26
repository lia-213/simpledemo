package ilp.tutorials.ilp_cw1.controllers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import ilp.ilp_cw.ilp_1_2.controllers.DroneController;
import ilp.ilp_cw.ilp_1_2.dto.*;
import ilp.ilp_cw.ilp_1_2.model.Position;
import ilp.ilp_cw.ilp_1_2.model.Region;
import ilp.ilp_cw.ilp_1_2.service.DroneService;

import java.math.BigDecimal;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
/*
  Unit tests for {@link DroneController} using Mockito to mock the {@link DroneService}.
  <p>
  Verifies controller mapping and response handling for distance, closeness, next-position
  and region endpoints under various valid and invalid input scenarios.
 */
class DroneControllerTest {

    @Mock
    private DroneService droneService;

    @InjectMocks
    private DroneController bc;

    private Position pos(double lng, double lat) {
        return new Position(BigDecimal.valueOf(lng), BigDecimal.valueOf(lat));
    }

    private Region createSquareRegion() {
        return new Region("square region", Arrays.asList(
                pos(0, 0),
                pos(0, 1),
                pos(1, 1),
                pos(1, 0)
        ));
    }

    private Region createTriangleRegion() {
        return new Region("triangle region", Arrays.asList(
                pos(-10, -10),
                pos(10, -10),
                pos(0, 10)
        ));
    }

    private Region createComplexRegion() {
        return new Region("complex region", Arrays.asList(
                pos(-70, -20),
                pos(-40, 60),
                pos(0, 90),
                pos(50, 50),
                pos(80, -10),
                pos(30, -80),
                pos(-30, -60)
        ));
    }

    @BeforeEach
    void setup() {
        lenient().when(droneService.getUID()).thenReturn("s2141930");
    }

    /* ========== BASIC TESTS ========== */

    @Test
    void uid_returnsMockedUid() {
        assertEquals("s2141930", bc.uid());
    }

    /* ========== DISTANCE TESTS ========== */

    @Test
    void distanceTo_validPositions_returnsCorrectDistance() {
        PositionsDto positionsDto = new PositionsDto(new Position(BigDecimal.valueOf(-3.192473), BigDecimal.valueOf(55.946233)), new Position(BigDecimal.valueOf(-3.192473), BigDecimal.valueOf(55.942617)));

        when(droneService.distanceTo(any())).thenReturn(
                new ResponseEntity<>(new BigDecimal("0.003616"), HttpStatus.OK)
        );

        ResponseEntity<BigDecimal> response = bc.distanceTo(positionsDto);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(new BigDecimal("0.003616"), response.getBody());
    }

    @Test
    void distanceTo_invalidJson_returnsBadRequest() {
        PositionsDto positionsDto = new PositionsDto(new Position(BigDecimal.valueOf(55.946233), null), new Position(BigDecimal.valueOf(-3.192473), BigDecimal.valueOf(55.942617)));

        when(droneService.distanceTo(any())).thenThrow(new IllegalArgumentException("Invalid JSON"));

        ResponseEntity<BigDecimal> response = bc.distanceTo(positionsDto);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void distanceTo_missingPosition_returnsBadRequest() {
        PositionsDto positionsDto = new PositionsDto(null, new Position(BigDecimal.valueOf(-3.192473), BigDecimal.valueOf(55.942617)));

        when(droneService.distanceTo(any())).thenReturn(new ResponseEntity<>(null, HttpStatus.BAD_REQUEST));

        ResponseEntity<BigDecimal> response = bc.distanceTo(positionsDto);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    /* ========== IS CLOSE TO TESTS ========== */

    @Test
    void isCloseTo_positionsClose_returnsTrue() {
        PositionsDto positionsDto = new PositionsDto(
                new Position(BigDecimal.valueOf(-3.192473), BigDecimal.valueOf(55.946233)),
                new Position(BigDecimal.valueOf(-3.192473), BigDecimal.valueOf(55.946300)));

        when(droneService.isCloseTo(any())).thenReturn(new ResponseEntity<>(true, HttpStatus.OK));

        ResponseEntity<Boolean> response = bc.isCloseTo(positionsDto);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody());
    }

    @Test
    void isCloseTo_positionsFar_returnsFalse() {
        PositionsDto positionsDto = new PositionsDto(
                new Position(BigDecimal.valueOf(-3.192473), BigDecimal.valueOf(55.946233)),
                new Position(BigDecimal.valueOf(-3.192473), BigDecimal.valueOf(55.942617)));

        when(droneService.isCloseTo(any())).thenReturn(new ResponseEntity<>(false, HttpStatus.OK));

        ResponseEntity<Boolean> response = bc.isCloseTo(positionsDto);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody());
    }

    @Test
    void isCloseTo_invalidJson_returnsBadRequest() {
        PositionsDto positionsDto = new PositionsDto(new Position(BigDecimal.valueOf(55.946233), BigDecimal.valueOf(-3.192473)), new Position(null, null));

        when(droneService.isCloseTo(any())).thenThrow(new IllegalArgumentException("Malformed JSON"));

        ResponseEntity<Boolean> response = bc.isCloseTo(positionsDto);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void isCloseTo_missingPosition_returnsBadRequest() {
        PositionsDto positionsDto = new PositionsDto(new Position(BigDecimal.valueOf(-3.192473), BigDecimal.valueOf(55.946233)), null);

        when(droneService.isCloseTo(any())).thenReturn(new ResponseEntity<>(null, HttpStatus.BAD_REQUEST));

        ResponseEntity<Boolean> response = bc.isCloseTo(positionsDto);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    /* ========== NEXT POSITION TESTS ========== */

    @Test
    void nextPosition_validInput_returnsNextPosition() {
        Position start = pos(-3.192473, 55.946233);
        DroneMoveRqstDto req = new DroneMoveRqstDto(start, BigDecimal.valueOf(45));
        Position next = pos(-3.192323, 55.946338);

        when(droneService.computeNextPosition(any()))
                .thenReturn(new ResponseEntity<>(next, HttpStatus.OK));

        ResponseEntity<Position> response = bc.nextPosition(req);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(next, response.getBody());
    }

    @Test
    void nextPosition_invalidAngle_returnsBadRequest() {
        DroneMoveRqstDto req = new DroneMoveRqstDto(pos(-3.192473, 55.946233), new BigDecimal(400));

        when(droneService.computeNextPosition(any()))
                .thenReturn(new ResponseEntity<>(null, HttpStatus.BAD_REQUEST));

        ResponseEntity<Position> response = bc.nextPosition(req);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void nextPosition_outOfBoundsPosition_returnsBadRequest() {
        DroneMoveRqstDto req = new DroneMoveRqstDto(pos(-3.192473, 95), BigDecimal.valueOf(45));

        when(droneService.computeNextPosition(any()))
                .thenReturn(new ResponseEntity<>(null, HttpStatus.BAD_REQUEST));

        ResponseEntity<Position> response = bc.nextPosition(req);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    /* ========== IS IN REGION TESTS ========== */

    @Test
    void isInRegion_nullRequest_returnsBadRequest() {
        when(droneService.isInRegion(null))
                .thenReturn(new ResponseEntity<>(false, HttpStatus.BAD_REQUEST));

        ResponseEntity<Boolean> resp = bc.isInRegion(null);
        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
    }

    @Test
    void isInRegion_nullPosition_returnsBadRequest() {
        LocationRegionDto wrapper = new LocationRegionDto();
        wrapper.setRegion(createSquareRegion());
        when(droneService.isInRegion(any()))
                .thenReturn(new ResponseEntity<>(false, HttpStatus.BAD_REQUEST));

        ResponseEntity<Boolean> resp = bc.isInRegion(wrapper);
        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
    }

    @Test
    void isInRegion_nullRegion_returnsBadRequest() {
        LocationRegionDto wrapper = new LocationRegionDto();
        wrapper.setCurrPosition(pos(0.5, 0.5));

        when(droneService.isInRegion(any()))
                .thenReturn(new ResponseEntity<>(false, HttpStatus.BAD_REQUEST));

        ResponseEntity<Boolean> resp = bc.isInRegion(wrapper);
        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
    }

    @Test
    void isInRegion_invalidPosition_returnsBadRequest() {
        LocationRegionDto wrapper = new LocationRegionDto(pos(0.0, 100.0), createSquareRegion());
        when(droneService.isInRegion(any()))
                .thenReturn(new ResponseEntity<>(false, HttpStatus.BAD_REQUEST));

        ResponseEntity<Boolean> resp = bc.isInRegion(wrapper);
        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
    }

    @Test
    void isInRegion_invalidRegion_returnsBadRequest() {
        Region invalidRegion = new Region("invalid", Arrays.asList(pos(0, 0), pos(1, 1)));
        LocationRegionDto wrapper = new LocationRegionDto(pos(0.5, 0.5), invalidRegion);

        when(droneService.isInRegion(any()))
                .thenReturn(new ResponseEntity<>(false, HttpStatus.BAD_REQUEST));

        ResponseEntity<Boolean> resp = bc.isInRegion(wrapper);
        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
    }

    @Test
    void isInRegion_invalidUnclosedRegion_returnsBadRequest() {
        Region invalidRegion = new Region("invalid", Arrays.asList(pos(0, 0), pos(1, 1), pos(7, 2)));
        LocationRegionDto wrapper = new LocationRegionDto(pos(0.5, 0.5), invalidRegion);

        when(droneService.isInRegion(any()))
                .thenReturn(new ResponseEntity<>(false, HttpStatus.BAD_REQUEST));

        ResponseEntity<Boolean> resp = bc.isInRegion(wrapper);
        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
    }


    @Test
    void inInRegion_pointInside_triangle_returnsTrue() {
        LocationRegionDto wrapper = new LocationRegionDto(pos(0.5, 0.5), createSquareRegion());
        when(droneService.isInRegion(any())).thenReturn(new ResponseEntity<>(true, HttpStatus.OK));

        ResponseEntity<Boolean> resp = bc.isInRegion(wrapper);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertNotNull(resp.getBody());
        assertTrue(resp.getBody());
    }

    @Test
    void isInRegion_pointOutside_triangle_returnsFalse() {
        LocationRegionDto wrapper = new LocationRegionDto(pos(100, 100), createTriangleRegion());
        when(droneService.isInRegion(any())).thenReturn(new ResponseEntity<>(false, HttpStatus.OK));

        ResponseEntity<Boolean> resp = bc.isInRegion(wrapper);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertNotNull(resp.getBody());
        assertFalse(resp.getBody());
    }

    @Test
    void isInRegion_pointInside_square_returnsTrue() {
        LocationRegionDto wrapper = new LocationRegionDto(pos(0.5, 0.5), createSquareRegion());
        when(droneService.isInRegion(any())).thenReturn(new ResponseEntity<>(true, HttpStatus.OK));

        ResponseEntity<Boolean> resp = bc.isInRegion(wrapper);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertNotNull(resp.getBody());
        assertTrue(resp.getBody());
    }

    @Test
    void isInRegion_pointOutside_square_returnsFalse() {
        LocationRegionDto wrapper = new LocationRegionDto(pos(2.0, 2.0), createSquareRegion());
        when(droneService.isInRegion(any())).thenReturn(new ResponseEntity<>(false, HttpStatus.OK));

        ResponseEntity<Boolean> resp = bc.isInRegion(wrapper);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertNotNull(resp.getBody());
        assertFalse(resp.getBody());
    }

    @Test
    void isInRegion_pointOnBorder_square_returnsTrue() {
        LocationRegionDto wrapper = new LocationRegionDto(pos(0, 0.5), createSquareRegion());
        when(droneService.isInRegion(any())).thenReturn(new ResponseEntity<>(true, HttpStatus.OK));

        ResponseEntity<Boolean> resp = bc.isInRegion(wrapper);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertNotNull(resp.getBody());
        assertTrue(resp.getBody());
    }

    @Test
    void isInRegion_justInBorder_square_returnsTrue() {
        LocationRegionDto wrapper = new LocationRegionDto(pos(0.99999, 0.99999), createSquareRegion());
        when(droneService.isInRegion(any())).thenReturn(new ResponseEntity<>(true, HttpStatus.OK));

        ResponseEntity<Boolean> resp = bc.isInRegion(wrapper);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertNotNull(resp.getBody());
        assertTrue(resp.getBody());
    }

    @Test
    void isInRegion_justOutBorder_square_returnsFalse() {
        LocationRegionDto wrapper = new LocationRegionDto(pos(1.00016, 1), createSquareRegion());
        when(droneService.isInRegion(any())).thenReturn(new ResponseEntity<>(false, HttpStatus.OK));

        ResponseEntity<Boolean> resp = bc.isInRegion(wrapper);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertNotNull(resp.getBody());
        assertFalse(resp.getBody());
    }

    @Test
    void isInRegion_pointInside_complexPolygon_returnsTrue() {
        LocationRegionDto wrapper = new LocationRegionDto(pos(0, 0.5), createComplexRegion());
        when(droneService.isInRegion(any())).thenReturn(new ResponseEntity<>(true, HttpStatus.OK));

        ResponseEntity<Boolean> resp = bc.isInRegion(wrapper);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertNotNull(resp.getBody());
        assertTrue(resp.getBody());
    }

    @Test
    void isInRegion_pointOnBorder_complexPolygon_returnsTrue() {
        LocationRegionDto wrapper = new LocationRegionDto(pos(0, 0.5), createComplexRegion());
        when(droneService.isInRegion(any())).thenReturn(new ResponseEntity<>(true, HttpStatus.OK));

        ResponseEntity<Boolean> resp = bc.isInRegion(wrapper);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertNotNull(resp.getBody());
        assertTrue(resp.getBody());
    }

    @Test
    void isInRegion_justOutBorder_complexPolygon_returnsFalse1() {
        LocationRegionDto wrapper = new LocationRegionDto(pos(1.00014, 1), createComplexRegion());
        when(droneService.isInRegion(any())).thenReturn(new ResponseEntity<>(false, HttpStatus.OK));

        ResponseEntity<Boolean> resp = bc.isInRegion(wrapper);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertNotNull(resp.getBody());
        assertFalse(resp.getBody());
    }

    @Test
    void isInRegion_atVertex_complexPolygon_returnsTrue() {
        LocationRegionDto wrapper = new LocationRegionDto(pos(-40, 60), createComplexRegion());
        when(droneService.isInRegion(any())).thenReturn(new ResponseEntity<>(true, HttpStatus.OK));

        ResponseEntity<Boolean> resp = bc.isInRegion(wrapper);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertNotNull(resp.getBody());
        assertTrue(resp.getBody());
    }

}
