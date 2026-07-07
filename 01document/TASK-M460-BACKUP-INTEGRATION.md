# _5 备电时长整合计划

## 范围

本文只处理 `_5` 备电时长测试。

- `_3` 核容测试本阶段暂缓，不随 `_5` 一起扩展。
- 通用空开业务不是 `_5` 备电时长主流程，不合并进 `_5`；但 `_5` 备电运行所必需的外部开关/备电模块控制能力，需要作为 `_5` 直控链路的一部分建模。
- `_5` 不进入 600 单体采集命令队列。
- `_5` 控制生命周期按外部核容/备电模块链路设计；结果计算继续留在实时采集和后处理链路。
- 整合目标是 energy 绕过 M460 直接控制外部核容/备电/空开模块；M460 的 `0x30 mode=1/4` 能力只作为协议参考和过渡兼容链路，最终应废弃。

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

该链路只保存测试参数，不直接计算结果。当前 energy 侧已经用 `DevBatteryOpt` 保存平台计划和测试参数，`/batteryOpt/edit` 与同步计划保存入口不再自动下发旧 `0x31..0x35`，避免平台计划和 M460 内置计划双触发。

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

`0x30 mode=1/4` 在 energy 侧是上位机到 M460 的旧 JSON/TCP/C3 控制协议，不是 energy 直接发给下层外接设备的 Modbus 帧。M460 收到 `0x30` 后进入 `Verify_Capacity_Package_Set_Command(..., E_4G_CHANNEL)`，再由 `capacity.c` 根据外部空开/备电设备状态生成下层 Modbus 写单寄存器控制：`mode=1` 对应远程断开/备电运行，`mode=4` 对应远程合上/停止空闲；下层设备响应成功后，M460 再组织 `0xE0` 返回给上位机。energy 最终整合不能停留在 `genCmd30/_E0`，而应直接复刻这段下层 Modbus 控制语义。

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

- 不把通用空开业务命令合并进 `_5`。
- 不通过 600 单体采集队列发送空开命令。
- 如后续需要通用空开能力，应作为独立设备能力建模；`_5` 只使用备电运行所需的最小外部开关/备电模块控制语义。
- 物理串口共用只属于传输层问题，不改变业务生命周期边界。

## Energy 整合方向

Energy 应围绕外部核容/备电模块补齐 `_5` 直控生命周期：不再依赖 M460 代理 `0x30/_E0`，而是在 energy 内实现下层外部模块 Modbus 写控制、响应解析、超时和日志状态闭环；结果计算仍留在 energy 实时采集和后处理链路。

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

当前代码只完成 `_5` 过渡阶段：启停下发已从 `ControlBattery` 旧 fallback 收敛进 `BatteryOptCapacityModuleCommandAdapter`，但该 adapter 仍调用 `genCmd30/_E0` 走 M460 代理链路，不是最终直控实现：

- `BatteryOptCapacityModuleCommandAdapter`
  - 只承接 `_5` 备电时长外部模块控制
  - 不处理 `_3` 核容
  - 不处理空开
  - 过渡 start 仍通过上位协议向 M460 下发 `0x30 mode=1`；最终应改为 energy 直接下发下层外部模块 Modbus 控制
  - 过渡 start 仍等待 M460 `_E0` 回执；最终应等待 energy 直控 Modbus 响应或明确超时结果
  - start 被拒绝、指令生成失败或等待超时时不创建 running log
  - 过渡 stop 仍通过上位协议向 M460 下发 `0x30 mode=4`；最终应改为 energy 直接下发下层外部模块 Modbus 控制
  - stop 下发后关闭 `_5` running log，沿用旧手动停止语义

- `ControlBattery`
  - `_5` 执行和停止均先进入 `BatteryOptCapacityModuleCommandAdapter`
  - `_5` 不再回退 `generateCommand` / `executeCommandAndLog` 旧直发链路
  - 旧 M460/980 fallback 当前只保留给暂缓的 `_3` 核容，避免行为回归

这表示 `_5` 当前只完成了 M460 代理链路的收口，不是最终整合闭环。最终闭环应由 energy 直接向外部核容/备电/空开模块下发 Modbus 写控制，自己维护响应、超时、失败和 running log；结果计算继续交给实时采集、`CapacityPredictionProcessor` 和 `BatteryPredictorServiceImpl`。`0x35` 不再作为默认待补控制链路；只有现场明确仍需要兼容 M460 内置计划时，才作为过渡候选项单独评估，并且必须避免双触发。

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

状态：energy 直控第一阶段已完成；`_5` adapter 已切到 `BackupExternalModuleControlService`，默认配置关闭，现场参数确认前不会误发。

目标：补齐 `_5` 外部备电模块控制生命周期，同时保持结果计算在 energy 后处理链路。

已落地：

