# SDK VISIBILITY VIOLATIONS ANALYSIS

**Date:** 2026-01-05  
**Purpose:** Identify all public SDK types and propose minimal refactor to single `Autwit` facade  
**Mode:** Analysis Only (No Code Changes)

---

## 1️⃣ PUBLIC TYPES IN SDK PACKAGE

### Complete List

| Type | Visibility | Location | Status |
|------|-----------|----------|--------|
| `Autwit` | `public interface` | `Autwit.java` | ✅ **ALLOWED** (only public interface) |
| `AutwitImpl` | `class` (package-private) | `AutwitImpl.java` | ✅ **OK** (already package-private) |
| `EventExpectation` | `public interface` | `EventExpectation.java` | ❌ **VIOLATION** |
| `EventExpectationImpl` | `public class` | `EventExpectationImpl.java` | ❌ **VIOLATION** |
| `ScenarioStepStatus` | `public interface` | `ScenarioStepStatus.java` | ❌ **VIOLATION** |
| `ScenarioStepStatusImpl` | `public class` | `ScenarioStepStatusImpl.java` | ❌ **VIOLATION** |
| `ContextAccessor` | `public interface` | `ContextAccessor.java` | ❌ **VIOLATION** |
| `ContextAccessorImpl` | `class` (package-private) | `ContextAccessorImpl.java` | ✅ **OK** (already package-private) |
| `ApiClient` | `public interface` | `ApiClient.java` | ❌ **VIOLATION** |
| `ApiClientImpl` | `public class` | `ApiClientImpl.java` | ❌ **VIOLATION** |
| `SoftAssertions` | `public interface` | `SoftAssertions.java` | ❌ **VIOLATION** |
| `SoftAssertionsImpl` | `public class` | `SoftAssertionsImpl.java` | ❌ **VIOLATION** |
| `ScenarioContextAccessPort` | `public interface` | `ScenarioContextAccessPort.java` | ❌ **VIOLATION** |
| `ScenarioContextAccessAdapter` | `public class` | `ScenarioContextAccessAdapter.java` | ❌ **VIOLATION** |
| `TestData` | `public class` | `testData/TestData.java` | ❌ **VIOLATION** |

**Total Violations:** 11 public types (excluding `Autwit`)

---

## 2️⃣ CLIENT CODE USAGE ANALYSIS

### Direct Imports from Client Tests

**File:** `client-tests/src/test/java/com/bjs/tests/stepDefinitions/EventDrivenOrderLifecycleStepDefsFacedBased.java`

**Line 5:** `import com.acuver.autwit.client.sdk.EventExpectation;`

**Usage Pattern:**
```java
EventExpectation expectation = autwit.expectEvent(orderId, eventType);
expectation.assertSatisfied();
```

**Impact:** ⚠️ **CRITICAL** - Client code directly imports `EventExpectation` interface

**Other Types:** No direct imports found in client code for:
- `ScenarioStepStatus`
- `ContextAccessor`
- `ApiClient`
- `SoftAssertions`
- Any `*Impl` classes
- `ScenarioContextAccessPort`
- `ScenarioContextAccessAdapter`
- `TestData`

---

## 3️⃣ INTERNAL USAGE ANALYSIS

### Within SDK Package

**AutwitImpl.java** uses:
- `EventExpectation` (return type) - Line 19
- `EventExpectationImpl` (instantiation) - Line 20
- `ScenarioStepStatus` (return type) - Line 23
- `ScenarioStepStatusImpl` (instantiation) - Line 24
- `ContextAccessor` (return type) - Line 27
- `ContextAccessorImpl` (instantiation) - Line 28
- `ScenarioContextAccessPort` (field) - Line 17

**ContextAccessorImpl.java** uses:
- `ApiClient` (return type) - Line 28
- `ApiClientImpl` (instantiation) - Line 29
- `SoftAssertions` (return type) - Line 33
- `SoftAssertionsImpl` (instantiation) - Line 34
- `ScenarioContextAccessPort` (field) - Line 10

**ApiClientImpl.java** uses:
- `ScenarioContextAccessPort` (field) - Line 10

**No External Usage:**
- No imports from `autwit-core` packages
- No imports from `autwit-runner` packages
- `TestData` is commented out in `TestDataPort.java` (not used)

