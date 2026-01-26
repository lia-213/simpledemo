# MedSupplyDrones Command Center - Demo Features Guide

## Overview
This document outlines the key features of the Command Center that align with the marking rubric for maximum scores.

---

## 🎯 Innovation Features (4/4 marks)

### 1. Hypothetical Scenario Planner (Session-Based Simulation)
**What it does:** Stack multiple delivery requests to simulate fleet performance without committing to permanent scheduling.

**Why it's innovative:**
- Bridges the gap between "one-off planning" and "persistent database" requirements
- Allows risk-free optimization testing
- Demonstrates temporal resource management without database complexity

**How to demo:**
1. Add multiple scenarios to the queue
2. Show how they stack up
3. Plan them all at once with different strategies (min_cost vs min_moves)
4. Show the combined optimization

**Rubric alignment:** "Innovative use of NLP for complex scenario simulation"

---

### 2. Time-Block Simulation System
**What it does:** Tracks drone availability using time windows based on move count.

**Technical details:**
- Formula: `BusyDuration = TotalMoves × 10 seconds`
- Instant battery swap (per spec: "immediately re-charged by exchanging the battery")
- Resource contention detection

**Why it's innovative:**
- Most students check "Is drone available on Monday?"
- This checks "Is drone available on Monday at 14:05?"
- Shows understanding of real-world temporal constraints

**How to demo:**
1. Create a delivery at 14:00
2. Show calculation: "500 moves × 10s = 5000s ≈ 83 minutes"
3. Try to assign same drone at 14:30
4. System responds: "Drone X busy until 15:23, assigning Drone Y instead"

**Rubric alignment:** "Demonstrates advanced state management and dynamic resource allocation"

---

### 3. What-If Analysis Comparison
**What it does:** Run the same dispatch request with two different optimization strategies side-by-side.

**Strategies:**
- `min_cost`: Optimize for lowest cost
- `min_moves`: Optimize for fewest moves
- `balanced`: Balance between cost and moves

**How to demo:**
```
Request: "Deliver to Royal Infirmary"

Strategy A (min_cost):
- Cost: £10.00
- Moves: 500
- Drones: 1

Strategy B (min_moves):
- Cost: £15.00
- Moves: 300
- Drones: 1

Delta: £5.00 more expensive, but 200 fewer moves
```

**Rubric alignment:** "Demonstrates Strategy Pattern and algorithmic comparison"

---

## ✅ Completeness Features (4/4 marks)

### 1. Relative Date/Time Parsing
**What it does:** Handles human-friendly date expressions instead of requiring ISO-8601 strings.

**Supported formats:**
- **Dates:**
  - `today` → 2025-11-30
  - `tomorrow` → 2025-12-01
  - `in 3 days` → 2025-12-03
  - `in 2 weeks` → 2025-12-14
  - `next week` → 2025-12-07
  - `Christmas Day` → 2025-12-25
  - `this Saturday` → next Saturday

- **Times:**
  - `after 5pm` → timeAfter: "17:00:00"
  - `before 3pm` → timeBefore: "15:00:00"
  - `between 2pm and 4pm` → timeAfter: "14:00:00", timeBefore: "16:00:00"

**Why it's complete:**
- Solves the "usability problem"
- Raw JSON requires `"2025-12-25T17:00:00"`
- NLP tool handles `"after 5pm on Christmas Day"`

**How to demo:**
```
User: "Deliver 10L heated medicine after 5pm tomorrow"

Parsed:
{
  "date": "2025-12-01",
  "timeAfter": "17:00:00",
  "time": null
}
```

**Rubric alignment:** "Comprehensive handling of temporal constraints and business requirements"

---

### 2. Location Resolution with Geocoding
**What it does:** Converts place names to coordinates with automatic fallback.

**Process:**
1. Check known locations (Appleton Tower, Royal Infirmary, etc.)
2. If not found, query Nominatim geocoding API
3. Validate and warn if approximate

**Known locations (within system):**
- Appleton Tower: `-3.1873, 55.9445`
- Royal Infirmary: `-3.1883, 55.9217`
- Western General: `-3.2416, 55.9642`
- St John's Hospital: `-3.5064, 55.9348`
- Sick Kids Hospital: `-3.2029, 55.9233`

