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

    @Column(name = "paused")
    private boolean paused;

    @Column(name = "resume_ready")
    private boolean resumeReady;

    @Column(name = "retry_count")
    private int retryCount;

    @Column(name = "first_paused_at")
    private long firstPausedAt;

    @Column(name = "last_retry_at")
    private long lastRetryAt;

    @Column(name = "status")
    private String status;

    @Column(name = "timestamp")
    private long timestamp;
}
