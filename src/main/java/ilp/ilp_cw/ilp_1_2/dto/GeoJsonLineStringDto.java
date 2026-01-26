package ilp.ilp_cw.ilp_1_2.dto;

import java.util.List;

/**
 * DTO for representing a GeoJSON LineString FeatureCollection for a drone path.
 */
public class GeoJsonLineStringDto {
    private String type = "FeatureCollection";
    private List<Feature> features;

    public GeoJsonLineStringDto(List<Feature> features) {
        this.features = features;
    }

    public String getType() {
        return type;
    }

    public List<Feature> getFeatures() {
        return features;
    }

    public static class Feature {
        private String type = "Feature";
        private Properties properties;
        private Geometry geometry;

        public Feature(Properties properties, Geometry geometry) {
            this.properties = properties;
            this.geometry = geometry;
        }

        public String getType() {
            return type;
        }

        public Properties getProperties() {
            return properties;
        }

        public Geometry getGeometry() {
            return geometry;
        }
    }

    public static class Properties {
        private int droneId;
        private int totalMoves;

        public Properties(int droneId, int totalMoves) {
            this.droneId = droneId;
            this.totalMoves = totalMoves;
        }

        public int getDroneId() {
            return droneId;
        }

        public int getTotalMoves() {
            return totalMoves;
        }
    }

    public static class Geometry {
        private String type = "LineString";
        private List<List<Number>> coordinates;

        public Geometry(List<List<Number>> coordinates) {
            this.coordinates = coordinates;
        }

        public String getType() {
            return type;
        }

        public List<List<Number>> getCoordinates() {
            return coordinates;
        }
    }
}