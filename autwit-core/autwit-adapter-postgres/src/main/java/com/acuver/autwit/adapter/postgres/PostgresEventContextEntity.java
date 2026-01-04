package com.acuver.autwit.adapter.postgres;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@Entity
@Table(name = "event_context")
public class PostgresEventContextEntity {

    @Id
    @Column(name = "canonical_key")
    private String canonicalKey;

    @Column(name = "order_id")
    private String orderId;

    @Column(name = "event_type")
    private String eventType;

    @Column(name = "event_timestamp")
    private long eventTimestamp;

    @Column(name = "kafka_payload", columnDefinition = "TEXT")
    private String kafkaPayload;

    private boolean paused;
    private boolean resumeReady;
    private int retryCount;
    private long firstPausedAt;
    private long lastRetryAt;

    private String status;
    private long timestamp;
}