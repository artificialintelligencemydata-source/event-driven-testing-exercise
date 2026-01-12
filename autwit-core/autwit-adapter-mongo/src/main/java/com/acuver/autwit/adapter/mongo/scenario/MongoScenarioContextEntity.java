package com.acuver.autwit.adapter.mongo.scenario;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "scenario_context")
public class MongoScenarioContextEntity {

    @Id
    private String _id;
    private String exampleId;
    private String testCaseId;
    private String scenarioName;

    private Map<String, String> stepStatus;
    private Map<String, Map<String, String>> stepData;

    private long lastUpdated;
    private String scenarioStatus;
}
