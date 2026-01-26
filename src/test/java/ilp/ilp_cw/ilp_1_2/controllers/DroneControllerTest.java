package ilp.ilp_cw.ilp_1_2.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import ilp.ilp_cw.ilp_1_2.dto.*;
import ilp.ilp_cw.ilp_1_2.model.*;
import ilp.ilp_cw.ilp_1_2.service.DroneService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Comprehensive test suite for DroneController REST endpoints.
 * Uses @WebMvcTest to test the controller layer in isolation with mocked service.
 */
@WebMvcTest(DroneController.class)
class DroneControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private DroneService droneService;

    private Position position1;
    private Position position2;
    private Drone testDrone;

    @BeforeEach
    void setUp() {
        position1 = new Position();
        position1.setLongitude(BigDecimal.valueOf(-3.1878));
        position1.setLatitude(BigDecimal.valueOf(55.9445));

        position2 = new Position();
        position2.setLongitude(BigDecimal.valueOf(-3.1900));
        position2.setLatitude(BigDecimal.valueOf(55.9450));

        // Create test drone
        Capability capability = new Capability(true, false, 100.0f, 500, 0.5f, 10.0f, 5.0f);
        testDrone = new Drone("TestDrone", "1", capability);
    }

    // ==================== UID Endpoint Tests ====================

    @Test
    void testUid_ReturnsStudentUID() throws Exception {
        // Arrange
        String expectedUID = "s2345678";
        when(droneService.getUID()).thenReturn(expectedUID);

        // Act & Assert
        mockMvc.perform(get("/api/v1/uid"))
                .andExpect(status().isOk())
                .andExpect(content().string(expectedUID));

        verify(droneService).getUID();
    }

    // ==================== DistanceTo Endpoint Tests ====================

    @Test
    void testDistanceTo_Success() throws Exception {
        // Arrange
        PositionsDto positionsDto = new PositionsDto(position1, position2);
        BigDecimal expectedDistance = BigDecimal.valueOf(0.0025);
        when(droneService.distanceTo(any(PositionsDto.class)))
                .thenReturn(ResponseEntity.ok(expectedDistance));

        // Act & Assert
        mockMvc.perform(post("/api/v1/distanceTo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(positionsDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(expectedDistance.doubleValue()));

        verify(droneService).distanceTo(any(PositionsDto.class));
    }

    @Test
    void testDistanceTo_ServiceThrowsException_ReturnsBadRequest() throws Exception {
        // Arrange
        PositionsDto positionsDto = new PositionsDto(position1, position2);
        when(droneService.distanceTo(any(PositionsDto.class)))
                .thenThrow(new RuntimeException("Invalid positions"));

        // Act & Assert
        mockMvc.perform(post("/api/v1/distanceTo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(positionsDto)))
                .andExpect(status().isBadRequest());

        verify(droneService).distanceTo(any(PositionsDto.class));
    }

    // ==================== IsCloseTo Endpoint Tests ====================

    @Test
    void testIsCloseTo_ReturnsTrue() throws Exception {
        // Arrange
        PositionsDto positionsDto = new PositionsDto(position1, position2);
        when(droneService.isCloseTo(any(PositionsDto.class)))
                .thenReturn(ResponseEntity.ok(true));

        // Act & Assert
        mockMvc.perform(post("/api/v1/isCloseTo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(positionsDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(true));

        verify(droneService).isCloseTo(any(PositionsDto.class));
    }

    @Test
    void testIsCloseTo_ReturnsFalse() throws Exception {
        // Arrange
        PositionsDto positionsDto = new PositionsDto(position1, position2);
        when(droneService.isCloseTo(any(PositionsDto.class)))
                .thenReturn(ResponseEntity.ok(false));

        // Act & Assert
        mockMvc.perform(post("/api/v1/isCloseTo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(positionsDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(false));
    }

    @Test
    void testIsCloseTo_ServiceThrowsException_ReturnsBadRequest() throws Exception {
        // Arrange
        PositionsDto positionsDto = new PositionsDto(position1, position2);
        when(droneService.isCloseTo(any(PositionsDto.class)))
                .thenThrow(new RuntimeException("Invalid positions"));

        // Act & Assert
        mockMvc.perform(post("/api/v1/isCloseTo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(positionsDto)))
                .andExpect(status().isBadRequest());
    }

    // ==================== NextPosition Endpoint Tests ====================

    @Test
    void testNextPosition_Success() throws Exception {
        // Arrange
        DroneMoveRqstDto moveRequest = new DroneMoveRqstDto(position1, BigDecimal.valueOf(90));
        Position expectedPosition = new Position();
        expectedPosition.setLongitude(BigDecimal.valueOf(-3.1878));
        expectedPosition.setLatitude(BigDecimal.valueOf(55.9446));

        when(droneService.computeNextPosition(any(DroneMoveRqstDto.class)))
                .thenReturn(ResponseEntity.ok(expectedPosition));

        // Act & Assert
        mockMvc.perform(post("/api/v1/nextPosition")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(moveRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lng").exists())
                .andExpect(jsonPath("$.lat").exists());

        verify(droneService).computeNextPosition(any(DroneMoveRqstDto.class));
    }

    @Test
    void testNextPosition_ServiceThrowsException_ReturnsBadRequest() throws Exception {
        // Arrange
        DroneMoveRqstDto moveRequest = new DroneMoveRqstDto(position1, BigDecimal.valueOf(370)); // Invalid angle
        when(droneService.computeNextPosition(any(DroneMoveRqstDto.class)))
                .thenThrow(new IllegalArgumentException("Invalid angle"));

        // Act & Assert
        mockMvc.perform(post("/api/v1/nextPosition")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(moveRequest)))
                .andExpect(status().isBadRequest());
    }

    // ==================== IsInRegion Endpoint Tests ====================

    @Test
    void testIsInRegion_ReturnsTrue() throws Exception {
        // Arrange
        Region region = new Region("TestRegion", new ArrayList<>());
        LocationRegionDto locationRegionDto = new LocationRegionDto(position1, region);

        when(droneService.isInRegion(any(LocationRegionDto.class)))
                .thenReturn(ResponseEntity.ok(true));

        // Act & Assert
        mockMvc.perform(post("/api/v1/isInRegion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(locationRegionDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(true));

        verify(droneService).isInRegion(any(LocationRegionDto.class));
    }

    @Test
    void testIsInRegion_ServiceThrowsException_ReturnsBadRequest() throws Exception {
        // Arrange
        Region region = new Region("TestRegion", new ArrayList<>());
        LocationRegionDto locationRegionDto = new LocationRegionDto(position1, region);

        when(droneService.isInRegion(any(LocationRegionDto.class)))
                .thenThrow(new RuntimeException("Invalid region"));

        // Act & Assert
        mockMvc.perform(post("/api/v1/isInRegion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(locationRegionDto)))
                .andExpect(status().isBadRequest());
    }

    // ==================== DronesWithCooling Endpoint Tests ====================

    @Test
    void testDronesWithCooling_True() throws Exception {
        // Arrange
        List<Integer> expectedIds = Arrays.asList(1, 3, 5);
        when(droneService.dronesWithCooling(true))
                .thenReturn(ResponseEntity.ok(expectedIds));

        // Act & Assert
        mockMvc.perform(get("/api/v1/dronesWithCooling/true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0]").value(1))
                .andExpect(jsonPath("$[1]").value(3))
                .andExpect(jsonPath("$[2]").value(5));

        verify(droneService).dronesWithCooling(true);
    }

    @Test
    void testDronesWithCooling_False() throws Exception {
        // Arrange
        List<Integer> expectedIds = Arrays.asList(2, 4);
        when(droneService.dronesWithCooling(false))
                .thenReturn(ResponseEntity.ok(expectedIds));

        // Act & Assert
        mockMvc.perform(get("/api/v1/dronesWithCooling/false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2));

        verify(droneService).dronesWithCooling(false);
    }

    @Test
    void testDronesWithCooling_CaseInsensitive() throws Exception {
        // Arrange
        when(droneService.dronesWithCooling(true))
                .thenReturn(ResponseEntity.ok(Arrays.asList(1)));

        // Act & Assert - Test various case variations
        mockMvc.perform(get("/api/v1/dronesWithCooling/TRUE"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/dronesWithCooling/True"))
                .andExpect(status().isOk());
    }

    // ==================== DronesWithHeating Endpoint Tests ====================

    @Test
    void testDronesWithHeating_True() throws Exception {
        // Arrange
        List<Integer> expectedIds = Arrays.asList(2, 3, 6);
        when(droneService.dronesWithHeating(true))
                .thenReturn(ResponseEntity.ok(expectedIds));

        // Act & Assert
        mockMvc.perform(get("/api/v1/dronesWithHeating/true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(3));

        verify(droneService).dronesWithHeating(true);
    }

    @Test
    void testDronesWithHeating_False() throws Exception {
        // Arrange
        List<Integer> expectedIds = Collections.emptyList();
        when(droneService.dronesWithHeating(false))
                .thenReturn(ResponseEntity.ok(expectedIds));

        // Act & Assert
        mockMvc.perform(get("/api/v1/dronesWithHeating/false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    // ==================== DroneDetails Endpoint Tests ====================

    @Test
    void testDroneDetails_ValidId_ReturnsDrone() throws Exception {
        // Arrange
        when(droneService.getDrone(1))
                .thenReturn(ResponseEntity.ok(testDrone));

        // Act & Assert
        mockMvc.perform(get("/api/v1/droneDetails/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("TestDrone"))
                .andExpect(jsonPath("$.id").value("1"));

        verify(droneService).getDrone(1);
    }

    @Test
    void testDroneDetails_InvalidId_ReturnsNotFound() throws Exception {
        // Arrange
        when(droneService.getDrone(999))
                .thenReturn(ResponseEntity.notFound().build());

        // Act & Assert
        mockMvc.perform(get("/api/v1/droneDetails/999"))
                .andExpect(status().isNotFound());

        verify(droneService).getDrone(999);
    }

    @Test
    void testDroneDetails_InvalidIdFormat_HandlesGracefully() throws Exception {
        // Arrange
        when(droneService.getDrone(-1))
                .thenReturn(ResponseEntity.badRequest().build());

        // Act & Assert
        mockMvc.perform(get("/api/v1/droneDetails/invalid"))
                .andExpect(status().isBadRequest());

        verify(droneService).getDrone(-1);
    }

    // ==================== QueryAsPath Endpoint Tests ====================

    @Test
    void testQueryAsPath_Success() throws Exception {
        // Arrange
        List<Integer> expectedIds = Arrays.asList(1, 2, 3);
        when(droneService.queryAsPath("capacity", "100"))
                .thenReturn(ResponseEntity.ok(expectedIds));

        // Act & Assert
        mockMvc.perform(get("/api/v1/queryAsPath/capacity/100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(3));

        verify(droneService).queryAsPath("capacity", "100");
    }

    @Test
    void testQueryAsPath_NoMatches() throws Exception {
        // Arrange
        when(droneService.queryAsPath("cooling", "invalid"))
                .thenReturn(ResponseEntity.ok(Collections.emptyList()));

        // Act & Assert
        mockMvc.perform(get("/api/v1/queryAsPath/cooling/invalid"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    // ==================== Query Endpoint Tests ====================

    @Test
    void testQuery_MultipleAttributes_Success() throws Exception {
        // Arrange
        List<QueryAttributeDto> queryAttrs = Arrays.asList(
                new QueryAttributeDto("capacity", ">=", "100"),
                new QueryAttributeDto("cooling", "=", "true")
        );
        List<String> expectedIds = Arrays.asList("1", "3");

        when(droneService.query(anyList()))
                .thenReturn(ResponseEntity.ok(expectedIds));

        // Act & Assert
        mockMvc.perform(post("/api/v1/query")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(queryAttrs)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2));

        verify(droneService).query(anyList());
    }

    @Test
    void testQuery_EmptyList_ReturnsEmptyResult() throws Exception {
        // Arrange
        when(droneService.query(anyList()))
                .thenReturn(ResponseEntity.ok(Collections.emptyList()));

        // Act & Assert
        mockMvc.perform(post("/api/v1/query")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[]"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    // ==================== QueryAvailableDrones Endpoint Tests ====================

    @Test
    void testQueryAvailableDrones_Success() throws Exception {
        // Arrange
        MedDispatchRecDto dispatchRec = createSampleMedDispatchRec();
        List<MedDispatchRecDto> requests = Collections.singletonList(dispatchRec);
        List<Integer> expectedIds = Arrays.asList(1, 2);

        when(droneService.getSuitableDrones(anyList()))
                .thenReturn(ResponseEntity.ok(expectedIds));

        // Act & Assert
        mockMvc.perform(post("/api/v1/queryAvailableDrones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requests)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2));

        verify(droneService).getSuitableDrones(anyList());
    }

    @Test
    void testQueryAvailableDrones_NoSuitableDrones() throws Exception {
        // Arrange
        when(droneService.getSuitableDrones(anyList()))
                .thenReturn(ResponseEntity.ok(Collections.emptyList()));

        // Act & Assert
        mockMvc.perform(post("/api/v1/queryAvailableDrones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[]"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    // ==================== CalcDeliveryPath Endpoint Tests ====================

    @Test
    void testCalcDeliveryPath_Success() throws Exception {
        // Arrange
        MedDispatchRecDto dispatchRec = createSampleMedDispatchRec();
        List<MedDispatchRecDto> requests = Collections.singletonList(dispatchRec);
        
        DeliveryPathReturnStructure deliveryPath = new DeliveryPathReturnStructure(
            25.5f, 
            10, 
            new ArrayList<>()
        );

        when(droneService.calcDeliveryPath(anyList(), isNull()))
                .thenReturn(ResponseEntity.ok(deliveryPath));

        // Act & Assert
        mockMvc.perform(post("/api/v1/calcDeliveryPath")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requests)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCost").value(25.5))
                .andExpect(jsonPath("$.totalMoves").value(10));

        verify(droneService).calcDeliveryPath(anyList(), isNull());
    }

    @Test
    void testCalcDeliveryPath_WithStrategy() throws Exception {
        // Arrange
        MedDispatchRecDto dispatchRec = createSampleMedDispatchRec();
        List<MedDispatchRecDto> requests = Collections.singletonList(dispatchRec);
        
        DeliveryPathReturnStructure deliveryPath = new DeliveryPathReturnStructure(
            20.0f, 
            8, 
            new ArrayList<>()
        );

        when(droneService.calcDeliveryPath(anyList(), eq("min_cost")))
                .thenReturn(ResponseEntity.ok(deliveryPath));

        // Act & Assert
        mockMvc.perform(post("/api/v1/calcDeliveryPath?strategy=min_cost")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requests)))
                .andExpect(status().isOk());

        verify(droneService).calcDeliveryPath(anyList(), eq("min_cost"));
    }

    // ==================== CalcDeliveryPathAsGeoJson Endpoint Tests ====================

    @Test
    void testCalcDeliveryPathAsGeoJson_Success() throws Exception {
        // Arrange
        MedDispatchRecDto dispatchRec = createSampleMedDispatchRec();
        List<MedDispatchRecDto> requests = Collections.singletonList(dispatchRec);
        
        GeoJsonLineStringDto geoJson = new GeoJsonLineStringDto(new ArrayList<>());

        when(droneService.calcDeliveryPathAsGeoJson(anyList()))
                .thenReturn(ResponseEntity.ok(geoJson));

        // Act & Assert
        mockMvc.perform(post("/api/v1/calcDeliveryPathAsGeoJson")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requests)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("FeatureCollection"));

        verify(droneService).calcDeliveryPathAsGeoJson(anyList());
    }

    // ==================== CalcMultiDroneDeliveryPathAsGeoJson Endpoint Tests ====================

    @Test
    void testCalcMultiDroneDeliveryPathAsGeoJson_Success() throws Exception {
        // Arrange
        MedDispatchRecDto dispatchRec = createSampleMedDispatchRec();
        List<MedDispatchRecDto> requests = Collections.singletonList(dispatchRec);
        
        GeoJsonLineStringDto geoJson = new GeoJsonLineStringDto(new ArrayList<>());

        when(droneService.calcMultiDroneDeliveryPathAsGeoJson(anyList(), isNull()))
                .thenReturn(ResponseEntity.ok(geoJson));

        // Act & Assert
        mockMvc.perform(post("/api/v1/calcMultiDroneDeliveryPathAsGeoJson")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requests)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("FeatureCollection"));

        verify(droneService).calcMultiDroneDeliveryPathAsGeoJson(anyList(), isNull());
    }

    @Test
    void testCalcMultiDroneDeliveryPathAsGeoJson_WithStrategy() throws Exception {
        // Arrange
        MedDispatchRecDto dispatchRec = createSampleMedDispatchRec();
        List<MedDispatchRecDto> requests = Collections.singletonList(dispatchRec);
        
        GeoJsonLineStringDto geoJson = new GeoJsonLineStringDto(new ArrayList<>());

        when(droneService.calcMultiDroneDeliveryPathAsGeoJson(anyList(), eq("balanced")))
                .thenReturn(ResponseEntity.ok(geoJson));

        // Act & Assert
        mockMvc.perform(post("/api/v1/calcMultiDroneDeliveryPathAsGeoJson?strategy=balanced")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requests)))
                .andExpect(status().isOk());

        verify(droneService).calcMultiDroneDeliveryPathAsGeoJson(anyList(), eq("balanced"));
    }

    // ==================== Helper Methods ====================

    private MedDispatchRecDto createSampleMedDispatchRec() {
        MedDispatchRecDto dto = new MedDispatchRecDto();
        dto.setId(1);
        
        Position destination = new Position();
        destination.setLongitude(BigDecimal.valueOf(-3.1878));
        destination.setLatitude(BigDecimal.valueOf(55.9445));
        dto.setDelivery(destination);
        
        Requirements requirements = new Requirements();
        requirements.setCooling(true);
        requirements.setCapacity(50.0f);
        dto.setRequirements(requirements);
        
        return dto;
    }
}
