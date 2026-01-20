# AUTWIT Maven Dependency Analysis Report

**Generated:** 2026-01-16  
**Spring Boot Version:** 3.5.7  
**Java Version:** 21

---

## ✅ Summary

| Category | Status | Count |
|----------|--------|-------|
| **Critical Issues** | ❌ FAIL | 4 |
| **Medium Issues** | ⚠️ WARN | 5 |
| **Minor Issues** | ⚠️ WARN | 3 |
| **Total Issues** | ❌ FAIL | 12 |

---

## 🔴 Critical Issues

### 1. **Multiple SLF4J Bindings (Logback + Log4j2)**

**Problem:** Both `logback-classic` and `log4j-slf4j2-impl` are present in the classpath, causing SLF4J warnings:
```
SLF4J(W): Class path contains multiple SLF4J providers.
SLF4J(W): Found provider [ch.qos.logback.classic.spi.LogbackServiceProvider]
SLF4J(W): Found provider [org.apache.logging.slf4j.SLF4JServiceProvider]
```

**Root Cause:**
- `spring-boot-starter-data-jpa` and `spring-boot-starter-data-mongodb` bring in `spring-boot-starter-logging` (Logback)
- Modules explicitly add `log4j-slf4j2-impl` (Log4j2)
- Both bindings are active simultaneously

**Affected Modules:**
- `autwit-adapter-postgres` (has both logback via JPA starter + log4j2)
- `autwit-adapter-h2` (has both logback via JPA starter + log4j2)
- `autwit-adapter-mongo` (has both logback via Mongo starter + log4j2)
- `autwit-runner` (excludes logback but may transitively get it)
- `client-tests` (excludes logback but may transitively get it)

**Fix:** Exclude `spring-boot-starter-logging` from all Spring Boot starters and use only Log4j2.

---

### 2. **autwit-runner Missing Parent POM**

**Problem:** `autwit-runner/pom.xml` does NOT inherit from `autwit-root`, causing:
- No Spring Boot BOM inheritance
- Duplicate BOM imports
- Version management inconsistencies

**Current State:**
```xml
<!-- autwit-runner/pom.xml - NO <parent> tag -->
<groupId>com.acuver</groupId>
<artifactId>autwit-runner</artifactId>
```

**Fix:** Add parent declaration.

---

### 3. **MongoDB Driver Version Conflict**

**Problem:** `autwit-adapter-mongo` explicitly uses MongoDB driver `4.11.0`, but Spring Boot 3.5.7 manages `5.5.2`.

**Evidence from dependency tree:**
```
[INFO] |  +- (org.mongodb:mongodb-driver-sync:jar:5.5.2:compile - version managed from 5.5.2; omitted for conflict with 4.11.0)
[INFO] +- org.mongodb:mongodb-driver-sync:jar:4.11.0:compile (scope not updated to compile)
```

**Impact:** Potential compatibility issues, version conflicts, and unexpected behavior.

**Fix:** Remove explicit version and let Spring Boot manage it, OR align with Spring Boot's version.

---

### 4. **Log4j2 BOM Version Mismatch**

**Problem:** 
- Root POM: `log4j-bom:2.23.1`
- `autwit-runner`: `log4j-bom:2.24.3` (explicit override)

**Impact:** Inconsistent Log4j2 versions across modules, potential runtime issues.

**Fix:** Remove version override from `autwit-runner` and use root POM's managed version.

---

## ⚠️ Medium Issues

### 5. **Jackson Version Override in autwit-client-sdk**

**Problem:** `autwit-client-sdk` explicitly uses Jackson `2.17.2`, but Spring Boot 3.5.7 manages `2.19.2`.

**Evidence:**
```
[INFO] |  \- (com.fasterxml.jackson.core:jackson-databind:jar:2.19.2:compile - version managed from 2.19.2; omitted for conflict with 2.17.2)
[INFO] +- com.fasterxml.jackson.core:jackson-databind:jar:2.17.2:compile
```

**Impact:** Version conflicts, potential serialization/deserialization issues.

**Fix:** Remove explicit version and let Spring Boot manage it.

---

### 6. **spring-boot-starter-data-jpa in client-tests**

**Problem:** `client-tests` directly declares `spring-boot-starter-data-jpa`, but JPA should be provided by adapters only.

**Current:**
```xml
<!-- client-tests/pom.xml -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>
```

