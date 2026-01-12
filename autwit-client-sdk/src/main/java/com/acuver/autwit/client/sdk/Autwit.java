package com.acuver.autwit.client.sdk;

import io.restassured.response.Response;
import org.testng.asserts.SoftAssert;

import java.util.Map;

/**
 * Client-facing facade.
 * Expresses intent, never mechanics.
 */
public interface Autwit {
    
    /**
     * Event expectation for verification.
     */
    interface EventExpectation {
        /**
         * Assert that the expected event is satisfied.
         * If event not found, scenario pauses automatically.
         * 
         * @throws org.testng.SkipException if event not available (scenario pauses)
         * @throws RuntimeException if event verification fails
         */
        void assertSatisfied();
        
        /**
         * Validate event payload using a read-only function.
         * Payload is provided as a Map for safe, read-only access.
         * 
         * @param validator Function that receives payload as Map and returns boolean
         * @return this for method chaining
         * @throws org.testng.SkipException if event not available
         * @throws RuntimeException if validation fails
         */
        EventExpectation assertPayload(java.util.function.Function<Map<String, Object>, Boolean> validator);
    }
    
    /**
     * Step lifecycle operations.
     */
    interface ScenarioStepStatus {
        /**
         * Mark current step as successfully completed.
         * Step name is automatically extracted from context.
         */
        void markStepSuccess();
        
        /**
         * Mark current step as failed.
         * 
         * @param reason Failure reason
         */
        void markStepFailed(String reason);
        /**
         * Mark current step as skipped.
         *
         * This indicates that the step cannot proceed yet
         * (for example, an expected async event has not arrived).
         *
         * The scenario will be paused and eligible for resume.
         *
         * @param reason Reason for skipping the step
         */
        void markStepSkipped(String reason);
        /**
         * Check if this step already succeeded earlier.
         * If yes, restore step data and signal caller to skip execution.
         *
         * @return true if step should be skipped
         */
        boolean skipIfAlreadySuccessful();
        /**
         * Retrieve persisted data for the current step.
         *
         * @return Map of step data, or empty map if none exists
         */
        Map<String, String> getStepData();
    }

    /**
     * Scenario-scoped context access.
     * Provides data storage, API clients, and assertions.
     */
    interface ContextAccessor {
        /**
         * Set the current step name (for step tracking).
         * 
         * @param stepName Current step name
         */
        void setCurrentStep(String stepName);
        
        /**
         * Store a value in scenario context.
         * 
         * @param key Context key
         * @param value Value to store
         * @param <T> Value type
         */
        <T> void set(String key, T value);
        
        /**
         * Retrieve a value from scenario context.
         * 
         * @param key Context key
         * @param <T> Value type
         * @return Stored value, or null if not found
         */
        <T> T get(String key);
        
        /**
         * Access API client for making HTTP calls.
         * 
         * @return ApiClient for REST API operations
         */
        ApiClient api();
        
        /**
         * Access soft assertions for validation.
         * 
         * @return SoftAssertions for non-fatal validations
         */
        SoftAssertions assertions();
        
        /**
         * Set order ID in context and MDC (for logging correlation).
         * Convenience method that sets both context and MDC.
         * 
         * @param orderId Order ID to set
         */
        void setOrderId(String orderId);
        
        /**
         * API client for making HTTP calls.
         * Wraps internal ApiCalls.
         */
        interface ApiClient {
            /**
             * Create an order via REST API.
             * 
             * @param payload Order payload
             * @return Response object
             */
            Response createOrder(Map<String, Object> payload);
        }
        
        /**
         * Soft assertions for non-fatal validations.
         * Wraps internal SoftAssertUtils.
         */
        interface SoftAssertions {
            /**
             * Get the soft assert instance.
             * 
             * @return SoftAssert instance
             */
            SoftAssert getSoftAssert();
            
            /**
             * Assert all collected soft assertions.
             * Throws if any assertion failed.
             */
            void assertAll();
        }
    }
    
    /**
     * Declare an event expectation.
     * 
     * @param orderId Business key (e.g., order ID, correlation ID)
     * @param eventType Event type to expect (e.g., "ORDER_CREATED")
     * @return EventExpectation for verification
     */
    EventExpectation expectEvent(String orderId, String eventType);
    
    /**
     * Access step lifecycle operations.
     * 
     * @return ScenarioStepStatus for marking step success/failure
     */
    ScenarioStepStatus step();
    
    /**
     * Access scenario context (data storage, API clients, assertions).
     * 
     * @return ContextAccessor for scenario-scoped operations
     */
    ContextAccessor context();
}