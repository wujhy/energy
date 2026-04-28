-- Battery collector independent tables.
-- Apply before enabling battery-collector.collector.rawFrameLogEnabled.

create table if not exists battery_module_frame_log
(
    id                          integer primary key autoincrement,
    create_time                 datetime,
    channel_name                varchar(64),
    port_name                   varchar(64),
    battery_group               integer,
    module_address              integer,
    command_code                varchar(8),
    known                       integer,
    success                     integer,
    response_flag               integer,
    payload_length              integer,
    payload_hex                 text,
    frame_hex                   text,
    parsed_type                 varchar(64),
    cell_voltage                double,
    internal_resistance         integer,
    cell_temperature            double,
    leakage_status              integer,
    swollen_voltage             double,
    charge_discharge_current    double,
    float_current               double,
    external_voltage            double,
    environment_temperature1    double,
    environment_temperature2    double,
    connect_battery_voltage     double,
    connect_test_voltage        double
);

create index if not exists idx_battery_module_frame_log_time
    on battery_module_frame_log (create_time);

create index if not exists idx_battery_module_frame_log_channel
    on battery_module_frame_log (channel_name, module_address, command_code);

create table if not exists battery_module_cell_realtime
(
    id                    integer primary key autoincrement,
    create_time           datetime,
    update_time           datetime,
    channel_name          varchar(64) not null,
    port_name             varchar(64),
    battery_group         integer not null,
    module_address        integer not null,
    cell_voltage          double,
    internal_resistance   integer,
    cell_temperature      double,
    leakage_status        integer,
    swollen_voltage       double,
    success               integer,
    response_flag         integer,
    poll_batch_no         varchar(64),
    poll_started_at       datetime,
    unique (channel_name, battery_group, module_address)
);

create index if not exists idx_battery_module_cell_realtime_update
    on battery_module_cell_realtime (update_time);

create index if not exists idx_battery_module_cell_realtime_batch
    on battery_module_cell_realtime (channel_name, battery_group, poll_batch_no);

create table if not exists battery_module_group_realtime
(
    id                         integer primary key autoincrement,
    create_time                datetime,
    update_time                datetime,
    channel_name               varchar(64) not null,
    port_name                  varchar(64),
    battery_group              integer not null,
    module_address             integer not null,
    charge_discharge_current   double,
    float_current              double,
    external_voltage           double,
    environment_temperature1   double,
    environment_temperature2   double,
    success                    integer,
    response_flag              integer,
    poll_batch_no              varchar(64),
    poll_started_at            datetime,
    unique (channel_name, battery_group, module_address)
);

create index if not exists idx_battery_module_group_realtime_update
    on battery_module_group_realtime (update_time);

create index if not exists idx_battery_module_group_realtime_batch
    on battery_module_group_realtime (channel_name, battery_group, poll_batch_no);

create table if not exists battery_module_group_calculation
(
    id                              integer primary key autoincrement,
    create_time                     datetime,
    update_time                     datetime,
    channel_name                    varchar(64) not null,
    battery_group                   integer not null,
    cell_count                      integer,
    online_cell_count               integer,
    stale_cell_count                integer,
    data_fresh                      integer,
    max_voltage_module_address      integer,
    max_cell_voltage                double,
    min_voltage_module_address      integer,
    min_cell_voltage                double,
    avg_cell_voltage                double,
    voltage_range                   double,
    max_temperature_module_address  integer,
    max_cell_temperature            double,
    min_temperature_module_address  integer,
    min_cell_temperature            double,
    avg_cell_temperature            double,
    temperature_range               double,
    max_resistance_module_address   integer,
    max_internal_resistance         integer,
    min_resistance_module_address   integer,
    min_internal_resistance         integer,
    avg_internal_resistance         double,
    resistance_range                integer,
    external_voltage                double,
    charge_discharge_current        double,
    float_current                   double,
    environment_temperature1        double,
    environment_temperature2        double,
    latest_cell_update_time         datetime,
    latest_group_update_time        datetime,
    unique (channel_name, battery_group)
);

create index if not exists idx_battery_module_group_calculation_update
    on battery_module_group_calculation (update_time);
