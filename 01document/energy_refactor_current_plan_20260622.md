# energy 重构当前权威计划

更新时间：2026-06-22

本文由 Codex 维护，作为后续重构执行的权威入口。其他 AI 只执行本文拆出的明确代码任务，不负责文档统筹、目录盘点、架构判断。

## 1. 分工规则

Codex 负责：

1. 文档修订、任务规划、全局目录盘点。
2. 跨模块影响判断。
3. 高引用门面是否迁包的决策。
4. `BatteryCollectorService` 主流程继续拆分的审查。

其他 AI 只执行明确任务：

1. 只改 1 到 3 个明确文件。
2. 指定类名、方法名和允许修改范围。
3. 给出停止条件和验证命令。
4. 不提交 `03code/energy/sql/rysqlite3.db`。
5. 不提交 `.codegraph/`。
6. 不使用 PowerShell `Get-Content` / `Set-Content` 写中文文件。
7. 不自行新增 README 类任务。
8. 不自行做目录盘点、计划修订、架构判断。
9. 不自行移动高引用类。

## 2. 已完成状态

| 任务 | 状态 | 结果 |
|---|---|---|
| `TASK-REF-COLLECTOR-001` | 已完成 | 已抽取 `BatteryCollectorPollingService` |
| `TASK-REF-COLLECTOR-002` | 已完成 | 已抽取 `BatteryCollectorFrameIoService` |
| `TASK-REF-COLLECTOR-003-TIMEOUT` | 已完成 | 已抽取 `BatteryCollectorTimeoutService` |
| `TASK-REF-COMMAND-001/001B` | 已完成 | 已抽取命令队列执行、完成回调和超时收尾 |
| 连接条电阻流程拆分 | 已完成 | 已新增 `BatteryConnectResistanceCommandProcessor` |
| `TASK-REF-POSTPROCESS-001` | 已完成 | 后处理主代码已迁入 `collector/battery/postprocess` |
| `TASK-REF-STATE-001` | 部分完成 | `BatteryCollectorDeviceStateService` 已迁入 `state`，高引用状态门面保留 |
| `TASK-REF-REALTIME-001` | 部分完成 | 组级计算与兼容填充已迁入 `realtime`，高引用实时门面保留 |
| `TASK-REF-CONSOLIDATE-001/002` | 已完成 | 已收缩兼容历史同步薄服务，暂无其他立即收缩候选 |
| `TASK-REF-ENERGY-DIR-001` | 已完成 | 已重写项目级目录盘点文档 |

## 3. 暂不迁移的高引用门面

以下类第一轮保留原包名，不交给其他 AI 迁移：

1. `BatteryCollectorService`
2. `BatteryCollectorCommandService`
3. `BatteryCollectorCommandLogService`
4. `BatteryModuleControlCommandService`
5. `BatteryModuleRealtimeSnapshotService`
6. `BatteryModuleRealtimeConsumer`
7. `BatteryDeviceStateService`
8. `BatteryDeviceStateServiceImpl`
9. `BatteryModeStatusService`
10. `ControlBattery`
11. `ControlBatterySet`
12. `BatteryReportLogService`
13. `BatteryReportLogServiceImpl`

## 4. Codex 统筹任务

### TASK-CODEX-PLAN-001：清理历史计划过期描述

状态：已完成。已修正 `energy_refactor_plan_20260618.md` 中 `COMMAND-001B`、`COLLECTOR-003-TIMEOUT`、测试适配等过期描述。

### TASK-CODEX-COLLECTOR-001：主流程剩余职责审查

状态：已完成审查，暂不直接改 Java。对象：

1. `BatteryCollectorService.readOnce`
2. `BatteryCollectorService.handleCompletedPendingResponse`
3. `BatteryCollectorService.writeFrame`
4. `BatteryCollectorService.writeFrameWithoutPending`
5. `BatteryCollectorService.closeQuietly`

审查结论：

1. `readOnce` 值得拆分，但不应交给弱 AI 直接执行。该方法同时负责串口读取、receive buffer、帧解码、协议日志、dispatcher 分发、pending 完成状态清理，建议后续由 Codex 拆出 `BatteryCollectorFrameReceiveService` 或等价内部协作类。
2. `handleCompletedPendingResponse` 值得继续拆分，优先把自动编号响应推进和地址缓存重置封装到命令侧协作类；连接条分支已由 `BatteryConnectResistanceCommandProcessor` 承担，暂不再扩。
3. `writeFrame` 和 `writeFrameWithoutPending` 当前只在主服务内部调用，且与串口写入、pending 状态设置、协议日志强绑定。暂不单独迁包；若后续拆，只能作为 `BatteryCollectorFrameIoService` 的小步复用，不改变 pending 字段设置顺序。
4. `closeQuietly` 暂留主服务。它同时处理串口关闭、通道状态重置、receive buffer 清理和设备状态落库，拆分收益小于风险。
5. `BatteryCollectorService` 仍是 controller、命令服务、恢复服务和测试引用的稳定门面，不迁包。

后续候选任务：

#### TASK-CODEX-COLLECTOR-002：拆分串口接收和响应分派

执行者：Codex。

状态：已完成。已新增 `BatteryCollectorFrameReceiveService` 承担串口可用字节读取、receive buffer 维护、decode、帧分发和 pending 命中后的状态清理；`BatteryCollectorService.readOnce` 保留为主流程委托入口。

允许修改：

1. `03code/energy/src/main/java/com/shanhe/project/collector/battery/service/BatteryCollectorService.java`
2. `03code/energy/src/main/java/com/shanhe/project/collector/battery/runtime/BatteryCollectorFrameReceiveService.java`（可新增）
3. 必要时调整 `BatteryCollectorServiceTest`

要求：

