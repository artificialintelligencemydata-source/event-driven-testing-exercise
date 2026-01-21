# 🔒 AUTWIT EXECUTION MODEL — FINAL ARCHITECTURAL LOCK

**Date:** 2026-01-06  
**Status:** ✅ **LOCKED — NO FURTHER EXPERIMENTS**

---

## 🎯 EXECUTIVE SUMMARY

This document **permanently locks** AUTWIT's execution model. All previous attempts to make `autwit-runner` the test executor have failed due to fundamental JVM/Maven/Cucumber constraints. The **ONLY correct model** is documented here.

**Key Principle:** The test executor MUST be in the same module as the step definitions, because Cucumber can only discover glue that is on the test classpath.

---

## ❌ WHY PREVIOUS MODELS FAILED

### Attempt 1: Runner in `autwit-runner`, Step Definitions in `client-tests`

**What Was Tried:**
- Runner class in `autwit-runner/src/test/java`
- Step definitions in `client-tests/src/test/java`
- Runner's `@CucumberOptions` tries to specify client glue packages

**Why It Failed:**
- Cucumber can ONLY discover glue that is on the test classpath
- If `client-tests` is not a dependency of `autwit-runner`, step definitions are not on the classpath
- Making `autwit-runner` depend on `client-tests` violates architecture (runner must not know client code)
- `cucumber.properties` cannot magically load classes from another module

**Error:** `Undefined step: "I place an order..."`

### Attempt 2: Copying Step Definitions to Runner Module

**What Was Tried:**
- Maven resource plugin copies step definitions from `client-tests` to `autwit-runner`

**Why It Failed:**
- Copying `.java` files doesn't compile them
- Copying `.class` files breaks package structure
- Classpath confusion and duplicate class errors
- Fundamentally broken approach

**Error:** `ClassNotFoundException` or `NoClassDefFoundError`

### Attempt 3: Dynamic Glue Discovery via Reflection

**What Was Tried:**
- `CucumberGlueBootstrap` tries to dynamically add glue packages via system properties

**Why It Failed:**
- `cucumber.properties` can only override `@CucumberOptions` if packages are already on classpath
- System properties don't magically add classes to classpath
- Reflection cannot discover classes that aren't loaded

**Error:** Glue packages specified but classes not found

---

## ✅ THE ONLY CORRECT MODEL

### Core Principle

**The test executor MUST be in the same module as the step definitions.**

This is not a design choice—it's a **JVM/Cucumber/Maven constraint**.

### Final Architecture

```
client-tests/                    ← THIS is the test executor module
├── src/test/java/
│   └── com/bjs/tests/
│       ├── runner/
│       │   └── ClientCucumberRunner.java  ← Runner class (MUST be here)
│       └── stepDefinitions/
│           └── *.java                     ← Step definitions (MUST be here)
└── src/test/resources/
    └── features/
        └── *.feature                      ← Feature files (MUST be here)

autwit-runner/                   ← THIS is a library, NOT an executor
├── src/main/java/
│   └── com/acuver/autwit/runner/
│       └── BaseAutwitCucumberRunner.java  ← Base runner (abstract/concrete)
└── src/main/resources/
    └── application.yaml                    ← Spring configuration
```

### Dependency Direction

```
client-tests
    ↓ (depends on)
autwit-runner (library)
    ↓ (depends on)
autwit-client-sdk
autwit-internal-testkit
autwit-engine
autwit-adapter-*
```

**Key Rule:** `autwit-runner` must NEVER depend on `client-tests`.

---

## 📋 IMPLEMENTATION REQUIREMENTS

### 1. Base Runner in `autwit-runner`

**File:** `autwit-runner/src/main/java/com/acuver/autwit/runner/BaseAutwitCucumberRunner.java`

