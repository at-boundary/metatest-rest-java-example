# Metatest REST API Example Project

This is a sample project demonstrating how to use **Metatest** - a library that validates API test reliability through fault simulation and business rule (invariant) validation.

> **Experimental Project (v0.1.0)**
>
> This example uses the experimental Metatest library. Features and APIs may change between versions.

## Table of Contents

- [What is Metatest?](#what-is-metatest)
- [Quick Setup](#quick-setup)
  - [1. Add JitPack Repository](#1-add-jitpack-repository)
  - [2. Add Dependency and Configure AspectJ](#2-add-dependency-and-configure-aspectj)
  - [3. Create Configuration File](#3-create-configuration-file)
- [Invariants DSL - Define Your Business Rules](#invariants-dsl---define-your-business-rules)
  - [Basic Operators](#basic-operators)
  - [All Supported Operators](#all-supported-operators)
  - [Conditional Invariants (if/then)](#conditional-invariants-ifthen)
  - [Cross-Field Comparisons](#cross-field-comparisons)
  - [Array Field Validation](#array-field-validation)
- [Complete Example: Trading API](#complete-example-trading-api)
- [Running Tests](#running-tests)
- [How It Works](#how-it-works)
- [Example Test](#example-test)
- [GitHub Actions CI/CD](#github-actions-cicd)
- [Troubleshooting](#troubleshooting)
- [Documentation](#documentation)
- [Support](#support)

---

## What is Metatest?

Metatest uses AspectJ bytecode weaving to:
1. **Inject contract faults** - Set fields to null, remove fields, empty strings/arrays
2. **Violate business rules (invariants)** - Generate mutations that break your defined invariants

It then re-runs your tests to verify they catch these violations. Tests that pass despite injected faults indicate weak assertions.

---

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

### 3. Create Configuration File

Create `src/main/resources/config.yml`:

```yaml
version: "1.0"

settings:
  default_quantifier: all

# Define your API endpoints and invariants
endpoints:
  /api/users/{id}:
    GET:
      invariants:
        - name: user_has_email
          field: email
          is_not_empty: true

        - name: valid_status
          field: status
          in: [ACTIVE, SUSPENDED, PENDING]

# Enable contract fault types
faults:
  null_field:
    enabled: true
  missing_field:
    enabled: true

# Optional: exclude certain endpoints/tests
exclusions:
  urls:
    - '*/health*'
  tests:
    - '*SmokeTest*'
```

---

## Invariants DSL - Define Your Business Rules

Invariants are business rules that your API responses must satisfy. Metatest generates mutations that violate these rules and verifies your tests catch the violations.

### Basic Operators

```yaml
endpoints:
  /api/products/{id}:
    GET:
      invariants:
        # Numeric constraints
        - name: positive_price
          field: price
          greater_than: 0

        - name: stock_non_negative
          field: stock_quantity
          greater_than_or_equal: 0

        # Enum validation
        - name: valid_category
          field: category
          in: [ELECTRONICS, CLOTHING, FOOD, OTHER]

        # Null checks
        - name: has_name
          field: name
          is_not_null: true

        - name: has_description
          field: description
          is_not_empty: true
```

### All Supported Operators

| Operator | Description | Example |
|----------|-------------|---------|
| `equals` | Exact match | `equals: "ACTIVE"` |
| `not_equals` | Not equal to | `not_equals: "DELETED"` |
| `greater_than` | Numeric > | `greater_than: 0` |
| `greater_than_or_equal` | Numeric >= | `greater_than_or_equal: 0` |
| `less_than` | Numeric < | `less_than: 100` |
| `less_than_or_equal` | Numeric <= | `less_than_or_equal: 1000` |
| `in` | Value in list | `in: [A, B, C]` |
| `not_in` | Value not in list | `not_in: [DELETED]` |
| `is_null` | Must be null | `is_null: true` |
| `is_not_null` | Must not be null | `is_not_null: true` |
| `is_empty` | Must be empty | `is_empty: true` |
| `is_not_empty` | Must not be empty | `is_not_empty: true` |

### Conditional Invariants (if/then)

Define rules that only apply when a condition is met:

```yaml
endpoints:
  /api/orders/{id}:
    GET:
      invariants:
        # If order is shipped, tracking number must exist
        - name: shipped_has_tracking
          if:
            field: status
            equals: SHIPPED
          then:
            field: tracking_number
            is_not_empty: true

        # If order is completed, completed_at must be set
        - name: completed_has_timestamp
          if:
            field: status
            equals: COMPLETED
          then:
            field: completed_at
            is_not_null: true

        # If order is cancelled, refund should be processed
        - name: cancelled_has_refund
          if:
            field: status
            equals: CANCELLED
          then:
            field: refund_status
            in: [PENDING, PROCESSED]
```

### Cross-Field Comparisons

Reference other fields using `$.field_name`:

```yaml
endpoints:
  /api/orders/{id}:
    GET:
      invariants:
        # created_at must be before or equal to updated_at
        - name: created_before_updated
          field: created_at
          less_than_or_equal: $.updated_at

        # discount_price must be less than original price
        - name: discount_less_than_original
          if:
            field: discount_price
            is_not_null: true
          then:
            field: discount_price
            less_than: $.price
```

### Array Field Validation

Use `$[*].field` to validate all items in an array:

```yaml
endpoints:
  /api/orders:
    GET:
      invariants:
        # All orders must have positive total
        - name: all_orders_positive_total
          field: $[*].total_amount
          greater_than: 0

        # All orders must have valid status
        - name: all_orders_valid_status
          field: $[*].status
          in: [PENDING, PROCESSING, SHIPPED, COMPLETED, CANCELLED]

  /api/orders/{id}:
    GET:
      invariants:
        # All line items must have positive quantity
        - name: all_items_positive_qty
          field: $[*].items.quantity
          greater_than: 0
```

---

## Complete Example: Trading API

Here's a real-world example for a trading/brokerage API:

```yaml
version: "1.0"

settings:
  default_quantifier: all

endpoints:
  # ============================================
  # ORDERS
  # ============================================
  /api/v1/orders/{id}:
    GET:
      invariants:
        - name: positive_quantity
          field: quantity
          greater_than: 0

        - name: non_negative_price
          field: price
          greater_than_or_equal: 0

        - name: valid_order_type
          field: order_type
          in: [BUY, SELL]

        - name: valid_status
          field: status
          in: [PENDING, FILLED, REJECTED, CANCELLED]

        - name: filled_order_has_filled_at
          if:
            field: status
            equals: FILLED
          then:
            field: filled_at
            is_not_null: true

        - name: rejected_order_has_reason
          if:
            field: status
            equals: REJECTED
          then:
            field: rejection_reason
            is_not_empty: true

  /api/v1/orders:
    GET:
      invariants:
        - name: all_orders_positive_quantity
          field: $[*].quantity
          greater_than: 0

    POST:
      invariants:
        - name: new_order_pending_or_filled
          field: status
          in: [PENDING, FILLED, REJECTED]

  # ============================================
  # ACCOUNTS
  # ============================================
  /api/v1/accounts/{id}:
    GET:
      invariants:
        - name: non_negative_cash
          field: cash_balance
          greater_than_or_equal: 0

        - name: valid_account_status
          field: status
          in: [ACTIVE, SUSPENDED, CLOSED]

        - name: account_number_present
          field: account_number
          is_not_empty: true

        - name: updated_after_created
          field: created_at
          less_than_or_equal: $.updated_at

  /api/v1/accounts/{id}/deposit:
    POST:
      invariants:
        - name: positive_cash_after_deposit
          field: cash_balance
          greater_than: 0

  /api/v1/accounts/{id}/withdraw:
    POST:
      invariants:
        - name: non_negative_cash_after_withdraw
          field: cash_balance
          greater_than_or_equal: 0

  # ============================================
  # POSITIONS
  # ============================================
  /api/v1/positions:
    GET:
      invariants:
        - name: all_positions_positive_qty
          field: $[*].quantity
          greater_than: 0

        - name: all_positions_positive_cost
          field: $[*].average_cost
          greater_than: 0

        - name: all_positions_have_symbol
          field: $[*].symbol
          is_not_empty: true

  # ============================================
  # AUTHENTICATION
  # ============================================
  /api/v1/auth/login:
    POST:
      invariants:
        - name: token_present
          field: access_token
          is_not_empty: true

        - name: token_type_bearer
          field: token_type
          equals: bearer

# Contract fault types to test
faults:
  null_field:
    enabled: true
  missing_field:
    enabled: true
  empty_list:
    enabled: false
  empty_string:
    enabled: false
```

---

## Running Tests

### Normal test execution (no fault simulation):
```bash
./gradlew test
```

### With Metatest fault simulation:
```bash
./gradlew test -DrunWithMetatest=true
```

### View the Reports

**HTML Report (Recommended):**
```bash
# Open in your default browser
start metatest_report.html   # Windows
open metatest_report.html    # macOS
xdg-open metatest_report.html # Linux
```

The HTML report shows:
- Overall detection rate
- Contract faults vs Invariant faults breakdown
- Per-endpoint fault details
- Which tests caught which faults

**JSON Report (for CI/CD):**
```bash
cat fault_simulation_report.json
```

---

## How It Works

```
┌─────────────────────────────────────────────────────────────┐
│  1. Your test runs normally (baseline)                      │
└─────────────────────────────────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────────┐
│  2. Metatest captures HTTP response                         │
└─────────────────────────────────────────────────────────────┘
                          │
              ┌───────────┴───────────┐
              ▼                       ▼
┌──────────────────────┐  ┌──────────────────────┐
│  Contract Faults     │  │  Invariant Faults    │
│  - null_field        │  │  - Violate business  │
│  - missing_field     │  │    rules you defined │
│  - empty_string      │  │  - e.g., set price   │
│  - empty_list        │  │    to -1 when rule   │
│                      │  │    says price > 0    │
└──────────────────────┘  └──────────────────────┘
              │                       │
              └───────────┬───────────┘
                          ▼
┌─────────────────────────────────────────────────────────────┐
│  3. Re-run test with mutated response                       │
└─────────────────────────────────────────────────────────────┘
                          │
                    ┌─────┴─────┐
                    ▼           ▼
              Test Passes   Test Fails
                    │           │
                    ▼           ▼
              ┌─────────┐ ┌─────────┐
              │ ESCAPED │ │DETECTED │
              │  (BAD)  │ │ (GOOD)  │
              └─────────┘ └─────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────────┐
│  4. Generate report showing fault detection coverage        │
└─────────────────────────────────────────────────────────────┘
```

---

## Example Test

```java
@Test
void testGetOrder() {
    Response response = RestAssured.get("/api/v1/orders/123");

    assertEquals(200, response.statusCode());

    // These assertions will be tested by Metatest:
    assertNotNull(response.jsonPath().get("id"));
    assertTrue(response.jsonPath().getInt("quantity") > 0);  // Tests positive_quantity invariant
    assertNotNull(response.jsonPath().get("status"));
}
```

Metatest will automatically:
1. **Contract faults**: Set `id` to null, remove `quantity`, etc.
2. **Invariant faults**: Set `quantity` to 0 or -1 (violating `greater_than: 0`)
3. Re-run the test and check if assertions catch the violations

---

## GitHub Actions CI/CD

```yaml
name: API Test Quality

on: [push, pull_request]

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

      - name: Upload reports
        uses: actions/upload-artifact@v4
        if: always()
        with:
          name: metatest-reports
          path: |
            fault_simulation_report.json
            metatest_report.html
```

---

## Troubleshooting

### Dependency not found
- Ensure JitPack repository is added to settings.gradle.kts
- Check the version tag exists: https://jitpack.io/#at-boundary/metatest-rest-java

### Config parsing error
- Ensure `config.yml` uses `invariants:` (not `relations:`)
- Check YAML syntax is valid

### Tests not running with Metatest
- Verify `-DrunWithMetatest=true` is set
- Check that aspectjAgent is found in the test task
- Run with `--info` for detailed output

---

## Documentation

- Main Metatest Documentation: https://github.com/at-boundary/metatest-rest-java
- Invariants DSL Reference: See main documentation

## Support

For issues or questions:
- GitHub Issues: https://github.com/at-boundary/metatest-rest-java/issues
