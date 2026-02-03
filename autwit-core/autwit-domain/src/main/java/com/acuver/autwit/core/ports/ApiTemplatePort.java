package com.acuver.autwit.core.ports;


import com.acuver.autwit.core.domain.ApiTemplateEntities;

import java.util.List;
import java.util.Optional;

/**
 * Port interface for API Template repository operations.
 *
 * <h2>PURPOSE</h2>
 * Defines contract for storing and retrieving reusable API templates
 * with parameterized request payloads.
 *
 * <h2>HEXAGONAL ARCHITECTURE</h2>
 * This is a PORT in the domain layer. Adapters implement this interface.
 *
 * @author AUTWIT Framework
 * @version 2.0.0
 */
public interface ApiTemplatePort {

    /**
     * Find API template by name.
     *
     * @param apiName The API name (e.g., "createOrder")
     * @return Optional containing template if found
     */
    Optional<ApiTemplateEntities> findByApiName(String apiName);

    /**
     * Find all API templates.
     *
     * @return List of all templates
     */
    List<ApiTemplateEntities> findAll();

    /**
     * Find templates by service flag.
     *
     * @param isService true for service calls, false for API calls
     * @return List of matching templates
     */
    List<ApiTemplateEntities> findByIsService(Boolean isService);

    /**
     * Find templates by HTTP method.
     *
     * @param httpMethod The HTTP method (GET, POST, etc.)
     * @return List of matching templates
     */
    List<ApiTemplateEntities> findByHttpMethod(String httpMethod);

    /**
     * Save or update API template.
     *
     * @param template The template to save
     * @return Saved template with generated ID
     */
    ApiTemplateEntities save(ApiTemplateEntities template);

    /**
     * Delete API template by name.
     *
     * @param apiName The API name to delete
     */
    void deleteByApiName(String apiName);

    /**
     * Check if template exists by name.
     *
     * @param apiName The API name to check
     * @return true if exists, false otherwise
     */
    boolean existsByApiName(String apiName);

    /**
     * Count total templates.
     *
     * @return Total count
     */
    long count();
}