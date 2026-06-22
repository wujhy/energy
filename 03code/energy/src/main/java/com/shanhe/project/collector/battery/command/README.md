# collector.battery.command 包职责

命令队列执行。

## 类清单

| 类 | 职责 |
|---|---|
| BatteryCollectorCommandQueueService | 命令出队、发送协调、完成回调、模式停止、地址缓存重置 |
| BatteryConnectResistanceCommandProcessor | 连接条电阻 0F/11/91 流程：排队、解析、计算、最终日志和模式收尾 |

## 不负责

- 不直接拼装 opt-log 字段（归 BatteryCollectorCommandLogService，第一轮保留在 service 包）
- 不负责帧 I/O（归 runtime 包 BatteryCollectorFrameIoService）
- 不负责轮询编排（归 runtime 包 BatteryCollectorPollingService）
- 不负责后处理（归 postprocess 包）

## 第一轮保留原包名的类

- `BatteryCollectorCommandLogService` — 日志持久化，保留在 service 包
- `BatteryCollectorCommandService` — 外部门面，保留在 service 包
- `BatteryModuleControlCommandService` — 命令 payload helper，保留在 service 包