---

## 4️⃣ RECOMMENDED VISIBILITY CHANGES

### Strategy: Nested Interfaces + Package-Private Implementations

| Current Type | Current Visibility | Recommended Change | Rationale |
|--------------|-------------------|-------------------|-----------|
| `EventExpectation` | `public interface` | **Nested in `Autwit`** | Returned by `Autwit.expectEvent()`, client uses it |
| `EventExpectationImpl` | `public class` | **package-private class** | Implementation detail, only used within SDK |
| `ScenarioStepStatus` | `public interface` | **Nested in `Autwit`** | Returned by `Autwit.step()`, client uses it |
| `ScenarioStepStatusImpl` | `public class` | **package-private class** | Implementation detail, only used within SDK |
| `ContextAccessor` | `public interface` | **Nested in `Autwit`** | Returned by `Autwit.context()`, client uses it |
| `ContextAccessorImpl` | `class` (package-private) | **No change** | Already package-private ✅ |
| `ApiClient` | `public interface` | **Nested in `ContextAccessor`** | Returned by `ContextAccessor.api()`, client uses it |
| `ApiClientImpl` | `public class` | **package-private class** | Implementation detail, only used within SDK |
| `SoftAssertions` | `public interface` | **Nested in `ContextAccessor`** | Returned by `ContextAccessor.assertions()`, client uses it |
| `SoftAssertionsImpl` | `public class` | **package-private class** | Implementation detail, only used within SDK |
| `ScenarioContextAccessPort` | `public interface` | **Move to `autwit-core` or make package-private** | Internal port, not client-facing |
| `ScenarioContextAccessAdapter` | `public class` | **Move to `autwit-core` or make package-private** | Internal adapter, not client-facing |
| `TestData` | `public class` | **Move to `autwit-core` or remove** | Not used, appears to be experimental |

---

## 5️⃣ PROPOSED REFACTOR PLAN

### Phase 1: Nest Return Type Interfaces (CRITICAL)

**Goal:** Make `EventExpectation`, `ScenarioStepStatus`, `ContextAccessor` nested in `Autwit`

**Changes:**

1. **Autwit.java:**
   ```java
   public interface Autwit {
       interface EventExpectation {
           void assertSatisfied();
       }
       
       interface ScenarioStepStatus {
           void markStepSuccess();
           void markStepFailed(String reason);
       }
       
       interface ContextAccessor {
           void setCurrentStep(String stepName);
           <T> void set(String key, T value);
           <T> T get(String key);
           ApiClient api();
           SoftAssertions assertions();
           void setOrderId(String orderId);
           
           interface ApiClient {
               Response createOrder(Map<String, Object> payload);
           }
           
           interface SoftAssertions {
               SoftAssert getSoftAssert();
               void assertAll();
           }
       }
       
       EventExpectation expectEvent(String orderId, String eventType);
       ScenarioStepStatus step();
       ContextAccessor context();
   }
   ```

2. **Delete standalone interface files:**
   - `EventExpectation.java` → Remove (nested in `Autwit`)
   - `ScenarioStepStatus.java` → Remove (nested in `Autwit`)
   - `ContextAccessor.java` → Remove (nested in `Autwit`)
   - `ApiClient.java` → Remove (nested in `ContextAccessor`)
   - `SoftAssertions.java` → Remove (nested in `ContextAccessor`)

3. **Update implementation classes:**
   - `EventExpectationImpl` → Change to `implements Autwit.EventExpectation`
   - `ScenarioStepStatusImpl` → Change to `implements Autwit.ScenarioStepStatus`
   - `ContextAccessorImpl` → Change to `implements Autwit.ContextAccessor`
   - `ApiClientImpl` → Change to `implements Autwit.ContextAccessor.ApiClient`
   - `SoftAssertionsImpl` → Change to `implements Autwit.ContextAccessor.SoftAssertions`

4. **Update client code:**
   - `EventDrivenOrderLifecycleStepDefsFacedBased.java`:
     - Change: `import com.acuver.autwit.client.sdk.EventExpectation;`
     - To: `import com.acuver.autwit.client.sdk.Autwit.EventExpectation;`
     - OR: Use fully qualified name: `Autwit.EventExpectation`

