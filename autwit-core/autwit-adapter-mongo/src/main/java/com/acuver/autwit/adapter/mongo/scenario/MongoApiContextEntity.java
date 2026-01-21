package com.acuver.autwit.adapter.mongo.scenario;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * MongoDB document for API context persistence.
 *
 * <h2>VISIBILITY</h2>
 * Package-private - internal to the MongoDB adapter.
 * Not exposed to domain layer or other modules.
 *
 * <h2>COLLECTION</h2>
 * Stored in collection: api_context
 *
 * @author AUTWIT Framework
 * @since 2.0.0
 */
@Document(collection = "api_context")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class MongoApiContextEntity {

    @Id
    private String id;

    @Indexed(unique = true)
    private String apiName;

    @Indexed
    private String httpMethod;

    private String apiTemplate;

    @Indexed
    private String dataRepresentation;

    private String requestPayload;

    private String responsePayload;

    @Indexed
    private Boolean isService;

    @Indexed
    private String serviceName;

    @Indexed
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private Map<String, Object> additionalMetadata;
}