**Impact:** Violates AUTWIT's adapter ownership principle. JPA should come from `autwit-adapter-postgres` or `autwit-adapter-h2` only.

**Fix:** Remove from `client-tests` and ensure adapters provide it.

---

### 7. **autwit-client-sdk Missing Spring Boot BOM**

**Problem:** `autwit-client-sdk` does NOT import Spring Boot BOM, but uses Spring-managed dependencies.

**Impact:** No version management for Spring dependencies, potential conflicts.

**Fix:** Add Spring Boot BOM import OR ensure parent provides it (if parent is added).

---

### 8. **Version Override in client-tests for spring-boot-starter-test**

**Problem:** `client-tests` explicitly sets version for `spring-boot-starter-test`:
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <version>${spring.boot.version}</version>  <!-- Unnecessary -->
    <scope>test</scope>
</dependency>
```

**Impact:** Redundant, but not harmful. Should rely on BOM.

**Fix:** Remove explicit version.

---

### 9. **MongoDB Dependencies Present When Postgres Profile Active**

**Problem:** `client-tests` includes both `autwit-adapter-mongo` and `autwit-adapter-postgres` as compile dependencies.

**Current:**
```xml
<!-- client-tests/pom.xml -->
<dependency>
    <groupId>com.acuver</groupId>
    <artifactId>autwit-adapter-mongo</artifactId>
    <version>${project.version}</version>
</dependency>
<dependency>
    <groupId>com.acuver</groupId>
    <artifactId>autwit-adapter-postgres</artifactId>
    <version>${project.version}</version>
</dependency>
```

**Impact:** Both adapters' dependencies are on classpath even when only one is active. This is acceptable IF adapters use `@ConditionalOnProperty` correctly (which they do), but adds unnecessary dependencies.

**Fix:** Consider making adapters `provided` scope or using profiles to exclude unused adapters.

---

## ⚠️ Minor Issues

### 10. **Duplicate Log4j2 Dependencies**

**Problem:** Some modules declare `log4j-api` and `log4j-core` explicitly when they could use `spring-boot-starter-log4j2`.

**Affected Modules:**
- `autwit-adapter-postgres` (has explicit log4j-api, log4j-core + spring-boot-starter-log4j2)
- `autwit-adapter-h2` (has explicit log4j-api, log4j-core)
- `autwit-adapter-mongo` (has explicit log4j-api, log4j-core)
- `autwit-engine` (has explicit log4j-api, log4j-core)

**Fix:** Use `spring-boot-starter-log4j2` instead of individual artifacts.

---

### 11. **Lombok Version Override in Multiple Modules**

**Problem:** Several modules explicitly set Lombok version instead of relying on parent's `dependencyManagement`.

**Affected Modules:**
- `autwit-adapter-postgres`
- `autwit-adapter-h2`
- `autwit-adapter-mongo`
- `autwit-engine`
- `autwit-domain`
- `autwit-shared`
- `autwit-internal-testkit`
- `autwit-client-sdk`

**Impact:** Redundant but not harmful if versions match.

**Fix:** Remove explicit versions and rely on parent's `dependencyManagement`.

---

### 12. **org.json Duplicate (Minor)**

**Problem:** Both `org.json:json:20250517` and `android-json` (which includes org.json) are present, but exclusions are in place.

**Status:** ✅ **ACCEPTABLE** - Exclusions are correctly configured in `autwit-core/pom.xml` and `autwit-runner/pom.xml`.

---

## ✅ Recommended POM Changes

### Fix 1: Add Parent to autwit-runner

**File:** `autwit-runner/pom.xml`

```xml
<project>
  <modelVersion>4.0.0</modelVersion>

  <parent>
    <groupId>com.acuver</groupId>
    <artifactId>autwit-root</artifactId>
    <version>1.0.0-SNAPSHOT</version>
  </parent>

  <artifactId>autwit-runner</artifactId>
  <packaging>jar</packaging>

  <!-- Remove duplicate Spring Boot BOM import (now from parent) -->
  <!-- Remove duplicate Log4j2 BOM import (now from parent) -->
  <!-- Keep dependencyManagement only for module-specific overrides -->
</project>
```

---

### Fix 2: Exclude spring-boot-starter-logging from JPA/Mongo Starters

**File:** `autwit-core/autwit-adapter-postgres/pom.xml`

```xml
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-data-jpa</artifactId>
  <exclusions>
    <exclusion>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-logging</artifactId>
    </exclusion>
  </exclusions>
