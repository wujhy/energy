# collector.battery.realtime 包职责

实时消费、实时快照、组计算、兼容填充。

## 当前归属

| 类 | 位置 | 说明 |
|---|---|---|
| BatteryModuleRealtimeConsumer | service | 实时消费主入口，第一轮保留原包名 |
| BatteryModuleRealtimeSnapshotService | service | 实时快照服务，高引用门面，保留原包名 |
| BatteryModuleGroupCalculationService | realtime | 组计算服务（电压/温度/内阻极值、在线数、新鲜度） |
| BatteryModuleGroupCompatibilityFillService | realtime | 组级兼容填充服务 |
| BatteryModuleCellCompatibilityFillService | service | 单体兼容填充服务，有外部引用（RestoreServiceImpl），暂保留 |
| BatteryModuleRealtimeAdapterService | service | 实时适配服务，待评估 |
| BatteryModuleReportLogAdapterService | service | 报告日志适配服务，待评估 |
| BatteryCurrentStateService | service | 当前态查询服务，待评估 |
| BatteryModuleCompatReportLogSyncService | service | 兼容历史同步服务，仅 postprocess 内部调用 |
| BatteryRealtimePostProcessContextFactory | service | 后处理上下文工厂，暂不跟本任务 |

## 禁止事项

- 不修改实时表 mapper SQL
- 不修改快照新鲜度规则
- 不修改 batSinSize 限制规则
- 不修改后处理 processor 业务算法