**Risk:** ⚠️ **LOW** - Only affects import statements, no behavior change

---

### Phase 2: Make Implementation Classes Package-Private

**Goal:** Hide all `*Impl` classes from external packages

**Changes:**

1. **EventExpectationImpl.java:**
   - Change: `public class EventExpectationImpl`
   - To: `class EventExpectationImpl`

2. **ScenarioStepStatusImpl.java:**
   - Change: `public class ScenarioStepStatusImpl`
   - To: `class ScenarioStepStatusImpl`

3. **ApiClientImpl.java:**
   - Change: `public class ApiClientImpl`
   - To: `class ApiClientImpl`

4. **SoftAssertionsImpl.java:**
   - Change: `public class SoftAssertionsImpl`
   - To: `class SoftAssertionsImpl`

**Risk:** ✅ **NONE** - Already only used within SDK package

---

### Phase 3: Move Internal Ports/Adapters

**Goal:** Move `ScenarioContextAccessPort` and `ScenarioContextAccessAdapter` to internal package

**Option A: Move to `autwit-core`**
- Move to: `autwit-core/autwit-domain/src/main/java/com/acuver/autwit/core/ports/`
- Update imports in `AutwitImpl` and `ContextAccessorImpl`

**Option B: Make Package-Private (if only used in SDK)**
- Change `ScenarioContextAccessPort` to `interface` (package-private)
- Change `ScenarioContextAccessAdapter` to `class` (package-private)
- Keep in SDK package if no external dependencies

**Recommendation:** **Option B** (simpler, no cross-module dependency)

**Changes:**

1. **ScenarioContextAccessPort.java:**
   - Change: `public interface ScenarioContextAccessPort`
   - To: `interface ScenarioContextAccessPort`

2. **ScenarioContextAccessAdapter.java:**
   - Change: `public class ScenarioContextAccessAdapter`
   - To: `class ScenarioContextAccessAdapter`

**Risk:** ✅ **NONE** - Only used within SDK package

---

### Phase 4: Remove or Move TestData

**Goal:** Remove unused `TestData` class

**Changes:**

1. **Delete:** `autwit-client-sdk/src/main/java/com/acuver/autwit/client/sdk/testData/TestData.java`

**Rationale:**
- Not used in any code
- Commented out in `TestDataPort.java`
- Appears to be experimental/unused code

**Risk:** ✅ **NONE** - Not referenced anywhere

---

## 6️⃣ SAFE REFACTOR ORDER

### Recommended Sequence

**Step 1: Nest Interfaces (Phase 1)**
- ✅ Highest impact (removes 5 public interfaces)
- ⚠️ Requires client code update (1 file)
- ✅ No behavior change

**Step 2: Make Impl Classes Package-Private (Phase 2)**
- ✅ Zero risk
- ✅ No client impact
- ✅ Immediate visibility reduction

**Step 3: Make Internal Ports Package-Private (Phase 3)**
- ✅ Zero risk
- ✅ No client impact
- ✅ Cleanup internal structure

**Step 4: Remove TestData (Phase 4)**
- ✅ Zero risk
- ✅ No impact
- ✅ Code cleanup

---

## 7️⃣ FINAL STATE

### After Refactor

**Public Types in SDK:**
- ✅ `Autwit` (public interface) - **ONLY public type**

**Nested Public Interfaces:**
- ✅ `Autwit.EventExpectation` (nested)
- ✅ `Autwit.ScenarioStepStatus` (nested)
- ✅ `Autwit.ContextAccessor` (nested)
- ✅ `Autwit.ContextAccessor.ApiClient` (nested)
- ✅ `Autwit.ContextAccessor.SoftAssertions` (nested)

**Package-Private Types:**
- ✅ `AutwitImpl` (package-private)
- ✅ `EventExpectationImpl` (package-private)
- ✅ `ScenarioStepStatusImpl` (package-private)
- ✅ `ContextAccessorImpl` (package-private)
- ✅ `ApiClientImpl` (package-private)
- ✅ `SoftAssertionsImpl` (package-private)
- ✅ `ScenarioContextAccessPort` (package-private)
- ✅ `ScenarioContextAccessAdapter` (package-private)

