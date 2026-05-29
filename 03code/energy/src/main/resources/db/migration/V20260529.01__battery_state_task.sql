-- Battery operation/state persistence extension.
-- Keep dev_opt_log as the operation/test fact table and add one current-state table.
ALTER TABLE dev_opt_log ADD COLUMN source TEXT;
ALTER TABLE dev_opt_log ADD COLUMN channel_name TEXT;
ALTER TABLE dev_opt_log ADD COLUMN target_type TEXT;
ALTER TABLE dev_opt_log ADD COLUMN target_address INTEGER;
ALTER TABLE dev_opt_log ADD COLUMN mode INTEGER;
ALTER TABLE dev_opt_log ADD COLUMN status TEXT;
ALTER TABLE dev_opt_log ADD COLUMN request_code INTEGER;
ALTER TABLE dev_opt_log ADD COLUMN response_code INTEGER;
ALTER TABLE dev_opt_log ADD COLUMN protocol_code TEXT;
ALTER TABLE dev_opt_log ADD COLUMN command_name TEXT;
ALTER TABLE dev_opt_log ADD COLUMN request_payload TEXT;
ALTER TABLE dev_opt_log ADD COLUMN response_payload TEXT;
ALTER TABLE dev_opt_log ADD COLUMN error_message TEXT;
ALTER TABLE dev_opt_log ADD COLUMN poll_batch_no TEXT;
ALTER TABLE dev_opt_log ADD COLUMN started_at TEXT;
ALTER TABLE dev_opt_log ADD COLUMN ended_at TEXT;

CREATE INDEX IF NOT EXISTS idx_dev_opt_log_pack_type_status
    ON dev_opt_log (pack_num, type, status);

CREATE INDEX IF NOT EXISTS idx_dev_opt_log_source
    ON dev_opt_log (source);

CREATE INDEX IF NOT EXISTS idx_dev_opt_log_channel_status
    ON dev_opt_log (channel_name, status);

CREATE TABLE IF NOT EXISTS battery_device_state
(
    state_id                 INTEGER PRIMARY KEY AUTOINCREMENT,
    scope_type               TEXT NOT NULL,
    scope_key                TEXT NOT NULL,
    pack_num                 INTEGER,
    channel_name             TEXT,
    model_num                INTEGER,
    state_code               TEXT NOT NULL,
    state_value              TEXT,
    state_level              TEXT,
    source                   TEXT,
    source_ref_id            TEXT,
    mode                     INTEGER,
    opt_log_id               INTEGER,
    first_seen_time          TEXT,
    last_change_time         TEXT,
    last_update_time         TEXT DEFAULT (datetime(CURRENT_TIMESTAMP, 'localtime')),
    expire_time              TEXT,
    detail                   TEXT,
    remark                   TEXT
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_battery_device_state_scope_code
    ON battery_device_state (scope_type, scope_key, state_code);

CREATE INDEX IF NOT EXISTS idx_battery_device_state_pack_code
    ON battery_device_state (pack_num, state_code);

CREATE INDEX IF NOT EXISTS idx_battery_device_state_channel_code
    ON battery_device_state (channel_name, state_code);

CREATE INDEX IF NOT EXISTS idx_battery_device_state_update_time
    ON battery_device_state (last_update_time);

CREATE INDEX IF NOT EXISTS idx_battery_device_state_opt_log
    ON battery_device_state (opt_log_id);
