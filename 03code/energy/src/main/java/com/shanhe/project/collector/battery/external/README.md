# collector.battery.external 包职责

外部读取适配。页面、JSON/TCP、Modbus 读取来源应走标准实时模型和快照，不依赖 `dev_battery_report_log` 作为主来源。

## 当前归属

| 类 | 位置 | 说明 |
|---|---|---|
| BatteryModuleModbusReadMappingService | service | Modbus 只读寄存器映射，保留原包名 |
| BatteryCurrentStateService | service | 当前态查询服务，待评估迁移 |
| ModbusRtuServer | modbus/rtu | RTU 从站运行骨架 |
| ModbusWriteMappingService | modbus/service | Modbus 写寄存器映射 |

## 读取来源规则

1. 标准实时表：`battery_module_cell_realtime`、`battery_module_group_realtime`
2. 实时快照：`BatteryModuleRealtimeSnapshotService`
3. 设备状态：`BatteryDeviceStateService`
4. 告警摘要：`IAlarmLogService`
5. 不依赖 `dev_battery_report_log` 作为主来源

## 禁止事项

- 不修改 Modbus 寄存器地址
- 不修改首次无数据异常语义
- 不修改已有数据后缺字段填 0 语义
- 不修改快照新鲜度规则
- 不修改旧 JSON/TCP 上报行为