```java
package com.acuver.autwit.runner;

import io.cucumber.testng.AbstractTestNGCucumberTests;
import org.testng.ITestContext;
import org.testng.annotations.DataProvider;

import java.util.Arrays;

/**
 * Base runner for AUTWIT Cucumber tests.
 * 
 * Clients MUST extend this class and provide their own @CucumberOptions
 * with client-specific glue packages.
 * 
 * This class provides:
 * - Common TestNG data providers
 * - Retry/scenario filtering logic
 * - AUTWIT infrastructure wiring
 */
public abstract class BaseAutwitCucumberRunner extends AbstractTestNGCucumberTests {

    @Override
    @DataProvider(name = "scenarios", parallel = true)
    public Object[][] scenarios() {
        return super.scenarios();
    }

    @DataProvider(name = "filteredScenarios", parallel = true)
    public Object[][] filteredScenarios(ITestContext context) {
        String retryParam = context.getCurrentXmlTest().getParameter("scenariosToRun");
        Object[][] original = scenarios();

        if (retryParam == null || retryParam.isBlank()) {
            return original;
        }

        System.out.println("🔁 RETRY MODE: Running only: " + retryParam);

        String[] scenariosToRun = retryParam.split(",");
        return Arrays.stream(original)
                .filter(s -> Arrays.stream(scenariosToRun)
                        .anyMatch(scenario -> s[0].toString().contains(scenario.trim())))
                .toArray(Object[][]::new);
    }
}
```

**Key Points:**
- ✅ In `src/main/java` (not `src/test/java`) — it's a library class
- ✅ Abstract or concrete (clients extend it)
- ✅ Provides common TestNG data providers
- ✅ NO `@CucumberOptions` — clients provide their own

### 2. Client Runner in `client-tests`

**File:** `client-tests/src/test/java/com/bjs/tests/runner/ClientCucumberRunner.java`

```java
package com.bjs.tests.runner;

import com.acuver.autwit.runner.BaseAutwitCucumberRunner;
import io.cucumber.testng.CucumberOptions;

/**
 * Client-specific Cucumber runner.
 * 
 * This is the ACTUAL test executor.
 * Maven runs tests from this class.
 */
@CucumberOptions(
        features = "classpath:features",
        glue = {
                "com.bjs.tests.stepDefinitions",      // ← Client step definitions
                "com.acuver.autwit.config",            // ← AUTWIT Spring config
                "com.acuver.autwit.internal"            // ← AUTWIT hooks
        },
        plugin = {
                "pretty",
                "html:target/cucumber-reports.html",
                "io.qameta.allure.cucumber7jvm.AllureCucumber7Jvm"
        },
        monochrome = true
)
public class ClientCucumberRunner extends BaseAutwitCucumberRunner {
    // Client can override methods if needed
}
```

**Key Points:**
- ✅ In `client-tests/src/test/java` — this is the test executor
- ✅ Extends `BaseAutwitCucumberRunner`
- ✅ Provides `@CucumberOptions` with client glue packages
- ✅ Maven runs `mvn test` from `client-tests` module

### 3. TestNG Configuration

**File:** `client-tests/src/test/resources/testng.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<suite name="ClientTestSuite" verbose="1" parallel="tests" thread-count="4">
    <parameter name="scenariosToRun" value=""/>
    <test name="ClientCucumberTests" parallel="methods" thread-count="4">
        <parameter name="dataProvider" value="scenarios"/>
        <classes>
            <class name="com.bjs.tests.runner.ClientCucumberRunner"/>
        </classes>
    </test>
</suite>
```

**Key Points:**
- ✅ In `client-tests/src/test/resources`
- ✅ References `ClientCucumberRunner` (not `CucumberRunner` from `autwit-runner`)

### 4. Maven Configuration

**File:** `client-tests/pom.xml`