1. 只抽取 `readOnce` 内的串口读取、receive buffer、decode、帧遍历协调。
2. 保持 `moduleFrameDispatcher.dispatch` 在 pending 判断前执行。
3. 保持 pending 清理字段顺序不变。
4. 保持非预期帧日志级别和文案不变。
5. 不迁移 `BatteryCollectorService` 包名。

验证：

```powershell
mvn "-DskipTests" compile
mvn "-DskipTests=false" "-Dmaven.test.skip=false" "-Dtest=BatteryCollectorServiceTest,BatteryCollectorCommandLogServiceTest" test
git diff --check
```

实际验证：以上命令已通过。

#### TASK-CODEX-COLLECTOR-003：拆分自动编号响应推进

执行者：Codex。

状态：已完成。已新增 `BatteryCollectorCommandQueueService.handleAutoSetAddressResponse`，收拢自动编号响应后的后续步骤排队、模式停止和地址缓存重置协调；`BatteryCollectorService` 仅保留失败日志和回调传入。

允许修改：

1. `03code/energy/src/main/java/com/shanhe/project/collector/battery/service/BatteryCollectorService.java`
2. `03code/energy/src/main/java/com/shanhe/project/collector/battery/command/BatteryCollectorCommandQueueService.java`
3. 必要时调整 `BatteryCollectorServiceTest`

要求：

1. 只收缩 `handleCompletedPendingResponse` 中自动编号分支。
2. 不改变 `markModeStopped`、`markModeRunning`、地址缓存重置的触发条件。
3. 不改连接条电阻分支。
4. 不改 opt-log 字段、状态码、错误文案。

验证：

```powershell
mvn "-DskipTests" compile
mvn "-DskipTests=false" "-Dmaven.test.skip=false" "-Dtest=BatteryCollectorServiceTest,BatteryCollectorCommandLogServiceTest" test
git diff --check
```

实际验证：以上命令已通过。

### TASK-CODEX-EXTERNAL-001：外部读取缓存边界复核

状态：已完成审查，暂不直接改 Java。对象：

1. `BatteryModuleModbusReadMappingService`
2. `BatteryAlarmHandler`
3. `BatteryPackHandler`
4. 页面当前状态查询相关 service

审查结论：

1. Modbus 读取入口 `BatteryModuleModbusReadMappingService.loadSnapshot` 已优先使用 `BatteryModuleRealtimeSnapshotService.getCachedSnapshot(packNum)`；当构造器未注入 snapshotService 时才回退 `BatteryModuleRealtimeMapper.selectCells/selectGroup`。高频 Modbus 读取不应再主动走 `BatteryReportLogService.lastCache`。
2. 页面当前态入口 `/collector/battery/currentState` 走 `BatteryCurrentStateService.getCurrentState`，已优先读 `BatteryModuleRealtimeSnapshotService.getCachedSnapshot`，缓存未命中时才回退实时表。
3. 大屏和设备控制部分入口已通过 `BatteryModuleReportLogAdapterService.buildReportLog(packNum)` 使用实时快照适配旧 `BatteryReportLog` 结构；例如 `BatteryReportLogController`、`ScreenServiceImpl`、`ControlBattery`、`ControlBatterySet`。
4. 旧 JSON/TCP/CM03N 入口 `BatteryPackHandler`、`BatteryAlarmHandler` 仍属于旧上报处理链路：`BatteryPackHandler` 会解析上报并写 `BatteryReportLog`，`BatteryAlarmHandler` 告警关联数据仍读取 `BatteryReportLogService.lastCache`。这不是只读查询入口，但会影响旧链路告警/统计拿到的上下文是否来自最新实时快照。
5. `BatteryModuleRealtimeSnapshotService` 的快照合并已按 batSinSize 限制数量、按单体编号排序补前一轮缺额，并以连续缺采两轮作为 stale 判断；当前外部读取应优先复用该快照，不应重新实现新鲜度规则。

后续候选任务：

#### TASK-CODEX-EXTERNAL-002：旧 JSON/TCP 告警上下文切实时适配

执行者：Codex。

状态：已完成。`BatteryAlarmHandler` 的 87 告警和 8D 故障告警上下文已优先使用 `BatteryModuleReportLogAdapterService.buildReportLog(packNum)`，实时适配为空或异常时回退 `BatteryReportLogService.lastCache`。

允许修改：

1. `03code/energy/src/main/java/com/shanhe/project/iot/battery/BatteryAlarmHandler.java`
2. `03code/energy/src/main/java/com/shanhe/project/collector/battery/service/BatteryModuleReportLogAdapterService.java`（仅必要时）
3. 必要时调整 `BatteryAlarmHandlerTest`

要求：

1. 仅将 `BatteryAlarmHandler` 中用于告警上下文的 `BatteryReportLogService.lastCache` 优先切到 `BatteryModuleReportLogAdapterService.buildReportLog(packNum)`。
2. 保留旧 `lastCache` 作为实时适配为空或缺少组参数时的回退。
3. 不改告警位解析、告警编码、`alarmFix` 逻辑。
4. 不改 JSON/TCP 上报写库链路。

验证：

```powershell
mvn "-DskipTests" compile
mvn "-DskipTests=false" "-Dmaven.test.skip=false" "-Dtest=BatteryAlarmHandlerTest" test
git diff --check
```

实际验证：以上命令已通过。

#### TASK-CODEX-EXTERNAL-003：旧 JSON/TCP 实时上报后处理 oldInfo 切实时适配

执行者：Codex。

状态：已完成。`BatteryPackHandler.loadRecentOldReportLog` 已优先使用 `BatteryModuleReportLogAdapterService.buildReportLog(packNum)` 作为后处理 oldInfo；实时适配为空或异常时保留 5 分钟内旧 `BatteryReportLogService.lastCache` 回退。

允许修改：

1. `03code/energy/src/main/java/com/shanhe/project/iot/battery/BatteryPackHandler.java`
2. 必要时调整 `BatteryPackHandlerTest`

