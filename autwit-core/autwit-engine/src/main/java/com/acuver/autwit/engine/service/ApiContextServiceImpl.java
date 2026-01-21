package com.acuver.autwit.engine.service;

import com.acuver.autwit.core.domain.ApiCallStatistics;
import com.acuver.autwit.core.domain.ApiContextEntities;
import com.acuver.autwit.core.domain.StorageException;
import com.acuver.autwit.core.ports.ApiContextPort;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * ApiContextService - Business logic layer implementing ApiContextPort.
 *
 * <h2>ARCHITECTURE</h2>
 * This service:
 * - Lives in autwit-engine (application/service layer)
 * - IMPLEMENTS ApiContextPort (from autwit-domain)
 * - Delegates to storage adapters for persistence
 * - Adds business logic, validation, and transaction management
 *
 * <h2>PATTERN</h2>
 * Service Layer implements Port → uses Adapter for storage
 *
 * @author AUTWIT Framework
 * @since 2.0.0
 */
@Service
@Qualifier("apiContextService")
public class ApiContextServiceImpl implements ApiContextPort {


    private static final Logger logger = LogManager.getLogger(ApiContextServiceImpl.class);

    // Inject the actual storage adapter (PostgreSQL, MongoDB, or H2)
    private final ApiContextPort storageAdapter;

    @Autowired
    public ApiContextServiceImpl(@Qualifier("storageAdapter") ApiContextPort storageAdapter) {
        this.storageAdapter = storageAdapter;
        logger.info("ApiContextService initialized with storage adapter: {}",
                storageAdapter.getClass().getSimpleName());
    }

    @Override
    public ApiContextEntities save(ApiContextEntities apiContext) {
        logger.debug("Saving API context: {}", apiContext.getApiName());

        // ✅ Business validation
        validateApiContext(apiContext);

        // ✅ Business logic: Auto-set timestamps
        LocalDateTime now = LocalDateTime.now();
        if (apiContext.getCreatedAt() == null) {
            apiContext.setCreatedAt(now);
        }
        apiContext.setUpdatedAt(now);

        // ✅ Business logic: Log update vs insert
        boolean exists = storageAdapter.existsByApiName(apiContext.getApiName());
        if (exists) {
            logger.debug("Updating existing API context: {}", apiContext.getApiName());
        } else {
            logger.debug("Creating new API context: {}", apiContext.getApiName());
        }

        try {
            // Delegate to storage adapter
            ApiContextEntities saved = storageAdapter.save(apiContext);
            logger.info("Successfully saved API context: {} (isService={})",
                    saved.getApiName(), saved.getIsService());
            return saved;
        } catch (Exception e) {
            logger.error("Failed to save API context: {}", apiContext.getApiName(), e);
            throw new StorageException("Failed to save API context", e);
        }
    }

    @Override
    public Optional<ApiContextEntities> findByApiName(String apiName) {
        logger.debug("Finding API context by name: {}", apiName);

        // ✅ Business validation
        if (apiName == null || apiName.trim().isEmpty()) {
            throw new IllegalArgumentException("API name cannot be null or empty");
        }

        return storageAdapter.findByApiName(apiName);
    }

    @Override
    public List<ApiContextEntities> findAll() {
        logger.debug("Finding all API contexts");
        return storageAdapter.findAll();
    }

    @Override
    public List<ApiContextEntities> findByServiceName(String serviceName) {
        logger.debug("Finding API contexts by service name: {}", serviceName);

        // ✅ Business validation
        if (serviceName == null || serviceName.trim().isEmpty()) {
            throw new IllegalArgumentException("Service name cannot be null or empty");
        }

        return storageAdapter.findByServiceName(serviceName);
    }

    @Override
    public List<ApiContextEntities> findServicesOnly() {
        logger.debug("Finding service-only API contexts");
        return storageAdapter.findServicesOnly();
    }

    @Override
    public List<ApiContextEntities> findByHttpMethod(ApiContextEntities.HttpMethod httpMethod) {
        logger.debug("Finding API contexts by HTTP method: {}", httpMethod);

        // ✅ Business validation
        if (httpMethod == null) {
            throw new IllegalArgumentException("HTTP method cannot be null");
        }

        return storageAdapter.findByHttpMethod(httpMethod);
    }

    @Override
    public List<ApiContextEntities> findByDataRepresentation(String dataRepresentation) {
        logger.debug("Finding API contexts by data representation: {}", dataRepresentation);
        return storageAdapter.findByDataRepresentation(dataRepresentation);
    }

    @Override
    public void deleteByApiName(String apiName) {
        logger.info("Deleting API context: {}", apiName);

        // ✅ Business logic: Verify exists before deleting
        if (!storageAdapter.existsByApiName(apiName)) {
            logger.warn("Attempted to delete non-existent API context: {}", apiName);
            throw new IllegalArgumentException("API context not found: " + apiName);
        }

        storageAdapter.deleteByApiName(apiName);
    }

