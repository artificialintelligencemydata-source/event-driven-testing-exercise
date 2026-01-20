package com.acuver.autwit.adapter.mongo.scenario;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Map;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "scenario_context")
public class MongoScenarioContextEntity {

    @Id
    private UUID _id;
    private String exampleId;
    private String testCaseId;
    private String scenarioName;
    private String scenarioKey;

    private Map<String, String> stepStatus;
    private Map<String, Map<String, String>> stepData;

    private long lastUpdated;
    private String scenarioStatus;
}
