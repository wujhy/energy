# energy 项目级目录盘点

更新时间：2026-06-22

本文由 Codex 维护，用于全局统筹目录重构。其他 AI 不应修改本文件；其他 AI 只执行本文拆出的、边界明确的代码任务。

## 1. 当前结论

`03code/energy` 目录整体按业务域划分，暂不建议继续做项目级大迁包。

当前优先级：

1. 保持已完成的 `collector/battery` 重构结果稳定。
2. 不移动高引用门面类，尤其是跨 `device`、`iot`、`sync`、`modbus`、`scheduled` 的类。
3. 后续若要继续目录调整，必须按单个类或单个小包拆任务，先列引用面、停止条件和验证命令。

## 2. 顶级包职责

| 包 | 职责 | 结论 |
|---|---|---|
| `collector` | 蓄电池采集、实时缓存、后处理、外部读取适配 | `collector/battery` 已完成主要拆分，继续以稳定为主 |
| `device` | 设备配置、告警、操作、屏幕、历史兼容服务 | 高引用门面较多，第一轮不做迁包 |
| `energy` | 能源统计、容量预测 | `stat` 与 `capacity` 职责清晰，暂不合并 |
| `iot` | 旧 JSON/TCP/CM03N 兼容入口 | 标记为 legacy，禁止新增主业务 |
| `sync` | 数据同步 controller、handler、job、service | 当前按同步域聚合，暂不拆 |
| `modbus` | Modbus RTU 与读写映射 | 外部协议边界，暂不迁移 |
| `scheduled` | 定时任务 | 已补归属说明，暂不迁包 |
| `monitor` | 操作日志、服务监控、缓存管理 | 规模小，暂不拆 |
| `system` | 用户、字典、文件 | 保持现状 |
| `common` | 通用基础能力 | 暂无立即抽取任务 |

## 3. collector/battery 现状

已完成的主要边界：

| 包 | 关键类 | 职责 |
|---|---|---|
| `runtime` | `BatteryCollectorFrameIoService` | 串口打开、关闭、写字节、收包 buffer 裁剪 |
| `runtime` | `BatteryCollectorPollingService` | 自动轮询编排、地址列表、地址缓存更新 |
| `runtime` | `BatteryCollectorTimeoutService` | pending 超时判断、重试、最终超时收尾 |
| `command` | `BatteryCollectorCommandQueueService` | 命令出队、pending 构造、完成快照、模式停止、命令日志更新 |
| `command` | `BatteryConnectResistanceCommandProcessor` | 连接条电阻 0F/11/91 流程推进和结果写入 |
| `state` | `BatteryCollectorDeviceStateService` | 采集运行态持久化去重 |
| `postprocess` | `*Processor`、`BatteryRealtimePostProcessService` | 采集入库后的实时后处理 |
| `realtime` | group 计算与兼容填充 | 组级实时数据计算与旧字段兼容 |

保留在 `service` 包的高引用或门面类：

| 类 | 保留原因 |
|---|---|
| `BatteryCollectorService` | 采集主流程门面，仍持有串口读写、响应分派入口 |
| `BatteryCollectorCommandService` | 外部命令入口门面 |
| `BatteryCollectorCommandLogService` | opt-log 持久化服务，命令服务依赖 |
| `BatteryModuleRealtimeSnapshotService` | 页面、Modbus、device、collector 多方读取 |
| `BatteryDeviceStateService` / `impl` | 告警、定时任务、Modbus、恢复链路依赖 |
| `BatteryModeStatusService` | 旧 iot、device、命令链路依赖 |

## 4. 候选项

以下候选只允许由 Codex 继续统筹，不直接交给弱 AI 做目录迁移。

### 4.1 `device/opt` 中的蓄电池控制入口

候选类：

- `device/opt/service/ControlBattery.java`
- `device/opt/service/ControlBatterySet.java`

结论：第一轮不迁移。

原因：它们是旧页面、设备操作、同步、Modbus 之间的跨模块门面。移动 package 收益小，破坏面大。

### 4.2 `device/config` 中的历史兼容服务

候选类：

- `device/config/service/BatteryReportLogService.java`
- `device/config/service/impl/BatteryReportLogServiceImpl.java`

结论：第一轮不迁移。

原因：该服务被 iot、collector、scheduled、device 多处引用，并绑定 mapper namespace 与历史 SQL 语义。目录迁移收益不足。

### 4.3 `collector/battery/service` 剩余门面

可后续评估，但不立即迁移：

- `BatteryModuleCellCompatibilityFillService`
- `BatteryModuleRealtimeAdapterService`
- `BatteryModuleReportLogAdapterService`
- `BatteryCurrentStateService`

规则：只允许先做引用面核验和 README 说明；不得为了减少 `service` 文件数量强行迁包。

## 5. 已完成任务状态

