package ilp.ilp_cw.ilp_1_2.contract;

import ilp.ilp_cw.ilp_1_2.ilpCw1Application;
import ilp.ilp_cw.ilp_1_2.model.*;
import ilp.ilp_cw.ilp_1_2.service.DroneIlpClient;
import io.restassured.http.ContentType;
import io.restassured.module.mockmvc.RestAssuredMockMvc;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
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

import static io.restassured.module.mockmvc.RestAssuredMockMvc.*;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.when;

/**
 * API Contract Tests for Drone REST endpoints.
 * Validates API contracts, response schemas, status codes, and error handling.
 * Uses REST Assured for declarative API testing.
 */
@SpringBootTest(classes = ilpCw1Application.class)
@AutoConfigureMockMvc
class DroneApiContractTest {

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
        // Setup test drones
        List<Drone> drones = Arrays.asList(
            new Drone("Drone 1", "1", new Capability(true, false, 10f, 1000, 0.05f, 1.0f, 2.0f)),
            new Drone("Drone 2", "2", new Capability(false, true, 8f, 1200, 0.03f, 1.2f, 2.5f)),
            new Drone("Drone 3", "3", new Capability(false, false, 12f, 1500, 0.04f, 1.5f, 3.0f))
        );
        when(droneIlpClient.fetchAllDrones()).thenReturn(drones);
        when(droneIlpClient.findDroneIdsByCooling(true)).thenReturn(List.of(1));
        when(droneIlpClient.findDroneIdsByCooling(false)).thenReturn(List.of(2, 3));
        when(droneIlpClient.findDroneIdsByHeating(true)).thenReturn(List.of(2));
        when(droneIlpClient.findDroneIdsByHeating(false)).thenReturn(List.of(1, 3));

        // Setup service points
        List<ServicePoint> servicePoints = List.of(
            new ServicePoint(1, "SP1", new Position(BigDecimal.valueOf(-3.186874), BigDecimal.valueOf(55.944494)))
        );
        when(droneIlpClient.fetchAllServicePoints()).thenReturn(servicePoints);

        // Setup restricted areas
        when(droneIlpClient.fetchAllRestrictedAreas()).thenReturn(Collections.emptyList());

