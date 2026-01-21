package com.acuver.autwit.internal.resume;

import com.acuver.autwit.core.domain.EventContextEntities;
import com.acuver.autwit.core.ports.EventContextPort;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * ResumeScheduler - Detects resumeReady scenarios and triggers re-execution.
 *
 * <h2>RESPONSIBILITY</h2>
 * <ul>
 *   <li>Poll for scenarios marked resumeReady in database</li>
 *   <li>Build dynamic TestNG configuration for resume</li>
 *   <li>Trigger programmatic test execution via ResumeExecutor</li>
 * </ul>
 *
 * <h2>ARCHITECTURAL CONSTRAINT</h2>
 * This component does NOT make resume decisions. That is the sole responsibility
 * of ResumeEngine. This component only orchestrates re-execution of scenarios
 * that have ALREADY been approved for resume.
 *
 * <h2>CONFIGURATION</h2>
 * <pre>
 * autwit:
 *   resume:
 *     enabled: true              # Enable/disable resume scheduler
 *     poll-interval: 30000       # Poll every 30 seconds
 *     batch-size: 10             # Process 10 scenarios per batch
 *     max-retries: 3             # Maximum resume attempts per scenario
 * </pre>
 *
 * @author AUTWIT Framework
 * @since 1.0.0
 */
@Component
@EnableScheduling
@ConditionalOnProperty(name = "autwit.resume.enabled", havingValue = "true", matchIfMissing = false)
public class ResumeScheduler {

    private static final Logger log = LogManager.getLogger(ResumeScheduler.class);

    private final EventContextPort eventContextPort;
    private final ResumeExecutor resumeExecutor;

    @Value("${autwit.resume.batch-size:10}")
    private int batchSize;

    @Value("${autwit.resume.max-retries:3}")
    private int maxRetries;

    /**
     * Constructor with dependency injection.
     *
     * @param eventContextPort Port for querying event/scenario state
     * @param resumeExecutor Executor for running resumed scenarios
     */
    public ResumeScheduler(EventContextPort eventContextPort, ResumeExecutor resumeExecutor) {
        this.eventContextPort = eventContextPort;
        this.resumeExecutor = resumeExecutor;
        log.info("✅ ResumeScheduler initialized (batch-size={}, max-retries={})", batchSize, maxRetries);
    }

    /**
     * Scheduled task to check for resumable scenarios.
     *
     * <p>Runs at fixed intervals (default: 30 seconds). Queries database for
     * scenarios with resumeReady=true, then triggers re-execution.</p>
     */
    @Scheduled(fixedDelayString = "${autwit.resume.poll-interval:30000}")
    public void checkForResumableScenarios() {
        log.trace("🔍 Checking for resumable scenarios...");

        try {
            // 1. Find all paused scenarios that are ready to resume
            List<EventContextEntities> resumeReady = findResumeReadyScenarios();

            if (resumeReady.isEmpty()) {
                log.trace("No scenarios ready for resume");
                return;
            }

            log.info("🔄 Found {} scenario(s) ready for resume", resumeReady.size());

            // 2. Process in batches to avoid overwhelming system
            List<List<EventContextEntities>> batches = partition(resumeReady, batchSize);

            for (int i = 0; i < batches.size(); i++) {
                List<EventContextEntities> batch = batches.get(i);
                log.info("📦 Processing batch {}/{} ({} scenarios)",
                        i + 1, batches.size(), batch.size());
                processBatch(batch);
            }

        } catch (Exception e) {
            log.error("❌ Error checking for resumable scenarios: {}", e.getMessage(), e);
        }
    }

    /**
     * Manual trigger for resume check.
     *
     * <p>Can be called programmatically to force immediate check,
     * for example after a known event arrival.</p>
     */
    public void triggerImmediateCheck() {
        log.info("🔔 Manual resume check triggered");
        checkForResumableScenarios();
    }

    /**
     * Find all scenarios that are ready for resume.
     */
    private List<EventContextEntities> findResumeReadyScenarios() {
        try {
            List<EventContextEntities> paused = eventContextPort.findPaused();

            return paused.stream()
                    .filter(EventContextEntities::isResumeReady)
                    .filter(ctx -> ctx.getRetryCount() < maxRetries)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Failed to query resumable scenarios: {}", e.getMessage());
            return List.of();
        }
    }

    /**
     * Process a batch of scenarios for resume.
     */
    private void processBatch(List<EventContextEntities> batch) {
        // Extract unique scenario identifiers
        List<String> scenarioKeys = batch.stream()
                .map(this::extractScenarioKey)
                .distinct()
                .collect(Collectors.toList());

        log.info("🚀 Resuming batch: {}", scenarioKeys);

        try {
            // Execute via ResumeExecutor
            resumeExecutor.execute(scenarioKeys);

            // Mark scenarios as processed
            for (EventContextEntities ctx : batch) {
                markAsProcessed(ctx);
            }

            log.info("✅ Batch resume completed successfully");

        } catch (Exception e) {
            log.error("❌ Batch execution failed: {}", e.getMessage(), e);

            // Increment retry count for failed scenarios
            for (EventContextEntities ctx : batch) {
                incrementRetryCount(ctx);
            }
        }
    }

    /**
     * Extract scenario key from EventContextEntities.
     *
     * <p>The canonical key format is: orderId::eventType
     * For resume, we need the scenario name which may be stored separately.</p>
     */
    private String extractScenarioKey(EventContextEntities ctx) {
        // Current canonical key format: orderId::eventType
        // TODO: After CanonicalKeyGenerator enhancement, this will be: scenarioName::orderId::eventType
        String canonicalKey = ctx.getCanonicalKey();

        if (canonicalKey == null) {
            return "unknown";
        }

        // For now, return the canonical key as the scenario identifier
        // The executor will need to map this to actual scenario for re-execution
        return canonicalKey;
    }

    /**
     * Mark scenario as processed after successful resume trigger.
     */
    private void markAsProcessed(EventContextEntities ctx) {
        try {
            ctx.setResumeReady(false);
            ctx.setStatus("RESUMED");
            ctx.setLastRetryAt(System.currentTimeMillis());
            eventContextPort.save(ctx);

            log.debug("✓ Marked as processed: {}", ctx.getCanonicalKey());

        } catch (Exception e) {
            log.warn("Failed to mark scenario as processed: {}", e.getMessage());
        }
    }

    /**
     * Increment retry count for failed resume attempt.
     */
    private void incrementRetryCount(EventContextEntities ctx) {
        try {
            ctx.setRetryCount(ctx.getRetryCount() + 1);
            ctx.setLastRetryAt(System.currentTimeMillis());
            eventContextPort.save(ctx);

            log.debug("Incremented retry count for {}: {}/{}",
                    ctx.getCanonicalKey(), ctx.getRetryCount(), maxRetries);

        } catch (Exception e) {
            log.warn("Failed to update retry count: {}", e.getMessage());
        }
    }

    /**
     * Partition a list into smaller batches.
     */
    private <T> List<List<T>> partition(List<T> list, int size) {
        if (list == null || list.isEmpty()) {
            return List.of();
        }

        List<List<T>> partitions = new ArrayList<>();
        for (int i = 0; i < list.size(); i += size) {
            partitions.add(new ArrayList<>(
                    list.subList(i, Math.min(i + size, list.size()))
            ));
        }
        return partitions;
    }
}