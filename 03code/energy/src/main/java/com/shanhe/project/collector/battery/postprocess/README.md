# collector.battery.postprocess 包职责

后处理编排与处理器。只消费 `BatteryRealtimePostProcessContext`，不得反向访问串口、命令队列、轮询状态。

## 当前归属

| 类 | 位置 | 说明 |
|---|---|---|
| BatteryRealtimePostProcessor | postprocess | 处理器接口 |
| BatteryRealtimePostProcessService | postprocess | 编排服务 |
| BatteryRealtimePostProcessContext | postprocess | 后处理上下文 |
| PostProcessBatchGuard | postprocess | 批次去重守卫 |
| RealtimeToReportLogAdapter | postprocess | 标准实时到报告日志适配器 |
| VoltageRangeProcessor | postprocess | 电压极差处理器 |
| OnlineStatusProcessor | postprocess | 在线状态处理器 |
| StatisticsProcessor | postprocess | 统计处理器 |
| AlarmContextProcessor | postprocess | 告警上下文处理器 |
| CapacityPredictionProcessor | postprocess | 容量预测处理器 |
| CompatReportLogSyncProcessor | postprocess | 兼容历史同步处理器 |
| OperationLogProcessor | postprocess | 操作日志处理器 |
| ResistanceStatisticsProcessor | postprocess | 内阻统计处理器 |

## 禁止事项

- 不修改 SOC/SOH/容量算法
- 不修改告警规则
- 不修改兼容 report-log 写入规则
- 不修改轮询线程逻辑
- processor 不得访问串口、命令队列、轮询状态