要求：

1. 仅调整 `loadRecentOldReportLog`，优先使用 `BatteryModuleReportLogAdapterService.buildReportLog(packNum)` 作为 oldInfo。
2. 保留 5 分钟内旧 `BatteryReportLogService.lastCache` 回退，避免实时快照未建立时旧链路失去上下文。
3. 不改上报解析、写 `BatteryReportLog`、容量预测、操作日志、统计服务调用顺序。
4. 不把旧 JSON/TCP 链路改造成新主链路。

验证：

```powershell
mvn "-DskipTests" compile
mvn "-DskipTests=false" "-Dmaven.test.skip=false" "-Dtest=BatteryPackHandlerTest" test
git diff --check
```

实际验证：以上命令已通过。

### TASK-CODEX-M460-001：M460 剩余能力复核

状态：已完成复核，本轮只更新计划，不改 Java。

当前结论：

1. 已基本完成或已有明确落点：
   - 600 模块轮询、响应分发、pending 超时和命令队列协调。
   - 实时快照、batSinSize 限制、按单体编号补前一轮缺额、连续缺采新鲜度。
   - Modbus 高频读的实时快照优先路径。
   - 页面当前态、大屏/控制判断、旧 JSON/TCP 告警上下文和 oldInfo 的实时适配优先路径。
   - 600 命令矩阵中自动编号、均衡、单体内阻、连接条电阻等基础命令入口。
2. 仍未完成且属于功能开发优先级：
   - `OptBatteryController` / `dev_battery_opt` 测试计划闭环：保存计划、到点调度、立即执行、停止、互斥和结果落库还未统一接入独立采集命令体系。
   - Modbus 告警/状态读取扩展：告警寄存器、总告警标志、参数寄存器和可选 SOC/SOH/容量寄存器仍需逐项映射。
   - Modbus 写白名单扩展：当前已存在均衡写路径，内阻测试、连接条测试、地址设置、清调试数据、系数/校准、告警屏蔽/解除仍需冻结寄存器所有权后再接入命令服务。
   - 告警过滤/屏蔽/解除：M460 连续确认、恢复边界、`39/E9`、`3C/EC`、Modbus 屏蔽区语义未完成。
   - 内阻/连接条测试闭环：命令触发已有基础，但结果状态、统计刷新、JSON/TCP/Modbus 可观测状态仍需闭环核验。
   - SOC/SOH/容量/备电/放电时长：只能作为后处理/历史状态任务，不能在采集线程或单批次帧解析中临时硬算。
3. 继续排除：
   - M460 板级外设：蜂鸣器、LED、LCD、继电器、ADC/DAC、RTC、SD、OTA、WDT、FRAM/EEPROM 抽象。
   - 980 PC/网络端协议作为运行时桥接。
   - Modbus TCP，除非出现明确部署需求。
   - 无硬件来源的热失控、氢气、离线总线、网络和传感器故障告警。

后续 Codex 功能任务：

#### TASK-CODEX-M460-OPTPLAN-001：测试计划闭环方案细化

只做方案和任务拆分，不改 Java。

对象：

1. `OptBatteryController`
2. `ControlBattery`
3. `ControlBatterySet`
4. `DevBatteryOpt`
5. `IDevBatteryOptService`
6. `BatteryCollectorCommandService`
7. 现有 scheduled/job 包

输出：

1. 哪些 testType 可映射到独立采集命令。
2. 哪些 testType 暂时 deferred。
3. 到点调度 job 的候选入口、互斥规则、停止规则。
4. 可交给其他 AI 的最小代码任务。

核验结论：

1. `/batteryOpt/edit` 目前不是纯保存计划：`OptBatteryController.edit` 设置默认 `configId/isSync` 后直接调用 `ControlBattery.toSendCmdToOat`，该方法会保存 `DevBatteryOpt`，再按 `_1.._5` 生成旧 980 配置命令并通过 `CommServer.returnCmd` 下发。
2. `/batteryOpt/doCmdOptBatteryTest` 仍以 `ControlBattery.toSendBatteryCmdToOat` 为主，前置判断已优先读取 600 实时快照适配结果，但命令下发仍主要走旧 `CmdBatteryControlService` + `CommServer.returnCmd`。
3. `BatterySyncHandler.syncBatteryOpt` 的立即执行分支已有独立采集命令尝试，但只覆盖 `_2` 连接条电阻测试和 `_6` 单节内阻测试；计划/配置同步分支仍回到 `ControlBattery.toSendCmdToOat`。
4. 在 `scheduled`、`sync`、`device` 相关范围内未确认存在按 `DevBatteryOpt.testTime/isEnabled` 到点扫描并触发测试的 job。当前“测试计划闭环”不能视为已完成。
5. `BatteryCollectorCommandService` 已具备 `_2` 连接条电阻测试和 `_6` 单节内阻测试所需入口；连接条测试会使用电池组单体数量限制，单节内阻要求 `modelNum`。
6. `_1` 内阻测试在旧立即执行链路有模式互斥和 5 分钟重复限制，但当前独立采集命令服务没有明确“整组内阻测试”高级入口，不能简单等同 `_6` 单节内阻测试。
7. `_3` 核容、`_5` 备电时长属于长周期放电/后处理闭环，当前只能暂缓，不应在采集线程或普通命令队列里临时闭环。
8. `_4` 浮充测试、`_7` 充电测试缺少已核验的 M460 命令和运行态闭环，暂不新增映射。

testType 映射边界：

