package com.acuver.autwit.adapter.h2;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@Entity
@Table(name = "event_context")
public class H2EventContextEntity {

    @Id
    private String canonicalKey;   // primary id across DBs

    private String orderId;
    private String eventType;
    private long eventTimestamp;

    @Lob
    private String kafkaPayload;

    private boolean paused;
    private boolean resumeReady;

    private int retryCount;
    private long firstPausedAt;
    private long lastRetryAt;

    private String status;
    private long createdAt;
}
