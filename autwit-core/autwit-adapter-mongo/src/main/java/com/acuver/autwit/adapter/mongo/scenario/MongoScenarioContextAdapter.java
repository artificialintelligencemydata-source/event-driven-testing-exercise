package com.acuver.autwit.adapter.mongo.scenario;
import com.acuver.autwit.core.domain.ScenarioStateContext;
import com.acuver.autwit.core.ports.ScenarioContextPort;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Optional;
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "autwit.database", havingValue = "mongo")
public class MongoScenarioContextAdapter implements ScenarioContextPort {

    private final MongoScenarioContextRepository repo;

    @Override
    public Optional<ScenarioStateContext> findByScenarioName(String name) {
        return repo.findById(name)
                .map(this::toDomain);
    }

    @Override
    public ScenarioStateContext save(ScenarioStateContext state) {
        return toDomain(repo.save(toEntity(state)));
    }

    @Override
    public void delete(String scenarioName) {
        repo.deleteById(scenarioName);
    }

    @Override
    public void deleteAll() {
        repo.deleteAll();
    }

    private MongoScenarioContextEntity toEntity(ScenarioStateContext ctx) {
        return new MongoScenarioContextEntity(
                ctx.get_id(),
                ctx.getExampleId(),
                ctx.getTestCaseId(),
                ctx.getScenarioName(),
                ctx.getStepStatus(),
                ctx.getStepData(),
                ctx.getLastUpdated(),
                ctx.getScenarioStatus()
        );
    }

    private ScenarioStateContext toDomain(MongoScenarioContextEntity e) {
        return new ScenarioStateContext(
                e.get_id(),
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