| 任务 | 状态 | 说明 |
|---|---|---|
| `TASK-REF-DIR-001` | 已完成 | 已建立 `collector/battery` 包职责说明 |
| `TASK-REF-COLLECTOR-001` | 已完成 | 已抽取轮询编排 |
| `TASK-REF-COLLECTOR-002` | 已完成 | 已抽取最小串口帧 I/O |
| `TASK-REF-COLLECTOR-003-TIMEOUT` | 已完成 | 已新增 `BatteryCollectorTimeoutService` |
| `TASK-REF-COMMAND-001` | 已完成 | 已抽取命令队列基础能力 |
| `TASK-REF-COMMAND-001B` | 已完成 | 已补齐出队、发送协调、完成回调、超时完成 |
| `TASK-REF-POSTPROCESS-001` | 已完成 | 后处理主代码已迁入 `collector/battery/postprocess` |
| `TASK-REF-STATE-001` | 部分完成 | `BatteryCollectorDeviceStateService` 已迁入 `state`，高引用状态门面保留 |
| `TASK-REF-CONSOLIDATE-001` | 已完成 | 已收缩 `BatteryModuleCompatReportLogSyncService` |
| `TASK-REF-CONSOLIDATE-002` | 已完成 | 已盘点，无其他立即收缩候选 |
| `TASK-REF-SCHEDULED-001` | 已完成 | 已补定时任务归属说明 |
| `TASK-REF-EXTERNAL-001` | 已完成 | 已补外部读取适配边界说明 |
| `TASK-REF-ENERGY-DIR-001` | 已完成 | 本文完成项目级目录盘点 |

## 6. 后续任务池

以下不是“必须立即执行”的任务，只在有明确收益时继续。

### TASK-REF-DOC-001：同步主计划状态

类型：文档任务，仅 Codex 执行。

内容：

1. 修正 `energy_refactor_plan_20260618.md` 中 `COMMAND-001B`、`COLLECTOR-003-TIMEOUT` 的过期描述。
2. 删除“测试仍需适配”的过期结论。
3. 保留高引用门面不迁移的规则。

### TASK-REF-COLLECTOR-004：主流程剩余职责审查

类型：代码审查/计划任务，仅 Codex 执行。

只审查，不改代码：

- `BatteryCollectorService.readOnce`
- `BatteryCollectorService.handleCompletedPendingResponse`
- `BatteryCollectorService.writeFrame`
- `BatteryCollectorService.writeFrameWithoutPending`
- `BatteryCollectorService.closeQuietly`

输出：是否值得继续抽取、风险点、验证命令。

### TASK-REF-CODE-AUDIT-001：盘点可收缩的薄服务

类型：代码审查任务，可交给其他 AI 做初筛，Codex 负责结论。

只检查，不改代码：

- `collector/battery/service`
- `device/config/service`
- `device/opt/service`
- `energy/stat/service`
- `energy/capacity/service`

输出：

1. 服务类名。
2. public 方法数。
3. 每个 public 方法是否只是直接转调另一个 service / mapper。
4. 调用方数量。
5. 不提出迁移或删除结论，交给 Codex 判断。

### TASK-REF-CODE-AUDIT-002：盘点可归入功能包的模型类

类型：代码审查任务，可交给其他 AI 做初筛，Codex 负责结论。

只检查，不改代码：

- `collector/battery/model`
- `device/config/domain`
- `device/opt/domain`
- `energy/stat/domain`
- `energy/capacity/vo`
- `sync/domain`

输出：

1. 模型类名。
2. 是否只被单一业务包引用。
3. 是否跨 controller / service / mapper / xml 使用。
4. 是否涉及 JSON 字段名、MyBatis resultMap、序列化。
5. 不提出迁包结论，交给 Codex 判断。

### TASK-REF-CODE-AUDIT-003：盘点公共工具抽取候选

类型：代码审查任务，可交给其他 AI 做初筛，Codex 负责结论。

只检查，不改代码：

- 日期格式化与解析。
- 十六进制转换。
- 缓存 key 拼接。
- 电池编号、组号、单体号排序和截断。
- JSON 解析与字段安全读取。

输出：

1. 重复代码所在文件和方法。
2. 现有工具类是否已经覆盖。
3. 是否属于协议专用能力。
4. 是否适合抽到 `collector/battery/protocol`、`collector/battery/model`、`common` 或保留原地。
5. 不新增工具类。

## 7. 交给其他 AI 的规则

其他 AI 只执行边界明确的代码开发任务，必须满足：

1. 任务只允许改 1 到 3 个文件。
2. 必须给出精确文件路径和方法名。
3. 必须给出停止条件。
4. 必须给出验证命令。
5. 不允许做目录盘点、计划修订、全局架构判断。
6. 不允许使用 PowerShell `Get-Content` / `Set-Content` 写中文文件。
7. 不允许提交 `rysqlite3.db`、`.codegraph/`。
8. 不允许新增 README 或纯文档任务，除非 Codex 明确指定。

固定验证：

```powershell
mvn "-DskipTests" compile
mvn "-DskipTests=false" "-Dmaven.test.skip=false" "-Dtest=BatteryCollectorServiceTest,BatteryCollectorCommandLogServiceTest" test
git diff --check
```
