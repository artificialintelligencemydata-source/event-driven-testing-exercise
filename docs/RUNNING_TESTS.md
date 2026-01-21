# Running AUTWIT Tests

This document explains how to run AUTWIT tests, where files must be located, and how to avoid common mistakes.

---

## Overview

AUTWIT tests are executed via:
1. **autwit-runner** module (execution shell)
2. **TestNG** (test framework)
3. **Cucumber** (BDD framework)
4. **Spring Boot** (dependency injection)

---

## Module Structure for Test Execution

### Where Files Must Live

```
autwit-runner/
└── src/
    └── test/
        └── java/
            └── com/acuver/autwit/runner/
                └── CucumberTestRunner.java  ← Runner class (MUST be here)

client-tests/
└── src/
    └── test/
        ├── java/
        │   └── com/bjs/tests/
        │       └── stepDefinitions/
        │           └── *.java  ← Step definitions (MUST be here)
        └── resources/
            └── features/
                └── *.feature  ← Feature files (MUST be here)
```

**Key Rules:**
- **Runner class** must be in `autwit-runner/src/test/java`
- **Feature files** must be in `client-tests/src/test/resources/features`
- **Step definitions** must be in `client-tests/src/test/java`

---

## Cucumber Configuration

### Runner Class Location

**File:** `autwit-runner/src/test/java/com/acuver/autwit/runner/CucumberTestRunner.java`

```java
@CucumberOptions(
    features = "src/test/resources/features",  // ← Relative to runner module
    glue = {
        "com.acuver.autwit.stepdef",           // ← Step definition packages
        "com.acuver.autwit.internal",          // ← Internal hooks
        "com.acuver.autwit.config"              // ← Configuration
    },
    plugin = {
        "pretty",
        "html:target/cucumber-reports.html",
        "io.qameta.allure.cucumber7jvm.AllureCucumber7Jvm"
    },
    monochrome = true
)
public class CucumberTestRunner extends AbstractTestNGCucumberTests {
    // ...
}
```

### Feature File Discovery

**Cucumber looks for features relative to the runner module:**
- Runner is in: `autwit-runner/src/test/java`
- Features are in: `client-tests/src/test/resources/features`
- **Problem:** Runner and features are in different modules

**Solution:** The `features` path in `@CucumberOptions` must be configured to find features across modules, or features must be copied to the runner's resources during build.

### Glue Code Discovery

**Glue packages must include:**
- Step definition packages (from `client-tests`)
- Internal hooks (from `autwit-internal-testkit`)
- Configuration classes (from `autwit-runner`)

**Example:**
```java
glue = {
    "com.bjs.tests.stepDefinitions",  // ← Client step definitions
    "com.acuver.autwit.internal",     // ← Internal hooks
    "com.acuver.autwit.config"        // ← Spring configuration
}
```

---

## TestNG Configuration

### testng.xml Location

**File:** `autwit-runner/src/test/resources/testng.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<suite name="AutwitSuite" verbose="1" parallel="tests" thread-count="4">
    <parameter name="scenariosToRun" value=""/>
    <test name="AutwitCucumberTests" parallel="methods" thread-count="4">
        <parameter name="dataProvider" value="scenarios"/>
        <classes>
            <class name="com.acuver.autwit.runner.CucumberRunner"/>
        </classes>
    </test>
</suite>
```

### Running Tests

**Via Maven:**
```bash
cd autwit-runner
mvn test
```

**Via RunnerApp (Spring Boot):**
```bash
cd autwit-runner
mvn spring-boot:run
```

---

## Spring Boot Configuration

### Application Configuration

**File:** `autwit-runner/src/main/resources/application.yaml`

```yaml
autwit:
  database: mongo  # or postgres, h2
  environment: qa
  suiteFile: src/test/resources/testng.xml
```

### Spring Boot Startup

**File:** `autwit-runner/src/main/java/com/acuver/autwit/runner/RunnerApp.java`

```java
@SpringBootApplication(scanBasePackages = "com.acuver.autwit")
public class RunnerApp {
    public static void main(String[] args) {
        SpringApplication.run(RunnerApp.class, args);
        // TestNG execution happens here
    }
}
```

**What happens:**
1. Spring Boot starts
2. All AUTWIT beans are wired (Engine, Adapters, SDK, TestKit)
3. TestNG is initialized
4. CucumberTestRunner executes
5. Features are discovered and executed

---

## Common Mistakes

### 1. "No features found"

**Symptom:**
```
0 Scenarios
0 Steps
0m0.000s
```

**Causes:**
- Feature files not in correct location
- `features` path in `@CucumberOptions` is wrong
- Features not copied to classpath during build