| testType | 名称 | 当前结论 | 后续处理 |
| --- | --- | --- | --- |
| `_1` | 内阻测试 | 旧链路可发起整组测试，600 独立命令只确认单节入口 | 先保留旧链路，另立任务核验 M460 整组内阻协议与状态闭环 |
| `_2` | 连接条电阻测试 | 可映射 `BatteryCollectorCommandService.connectResistanceTest` | 优先切立即执行入口，保留电流条件、运行互斥和日志口径 |
| `_3` | 核容测试 | 暂无短命令闭环 | deferred，等待容量/备电后处理任务 |
| `_4` | 浮充测试 | M460 命令来源和状态未确认 | deferred，不自动迁移 |
| `_5` | 备电时长测试 | 暂无短命令闭环 | deferred，等待容量/备电后处理任务 |
| `_6` | 单节内阻测试 | 可映射 `BatteryCollectorCommandService.singleInternalResistanceTest` | 优先切立即执行入口，必须校验 `modelNum` 和范围 |
| `_7` | 充电测试 | 命令来源和安全条件未确认 | deferred，不自动迁移 |

下一步代码任务必须按以下顺序：

1. 先新增一个窄的测试计划命令适配服务，只负责把 `DevBatteryOpt` 转为独立采集命令结果，不移动 controller/service 包。
2. 再把 `/batteryOpt/doCmdOptBatteryTest` 的 `_2`、`_6` 分支接入该适配服务；适配失败或未支持类型继续走旧链路。
3. 单独核验 `/batteryOpt/edit` 是否应改为“只保存计划、不立即下发旧配置命令”。未确认前不得改行为。
4. 到点调度 job 单独设计，先只扫描 enabled 且到期的计划，不和 controller 改造混在同一个任务。
5. 停止命令单独设计；当前停止逻辑只对 `_1` 有本地状态收口，其余走旧 `genCmd30`，不能被 `_2/_6` 立即执行切换顺手改掉。

#### TASK-CODEX-M460-MODBUS-READ-001：Modbus 告警和参数寄存器任务拆分

只做方案和任务拆分，不改 Java。

对象：

1. `BatteryModuleModbusReadMappingService`
2. `BatteryCurrentStateService`
3. `BatteryDeviceStateService`
4. `IAlarmLogService`
5. `ConfigAttribute` / `AlarmItemLevelVo`

输出：寄存器范围、数据来源、无来源时返回策略、是否可给其他 AI 开发。

#### TASK-CODEX-M460-MODBUS-WRITE-001：Modbus 写白名单扩展方案

只做方案和任务拆分，不改 Java。

对象：

1. `ModbusWriteMappingService`
2. `BatteryCollectorCommandService`
3. `BatteryModuleControlCommandService`
4. `ControlBatterySet`

输出：每个候选写寄存器对应的内部命令、参数校验、互斥、失败返回和验证测试。

#### TASK-CODEX-M460-ALARM-001：告警过滤/屏蔽/解除能力拆分

只做方案和任务拆分，不改 Java。

对象：

1. `AlarmContextProcessor`
2. `BatteryModuleAlarmAdaptService`
3. `IAlarmLogService`
4. `BatteryAlarmHandler`
5. `BatteryAlarmBitMapping`

输出：哪些告警已由实时/设备状态生成，哪些需要过滤，哪些明确 excluded。

#### TASK-CODEX-M460-CAPACITY-001：SOC/SOH/容量状态任务拆分

只做方案和任务拆分，不改 Java。

对象：

1. `CapacityPredictionProcessor`
2. `BatteryPredictorService`
3. `PreBatteryGroupService`
4. `BatteryModuleGroupCompatibilityFillService`
5. `BatteryModuleReportLogAdapterService`

输出：可立即使用的字段、需要历史状态的字段、deferred 字段和测试入口。

## 5. 可交给其他 AI 的代码层任务

这些任务面向整个 `energy` 项目的代码层重构，重点是“类放到合适位置、公共能力抽取、重复逻辑收缩”。测试和 README 后续再统一补。

### TASK-AI-CHECK-001：检查测试中反射旧私有方法残留

只检查，不改代码。

执行：

```powershell
rg "ReflectionTestUtils.invokeMethod\(.*(pollOnce|updateModuleAddressCache|processQueuedModuleCommand|handleTimedOutPendingRequest|shouldStopModeAfterNoResponseCommand)" 03code/energy/src/test/java
```

输出：无命中写“无残留”；有命中列文件和行号，交给 Codex 判断。

### TASK-AI-CHECK-002：检查 collector/battery/service 剩余类清单

只检查，不改代码。

执行：

```powershell
rg --files 03code/energy/src/main/java/com/shanhe/project/collector/battery/service
```

输出：按“门面保留 / 可评估 / 不确定”三类列出，不做迁包建议。

### TASK-AI-CHECK-003：检查是否仍引用已删除薄服务

只检查，不改代码。

执行：

```powershell
rg "BatteryModuleCompatReportLogSyncService|collector\.battery\.service\.BatteryModuleGroupCalculationService|collector\.battery\.service\.BatteryModuleGroupCompatibilityFillService|collector\.battery\.service\.BatteryCollectorDeviceStateService" 03code/energy/src/main/java 03code/energy/src/test/java
```

输出命中列表，交给 Codex 判断是否需要修复。

### TASK-AI-CHECK-004：盘点 util/helper 候选重复逻辑

只检查，不改代码。

执行：

```powershell
rg "new SimpleDateFormat|DateTimeFormatter|bytesToHex|toHex|split\(|StringUtils\.isBlank|CollectionUtils|CacheUtils|CodingUtil" 03code/energy/src/main/java/com/shanhe/project -n
```

输出：按“日期格式化 / 十六进制转换 / 字符串判空 / 缓存访问 / 其他”分类列出候选，不新增工具类。

### TASK-AI-CHECK-005：盘点 device/opt 中蓄电池控制类引用面

只检查，不改代码。

执行：

```powershell
rg "ControlBattery|ControlBatterySet" 03code/energy/src/main/java 03code/energy/src/test/java -n
```

输出：列出调用方，并标注是否跨 `controller`、`sync`、`modbus`、`iot`。不得提出迁包结论。

