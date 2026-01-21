package com.acuver.autwit.core.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Domain entity representing API context for test orchestration.
 *
 * <h2>PURPOSE</h2>
 * Stores comprehensive context about API calls made during test execution,
 * including request/response payloads, HTTP methods, and service information.
 *
 * <h2>ARCHITECTURE</h2>
 * Part of autwit-domain (core domain layer).
 * Persistence is handled by adapters implementing {@link com.acuver.autwit.core.ports.ApiContextPort}
 *
 * <h2>RELATIONSHIP WITH OTHER CONTEXTS</h2>
 * <ul>
 *   <li>{@link EventContextEntities} - Stores event correlation data</li>
 *   <li>{@link ScenarioStateContextEntities} - Stores scenario execution state</li>
 *   <li>{@link ApiContextEntities} - Stores API call metadata (this class)</li>
 * </ul>
 *
 * @author AUTWIT Framework
 * @since 2.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiContextEntities {

    /**
     * Unique identifier (managed by persistence layer)
     */
    private Long id;

    /**
     * API name (e.g., "createOrder", "getOrderList")
     * Must be unique within the system.
     */
    private String apiName;

    /**
     * HTTP method used for the API call
     */
    private HttpMethod httpMethod;

    /**
     * API template/endpoint pattern
     * Example: "/api/order/{orderId}"
     */
    private String apiTemplate;

    /**
     * Data representation format (XML, JSON, etc.)
     */
    private String dataRepresentation;

    /**
     * Request payload sent to the API
     */
    private String requestPayload;

    /**
     * Response payload received from the API
     */
    private String responsePayload;

    /**
     * Flag indicating if this is a service/flow call
     */
    private Boolean isService;

    /**
     * Service name (populated when isService=true)
     */
    private String serviceName;

    /**
     * Timestamp when the record was created
     */
    private LocalDateTime createdAt;

    /**
     * Timestamp when the record was last updated
     */
    private LocalDateTime updatedAt;

    /**
     * Additional metadata (extensible map for future use)
     */
    private Map<String, Object> additionalMetadata;

    /**
     * HTTP methods supported by AUTWIT
     */
    public enum HttpMethod {
        GET,
        POST,
        PUT,
        PATCH,
        DELETE,
        HEAD,
        OPTIONS
    }
}