```xml
<dependencies>
    <!-- AUTWIT SDK -->
    <dependency>
        <groupId>com.acuver</groupId>
        <artifactId>autwit-client-sdk</artifactId>
        <version>${project.version}</version>
    </dependency>

    <!-- AUTWIT Runner (library) -->
    <dependency>
        <groupId>com.acuver</groupId>
        <artifactId>autwit-runner</artifactId>
        <version>${project.version}</version>
        <scope>test</scope>
    </dependency>

    <!-- Cucumber + TestNG -->
    <dependency>
        <groupId>io.cucumber</groupId>
        <artifactId>cucumber-java</artifactId>
        <version>${cucumber.version}</version>
        <scope>test</scope>
    </dependency>

    <dependency>
        <groupId>io.cucumber</groupId>
        <artifactId>cucumber-testng</artifactId>
        <version>${cucumber.version}</version>
        <scope>test</scope>
    </dependency>

    <dependency>
        <groupId>org.testng</groupId>
        <artifactId>testng</artifactId>
        <version>${testng.version}</version>
        <scope>test</scope>
    </dependency>
</dependencies>

<build>
    <plugins>
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-surefire-plugin</artifactId>
            <configuration>
                <suiteXmlFiles>
                    <suiteXmlFile>src/test/resources/testng.xml</suiteXmlFile>
                </suiteXmlFiles>
            </configuration>
        </plugin>
    </plugins>
</build>
```

**Key Points:**
- ✅ `autwit-runner` is a `test` scope dependency (library, not executor)
- ✅ Surefire plugin runs tests from `client-tests` module

### 5. Remove Runner from `autwit-runner`

**Action:** Delete `autwit-runner/src/test/java/com/acuver/autwit/runner/CucumberRunner.java`

**Reason:** This was the broken executor. It's replaced by `BaseAutwitCucumberRunner` in `src/main/java`.

---

## 🧠 ANSWERS TO CORE QUESTIONS

### 1. Who is the legitimate Maven test executor?

**Answer:** `client-tests` module is the test executor.

- Maven runs `mvn test` from `client-tests`
- TestNG discovers `ClientCucumberRunner` in `client-tests`
- Cucumber executes features and step definitions from `client-tests`

**Why:** The executor must be in the same module as step definitions (JVM classpath constraint).

### 2. Why can Cucumber only bind glue that is present on the test classpath?

**Answer:** Cucumber uses Java reflection to discover step definitions.

- Reflection can only find classes that are loaded by the classloader
- Classes are only loaded if they're on the classpath
- Maven test classpath includes `target/test-classes` of the current module and its test dependencies
- If step definitions are in `client-tests` but executor is in `autwit-runner`, they're not on the same classpath

**Why:** This is a fundamental JVM constraint, not a Cucumber limitation.

### 3. Why does `cucumber.properties` NOT magically load glue from another module?

**Answer:** `cucumber.properties` can only override `@CucumberOptions` if the glue packages are already on the classpath.

- `cucumber.properties` is read at runtime, not compile time
- It cannot add classes to the classpath
- It can only tell Cucumber which packages to scan from classes that are already loaded

**Why:** Properties files cannot modify the JVM classpath.

### 4. Why copying step definitions across modules is fundamentally broken?

**Answer:** Copying `.java` files doesn't compile them, and copying `.class` files breaks package structure.

- Maven compiles `.java` files in `src/test/java` to `target/test-classes`
- Copying `.java` files to another module doesn't trigger compilation
- Copying `.class` files breaks package structure (classes expect to be in their original packages)
- Classpath confusion leads to `ClassNotFoundException` or duplicate class errors

**Why:** Java's package system and classloader model don't support this.

### 5. Why the runner class MUST live in a client-consumable artifact?

**Answer:** The runner class must be in the same module as step definitions to be on the same classpath.

- If runner is in `autwit-runner` and step definitions are in `client-tests`, they're on different classpaths
- Cucumber cannot discover step definitions from a different classpath
- The runner must be in `client-tests` to access step definitions

**Why:** JVM classpath isolation prevents cross-module class discovery.

### 6. What is the ONLY correct place for `BaseAutwitCucumberRunner`?

**Answer:** `autwit-runner/src/main/java/com/acuver/autwit/runner/BaseAutwitCucumberRunner.java`

- It's a library class (not a test class)
- Clients extend it in their own test modules
- It provides common functionality (data providers, retry logic)
- It does NOT have `@CucumberOptions` (clients provide their own)