### TASK-AI-CHECK-006：盘点 BatteryReportLogService 引用面

只检查，不改代码。

执行：

```powershell
rg "BatteryReportLogService|BatteryReportLogServiceImpl" 03code/energy/src/main/java 03code/energy/src/test/java -n
```

输出：列出调用方，并标注是否跨 `iot`、`collector`、`scheduled`、`device`。不得提出迁包结论。

### TASK-AI-CHECK-007：盘点 energy/stat 与 energy/capacity 交叉引用

只检查，不改代码。

执行：

```powershell
rg "com\.shanhe\.project\.energy\.(stat|capacity)" 03code/energy/src/main/java/com/shanhe/project/energy -n
```

输出：列出 `stat` 引用 `capacity`、`capacity` 引用 `stat` 的位置。不得合并目录。

### TASK-AI-CHECK-008：盘点 sync/handler 中蓄电池相关 handler 边界

只检查，不改代码。

执行：

```powershell
rg "Battery|battery|蓄电池" 03code/energy/src/main/java/com/shanhe/project/sync/handler -n
```

输出：列出 handler、方法名、依赖服务。不得移动 handler。

### TASK-AI-CHECK-009：盘点 scheduled 中蓄电池相关定时任务

只检查，不改代码。

执行：

```powershell
rg "Battery|battery|蓄电池|BatteryReport|BatteryPack" 03code/energy/src/main/java/com/shanhe/project/scheduled -n
```

输出：列出任务类、触发方法、依赖服务。不得修改 cron 或业务逻辑。

### TASK-AI-FIX-001：清理明显未使用 import

前置条件：必须先运行编译确认当前代码通过。

允许修改：

1. `03code/energy/src/main/java/com/shanhe/project/collector/battery/service/BatteryCollectorService.java`
2. `03code/energy/src/test/java/com/shanhe/project/collector/battery/service/BatteryCollectorServiceTest.java`

要求：只删除 IDE/编译器明确提示的未使用 import；不改方法体；不改注释；不格式化全文件。

验证：

```powershell
mvn "-DskipTests" compile
git diff --check
```

### TASK-AI-FIX-002：收缩单一调用的测试 helper 重复创建

前置条件：Codex 明确指出重复位置后执行。

允许修改：

1. `03code/energy/src/test/java/com/shanhe/project/collector/battery/service/BatteryCollectorServiceTest.java`

要求：只抽取重复测试 helper；不改断言语义；不新增生产代码。

验证：

```powershell
mvn "-DskipTests=false" "-Dmaven.test.skip=false" "-Dtest=BatteryCollectorServiceTest" test
```

### TASK-AI-RELOC-001：迁移低引用纯模型类前置检查

只检查，不改代码。

候选包：

1. `collector/battery/model`
2. `collector/battery/protocol`
3. `collector/battery/command`

执行：

```powershell
rg "BatteryCollectorCommandResult|BatteryAggregateCommandDefinition" 03code/energy/src/main/java 03code/energy/src/test/java -n
```

输出引用面，交给 Codex 判断是否拆迁包任务。

### TASK-AI-COMMON-001：公共日期格式化工具抽取前置检查

只检查，不改代码。

执行：

```powershell
rg "new SimpleDateFormat|DateTimeFormatter|DateUtil|parseDate|formatDate" 03code/energy/src/main/java/com/shanhe/project -n
```

输出重复模式和候选调用点，不新增工具类。

### TASK-AI-COMMON-002：协议十六进制工具复用检查

只检查，不改代码。

执行：

```powershell
rg "bytesToHex|toHex|hexTo|String\.format\(\"%02X\"" 03code/energy/src/main/java/com/shanhe/project -n
```

输出重复实现位置，交给 Codex 判断是否统一到现有协议工具。

### TASK-AI-CHECK-010：盘点 collector/battery/service 薄服务候选

只检查，不改代码。

执行：

```powershell
rg "class |interface |public .*\\(" 03code/energy/src/main/java/com/shanhe/project/collector/battery/service -n
```

输出：按类列出 public 方法数量、是否仅转调 mapper/service、调用方数量。不得删除类，不得迁包。

### TASK-AI-CHECK-011：盘点 device/config 下蓄电池历史模型边界

只检查，不改代码。

执行：

```powershell
rg "BatteryReportLog|BatteryPack|DevBatteryOpt|BatteryMonitor|MonitorData" 03code/energy/src/main/java 03code/energy/src/main/resources -n
```

输出：列出 domain、mapper、xml、controller、service 的引用链，标注是否涉及 MyBatis namespace/resultMap。不得移动 domain 或 mapper。

### TASK-AI-CHECK-012：盘点 device/opt 命令控制边界

只检查，不改代码。

执行：

```powershell
rg "CmdBatteryControlService|ControlBattery|ControlBatterySet|RestoreService|OptBattery" 03code/energy/src/main/java 03code/energy/src/test/java -n
```

输出：列出页面 controller、service、sync、modbus、collector 的引用关系。不得改控制流程。

### TASK-AI-CHECK-013：盘点 iot legacy 蓄电池入口

只检查，不改代码。

执行：

```powershell
rg "Battery|battery|蓄电池|BatteryReport|BatteryPack" 03code/energy/src/main/java/com/shanhe/project/iot -n
```

输出：列出 handler、入口方法、依赖 service、是否读取实时缓存或历史表。不得移动 `iot` 代码。

### TASK-AI-CHECK-014：盘点 modbus 蓄电池读取入口

只检查，不改代码。

执行：

```powershell
rg "Battery|battery|BatteryModule|BatteryPack|BatteryReport" 03code/energy/src/main/java/com/shanhe/project/modbus 03code/energy/src/main/java/com/shanhe/project/collector/battery/service/BatteryModuleModbusReadMappingService.java -n
```