</dependency>

<!-- Replace explicit log4j-api, log4j-core with starter -->
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-log4j2</artifactId>
</dependency>
```

**Apply same fix to:**
- `autwit-core/autwit-adapter-h2/pom.xml`
- `autwit-core/autwit-adapter-mongo/pom.xml`

---

### Fix 3: Remove MongoDB Driver Version Override

**File:** `autwit-core/autwit-adapter-mongo/pom.xml`

```xml
<!-- BEFORE -->
<dependency>
  <groupId>org.mongodb</groupId>
  <artifactId>mongodb-driver-sync</artifactId>
  <version>${mongo.driver.version}</version>  <!-- REMOVE THIS -->
</dependency>

<!-- AFTER -->
<dependency>
  <groupId>org.mongodb</groupId>
  <artifactId>mongodb-driver-sync</artifactId>
  <!-- Version managed by Spring Boot BOM (5.5.2) -->
</dependency>
```

**Also remove from `autwit-core/pom.xml` properties:**
```xml
<!-- REMOVE -->
<!-- <mongo.driver.version>4.11.0</mongo.driver.version> -->
```

---

### Fix 4: Remove Jackson Version Override

**File:** `autwit-client-sdk/pom.xml`

```xml
<!-- BEFORE -->
<dependency>
  <groupId>com.fasterxml.jackson.core</groupId>
  <artifactId>jackson-databind</artifactId>
  <version>2.17.2</version>  <!-- REMOVE THIS -->
</dependency>

<!-- AFTER -->
<dependency>
  <groupId>com.fasterxml.jackson.core</groupId>
  <artifactId>jackson-databind</artifactId>
  <!-- Version managed by Spring Boot BOM (2.19.2) -->
</dependency>
```

**Also remove from root `pom.xml` properties:**
```xml
<!-- REMOVE -->
<!-- <jackson.version>2.17.2</jackson.version> -->
```

---

### Fix 5: Remove spring-boot-starter-data-jpa from client-tests

**File:** `client-tests/pom.xml`

```xml
<!-- REMOVE THIS ENTIRE DEPENDENCY -->
<!--
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>
-->
```

**Reason:** JPA is provided by `autwit-adapter-postgres` or `autwit-adapter-h2`.

---

### Fix 6: Remove Version Override from client-tests spring-boot-starter-test

**File:** `client-tests/pom.xml`

```xml
<!-- BEFORE -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <version>${spring.boot.version}</version>  <!-- REMOVE THIS -->
    <scope>test</scope>
    <!-- ... -->
</dependency>

<!-- AFTER -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <!-- Version managed by Spring Boot BOM -->
    <scope>test</scope>
    <!-- ... -->
</dependency>
```

---

### Fix 7: Remove Log4j2 BOM Version Override from autwit-runner

**File:** `autwit-runner/pom.xml`

```xml
<!-- REMOVE THIS ENTIRE dependencyManagement SECTION (if parent is added) -->
<!-- OR remove just the Log4j2 BOM import if keeping separate BOM -->
<dependencyManagement>
  <dependencies>
    <!-- REMOVE -->
    <!--
    <dependency>
      <groupId>org.apache.logging.log4j</groupId>
      <artifactId>log4j-bom</artifactId>
      <version>${log4j.version}</version>
      <type>pom</type>
      <scope>import</scope>
    </dependency>
    -->
  </dependencies>
</dependencyManagement>
```

**Also remove from properties:**
```xml
<!-- REMOVE -->
<!-- <log4j.version>2.24.3</log4j.version> -->
```

---

### Fix 8: Add Spring Boot BOM to autwit-client-sdk (if not inheriting)

**File:** `autwit-client-sdk/pom.xml`

```xml
<dependencyManagement>
  <dependencies>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-dependencies</artifactId>
      <version>${spring.boot.version}</version>
      <type>pom</type>
      <scope>import</scope>
    </dependency>
  </dependencies>
</dependencyManagement>
```

**OR ensure parent provides it (preferred if parent is added).**

---

### Fix 9: Standardize Log4j2 Usage

**Replace explicit log4j artifacts with starter in:**
- `autwit-core/autwit-engine/pom.xml`
- `autwit-core/autwit-adapter-postgres/pom.xml` (if not already using starter)
- `autwit-core/autwit-adapter-h2/pom.xml`
- `autwit-core/autwit-adapter-mongo/pom.xml`

**Pattern:**
```xml
<!-- REMOVE -->
<!--
<dependency>
  <groupId>org.apache.logging.log4j</groupId>
  <artifactId>log4j-api</artifactId>
