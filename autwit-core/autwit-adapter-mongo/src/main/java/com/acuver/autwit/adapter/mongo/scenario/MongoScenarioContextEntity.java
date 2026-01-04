package com.acuver.autwit.adapter.mongo.scenario;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "scenario_audit_log")
public class MongoScenarioContextEntity {

    @Id
    private String scenarioName;

    private Map<String, String> stepStatus;
    private Map<String, Map<String, String>> stepData;

    private long lastUpdated;
}
