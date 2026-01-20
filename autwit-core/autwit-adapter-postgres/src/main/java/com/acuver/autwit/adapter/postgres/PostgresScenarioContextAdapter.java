package com.acuver.autwit.adapter.postgres;

import com.acuver.autwit.core.domain.ScenarioStateContext;
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
    public Optional<ScenarioStateContext> findByScenarioName(String scenarioName) {
        return repo.findByScenarioName(scenarioName).map(this::toDomain);
    }

    @Override
    public ScenarioStateContext save(ScenarioStateContext state) {
        return toDomain(repo.save(toEntity(state)));
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
    private PostgresScenarioContextEntity toEntity(ScenarioStateContext ctx) {
        return new PostgresScenarioContextEntity(
                ctx.get_id(),
                ctx.getScenarioKey(),
                ctx.getExampleId(),
                ctx.getTestCaseId(),
                ctx.getScenarioName(),
                ctx.getStepStatus(),
                ctx.getStepData(),
                ctx.getLastUpdated(),
                ctx.getScenarioStatus()
        );
    }

    private ScenarioStateContext toDomain(PostgresScenarioContextEntity e) {
        return new ScenarioStateContext(
                e.getId(),
                e.getScenarioKey(),
                e.getExampleId(),
                e.getTestCaseId(),
                e.getScenarioName(),
                e.getStepStatus(),
                e.getStepData(),
                e.getLastUpdated(),
                e.getScenarioStatus()
        );
    }
}