</dependency>
<dependency>
  <groupId>org.apache.logging.log4j</groupId>
  <artifactId>log4j-core</artifactId>
</dependency>
-->

<!-- ADD -->
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-log4j2</artifactId>
</dependency>
```

---

## ✅ Verification Commands

### 1. Check Dependency Tree for Conflicts

```bash
# Full tree
mvn dependency:tree -Dverbose > dependency-tree-full.txt

# Check for SLF4J bindings
mvn dependency:tree | findstr /i "logback\|log4j-slf4j"

# Check for version conflicts
mvn dependency:tree -Dverbose | findstr /i "conflict\|omitted for conflict"

# Check for Jackson versions
mvn dependency:tree | findstr /i "jackson-databind"

# Check for MongoDB versions
mvn dependency:tree | findstr /i "mongodb-driver"
```

### 2. Verify Effective POM

```bash
# Check effective POM for a specific module
mvn help:effective-pom -pl autwit-runner > autwit-runner-effective-pom.xml
mvn help:effective-pom -pl client-tests > client-tests-effective-pom.xml
```

### 3. Check for Duplicate Classes

```bash
# Use Maven Enforcer Plugin (if configured)
mvn enforcer:enforce

# Or use maven-dependency-plugin
mvn dependency:analyze-duplicate
```

### 4. Verify SLF4J Bindings

```bash
# After fixes, run tests and check logs for SLF4J warnings
mvn clean test 2>&1 | findstr /i "SLF4J"
```

### 5. Verify Spring Boot BOM Inheritance

```bash
# Check if all modules inherit Spring Boot versions correctly
mvn dependency:tree -Dincludes=org.springframework.boot:* | findstr /i "version managed"
```

---

## 📋 Summary of Required Changes

| Module | Change Type | Priority |
|--------|-------------|----------|
| `autwit-runner/pom.xml` | Add parent, remove duplicate BOMs | 🔴 Critical |
| `autwit-core/autwit-adapter-postgres/pom.xml` | Exclude logback, use log4j2 starter | 🔴 Critical |
| `autwit-core/autwit-adapter-h2/pom.xml` | Exclude logback, use log4j2 starter | 🔴 Critical |
| `autwit-core/autwit-adapter-mongo/pom.xml` | Exclude logback, remove MongoDB version, use log4j2 starter | 🔴 Critical |
| `autwit-client-sdk/pom.xml` | Remove Jackson version, add Spring Boot BOM | ⚠️ Medium |
| `client-tests/pom.xml` | Remove JPA starter, remove test version override | ⚠️ Medium |
| `autwit-core/pom.xml` | Remove mongo.driver.version property | ⚠️ Medium |
| `pom.xml` (root) | Remove jackson.version property | ⚠️ Minor |
| `autwit-core/autwit-engine/pom.xml` | Use log4j2 starter | ⚠️ Minor |
| All adapter modules | Remove explicit Lombok versions | ⚠️ Minor |

---

## ✅ Post-Fix Validation Checklist

- [ ] No SLF4J multiple binding warnings in logs
- [ ] All modules inherit Spring Boot BOM correctly
- [ ] No version conflicts in `mvn dependency:tree -Dverbose`
- [ ] MongoDB driver version matches Spring Boot managed version (5.5.2)
- [ ] Jackson version matches Spring Boot managed version (2.19.2)
- [ ] Log4j2 version consistent across all modules (2.23.1 from root)
- [ ] `autwit-runner` has parent POM
- [ ] `client-tests` does NOT have `spring-boot-starter-data-jpa`
- [ ] All tests pass: `mvn clean test`
- [ ] No duplicate class warnings

---

## 📚 References

- [Spring Boot Dependency Management](https://docs.spring.io/spring-boot/docs/current/reference/html/dependency-versions.html)
- [SLF4J Multiple Bindings](http://www.slf4j.org/codes.html#multiple_bindings)
- [Maven Dependency Management](https://maven.apache.org/guides/introduction/introduction-to-dependency-mechanism.html)
- [Spring Boot 3.5.7 Release Notes](https://github.com/spring-projects/spring-boot/releases/tag/v3.5.7)

---

**Report Generated by:** Maven Dependency Analysis Tool  
**Next Review:** After implementing fixes

