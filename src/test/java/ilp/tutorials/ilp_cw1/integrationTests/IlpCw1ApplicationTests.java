package ilp.tutorials.ilp_cw1.integrationTests;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;

import static org.mockito.Mockito.when;
import java.util.Arrays;
import java.util.List;
import java.time.LocalTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ilp.ilp_cw.ilp_1_2.ilpCw1Application;
import ilp.ilp_cw.ilp_1_2.service.DroneIlpClient;
import ilp.ilp_cw.ilp_1_2.model.*;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for the application.
 * This class tests the integration of various components in the application.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = ilpCw1Application.class
)
@AutoConfigureMockMvc
public class IlpCw1ApplicationTests {
    @Autowired
    MockMvc mvc;

    @MockBean
    private DroneIlpClient droneIlpClient;

    /**
     * Tests the /api/v1/uid endpoint to ensure it returns the correct UID.
     */
    @Test
    void testMockUid() {
        try {
            mvc.perform(get("/api/v1/uid"))
                    .andExpect(status().isOk())
                    .andExpect(content().string("s2141930"));
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

        @BeforeEach
        void setupIlpClient() {
        // Build deterministic drone list matching test expectations
        List<Drone> drones = Arrays.asList(
                // capacity=8 expected list order: [2,4,7,9]
            new Drone("Drone 2", "2", new Capability(false, false, 8f, 1000, 0.03f, 1.0f, 2.0f)),
            new Drone("Drone 4", "4", new Capability(false, false, 8f, 900, 0.03f, 1.0f, 2.0f)),
            new Drone("Drone 7", "7", new Capability(false, false, 8f, 1000, 0.02f, 1.0f, 2.0f)),
            new Drone("Drone 9", "9", new Capability(true, false, 8f, 1100, 0.04f, 1.0f, 2.0f)),
                // maxMoves=1500 expected order: [5,10,99998,888,456]
            new Drone("Drone 5", "5", new Capability(true, false, 12f, 1500, 0.05f, 1.4f, 3.5f)),
            new Drone("Drone 10", "10", new Capability(false, false, 10f, 1500, 0.07f, 1.2f, 3.5f)),
            new Drone("Drone 99998", "99998", new Capability(false, false, 12f, 1500, 0.07f, 1.4f, 3.5f)),
            new Drone("Drone 888", "888", new Capability(true, false, 9f, 1500, 0.07f, 1.4f, 3.5f)),
            new Drone("Drone 456", "456", new Capability(false, false, 6f, 1500, 0.07f, 1.0f, 3.5f)),
                // remaining drones (ensures POST query result ["1","6"] still possible)
            new Drone("Drone 1", "1", new Capability(false, false, 4f, 1200, 0.02f, 1.0f, 2.0f)),
            new Drone("Drone 3", "3", new Capability(false, false, 6f, 1000, 0.05f, 1.0f, 2.0f)),
            new Drone("Drone 6", "6", new Capability(false, false, 3f, 2000, 0.01f, 1.0f, 2.0f)),
            new Drone("Drone 8", "8", new Capability(true, false, 5f, 1000, 0.05f, 1.0f, 2.0f))
        );

        when(droneIlpClient.fetchAllDrones()).thenReturn(drones);

        // cooling true -> [1,5,8,9,888]
        when(droneIlpClient.findDroneIdsByCooling(true)).thenReturn(Arrays.asList(1,5,8,9,888));
        when(droneIlpClient.findDroneIdsByCooling(false)).thenReturn(Arrays.asList(2,3,4,6,7,10,99998,456));

        when(droneIlpClient.findDroneIdsByHeating(true)).thenReturn(Arrays.asList());
        when(droneIlpClient.findDroneIdsByHeating(false)).thenReturn(Arrays.asList(1,2,3,4,5,6,7,8,9,10,456,888,99998));

        // Minimal stubs for other calls used by services
        when(droneIlpClient.fetchAllDronesForServicePoints()).thenReturn(Arrays.asList(
            new DroneForServicePoint(1, Arrays.asList(
                new DroneAvailability("1", Arrays.asList(new Availability("MONDAY", LocalTime.of(0,0), LocalTime.of(23,59))))
            ))
        ));

        when(droneIlpClient.fetchAllServicePoints()).thenReturn(Arrays.asList(
            new ServicePoint(1, "SP1", new Position(BigDecimal.ZERO, BigDecimal.ZERO))
        ));

        when(droneIlpClient.fetchAllRestrictedAreas()).thenReturn(Arrays.asList());
        }

    /**
     * Tests the /api/v1/distanceTo endpoint with valid positions.
     */
    @Test
    void testMockDistanceValid() {
        String json = """
                    {
                        "position1": { "lat": 0, "lng": 0 },
                        "position2": { "lat": 3, "lng": 4 }
                    }
                """;

        try {
            mvc.perform(post("/api/v1/distanceTo")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isOk())
                    .andExpect(content().string("5"));
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    @Test
    void testMockDistanceValidWithNegatives() {
        String json = """
                    {
                        "position1": { "lat": -10, "lng": -10},
                        "position2": { "lat": 2, "lng": -6}
                    }
                """;

        try {
            mvc.perform(post("/api/v1/distanceTo")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isOk())
                    .andExpect(result -> {
                        BigDecimal value = new BigDecimal(result.getResponse().getContentAsString());
                        BigDecimal expected = new BigDecimal("12.649110640673517");
                        BigDecimal rounded = value.setScale(15, RoundingMode.HALF_UP);
                        assertEquals(expected, rounded);
                    });

        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    @Test
    void testMockDistanceValidWithInvalidPositions() {
        try {
            // position1 missing latitude -> service should return 400
            String json = "{ \"position1\": { \"lng\": 0 }, \"position2\": { \"lng\": 1, \"lat\": 1 } }";
            mvc.perform(post("/api/v1/distanceTo")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isBadRequest());
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    @Test
    void testMockDistanceInvalid() {
        try {
            String badJson = "{ this is : not valid json }";
            mvc.perform(post("/api/v1/distanceTo")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(badJson))
                    .andExpect(status().isBadRequest());
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    @Test
    void testMockDistanceInvalidWithNegatives() {
        try {
            // both positions null -> bad request
            String json = "{ \"position1\": null, \"position2\": null }";
            mvc.perform(post("/api/v1/distanceTo")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isBadRequest());
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    @Test
    void testMockClosenessValid() {
        try {
            // positions very close -> distance < 0.00015 -> expect true
            String json = "{ \"position1\": { \"lng\": 0, \"lat\": 0 }, \"position2\": { \"lng\": 0, \"lat\": 0.0001 } }";
            mvc.perform(post("/api/v1/isCloseTo")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isOk())
                    .andExpect(content().string("true"));
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    @Test
    void testMockClosenessInvalid() {
        try {
            String badJson = "{ this is : not valid json }";
            mvc.perform(post("/api/v1/isCloseTo")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(badJson))
                    .andExpect(status().isBadRequest());
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    @Test
    void testMockClosenessInvalidWithNegatives() {
        try {
            // missing coordinates -> bad request
            String json = "{ \"position1\": { \"lng\": 0 }, \"position2\": { \"lat\": 0.0001 } }";
            mvc.perform(post("/api/v1/isCloseTo")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isBadRequest());
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    @Test
    void testMockClosenessInvalidWithInvalidPositions() {
        try {
            // explicit nulls
            String json = "{ \"position1\": null, \"position2\": null }";
            mvc.perform(post("/api/v1/isCloseTo")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isBadRequest());
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    @Test
    void testMockNextPositionValid() {
        try {
            // valid start and angle
            String json = "{ \"start\": { \"lng\": -3.192473, \"lat\": 55.946233 }, \"angle\": 45 }";
            mvc.perform(post("/api/v1/nextPosition")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isOk())
                    .andExpect(result -> {
                        String body = result.getResponse().getContentAsString();
                        // response should be a JSON object containing lng and lat
                        if (!body.contains("\"lng\"") || !body.contains("\"lat\"")) {
                            throw new AssertionError("Expected lng/lat in response: " + body);
                        }
                    });
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    @Test
    void testMockNextPositionInvalid() {
        try {
            // invalid angle (>=360)
            String json = "{ \"start\": { \"lng\": -3.192473, \"lat\": 55.946233 }, \"angle\": 400 }";
            mvc.perform(post("/api/v1/nextPosition")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isBadRequest());
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    @Test
    void testMockNextPositionInvalidWithNegatives() {
        try {
            // out-of-range latitude -> invalid start
            String json = "{ \"start\": { \"lng\": -3.192473, \"lat\": 95 }, \"angle\": 45 }";
            mvc.perform(post("/api/v1/nextPosition")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isBadRequest());
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    @Test
    void testMockNextPositionInvalidWithInvalidPositions() {
        try {
            // missing start
            String json = "{ \"start\": null, \"angle\": 45 }";
            mvc.perform(post("/api/v1/nextPosition")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isBadRequest());
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    @Test
    void testMockNextPositionInvalidWithInvalidMove() {
        try {
            // angle not a multiple of 22.5 -> should be rejected
            String json = "{ \"start\": { \"lng\": -3.192473, \"lat\": 55.946233 }, \"angle\": 22.3 }";
            mvc.perform(post("/api/v1/nextPosition")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isBadRequest());
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    @Test
    void testMockNextPositionInvalidWithInvalidAngle() {
        try {
            // angle missing
            String json = "{ \"start\": { \"lng\": -3.192473, \"lat\": 55.946233 } }";
            mvc.perform(post("/api/v1/nextPosition")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isBadRequest());
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    @Test
    void testMockRegion_nullRequest_returnsBadRequest() {
        try {
            // send empty body (json = "") -> should be parsed as bad request
            mvc.perform(post("/api/v1/isInRegion")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(""))
                    .andExpect(status().isBadRequest());
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    @Test
    void testMockRegion_malformedJson_returnsBadRequest() {
        try {
            String invalidJson = "{ this is : not valid json }";
            mvc.perform(post("/api/v1/isInRegion")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(invalidJson))
                    .andExpect(status().isBadRequest());
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    @Test
    void testMockRegion_nullPosition_returnsBadRequest() {
        try {
            // region provided but position null
            String json = "{ \"position\": null, \"region\": { \"name\": \"square\", \"vertices\": [{ \"lng\": 0, \"lat\": 0 }, { \"lng\": 0, \"lat\": 1 }, { \"lng\": 1, \"lat\": 0 }, { \"lng\": 1, \"lat\": 1 }] } }";
            mvc.perform(post("/api/v1/isInRegion")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isBadRequest());
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    @Test
    void testMockRegion_nullRegion_returnsBadRequest() {
        try {
            // position provided but region null
            String json = "{ \"position\": { \"lng\": 0, \"lat\": 0.5 }, \"region\": null }";
            mvc.perform(post("/api/v1/isInRegion")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isBadRequest());
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    @Test
    void testMockRegion_pointInside_square_returnsTrue() {
        try {
            String json = "{ \"position\": { \"lng\": 0.5, \"lat\": 0.5 }, \"region\": { \"name\": \"square\", \"vertices\": [ { \"lng\": 0, \"lat\": 0 }, { \"lng\": 0, \"lat\": 1 }, { \"lng\": 1, \"lat\": 1 }, { \"lng\": 1, \"lat\": 0 } ] } }";
            mvc.perform(post("/api/v1/isInRegion")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isOk())
                    .andExpect(content().string("true"));
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    @Test
    void testMockRegion_pointOutside_square_returnsFalse() {
        try {
            String json = "{ \"position\": { \"lng\": 2.0, \"lat\": 2.0 }, \"region\": { \"name\": \"square\", \"vertices\": [ { \"lng\": 0, \"lat\": 0 }, { \"lng\": 0, \"lat\": 1 }, { \"lng\": 1, \"lat\": 1 }, { \"lng\": 1, \"lat\": 0 } ] } }";
            mvc.perform(post("/api/v1/isInRegion")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isOk())
                    .andExpect(content().string("false"));
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    @Test
    void testMockRegion_pointOnBorder_square_returnsTrue() {
        try {
            String json = "{ \"position\": { \"lng\": 0, \"lat\": 0.5 }, \"region\": { \"name\": \"square\", \"vertices\": [ { \"lng\": 0, \"lat\": 0 }, { \"lng\": 0, \"lat\": 1 }, { \"lng\": 1, \"lat\": 1 }, { \"lng\": 1, \"lat\": 0 } ] } }";
            mvc.perform(post("/api/v1/isInRegion")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isOk())
                    .andExpect(content().string("true"));
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    @Test
    void testMockRegion_pointInside_complexRegion_returnsTrue() {
        try {
            String json = "{ \"position\": { \"lng\": 0, \"lat\": 0.5 }, \"region\": { \"name\": \"complex\", \"vertices\": [ { \"lng\": -70, \"lat\": -20 }, { \"lng\": -40, \"lat\": 60 }, { \"lng\": 0, \"lat\": 90 }, { \"lng\": 50, \"lat\": 50 }, { \"lng\": 80, \"lat\": -10 }, { \"lng\": 30, \"lat\": -80 }, { \"lng\": -30, \"lat\": -60 } ] } }";
            mvc.perform(post("/api/v1/isInRegion")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isOk())
                    .andExpect(content().string("true"));
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    @Test
    void testMockRegion_pointOnVertex_complexRegion_returnsTrue() {
        try {
            String json = "{ \"position\": { \"lng\": -40, \"lat\": 60 }, \"region\": { \"name\": \"complex\", \"vertices\": [ { \"lng\": -70, \"lat\": -20 }, { \"lng\": -40, \"lat\": 60 }, { \"lng\": 0, \"lat\": 90 }, { \"lng\": 50, \"lat\": 50 }, { \"lng\": 80, \"lat\": -10 }, { \"lng\": 30, \"lat\": -80 }, { \"lng\": -30, \"lat\": -60 } ] } }";
            mvc.perform(post("/api/v1/isInRegion")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isOk())
                    .andExpect(content().string("true"));
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    @Test
    void testDistance_semanticError_outOfBounds_returnsBadRequest() {
        try {
            String json = "{ \"position1\": { \"lng\": -300.192473, \"lat\": 550.946233 }, \"position2\": { \"lng\": -3202.192473, \"lat\": 5533.942617 } }";
            mvc.perform(post("/api/v1/distanceTo")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isBadRequest());
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    @Test
    void testDistance_syntaxError_malformedAndWrongField_returnsBadRequest() {
        try {
            String json = "{ \"position1\": { \"lng\": -3.192473, }, \"position2\": { \"lng\": -3.192473, \"lat_Pos2\": 55.942617 } }";
            mvc.perform(post("/api/v1/distanceTo")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isBadRequest());
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    @Test
    void testIsCloseTo_semanticError_outOfBounds_returnsBadRequest() {
        try {
            String json = "{ \"position1\": { \"lng\": -3004.192473, \"lat\": 550.946233 }, \"position2\": { \"lng\": -390.192473, \"lat\": 551.942617 } }";
            mvc.perform(post("/api/v1/isCloseTo")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isBadRequest());
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    @Test
    void testNextPosition_semanticError_invalidAngle900_returnsBadRequest() {
        try {
            String json = "{ \"start\": { \"lng\": -3.192473, \"lat\": 55.946233 }, \"angle\": 900 }";
            mvc.perform(post("/api/v1/nextPosition")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isBadRequest());
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    @Test
    void testIsInRegion_semanticError_outOfBoundsPosition_returnsBadRequest() {
        try {
            String json = "{ \"position\": { \"lng\": -390.186000, \"lat\": 550.944000 }, \"region\": { \"name\": \"central\", \"vertices\": [ { \"lng\": -3.192473, \"lat\": 558.946233 }, { \"lng\": -367.192473, \"lat\": 55.942617 }, { \"lng\": -3.184319, \"lat\": 55.942617 }, { \"lng\": -3.184319, \"lat\": 55.946233 }, { \"lng\": -3.192473, \"lat\": 55.946233 } ] } }";
            mvc.perform(post("/api/v1/isInRegion")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isBadRequest());
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    @Test
    void testIsInRegion_syntaxError_wrongFieldNames_returnsBadRequest() {
        try {
            String json = "{ \"currentPosition\": { \"lng\": 1.234, \"lat\": 1.222 }, \"region\": { \"names\": \"central\", \"verticesList\": [ { \"lng\": -3.192473, \"lat\": 55.946233 }, { \"lng\": -3.192473, \"lat\": 55.942617 }, { \"lng\": -3.184319, \"lat\": 55.942617 }, { \"lng\": -3.184319, \"lat\": 55.946233 }, { \"lng\": -3.192473, \"lat\": 55.946233 } ] } }";
            mvc.perform(post("/api/v1/isInRegion")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isBadRequest());
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    @Test
    void testIsInRegion_openVerticesPolygon_returnsBadRequest() {
        try {
            String json = "{ \"position\": { \"lng\": 398.234, \"lat\": 500.222 }, \"region\": { \"name\": \"central\", \"vertices\": [ { \"lng\": -3.192473, \"lat\": 55.946233 }, { \"lng\": -3.192473, \"lat\": 55.942617 }, { \"lng\": -3.184319, \"lat\": 55.942617 } ] } }";
            mvc.perform(post("/api/v1/isInRegion")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isBadRequest());
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    @Test
    void testGetDroneDetails_validId_returnsExpectedJson() {
        try {
            String expected = "{\"id\":\"99998\",\"name\":\"Drone 99998\",\"capability\":{\"cooling\":false,\"heating\":false,\"capacity\":12.0,\"maxMoves\":1500,\"costPerMove\":0.07,\"costInitial\":1.4,\"costFinal\":3.5}}";
            mvc.perform(get("/api/v1/droneDetails/99998"))
                    .andExpect(status().isOk())
                    .andExpect(content().json(expected, false));
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    @Test
    void testGetDroneDetails_invalidId_returnsNotFound() {
        try {
            mvc.perform(get("/api/v1/droneDetails/99999"))
                    .andExpect(status().isNotFound());
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    @Test
    void testGetDronesWithCooling_true_and_false() {
        try {
            mvc.perform(get("/api/v1/dronesWithCooling/true"))
                    .andExpect(status().isOk())
                    .andExpect(content().json("[1,5,8,9,888]"));

            mvc.perform(get("/api/v1/dronesWithCooling/false"))
                    .andExpect(status().isOk())
                    .andExpect(content().json("[2,3,4,6,7,10,99998,456]"));
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    @Test
    void testQueryAsPath_getQueries_returnExpectedArrays() {
        try {
            mvc.perform(get("/api/v1/queryAsPath/capacity/8"))
                    .andExpect(status().isOk())
                    .andExpect(content().json("[2,4,7,9]"));

            mvc.perform(get("/api/v1/queryAsPath/maxMoves/1500"))
                    .andExpect(status().isOk())
                    .andExpect(content().json("[5,10,99998,888,456]"));

            mvc.perform(get("/api/v1/queryAsPath/costPerMove/0.07"))
                    .andExpect(status().isOk())
                    .andExpect(content().json("[10,99998,888,456]"));
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    @Test
    void testQueryPost_filters_returnExpectedArrays() {
        try {
            String body1 = "[{\"attribute\":\"costPerMove\",\"operator\":\"<\",\"value\":\"0.04\"},{\"attribute\":\"maxMoves\",\"operator\":\">\",\"value\":\"1000\"}]";
            mvc.perform(post("/api/v1/query")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body1))
                    .andExpect(status().isOk())
                    .andExpect(content().json("[\"1\",\"6\"]"));

            String body2 = "[{\"attribute\":\"costFinal\",\"operator\":\"=\",\"value\":\"3.5\"},{\"attribute\":\"maxMoves\",\"operator\":\"=\",\"value\":\"1500\"}]";
            mvc.perform(post("/api/v1/query")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body2))
                    .andExpect(status().isOk())
                    .andExpect(content().json("[\"5\",\"10\",\"99998\",\"888\",\"456\"]"));
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

}