**Why:** Base classes belong in libraries, not test code.

### 7. Why `autwit-runner` must be a library and never a test executor?

**Answer:** `autwit-runner` must not know about client code.

- If `autwit-runner` is the executor, it must depend on `client-tests` (violates architecture)
- If `autwit-runner` is a library, clients depend on it (correct direction)
- Library classes can be reused across multiple client projects
- Test executors are client-specific

**Why:** Dependency direction must flow from client to framework, not framework to client.

---

## 🚫 WHAT MUST NEVER BE ATTEMPTED AGAIN

### ❌ DO NOT:

1. **Put runner in `autwit-runner/src/test/java`**
   - Runner cannot discover step definitions from `client-tests`
   - Violates classpath isolation

2. **Make `autwit-runner` depend on `client-tests`**
   - Violates dependency direction
   - Runner must not know client code

3. **Copy step definitions to `autwit-runner`**
   - Broken compilation model
   - Package structure violations

4. **Use `cucumber.properties` to dynamically load glue**
   - Properties cannot modify classpath
   - Glue packages must be on classpath

5. **Use reflection to discover step definitions**
   - Reflection cannot find classes not on classpath
   - Fundamentally broken approach

6. **Make `autwit-runner` the Maven test executor**
   - Requires `autwit-runner` to know client packages
   - Violates architecture

---

## ✅ EXECUTION FLOW (FINAL)

```
1. Developer runs: mvn test (from root or client-tests)
   ↓
2. Maven builds all modules
   ↓
3. Maven Surefire runs tests from client-tests module
   ↓
4. TestNG discovers ClientCucumberRunner
   ↓
5. ClientCucumberRunner extends BaseAutwitCucumberRunner
   ↓
6. @CucumberOptions specifies glue packages:
   - com.bjs.tests.stepDefinitions (client)
   - com.acuver.autwit.config (AUTWIT)
   - com.acuver.autwit.internal (AUTWIT)
   ↓
7. Cucumber discovers features from classpath:features
   ↓
8. Cucumber discovers step definitions from glue packages
   ↓
9. Spring Boot starts (via @SpringBootTest in CucumberSpringConfiguration)
   ↓
10. AUTWIT beans are wired (Engine, Adapters, SDK)
   ↓
11. Scenarios execute
   ↓
12. Results reported
```

---

## 📦 MENTAL MODEL FOR FUTURE ENGINEERS

### AUTWIT is a Framework, Not an Executor

**Think of AUTWIT like JUnit or TestNG:**

- JUnit provides `@Test` annotation and test runners
- Clients write test classes that use JUnit annotations
- Clients run their own tests (JUnit doesn't run them)

**AUTWIT is the same:**

- AUTWIT provides `BaseAutwitCucumberRunner` and infrastructure
- Clients write step definitions and extend the base runner
- Clients run their own tests (AUTWIT doesn't run them)

### Module Responsibilities

**`autwit-runner`:**
- Provides base runner class (library)
- Provides Spring Boot configuration
- Provides TestNG data providers
- **Does NOT** execute tests
- **Does NOT** know client packages

**`client-tests`:**
- Extends base runner
- Provides `@CucumberOptions` with client glue
- Contains step definitions
- Contains feature files
- **IS** the test executor

### Dependency Flow

```
Client Code
    ↓ (depends on)
AUTWIT Framework
    ↓ (provides)
Infrastructure
```

**Never reverse this flow.**

---

## 🔒 FINAL LOCK STATEMENT

**This execution model is LOCKED. No further experiments.**

- ✅ Runner class in `client-tests` module
- ✅ Base runner in `autwit-runner/src/main/java` (library)
- ✅ Client extends base runner with own `@CucumberOptions`
- ✅ Maven runs tests from `client-tests`
- ✅ `autwit-runner` is a library, not an executor

**Any deviation from this model will fail due to JVM/Cucumber/Maven constraints.**

---

**END OF DOCUMENT**