- 新增 `BackupExternalModuleProperties`，配置前缀为 `battery-opt.backup-external-module`，默认关闭
- 新增 `BackupExternalModuleControlService`，由 energy 主动构造 Modbus RTU `0x06` 写单寄存器帧并等待外部模块响应
- `BatteryOptCapacityModuleCommandAdapter` 已调用直控服务；start 成功响应后创建 `_5` running log，失败不创建；stop 成功响应后关闭 running log，失败保留运行态
- `_5` 已移除对 `ControlBattery.generateCommand(_5)` 旧 fallback 的依赖，adapter 不再调用 `genCmd30/_E0`

待完成：

- 现场确认外部模块串口/通道配置、站号映射、寄存器地址、写值、响应解析、超时和重试策略是否与默认参考值一致
- 如需现场兼容 M460 代理链路，再单独增加显式开关；当前 `_5` adapter 默认不再调用 `genCmd30/_E0`
- 如现场明确要求设备内置计划同步，再单独评估 `0x35` 配置兼容开关，不能默认恢复旧配置下发
- 按现场要求评估是否需要直读外部模块结果；默认仍以 energy 实时采集和后处理为准

边界：

- 不把 `_5` 放进 600 单体采集命令队列
- 不把通用空开业务命令合并进 `_5`；`_5` 只封装备电运行所需的外部开关/备电模块最小控制
- 不在 `_5` 命令 adapter 中重复实现 `CapacityPredictionProcessor` / `BatteryPredictorServiceImpl` 的结果计算
- `0x36` 备电/容量数据读取只作为可选兼容能力，除非现场要求直接读取外部模块结果


### TASK-BACKUP-004：energy 直控外部模块通道建模

状态：第一阶段已完成。

目标：新增独立于 600 单体采集队列、也独立于旧 `CommServer/genCmd30` 的外部备电模块直控通道。

设计边界：

- 不复用 `BatteryCollectorCommandService` 的 600 单体模块命令队列；外部备电/开关模块是独立下层设备能力。
- 不复用 `ModbusWriteMappingService`；该服务是“外部上位 Modbus 写入 energy”的从站映射，不是 energy 主动下发到外部设备的主站客户端。
- 新增服务应面向 `_5` 业务语义，例如 `startBackup(packNum)` / `stopBackup(packNum)`，内部再映射为 Modbus RTU 写单寄存器。
- M460 `Capacity_Set_Equipment_Operation_Mode` 只作为寄存器语义参考，不能继续作为运行链路。

待确认参数：

- 外部模块串口配置来源：是否复用采集通道配置，还是新增外部模块通道配置。
- 站号映射：M460 使用 `0x6E + packIndex - 1`，energy 是否沿用该映射。
- 写寄存器地址：备电运行/停止对应的远程断开、远程合上寄存器地址。
- 写入值：M460 使用 `0x00FF`，需现场确认外部设备是否一致。
- 响应语义：写单寄存器正常回显、异常码、无响应超时、重试次数。

### TASK-BACKUP-005：替换 `_5` adapter 控制实现

状态：第一阶段已完成。

目标：把 `BatteryOptCapacityModuleCommandAdapter` 从 M460 代理链路切换到 energy 直控外部模块服务。

实施顺序：

1. 新增外部模块直控服务和配置后，先让 `_5` start 调用 `startBackup(packNum)`。
2. start 仅在直控服务返回“写入成功/收到有效响应”后创建 `_5` running log。
3. start 被拒绝、通道缺失、响应异常或超时时，不创建 running log，并返回中文错误。
4. stop 调用 `stopBackup(packNum)`，下发成功后关闭 `_5` running log；若停止下发失败，应返回失败并保留运行态供补偿处理。
5. 切换完成后，`genCmd30/_E0` 仅可保留在显式兼容开关后，默认不再执行。

### TASK-BACKUP-006：废弃 M460 代理链路

目标：在 `_5` 直控链路稳定后，删除或显式开关化 M460 代理路径，避免后续误以为 `0x30/_E0` 是完成态。

删除条件：

- `_5` start/stop 已默认走 energy 直控外部模块服务。
- 计划调度、页面立即执行、同步立即执行均通过同一入口触发直控服务。
- 运行日志、补偿、结果后处理均不依赖 M460 `_E0`。
- 如仍需现场兼容，必须有明确配置开关、中文日志和文档说明。
## 暂缓

### `_3` 核容测试

本阶段不做新的 `_3` 集成工作。现有 `_3` legacy fallback 只用于避免 `_5` 收敛到外部备电控制路径时造成兼容回归。

完成 `_5` 时，不新增 `_3` 执行、停止、状态、结果接入或调度工作。

### 空开

空开作为独立设备能力暂缓。

不要把空开控制挂到 `_5` 备电时长测试上。
