package com.acuver.autwit.adapter.mongo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * MongoDB storage entity for EventContextEntities.
 * Mirrors the core-domain EventContextEntities fields.
 */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "event_context")
public class MongoEventContextEntity {

    @Id
    private String canonicalKey;

    private String orderId;
    private String eventType;
    private long eventTimestamp;
    private String kafkaPayload;

    private boolean paused;
    private boolean resumeReady;

    private int retryCount;
    private long firstPausedAt;
    private long lastRetryAt;

    private String status;
    private long timestamp;
}
