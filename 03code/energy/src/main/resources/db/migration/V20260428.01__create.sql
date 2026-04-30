-- SQLite migration for the independent battery collector.
-- Default runtime tables are frame log, cell realtime and group realtime.
-- History tables are deferred, alarm events should reuse or extend dev_alarm_log.

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
    id                          INTEGER PRIMARY KEY,
    create_time                 TEXT,
    pack_num                    INTEGER NOT NULL,
    bat_num                     INTEGER NOT NULL,
    voltage                     REAL,
    resistance                  INTEGER,
    temperature                 REAL,
    capacity                    REAL,
    resistance_rage_slip        REAL,
    resistance_rate_change      REAL,
    swollen_voltage             REAL,
    leakage_status              INTEGER,
    poll_batch_no               TEXT,
    poll_started_at             TEXT
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_battery_module_cell_realtime
    ON battery_module_cell_realtime (pack_num, bat_num);

CREATE INDEX IF NOT EXISTS idx_battery_module_cell_realtime_time
    ON battery_module_cell_realtime (create_time);

CREATE INDEX IF NOT EXISTS idx_battery_module_cell_realtime_batch
    ON battery_module_cell_realtime (pack_num, poll_batch_no);

CREATE TABLE IF NOT EXISTS battery_module_group_realtime
(
    id                              INTEGER PRIMARY KEY,
    create_time                     TEXT,
    pack_num                        INTEGER NOT NULL,
    pack_voltage                    REAL,
    pack_current                    REAL,
    battery_pack_float_current      REAL,
    battery_pack_outer_voltage      REAL,
    environment_temperature1        REAL,
    environment_temperature2        REAL,
    charge_discharge_current        REAL,
    float_current                   REAL,
    external_voltage                REAL,
    cell_count                      INTEGER,
    online_cell_count               INTEGER,
    stale_cell_count                INTEGER,
    data_fresh                      INTEGER CHECK (data_fresh IN (0, 1) OR data_fresh IS NULL),
    max_voltage_bat_num             INTEGER,
    max_cell_voltage                REAL,
    min_voltage_bat_num             INTEGER,
    min_cell_voltage                REAL,
    avg_cell_voltage                REAL,
    voltage_range                   REAL,
    max_temperature_bat_num         INTEGER,
    max_cell_temperature            REAL,
    min_temperature_bat_num         INTEGER,
    min_cell_temperature            REAL,
    avg_cell_temperature            REAL,
    temperature_range               REAL,
    max_resistance_bat_num          INTEGER,
    max_internal_resistance         INTEGER,
    min_resistance_bat_num          INTEGER,
    min_internal_resistance         INTEGER,
    avg_internal_resistance         REAL,
    resistance_range                INTEGER,
    battery_pack_soc                REAL,
    battery_pack_soh                REAL,
    latest_cell_update_time         TEXT,
    latest_group_update_time        TEXT,
    group_module_fresh              INTEGER CHECK (group_module_fresh IN (0, 1) OR group_module_fresh IS NULL),
    poll_batch_no                   TEXT,
    poll_started_at                 TEXT
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_battery_module_group_realtime
    ON battery_module_group_realtime (pack_num);

CREATE INDEX IF NOT EXISTS idx_battery_module_group_realtime_time
    ON battery_module_group_realtime (create_time);

CREATE INDEX IF NOT EXISTS idx_battery_module_group_realtime_batch
    ON battery_module_group_realtime (pack_num, poll_batch_no);