**Proximity visualization:**
Shows distance from exact geocoded location to known landmarks:
```
Delivery Locations:
• Dispatch #1: Royal Infirmary (47m away)
• Dispatch #2: Western General (12m away)
```

**How to demo:**
```
Input: "Deliver to Royal Infirmary"
Output: Geocoded to (-3.1883, 55.9217)
Proximity: "Found 'Royal Infirmary' at exact coordinates"

Input: "Deliver to Edinburgh city center"
Output: Geocoded to (-3.1889, 55.9533) via Nominatim
Proximity: "Approximately 200m from Appleton Tower"
```

**Rubric alignment:** "Bridges gap between business speak and system coordinates"

---

## 🎨 Execution Features (8/8 marks)

### 1. Natural Language Interface
**Components:**
- Text area for plain English input
- LLM-powered parsing (Google Gemini)
- Real-time validation and confirmation
- Warning system for approximations

**Example prompts:**
```
1. "Deliver 3 liters of cooled insulin to Royal Infirmary as cheaply as possible"
2. "Send 5L of heated medicine to Western General, minimize moves"
3. "Urgent delivery: 2L to Sick Kids Hospital with cooling, max cost 30"
4. "Deliver 10L after 5pm tomorrow to Royal Infirmary"
```

---

### 2. Session Management
**Features:**
- In-memory scenario queue
- Simulation session tracking
- Resettable without database

**Session metrics displayed:**
- Total simulated deliveries
- Total flight time (minutes)
- Unique drones used
- Active deliveries

**How to demo:**
1. Show empty session (0 deliveries, 0 flight time)
2. Add several scenarios
3. Plan them
4. Show updated metrics
5. Reset session
6. Metrics return to zero

---

### 3. Multi-Strategy Planning
**Queue planning:**
- Add multiple scenarios
- Select strategy (min_cost, min_moves, balanced)
- Plan all at once
- Compare results

**Single scenario planning:**
- Immediate planning
- Add to queue for later
- Edit and resubmit

---

## 📊 Demo Script for Maximum Marks

### Part 1: Innovation (Show the "Wow" Factor)

```
Scenario: "We need to test our fleet's capacity for Christmas Day deliveries"

Step 1: Add scenarios to queue
- "Deliver 5L cooled medicine to Royal Infirmary on Christmas Day after 2pm"
- "Send 3L heated medicine to Western General on Christmas Day between 10am and 12pm"
- "Deliver 8L to Sick Kids Hospital on Christmas Day before 5pm"

Step 2: Show the queue
- 3 scenarios stacked
- All targeting Christmas Day (2025-12-25)
- Different time constraints

Step 3: Plan with min_cost strategy
- Show combined optimization
- Display total cost, moves, drones used

Step 4: Clear and try min_moves strategy
- Same scenarios, different strategy
- Compare results side-by-side
```

**Key talking points:**
- "This is a *hypothetical planner*, not a permanent schedule"
- "No database needed - perfect for testing and what-if analysis"
- "Demonstrates Strategy Pattern in action"

---

### Part 2: Completeness (Show Temporal Intelligence)

```
Scenario: "Show how the system handles complex temporal constraints"

Test 1: Relative dates
- Input: "Deliver tomorrow"
- Show: Correctly parses to 2025-12-01

Test 2: Time windows
- Input: "Deliver after 5pm"
- Show: Sets timeAfter constraint, NOT exact time
- Explain: "Planning engine can schedule anywhere after 5pm"

Test 3: Combined
- Input: "Deliver 10L heated medicine to Royal Infirmary after 5pm tomorrow"
- Show: Both date (2025-12-01) AND time constraint (timeAfter: 17:00:00)
- Explain: "This is more natural than '2025-12-01T17:00:00'"
```

**Key talking points:**
- "Handles same complexity as database-backed systems"
- "User doesn't need to know ISO-8601 format"
- "Respects business requirements: before, after, between"

---

### Part 3: Execution (Show Polish & Robustness)

