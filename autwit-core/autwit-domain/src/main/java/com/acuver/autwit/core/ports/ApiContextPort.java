package com.acuver.autwit.core.ports;
import com.acuver.autwit.core.domain.ApiCallStatistics;
import com.acuver.autwit.core.domain.ApiContextEntities;
import com.acuver.autwit.core.domain.StorageException;

import java.util.List;
import java.util.Optional;

/**
 * Port interface for API context persistence operations.
 *
 * <h2>HEXAGONAL ARCHITECTURE</h2>
 * This port defines the contract for storing and retrieving API context.
 * Concrete implementations (adapters) provide actual persistence mechanisms:
 * <ul>
 *   <li>File-based storage adapter</li>
 *   <li>PostgreSQL adapter</li>
 *   <li>MongoDB adapter</li>
 *   <li>H2 in-memory adapter</li>
 * </ul>
 *
 * <h2>LOCATION</h2>
 * Module: autwit-domain
 * Package: com.acuver.autwit.core.ports
 *
 * <h2>RELATED PORTS</h2>
 * <ul>
 *   <li>{@link EventContextPort} - Event correlation data</li>
 *   <li>{@link ScenarioContextPort} - Scenario execution data</li>
 *   <li>{@link ApiContextPort} - API call metadata (this interface)</li>
 * </ul>
 *
 * <h2>USAGE</h2>
 * Inject this port interface in your service classes. The actual implementation
 * is selected based on configuration (storage mode).
 *
 * @author AUTWIT Framework
 * @since 2.0.0
 */
public interface ApiContextPort {

    /**
     * Save or update API context.
     *
     * @param apiContextEntities API context to persist
     * @return Saved API context with generated ID
     * @throws StorageException if save operation fails
     */
    ApiContextEntities save(ApiContextEntities apiContextEntities);

    /**
     * Find API context by API name.
     *
     * @param apiName API name to search for
     * @return Optional containing the API context if found
     */
    Optional<ApiContextEntities> findByApiName(String apiName);

    /**
     * Retrieve all API context records.
     *
     * @return List of all API contexts
     */
    List<ApiContextEntities> findAll();

    /**
     * Find API context by service name.
     *
     * @param serviceName Service name to filter by
     * @return List of API contexts for the given service
     */
    List<ApiContextEntities> findByServiceName(String serviceName);

    /**
     * Find all service/flow calls (where isService=true).
     *
     * @return List of service API contexts
     */
    List<ApiContextEntities> findServicesOnly();

    /**
     * Find API context by HTTP method.
     *
     * @param httpMethod HTTP method to filter by
     * @return List of API contexts using the specified HTTP method
     */
    List<ApiContextEntities> findByHttpMethod(ApiContextEntities.HttpMethod httpMethod);

    /**
     * Find API context by data representation format.
     *
     * @param dataRepresentation Format (e.g., "XML", "JSON")
     * @return List of API contexts with the specified format
     */
    List<ApiContextEntities> findByDataRepresentation(String dataRepresentation);

    /**
     * Delete API context by API name.
     *
     * @param apiName API name to delete
     */
    void deleteByApiName(String apiName);

    /**
     * Check if API context exists for the given name.
     *
     * @param apiName API name to check
     * @return true if exists, false otherwise
     */
    boolean existsByApiName(String apiName);

    /**
     * Delete all API context records.
     * Use with caution - typically for testing purposes only.
     */
    void deleteAll();

    /**
     * Count total number of API context records.
     *
     * @return Total count
     */
    long count();

    // ==================== Statistics and Analytics ====================

    /**
     * Get aggregated statistics about API calls.
     *
     * <h3>IMPLEMENTATION NOTE</h3>
     * This method is typically implemented by the service layer
     * (e.g., ApiContextServiceImpl), NOT by storage adapters.
     *
     * Storage adapters can provide a default implementation that
     * throws UnsupportedOperationException.
     *
     * @return API call statistics
     * @throws UnsupportedOperationException if called on a storage adapter
     */
    default ApiCallStatistics getStatistics() {
        throw new UnsupportedOperationException(
                "Statistics are only available from the service layer, not storage adapters. " +
                        "Use ApiContextServiceImpl to get statistics."
        );
    }

    /**
     * Get call count for a specific API or service.
     *
     * <h3>IMPLEMENTATION NOTE</h3>
     * This is a business logic method, typically implemented by service layer.
     *
     * @param name API name or service name
     * @param isService true for service count, false for API count
     * @return Number of calls
     * @throws UnsupportedOperationException if called on a storage adapter
     */
    default long getCallCount(String name, boolean isService) {
        throw new UnsupportedOperationException(
                "Call count is only available from the service layer, not storage adapters. " +
                        "Use ApiContextServiceImpl to get call counts."
        );
    }
}