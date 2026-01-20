-- H2 Schema - Use TEXT for JSON storage
CREATE TABLE IF NOT EXISTS scenario_context (
    id VARCHAR(255) PRIMARY KEY,
    example_id VARCHAR(255),
    test_case_id VARCHAR(255),
    scenario_name VARCHAR(255) NOT NULL,
    step_status TEXT DEFAULT '{}',
    step_data TEXT DEFAULT '{}',
    last_updated BIGINT,
    scenario_status VARCHAR(100)
);

CREATE INDEX IF NOT EXISTS idx_scenario_context_scenario_name ON scenario_context(scenario_name);