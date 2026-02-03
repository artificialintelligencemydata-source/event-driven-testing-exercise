package com.acuver.autwit.core.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * ApiCallStatistics - Value object for API call analytics.
 *
 * <h2>NOT AN ENTITY</h2>
 * This is a computed value object, not persisted to database.
 * Created by ApiContextServiceImpl from stored ApiContextEntities.
 *
 * @author AUTWIT Framework
 * @since 2.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiCallStatistics {

    /**
     * Total number of API calls.
     */
    private long totalCalls;

    /**
     * Number of regular API calls (isService=false).
     */
    private long apiCalls;

    /**
     * Number of service/flow calls (isService=true).
     */
    private long serviceCalls;

    /**
     * Breakdown by HTTP method.
     * Key: Method name (GET, POST, etc.)
     * Value: Count
     */
    private Map<String, Long> callsByHttpMethod;

    /**
     * Breakdown by data format.
     * Key: Format (XML, JSON)
     * Value: Count
     */
    private Map<String, Long> callsByDataRepresentation;

    /**
     * Most frequently called API.
     */
    private String mostUsedApi;

    /**
     * Most frequently called service.
     */
    private String mostUsedService;

    /**
     * Calculate service call percentage.
     */
    public double getServiceCallPercentage() {
        if (totalCalls == 0) return 0.0;
        return (serviceCalls * 100.0) / totalCalls;
    }

    /**
     * Calculate API call percentage.
     */
    public double getApiCallPercentage() {
        if (totalCalls == 0) return 0.0;
        return (apiCalls * 100.0) / totalCalls;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n========== API Call Statistics ==========\n");
        sb.append(String.format("Total Calls:        %d%n", totalCalls));
        sb.append(String.format("API Calls:          %d (%.1f%%)%n", apiCalls, getApiCallPercentage()));
        sb.append(String.format("Service Calls:      %d (%.1f%%)%n", serviceCalls, getServiceCallPercentage()));
        sb.append(String.format("Most Used API:      %s%n", mostUsedApi));
        sb.append(String.format("Most Used Service:  %s%n", mostUsedService));

        if (callsByHttpMethod != null && !callsByHttpMethod.isEmpty()) {
            sb.append("\nCalls by HTTP Method:\n");
            callsByHttpMethod.forEach((method, count) ->
                    sb.append(String.format("  %s: %d%n", method, count)));
        }

        if (callsByDataRepresentation != null && !callsByDataRepresentation.isEmpty()) {
            sb.append("\nCalls by Data Format:\n");
            callsByDataRepresentation.forEach((format, count) ->
                    sb.append(String.format("  %s: %d%n", format, count)));
        }

        sb.append("==========================================\n");
        return sb.toString();
    }
}