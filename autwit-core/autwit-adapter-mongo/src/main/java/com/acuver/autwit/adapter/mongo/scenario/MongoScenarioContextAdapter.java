package com.acuver.autwit.adapter.mongo.scenario;
import com.acuver.autwit.core.domain.ScenarioContext;
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
    public Optional<ScenarioContext> findByScenarioName(String name) {
        return repo.findById(name)
                .map(this::toDomain);
    }

    @Override
    public ScenarioContext save(ScenarioContext state) {
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

    private MongoScenarioContextEntity toEntity(ScenarioContext ctx) {
        return new MongoScenarioContextEntity(
                ctx.getScenarioName(),
                ctx.getStepStatus(),
                ctx.getStepData(),
                ctx.getLastUpdated()
        );
    }

    private ScenarioContext toDomain(MongoScenarioContextEntity e) {
        return new ScenarioContext(
                e.getScenarioName(),
                e.getStepStatus(),
                e.getStepData(),
                e.getLastUpdated()
        );
    }
}
