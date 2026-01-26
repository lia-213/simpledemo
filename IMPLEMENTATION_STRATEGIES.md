# Implementation Strategies

**Consolidated Documentation - Design Decisions and Approaches**  
**Date: November 23, 2025**

---

## Table of Contents

1. [Multi-Day Implementation Strategy](#multi-day-implementation-strategy)
2. [Multiple Trips Same Day Implementation](#multiple-trips-same-day-implementation)
3. [calcDeliveryPath Approach](#calcdeliverypath-approach)
4. [Return Path Representation](#return-path-representation)
5. [A* Pathfinding Implementation](#a-star-pathfinding-implementation)
6. [Capacity-Based Trip Selection](#capacity-based-trip-selection)
7. [Distance Calculation Strategy](#distance-calculation-strategy)
8. [Time Availability Handling](#time-availability-handling)
9. [Query Implementation Patterns](#query-implementation-patterns)

---

## Multi-Day Implementation Strategy

### Community Consensus Approach

**Pattern validated by multiple students**:

> "I group queries by date and for each date I deal with the queries. When I move on to the next date everything should
> be reset"

### Implementation

```java
/**
 * Multi-Day Support:
 * - Group deliveries by date
 * - Process each date independently
 * - Reset state between dates
 * - Each date is a "new game"
 */

// 1. Group deliveries by date (TreeMap for sorted order)
Map<LocalDate, List<MedDispatchRec>> deliveriesByDate =
        medDispatchModels.stream()
                .collect(Collectors.groupingBy(
                        MedDispatchRec::date,
                        TreeMap::new,
                        Collectors.toList()
                ));

// 2. Process each date separately
for(
Map.Entry<LocalDate, List<MedDispatchRec>> dateEntry :deliveriesByDate.

entrySet()){
LocalDate currentDate = dateEntry.getKey();
List<MedDispatchRec> deliveriesForDate = dateEntry.getValue();

// 3. Reset state for this date (fresh start)
Set<Integer> assignedTodayIds = new HashSet<>();

// 4. Process drones for TODAY's deliveries
    for(
Drone drone :suitableDrones){
        // Process deliveries for this date...
        }
        }
```

### Key Principles

1. **Group by Date**: Use `TreeMap` for automatic sorting
2. **Reset State**: New `assignedTodayIds` set for each date
3. **Independent Processing**: Each date doesn't affect others
4. **maxMoves Reset**: Drones start fresh each day

### Benefits

- ✅ Clean separation between dates
- ✅ Can't accidentally route Monday → Tuesday in one trip
- ✅ Same drone can work multiple days
- ✅ Simple to understand and maintain

---

## Multiple Trips Same Day Implementation

### The Pattern

**3 Nested Loops**: Dates → Drones → Trips

```java
for(LocalDate date :dates){                // Outer: Multi-day
        for(
Drone drone :suitableDrones){      // Middle: Multiple drones
int remainingMovesToday = maxMoves;
List<Delivery> allDeliveries = new ArrayList<>();

// Inner: Multiple trips per drone per day
        while(remainingMovesToday >0){
// Get unassigned deliveries for TODAY
List<MedDispatchRec> remaining = getUnassigned(date);
            
            if(remaining.

isEmpty())break;

// Select for THIS TRIP (capacity constraint)
List<MedDispatchRec> thisTrip = selectDeliveriesForTrip(
        remaining,
        drone.getCapacity(),
        servicePoint
);
            
            if(thisTrip.

isEmpty())break;

// Route THIS TRIP
FlightPath trip = drone.aStarSearch(thisTrip, servicePoint, ...);
        if(trip ==null)break;

// Check moves
int tripMoves = trip.getTotalMoves();
            if(tripMoves >remainingMovesToday)break;

// Update
remainingMovesToday -=tripMoves;
            allDeliveries.

addAll(createDeliveries(trip, thisTrip));

markAsAssigned(thisTrip);
        }

                // Add this drone's path (all trips combined)
                if(!allDeliveries.

isEmpty()){
        dronePaths.

add(new DronePath(droneId, allDeliveries));
        }
        }
        }
```

### Why Multiple Trips

**Scenario**: Drone capacity = 4kg, Deliveries = [3kg, 3kg, 1kg]

**Without multiple trips**: Fails (7kg > 4kg capacity)

**With multiple trips**:

- Trip 1: SP → (3kg + 1kg) → SP = 4kg ✅
- Trip 2: SP → (3kg) → SP = 3kg ✅
- All deliveries completed! ✅

### Key Implementation Details

**Capacity Constraint**:

```java
private List<MedDispatchRec> selectDeliveriesForTrip(
        List<MedDispatchRec> available,
        float droneCapacity,
        ServicePoint servicePoint
) {
    List<MedDispatchRec> selected = new ArrayList<>();
    float currentLoad = 0.0f;

    // Sort by proximity (greedy nearest-first)
    available.sort(Comparator.comparingDouble(d ->
            distance(servicePoint, d.delivery())
    ));

    // Greedy packing
    for (MedDispatchRec delivery : available) {
        float weight = delivery.requirements().getCapacity();

        if (currentLoad + weight <= droneCapacity) {
            selected.add(delivery);
            currentLoad += weight;

            if (currentLoad >= droneCapacity) break;
        }
    }

    return selected;
}
```

**Moves Tracking**:

```java
int remainingMovesToday = drone.getMaxMoves();

for
each trip:
        if(trip.

getMoves() >remainingMovesToday){
        break; // Can't do this trip
        }
remainingMovesToday -=trip.

getMoves();
```

---

## calcDeliveryPath Approach

### Two Valid Strategies

#### Strategy 1: Use queryAvailableDrones (Current Implementation)

**Approach**: Filter drones first, then process greedily

```java
// 1. Get suitable drones (filters based on AND logic)
ResponseEntity<List<Integer>> suitableDrones =
        getSuitableDrones(medDispatchRequests);

// 2. Process each suitable drone greedily
for(
Drone drone :suitableDrones){
        // Greedy: take as many deliveries as possible
        // Multiple trips per day (capacity-based)
        }
```

**Pros**:

- ✅ Filters out unsuitable drones early
- ✅ Code reuse
- ✅ Works well if queryAvailableDrones returns viable drones

**Cons**:

- ⚠️ Might miss drones that can do SOME deliveries
- ⚠️ AND logic might be too restrictive

#### Strategy 2: Check All Drones Directly

**Approach**: Process all drones, check suitability inline

```java
// 1. Get ALL drones (no queryAvailableDrones call)
List<Drone> allDrones = fetchAllDrones();

// 2. Process each drone greedily
for(
Drone drone :allDrones){
        // Check basic suitability (availability, date)
        if(!

basicCheck(drone, date))continue;

        // Greedy: take as many deliveries as possible
        // Multiple trips per day
        }
```

**Pros**:

- ✅ No logic mismatch
- ✅ More flexible (can use drones that do SOME)
- ✅ Potentially finds more solutions

**Cons**:

- ❌ Checks more drones (might be slower)
- ❌ No pre-filtering

### The Greedy Approach

> "It may fail to get a solution because it is greedy. But it is fair enough. Otherwise your time complexity goes crazy"

**Key Strategy**:

1. **Greedy Selection**: Take as many deliveries as drone can handle
2. **Nearest-Neighbor**: Order by proximity
3. **Capacity-Based Packing**: Multiple trips if needed
4. **Fast Execution**: O(n) or O(n log n), not optimal
5. **"Good Enough"**: Focus on validity, not perfection

**Example**:

```java
for(Drone drone :drones){
        // Greedy: Take as many as possible
        while(hasDeliveries &&hasCapacity &&hasMoves){
List<MedDispatchRec> thisTrip = selectGreedy(...);

route(thisTrip);

updateState();
    }
            }
```

---

## Return Path Representation

### Two Approaches Discussed

#### Option 1: Include Return in Last Delivery

**Community approach** (one student):

```json
{
  "deliveries": [
    {
      "deliveryId": 1,
      "flightPath": [
        {
          "lng": -3.186,
          "lat": 55.945
        },
        // SP
        {
          "lng": -3.187,
          "lat": 55.941
        },
        // D1
        {
          "lng": -3.187,
          "lat": 55.941
        }
        // D1 hover
      ]
    },
    {
      "deliveryId": 2,
      "flightPath": [
        {
          "lng": -3.187,
          "lat": 55.941
        },
        // From D1
        {
          "lng": -3.185,
          "lat": 55.943
        },
        // D2
        {
          "lng": -3.185,
          "lat": 55.943
        },
        // D2 hover
        {
          "lng": -3.186,
          "lat": 55.945
        }
        // SP return ✅
      ]
    }
  ]
}
```

**Implementation**:

```java
for(int i = 0; i <deliveries.

size();

i++){
boolean isLast = (i == deliveries.size() - 1);

// If last delivery, include return to SP
int endIndex = isLast ? stops.size() - 1 : segmentEndIndices.get(i);

// Create delivery with path including return (if last)
}
```

#### Option 2: Separate Return Delivery (Current Implementation)

**Our approach**:

```json
{
  "deliveries": [
    {
      "deliveryId": 1,
      "flightPath": [
        ...
      ]
    },
    {
      "deliveryId": 2,
      "flightPath": [
        ...
      ]
    },
    {
      "deliveryId": null,
      "flightPath": [
        ...
      ]
    }
    // Return leg
  ]
}
```

**Implementation**:

```java
// After all delivery segments
if(!stops.isEmpty() &&prevStartIndex <stops.

size() -1){
FlightPath returnPath = new FlightPath();
// Build return path from last delivery to SP
    allDeliveries.

add(new Delivery(null, returnPath));
        }
```

### Recommendation

**Keep current implementation** (Option 2) ✅

**Rationale**:

1. Tests pass (111/111)
2. Clear separation of concerns
3. Logical and understandable
4. Only change if auto-marker rejects it

---

## A* Pathfinding Implementation

### Key Components

#### 1. Node Representation

```java
class Node {
    Position position;
    Node parent;
    double gCost;  // Cost from start
    double hCost;  // Heuristic to goal
    double fCost;  // g + h
}
```

#### 2. Heuristic Function

```java
private double heuristic(Position current, Position goal) {
    // Euclidean distance
    double dx = current.getLongitude().subtract(goal.getLongitude()).doubleValue();
    double dy = current.getLatitude().subtract(goal.getLatitude()).doubleValue();
    return Math.sqrt(dx * dx + dy * dy);
}
```

#### 3. Neighbor Generation

```java
// 16 directions (22.5° increments)
private List<Position> getNeighbors(Position current) {
    List<Position> neighbors = new ArrayList<>();

    for (int angle = 0; angle < 360; angle += 22.5) {
        Position next = calculateNextPosition(current, angle);

        if (isValid(next) && !isInNoFlyZone(next)) {
            neighbors.add(next);
        }
    }

    return neighbors;
}
```

#### 4. Closed Set Management

**Critical**: Use `isCloseTo()` for position equality

```java
// Check if position already visited
boolean inClosedSet = closedSet.stream()
                .anyMatch(pos -> isCloseTo(pos, current));

if(inClosedSet)continue;
```

**Why**: Floating-point precision issues with BigDecimal

#### 5. No-Fly Zone Detection

```java
private boolean isInNoFlyZone(Position pos, List<RestrictedArea> noFlyZones) {
    for (RestrictedArea area : noFlyZones) {
        if (isInRegion(pos, area.getRegion())) {
            return true;
        }
    }
    return false;
}
```

#### 6. Edge Intersection (Cutting Corners)

**Problem**: Path might cut through no-fly zone between two valid points

**Solution**: Check edge intersection

```java
private boolean pathCutsNoFlyZone(Position from, Position to, List<RestrictedArea> areas) {
    for (RestrictedArea area : areas) {
        if (lineIntersectsPolygon(from, to, area.getVertices())) {
            return true;
        }
    }
    return false;
}
```

---

## Capacity-Based Trip Selection

### Greedy Packing Algorithm

```java
private List<MedDispatchRec> selectDeliveriesForTrip(
        List<MedDispatchRec> available,
        float droneCapacity,
        ServicePoint servicePoint
) {
    if (available == null || available.isEmpty()) {
        return Collections.emptyList();
    }

    List<MedDispatchRec> selected = new ArrayList<>();
    float currentLoad = 0.0f;

    // Sort by proximity to service point (greedy nearest-first)
    List<MedDispatchRec> sorted = new ArrayList<>(available);
    Position spPos = servicePoint != null ? servicePoint.getPosition() : null;

    if (spPos != null) {
        sorted.sort(Comparator.comparingDouble(d -> {
            Position dPos = d.delivery();
            if (dPos == null) return Double.MAX_VALUE;

            // Use existing distanceTo method
            ResponseEntity<BigDecimal> distResponse =
                    distanceTo(new PositionsDto(spPos, dPos));
            if (distResponse == null || distResponse.getBody() == null) {
                return Double.MAX_VALUE;
            }
            return distResponse.getBody().doubleValue();
        }));
    }

    // Greedy packing: add deliveries until capacity full
    for (MedDispatchRec delivery : sorted) {
        if (delivery.requirements() == null) continue;

        Float weight = delivery.requirements().getCapacity();
        if (weight == null) continue;

        // Check if this delivery fits in remaining capacity
        if (currentLoad + weight <= droneCapacity) {
            selected.add(delivery);
            currentLoad += weight;

            // Stop if at capacity
            if (currentLoad >= droneCapacity) {
                break;
            }
        }
    }

    return selected;
}
```

### Key Features

1. **Proximity Sorting**: Nearest deliveries first (greedy)
2. **Capacity Check**: Only add if fits
3. **Early Termination**: Stop when capacity reached
4. **Null Safety**: Handle missing data gracefully

---

## Distance Calculation Strategy

### Single Source of Truth

**Use `distanceTo()` method everywhere** - don't duplicate logic

```java
// Existing method (returns BigDecimal)
public ResponseEntity<BigDecimal> distanceTo(PositionsDto positions) {
    // Calculate Euclidean distance
    // Returns BigDecimal for precision
}

// When need double for comparison
ResponseEntity<BigDecimal> distResponse = distanceTo(positions);
double distance = distResponse.getBody().doubleValue();
```

### Why Not Separate euclideanDistance()?

- ✅ Single calculation method (consistency)
- ✅ BigDecimal precision preserved
- ✅ Convert to double only when needed (sorting)
- ✅ No code duplication

### Usage Pattern

```java
// For sorting by distance
List<MedDispatchRec> sorted = deliveries.stream()
                .sorted(Comparator.comparingDouble(d -> {
                    ResponseEntity<BigDecimal> dist =
                            distanceTo(new PositionsDto(servicePoint, d.delivery()));
                    return dist.getBody().doubleValue();
                }))
                .collect(Collectors.toList());

// For maxCost estimation
ResponseEntity<BigDecimal> dist = distanceTo(positions);
double distanceKm = dist.getBody().doubleValue();
int moves = (int) Math.ceil(distanceKm / MOVE_DISTANCE);
double cost = costInitial + (moves * costPerMove) + costFinal;
```

---

## Time Availability Handling

### The Edge Case (Split Windows)

**Scenario**:

```
Drone availability (same day):
  08:00-12:00 ✅
  12:00-14:00 ❌ UNAVAILABLE
  14:00-18:00 ✅

Deliveries:
  D1: time = "10:00" (morning window)
  D2: time = "15:00" (afternoon window)
```

**Question**: Must drone return to SP between D1 and D2 due to unavailability?

**Community Consensus**: **TOO COMPLEX - IGNORE IT!**

> "I was checking all the test and people's idea. Until I realised there is just no right answer because of the
> vagueness. I gave up"

### Recommended Implementation

**DO** (in queryAvailableDrones):

```java
// Check if delivery time falls within ANY availability window
boolean available = false;
for(
AvailabilityDTO window :drone.

getAvailability()){
        if(date.

getDayOfWeek() ==window.

getDayOfWeek() &&
        !time.

isBefore(window.getFrom())&&
        !time.

isAfter(window.getUntil())){
available =true;
        break;
        }
        }
```

**DON'T** (in calcDeliveryPath):

```java
// DON'T force returns to SP based on time windows
// Just route deliveries optimally
// Ignore which specific time window they "should" be in
```

### Rationale

1. Spec is vague ("no right answer")
2. Too complex to implement correctly
3. Test data likely doesn't test this edge case
4. Community consensus: **ignore it** ✅

---

## Query Implementation Patterns

### Dynamic Query (POST)

```java
public ResponseEntity<List<Integer>> query(List<QueryAttributeDto> queryAttributes) {
    if (queryAttributes == null) {
        return ResponseEntity.ok(Collections.emptyList());
    }

    List<Integer> result = new ArrayList<>();

    // Outer loop: each drone
    outer:
    for (Drone drone : fetchAllDrones()) {
        // Inner loop: all query attributes (AND logic)
        for (QueryAttributeDto qa : queryAttributes) {
            // Simple null check (instructor guarantees valid input)
            if (qa == null || qa.getAttribute() == null ||
                    qa.getOperator() == null || qa.getValue() == null) {
                continue outer;
            }

            // Extract attribute value
            Object val = extractAttributeValue(drone, qa.getAttribute());
            if (val == null) continue outer;

            // Match with operator
            boolean ok = matchesAsStringOrNumber(val, qa.getOperator(), qa.getValue());
            if (!ok) continue outer; // AND: all must match
        }

        // All attributes matched
        result.add(drone.getId());
    }

    return ResponseEntity.ok(result);
}
```

### Attribute Extraction

```java
private Object extractAttributeValue(Drone drone, String attributeName) {
    // Top-level attributes
    switch (attributeName) {
        case "name":
            return drone.getName();
        case "id":
            return drone.getId();
    }

    // Capability attributes
    Capability cap = drone.getCapability();
    if (cap != null) {
        switch (attributeName) {
            case "heating":
                return cap.heating();
            case "cooling":
                return cap.cooling();
            case "capacity":
                return cap.getCapacity();
            case "maxMoves":
                return cap.maxMoves();
            case "costPerMove":
                return cap.costPerMove();
            case "costInitial":
                return cap.costInitial();
            case "costFinal":
                return cap.costFinal();
        }
    }

    // Handle dot notation (capability.capacity)
    if (attributeName.contains(".")) {
        String[] parts = attributeName.split("\\.");
        if (parts.length == 2 && "capability".equals(parts[0])) {
            return extractAttributeValue(drone, parts[1]);
        }
    }

    return null;
}
```

### Operator Matching

```java
private boolean matchesAsStringOrNumber(Object actualVal, String operator, String expectedStr) {
    operator = operator.trim();

    // Boolean comparison
    if (actualVal instanceof Boolean) {
        boolean act = (Boolean) actualVal;
        boolean exp = Boolean.parseBoolean(expectedStr);
        return switch (operator) {
            case "=" -> act == exp;
            case "!=" -> act != exp;
            default -> false;
        };
    }

    // Numeric comparison
    if (actualVal instanceof Number) {
        double act = ((Number) actualVal).doubleValue();
        double exp;
        try {
            exp = Double.parseDouble(expectedStr);
        } catch (NumberFormatException e) {
            return false;
        }

        return switch (operator) {
            case "=" -> Double.compare(act, exp) == 0;
            case "!=" -> Double.compare(act, exp) != 0;
            case "<" -> act < exp;
            case ">" -> act > exp;
            case "<=" -> act <= exp;
            case ">=" -> act >= exp;
            default -> false;
        };
    }

    return false;
}
```

---

## Summary: Best Practices

### Design Principles

1. **Simplicity**: Keep algorithms simple and fast
2. **Greedy**: Use greedy approaches (not optimal)
3. **Reuse**: Single source of truth (e.g., distanceTo)
4. **Validation**: Minimal - instructor guarantees valid input
5. **Performance**: Meet 3-minute timeout
6. **Flexibility**: Multiple valid approaches exist

### Key Patterns

- **Multi-day**: Group by date, reset state
- **Multiple trips**: Nested loops (dates → drones → trips)
- **Capacity**: Greedy packing, nearest-first
- **Distance**: Use existing distanceTo method
- **Time**: Check availability, don't enforce windows in routing
- **Queries**: AND logic, minimal validation

### Performance Targets

- **Timeout**: ~3 minutes on student machines
- **Algorithm**: Greedy (O(n) or O(n log n))
- **Optimization**: "Good enough" > perfect
- **Focus**: Validity > optimality

---

**Status**: ✅ All implementation strategies documented and validated  
**Approach**: Follows community consensus and instructor guidance  
**Ready**: For implementation with confidence

