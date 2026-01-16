-- ======================================================
--  PostgreSQL Database Schema
--  AUTWIT – Event-Driven Test Orchestration
-- ======================================================

-- ======================================================
--  TABLE: event_context
--  Purpose:
--    • Tracks resumable scenario state per canonical key
--    • Used by ResumeEngine / Pollers
-- ======================================================

CREATE TABLE IF NOT EXISTS event_context (
    canonical_key   VARCHAR(200) PRIMARY KEY,
    order_id        VARCHAR(100) NOT NULL,
    event_type      VARCHAR(200),
    event_timestamp BIGINT,
    kafka_payload   TEXT,
    paused          BOOLEAN DEFAULT FALSE,
    resume_ready    BOOLEAN DEFAULT FALSE,
    retry_count     INT DEFAULT 0,
    first_paused_at BIGINT,
    last_retry_at   BIGINT,
    status          VARCHAR(100),
    timestamp       BIGINT
);

-- Indexes
CREATE INDEX IF NOT EXISTS idx_event_context_order_id
    ON event_context(order_id);

CREATE INDEX IF NOT EXISTS idx_event_context_resume_ready
    ON event_context(resume_ready);

-- ======================================================
--  TABLE: event_store
--  Purpose:
--    • Immutable store of Kafka events
--    • Debugging / replay / traceability
-- ======================================================

CREATE TABLE IF NOT EXISTS event_store (
    id              SERIAL PRIMARY KEY,
    order_id        VARCHAR(100) NOT NULL,
    event_type      VARCHAR(200) NOT NULL,
    event_timestamp BIGINT,
    kafka_payload   TEXT,
    received_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Indexes
CREATE INDEX IF NOT EXISTS idx_event_store_order_id
    ON event_store(order_id);

CREATE INDEX IF NOT EXISTS idx_event_store_event_type
    ON event_store(event_type);

-- ======================================================
--  TABLE: scenario_audit_log
--  Purpose:
--    • Step-level execution audit
--    • Operator visibility & reporting
-- ======================================================

CREATE TABLE IF NOT EXISTS scenario_audit_log (
    id            SERIAL PRIMARY KEY,
    scenario_key  VARCHAR(300) NOT NULL,
    step_name     VARCHAR(300),
    status        VARCHAR(50),
    details       TEXT,
    timestamp     BIGINT NOT NULL
);

-- Indexes
CREATE INDEX IF NOT EXISTS idx_scenario_audit_log_key
    ON scenario_audit_log(scenario_key);

CREATE INDEX IF NOT EXISTS idx_scenario_audit_log_timestamp
    ON scenario_audit_log(timestamp);

-- ======================================================
--  TABLE: scenario_context
--  Purpose:
--    • Stores scenario execution state
--    • Step status & step data as JSONB
--    • Central to resumable execution
-- ======================================================

CREATE TABLE IF NOT EXISTS scenario_context (
    id              VARCHAR(255) PRIMARY KEY,           -- scenarioId
    example_id      VARCHAR(255),                       -- exampleId (Cucumber)
    test_case_id    VARCHAR(255),                       -- testCaseId
    scenario_name   VARCHAR(255) NOT NULL,              -- human-readable name
    step_status     JSONB DEFAULT '{}'::jsonb,           -- Map<String, String>
    step_data       JSONB DEFAULT '{}'::jsonb,           -- Map<String, Map<String, String>>
    last_updated    BIGINT,                              -- epoch millis
    scenario_status VARCHAR(100)                         -- overall status
);

-- Indexes
CREATE INDEX IF NOT EXISTS idx_scenario_context_scenario_name
    ON scenario_context(scenario_name);

CREATE INDEX IF NOT EXISTS idx_scenario_context_test_case_id
    ON scenario_context(test_case_id);

CREATE INDEX IF NOT EXISTS idx_scenario_context_example_id
    ON scenario_context(example_id);

-- GIN indexes for JSONB
CREATE INDEX IF NOT EXISTS idx_scenario_context_step_status_gin
    ON scenario_context USING GIN (step_status);

CREATE INDEX IF NOT EXISTS idx_scenario_context_step_data_gin
    ON scenario_context USING GIN (step_data);
