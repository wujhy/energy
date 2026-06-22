# energy 项目级目录盘点

更新时间：2026-06-18

## 1. 顶级包文件统计

| 包 | 文件数 | 主要职责 |
|---|---|---|
| collector | 82 | 蓄电池采集（已重构，子包结构清晰） |
| device | 63 | 设备配置、告警、操作、屏幕 |
| energy | 34 | 能源统计和容量预测 |
| iot | 15 | 旧 JSON/TCP/CM03N 兼容入口 |
| sync | 27 | 数据同步 |
| modbus | 6 | Modbus RTU 和映射 |
| monitor | 15 | 系统监控 |
| scheduled | 8 | 定时任务 |
| system | 14 | 系统用户、文件、字典 |
| common | 1 | 通用基础能力 |

## 2. 各包目录结构和职责

### collector（已重构）

子包结构清晰，职责边界已通过前序重构任务冻结。不再需要项目级调整。

### device

```
device/alarm       告警（controller/domain/mapper/service）
device/config      设备配置（controller/domain/mapper/service）
device/host        主机信息（controller/domain/mapper/service）
device/opt         设备操作（cmd/controller/domain/mapper/service/vo）
device/screen      大屏（controller/service）
```

职责边界清晰，按业务域分层。无明显混放问题。

### energy

```
energy/capacity    容量预测（mapper/service/tool/vo）
energy/stat        能源统计（controller/domain/mapper/service/vo）
```

职责边界清晰。`capacity` 与 `stat` 独立。

### iot

```
iot/CM03N          旧 CM03N 上报入口
iot/battery        旧蓄电池 JSON/TCP 兼容入口
iot/data           数据工厂和启动
iot/model          IoT 模型
iot/service        数据服务
```

旧兼容入口，已在 LEGACY-001 中标记。不新增业务。

### sync

```
sync/common        通用同步工具
sync/consts        同步常量
sync/controller    同步控制器
sync/domain        同步领域模型
sync/handler       同步处理器
sync/scheduled     同步定时任务
sync/service       同步服务
```

职责边界清晰。

### modbus

```
modbus/config      Modbus 配置
modbus/rtu         RTU 从站运行
modbus/service     Modbus 写映射服务
```

职责边界清晰。

### monitor

```
monitor/operlog    操作日志
monitor/server     服务器监控
monitor/cache      缓存管理
```

职责边界清晰。

### scheduled

```
scheduled/         定时任务（设备在线、数据上报、日志清理、缓存等）
```

已在 SCHEDULED-001 中梳理归属。

### system

```
system/dict        字典
system/file        文件
system/user        用户
```

职责边界清晰。

## 3. 候选迁移清单

基于目录盘点，以下为潜在的目录优化候选。按优先级排序：

### 候选 1：device/opt 内 ControlBattery/ControlBatterySet 归属

- **现状**：`device/opt/service/ControlBattery.java` 和 `ControlBatterySet.java` 是蓄电池控制入口，与 `device/opt` 下的其他操作（`ControlBase`、`ControlSwitch`、`RestoreService`）混放。
- **引用面**：ControlBatterySet 被 controller、sync、modbus 多处引用。
- **建议**：第一轮不迁移。它们是跨模块门面，保留原包名。
- **禁止事项**：不改 URL、不改方法签名。

### 候选 2：device/config 下 BatteryReportLogService 归属

- **现状**：`device/config/service/BatteryReportLogService.java` 是旧兼容历史服务，与配置服务混放。
- **引用面**：被 iot、collector、scheduled、device 多处引用。
- **建议**：第一轮不迁移。高引用门面，保留原包名。
- **禁止事项**：不改 mapper namespace、不改 SQL。

### 候选 3：energy/stat 与 energy/capacity 合并评估

- **现状**：`energy/stat`（统计）和 `energy/capacity`（容量预测）在同一个顶级包下，职责不同但有数据依赖。
- **建议**：保持现状。两个子包职责清晰，无混放问题。

### 候选 4：sync 包内 handler 归属

- **现状**：`sync/handler/` 下有 `BatterySyncHandler.java`、`HostHandler.java`、`AttributeHandler.java` 等，职责各异。
- **建议**：第一轮不迁移。handler 按设备类型分，当前结构可接受。

### 候选 5：monitor 包归属

- **现状**：`monitor/` 下有操作日志、服务器监控、缓存管理，职责各异但规模小。
- **建议**：保持现状。文件数少（15），不值得拆分。

## 4. 结论

当前 `03code/energy` 项目级目录结构基本合理。主要的目录混乱问题（`collector/battery/service` 职责过宽）已通过前序重构任务解决。其余包按业务域分层，无明显混放点。

**无需立即执行目录迁移的候选**。后续如有功能开发需要调整目录，按 `TASK-REF-ENERGY-DIR-002` 规则小步执行。

## 5. 三类候选总结

| 类型 | 候选数 | 立即执行 | 保留观察 |
|---|---|---|---|
| 目录迁移 | 2 | 0 | 2（ControlBattery、BatteryReportLogService） |
| 薄服务收缩 | 0 | 0 | 0（已在 CONSOLIDATE-002 盘点） |
| 公共能力提取 | 0 | 0 | 0（已在 COMMON-001 盘点） |
