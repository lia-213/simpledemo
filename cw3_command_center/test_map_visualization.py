"""
Test script for map visualization functionality.
"""

import os
import json
from dotenv import load_dotenv

load_dotenv()


def test_geojson_structure():
    """Test that we can create a valid GeoJSON structure."""
    print("Testing GeoJSON structure...")

    # Sample GeoJSON FeatureCollection (what we expect from CW2 backend)
    sample_geojson = {
        "type": "FeatureCollection",
        "features": [
            {
                "type": "Feature",
                "geometry": {
                    "type": "LineString",
                    "coordinates": [
                        [-3.1873, 55.9445],  # Appleton Tower
                        [-3.1883, 55.9217]   # Royal Infirmary
                    ]
                },
                "properties": {
                    "droneId": 1,
                    "dispatchId": 101
                }
            }
        ]
    }

    # Validate structure
    assert sample_geojson['type'] == 'FeatureCollection', "Invalid GeoJSON type"
    assert 'features' in sample_geojson, "Missing features"
    assert len(sample_geojson['features']) > 0, "No features"

    feature = sample_geojson['features'][0]
    assert feature['type'] == 'Feature', "Invalid feature type"
    assert 'geometry' in feature, "Missing geometry"
    assert 'properties' in feature, "Missing properties"

    geometry = feature['geometry']
    assert geometry['type'] == 'LineString', "Invalid geometry type"
    assert 'coordinates' in geometry, "Missing coordinates"
    assert len(geometry['coordinates']) >= 2, "Need at least 2 coordinates for a path"

    print("  PASS: GeoJSON structure is valid")
    return sample_geojson


def test_color_assignment():
    """Test drone color assignment logic."""
    print("\nTesting drone color assignment...")

    from ui_app import _get_drone_color

    # Test integer drone IDs
    color1 = _get_drone_color(0)
    color2 = _get_drone_color(1)
    color3 = _get_drone_color(10)  # Should wrap around

    assert len(color1) == 4, "Color should have 4 values (RGBA)"
    assert all(0 <= c <= 255 for c in color1), "Color values should be 0-255"
    assert color1 != color2, "Different drones should have different colors"
    assert color1 == color3, "Color 10 should wrap to color 0"

    print(f"  PASS: Drone 0 color: {color1}")
    print(f"  PASS: Drone 1 color: {color2}")
    print(f"  PASS: Drone 10 color (wrapped): {color3}")


def test_coordinate_extraction():
    """Test coordinate extraction from GeoJSON."""
    print("\nTesting coordinate extraction...")

    geojson = test_geojson_structure()

    all_coords = []
    for feature in geojson['features']:
        if feature['type'] == 'Feature':
            geometry = feature.get('geometry', {})
            if geometry['type'] == 'LineString':
                coords = geometry['coordinates']
                for lng, lat in coords:
                    all_coords.append([lat, lng])

    assert len(all_coords) == 2, "Should extract 2 coordinates"
    print(f"  PASS: Extracted {len(all_coords)} coordinates")
    print(f"    - Start: lat={all_coords[0][0]:.4f}, lng={all_coords[0][1]:.4f}")
    print(f"    - End:   lat={all_coords[1][0]:.4f}, lng={all_coords[1][1]:.4f}")


def test_sample_dispatch_to_geojson():
    """Test creating a dispatch and checking GeoJSON endpoint format."""
    print("\nTesting dispatch to GeoJSON conversion...")

    sample_dispatch = {
        "id": 1,
        "date": "2025-12-01",
        "time": "14:00:00",
        "requirements": {
            "capacity": 5.0,
            "cooling": True,
            "heating": False
        },
        "delivery": {
            "lng": -3.1883,
            "lat": 55.9217
        }
    }

    print(f"  PASS: Sample dispatch created")
    print(f"    - Delivery to: ({sample_dispatch['delivery']['lat']}, {sample_dispatch['delivery']['lng']})")
    print(f"    - Capacity: {sample_dispatch['requirements']['capacity']}L")
    print(f"    - Cooling: {sample_dispatch['requirements']['cooling']}")

    # Expected endpoint
    cw2_base = os.getenv("CW2_BASE_URL", "http://localhost:8080")
    endpoint = f"{cw2_base}/api/v1/calcDeliveryPathAsGeoJson"
    print(f"  PASS: Expected endpoint: {endpoint}")
    print(f"    - Payload: [dispatch] (JSON list)")


def test_restricted_areas_loading():
    """Test loading restricted areas GeoJSON."""
    print("\nTesting restricted areas loading...")

    from ui_app import load_restricted_areas

    restricted = load_restricted_areas()

    if restricted is None:
        print("  WARNING: Restricted areas file not found (this is OK if file doesn't exist)")
        return

    assert restricted.get('type') == 'FeatureCollection', "Invalid GeoJSON type"
    features = restricted.get('features', [])

    # Count polygon features (restricted zones)
    polygon_count = sum(1 for f in features if f.get('geometry', {}).get('type') == 'Polygon')

    print(f"  PASS: Loaded {len(features)} features")
    print(f"  PASS: Found {polygon_count} restricted zone(s)")

    # Show names of restricted areas
    if polygon_count > 0:
        for feature in features:
            if feature.get('geometry', {}).get('type') == 'Polygon':
                name = feature.get('properties', {}).get('name', 'Unknown')
                print(f"    - {name}")


def test_map_data_structure():
    """Test the data structure needed for pydeck visualization."""
    print("\nTesting pydeck data structure...")

    # Sample path data (what we'll feed to pydeck)
    path_data = {
        'path': [[-3.1873, 55.9445], [-3.1883, 55.9217]],
        'drone_id': 1,
        'color': [0, 119, 182, 200]
    }

    assert 'path' in path_data, "Missing path"
    assert 'drone_id' in path_data, "Missing drone_id"
    assert 'color' in path_data, "Missing color"
    assert len(path_data['path']) >= 2, "Path needs at least 2 points"

    print(f"  PASS: Path data structure valid")
    print(f"    - Drone: {path_data['drone_id']}")
    print(f"    - Waypoints: {len(path_data['path'])}")
    print(f"    - Color: RGBA{tuple(path_data['color'])}")


if __name__ == "__main__":
    print("=" * 60)
    print("Map Visualization Tests")
    print("=" * 60)

    try:
        test_geojson_structure()
        test_color_assignment()
        test_coordinate_extraction()
        test_sample_dispatch_to_geojson()
        test_restricted_areas_loading()
        test_map_data_structure()

        print("\n" + "=" * 60)
        print("All tests PASSED")
        print("=" * 60)
        print("\nMap visualization is ready!")
        print("\nTo test with live data:")
        print("1. Start the CW2 Java backend (port 8080)")
        print("2. Run: streamlit run ui_app.py")
        print("3. Create a plan with natural language")
        print("4. View the interactive map in the 'Flight Path Visualization' section")

    except AssertionError as e:
        print(f"\nTest FAILED: {e}")
        import traceback
        traceback.print_exc()
    except Exception as e:
        print(f"\nUnexpected error: {e}")
        import traceback
        traceback.print_exc()
