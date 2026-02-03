package com.acuver.autwit.adapter.postgres;

import com.acuver.autwit.core.domain.ScenarioStateContextEntities;
import com.acuver.autwit.core.ports.ScenarioContextPort;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Optional;


@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "autwit.database", havingValue = "postgres")
public class PostgresScenarioContextAdapter implements ScenarioContextPort {

    private final PostgresScenarioContextRepository repo;

    @Override
    public Optional<ScenarioStateContextEntities> findByScenarioKey(String scenarioKey) {
        return repo.findByScenarioKey(scenarioKey)
                .map(this::toDomain);
    }
    @Override
    public Optional<ScenarioStateContextEntities> findByScenarioName(String scenarioName) {
        return repo.findByScenarioName(scenarioName).map(this::toDomain);
    }

    @Override
    public ScenarioStateContextEntities save(ScenarioStateContextEntities state) {
        return toDomain(repo.save(toEntity(state)));
    }

    @Override
    public void deleteByScenarioKey(String scenarioKey) {
        repo.deleteByScenarioKey(scenarioKey);
    }

    @Override
    public void delete(String scenarioKey) {
        repo.deleteByScenarioKey(scenarioKey);
    }

    @Override
    public void deleteAll() {
        repo.deleteAll();
    }

    // ---------- Mapping ----------
    private PostgresScenarioContextEntity toEntity(ScenarioStateContextEntities ctx) {
        return PostgresScenarioContextEntity.builder()
                .id(ctx.get_id())
                .scenarioKey(ctx.getScenarioKey())
                .exampleId(ctx.getExampleId())
                .testCaseId(ctx.getTestCaseId())
                .scenarioName(ctx.getScenarioName())
                .stepStatus(ctx.getStepStatus())
                .stepData(ctx.getStepData())
                .lastUpdated(ctx.getLastUpdated())
                .scenarioStatus(ctx.getScenarioStatus())
                .build();
    }

    private ScenarioStateContextEntities toDomain(PostgresScenarioContextEntity e) {
        return ScenarioStateContextEntities.builder()
                ._id(e.getId())
                .scenarioKey(e.getScenarioKey())
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
