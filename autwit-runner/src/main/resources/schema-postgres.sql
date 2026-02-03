-- ======================================================
--  PostgreSQL Database Schema
--  AUTWIT Framework v2.0.0
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
    created_at BIGINT
);

CREATE INDEX IF NOT EXISTS idx_event_context_order_id ON event_context(order_id);
CREATE INDEX IF NOT EXISTS idx_event_context_resume_ready ON event_context(resume_ready);

-- ======================================================
--  TABLE: event_store
-- ======================================================

CREATE TABLE IF NOT EXISTS event_store (
    id SERIAL PRIMARY KEY,
    order_id VARCHAR(100) NOT NULL,
    event_type VARCHAR(200) NOT NULL,
    event_timestamp BIGINT,
    kafka_payload TEXT,
    received_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_event_store_order_id ON event_store(order_id);
CREATE INDEX IF NOT EXISTS idx_event_store_event_type ON event_store(event_type);

-- ======================================================
--  TABLE: scenario_audit_log
-- ======================================================

CREATE TABLE IF NOT EXISTS scenario_audit_log (
    id SERIAL PRIMARY KEY,
    scenario_key VARCHAR(300) NOT NULL,
    step_name VARCHAR(300),
    status VARCHAR(50),
    details TEXT,
    created_at  BIGINT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_scenario_audit_log_key ON scenario_audit_log(scenario_key);
CREATE INDEX IF NOT EXISTS idx_scenario_audit_log_timestamp ON scenario_audit_log(created_at);

-- ======================================================
--  TABLE: scenario_context
-- ======================================================

CREATE TABLE IF NOT EXISTS scenario_context (
    id UUID PRIMARY KEY,
    scenario_key VARCHAR(300) NOT NULL UNIQUE,
    example_id VARCHAR(255),
    test_case_id VARCHAR(255),
    scenario_name VARCHAR(255) NOT NULL,
    step_status JSONB DEFAULT '{}'::jsonb,
    step_data JSONB DEFAULT '{}'::jsonb,
    last_updated BIGINT,
    scenario_status VARCHAR(100)
);

CREATE INDEX IF NOT EXISTS idx_scenario_context_scenario_key ON scenario_context(scenario_key);
CREATE INDEX IF NOT EXISTS idx_scenario_context_scenario_name ON scenario_context(scenario_name);
CREATE INDEX IF NOT EXISTS idx_scenario_context_test_case_id ON scenario_context(test_case_id);
CREATE INDEX IF NOT EXISTS idx_scenario_context_example_id ON scenario_context(example_id);
CREATE INDEX IF NOT EXISTS idx_scenario_context_step_status_gin ON scenario_context USING GIN (step_status);
CREATE INDEX IF NOT EXISTS idx_scenario_context_step_data_gin ON scenario_context USING GIN (step_data);

-- ======================================================
--  TABLE: api_context
-- ======================================================

CREATE TABLE IF NOT EXISTS api_context (
    id BIGSERIAL PRIMARY KEY,
    scenario_key VARCHAR(200) NOT NULL,
    test_case_id VARCHAR(100),
    example_id VARCHAR(100),
    step_key VARCHAR(250) NOT NULL,
    step_name VARCHAR(200) NOT NULL,
    step_execution_index INTEGER NOT NULL DEFAULT 0,
    api_name VARCHAR(100) NOT NULL,
    call_index INTEGER NOT NULL DEFAULT 0,
    http_method VARCHAR(10) NOT NULL,
    api_template VARCHAR(2000) NOT NULL,
    data_representation VARCHAR(50) NOT NULL,
    request_payload TEXT,
    response_payload TEXT,
    is_service BOOLEAN NOT NULL DEFAULT FALSE,
    service_name VARCHAR(100),
    order_no VARCHAR(100),
    order_header_key VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_step_api_call
        UNIQUE (step_key, api_name, call_index)
);


CREATE INDEX IF NOT EXISTS idx_api_context_step_key ON api_context(step_key);
CREATE INDEX IF NOT EXISTS idx_api_context_step_name ON api_context(step_name);
CREATE INDEX IF NOT EXISTS idx_api_context_step_execution_index ON api_context(step_execution_index);
CREATE INDEX IF NOT EXISTS idx_api_context_scenario_key ON api_context(scenario_key);
CREATE INDEX IF NOT EXISTS idx_api_context_scenario_step ON api_context(scenario_key, step_key);
CREATE INDEX IF NOT EXISTS idx_api_context_api_name ON api_context(api_name);
CREATE INDEX IF NOT EXISTS idx_api_context_order_no ON api_context(order_no);
CREATE INDEX IF NOT EXISTS idx_api_context_order_header_key ON api_context(order_header_key);
CREATE INDEX IF NOT EXISTS idx_api_context_http_method ON api_context(http_method);
CREATE INDEX IF NOT EXISTS idx_api_context_data_representation ON api_context(data_representation);
CREATE INDEX IF NOT EXISTS idx_api_context_is_service ON api_context(is_service);
CREATE INDEX IF NOT EXISTS idx_api_context_created_at ON api_context(created_at);
CREATE INDEX IF NOT EXISTS idx_api_context_updated_at ON api_context(updated_at);


-- ======================================================
--  TABLE: api_template
-- ======================================================

CREATE TABLE IF NOT EXISTS api_template (
    id BIGSERIAL PRIMARY KEY,
    api_name VARCHAR(100) NOT NULL UNIQUE,
    http_method VARCHAR(10) NOT NULL,
    endpoint_template VARCHAR(500) NOT NULL,
    request_template TEXT NOT NULL,
    data_representation VARCHAR(50) NOT NULL DEFAULT 'XML',
    is_service BOOLEAN NOT NULL DEFAULT FALSE,
    service_name VARCHAR(100),
    description TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_api_template_api_name ON api_template(api_name);
CREATE INDEX IF NOT EXISTS idx_api_template_http_method ON api_template(http_method);
CREATE INDEX IF NOT EXISTS idx_api_template_is_service ON api_template(is_service);
CREATE INDEX IF NOT EXISTS idx_api_template_service_name ON api_template(service_name) WHERE service_name IS NOT NULL;

-- ======================================================
--  SAMPLE DATA
-- ======================================================

INSERT INTO api_template (api_name, http_method, endpoint_template, request_template, data_representation, is_service, description)
VALUES
    ('createOrder', 'POST', '/api/order', '<Order><OrderNo>{{orderNo}}</OrderNo><BuyerOrganizationCode>{{buyerOrg}}</BuyerOrganizationCode></Order>', 'XML', false, 'Create a new order'),
    ('scheduleOrder', 'POST', '/api/order/schedule', '<Order><OrderHeaderKey>{{orderHeaderKey}}</OrderHeaderKey></Order>', 'XML', false, 'Schedule an existing order'),
    ('releaseOrder', 'POST', '/api/order/release', '<Order><OrderHeaderKey>{{orderHeaderKey}}</OrderHeaderKey></Order>', 'XML', false, 'Release a scheduled order'),
    ('getOrderDetails', 'POST', '/api/order/getDetails', '<Order><OrderHeaderKey>{{orderHeaderKey}}</OrderHeaderKey></Order>', 'XML', false, 'Get order details')
ON CONFLICT (api_name) DO NOTHING;