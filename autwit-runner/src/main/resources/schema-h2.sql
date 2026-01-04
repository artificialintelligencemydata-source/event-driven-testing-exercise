-- ======================================================
--  TABLE: event_context (H2)
-- ======================================================

CREATE TABLE IF NOT EXISTS event_context (
    id IDENTITY PRIMARY KEY,
    canonical_key VARCHAR(200) NOT NULL UNIQUE,
    order_id VARCHAR(100) NOT NULL,
    event_type VARCHAR(200),
    event_timestamp BIGINT,
    kafka_payload CLOB,
    paused BOOLEAN DEFAULT FALSE,
    resume_ready BOOLEAN DEFAULT FALSE,
    retry_count INT DEFAULT 0,
    first_paused_at BIGINT,
    last_retry_at BIGINT,
    status VARCHAR(100),
    timestamp BIGINT
);

CREATE INDEX IF NOT EXISTS idx_event_context_order_id
    ON event_context(order_id);

CREATE INDEX IF NOT EXISTS idx_event_context_resume_ready
    ON event_context(resume_ready);



-- ======================================================
--  TABLE: event_store (H2)
-- ======================================================

CREATE TABLE IF NOT EXISTS event_store (
    id IDENTITY PRIMARY KEY,
    order_id VARCHAR(100) NOT NULL,
    event_type VARCHAR(200) NOT NULL,
    event_timestamp BIGINT,
    kafka_payload CLOB,
    received_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP()
);

CREATE INDEX IF NOT EXISTS idx_event_store_order_id
    ON event_store(order_id);

CREATE INDEX IF NOT EXISTS idx_event_store_event_type
    ON event_store(event_type);



-- ======================================================
--  TABLE: scenario_audit_log (H2)
-- ======================================================

CREATE TABLE IF NOT EXISTS scenario_audit_log (
    id IDENTITY PRIMARY KEY,
    scenario_key VARCHAR(300) NOT NULL,
    step_name VARCHAR(300),
    status VARCHAR(50),
    details CLOB,
    timestamp BIGINT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_scenario_key
    ON scenario_audit_log(scenario_key);

CREATE INDEX IF NOT EXISTS idx_scenario_timestamp
    ON scenario_audit_log(timestamp);