**Removed:**
- ✅ `TestData` (deleted)

**Client Imports:**
```java
// BEFORE:
import com.acuver.autwit.client.sdk.Autwit;
import com.acuver.autwit.client.sdk.EventExpectation;

// AFTER:
import com.acuver.autwit.client.sdk.Autwit;
// EventExpectation accessed as Autwit.EventExpectation (or fully qualified)
```

---

## 8️⃣ VIOLATION SUMMARY

### Critical Violations (Client-Facing)

| Violation | File | Impact | Priority |
|-----------|------|--------|----------|
| `EventExpectation` public interface | `EventExpectation.java` | Client imports it | 🔴 **HIGH** |
| `ScenarioStepStatus` public interface | `ScenarioStepStatus.java` | Returned by Autwit | 🟡 **MEDIUM** |
| `ContextAccessor` public interface | `ContextAccessor.java` | Returned by Autwit | 🟡 **MEDIUM** |
| `ApiClient` public interface | `ApiClient.java` | Returned by ContextAccessor | 🟡 **MEDIUM** |
| `SoftAssertions` public interface | `SoftAssertions.java` | Returned by ContextAccessor | 🟡 **MEDIUM** |

### Implementation Violations (Internal)

| Violation | File | Impact | Priority |
|-----------|------|--------|----------|
| `EventExpectationImpl` public class | `EventExpectationImpl.java` | Implementation detail | 🟢 **LOW** |
| `ScenarioStepStatusImpl` public class | `ScenarioStepStatusImpl.java` | Implementation detail | 🟢 **LOW** |
| `ApiClientImpl` public class | `ApiClientImpl.java` | Implementation detail | 🟢 **LOW** |
| `SoftAssertionsImpl` public class | `SoftAssertionsImpl.java` | Implementation detail | 🟢 **LOW** |

### Internal Structure Violations

| Violation | File | Impact | Priority |
|-----------|------|--------|----------|
| `ScenarioContextAccessPort` public interface | `ScenarioContextAccessPort.java` | Internal port | 🟢 **LOW** |
| `ScenarioContextAccessAdapter` public class | `ScenarioContextAccessAdapter.java` | Internal adapter | 🟢 **LOW** |
| `TestData` public class | `testData/TestData.java` | Unused | 🟢 **LOW** |

---

## 9️⃣ MIGRATION CHECKLIST

### Pre-Refactor
- [ ] Backup current code
- [ ] Verify all tests pass
- [ ] Document current client usage

### Phase 1: Nest Interfaces
- [ ] Update `Autwit.java` with nested interfaces
- [ ] Delete standalone interface files
- [ ] Update all `*Impl` classes to use nested types
- [ ] Update `AutwitImpl` to use nested types
- [ ] Update client code: `EventDrivenOrderLifecycleStepDefsFacedBased.java`
- [ ] Run tests to verify compilation

### Phase 2: Package-Private Impl Classes
- [ ] Change `EventExpectationImpl` visibility
- [ ] Change `ScenarioStepStatusImpl` visibility
- [ ] Change `ApiClientImpl` visibility
- [ ] Change `SoftAssertionsImpl` visibility
- [ ] Run tests to verify no external access

### Phase 3: Package-Private Internal Ports
- [ ] Change `ScenarioContextAccessPort` visibility
- [ ] Change `ScenarioContextAccessAdapter` visibility
- [ ] Run tests to verify no external access

### Phase 4: Remove TestData
- [ ] Delete `TestData.java`
- [ ] Verify no references remain

### Post-Refactor
- [ ] Run full test suite
- [ ] Verify client code compiles
- [ ] Verify no public types except `Autwit`
- [ ] Update documentation

---

## 🔟 RISK ASSESSMENT

### Overall Risk: ⚠️ **LOW**

**Reasons:**
1. Only 1 client file needs import update
2. No behavior changes
3. All changes are visibility-only
4. Nested interfaces maintain same API contract
5. Package-private changes have zero external impact

**Mitigation:**
- Update client code in same commit as Phase 1
- Run tests after each phase
- Verify compilation before proceeding

---

**END OF ANALYSIS**

