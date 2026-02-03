package com.acuver.autwit.adapter.mongo;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Document(collection = "api_template")
public class MongoApiTemplateDocument {

    @Id
    private String id;

    @Indexed(unique = true)
    private String apiName;

    private String httpMethod;
    private String endpointTemplate;
    private String requestTemplate;
    private String dataRepresentation;
    private Boolean isService;
    private String serviceName;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}