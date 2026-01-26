# Testing Guide

This project now includes comprehensive testing capabilities with three must-have testing types.

## 📋 Table of Contents
- [Test Types](#test-types)
- [Running Tests](#running-tests)
- [Code Coverage](#code-coverage)
- [New Testing Features](#new-testing-features)

## 🧪 Test Types

### 1. Unit Testing
Tests individual components in isolation.
- **Location**: `src/test/java/ilp/*/model/`, `src/test/java/ilp/*/service/`
- **Examples**: `DroneTest.java`, `PositionTest.java`, `RegionTest.java`
- **Framework**: JUnit 5

### 2. Mock Testing  
Tests components with mocked dependencies using Mockito.
- **Location**: `src/test/java/ilp/*/service/impl/`
- **Examples**: `DroneServiceImplMockTest.java`, `DroneControllerTest.java`
- **Framework**: Mockito

### 3. Integration Testing
Tests multiple components working together.
- **Location**: `src/test/java/ilp/*/integrationTests/`
- **Examples**: `IlpCw1ApplicationTests.java`
- **Framework**: Spring Boot Test + MockMvc

### 4. Performance Testing
Validates operations complete within time constraints.
- **Location**: `src/test/java/ilp/*/performance/`
- **Examples**: `DeliveryPathPerformanceTest.java`
- **Framework**: JUnit 5 with `assertTimeout()`

### 5. ⭐ Parameterized Testing (NEW)
Efficient testing with multiple input sets.
- **Location**: `src/test/java/ilp/ilp_cw/ilp_1_2/model/`
- **Examples**: `DroneParameterizedTest.java`, `PositionParameterizedTest.java`
- **Framework**: JUnit 5 Parameterized Tests
- **Benefits**:
  - Reduces code duplication
  - Tests boundary values systematically
  - Easy to add new test cases

### 6. ⭐ API Contract Testing (NEW)
Validates REST API contracts and response schemas.
- **Location**: `src/test/java/ilp/ilp_cw/ilp_1_2/contract/`
- **Examples**: `DroneApiContractTest.java`, `DroneApiContractEdgeCaseTest.java`
- **Framework**: REST Assured
- **Benefits**:
  - Ensures API compatibility
  - Validates status codes and response formats
  - Tests error handling

### 7. ⭐ Code Coverage Analysis (NEW)
Measures test effectiveness and identifies untested code.
- **Tool**: JaCoCo
- **Reports**: HTML, XML, CSV formats
- **Threshold**: 50% line coverage minimum

## 🚀 Running Tests

### Run All Tests
```bash
mvn test
```

### Run Specific Test Class
```bash
mvn test -Dtest=DroneParameterizedTest
```

### Run Tests with Pattern
```bash
mvn test -Dtest=*ContractTest
```

### Run Only Parameterized Tests
```bash
mvn test -Dtest=*ParameterizedTest
```

### Run Only Contract Tests
```bash
mvn test -Dtest=*ContractTest
```

## 📊 Code Coverage

### Generate Coverage Report
```bash
mvn clean test
```

Coverage reports are automatically generated in:
- **HTML**: `target/site/jacoco/index.html`
- **XML**: `target/site/jacoco/jacoco.xml`
- **CSV**: `target/site/jacoco/jacoco.csv`

### View Coverage Report
1. Run tests: `mvn clean test`
2. Open in browser: `target/site/jacoco/index.html`
3. Navigate through packages to see detailed coverage

### Coverage Metrics
- **Line Coverage**: Percentage of code lines executed
- **Branch Coverage**: Percentage of decision branches taken
- **Method Coverage**: Percentage of methods invoked
- **Class Coverage**: Percentage of classes loaded

### Coverage Threshold
The project enforces a minimum of **50% line coverage** per package. Build will fail if coverage falls below this threshold.

To adjust the threshold, edit `pom.xml`:
```xml
<limit>
    <counter>LINE</counter>
    <value>COVEREDRATIO</value>
    <minimum>0.50</minimum>  <!-- Change this value -->
</limit>
```

## 🆕 New Testing Features

### Parameterized Tests Examples

#### Using @ValueSource
```java
@ParameterizedTest
@ValueSource(strings = {"1", "42", "999"})
void testValidIds(String id) {
    // Test logic
}
```

#### Using @CsvSource
```java
@ParameterizedTest
@CsvSource({
    "true,  false, true",
    "false, false, false"
})
void testCapabilities(boolean cooling, boolean heating, boolean expected) {
    // Test logic
}
```

#### Using @MethodSource
```java
@ParameterizedTest
@MethodSource("coordinateProvider")
void testCoordinates(double lng, double lat) {
    // Test logic
}

static Stream<Object[]> coordinateProvider() {
    return Stream.of(
        new Object[]{0.0, 0.0},
        new Object[]{55.944425, -3.188267}
    );
}
```

### API Contract Test Examples

#### Testing Status Codes
```java
given()
    .contentType(ContentType.JSON)
    .body(requestBody)
.when()
    .post("/api/v1/distanceTo")
.then()
    .statusCode(200)
    .body(notNullValue());
```

#### Testing Response Schema
```java
given()
.when()
    .get("/api/v1/droneDetails/1")
.then()
    .statusCode(200)
    .body("name", isA(String.class))
    .body("capability.cooling", isA(Boolean.class));
```

## 📈 Coverage Analysis Workflow

1. **Run tests with coverage**:
   ```bash
   mvn clean test
   ```

2. **Review coverage report**:
   - Open `target/site/jacoco/index.html`
   - Identify red (uncovered) and yellow (partially covered) code

3. **Add tests for uncovered code**:
   - Focus on high-value business logic
   - Prioritize complex conditional branches
   - Add edge case tests

4. **Verify improvement**:
   ```bash
   mvn clean test
   ```

## 🎯 Test Best Practices

### Parameterized Tests
- Use `@DisplayName` for readable test names
- Group related test cases together
- Test boundary values and edge cases
- Include both valid and invalid inputs

### API Contract Tests
- Test all HTTP methods (GET, POST, etc.)
- Validate response status codes
- Check response body schema
- Test error scenarios (400, 404, 500)
- Verify content types

### Code Coverage
- Aim for **80%+ coverage** for critical business logic
- Don't chase 100% - focus on valuable tests
- Review coverage reports regularly
- Use coverage to find gaps, not as the only metric

## 🔧 Troubleshooting

### Tests Fail with "No tests found"
- Ensure test classes end with `Test`
- Check that test methods have `@Test` or `@ParameterizedTest` annotation
- Verify tests are in `src/test/java/` directory

### Coverage Report Not Generated
- Run `mvn clean test` (not just `mvn test`)
- Check that JaCoCo plugin is in `pom.xml`
- Look for errors in Maven output

### REST Assured Tests Fail
- Ensure Spring Boot application context loads correctly
- Check that `@MockBean` dependencies are properly configured
- Verify `@SpringBootTest` annotation is present

### Parameterized Tests Not Running
- Add `junit-jupiter-params` dependency
- Use correct annotation (`@ParameterizedTest`, not `@Test`)
- Provide valid data source (`@ValueSource`, `@CsvSource`, etc.)

## 📚 Further Reading

- [JUnit 5 User Guide](https://junit.org/junit5/docs/current/user-guide/)
- [Mockito Documentation](https://javadoc.io/doc/org.mockito/mockito-core/latest/org/mockito/Mockito.html)
- [JaCoCo Documentation](https://www.jacoco.org/jacoco/trunk/doc/)
- [REST Assured Documentation](https://rest-assured.io/)
- [Spring Boot Testing](https://spring.io/guides/gs/testing-web)

## 📝 Summary

This project now includes:
- ✅ **50+ parameterized test cases** reducing code duplication
- ✅ **30+ API contract tests** ensuring API reliability
- ✅ **JaCoCo code coverage** with automatic reporting
- ✅ **Comprehensive test suite** covering unit, integration, and edge cases
- ✅ **50% minimum coverage threshold** enforced in build

Run `mvn clean test` and open `target/site/jacoco/index.html` to see your current coverage!
