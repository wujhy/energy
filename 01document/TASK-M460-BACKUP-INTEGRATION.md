# _5 备电时长整合计划

## 范围

本文只处理 `_5` 备电时长测试。

- `_3` 核容测试本阶段暂缓，不随 `_5` 一起扩展。
- 空开控制不是 `_5` 备电时长主流程，不合并进 `_5`。
- `_5` 不进入 600 单体采集命令队列。
- `_5` 控制生命周期按外部核容/备电模块链路设计；结果计算继续留在实时采集和后处理链路。
- 存在外部核容/备电模块时，`0x35/0x30/0x36` 相关能力属于 `_5` 控制生命周期需要支持的边界；除非现场明确不存在该模块，否则默认按需要支持外部控制链路处理。

M460 参考源码：

- `02sourceCode/M460_Series_BSP_CMSIS_V3.00.002/SampleCode/NuMaker_M467HJ/LwIP_TCP_EchoClient/MyFiles`

关键文件：

- `capacity.h`
- `capacity.c`
- `server.c`
- `alarm.c`
- `rs485.c` / `modbus.c`

## M460 备电做了什么

M460 备电时长测试主要有三类职责。

### 1. 配置

M460 支持备电时长测试配置：

| 命令 | 含义 | 响应 |
| --- | --- | --- |
| `0x35` | 设置备电时长测试配置 | `0xE5` |
| `0x43` | 读取备电时长测试配置 | `0xC3` |

处理路径：

- `server.c::Server_Set_Power_Support_Time_Test_Configuration`
- `Set_Power_Support_Time_Test_Configuration`

该链路只保存测试参数，不直接计算结果。

### 2. 运行模式

M460 通过 `0x30` 切换核容/备电运行模式：

| 命令 | 含义 | 响应 |
| --- | --- | --- |
| `0x30` | 设置核容/备电运行模式 | `0xE0` |

备电时长相关模式：

| 模式 | 含义 |
| --- | --- |
| `1` / `E_Backup_Power_Status` | 备电时长运行中 |
| `4` / `E_Idle_Status` | 停止/空闲 |

处理路径：

- `server.c::Server_Set_Verify_Capacity_Operation_Mode`
- `capacity.c::Verify_Capacity_Package_Set_Command`

`0x30 mode=1` 不是单纯修改软件状态。M460 会通过外部核容/备电/空开相关模块链路下发控制，使设备进入备电运行模式；`mode=4` 切回停止/空闲。

### 3. 计算和输出

M460 维护两类运行表：

| M460 表 | 含义 |
| --- | --- |
| `Verify_Capacity_Data_Table` | 当前组状态、电压、电流、显示状态 |
| `Verify_Capacity_Array_Table` | 累计容量、备电时长、放电时长 |

运行期周期计算入口：

- `Battery_Array_Verify_Capacity_Process_Time_Count`
- `Battery_Array_Capacity_Calculation`

当模式/状态为 `E_Backup_Power_Status` 时，M460 累计和输出：

- `Array_Support_Time`：备电时长
- `Time_Array_Capacity`：放电容量
- `Discharge_Time`：放电时长
- `Battery_Surplus_Discharge_Time`：剩余放电时长
- `Battery_Array_SOC_Value`：SOC
- `Battery_Array_SOH_Value`：SOH
- 组状态：备电 / 核容 / 充电 / 空闲

最终组级输出由以下链路组装：

- `server.c::Server_Summary_All_Data`
- `rs485.c` / `modbus.c` 输出映射

## 空开边界

M460 在 `capacity.h` 中也定义了空开设备寄存器，但这不是 `_5` 备电时长主流程。

空开定义包括：

- `AIR_SWITCH_DEVICE_TYPE = 0x0600`
- 地址 `0x6E..0x71`
- 寄存器 `0x1100`, `0x1141`, `0x1142`, `0x1143`

结论：

- 不把空开命令合并进 `_5`。
- 不通过 600 单体采集队列发送空开命令。
- 如后续需要空开能力，应作为独立设备能力建模。
- 物理串口共用只属于传输层问题，不改变业务生命周期边界。

## Energy 整合方向

Energy 应围绕外部核容/备电模块补齐 `_5` 控制生命周期，同时尽量把结果计算留在 energy 实时采集和后处理链路。

Energy 已有实时组字段和后处理锚点：