输出：列出 Modbus 地址映射、调用服务、是否依赖实时缓存。不得修改寄存器地址。

### TASK-AI-CHECK-015：盘点缓存 key 拼接重复逻辑

只检查，不改代码。

执行：

```powershell
rg "String\\.format\\(|CacheUtils|getKey\\(|cacheName|CACHE|cache" 03code/energy/src/main/java/com/shanhe/project -n
```

输出：按业务域列出缓存 key 构造方式，标注是否有配置号、packNum、moduleAddress 维度。不得新增缓存工具。

### TASK-AI-CHECK-016：盘点电池编号排序/截断逻辑

只检查，不改代码。

执行：

```powershell
rg "batSinSize|modelNum|moduleAddress|sort|Comparator|limit\\(|subList" 03code/energy/src/main/java/com/shanhe/project -n
```

输出：列出涉及电池组单体数量、编号排序、截断补齐的代码位置，标注是否影响 JSON/TCP、Modbus、页面查询。不得改排序逻辑。

### TASK-AI-CHECK-017：盘点 mapper XML 与 Java 包迁移风险

只检查，不改代码。

执行：

```powershell
rg "com\\.shanhe\\.project\\.(device|collector|energy|sync|iot|modbus)" 03code/energy/src/main/resources/mybatis 03code/energy/src/main/java -n
```

输出：列出 XML namespace、resultType、parameterType 中直接引用 Java 全限定名的位置。不得修改 XML。

### TASK-AI-FIX-003：收缩单方法薄服务的前置代码任务模板

仅在 Codex 明确指定某个类后执行。不得自行选择目标。

允许修改范围由 Codex 单独给出，默认最多 3 个文件：

1. 薄服务类。
2. 唯一调用方。
3. 对应测试或配置文件。

要求：

1. 只把薄服务的唯一 public 方法内联到唯一调用方。
2. 保留原方法中文注释中的有效业务说明。
3. 不改变事务注解、异步注解、缓存注解语义。
4. 若发现调用方不唯一、存在 AOP 注解或 Spring 循环依赖风险，立即停止。

验证：

```powershell
mvn "-DskipTests" compile
git diff --check
```

### TASK-AI-FIX-004：低风险工具复用代码任务模板

仅在 Codex 明确指定旧实现和目标工具方法后执行。不得自行新增工具类。

允许修改范围由 Codex 单独给出，默认最多 2 个文件。

要求：

1. 只替换完全等价的重复代码。
2. 保留大小写、分隔符、空值处理语义。
3. 不跨协议复用专用工具。
4. 若输出格式无法证明一致，立即停止。

验证：

```powershell
mvn "-DskipTests" compile
git diff --check
```

### TASK-AI-DIRECT-001：盘点新增 iot/battery 测试后的测试包边界

只检查，不改代码。

执行：

```powershell
rg --files 03code/energy/src/test/java/com/shanhe/project/iot/battery
rg "ReflectionTestUtils|Mockito\\.mock|lastCache|buildReportLog" 03code/energy/src/test/java/com/shanhe/project/iot/battery -n
```

输出：

1. 测试类清单。
2. 每个测试类覆盖的 handler。
3. 是否存在重复 setup 代码。
4. 不抽 helper，不改测试，交给 Codex 判断。

### TASK-AI-DIRECT-002：清理 BatteryPackHandlerTest 明显重复 setup 的前置检查

只检查，不改代码。

执行：

```powershell
rg "new BatteryPackHandler|Mockito\\.mock|ReflectionTestUtils\\.setField|reportLogWithPackParam" 03code/energy/src/test/java/com/shanhe/project/iot/battery/BatteryPackHandlerTest.java -n
```

输出：列出重复 setup 行号；不得修改测试。若重复少于 3 处，输出“不建议抽 helper”。

### TASK-AI-DIRECT-003：BatteryAlarmHandler 实时上下文回退路径检查

只检查，不改代码。

执行：

```powershell
rg "loadAlarmContextReportLog|batteryModuleReportLogAdapterService|batteryReportLogService\\.lastCache|alarmBattery\\(" 03code/energy/src/main/java/com/shanhe/project/iot/battery/BatteryAlarmHandler.java 03code/energy/src/test/java/com/shanhe/project/iot/battery/BatteryAlarmHandlerTest.java -n
```

输出：

1. 87 告警是否调用 `loadAlarmContextReportLog`。
2. 8D 故障是否调用 `loadAlarmContextReportLog`。
3. 是否有测试覆盖实时优先。
4. 是否有测试覆盖历史回退。
5. 不改代码，交给 Codex 判断是否补测试。

### TASK-AI-DIRECT-004：BatteryPackHandler oldInfo 回退路径检查

只检查，不改代码。

执行：

```powershell
rg "loadRecentOldReportLog|loadRealtimeOldReportLog|batteryModuleReportLogAdapterService|batteryReportLogService\\.lastCache|doTotalBatteryStep|insertBattery" 03code/energy/src/main/java/com/shanhe/project/iot/battery/BatteryPackHandler.java 03code/energy/src/test/java/com/shanhe/project/iot/battery/BatteryPackHandlerTest.java -n
```

输出：

1. 是否优先调用实时适配。
2. 是否保留 5 分钟历史回退。
3. 是否改变 `executePostSaveProcesses` 调用顺序。
4. 不改代码。

### TASK-AI-DIRECT-005：collector/battery/runtime 包职责候选检查

只检查，不改代码。

执行：

```powershell
rg --files 03code/energy/src/main/java/com/shanhe/project/collector/battery/runtime
rg "public class|public .*\\(" 03code/energy/src/main/java/com/shanhe/project/collector/battery/runtime -n
```

输出：按类列出 public 方法和职责一句话，标注是否依赖 `service` 包。不得移动类。

### TASK-AI-DIRECT-006：collector/battery/command 包职责候选检查

只检查，不改代码。

执行：

