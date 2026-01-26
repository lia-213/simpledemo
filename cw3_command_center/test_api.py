"""
Simple test script to verify API functionality.
Run this after starting the services to check connectivity.
"""

import requests
import json
from dotenv import load_dotenv
import os

load_dotenv()

API_BASE = os.getenv("DISPATCH_API_URL", "http://localhost:8000")
CW2_BASE = os.getenv("CW2_ENDPOINT", "http://localhost:8080")
ILP_BASE = os.getenv("ILPENDPOINT", "https://ilp-rest-2025-bvh6e9hschfagrgy.ukwest-01.azurewebsites.net")


def test_health():
    """Test API health check."""
    print("Testing API health...")
    try:
        response = requests.get(f"{API_BASE}/", timeout=5)
        print(f"✓ API is running: {response.json()['status']}")
        return True
    except Exception as e:
        print(f"✗ API health check failed: {e}")
        return False


def test_cw2_connection():
    """Test CW2 service connectivity."""
    print("\nTesting CW2 connection...")
    try:
        response = requests.get(f"{CW2_BASE}/api/v1/uid", timeout=5)
        print(f"✓ CW2 service connected, UID: {response.text}")
        return True
    except Exception as e:
        print(f"✗ CW2 connection failed: {e}")
        return False


def test_ilp_connection():
    """Test ILP REST connectivity."""
    print("\nTesting ILP REST connection...")
    try:
        response = requests.get(f"{ILP_BASE}/drones", timeout=5)
        drones = response.json()
        print(f"✓ ILP REST connected, found {len(drones)} drones")
        return True
    except Exception as e:
        print(f"✗ ILP REST connection failed: {e}")
        return False


def test_fleet_summary():
    """Test fleet summary endpoint."""
    print("\nTesting /summary/fleet endpoint...")
    try:
        response = requests.get(f"{API_BASE}/summary/fleet", timeout=10)
        summary = response.json()
        print(f"✓ Fleet summary retrieved:")
        print(f"  - Total drones: {summary['totalDrones']}")
        print(f"  - With cooling: {summary['dronesWithCooling']}")
        print(f"  - With heating: {summary['dronesWithHeating']}")
        return True
    except Exception as e:
        print(f"✗ Fleet summary failed: {e}")
        return False


def test_plan_endpoint():
    """Test plan creation endpoint."""
    print("\nTesting /plan endpoint...")

    # Simple test scenario
    scenario = {
        "dispatches": [
            {
                "id": 1,
                "date": "2025-01-15",
                "time": "14:00:00",
                "requirements": {
                    "capacity": 3.0,
                    "cooling": True,
                    "heating": False,
                    "maxCost": 50.0
                },
                "delivery": {
                    "lng": -3.1883,
                    "lat": 55.9217
                }
            }
        ],
        "strategy": "min_cost"
    }

    try:
        response = requests.post(
            f"{API_BASE}/plan",
            json=scenario,
            timeout=30
        )
        plan = response.json()
        print(f"✓ Plan created:")
        print(f"  - Total cost: £{plan['totalCost']:.2f}")
        print(f"  - Total moves: {plan['totalMoves']}")
        print(f"  - Drones used: {len(plan['dronePaths'])}")
        return True
    except Exception as e:
        print(f"✗ Plan creation failed: {e}")
        return False


def test_what_if():
    """Test what-if comparison endpoint."""
    print("\nTesting /what-if endpoint...")

    request = {
        "dispatches": [
            {
                "id": 1,
                "date": "2025-01-15",
                "time": "14:00:00",
                "requirements": {
                    "capacity": 3.0,
                    "cooling": True
                },
                "delivery": {
                    "lng": -3.1883,
                    "lat": 55.9217
                }
            }
        ],
        "strategyA": "min_cost",
        "strategyB": "min_moves"
    }

    try:
        response = requests.post(
            f"{API_BASE}/what-if",
            json=request,
            timeout=30
        )
        result = response.json()
        print(f"✓ What-if comparison complete:")
        print(f"  - Plan A cost: £{result['planA']['totalCost']:.2f}")
        print(f"  - Plan B cost: £{result['planB']['totalCost']:.2f}")
        print(f"  - Difference: £{result['delta']['costDifference']:.2f}")
        return True
    except Exception as e:
        print(f"✗ What-if comparison failed: {e}")
        return False


def main():
    """Run all tests."""
    print("=" * 60)
    print("MedSupplyDrones Command Center - API Test Suite")
    print("=" * 60)

    results = []

    # Run tests
    results.append(("API Health", test_health()))
    results.append(("CW2 Connection", test_cw2_connection()))
    results.append(("ILP REST Connection", test_ilp_connection()))
    results.append(("Fleet Summary", test_fleet_summary()))
    results.append(("Plan Endpoint", test_plan_endpoint()))
    results.append(("What-If Endpoint", test_what_if()))

    # Summary
    print("\n" + "=" * 60)
    print("Test Summary")
    print("=" * 60)

    passed = sum(1 for _, result in results if result)
    total = len(results)

    for test_name, result in results:
        status = "PASS" if result else "FAIL"
        print(f"{test_name:.<40} {status}")

    print("=" * 60)
    print(f"Tests passed: {passed}/{total}")

    if passed == total:
        print("✓ All tests passed! System is ready.")
    else:
        print("✗ Some tests failed. Check configuration and services.")

    return passed == total


if __name__ == "__main__":
    success = main()
    exit(0 if success else 1)
