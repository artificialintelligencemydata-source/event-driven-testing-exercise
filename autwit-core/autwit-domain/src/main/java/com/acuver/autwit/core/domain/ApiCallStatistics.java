package com.acuver.autwit.core.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Map;
/**
 * Domain entity representing API call statistics.
 *
 * <h2>LOCATION</h2>
 * Module: autwit-domain
 * Package: com.acuver.autwit.core.domain
 *
 * <h2>PURPOSE</h2>
 * Aggregated statistics about API calls made during test execution.
 * Part of the core domain model.
 *
 * <h2>RELATIONSHIP WITH OTHER DOMAINS</h2>
 * <ul>
 *   <li>{@link ApiContextEntities} - Individual API call records</li>
 *   <li>{@link ApiCallStatistics} - Aggregated statistics (this class)</li>
 * </ul>
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
     * Total number of API calls recorded
     */
    private long totalCalls;

    /**
     * Number of regular API calls (isService=false)
     */
    private long apiCalls;

    /**
     * Number of service/flow calls (isService=true)
     */
    private long serviceCalls;

    /**
     * Count of calls grouped by HTTP method
     * Example: {"POST": 45, "GET": 23, "DELETE": 5}
     */
    private Map<String, Long> callsByHttpMethod;

    /**
     * Count of calls grouped by data representation format
     * Example: {"XML": 65, "JSON": 8}
     */
    private Map<String, Long> callsByDataRepresentation;

    /**
     * Name of the most frequently called API
     */
    private String mostUsedApi;

    /**
     * Name of the most frequently called service
     */
    private String mostUsedService;

    /**
     * Calculate percentage of service calls
     *
     * @return Percentage (0-100)
     */
    public double getServiceCallPercentage() {
        if (totalCalls == 0) {
            return 0.0;
        }
        return (serviceCalls * 100.0) / totalCalls;
    }

    /**
     * Calculate percentage of API calls
     *
     * @return Percentage (0-100)
     */
    public double getApiCallPercentage() {
        if (totalCalls == 0) {
            return 0.0;
        }
        return (apiCalls * 100.0) / totalCalls;
    }

    /**
     * Pretty print statistics for reporting
     *
     * @return Formatted statistics string
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("========== API Call Statistics ==========\n");
        sb.append(String.format("Total Calls:        %d\n", totalCalls));
        sb.append(String.format("API Calls:          %d (%.1f%%)\n", apiCalls, getApiCallPercentage()));
        sb.append(String.format("Service Calls:      %d (%.1f%%)\n", serviceCalls, getServiceCallPercentage()));
        sb.append(String.format("Most Used API:      %s\n", mostUsedApi));
        sb.append(String.format("Most Used Service:  %s\n", mostUsedService));

        if (callsByHttpMethod != null && !callsByHttpMethod.isEmpty()) {
            sb.append("\nCalls by HTTP Method:\n");
            callsByHttpMethod.entrySet().stream()
                    .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                    .forEach(e -> sb.append(String.format("  %s: %d\n", e.getKey(), e.getValue())));
        }

        if (callsByDataRepresentation != null && !callsByDataRepresentation.isEmpty()) {
            sb.append("\nCalls by Data Format:\n");
            callsByDataRepresentation.entrySet().stream()
                    .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                    .forEach(e -> sb.append(String.format("  %s: %d\n", e.getKey(), e.getValue())));
        }

        sb.append("==========================================");
        return sb.toString();
    }
}