```powershell
rg --files 03code/energy/src/main/java/com/shanhe/project/collector/battery/command
rg "public class|public .*\\(" 03code/energy/src/main/java/com/shanhe/project/collector/battery/command -n
```

输出：按类列出 public 方法和职责一句话，标注是否直接依赖 `service` 包。不得移动类。

### TASK-AI-DIRECT-007：协议十六进制输出复用前置检查

只检查，不改代码。

执行：

```powershell
rg "String\\.format\\(\"%02X\"|bytesToHex|toHex\\(" 03code/energy/src/main/java/com/shanhe/project/collector/battery 03code/energy/src/main/java/com/shanhe/project/iot -n
```

输出：

1. 列出格式化十六进制输出的位置。
2. 标注输出是否要求大写、是否允许 null、是否带分隔符。
3. 标注是否可复用 `BatteryCollectorFrameIoService.bytesToHex`。
4. 不修改代码，不新增工具类。

### TASK-AI-DIRECT-008：缓存 key 维度一致性前置检查

只检查，不改代码。

执行：

```powershell
rg "CacheKeyEnum|CacheUtils\\.(get|put|remove)|battery:module:snapshot|String\\.format\\(" 03code/energy/src/main/java/com/shanhe/project/collector/battery 03code/energy/src/main/java/com/shanhe/project/device 03code/energy/src/main/java/com/shanhe/project/iot -n
```

输出：

1. 按缓存用途列 key 构造方式。
2. 标注 key 是否包含 configId、packNum、channelName、moduleAddress。
3. 标注是否存在同一用途多种 key 维度。
4. 不修改代码。

### TASK-AI-DIRECT-009：BatteryModuleReportLogAdapterService 旧模型适配字段检查

只检查，不改代码。

执行：

```powershell
rg "put\\(packMap|setBatNum|setVoltage|setResistance|setTemperature|setBcapacity|setPackParam|setBatteryList" 03code/energy/src/main/java/com/shanhe/project/collector/battery/service/BatteryModuleReportLogAdapterService.java 03code/energy/src/test/java -n
```

输出：

1. 列出适配到旧 `packData` 的字段。
2. 列出适配到旧 `monitorData` 的字段。
3. 标注是否有测试覆盖关键字段。
4. 不新增字段，不改适配逻辑。

### TASK-AI-DIRECT-010：BatteryCurrentStateService 实时读取回退检查

只检查，不改代码。

执行：

```powershell
rg "getCurrentState|getCachedSnapshot|readGroup|readCells|resolveFreshness|FRESHNESS_" 03code/energy/src/main/java/com/shanhe/project/collector/battery/service/BatteryCurrentStateService.java 03code/energy/src/test/java -n
```

输出：

1. 是否优先读取 `getCachedSnapshot`。
2. 缓存未命中是否回退实时表。
3. 新鲜度状态有哪些。
4. 是否有测试覆盖 partial/stale/not_collected。
5. 不修改代码。

### TASK-AI-DIRECT-011：低风险未使用 import 清理候选检查

只检查，不改代码。

执行：

```powershell
mvn "-DskipTests" compile
rg "^import " 03code/energy/src/main/java/com/shanhe/project/collector/battery 03code/energy/src/main/java/com/shanhe/project/iot/battery 03code/energy/src/test/java/com/shanhe/project/iot/battery -n
```

输出：如果编译器或 IDE 明确提示未使用 import，列出文件；否则输出“不建议清理”。不得自行删除。

### TASK-AI-DIRECT-012：低引用模型迁移候选二次检查

只检查，不改代码。

执行：

```powershell
rg "BatteryCollectorCommandResult|BatteryAggregateCommandDefinition|BatteryRealtimePostProcessRequest|BatteryModulePollContext" 03code/energy/src/main/java 03code/energy/src/test/java -n
```

输出：

1. 每个模型的引用文件数量。
2. 是否跨 controller/service/postprocess/command/runtime。
3. 是否出现在 JSON 入参或返回值。
4. 不提出迁包结论，交给 Codex 判断。

### TASK-AI-DIRECT-013：测试计划立即执行采集命令适配服务

目标：新增一个很窄的适配服务，为后续 `/batteryOpt/doCmdOptBatteryTest` 切换 `_2/_6` 做准备。本任务只新增服务和中文类/方法注释，不改现有 controller 行为。

允许新增：

1. `03code/energy/src/main/java/com/shanhe/project/device/opt/service/BatteryOptCollectorCommandAdapter.java`

允许依赖：

1. `BatteryCollectorProperties`
2. `BatteryCollectorCommandService`
3. `IBatteryPackService`
4. `BatteryCollectorCommandResult`
5. `DevBatteryOpt`
6. `BatteryTestEnum`

方法要求：

1. 新增 `public AjaxResult tryExecute(DevBatteryOpt opt)`。
2. 独立采集命令开关 `jsonTcpModuleCommandEnabled` 未开启时返回 `null`。
3. 找不到 `channelName` 时返回 `null`，让旧链路继续兜底。
4. `_2` 调用 `connectResistanceTest(channelName, packNum, batteryCount, null)`，`batteryCount` 从 `IBatteryPackService.getBatteryMaxNumber(packNum)` 获取，异常或空值时使用 `245`，最大值限制为 `245`。
5. `_6` 必须要求 `modelNum` 非空；为空时返回 `null`，非空时调用 `singleInternalResistanceTest(channelName, packNum, modelNum, null)`。
6. 其他 testType 返回 `null`。
7. 命令成功时返回 `AjaxResult.success("独立采集模块命令已加入下发队列", result)`。
8. 命令失败时返回 `null`，不要直接报错，避免改变旧兜底行为。

禁止：

1. 不修改 `OptBatteryController`。
2. 不修改 `ControlBattery`。
3. 不移动 `BatterySyncHandler`。
4. 不新增单元测试，除非编译必须。
5. 不改 MyBatis XML。

