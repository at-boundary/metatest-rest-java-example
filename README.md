# Metatest REST API Example Project

This is a sample project demonstrating how to use **Metatest** - a REST API mutation testing framework that validates test reliability through fault injection.

> **⚠️ Experimental Project**
>
> This example uses the experimental Metatest library. The project is currently in development phase and dependencies are hosted on GitHub Packages temporarily.

## What is Metatest?

Metatest uses AspectJ bytecode weaving to inject faults into HTTP responses and verify if your tests catch them. It helps ensure your tests are robust and would catch real-world API contract violations.

## Quick Setup

### 1. Add Metatest Plugin and Dependency

**settings.gradle.kts:**
```kotlin
pluginManagement {
    repositories {
        mavenLocal()  // For local testing
        gradlePluginPortal()
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/at-boundary/metatest-rest-java")
            credentials {
                username = settings.providers.gradleProperty("gpr.user").orNull
                    ?: System.getenv("GPR_USER")
                password = settings.providers.gradleProperty("gpr.token").orNull
                    ?: System.getenv("GPR_TOKEN")
            }
        }
    }
}

rootProject.name = "your-project-name"
```

**build.gradle.kts:**
```kotlin
plugins {
    java
    id("io.metatest") version "1.0.0-dev-0e938b3"  // Add Metatest plugin
}

repositories {
    mavenCentral()
    maven {
        name = "GitHubPackages"
        url = uri("https://maven.pkg.github.com/at-boundary/metatest-rest-java")
        credentials {
            username = project.findProperty("gpr.user") as String? ?: System.getenv("GPR_USER")
            password = project.findProperty("gpr.token") as String? ?: System.getenv("GPR_TOKEN")
        }
    }
}

dependencies {
    testImplementation("io.metatest:metatest:1.0.0-dev-0e938b3")  // Add Metatest library

    // Your other dependencies
    testImplementation("org.junit.jupiter:junit-jupiter:5.9.1")
    testImplementation("io.rest-assured:rest-assured:5.3.0")
}
```

### 2. Add Configuration Files to src/main/resources/

Metatest requires configuration files in src/main/resources/:

**config.yml** (Required for local mode):
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



### 3. Set Up Credentials

Create gradle.properties in project root or ~/.gradle/gradle.properties:

```properties
gpr.user=your-github-username
gpr.token=your-github-personal-access-token
```

Or set environment variables:
```bash
export GPR_USER=your-github-username
export GPR_TOKEN=your-github-personal-access-token
```

## Running Tests

### Run tests WITHOUT Metatest (normal tests):
```bash
./gradlew test
```

### Run tests WITH Metatest (mutation testing):
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

1. Plugin automatically configures AspectJ weaving - no manual JVM args needed!
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
    permissions:
      contents: read
      packages: read

    steps:
      - uses: actions/checkout@v4

      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'

      - name: Run tests with Metatest
        env:
          GPR_USER: ${{ github.actor }}
          GPR_TOKEN: ${{ secrets.GITHUB_TOKEN }}
        run: ./gradlew test -DrunWithMetatest=true
```

## Key Features

- Zero code changes - Just add plugin and config files
- Automatic AspectJ setup - Plugin handles all JVM configuration
- Plug-and-play - Works with existing JUnit + RestAssured tests
- Multiple fault types - Null, missing, empty, invalid values
- Configurable - Exclude endpoints, tests, or fault types
- Detailed reports - JSON output with fault coverage metrics

## Troubleshooting

### Plugin not found
- Ensure credentials are set (GPR_USER and GPR_TOKEN)
- Check settings.gradle.kts has GitHub Packages repository in pluginManagement

### AspectJ weaver not found
- Ensure io.metatest:metatest is in testImplementation dependencies
- The plugin requires the library to be on the classpath

### Tests not running with Metatest
- Verify -DrunWithMetatest=true is set
- Check logs for [Metatest] Configuring test task...
- Run with --info for detailed output

## Documentation

- Main Metatest Documentation: https://github.com/at-boundary/metatest-rest-java

## Support

For issues or questions:
- GitHub Issues: https://github.com/at-boundary/metatest-rest-java/issues