- `BatteryModuleGroupRealtime.batteryPackStatus`
- `BatteryModuleGroupRealtime.backupDuration`
- `BatteryModuleGroupRealtime.disChargeCapacity`
- `BatteryModuleGroupRealtime.disChargeDuration`
- `BatteryModuleGroupRealtime.residualDischargeDuration`
- `BatteryModuleReportLogAdapterService`
- `OperationLogProcessor`
- `CapacityPredictionProcessor`
- `OptLogService`

M460 到 energy 的映射：

| M460 概念 | Energy 对应 |
| --- | --- |
| `Verify_Capacity_Data_Table.Display_Status` | `batteryPackStatus` 实时/上报字段 |
| `Verify_Capacity_Array_Table.Array_Support_Time` | `backupDuration` |
| `Time_Array_Capacity` | `disChargeCapacity` |
| `Discharge_Time` | `disChargeDuration` |
| `Battery_Surplus_Discharge_Time` | `residualDischargeDuration` |
| `Server_Summary_All_Data` | 实时/上报适配和 Modbus 读取映射 |

## 当前实现

当前代码已完成 `_5` 外部控制第一阶段，并已把启停下发从 `ControlBattery` 旧 fallback 收敛进 `BatteryOptCapacityModuleCommandAdapter`：

- `BatteryOptCapacityModuleCommandAdapter`
  - 只承接 `_5` 备电时长外部模块控制
  - 不处理 `_3` 核容
  - 不处理空开
  - start 下发外部模块 `0x30 mode=1`
  - start 设置 `_E0` 回执等待，成功后才插入 `_5` running `dev_opt_log`
  - start 被拒绝、指令生成失败或等待超时时不创建 running log
  - stop 下发外部模块 `0x30 mode=4`
  - stop 下发后关闭 `_5` running log，沿用旧手动停止语义

- `ControlBattery`
  - `_5` 执行和停止均先进入 `BatteryOptCapacityModuleCommandAdapter`
  - `_5` 不再回退 `generateCommand` / `executeCommandAndLog` 旧直发链路
  - 旧 M460/980 fallback 当前只保留给暂缓的 `_3` 核容，避免行为回归

这表示 `_5` 当前按“存在外部核容/备电模块”的控制边界闭合：控制由外部模块 `0x30 mode=1/4` 完成，结果计算继续交给实时采集、`CapacityPredictionProcessor` 和 `BatteryPredictorServiceImpl`。剩余控制链路任务主要是补齐需要推送平台参数时的 `0x35` 备电配置边界，以及按现场需要评估可选 `0x36` 读取能力。

## 状态投影说明

状态字段必须按来源和用途拆开，不能把旧 `pack_data` 或 M460 寄存器里暴露的所有字段都当成 600 单体协议原始事实。

### 内部运行态

这些是 energy 自维护控制状态，应优先驱动业务判断：

- `_5` 备电时长生命周期：running `_5` `dev_opt_log` 是当前生命周期标记。
- `_5` 当前由 `BatteryOptCapacityModuleCommandAdapter` 发送外部核容/备电模块控制命令：start 对应 `0x30 mode=1`，stop 对应 `0x30 mode=4`，start 回执成功后创建 running log。
- `_5` 已从 `ControlBattery` 旧 fallback 收敛到 adapter 边界；后续不应再新增 `_5` 对旧 `generateCommand` / `executeCommandAndLog` 的依赖。
- `_1/_6` 内阻生命周期：`BatteryModeStatusService`、running optLog、pending command、命令响应、超时、停止和补偿结果。
- `_2` 连接条电阻生命周期：沿用采集命令链路的内部 mode/queue/log 模型。
- 计划阻塞和运行态补偿应优先使用内部运行态，而不是只看兼容上报字段。

### 实时 / 计算状态

这些字段可来自采集物理量或后处理结果：

- `batteryPackStatus`：当前由 `BatteryModuleGroupCompatibilityFillService` 基于组充放电电流投影。当前阶段可保留该行为，用于 `_5` 备电时长门控和对外上报适配。
- `backupDuration`、`disChargeCapacity`、`disChargeDuration`、`residualDischargeDuration`、`batteryPackSoc`、`batteryPackSoh`：结果/计算字段，应来自实时采集、容量预测或后处理。不支持时不要写假默认 `0`。
- `deviceWorkStatus`、`deviceWorkIOStatus`：除非确认 energy 侧来源，否则只作为可选兼容字段保留。

### 兼容输出字段

这些字段主要面向旧上报日志、页面、JSON/TCP 或 Modbus/M460 兼容输出：