    @Override
    public boolean existsByApiName(String apiName) {
        return storageAdapter.existsByApiName(apiName);
    }

    @Override
    public void deleteAll() {
        logger.warn("Deleting all API contexts");
        storageAdapter.deleteAll();
    }

    @Override
    public long count() {
        return storageAdapter.count();
    }

    // ==================== Additional Business Methods ====================

    /**
     * Get statistics about API calls.
     * This is BUSINESS LOGIC, not in the port interface.
     */
    public ApiCallStatistics getStatistics() {
        logger.debug("Calculating API call statistics");

        List<ApiContextEntities> allContexts = storageAdapter.findAll();

        long totalCalls = allContexts.size();
        long serviceCalls = allContexts.stream()
                .filter(ApiContextEntities::getIsService)
                .count();
        long apiCalls = totalCalls - serviceCalls;

        Map<String, Long> callsByHttpMethod = allContexts.stream()
                .collect(Collectors.groupingBy(
                        ctx -> ctx.getHttpMethod().name(),
                        Collectors.counting()
                ));

        Map<String, Long> callsByDataRepresentation = allContexts.stream()
                .collect(Collectors.groupingBy(
                        ApiContextEntities::getDataRepresentation,
                        Collectors.counting()
                ));

        Map<String, Long> apiFrequency = allContexts.stream()
                .filter(ctx -> !ctx.getIsService())
                .collect(Collectors.groupingBy(
                        ApiContextEntities::getApiName,
                        Collectors.counting()
                ));
        String mostUsedApi = apiFrequency.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("N/A");

        Map<String, Long> serviceFrequency = allContexts.stream()
                .filter(ApiContextEntities::getIsService)
                .collect(Collectors.groupingBy(
                        ApiContextEntities::getServiceName,
                        Collectors.counting()
                ));
        String mostUsedService = serviceFrequency.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("N/A");

        return ApiCallStatistics.builder()
                .totalCalls(totalCalls)
                .apiCalls(apiCalls)
                .serviceCalls(serviceCalls)
                .callsByHttpMethod(callsByHttpMethod)
                .callsByDataRepresentation(callsByDataRepresentation)
                .mostUsedApi(mostUsedApi)
                .mostUsedService(mostUsedService)
                .build();
    }
    @Override
    public long getCallCount(String name, boolean isService) {
        logger.debug("Getting call count for {} (isService={})", name, isService);

        if (name == null || name.isBlank()) {
            return 0;
        }

        List<ApiContextEntities> all = storageAdapter.findAll();

        if (isService) {
            return all.stream()
                    .filter(ApiContextEntities::getIsService)
                    .filter(ctx -> name.equals(ctx.getServiceName()))
                    .count();
        } else {
            return all.stream()
                    .filter(ctx -> !ctx.getIsService())
                    .filter(ctx -> name.equals(ctx.getApiName()))
                    .count();
        }
    }

    // ==================== Additional Business Methods ====================

    /**
     * Clear all data for a specific API
     */
    public void clearApiHistory(String apiName) {
        logger.info("Clearing history for API: {}", apiName);
        deleteByApiName(apiName);
    }

    /**
     * Clear all service call history
     */
    public void clearAllServiceHistory() {
        logger.info("Clearing all service call history");
        List<ApiContextEntities> services = storageAdapter.findServicesOnly();
        services.forEach(s -> storageAdapter.deleteByApiName(s.getApiName()));
    }
    // ==================== Private Validation ====================

    private void validateApiContext(ApiContextEntities apiContext) {
        if (apiContext == null) {
            throw new IllegalArgumentException("API context cannot be null");
        }

        if (apiContext.getApiName() == null || apiContext.getApiName().trim().isEmpty()) {
            throw new IllegalArgumentException("API name is required");
        }

        if (apiContext.getHttpMethod() == null) {
            throw new IllegalArgumentException("HTTP method is required");
        }

        if (apiContext.getDataRepresentation() == null || apiContext.getDataRepresentation().trim().isEmpty()) {
            throw new IllegalArgumentException("Data representation is required");
        }

        if (apiContext.getIsService() == null) {
            throw new IllegalArgumentException("isService flag is required");
        }

        if (apiContext.getIsService() &&
                (apiContext.getServiceName() == null || apiContext.getServiceName().trim().isEmpty())) {
            throw new IllegalArgumentException("Service name is required when isService=true");
        }

        if (apiContext.getApiTemplate() == null || apiContext.getApiTemplate().trim().isEmpty()) {
            apiContext.setApiTemplate("N/A");
        }
    }
}