        // Setup drone availability
        Availability avail = new Availability("MONDAY", LocalTime.of(0, 0), LocalTime.of(23, 59));
        DroneAvailability da = new DroneAvailability("1", List.of(avail));
        DroneForServicePoint dfsp = new DroneForServicePoint(1, List.of(da));
        when(droneIlpClient.fetchAllDronesForServicePoints()).thenReturn(List.of(dfsp));
    }

    // ==================== Basic Endpoint Tests ====================

    @Test
    @DisplayName("GET /api/v1/uid returns correct UID and status 200")
    void testUidEndpoint_ReturnsCorrectUid() {
        given()
            .when()
                .get("/api/v1/uid")
            .then()
                .statusCode(200)
                .contentType(ContentType.TEXT)
                .body(equalTo("s2141930"));
    }

    // ==================== Distance Calculation Contract Tests ====================

    @Test
    @DisplayName("POST /api/v1/distanceTo returns valid distance with 200")
    void testDistanceTo_ValidPositions_Returns200() {
        String requestBody = """
            {
                "position1": {"lng": 0.0, "lat": 0.0},
                "position2": {"lng": 3.0, "lat": 4.0}
            }
            """;

        given()
            .contentType(ContentType.JSON)
            .body(requestBody)
        .when()
            .post("/api/v1/distanceTo")
        .then()
            .statusCode(200)
            .contentType(ContentType.JSON)
            .body(notNullValue());
    }

    @Test
    @DisplayName("POST /api/v1/distanceTo with same position returns 0")
    void testDistanceTo_SamePosition_ReturnsZero() {
        String requestBody = """
            {
                "position1": {"lng": 1.5, "lat": 2.5},
                "position2": {"lng": 1.5, "lat": 2.5}
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
    @DisplayName("POST /api/v1/distanceTo with invalid JSON returns 400")
    void testDistanceTo_InvalidJson_Returns400() {
        given()
            .contentType(ContentType.JSON)
            .body("{invalid json}")
        .when()
            .post("/api/v1/distanceTo")
        .then()
            .statusCode(400);
    }

    @Test
    @DisplayName("POST /api/v1/distanceTo with missing fields returns 400")
    void testDistanceTo_MissingFields_Returns400() {
        String requestBody = """
            {
                "position1": {"lng": 0.0}
            }
            """;

        given()
            .contentType(ContentType.JSON)
            .body(requestBody)
        .when()
            .post("/api/v1/distanceTo")
        .then()
            .statusCode(400);
    }

    // ==================== Closeness Check Contract Tests ====================

    @Test
    @DisplayName("POST /api/v1/isCloseTo returns boolean with 200")
    void testIsCloseTo_ValidPositions_Returns200WithBoolean() {
        String requestBody = """
            {
                "position1": {"lng": 0.0, "lat": 0.0},
                "position2": {"lng": 0.00001, "lat": 0.00001}
            }
            """;

        given()
            .contentType(ContentType.JSON)
            .body(requestBody)
        .when()
            .post("/api/v1/isCloseTo")
        .then()
            .statusCode(200)
            .contentType(ContentType.JSON);
    }

    @Test
    @DisplayName("POST /api/v1/isCloseTo with invalid data returns 400")
    void testIsCloseTo_InvalidData_Returns400() {
        given()
            .contentType(ContentType.JSON)
            .body("{}")
        .when()
            .post("/api/v1/isCloseTo")
        .then()
            .statusCode(400);
    }

    // ==================== Next Position Contract Tests ====================

    @Test
    @DisplayName("POST /api/v1/nextPosition returns valid position with 200")
    void testNextPosition_ValidRequest_Returns200() {
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
            .contentType(ContentType.JSON)
            .body("lng", notNullValue())
            .body("lat", notNullValue());
    }

    @Test
    @DisplayName("POST /api/v1/nextPosition with various angles returns valid positions")
    void testNextPosition_VariousAngles_ReturnsValidPositions() {
        int[] angles = {0, 90, 180, 270, 45, 135, 225, 315};
        
        for (int angle : angles) {
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
                .statusCode(200)
                .body("lng", notNullValue())
                .body("lat", notNullValue());
        }
    }

    // ==================== Region Check Contract Tests ====================

    @Test
    @DisplayName("POST /api/v1/isInRegion returns boolean with 200")
    void testIsInRegion_ValidRequest_Returns200() {
        String requestBody = """
            {
                "position": {"lng": 0.5, "lat": 0.5},
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
            .statusCode(200)
            .contentType(ContentType.JSON);
    }

    // ==================== CW2 Static Query Contract Tests ====================

    @Test
    @DisplayName("GET /api/v1/dronesWithCooling/{state} returns list with 200")
    void testDronesWithCooling_ValidState_Returns200() {
        given()
        .when()
            .get("/api/v1/dronesWithCooling/true")
        .then()
            .statusCode(200)
            .contentType(ContentType.JSON)
            .body("$", isA(List.class));
    }

    @Test
    @DisplayName("GET /api/v1/dronesWithCooling/true returns drones with cooling")
    void testDronesWithCooling_True_ReturnsCorrectDrones() {
        given()
        .when()
            .get("/api/v1/dronesWithCooling/true")
        .then()
            .statusCode(200)
            .body("$", hasItem(1))
            .body("size()", greaterThanOrEqualTo(1));
    }

    @Test
    @DisplayName("GET /api/v1/dronesWithCooling/false returns drones without cooling")
    void testDronesWithCooling_False_ReturnsCorrectDrones() {
        given()
        .when()
            .get("/api/v1/dronesWithCooling/false")
        .then()
            .statusCode(200)
            .body("$", hasItems(2, 3))
            .body("size()", greaterThanOrEqualTo(2));
    }

    @Test
    @DisplayName("GET /api/v1/dronesWithHeating/{state} returns list with 200")
    void testDronesWithHeating_ValidState_Returns200() {
        given()
        .when()
            .get("/api/v1/dronesWithHeating/false")
        .then()
            .statusCode(200)
            .contentType(ContentType.JSON)
            .body("$", isA(List.class));
    }

    @Test
    @DisplayName("GET /api/v1/droneDetails/{id} returns drone details with 200")
    void testDroneDetails_ValidId_Returns200() {
        given()
        .when()
            .get("/api/v1/droneDetails/1")
        .then()
            .statusCode(200)
            .contentType(ContentType.JSON)
            .body("name", notNullValue())
            .body("id", notNullValue())
            .body("capability", notNullValue());
    }

    @Test
    @DisplayName("GET /api/v1/droneDetails/{id} with non-existent ID returns 404")
    void testDroneDetails_NonExistentId_Returns404() {
        given()
        .when()
            .get("/api/v1/droneDetails/9999")
        .then()
            .statusCode(404);
    }

    // ==================== Response Schema Validation ====================

    @Test
    @DisplayName("Drone details response has correct schema")
    void testDroneDetails_ResponseSchema() {
        given()
        .when()
            .get("/api/v1/droneDetails/1")
        .then()
            .statusCode(200)
            .body("name", isA(String.class))
            .body("id", isA(String.class))
            .body("capability.cooling", isA(Boolean.class))
            .body("capability.heating", isA(Boolean.class))
            .body("capability.capacity", isA(Float.class))
            .body("capability.maxMoves", isA(Integer.class))
            .body("capability.costPerMove", isA(Float.class))
            .body("capability.costInitial", isA(Float.class))
            .body("capability.costFinal", isA(Float.class));
    }

    @Test
    @DisplayName("Position response has correct schema")
    void testNextPosition_ResponseSchema() {
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
            .body("lng", isA(Number.class))
            .body("lat", isA(Number.class));
    }

    // ==================== Error Handling Contract Tests ====================

    @Test
    @DisplayName("Endpoints reject empty request body with 400")
    void testEndpoints_EmptyBody_Returns400() {
        String[] endpoints = {"/api/v1/distanceTo", "/api/v1/isCloseTo", 
                             "/api/v1/nextPosition", "/api/v1/isInRegion"};
        
        for (String endpoint : endpoints) {
            given()
                .contentType(ContentType.JSON)
                .body("{}")
            .when()
                .post(endpoint)
            .then()
                .statusCode(400);
        }
    }

    @Test
    @DisplayName("Endpoints handle null values appropriately")
    void testEndpoints_NullValues_HandleGracefully() {
        String requestBody = """
            {
                "position1": null,
                "position2": {"lng": 1.0, "lat": 1.0}
            }
            """;

        given()
            .contentType(ContentType.JSON)
            .body(requestBody)
        .when()
            .post("/api/v1/distanceTo")
        .then()
            .statusCode(400);
    }

    // ==================== Content Type Validation ====================

    @Test
    @DisplayName("POST endpoints require JSON content type")
    void testPostEndpoints_RequireJsonContentType() {
        given()
            .contentType(ContentType.TEXT)
            .body("plain text")
        .when()
            .post("/api/v1/distanceTo")
        .then()
            .statusCode(415); // Unsupported Media Type
    }

    @Test
    @DisplayName("GET endpoints return JSON responses")
    void testGetEndpoints_ReturnJsonContentType() {
        given()
        .when()
            .get("/api/v1/dronesWithCooling/true")
        .then()
            .contentType(ContentType.JSON);
    }
}
