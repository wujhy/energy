# collector.battery 包职责说明

更新时间：2026-06-22

## 当前目录结构

```text
collector/battery/config           采集配置（BatteryCollectorProperties 等）
collector/battery/controller       对内/页面控制器
collector/battery/mapper           采集链路自有 mapper（实时表、状态表、帧日志表）
collector/battery/model            采集链路模型（帧数据、轮询上下文、实时数据、通道状态、命令模型）
collector/battery/protocol         600 模块协议编解码、协议常量、状态寄存器 codec
collector/battery/runtime          采集运行态（帧 I/O 协调、轮询编排、超时处理）
collector/battery/command          命令队列执行（出队、发送协调、完成回调、连接条电阻流程）
collector/battery/realtime         组级计算与兼容填充
collector/battery/postprocess      后处理编排与处理器（告警、统计、操作日志、内阻、容量预测、兼容历史同步）
collector/battery/state            设备状态持久化与去重
collector/battery/external         外部读取适配边界说明
collector/battery/service          核心服务门面（BatteryCollectorService、BatteryCollectorCommandService 等）
collector/battery/service/impl     服务实现
```

## 各子包职责

### runtime

采集运行态服务，不负责业务命令选择、后处理或页面/Modbus 查询。

- `BatteryCollectorFrameIoService` — 串口帧收发协调
- `BatteryCollectorPollingService` — 轮询循环编排
- `BatteryCollectorTimeoutService` — 超时判断与重试协调

### command

命令队列执行，不直接拼装 opt-log 字段。

- `BatteryCollectorCommandQueueService` — 命令出队、发送协调、完成回调
- `BatteryConnectResistanceCommandProcessor` — 连接条电阻 0F/11/91 流程
- `BatteryCollectorCommandLogService` 第一轮保留在 `service` 包

### realtime

组级计算与兼容填充，不直接访问串口或命令队列。

- `BatteryModuleGroupCalculationService` — 电压/温度/内阻极值、在线数、新鲜度
- `BatteryModuleGroupCompatibilityFillService` — 组级兼容填充

### postprocess

后处理编排与处理器，只消费 `BatteryRealtimePostProcessContext`，不反向访问串口、命令队列、轮询状态。

- `BatteryRealtimePostProcessor` — 处理器接口
- `BatteryRealtimePostProcessService` — 编排服务
- 具体 processor：告警、统计、在线状态、操作日志、容量预测、内阻统计、兼容历史同步

### state

设备状态持久化与去重。

- `BatteryCollectorDeviceStateService` — 采集通道状态持久化

### external

外部读取适配边界说明。页面、JSON/TCP、Modbus 读取应走标准实时模型和快照。

## 旧兼容入口（禁止新增新能力）

- `com.shanhe.project.iot.battery` — 旧 JSON/TCP 蓄电池兼容入口
- `com.shanhe.project.iot.CM03N.BatteryHandler` — 旧 CM03N 上报兼容入口

## 详细重构计划

参见 `01document/energy_refactor_current_plan_20260622.md`。
