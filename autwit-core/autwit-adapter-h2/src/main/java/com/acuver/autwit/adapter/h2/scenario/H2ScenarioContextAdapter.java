package com.acuver.autwit.adapter.h2.scenario;
import com.acuver.autwit.core.domain.ScenarioContext;
import com.acuver.autwit.core.ports.ScenarioContextPort;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Optional;
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "autwit.database", havingValue = "h2")
public class H2ScenarioContextAdapter implements ScenarioContextPort {

    private final H2ScenarioContextRepository repo;

    @Override
    public Optional<ScenarioContext> findByScenarioName(String name) {
        return repo.findById(name).map(this::toDomain);
    }

    @Override
    public ScenarioContext save(ScenarioContext ctx) {
        H2ScenarioContextEntity entity = toEntity(ctx);
        H2ScenarioContextEntity saved = repo.save(entity);
        return toDomain(saved);
    }

    @Override
    public void delete(String scenarioName) {
        repo.deleteById(scenarioName);
    }

    @Override
    public void deleteAll() {
        repo.deleteAll();
    }

    // ----------------------------------------------------------------------
    // Mapping (Domain → Entity)
    // ----------------------------------------------------------------------
    private H2ScenarioContextEntity toEntity(ScenarioContext ctx) {
        return H2ScenarioContextEntity.builder()
                .scenarioName(ctx.getScenarioName())
                .stepStatus(ctx.getStepStatus())
                .stepData(ctx.getStepData())
                .lastUpdated(ctx.getLastUpdated())
                .build();
    }

    // ----------------------------------------------------------------------
    // Mapping (Entity → Domain)
    // ----------------------------------------------------------------------
    private ScenarioContext toDomain(H2ScenarioContextEntity e) {
        return ScenarioContext.builder()
                .scenarioName(e.getScenarioName())
                .stepStatus(e.getStepStatus())
                .stepData(e.getStepData())
                .lastUpdated(e.getLastUpdated())
                .build();
    }
}
