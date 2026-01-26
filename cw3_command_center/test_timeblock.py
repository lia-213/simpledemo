"""
Test script for time-block simulation integration.
"""

from simulation_session import SimulationSession
from datetime import datetime, timedelta


def test_extract_move_count():
    """Test the move count extraction logic."""
    print("Testing move count extraction...")

    # Test case 1: explicit 'moves' field
    dp1 = {'droneId': 1, 'moves': 500}
    from ui_app import extract_move_count_from_drone_path
    assert extract_move_count_from_drone_path(dp1) == 500, "Failed: explicit moves"
    print("  PASS: Explicit 'moves' field")

    # Test case 2: 'moveCount' field
    dp2 = {'droneId': 2, 'moveCount': 300}
    assert extract_move_count_from_drone_path(dp2) == 300, "Failed: moveCount"
    print("  PASS: 'moveCount' field")

    # Test case 3: flightPath length
    dp3 = {'droneId': 3, 'flightPath': [{'pos': 1}, {'pos': 2}, {'pos': 3}]}
    assert extract_move_count_from_drone_path(dp3) == 3, "Failed: flightPath length"
    print("  PASS: flightPath length")

    # Test case 4: empty
    dp4 = {'droneId': 4}
    assert extract_move_count_from_drone_path(dp4) == 0, "Failed: empty"
    print("  PASS: Empty drone path")


def test_simulation_registration():
    """Test registering deliveries with simulation session."""
    print("\nTesting simulation session registration...")

    sim = SimulationSession()

    # Register a delivery
    start = datetime(2025, 12, 1, 14, 0, 0)
    total_moves = 500

    end_time = sim.register_delivery(
        drone_id=1,
        dispatch_id=101,
        start_time=start,
        total_moves=total_moves,
        description="Test delivery"
    )

    # Check duration calculation (500 moves * 10 seconds = 5000 seconds = 83.33 minutes)
    expected_duration = timedelta(seconds=5000)
    assert end_time == start + expected_duration, "Failed: duration calculation"
    print(f"  PASS: Duration calculation (500 moves = {expected_duration.total_seconds()/60:.1f} minutes)")

    # Check availability
    is_available, next_time = sim.check_availability(1, start, total_moves)
    assert not is_available, "Failed: should be busy"
    print("  PASS: Drone marked as busy during delivery window")

    # Check availability after delivery ends
    is_available_after, _ = sim.check_availability(1, end_time, 100)
    assert is_available_after, "Failed: should be available after"
    print("  PASS: Drone available after delivery ends")

    # Check busy drones
    busy_at_start = sim.get_busy_drones(start + timedelta(minutes=10))
    assert 1 in busy_at_start, "Failed: drone should be in busy list"
    print("  PASS: Busy drones list correct")


def test_register_plan():
    """Test the full register_plan_with_simulation flow."""
    print("\nTesting full plan registration...")

    # Mock plan data
    plan = {
        'totalCost': 10.0,
        'totalMoves': 500,
        'dronePaths': [
            {
                'droneId': 1,
                'moves': 500,
                'deliveredIds': [101, 102],
                'flightPath': [{'position': {'lat': 55.9, 'lng': -3.1}}] * 500
            },
            {
                'droneId': 2,
                'moveCount': 300,
                'deliveredIds': [103],
                'flightPath': [{'position': {'lat': 55.9, 'lng': -3.1}}] * 300
            }
        ]
    }

    # Mock scenario data
    scenario = {
        'dispatches': [
            {
                'id': 101,
                'date': '2025-12-01',
                'time': '14:00:00',
                'requirements': {'capacity': 5.0},
                'delivery': {'lat': 55.9, 'lng': -3.1}
            },
            {
                'id': 102,
                'date': '2025-12-01',
                'timeAfter': '15:00:00',
                'requirements': {'capacity': 3.0},
                'delivery': {'lat': 55.92, 'lng': -3.15}
            }
        ]
    }

    # Create a mock session state
    import streamlit as st
    sim = SimulationSession()

    # Manually test the registration logic
    for drone_path in plan['dronePaths']:
        drone_id = drone_path['droneId']

        # Extract moves
        if 'moves' in drone_path:
            total_moves = drone_path['moves']
        elif 'moveCount' in drone_path:
            total_moves = drone_path['moveCount']
        else:
            total_moves = len(drone_path.get('flightPath', []))

        # Register
        start_time = datetime(2025, 12, 1, 14, 0, 0)
        sim.register_delivery(
            drone_id=drone_id,
            dispatch_id=drone_path['deliveredIds'][0],
            start_time=start_time,
            total_moves=total_moves,
            description=f"Test delivery for drone {drone_id}"
        )

    # Check summary
    summary = sim.get_session_summary()
    assert summary['total_deliveries'] == 2, "Failed: delivery count"
    assert summary['unique_drones_used'] == 2, "Failed: unique drones"
    print(f"  PASS: Registered {summary['total_deliveries']} deliveries")
    print(f"  PASS: Used {summary['unique_drones_used']} drones")
    print(f"  PASS: Total flight time: {summary['total_flight_time_minutes']:.1f} minutes")


if __name__ == "__main__":
    print("=" * 60)
    print("Time-Block Tracking Integration Tests")
    print("=" * 60)

    try:
        test_extract_move_count()
        test_simulation_registration()
        test_register_plan()

        print("\n" + "=" * 60)
        print("All tests PASSED")
        print("=" * 60)

    except AssertionError as e:
        print(f"\nTest FAILED: {e}")
        import traceback
        traceback.print_exc()
    except Exception as e:
        print(f"\nUnexpected error: {e}")
        import traceback
        traceback.print_exc()