**Solution:**
- Ensure features are in `client-tests/src/test/resources/features`
- Verify `features` path in `@CucumberOptions` is correct
- Check Maven build includes feature files in classpath

### 2. "Step definitions not found"

**Symptom:**
```
Undefined scenarios/steps
```

**Causes:**
- Glue packages not specified correctly
- Step definitions not in scanned packages
- Step definitions not on classpath

**Solution:**
- Verify `glue` packages in `@CucumberOptions` include step definition packages
- Ensure step definitions are in packages listed in `glue`
- Check Maven dependencies include `client-tests` module

### 3. "Autwit bean not found"

**Symptom:**
```
No qualifying bean of type 'Autwit' available
```

**Causes:**
- Spring not scanning SDK package
- `AutwitImpl` not on classpath
- Spring Boot not configured correctly

**Solution:**
- Verify `@SpringBootApplication(scanBasePackages = "com.acuver.autwit")`
- Ensure `autwit-client-sdk` is in dependencies
- Check `AutwitImpl` is annotated with `@Component`

### 4. "Feature files in wrong module"

**Symptom:**
- Features not discovered
- Runner can't find features

**Solution:**
- Features MUST be in `client-tests/src/test/resources/features`
- Runner MUST be in `autwit-runner/src/test/java`
- Use Maven to ensure features are on classpath

---

## Maven Build Configuration

### Root pom.xml

```xml
<modules>
    <module>autwit-core</module>
    <module>autwit-client-sdk</module>
    <module>autwit-runner</module>
    <module>client-tests</module>
</modules>
```

### autwit-runner/pom.xml

Must include dependencies:
- `autwit-client-sdk`
- `autwit-internal-testkit`
- `autwit-engine`
- `autwit-adapter-*` (Mongo, Postgres, H2)
- `client-tests` (for step definitions and features)

### client-tests/pom.xml

Must include dependencies:
- `autwit-client-sdk` (ONLY this from AUTWIT)
- `autwit-runner` (for execution)

---

## Execution Flow

```
1. Maven builds all modules
   ↓
2. Spring Boot starts (RunnerApp)
   ↓
3. Spring wires all beans
   ↓
4. TestNG initializes
   ↓
5. CucumberTestRunner discovered
   ↓
6. CucumberOptions parsed
   ↓
7. Features discovered from classpath
   ↓
8. Step definitions discovered via glue
   ↓
9. Scenarios executed
   ↓
10. Results reported
```

---

## Running Tests

### Option 1: Via Maven (Recommended)

```bash
# From root
mvn clean test

# From runner module
cd autwit-runner
mvn test
```

### Option 2: Via Spring Boot

```bash
cd autwit-runner
mvn spring-boot:run
```

### Option 3: Via IDE

1. Run `RunnerApp.main()` as Spring Boot application
2. Or run `CucumberTestRunner` as TestNG test

---

## Verifying Setup

### Checklist

- [ ] Feature files in `client-tests/src/test/resources/features`
- [ ] Step definitions in `client-tests/src/test/java`
- [ ] Runner class in `autwit-runner/src/test/java`
- [ ] `@CucumberOptions` configured correctly
- [ ] `glue` packages include step definition packages
- [ ] `features` path points to feature files
- [ ] Maven dependencies include all required modules
- [ ] Spring Boot scans `com.acuver.autwit` package

---

## Troubleshooting

### Debug Feature Discovery

Add logging to see what Cucumber finds:
```java
@CucumberOptions(
    features = "src/test/resources/features",
    glue = { ... },
    plugin = { "pretty", "html:target/cucumber-reports.html" }
)
```

### Debug Glue Discovery

Check that step definitions are found:
- Verify package names match `glue` configuration
- Check step definitions are annotated with `@Given`, `@When`, `@Then`
- Ensure step definitions are on classpath

### Debug Spring Wiring

Enable Spring debug logging:
```yaml
logging:
  level:
    org.springframework: DEBUG
```

---

## Summary

**Key Points:**
1. **Runner** must be in `autwit-runner/src/test/java`
2. **Features** must be in `client-tests/src/test/resources/features`
3. **Step definitions** must be in `client-tests/src/test/java`
4. **Glue packages** must include step definition packages
5. **Maven** must include all required modules in dependencies
6. **Spring Boot** must scan `com.acuver.autwit` package

**Common Issues:**
- "No features found" → Check feature file location and `@CucumberOptions`
- "Step definitions not found" → Check `glue` packages
- "Autwit bean not found" → Check Spring Boot configuration

**Remember:** Maven build success ≠ Cucumber execution success. Always verify features and step definitions are discovered correctly.

