# Metatest REST API Example Project

This is a sample project demonstrating how to use **Metatest** - a library that validates API tests reliability through fault simulation.

> **⚠️ Experimental Project (v0.1.0)**
>
> This example uses the experimental Metatest library. Features and APIs may change between versions.

## What is Metatest?

Metatest uses AspectJ bytecode weaving to inject faults into HTTP responses and verify if your tests catch them. It helps ensure your tests are robust and would catch real-world API contract violations.

## Quick Setup

### 1. Add JitPack Repository

**settings.gradle.kts:**
```kotlin
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "your-project-name"
```

### 2. Add Dependency and Configure AspectJ

**build.gradle.kts:**
```kotlin
plugins {
    java
}

dependencies {
    // Metatest
    testImplementation("com.github.at-boundary:metatest-rest-java:v0.1.0")

    // Your other dependencies
    testImplementation("org.junit.jupiter:junit-jupiter:5.9.1")
    testImplementation("io.rest-assured:rest-assured:5.3.0")
}

tasks.test {
    useJUnitPlatform()

    val aspectjAgent = configurations.testRuntimeClasspath.get()
        .files.find { it.name.contains("aspectjweaver") }

    if (aspectjAgent != null) {
        jvmArgs(
            "-javaagent:$aspectjAgent",
            "-DrunWithMetatest=${System.getProperty("runWithMetatest") ?: "false"}"
        )
    }
}
```

### 3. Add Configuration File (Optional)

Create `src/main/resources/config.yml` to customize fault injection:

**config.yml:**
```yaml
faults:
  null_field:
    enabled: true
  missing_field:
    enabled: true
  empty_list:
    enabled: true
  empty_string:
    enabled: true
  invalid_value:
    enabled: true

endpoints:
  exclude:
    - '*/login*'
    - '*/auth/*'

tests:
  exclude:
    - '*LoginTest*'

simulation:
  only_success_responses: true
  skip_collections_response: true
  min_response_fields: 1

report:
  format: json
  output_path: "./fault_simulation_report.json"
```



## Running Tests

### Run tests WITHOUT Metatest (normal tests):
```bash
./gradlew test
```

### Run tests WITH Metatest (with fault simulation):
```bash
./gradlew test -DrunWithMetatest=true
```

### View the Report:

**HTML Report (Recommended):**
```bash
# Open in your default browser
start metatest_report.html   # Windows
open metatest_report.html    # macOS
xdg-open metatest_report.html # Linux
```

The HTML report provides an interactive, visual dashboard with:
- Summary metrics (fault detection rate, endpoint coverage)
- Expandable fault details with test results
- Gap analysis showing untested endpoints
- Detailed HTTP call logs

**JSON Report (for programmatic access):**
```bash
cat fault_simulation_report.json
```

## How It Works

1. AspectJ weaving intercepts HTTP calls during test execution
2. Tests run normally first - baseline execution captures HTTP requests/responses
3. Metatest simulates faults - null fields, missing fields, empty collections, etc.
4. Tests are re-executed - with each fault injected
5. Results are recorded - which faults were detected vs. missed
6. Report generated - shows fault coverage and weak spots

## Example Test

```java
@Test
void testGetUser() {
    Response response = RestAssured.get("/users/123");

    assertEquals(200, response.statusCode());
    assertEquals("John", response.jsonPath().getString("name"));
    assertNotNull(response.jsonPath().getString("email"));
}
```

Metatest will automatically:
- Set name to null and verify test catches it
- Remove name field entirely and verify test catches it
- Set email to null and verify test catches it
- And more...

## Mock REST API Setup (for this example)

This example uses WireMock for a mocked REST API:

```bash
docker pull wiremock/wiremock:latest

docker run -d --name wiremock-mocked-rest-api -p 8089:8089 \
  -v $(pwd)/mappings:/home/wiremock/mappings \
  wiremock/wiremock:latest --port 8089 --verbose
```

The mappings/ folder contains mock API resources loaded by WireMock.

## GitHub Actions CI/CD

```yaml
jobs:
  test:
    runs-on: ubuntu-latest

    steps:
      - uses: actions/checkout@v4

      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'

      - name: Run tests with Metatest
        run: ./gradlew test -DrunWithMetatest=true
```

## Key Features

- Zero code changes - Just add dependency and config
- Plug-and-play - Works with existing JUnit + RestAssured tests
- Multiple fault types - Null, missing, empty, invalid values
- Configurable - Exclude endpoints, tests, or fault types
- Detailed reports - JSON and HTML output with fault coverage metrics

## Troubleshooting

### Dependency not found
- Ensure JitPack repository is added to settings.gradle.kts
- Check the version tag exists: https://jitpack.io/#at-boundary/metatest-rest-java

### AspectJ weaver not found
- Ensure the metatest dependency is in testImplementation
- The library includes aspectjweaver as a transitive dependency

### Tests not running with Metatest
- Verify -DrunWithMetatest=true is set
- Check that aspectjAgent is found in the test task configuration
- Run with --info for detailed output

## Documentation

- Main Metatest Documentation: https://github.com/at-boundary/metatest-rest-java

## Support

For issues or questions:
- GitHub Issues: https://github.com/at-boundary/metatest-rest-java/issues
