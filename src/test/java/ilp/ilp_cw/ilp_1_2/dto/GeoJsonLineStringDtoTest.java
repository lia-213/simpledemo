package ilp.ilp_cw.ilp_1_2.dto;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GeoJsonLineStringDtoTest {

    private List<List<Number>> coordinates;
    private GeoJsonLineStringDto.Geometry geometry;
    private GeoJsonLineStringDto.Properties properties;
    private GeoJsonLineStringDto.Feature feature;
    private List<GeoJsonLineStringDto.Feature> features;

    @BeforeEach
    void setUp() {
        coordinates = new ArrayList<>();
        coordinates.add(Arrays.asList(-3.186874, 55.944494));
        coordinates.add(Arrays.asList(-3.186724, 55.944494));
        
        geometry = new GeoJsonLineStringDto.Geometry(coordinates);
        properties = new GeoJsonLineStringDto.Properties(1, 10);
        feature = new GeoJsonLineStringDto.Feature(properties, geometry);
        features = new ArrayList<>();
        features.add(feature);
    }

    // Tests for GeoJsonLineStringDto
    @Test
    void testGeoJsonLineStringDto_Constructor() {
        GeoJsonLineStringDto dto = new GeoJsonLineStringDto(features);

        assertNotNull(dto);
        assertEquals(features, dto.getFeatures());
    }

    @Test
    void testGeoJsonLineStringDto_GetType() {
        GeoJsonLineStringDto dto = new GeoJsonLineStringDto(features);

        assertEquals("FeatureCollection", dto.getType());
    }

    @Test
    void testGeoJsonLineStringDto_GetFeatures() {
        GeoJsonLineStringDto dto = new GeoJsonLineStringDto(features);

        assertEquals(features, dto.getFeatures());
        assertSame(features, dto.getFeatures());
    }

    @Test
    void testGeoJsonLineStringDto_WithEmptyFeatures() {
        List<GeoJsonLineStringDto.Feature> emptyFeatures = new ArrayList<>();
        GeoJsonLineStringDto dto = new GeoJsonLineStringDto(emptyFeatures);

        assertNotNull(dto.getFeatures());
        assertTrue(dto.getFeatures().isEmpty());
        assertEquals("FeatureCollection", dto.getType());
    }

    @Test
    void testGeoJsonLineStringDto_WithMultipleFeatures() {
        GeoJsonLineStringDto.Properties props2 = new GeoJsonLineStringDto.Properties(2, 15);
        GeoJsonLineStringDto.Geometry geom2 = new GeoJsonLineStringDto.Geometry(coordinates);
        GeoJsonLineStringDto.Feature feature2 = new GeoJsonLineStringDto.Feature(props2, geom2);

        List<GeoJsonLineStringDto.Feature> multipleFeatures = Arrays.asList(feature, feature2);
        GeoJsonLineStringDto dto = new GeoJsonLineStringDto(multipleFeatures);

        assertEquals(2, dto.getFeatures().size());
        assertEquals(multipleFeatures, dto.getFeatures());
    }

    @Test
    void testGeoJsonLineStringDto_WithNullFeatures() {
        GeoJsonLineStringDto dto = new GeoJsonLineStringDto(null);

        assertNull(dto.getFeatures());
        assertEquals("FeatureCollection", dto.getType());
    }

    // Tests for Feature
    @Test
    void testFeature_Constructor() {
        GeoJsonLineStringDto.Feature f = new GeoJsonLineStringDto.Feature(properties, geometry);

        assertNotNull(f);
        assertEquals(properties, f.getProperties());
        assertEquals(geometry, f.getGeometry());
    }

    @Test
    void testFeature_GetType() {
        assertEquals("Feature", feature.getType());
    }

    @Test
    void testFeature_GetProperties() {
        assertEquals(properties, feature.getProperties());
        assertSame(properties, feature.getProperties());
    }

    @Test
    void testFeature_GetGeometry() {
        assertEquals(geometry, feature.getGeometry());
        assertSame(geometry, feature.getGeometry());
    }

    @Test
    void testFeature_WithNullProperties() {
        GeoJsonLineStringDto.Feature f = new GeoJsonLineStringDto.Feature(null, geometry);

        assertNull(f.getProperties());
        assertEquals(geometry, f.getGeometry());
        assertEquals("Feature", f.getType());
    }

    @Test
    void testFeature_WithNullGeometry() {
        GeoJsonLineStringDto.Feature f = new GeoJsonLineStringDto.Feature(properties, null);

        assertEquals(properties, f.getProperties());
        assertNull(f.getGeometry());
        assertEquals("Feature", f.getType());
    }

    @Test
    void testFeature_WithBothNull() {
        GeoJsonLineStringDto.Feature f = new GeoJsonLineStringDto.Feature(null, null);

        assertNull(f.getProperties());
        assertNull(f.getGeometry());
        assertEquals("Feature", f.getType());
    }

    // Tests for Properties
    @Test
    void testProperties_Constructor() {
        GeoJsonLineStringDto.Properties props = new GeoJsonLineStringDto.Properties(5, 20);

        assertNotNull(props);
        assertEquals(5, props.getDroneId());
        assertEquals(20, props.getTotalMoves());
    }

    @Test
    void testProperties_GetDroneId() {
        assertEquals(1, properties.getDroneId());
    }

    @Test
    void testProperties_GetTotalMoves() {
        assertEquals(10, properties.getTotalMoves());
    }

    @Test
    void testProperties_WithZeroValues() {
        GeoJsonLineStringDto.Properties props = new GeoJsonLineStringDto.Properties(0, 0);

        assertEquals(0, props.getDroneId());
        assertEquals(0, props.getTotalMoves());
    }

    @Test
    void testProperties_WithNegativeValues() {
        GeoJsonLineStringDto.Properties props = new GeoJsonLineStringDto.Properties(-1, -5);

        assertEquals(-1, props.getDroneId());
        assertEquals(-5, props.getTotalMoves());
    }

    @Test
    void testProperties_WithLargeValues() {
        GeoJsonLineStringDto.Properties props = new GeoJsonLineStringDto.Properties(
                Integer.MAX_VALUE, Integer.MAX_VALUE);

        assertEquals(Integer.MAX_VALUE, props.getDroneId());
        assertEquals(Integer.MAX_VALUE, props.getTotalMoves());
    }

    // Tests for Geometry
    @Test
    void testGeometry_Constructor() {
        GeoJsonLineStringDto.Geometry geom = new GeoJsonLineStringDto.Geometry(coordinates);

        assertNotNull(geom);
        assertEquals(coordinates, geom.getCoordinates());
    }

    @Test
    void testGeometry_GetType() {
        assertEquals("LineString", geometry.getType());
    }

    @Test
    void testGeometry_GetCoordinates() {
        assertEquals(coordinates, geometry.getCoordinates());
        assertSame(coordinates, geometry.getCoordinates());
    }

    @Test
    void testGeometry_WithEmptyCoordinates() {
        List<List<Number>> emptyCoords = new ArrayList<>();
        GeoJsonLineStringDto.Geometry geom = new GeoJsonLineStringDto.Geometry(emptyCoords);

        assertNotNull(geom.getCoordinates());
        assertTrue(geom.getCoordinates().isEmpty());
        assertEquals("LineString", geom.getType());
    }

    @Test
    void testGeometry_WithNullCoordinates() {
        GeoJsonLineStringDto.Geometry geom = new GeoJsonLineStringDto.Geometry(null);

        assertNull(geom.getCoordinates());
        assertEquals("LineString", geom.getType());
    }

    @Test
    void testGeometry_WithSingleCoordinate() {
        List<List<Number>> singleCoord = Collections.singletonList(
                Arrays.asList(-3.186874, 55.944494));
        GeoJsonLineStringDto.Geometry geom = new GeoJsonLineStringDto.Geometry(singleCoord);

        assertEquals(1, geom.getCoordinates().size());
        assertEquals(singleCoord, geom.getCoordinates());
    }

    @Test
    void testGeometry_WithMultipleCoordinates() {
        List<List<Number>> multipleCoords = Arrays.asList(
                Arrays.asList(-3.186874, 55.944494),
                Arrays.asList(-3.186724, 55.944494),
                Arrays.asList(-3.186574, 55.944494),
                Arrays.asList(-3.186424, 55.944494)
        );
        GeoJsonLineStringDto.Geometry geom = new GeoJsonLineStringDto.Geometry(multipleCoords);

        assertEquals(4, geom.getCoordinates().size());
        assertEquals(multipleCoords, geom.getCoordinates());
    }

    @Test
    void testGeometry_WithThreeCoordinatesPerPoint() {
        // Some GeoJSON includes altitude as third coordinate
        List<List<Number>> coordsWith3D = Arrays.asList(
                Arrays.asList(-3.186874, 55.944494, 100.0),
                Arrays.asList(-3.186724, 55.944494, 105.5)
        );
        GeoJsonLineStringDto.Geometry geom = new GeoJsonLineStringDto.Geometry(coordsWith3D);

        assertEquals(2, geom.getCoordinates().size());
        assertEquals(3, geom.getCoordinates().get(0).size());
        assertEquals(coordsWith3D, geom.getCoordinates());
    }

    // Integration tests
    @Test
    void testFullGeoJsonStructure() {
        List<List<Number>> coords = Arrays.asList(
                Arrays.asList(-3.186874, 55.944494),
                Arrays.asList(-3.186724, 55.944494),
                Arrays.asList(-3.186574, 55.944494)
        );

        GeoJsonLineStringDto.Geometry geom = new GeoJsonLineStringDto.Geometry(coords);
        GeoJsonLineStringDto.Properties props = new GeoJsonLineStringDto.Properties(42, 25);
        GeoJsonLineStringDto.Feature feat = new GeoJsonLineStringDto.Feature(props, geom);

        List<GeoJsonLineStringDto.Feature> feats = Collections.singletonList(feat);
        GeoJsonLineStringDto dto = new GeoJsonLineStringDto(feats);

        // Verify the entire structure
        assertEquals("FeatureCollection", dto.getType());
        assertEquals(1, dto.getFeatures().size());

        GeoJsonLineStringDto.Feature retrievedFeature = dto.getFeatures().get(0);
        assertEquals("Feature", retrievedFeature.getType());
        assertEquals(42, retrievedFeature.getProperties().getDroneId());
        assertEquals(25, retrievedFeature.getProperties().getTotalMoves());
        assertEquals("LineString", retrievedFeature.getGeometry().getType());
        assertEquals(3, retrievedFeature.getGeometry().getCoordinates().size());
    }

    @Test
    void testMultipleDronesInFeatureCollection() {
        // Create features for multiple drones
        List<List<Number>> coords1 = Arrays.asList(
                Arrays.asList(-3.186874, 55.944494),
                Arrays.asList(-3.186724, 55.944494)
        );
        List<List<Number>> coords2 = Arrays.asList(
                Arrays.asList(-3.187000, 55.945000),
                Arrays.asList(-3.186850, 55.945000)
        );

        GeoJsonLineStringDto.Feature feat1 = new GeoJsonLineStringDto.Feature(
                new GeoJsonLineStringDto.Properties(1, 10),
                new GeoJsonLineStringDto.Geometry(coords1)
        );

        GeoJsonLineStringDto.Feature feat2 = new GeoJsonLineStringDto.Feature(
                new GeoJsonLineStringDto.Properties(2, 15),
                new GeoJsonLineStringDto.Geometry(coords2)
        );

        List<GeoJsonLineStringDto.Feature> feats = Arrays.asList(feat1, feat2);
        GeoJsonLineStringDto dto = new GeoJsonLineStringDto(feats);

        assertEquals(2, dto.getFeatures().size());
        assertEquals(1, dto.getFeatures().get(0).getProperties().getDroneId());
        assertEquals(2, dto.getFeatures().get(1).getProperties().getDroneId());
    }

    @Test
    void testTypeFieldsAreConstant() {
        // Verify that type fields are always set correctly regardless of constructor parameters
        GeoJsonLineStringDto dto = new GeoJsonLineStringDto(features);
        GeoJsonLineStringDto.Feature feat = new GeoJsonLineStringDto.Feature(properties, geometry);
        GeoJsonLineStringDto.Geometry geom = new GeoJsonLineStringDto.Geometry(coordinates);

        assertEquals("FeatureCollection", dto.getType());
        assertEquals("Feature", feat.getType());
        assertEquals("LineString", geom.getType());
    }

    @Test
    void testCoordinatesWithDifferentNumberTypes() {
        // Test with various Number subclasses
        List<List<Number>> mixedCoords = Arrays.asList(
                Arrays.asList(1, 2.0),           // Integer and Double
                Arrays.asList(3.0f, 4L),         // Float and Long
                Arrays.asList(-5.5, 6)           // Double and Integer
        );

        GeoJsonLineStringDto.Geometry geom = new GeoJsonLineStringDto.Geometry(mixedCoords);

        assertEquals(3, geom.getCoordinates().size());
        assertEquals(mixedCoords, geom.getCoordinates());
    }

    @Test
    void testMinimalValidGeoJson() {
        // Absolute minimum: empty features list
        GeoJsonLineStringDto dto = new GeoJsonLineStringDto(new ArrayList<>());

        assertEquals("FeatureCollection", dto.getType());
        assertNotNull(dto.getFeatures());
        assertEquals(0, dto.getFeatures().size());
    }

    @Test
    void testLargePathWithManyCoordinates() {
        // Test with a large number of coordinates
        List<List<Number>> largeCoords = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            largeCoords.add(Arrays.asList(-3.0 + i * 0.0001, 55.0 + i * 0.0001));
        }

        GeoJsonLineStringDto.Geometry geom = new GeoJsonLineStringDto.Geometry(largeCoords);
        GeoJsonLineStringDto.Properties props = new GeoJsonLineStringDto.Properties(1, 1000);
        GeoJsonLineStringDto.Feature feat = new GeoJsonLineStringDto.Feature(props, geom);
        GeoJsonLineStringDto dto = new GeoJsonLineStringDto(Collections.singletonList(feat));

        assertEquals(1000, dto.getFeatures().get(0).getGeometry().getCoordinates().size());
        assertEquals(1000, dto.getFeatures().get(0).getProperties().getTotalMoves());
    }
}
