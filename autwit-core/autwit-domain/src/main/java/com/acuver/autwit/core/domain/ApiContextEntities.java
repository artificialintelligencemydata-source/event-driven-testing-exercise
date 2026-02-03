package com.acuver.autwit.core.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * ApiContextEntities - Domain model for step-isolated API context.
 *
 * <h2>DOMAIN MODEL (Clean Architecture)</h2>
 * This is a pure domain object with no database/framework annotations.
 * Adapters convert between this and their specific entity types (JPA, MongoDB, etc.).
 *
 * <h2>IDENTITY MODEL</h2>
 * Composite natural key: (stepKey, apiName, callIndex)
 * - stepKey: Unique step execution identifier
 * - apiName: API name
 * - callIndex: Call index within step (0, 1, 2...)
 *
 * <h2>STEP-LEVEL ISOLATION (v2.0)</h2>
 * Primary tracking level is now the STEP, not the scenario.
 * This prevents cross-step data contamination and supports step reruns.
 *
 * @author AUTWIT Framework
 * @since 2.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiContextEntities {
// ==========================================================================
    // IDENTITY FIELDS
    // ==========================================================================

    /**
     * Auto-generated ID (surrogate key).
     *
     * <p>Not part of business identity - used only for database optimization.
     */
    private Long id;

    // ==========================================================================
    // SCENARIO ISOLATION (Legacy - Retained)
    // ==========================================================================

    /**
     * Scenario unique identifier.
     *
     * <h3>FORMAT</h3>
     * {scenarioName}_{exampleId}_T{threadId}_{uuid8}_{timestamp}
     *
     * <h3>EXAMPLE</h3>
     * CreateOrder_example_line_42_T5_a3f4b2c1_1737547890123
     *
     * <h3>GENERATION</h3>
     * Generated in Hooks.setupScenarioContext() and stored in RuntimeContextPort.
     *
     * <h3>LENGTH</h3>
     * ~70-100 characters (variable based on scenario name length)
     *
     * <h3>REQUIRED</h3>
     * MUST be set before save(). Null/blank will throw exception.
     */
    private String scenarioKey;

    /**
     * Test case ID (for grouping).
     *
     * <h3>EXAMPLE</h3>
     * "OrderCreation" (feature file name without .feature extension)
     *
     * <h3>USAGE</h3>
     * Used for test reporting and analytics.
     */
    private String testCaseId;

    /**
     * Example ID (for data-driven tests).
     *
     * <h3>EXAMPLE</h3>
     * "example_line_42" (for scenario outline row 42)
     * "default" (for non-parameterized scenarios)
     *
     * <h3>USAGE</h3>
     * Distinguishes different data-driven test executions.
     */
    private String exampleId;

    // ==========================================================================
    // ✅ NEW: STEP-LEVEL TRACKING (Primary)
    // ==========================================================================

    /**
     * Step execution unique identifier (part of composite natural key).
     *
     * <h3>FORMAT</h3>
     * {scenarioKey}_s{stepHash4}_{execIdx}
     *
     * <h3>EXAMPLE</h3>
     * CreateOrder_example_line_42_T5_a3f4b2c1_1737547890123_sAB12_0
     * └──────────────────────────────────────────────────┘ └──┘ └┘
     *                  scenarioKey                        hash  idx
     *
     * <h3>COMPONENTS</h3>
     * - scenarioKey: Parent scenario identifier (~70-100 chars)
     * - stepHash4: 4-char MD5 hash of step name (e.g., "AB12")
     * - execIdx: Step execution index for reruns (0, 1, 2...)
     *
     * <h3>LENGTH</h3>
     * ~80-110 characters
     *
     * <h3>GENERATION</h3>
     * Generated in Hooks.setupStepContext(@BeforeMethod) and stored in RuntimeContextPort.
     *
     * <h3>REQUIRED</h3>
     * MUST be set before save(). Null/blank will throw exception.
     */
    private String stepKey;

    /**
     * Human-readable step name.
     *
     * <h3>EXAMPLE</h3>
     * "I create an order"
     * "I check payment status"
     *
     * <h3>GENERATION</h3>
     * Extracted from method name in Hooks.setupStepContext(@BeforeMethod).
     * Method name "iCreateAnOrder" → "I create an order"
     *
     * <h3>LENGTH</h3>
     * Typically 20-50 characters
     *
     * <h3>USAGE</h3>
     * - Human-readable logging
     * - Querying previous step responses: getResponseFromStep("I create an order")
     * - Test reporting
     *
     * <h3>REQUIRED</h3>
     * MUST be set before save(). Null/blank will throw exception.
     */
    private String stepName;

    /**
     * Step execution index (for reruns).
     *
     * <h3>PURPOSE</h3>
     * Tracks how many times a step has been executed in the same scenario.
     *
     * <h3>VALUES</h3>
     * - 0: First execution
     * - 1: First rerun
     * - 2: Second rerun
     * - etc.
     *
     * <h3>USE CASES</h3>
     * - Scenario resume after event wait
     * - Step retry after failure
     * - Manual step re-execution
     *
     * <h3>DEFAULT</h3>
     * 0 (first execution)
     *
     * <h3>EXAMPLE</h3>
     * Step "I check payment status" runs 3 times:
     * - (stepKey_0, "checkPayment", 0) → step_execution_index = 0
     * - (stepKey_1, "checkPayment", 0) → step_execution_index = 1
     * - (stepKey_2, "checkPayment", 0) → step_execution_index = 2
     */
    @Builder.Default
    private Integer stepExecutionIndex = 0;

    // ==========================================================================
    // API IDENTIFICATION
    // ==========================================================================

    /**
     * API name (part of composite natural key).
     *
     * <h3>EXAMPLE</h3>
     * "createOrder", "getOrderDetails", "scheduleOrder"
     *
     * <h3>REQUIRED</h3>
     * MUST be set before save(). Null/blank will throw exception.
     */
    private String apiName;

    /**
     * Call index within step (part of composite natural key).
     *
     * <h3>PURPOSE</h3>
     * Supports multiple calls to same API within one step.
     *
     * <h3>SCOPE</h3>
     * ✅ NEW: Scoped to STEP (not scenario)
     * This prevents cross-step data contamination.
     *
     * <h3>DEFAULT</h3>
     * 0 (first call)
     *
     * <h3>EXAMPLE</h3>
     * Step calls "getOrderDetails" 3 times:
     * - (stepKey, "getOrderDetails", 0)
     * - (stepKey, "getOrderDetails", 1)
     * - (stepKey, "getOrderDetails", 2)
     *
     * <h3>TRACKING</h3>
     * Managed by BaseActionsNew.callIndexTracker (ThreadLocal)
     */
    @Builder.Default
    private Integer callIndex = 0;

    // ==========================================================================
    // API METADATA
    // ==========================================================================

    /**
     * HTTP method.
     *
     * <h3>REQUIRED</h3>
     * MUST be set before save(). Null will throw exception.
     */
    private HttpMethod httpMethod;

    /**
     * API template/endpoint pattern.
     *
     * <h3>EXAMPLE</h3>
     * "/api/order", "/api/order/{orderId}"
     *
     * <h3>DEFAULT</h3>
     * "N/A" if not provided
     */
    private String apiTemplate;

    /**
     * Data format (XML, JSON, etc.).
     *
     * <h3>REQUIRED</h3>
     * MUST be set before save(). Null/blank will throw exception.
     */
    private String dataRepresentation;

    // ==========================================================================
    // PAYLOADS
    // ==========================================================================

    /**
     * Request payload (full XML/JSON).
     *
     * <h3>STORAGE</h3>
     * Stored as TEXT in database (unlimited length)
     */
    private String requestPayload;

    /**
     * Response payload (full XML/JSON).
     *
     * <h3>STORAGE</h3>
     * Stored as TEXT in database (unlimited length)
     *
     * <h3>USAGE</h3>
     * Retrieved by getLastResponseFromCurrentStep() and similar methods
     */
    private String responsePayload;

    // ==========================================================================
    // SERVICE IDENTIFICATION
    // ==========================================================================

    /**
     * Whether this is a service/flow call.
     *
     * <h3>USAGE</h3>
     * Distinguishes between:
     * - Direct API calls (isService = false)
     * - Service/flow invocations (isService = true)
     *
     * <h3>REQUIRED</h3>
     * MUST be set before save(). Null will throw exception.
     */
    private Boolean isService;

    /**
     * Service name (if isService=true).
     *
     * <h3>EXAMPLE</h3>
     * "OrderCreationService", "PaymentProcessingFlow"
     *
     * <h3>VALIDATION</h3>
     * Required when isService=true, null otherwise.
     */
    private String serviceName;

    // ==========================================================================
    // ✅ NEW: BUSINESS ENTITY CORRELATION
    // ==========================================================================

    /**
     * Order number (business entity correlation).
     *
     * <h3>PURPOSE</h3>
     * Enables cross-scenario order tracking.
     *
     * <h3>EXAMPLE</h3>
     * "ORD-12345", "20250128-001"
     *
     * <h3>EXTRACTION</h3>
     * Automatically extracted from response XML in BaseActionsNew.storeToDatabase()
     * Looks for patterns: &lt;OrderNo&gt;, &lt;order_no&gt;, OrderNo="..."
     *
     * <h3>USAGE</h3>
     * - Find original createOrder call: findByOrderNo("ORD-12345")
     * - Track order lifecycle: findAllByOrderNo("ORD-12345")
     * - Cross-scenario correlation
     *
     * <h3>OPTIONAL</h3>
     * Null if not found in response or not applicable
     */
    private String orderNo;

    /**
     * Order header key (business entity correlation).
     *
     * <h3>PURPOSE</h3>
     * Sterling OMS primary key for order tracking.
     *
     * <h3>EXAMPLE</h3>
     * "202501280001", "HDR-ABC123"
     *
     * <h3>EXTRACTION</h3>
     * Automatically extracted from response XML in BaseActionsNew.storeToDatabase()
     * Looks for patterns: &lt;OrderHeaderKey&gt;, &lt;order_header_key&gt;, OrderHeaderKey="..."
     *
     * <h3>USAGE</h3>
     * - Link order creation to subsequent operations
     * - Track order modifications
     * - Cross-scenario correlation
     *
     * <h3>OPTIONAL</h3>
     * Null if not found in response or not applicable
     */
    private String orderHeaderKey;

    // ==========================================================================
    // TIMESTAMPS
    // ==========================================================================

    /**
     * When the call was made.
     *
     * <h3>GENERATION</h3>
     * Auto-set by ApiContextServiceImpl if null
     */
    private LocalDateTime createdAt;

    /**
     * Last update timestamp.
     *
     * <h3>GENERATION</h3>
     * Auto-set by ApiContextServiceImpl on every save
     */
    private LocalDateTime updatedAt;

    // ==========================================================================
    // ENUMS
    // ==========================================================================

    /**
     * HTTP method enumeration.
     */
    public enum HttpMethod {
        GET, POST, PUT, DELETE, PATCH, HEAD, OPTIONS
    }
}