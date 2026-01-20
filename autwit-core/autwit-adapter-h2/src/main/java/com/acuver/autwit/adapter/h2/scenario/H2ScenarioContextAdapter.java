package com.acuver.autwit.adapter.h2.scenario;

import com.acuver.autwit.core.domain.ScenarioStateContext;
import com.acuver.autwit.core.ports.ScenarioContextPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "autwit.database", havingValue = "h2")
@Slf4j
public class H2ScenarioContextAdapter implements ScenarioContextPort {

    private final H2ScenarioContextRepository repo;

    @Override
    public Optional<ScenarioStateContext> findByScenarioName(String name) {
        return repo.findByScenarioName(name).map(this::toDomain);
    }

    @Override
    public ScenarioStateContext save(ScenarioStateContext ctx) {
        log.debug("Saving ScenarioStateContext: scenarioName={}, exampleId={}, _id={}",
                ctx.getScenarioName(), ctx.getExampleId(), ctx.get_id());

        H2ScenarioContextEntity entityToSave;

        // Check if this is an UPDATE (existing record) or CREATE (new record)
        if (ctx.get_id() != null) {
            // Has an ID - check if it exists in DB
            Optional<H2ScenarioContextEntity> existingById = repo.findById(ctx.get_id());
            if (existingById.isPresent()) {
                // UPDATE existing entity
                entityToSave = existingById.get();
                updateEntity(entityToSave, ctx);
                log.debug("Updating existing entity by ID: {}", ctx.get_id());
            } else {
                // ID provided but doesn't exist - treat as new (ID will be regenerated)
                entityToSave = toNewEntity(ctx);
                log.debug("ID provided but not found, creating new entity");
            }
        } else {
            // No ID - check by business key (scenarioName + exampleId)
            Optional<H2ScenarioContextEntity> existingByBusinessKey =
                    findByBusinessKey(ctx.getScenarioName(), ctx.getExampleId());

            if (existingByBusinessKey.isPresent()) {
                // UPDATE existing entity found by business key
                entityToSave = existingByBusinessKey.get();
                updateEntity(entityToSave, ctx);
                log.debug("Updating existing entity by business key: {}_{}",
                        ctx.getScenarioName(), ctx.getExampleId());
            } else {
                // CREATE new entity - ID will be auto-generated
                entityToSave = toNewEntity(ctx);
                log.debug("Creating new entity for: {}_{}",
                        ctx.getScenarioName(), ctx.getExampleId());
            }
        }

        H2ScenarioContextEntity saved = repo.save(entityToSave);
        log.debug("Entity saved with ID: {}", saved.getId());

        return toDomain(saved);
    }

    @Override
    public void delete(String scenarioName) {
        repo.findByScenarioName(scenarioName).ifPresent(repo::delete);
    }

    @Override
    public void deleteAll() {
        repo.deleteAll();
    }

    // ----------------------------------------------------------------------
    // Helper: Find by business key (scenarioName + exampleId)
    // ----------------------------------------------------------------------
    private Optional<H2ScenarioContextEntity> findByBusinessKey(String scenarioName, String exampleId) {
        if (exampleId != null && !exampleId.isEmpty()) {
            return repo.findByScenarioNameAndExampleId(scenarioName, exampleId);
        } else {
            return repo.findByScenarioName(scenarioName);
        }
    }

    // ----------------------------------------------------------------------
    // Mapping: Create NEW Entity (ID will be auto-generated)
    // ----------------------------------------------------------------------
    private H2ScenarioContextEntity toNewEntity(ScenarioStateContext ctx) {
        return H2ScenarioContextEntity.builder()
                // NOTE: id is NOT set - JPA will auto-generate it
                .exampleId(ctx.getExampleId())
                .testCaseId(ctx.getTestCaseId())
                .scenarioName(ctx.getScenarioName())
                .stepStatus(ctx.getStepStatus())
                .stepData(ctx.getStepData())
                .lastUpdated(ctx.getLastUpdated() > 0 ? ctx.getLastUpdated() : System.currentTimeMillis())
                .scenarioStatus(ctx.getScenarioStatus())
                .build();
    }

    // ----------------------------------------------------------------------
    // Update existing entity with new values (preserves ID)
    // ----------------------------------------------------------------------
    private void updateEntity(H2ScenarioContextEntity entity, ScenarioStateContext ctx) {
        // ID is preserved - don't change it
        entity.setExampleId(ctx.getExampleId());
        entity.setTestCaseId(ctx.getTestCaseId());
        entity.setScenarioName(ctx.getScenarioName());
        entity.setStepStatus(ctx.getStepStatus());
        entity.setStepData(ctx.getStepData());
        entity.setLastUpdated(ctx.getLastUpdated() > 0 ? ctx.getLastUpdated() : System.currentTimeMillis());
        entity.setScenarioStatus(ctx.getScenarioStatus());
    }

    // ----------------------------------------------------------------------
    // Mapping (Entity → Domain)
    // ----------------------------------------------------------------------
    private ScenarioStateContext toDomain(H2ScenarioContextEntity e) {
        return ScenarioStateContext.builder()
                ._id(e.getId())
                .exampleId(e.getExampleId())
                .testCaseId(e.getTestCaseId())
                .scenarioName(e.getScenarioName())
                .stepStatus(e.getStepStatus())
                .stepData(e.getStepData())
                .lastUpdated(e.getLastUpdated())
                .scenarioStatus(e.getScenarioStatus())
                .build();
    }
}