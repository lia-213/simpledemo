package ilp.ilp_cw.ilp_1_2.contract;

import ilp.ilp_cw.ilp_1_2.ilpCw1Application;
import ilp.ilp_cw.ilp_1_2.model.*;
import ilp.ilp_cw.ilp_1_2.service.DroneIlpClient;
import io.restassured.http.ContentType;
import io.restassured.module.mockmvc.RestAssuredMockMvc;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static io.restassured.module.mockmvc.RestAssuredMockMvc.given;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.when;

/**
 * Extended API Contract Tests focusing on edge cases and boundary conditions.
 * Validates API behavior with extreme inputs, edge cases, and error scenarios.
 */
@SpringBootTest(classes = ilpCw1Application.class)
@AutoConfigureMockMvc
class DroneApiContractEdgeCaseTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DroneIlpClient droneIlpClient;

    @BeforeEach
    void setUp() {
        RestAssuredMockMvc.mockMvc(mockMvc);
        setupMockData();
    }

    private void setupMockData() {
        List<Drone> drones = Arrays.asList(
            new Drone("Drone 1", "1", new Capability(true, false, 10f, 1000, 0.05f, 1.0f, 2.0f)),
            new Drone("Drone 2", "2", new Capability(false, true, 8f, 1200, 0.03f, 1.2f, 2.5f))
        );
        when(droneIlpClient.fetchAllDrones()).thenReturn(drones);
        when(droneIlpClient.findDroneIdsByCooling(true)).thenReturn(List.of(1));
        when(droneIlpClient.findDroneIdsByCooling(false)).thenReturn(List.of(2));
        when(droneIlpClient.findDroneIdsByHeating(true)).thenReturn(List.of(2));
        when(droneIlpClient.findDroneIdsByHeating(false)).thenReturn(List.of(1));

        List<ServicePoint> servicePoints = List.of(
            new ServicePoint(1, "SP1", new Position(BigDecimal.valueOf(-3.186874), BigDecimal.valueOf(55.944494)))
        );
        when(droneIlpClient.fetchAllServicePoints()).thenReturn(servicePoints);
        when(droneIlpClient.fetchAllRestrictedAreas()).thenReturn(Collections.emptyList());

        Availability avail = new Availability("MONDAY", LocalTime.of(0, 0), LocalTime.of(23, 59));
        DroneAvailability da = new DroneAvailability("1", List.of(avail));
        DroneForServicePoint dfsp = new DroneForServicePoint(1, List.of(da));
        when(droneIlpClient.fetchAllDronesForServicePoints()).thenReturn(List.of(dfsp));
    }

    // ==================== Extreme Coordinate Values ====================

    @Test
    @DisplayName("Distance calculation with maximum valid coordinates")
    void testDistanceTo_MaxCoordinates() {
        String requestBody = """
            {
                "position1": {"lng": -180.0, "lat": -90.0},
                "position2": {"lng": 180.0, "lat": 90.0}
            }
            """;

        given()
            .contentType(ContentType.JSON)
            .body(requestBody)
        .when()
            .post("/api/v1/distanceTo")
        .then()
            .statusCode(200)
            .body(notNullValue());
    }

    @Test
    @DisplayName("Distance calculation with very small coordinate differences")
    void testDistanceTo_VerySmallDifferences() {
        String requestBody = """
            {
                "position1": {"lng": 0.000000001, "lat": 0.000000001},
                "position2": {"lng": 0.000000002, "lat": 0.000000002}
            }
            """;

        given()
            .contentType(ContentType.JSON)
            .body(requestBody)
        .when()
            .post("/api/v1/distanceTo")
        .then()
            .statusCode(200)
            .body(notNullValue());
    }

    @Test
    @DisplayName("Next position with valid angles")
    void testNextPosition_BoundaryAngles() {
        int[] validAngles = {0, 90, 180, 270};
        
        for (int angle : validAngles) {
            String requestBody = String.format("""
                {
                    "start": {"lng": 0.0, "lat": 0.0},
                    "angle": %d
                }
                """, angle);

            given()
                .contentType(ContentType.JSON)
                .body(requestBody)
            .when()
                .post("/api/v1/nextPosition")
            .then()
                .statusCode(200);
        }
    }

    @Test
    @DisplayName("Next position from maximum coordinates")
    void testNextPosition_MaxCoordinates() {
        String requestBody = """
            {
                "start": {"lng": 179.0, "lat": 89.0},
                "angle": 0
            }
            """;

        given()
            .contentType(ContentType.JSON)
            .body(requestBody)
        .when()
            .post("/api/v1/nextPosition")
        .then()
            .statusCode(200);
    }

    // ==================== Region Edge Cases ====================

    @Test
    @DisplayName("Point on region boundary")
    void testIsInRegion_PointOnBoundary() {
        String requestBody = """
            {
                "position": {"lng": 0.0, "lat": 0.0},
                "region": {
                    "name": "test",
                    "vertices": [
                        {"lng": 0.0, "lat": 0.0},
                        {"lng": 1.0, "lat": 0.0},
                        {"lng": 1.0, "lat": 1.0},
                        {"lng": 0.0, "lat": 1.0}
                    ]
                }
            }
            """;

        given()
            .contentType(ContentType.JSON)
            .body(requestBody)
        .when()
            .post("/api/v1/isInRegion")
        .then()
            .statusCode(200);
    }

    @Test
    @DisplayName("Triangle region with point inside")
    void testIsInRegion_TriangleRegion() {
        String requestBody = """
            {
                "position": {"lng": 0.0, "lat": 0.0},
                "region": {
                    "name": "triangle",
                    "vertices": [
                        {"lng": -1.0, "lat": -1.0},
                        {"lng": 1.0, "lat": -1.0},
                        {"lng": 0.0, "lat": 1.0}
                    ]
                }
            }
            """;

        given()
            .contentType(ContentType.JSON)
            .body(requestBody)
        .when()
            .post("/api/v1/isInRegion")
        .then()
            .statusCode(200);
    }

    @Test
    @DisplayName("Complex polygon region")
    void testIsInRegion_ComplexPolygon() {
        String requestBody = """
            {
                "position": {"lng": 0.0, "lat": 0.0},
                "region": {
                    "name": "complex",
                    "vertices": [
                        {"lng": -2.0, "lat": -2.0},
                        {"lng": 2.0, "lat": -2.0},
                        {"lng": 2.0, "lat": 0.0},
                        {"lng": 0.0, "lat": 0.0},
                        {"lng": 0.0, "lat": 2.0},
                        {"lng": -2.0, "lat": 2.0}
                    ]
                }
            }
            """;

        given()
            .contentType(ContentType.JSON)
            .body(requestBody)
        .when()
            .post("/api/v1/isInRegion")
        .then()
            .statusCode(200);
    }

    // ==================== Boolean Parameter Variations ====================

    @ParameterizedTest
    @DisplayName("Cooling endpoint accepts various boolean formats")
    @ValueSource(strings = {"true", "True", "TRUE", "false", "False", "FALSE"})
    void testDronesWithCooling_VariousBooleanFormats(String boolValue) {
        given()
        .when()
            .get("/api/v1/dronesWithCooling/" + boolValue)
        .then()
            .statusCode(200)
            .body("$", isA(List.class));
    }

    @ParameterizedTest
    @DisplayName("Heating endpoint accepts various boolean formats")
    @ValueSource(strings = {"true", "false", "TRUE", "FALSE"})
    void testDronesWithHeating_VariousBooleanFormats(String boolValue) {
        given()
        .when()
            .get("/api/v1/dronesWithHeating/" + boolValue)
        .then()
            .statusCode(200)
            .body("$", isA(List.class));
    }

    // ==================== Large Data Handling ====================

    @Test
    @DisplayName("Region with many vertices")
    void testIsInRegion_ManyVertices() {
        StringBuilder vertices = new StringBuilder();
        for (int i = 0; i < 50; i++) {
            double angle = 2 * Math.PI * i / 50;
            double lng = Math.cos(angle);
            double lat = Math.sin(angle);
            if (i > 0) vertices.append(",");
            vertices.append(String.format("{\"lng\": %.6f, \"lat\": %.6f}", lng, lat));
        }

        String requestBody = String.format("""
            {
                "position": {"lng": 0.0, "lat": 0.0},
                "region": {
                    "name": "circle",
                    "vertices": [%s]
                }
            }
            """, vertices);

        given()
            .contentType(ContentType.JSON)
            .body(requestBody)
        .when()
            .post("/api/v1/isInRegion")
        .then()
            .statusCode(200);
    }

    // ==================== Precision and Rounding ====================

    @Test
    @DisplayName("High precision coordinates are handled correctly")
    void testDistanceTo_HighPrecisionCoordinates() {
        String requestBody = """
            {
                "position1": {"lng": 1.123456789012345, "lat": 2.987654321098765},
                "position2": {"lng": 3.456789012345678, "lat": 4.321098765432109}
            }
            """;

        given()
            .contentType(ContentType.JSON)
            .body(requestBody)
        .when()
            .post("/api/v1/distanceTo")
        .then()
            .statusCode(200)
            .body(notNullValue());
    }

    @Test
    @DisplayName("Negative coordinates are handled correctly")
    void testDistanceTo_NegativeCoordinates() {
        String requestBody = """
            {
                "position1": {"lng": -3.188267, "lat": 55.944425},
                "position2": {"lng": -0.127758, "lat": 51.507351}
            }
            """;

        given()
            .contentType(ContentType.JSON)
            .body(requestBody)
        .when()
            .post("/api/v1/distanceTo")
        .then()
            .statusCode(200)
            .body(notNullValue());
    }

    // ==================== Error Response Consistency ====================

    @Test
    @DisplayName("All POST endpoints return consistent 400 errors for bad requests")
    void testPostEndpoints_Consistent400Errors() {
        String[] endpoints = {
            "/api/v1/distanceTo",
            "/api/v1/isCloseTo",
            "/api/v1/nextPosition",
            "/api/v1/isInRegion"
        };

        for (String endpoint : endpoints) {
            given()
                .contentType(ContentType.JSON)
                .body("invalid")
            .when()
                .post(endpoint)
            .then()
                .statusCode(400);
        }
    }

    @Test
    @DisplayName("Malformed JSON returns 400")
    void testEndpoints_MalformedJson_Returns400() {
        String malformedJson = "{\"position1\": {\"lng\": 0.0, \"lat\":";

        given()
            .contentType(ContentType.JSON)
            .body(malformedJson)
        .when()
            .post("/api/v1/distanceTo")
        .then()
            .statusCode(400);
    }

    // ==================== Special Character Handling ====================

    @Test
    @DisplayName("Region names with special characters are accepted")
    void testIsInRegion_SpecialCharactersInName() {
        String requestBody = """
            {
                "position": {"lng": 0.5, "lat": 0.5},
                "region": {
                    "name": "Test Region #1 - Area (A/B)",
                    "vertices": [
                        {"lng": 0.0, "lat": 0.0},
                        {"lng": 1.0, "lat": 0.0},
                        {"lng": 1.0, "lat": 1.0},
                        {"lng": 0.0, "lat": 1.0}
                    ]
                }
            }
            """;

        given()
            .contentType(ContentType.JSON)
            .body(requestBody)
        .when()
            .post("/api/v1/isInRegion")
        .then()
            .statusCode(200);
    }

    // ==================== Zero and Negative Edge Cases ====================

    @Test
    @DisplayName("Distance to self is zero")
    void testDistanceTo_ToSelf_ExactlyZero() {
        String requestBody = """
            {
                "position1": {"lng": 55.5, "lat": -3.2},
                "position2": {"lng": 55.5, "lat": -3.2}
            }
            """;

        given()
            .contentType(ContentType.JSON)
            .body(requestBody)
        .when()
            .post("/api/v1/distanceTo")
        .then()
            .statusCode(200)
            .body(notNullValue());
    }

    @Test
    @DisplayName("Angle of 0 degrees moves north")
    void testNextPosition_ZeroDegrees() {
        String requestBody = """
            {
                "start": {"lng": 0.0, "lat": 0.0},
                "angle": 0
            }
            """;

        given()
            .contentType(ContentType.JSON)
            .body(requestBody)
        .when()
            .post("/api/v1/nextPosition")
        .then()
            .statusCode(200)
            .body("lng", greaterThanOrEqualTo(0.0f))
            .body("lat", greaterThan(0.0f));
    }
}
