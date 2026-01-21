package com.acuver.autwit.core.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventContextEntities {
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
