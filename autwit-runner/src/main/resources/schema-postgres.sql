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
    id              UUID PRIMARY KEY,                  -- internal DB id (UUID)
    scenario_key    VARCHAR(300) NOT NULL UNIQUE,      -- AUTWIT Scenario Key (String)
    example_id      VARCHAR(255),                      -- exampleId (Cucumber)
    test_case_id    VARCHAR(255),                      -- testCaseId
    scenario_name   VARCHAR(255) NOT NULL,             -- human-readable name
    step_status     JSONB DEFAULT '{}'::jsonb,          -- Map<String, String>
    step_data       JSONB DEFAULT '{}'::jsonb,          -- Map<String, Map<String, String>>
    last_updated    BIGINT,                            -- epoch millis
    scenario_status VARCHAR(100)                       -- overall status
);

-- Indexes
CREATE INDEX IF NOT EXISTS idx_scenario_context_scenario_key
    ON scenario_context(scenario_key);

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

-- ======================================================
--  TABLE: api_context
--  Purpose:
--    • Stores API call metadata for test orchestration
--    • Tracks request/response payloads, HTTP methods
--    • Supports both API and Service/Flow calls
--    • Used by BaseActions for database persistence
-- ======================================================

CREATE TABLE IF NOT EXISTS api_context (
    id                   BIGSERIAL PRIMARY KEY,
    api_name             VARCHAR(255) NOT NULL UNIQUE,
    http_method          VARCHAR(10) NOT NULL,
    api_template         VARCHAR(2000) NOT NULL,
    data_representation  VARCHAR(50) NOT NULL,
    request_payload      TEXT,
    response_payload     TEXT,
    is_service           BOOLEAN NOT NULL DEFAULT FALSE,
    service_name         VARCHAR(255),
    created_at           TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at           TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Indexes
CREATE INDEX IF NOT EXISTS idx_api_context_api_name
    ON api_context(api_name);

CREATE INDEX IF NOT EXISTS idx_api_context_service_name
    ON api_context(service_name);

CREATE INDEX IF NOT EXISTS idx_api_context_is_service
    ON api_context(is_service);

CREATE INDEX IF NOT EXISTS idx_api_context_http_method
    ON api_context(http_method);

CREATE INDEX IF NOT EXISTS idx_api_context_data_representation
    ON api_context(data_representation);

CREATE INDEX IF NOT EXISTS idx_api_context_created_at
    ON api_context(created_at);

-- ======================================================
--  TRIGGER: Update updated_at timestamp on api_context
-- ======================================================

CREATE OR REPLACE FUNCTION update_api_context_timestamp()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_api_context_updated_at
    BEFORE UPDATE ON api_context
    FOR EACH ROW
    EXECUTE FUNCTION update_api_context_timestamp();

-- ======================================================
--  COMMENTS: Table and Column Documentation
-- ======================================================

COMMENT ON TABLE api_context IS
'Stores API call metadata including request/response payloads, HTTP methods, and service information for test orchestration and audit purposes';

COMMENT ON COLUMN api_context.id IS
'Primary key - auto-generated sequence';

COMMENT ON COLUMN api_context.api_name IS
'Unique API name (e.g., createOrder, getOrderDetails) - used as business key';

COMMENT ON COLUMN api_context.http_method IS
'HTTP method used for the API call (GET, POST, PUT, PATCH, DELETE, HEAD, OPTIONS)';

COMMENT ON COLUMN api_context.api_template IS
'API template or endpoint pattern (e.g., /api/order/{orderId})';

COMMENT ON COLUMN api_context.data_representation IS
'Data format of request/response (XML, JSON, etc.)';

COMMENT ON COLUMN api_context.request_payload IS
'Complete request payload sent to the API';

COMMENT ON COLUMN api_context.response_payload IS
'Complete response payload received from the API';

COMMENT ON COLUMN api_context.is_service IS
'Flag indicating if this is a service/flow call (true) or regular API call (false)';

COMMENT ON COLUMN api_context.service_name IS
'Service name if is_service=true (e.g., CreateOrder, scheduleOrder)';

COMMENT ON COLUMN api_context.created_at IS
'Timestamp when the record was first created';

COMMENT ON COLUMN api_context.updated_at IS
'Timestamp when the record was last updated - automatically maintained by trigger';

-- ======================================================
--  SAMPLE QUERIES: Useful for debugging and reporting
-- ======================================================

-- Query 1: Find all API calls for a specific service
-- SELECT * FROM api_context WHERE service_name = 'CreateOrder' ORDER BY created_at DESC;

-- Query 2: Find all POST requests
-- SELECT api_name, service_name, created_at FROM api_context WHERE http_method = 'POST' ORDER BY created_at DESC;

-- Query 3: Find all service calls
-- SELECT api_name, service_name, http_method, created_at FROM api_context WHERE is_service = true ORDER BY created_at DESC;

-- Query 4: Find recent API calls (last 24 hours)
-- SELECT api_name, http_method, is_service, created_at FROM api_context WHERE created_at > NOW() - INTERVAL '24 hours' ORDER BY created_at DESC;

-- Query 5: Count API calls by HTTP method
-- SELECT http_method, COUNT(*) as call_count FROM api_context GROUP BY http_method ORDER BY call_count DESC;

-- Query 6: Find all XML-based API calls
-- SELECT api_name, service_name, created_at FROM api_context WHERE data_representation = 'XML' ORDER BY created_at DESC;

-- Query 7: Search request/response payloads
-- SELECT api_name, created_at FROM api_context WHERE request_payload LIKE '%OrderNo%' OR response_payload LIKE '%OrderNo%';

-- ======================================================
--  MAINTENANCE QUERIES
-- ======================================================

-- Delete old API context records (older than 30 days)
-- DELETE FROM api_context WHERE created_at < NOW() - INTERVAL '30 days';

-- Archive old records before deletion
-- CREATE TABLE api_context_archive AS SELECT * FROM api_context WHERE created_at < NOW() - INTERVAL '90 days';
-- DELETE FROM api_context WHERE created_at < NOW() - INTERVAL '90 days';