验收：

```powershell
mvn "-DskipTests" compile
git diff --check
rg "�|钃|鐙|闆|鍛|绋|€|锟|閽|閻|\?\?\?" 03code/energy/src/main/java/com/shanhe/project/device/opt/service/BatteryOptCollectorCommandAdapter.java
```

### TASK-AI-DIRECT-014：立即执行入口优先尝试采集命令

依赖：必须先完成 `TASK-AI-DIRECT-013`。

目标：让 `/batteryOpt/doCmdOptBatteryTest` 的 `_2`、`_6` 优先尝试独立采集命令，失败或未支持时保留旧链路兜底。

允许修改：

1. `03code/energy/src/main/java/com/shanhe/project/device/opt/controller/OptBatteryController.java`

修改要求：

1. 注入 `BatteryOptCollectorCommandAdapter`。
2. 在 `OptLog opt = optLogService.getRunningOptLog(...)` 检查通过后、调用 `controlBattery.toSendBatteryCmdToOat` 前，调用 `batteryOptCollectorCommandAdapter.tryExecute(devBatteryOpt)`。
3. 当返回值非空时直接返回该结果。
4. 当返回值为空时保持原有 `controlBattery.toSendBatteryCmdToOat` 行为。
5. 保留 `_1` 成功后 `batteryModeStatusService.markRunning` 的原逻辑，不要移动到采集命令分支。
6. 不修改 `/batteryOpt/edit`。
7. 不修改 `/batteryOpt/doCmdStopBattery`。

禁止：

1. 不修改 `ControlBattery`。
2. 不调整旧 980 命令生成。
3. 不新增到点调度。
4. 不新增大范围测试。

验收：

```powershell
mvn "-DskipTests" compile
git diff --check
rg "�|钃|鐙|闆|鍛|绋|€|锟|閽|閻|\?\?\?" 03code/energy/src/main/java/com/shanhe/project/device/opt/controller/OptBatteryController.java
```

### TASK-AI-DIRECT-015：同步入口复用测试计划采集命令适配服务

依赖：必须先完成 `TASK-AI-DIRECT-013`。

目标：收缩 `BatterySyncHandler` 里重复的 `_2/_6` 采集命令映射逻辑，改为复用 `BatteryOptCollectorCommandAdapter`。

允许修改：

1. `03code/energy/src/main/java/com/shanhe/project/sync/handler/BatterySyncHandler.java`

修改要求：

1. 注入 `BatteryOptCollectorCommandAdapter`。
2. `syncBatteryOpt` 的立即执行分支中继续先尝试独立采集命令。
3. 删除或停止使用 `BatterySyncHandler` 内部的 `tryCollectorCommand`、`executeCollectorCommand`、`resolveBatteryCount` 重复逻辑。
4. 当适配服务返回非空结果时，保持原有 ResponseVo 返回语义：失败时把 AjaxResult msg 写入 `msg`，成功时 `msg` 为空。
5. 适配服务返回空时，继续调用 `controlBattery.toSendBatteryCmdToOat`。
6. 计划/配置同步分支 `controlBattery.toSendCmdToOat` 不改。

禁止：

1. 不改变 `MethodEnum._43` 返回。
2. 不改 `BatterySyncHandler` 的单体初装值同步方法。
3. 不改 `BatteryCollectorCommandService`。
4. 不新增 README。

验收：

```powershell
mvn "-DskipTests" compile
git diff --check
rg "tryCollectorCommand|executeCollectorCommand|resolveBatteryCount" 03code/energy/src/main/java/com/shanhe/project/sync/handler/BatterySyncHandler.java
rg "�|钃|鐙|闆|鍛|绋|€|锟|閽|閻|\?\?\?" 03code/energy/src/main/java/com/shanhe/project/sync/handler/BatterySyncHandler.java
```

### TASK-AI-DIRECT-016：测试计划到点调度入口前置盘点

只检查，不改代码。

目标：确认项目是否已有适合承载 `DevBatteryOpt` 到点调度的 job/定时器基础。

执行：

```powershell
rg "@Scheduled|Schedule|Quartz|Job|Timer|DevBatteryOpt|testTime|isEnabled" 03code/energy/src/main/java -n
```

输出：

1. 所有疑似定时任务入口文件。
2. 是否已有扫描 `DevBatteryOpt` 的逻辑。
3. 是否已有全局调度开关或 cron 配置。
4. 不写代码，不新增任务类，交给 Codex 决定调度方案。

## 6. 暂不执行任务

1. 批量迁移 `service` 包剩余类。
2. 移动 `BatteryModuleRealtimeSnapshotService`。
3. 移动 `BatteryDeviceStateService` 或实现类。
4. 移动 `BatteryModeStatusService`。
5. 移动 `ControlBattery`、`ControlBatterySet`。
6. 移动 `BatteryReportLogService`。
7. 重写 `BatteryCollectorService.readOnce`。
8. 合并 `energy/stat` 与 `energy/capacity`。
9. 拆分 `sync/handler`。
10. 拆分 `monitor` 包。
11. 新增多个 README 刷文档数量。
12. 未经 Codex 确认就抽公共工具类。
13. 弱 AI 自行把 `device`、`iot`、`sync`、`modbus` 中的蓄电池类迁入 `collector`。
14. 弱 AI 自行删除看似无用的 service、domain、mapper。
15. 弱 AI 自行修改 MyBatis XML namespace、resultMap、parameterType。

## 7. 固定验证命令

代码任务完成后执行：

```powershell
mvn "-DskipTests" compile
mvn "-DskipTests=false" "-Dmaven.test.skip=false" "-Dtest=BatteryCollectorServiceTest,BatteryCollectorCommandLogServiceTest" test
git diff --check
```

文档任务完成后执行：

```powershell
git diff --check
```
