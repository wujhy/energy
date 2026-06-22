# collector.battery 包职责说明

更新时间：2026-06-18

## 当前目录结构

```text
collector/battery/config           采集配置（BatteryCollectorProperties 等）
collector/battery/controller       对内/页面控制器
collector/battery/mapper           采集链路自有 mapper（实时表、状态表、帧日志表）
collector/battery/model            采集链路模型（帧数据、轮询上下文、实时数据、通道状态、命令模型）
collector/battery/protocol         600 模块协议编解码、协议常量、状态寄存器 codec
collector/battery/service          采集核心服务（轮询、命令、实时、快照、状态、日志、兼容填充、Modbus 读取）
collector/battery/service/impl     服务实现
collector/battery/postprocess          后处理编排与处理器（告警、统计、操作日志、内阻、容量预测、兼容历史同步）
```

## 目标目录结构（终态方向，不要求一次性建满）

```text
collector/battery/config           采集配置
collector/battery/controller       对内/页面控制器
collector/battery/mapper           采集链路自有 mapper
collector/battery/model            采集链路模型，后续可逐步细分
collector/battery/protocol         600 模块协议编解码、协议常量、状态寄存器 codec
collector/battery/runtime          采集运行态、轮询循环、通道上下文
collector/battery/command          命令构造、命令队列、命令日志、命令状态
collector/battery/realtime         实时消费、实时快照、实时视图、兼容填充
collector/battery/postprocess      后处理编排与处理器
collector/battery/state            设备状态、模式状态、状态持久化、去重
collector/battery/logging          协议帧日志、采集摘要日志
collector/battery/external         外部读取适配，可继续细分 modbus/jsontcp/page
collector/battery/legacy           只放兼容迁移适配，不放新业务
```

## 职责边界

### 新能力落点

所有新采集能力、新后处理能力、新外部读取适配必须进入 `collector/battery` 或其子包。

### 旧兼容入口（禁止新增新能力）

- `com.shanhe.project.iot.battery` — 旧 JSON/TCP 蓄电池兼容入口
- `com.shanhe.project.iot.CM03N.BatteryHandler` — 旧 CM03N 上报兼容入口

这些旧链路只做兼容入口，不承载新采集、新算法、新告警或新控制能力。

### 外部读取来源

页面、JSON/TCP、Modbus 读取来源应走标准实时模型（`battery_module_cell_realtime`、`battery_module_group_realtime`）和实时快照（`BatteryModuleRealtimeSnapshotService`），不依赖 `dev_battery_report_log` 作为主来源。

## 详细重构任务

参见 `01document/energy_refactor_plan_20260618.md`。