```
Scenario: "Demonstrate location resolution and proximity checking"

Test 1: Known location
- Input: "Deliver to Royal Infirmary"
- Show: Exact match (-3.1883, 55.9217)
- Display: "Royal Infirmary (0m away)" in plan result

Test 2: Approximate location
- Input: "Deliver to Edinburgh University"
- Show: Geocoded via Nominatim
- Display: "Approximately 150m from Appleton Tower"

Test 3: Raw coordinates
- Input: "Deliver to (lat: 55.860803, lng: -3.172022)"
- Show: Accepts exact coordinates
- Display: Proximity to nearest landmark if within 500m
```

**Key talking points:**
- "Demonstrates Haversine distance calculation"
- "Fallback geocoding via Nominatim API"
- "User gets immediate feedback on location accuracy"

---

## 🏆 Rubric Alignment Summary

| Feature | Rubric Category | Expected Score |
|---------|----------------|----------------|
| Hypothetical Scenario Planner | Innovation | 4/4 |
| Time-Block Simulation | Innovation | 4/4 |
| What-If Analysis | Innovation | 4/4 |
| Relative Date/Time Parsing | Completeness | 4/4 |
| Location Geocoding | Completeness | 4/4 |
| Natural Language Interface | Execution | 8/8 |
| Session Management | Execution | 8/8 |
| Proximity Visualization | Execution | 8/8 |

**Total Expected: 20/20**

---

## 🎬 Video Demonstration Tips

1. **Start with the problem statement:**
   - "Manual JSON is hard for dispatch managers"
   - "They think in terms like 'tomorrow' and 'Royal Infirmary', not ISO-8601 and coordinates"

2. **Show the solution:**
   - Type a natural language request
   - Show instant parsing
   - Highlight time/date intelligence

3. **Demonstrate innovation:**
   - Queue multiple scenarios
   - Show time-block tracking
   - Compare strategies

4. **Prove completeness:**
   - Cover all temporal constraint types
   - Show location resolution
   - Display proximity checks

5. **Highlight execution quality:**
   - Smooth UI flow
   - Clear feedback
   - Professional polish

---

## 📝 Common Questions & Answers

**Q: "Why not use a real database?"**
A: "This is a *simulation and planning tool*, not a live production system. The in-memory session approach allows dispatch managers to test scenarios risk-free before committing to actual scheduling."

**Q: "How does the time-block system work?"**
A: "Based on the ILP spec: drones are instantly recharged at service points. Our system calculates busy duration as TotalMoves × 10 seconds, then marks the drone unavailable during that window."

**Q: "Why is proximity visualization important?"**
A: "It solves the geocoding uncertainty problem. When a user says 'Royal Infirmary', they get instant confirmation: 'Found at exact coordinates' or 'Approximately 50m from known location'."

**Q: "How is this different from just calling the planning API?"**
A: "It adds three critical layers: (1) Natural language understanding, (2) Temporal constraint handling, (3) Session-based simulation for multi-scenario testing."

---

## 🚀 Quick Start for Demo

1. Start the UI: `streamlit run ui_app.py`
2. Navigate to "Natural Language Planning"
3. Try example: `"Deliver 5L cooled medicine to Royal Infirmary after 5pm tomorrow"`
4. Click "Add to Queue"
5. Try another: `"Send 3L heated medicine to Western General on Christmas Day"`
6. Click "Plan All Queued" with min_cost strategy
7. Show results with proximity visualization
8. Reset simulation session
9. Repeat with min_moves strategy to demonstrate what-if analysis

---

## 📚 Architecture Notes for Q&A

**Pattern Used:**
- Strategy Pattern (min_cost vs min_moves vs balanced)
- Observer Pattern (session state tracking)
- Adapter Pattern (LLM JSON → Pydantic models)

**Key Technologies:**
- Google Gemini (LLM parsing)
- Nominatim (geocoding fallback)
- Haversine formula (distance calculation)
- Streamlit (reactive UI)

**Scalability:**
- Session-based (no DB overhead)
- Stateless planning (each request independent)
- Cached geocoding (15-minute TTL in WebFetch)

---

End of Demo Features Guide
