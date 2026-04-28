-- SQLite migration for the independent battery collector.
-- Apply before enabling battery-collector.collector.rawFrameLogEnabled,
-- battery-collector.collector.realtimeDataEnabled, or groupCalculationEnabled.

CREATE TABLE IF NOT EXISTS battery_module_frame_log
(
    id                          INTEGER PRIMARY KEY,
    create_time                 TEXT,
    channel_name                TEXT,
    port_name                   TEXT,
    battery_group               INTEGER,
    module_address              INTEGER,
    command_code                TEXT,
    known                       INTEGER CHECK (known IN (0, 1) OR known IS NULL),
    success                     INTEGER CHECK (success IN (0, 1) OR success IS NULL),
    response_flag               INTEGER,
    payload_length              INTEGER,
    payload_hex                 TEXT,
    frame_hex                   TEXT,
    parsed_type                 TEXT,
    cell_voltage                REAL,
    internal_resistance         INTEGER,
    cell_temperature            REAL,
    leakage_status              INTEGER,
    swollen_voltage             REAL,
    charge_discharge_current    REAL,
    float_current               REAL,
    external_voltage            REAL,
    environment_temperature1    REAL,
    environment_temperature2    REAL,
    connect_battery_voltage     REAL,
    connect_test_voltage        REAL
);

CREATE INDEX IF NOT EXISTS idx_battery_module_frame_log_time
    ON battery_module_frame_log (create_time);

CREATE INDEX IF NOT EXISTS idx_battery_module_frame_log_channel
    ON battery_module_frame_log (channel_name, module_address, command_code);

CREATE TABLE IF NOT EXISTS battery_module_cell_realtime
(
    id                    INTEGER PRIMARY KEY,
    create_time           TEXT,
    update_time           TEXT,
    channel_name          TEXT NOT NULL,
    port_name             TEXT,
    battery_group         INTEGER NOT NULL,
    module_address        INTEGER NOT NULL,
    cell_voltage          REAL,
    internal_resistance   INTEGER,
    cell_temperature      REAL,
    leakage_status        INTEGER,
    swollen_voltage       REAL,
    success               INTEGER CHECK (success IN (0, 1) OR success IS NULL),
    response_flag         INTEGER,
    poll_batch_no         TEXT,
    poll_started_at       TEXT
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_battery_module_cell_realtime
    ON battery_module_cell_realtime (channel_name, battery_group, module_address);

CREATE INDEX IF NOT EXISTS idx_battery_module_cell_realtime_update
    ON battery_module_cell_realtime (update_time);

CREATE INDEX IF NOT EXISTS idx_battery_module_cell_realtime_batch
    ON battery_module_cell_realtime (channel_name, battery_group, poll_batch_no);

CREATE TABLE IF NOT EXISTS battery_module_group_realtime
(
    id                         INTEGER PRIMARY KEY,
    create_time                TEXT,
    update_time                TEXT,
    channel_name               TEXT NOT NULL,
    port_name                  TEXT,
    battery_group              INTEGER NOT NULL,
    module_address             INTEGER NOT NULL,
    charge_discharge_current   REAL,
    float_current              REAL,
    external_voltage           REAL,
    environment_temperature1   REAL,
    environment_temperature2   REAL,
    success                    INTEGER CHECK (success IN (0, 1) OR success IS NULL),
    response_flag              INTEGER,
    poll_batch_no              TEXT,
    poll_started_at            TEXT
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_battery_module_group_realtime
    ON battery_module_group_realtime (channel_name, battery_group, module_address);

CREATE INDEX IF NOT EXISTS idx_battery_module_group_realtime_update
    ON battery_module_group_realtime (update_time);

CREATE INDEX IF NOT EXISTS idx_battery_module_group_realtime_batch
    ON battery_module_group_realtime (channel_name, battery_group, poll_batch_no);

CREATE TABLE IF NOT EXISTS battery_module_group_calculation
(
    id                              INTEGER PRIMARY KEY,
    create_time                     TEXT,
    update_time                     TEXT,
    channel_name                    TEXT NOT NULL,
    battery_group                   INTEGER NOT NULL,
    cell_count                      INTEGER,
    online_cell_count               INTEGER,
    stale_cell_count                INTEGER,
    data_fresh                      INTEGER CHECK (data_fresh IN (0, 1) OR data_fresh IS NULL),
    max_voltage_module_address      INTEGER,
    max_cell_voltage                REAL,
    min_voltage_module_address      INTEGER,
    min_cell_voltage                REAL,
    avg_cell_voltage                REAL,
    voltage_range                   REAL,
    max_temperature_module_address  INTEGER,
    max_cell_temperature            REAL,
    min_temperature_module_address  INTEGER,
    min_cell_temperature            REAL,
    avg_cell_temperature            REAL,
    temperature_range               REAL,
    max_resistance_module_address   INTEGER,
    max_internal_resistance         INTEGER,
    min_resistance_module_address   INTEGER,
    min_internal_resistance         INTEGER,
    avg_internal_resistance         REAL,
    resistance_range                INTEGER,
    external_voltage                REAL,
    charge_discharge_current        REAL,
    float_current                   REAL,
    environment_temperature1        REAL,
    environment_temperature2        REAL,
    latest_cell_update_time         TEXT,
    latest_group_update_time        TEXT
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_battery_module_group_calculation
    ON battery_module_group_calculation (channel_name, battery_group);

CREATE INDEX IF NOT EXISTS idx_battery_module_group_calculation_update
    ON battery_module_group_calculation (update_time);
