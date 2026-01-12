-- ======================================================
--  H2 Database Schema
-- ======================================================

-- ======================================================
--  TABLE: event_context
-- ======================================================
CREATE TABLE IF NOT EXISTS event_context (
    canonical_key VARCHAR(200) PRIMARY KEY,
    order_id VARCHAR(100) NOT NULL,
    event_type VARCHAR(200),
    event_timestamp BIGINT,
    kafka_payload TEXT,
    paused BOOLEAN DEFAULT FALSE,
    resume_ready BOOLEAN DEFAULT FALSE,
    retry_count INT DEFAULT 0,
    first_paused_at BIGINT,
    last_retry_at BIGINT,
    status VARCHAR(100),
    timestamp BIGINT
);

-- Indexes
CREATE INDEX IF NOT EXISTS idx_event_context_order_id
    ON event_context(order_id);

CREATE INDEX IF NOT EXISTS idx_event_context_resume_ready
    ON event_context(resume_ready);

-- ======================================================
--  TABLE: event_store
--  Each entry = event received from Kafka
-- ======================================================
CREATE TABLE IF NOT EXISTS event_store (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id VARCHAR(100) NOT NULL,
    event_type VARCHAR(200) NOT NULL,
    event_timestamp BIGINT,
    kafka_payload TEXT,
    received_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Indexes
CREATE INDEX IF NOT EXISTS idx_event_store_order_id
    ON event_store(order_id);

CREATE INDEX IF NOT EXISTS idx_event_store_event_type
    ON event_store(event_type);

-- ======================================================
--  TABLE: scenario_audit_log
--  Used for scenario-specific logging
-- ======================================================
CREATE TABLE IF NOT EXISTS scenario_audit_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    scenario_key VARCHAR(300) NOT NULL,
    step_name VARCHAR(300),
    status VARCHAR(50),
    details TEXT,
    timestamp BIGINT NOT NULL
);

-- Indexes
CREATE INDEX IF NOT EXISTS idx_scenario_key
    ON scenario_audit_log(scenario_key);

CREATE INDEX IF NOT EXISTS idx_scenario_timestamp
    ON scenario_audit_log(timestamp);

-- ======================================================
--  TABLE: scenario_context
--  Stores scenario execution state (step status, step data)
--  Maps are stored as JSON TEXT
-- ======================================================
CREATE TABLE IF NOT EXISTS scenario_context (
    id VARCHAR(255) PRIMARY KEY,           -- Maps to _id / scenarioName
    example_id VARCHAR(255),               -- Maps to exampleId
    test_case_id VARCHAR(255),             -- Maps to testCaseId
    scenario_name VARCHAR(255) NOT NULL,   -- Same as id
    step_status TEXT,                      -- Map<String, String> as JSON
    step_data TEXT,                        -- Map<String, Map<String, String>> as JSON
    last_updated BIGINT,                   -- Timestamp in milliseconds
    scenario_status VARCHAR(100)           -- Overall scenario status
);

-- Indexes for better query performance
CREATE INDEX IF NOT EXISTS idx_scenario_context_scenario_name
    ON scenario_context(scenario_name);

CREATE INDEX IF NOT EXISTS idx_scenario_context_test_case_id
    ON scenario_context(test_case_id);

CREATE INDEX IF NOT EXISTS idx_scenario_context_example_id
    ON scenario_context(example_id);