- `batteryPackStatus`：当前输出基于电流推导的投影，后续如有更完整内部状态聚合器再替换。
- `resistanceTestStatus`：长期不应只是默认字段，应由 `_1/_6` 内部运行态投影后再通过 report-log 和 Modbus adapter 对外暴露。
- M460 `Battery_State_Register`：应由投影后的 `batteryPackStatus` 和投影后的 `resistanceTestStatus` 组合，而不是基于未确认的协议假设。

### 后续优化方向

- `_5` 聚焦控制生命周期和结果字段。
- `CapacityPredictionProcessor` 和 `BatteryPredictorServiceImpl` 是 energy 侧容量/备电结果主路径：它们已监听采集数据中的 `batteryPackStatus` 转换，并在备电结束时估算放电容量。不要在 `_5` 命令 adapter 中重复计算。
- 复核 `_5` 手动执行与现有自动操作日志/后处理的关系。若现有后处理已能可靠基于 BACKUP 转换创建/关闭 `_5` 日志，手动 `_5` 只需要补充用户意图/来源，不应重复计算逻辑。
- 后续新增状态投影清理任务：`_1/_6` 的 `resistanceTestStatus` 应从内部内阻测试运行态推导，并通过 report-log 和 Modbus adapter 对外暴露。
- 修改状态语义前，必须审计 `batteryPackStatus`、`resistanceTestStatus`、`deviceWorkStatus`、`deviceWorkIOStatus`、running optLog 和 `BatteryModeStatusService` 的所有消费方。
- `_5` 不依赖 `resistanceTestStatus`；二者服务不同业务流。

## 后续任务

### TASK-BACKUP-001：结果字段闭环

目标：确认实时/后处理能正确写入或保留以下字段：

- 备电时长
- 放电容量
- 放电时长
- 剩余放电时长
- SOC
- SOH
- 电池组状态

规则：

- 不写假默认 `0`
- 缺失值保持 null/unsupported
- Modbus 读取缺失值时保持现有未就绪行为

### TASK-BACKUP-002：运行态补偿

状态：第一阶段已实现。

目标：避免 `_5` running log 残留导致后续任务长期阻塞。

已实现规则：

- 系统重启后 running log 仍沿用现有全量恢复关闭规则
- 调度前运行期补偿中，超时的 `_5` running log 仅在实时 `batteryPackStatus` 已非 `BACKUP` 时关闭
- `_5` 没有采集队列 mode 投影，也不标记 `_1/_2/_6` 模式

剩余规则：

- 现场确认备电运行耗时后调整 `_5` 超时窗口
- 已有补偿决策覆盖后，不再为 `_5` 运行态补偿机械新增测试；仅在补偿规则继续变化或出现复现缺陷时补充聚焦验证

### TASK-BACKUP-003：外部备电模块控制链路

状态：第一阶段已完成；`0x30 mode=1/4` 已收敛进 adapter 原生链路。

目标：补齐 `_5` 外部备电模块控制生命周期，同时保持结果计算在 energy 后处理链路。

已落地：

- 通过 `BatteryOptCapacityModuleCommandAdapter` 下发 `0x30 mode=1` 启动备电时长运行
- 通过 `BatteryOptCapacityModuleCommandAdapter` 下发 `0x30 mode=4` 停止/空闲
- `_E0` 回执成功后创建 `_5` running log；启动被拒绝或超时不残留 running log
- `_5` 已移除对 `ControlBattery.generateCommand(_5)` 旧 fallback 的依赖

待完成：

- 需要推送平台参数时，补齐 `0x35` 备电配置边界
- 按现场要求评估是否需要 `0x36` 读取外部模块结果；默认仍以 energy 实时采集和后处理为准

边界：

- 不把 `_5` 放进 600 单体采集命令队列
- 不把空开业务命令合并进 `_5`
- 不在 `_5` 命令 adapter 中重复实现 `CapacityPredictionProcessor` / `BatteryPredictorServiceImpl` 的结果计算
- `0x36` 备电/容量数据读取只作为可选兼容能力，除非现场要求直接读取外部模块结果

## 暂缓

### `_3` 核容测试

本阶段不做新的 `_3` 集成工作。现有 `_3` legacy fallback 只用于避免 `_5` 收敛到外部备电控制路径时造成兼容回归。

完成 `_5` 时，不新增 `_3` 执行、停止、状态、结果接入或调度工作。

### 空开

空开作为独立设备能力暂缓。

不要把空开控制挂到 `_5` 备电时长测试上。
