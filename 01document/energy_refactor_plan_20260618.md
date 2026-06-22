# energy 项目重构计划（给执行 AI 的明确任务版）

更新时间：2026-06-18

本文只规划重构任务，不要求立即改代码。后续执行者必须按任务卡逐项实施、逐项验证、逐项提交；禁止一次性大范围改包名、改目录、改业务语义。

## 1. 重构目标

1. 梳理 `03code/energy` 文件目录，按业务和功能边界调整目录结构。
2. 提取公共能力和工具，减少多方参与造成的重复类、职责混杂和大文件。
3. 聚焦蓄电池采集主流程、实时快照、后处理、外部读取、命令控制等核心链路。
4. 将 `BatteryCollectorService` 等超大类拆为稳定的小职责服务，但保持对外行为不变。
5. 给能力较弱的 AI 提供无歧义任务：每个任务说明允许修改文件、禁止修改文件、步骤、验证命令、停止条件。

## 2. 非目标

1. 本轮不重写业务算法，不改变协议语义，不调整数据库表结构。
2. 本轮不恢复旧 M460 980 节运行链路；980 只作为兼容语义参考。
3. 本轮不把 `iot/battery`、`iot/CM03N` 旧 JSON/TCP 链路改造成新主链路。
4. 本轮不移动 MyBatis XML、SQL 脚本、前端文件，除非任务卡明确要求。
5. 本轮不为每个方法机械创建单元测试；仅核心和高风险行为补针对性测试。
6. 本轮不提交 `03code/energy/sql/rysqlite3.db` 和 `.codegraph/`。

## 3. 当前目录诊断

### 3.1 顶层业务包

当前主要包如下：

```text
com.shanhe.project.collector   采集能力，当前蓄电池新链路在这里
com.shanhe.project.common      通用基础能力
com.shanhe.project.device      设备配置、告警、操作、屏幕等业务
com.shanhe.project.energy      能源统计和容量相关业务
com.shanhe.project.iot         旧 JSON/TCP/CM03N 兼容入口
com.shanhe.project.modbus      Modbus RTU 和映射服务
com.shanhe.project.monitor     系统监控
com.shanhe.project.scheduled   定时任务
com.shanhe.project.sync        同步能力
com.shanhe.project.system      系统用户、文件、字典等
```

问题：

1. `collector/battery/service` 内职责过宽，轮询、命令、实时、快照、状态、日志、兼容输出、Modbus 读取混在一个包。
2. `BatteryCollectorService` 仍是采集主流程核心大类，后续功能继续堆入会导致维护风险上升。
3. `iot/battery` 和新 `collector/battery` 边界容易混淆，旧兼容链路不能继续承载新能力。
4. `modbus/service` 与 `collector/battery/service/BatteryModuleModbusReadMappingService` 存在跨包职责，需要明确所有权。
5. 部分公共能力散落在业务服务里，例如缓存 key、快照读取、协议日志、命令日志、状态去重等。

### 3.2 蓄电池采集包现状

当前目录：

```text
collector/battery/config
collector/battery/controller
collector/battery/mapper
collector/battery/model
collector/battery/protocol
collector/battery/service
collector/battery/service/impl
collector/battery/postprocess
```

建议目标目录：

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

执行原则：

1. 目标目录是终态方向，不要求一次性建满。
2. 每个提交最多移动一个职责组。
3. 移动类时先保证包名和 imports 编译通过，不改方法行为。
4. `service/postprocess` 已迁移到 `postprocess`；后续不得再新增 `service/postprocess` 主代码。测试文件是否跟随迁移按 Q9 执行。
5. 对外注入 Bean 名称不要主动变化；如必须变化，先全文搜索调用点并补兼容。
6. 已被 `device`、`modbus`、`sync`、controller 或大量测试直接引用的门面类，第一轮不要改 package；优先在新目录新增内部实现类，并让旧门面委托。
7. 模型类第一轮默认留在 `collector/battery/model`；只有当模型只服务单一子域且引用面很小，才允许在独立任务中迁移。

## 4. 总执行规则

所有执行 AI 必须遵守：

1. 开始前执行 `git status --short`，确认是否存在用户未提交改动。
2. 不得回滚不是本任务产生的文件改动。
3. 每个任务只做任务卡允许的文件和目录。
4. 每个任务完成后必须运行 `git diff --check`。
5. Java 代码变更至少运行 `mvn -DskipTests compile`；高风险任务运行任务卡指定测试。
6. 不提交 `03code/energy/sql/rysqlite3.db`。
7. 不提交 `.codegraph/`。
8. 不移动 mapper XML、resources、SQL，除非任务卡明确写出。
9. 不把旧 `iot/battery` 或 `iot/CM03N` 作为新功能落点。
10. 不用“顺手优化”扩大范围；发现额外问题只新增任务记录。
11. 测试遵循风险分级：协议映射、快照新鲜度、命令状态、告警、容量、状态持久化必须有针对性测试；低风险字段搬运、目录移动、简单转发可用编译和现有测试验证。
12. 任务内新增类和核心公开方法应补中文注释；不要给显而易见的 getter/setter 或简单私有方法堆注释。
13. 执行移动类任务前必须用 `rg` 或 `.codegraph` 检查影响面；若影响超过 20 个业务文件，改为“保留原门面，抽内部实现”，不要硬改包名。
14. `@Service`、`@Component` 等 Spring Bean 的类名和注入类型是兼容边界；迁移时优先保持原注入类型可用。

## 5. 重构阶段

### 5.0 启动门槛

执行重构前必须满足：

1. 当前正在开发的功能任务已经完成或明确暂停。
2. `git status --short` 中没有无法解释的 Java 代码改动。
3. 若存在文档改动，可以继续；若存在运行态数据库改动，必须确认不会提交。
4. 当前任务不依赖现场硬件联调结果。
5. 对蓄电池主链路的功能修复优先级高于目录迁移；发现功能缺陷时停止重构，新增功能修复任务。

### 阶段 A：目录边界冻结

目标：先形成目录和职责共识，不移动大量代码。

输出：

1. 包职责说明。
2. 禁止新增业务的旧包清单。
3. 后续文件移动任务的顺序。

### 阶段 B：采集主流程拆分

目标：优先拆 `BatteryCollectorService`，但每次只抽一个稳定职责。

顺序：

1. 串口发送、接收、帧处理协调。
2. 轮询循环和通道调度。
3. 命令队列执行。
4. 超时和 pending 请求处理。
5. 死代码和重复字段清理。

### 阶段 C：实时和后处理边界稳定

目标：让实时入库、快照、外部读取、后处理职责清楚。

顺序：

1. 实时消费和快照归入 `realtime`。
2. 后处理保持独立 `postprocess`。
3. JSON/TCP、Modbus、页面读取只经标准实时模型和快照。

### 阶段 D：外部读取和兼容边界

目标：旧 JSON/TCP 只做兼容入口，新能力在 collector 标准模型落地。

顺序：

1. 标记旧 `iot` 链路边界。
2. 抽外部读取适配。
3. 逐项迁移 JSON/TCP 字段来源。

### 阶段 E：项目级目录清理

目标：在蓄电池核心链路稳定后，再整理项目级公共能力和目录。

顺序：

1. 公共缓存 key、日期、数值换算、协议字节工具。
2. 定时任务按业务归属整理。
3. 超大工具类拆分。
4. 过度拆分的薄服务收缩回唯一调用者，减少无意义 Bean 和文件数量。
5. 扩展到整个 `energy` 项目目录梳理，但必须先冻结模块边界和迁移清单。

### 阶段 F：energy 项目级目录重构

目标：在蓄电池主链路和外部读写边界稳定后，梳理整个 `03code/energy` 项目的业务目录，降低历史多人协作造成的目录混乱。

原则：

1. 先文档盘点，再小步迁移；不得直接大规模改 package。
2. 优先按业务域划分：collector、device、energy/stat、energy/capacity、iot 兼容、sync、scheduled、framework。
3. 对 controller、mapper、service、domain/model 的既有分层保持兼容；跨域高引用门面第一轮不迁移。
4. 目录整理不得改变 Spring Bean 名称、URL、Mapper namespace、SQL、缓存 key、定时任务表达式。
5. 每次只迁一个职责组；如果只是为了目录整齐且引用面大，保留原包并补 README。
6. 发现薄服务/薄 adapter 只有唯一调用者时，优先评估收缩，而不是继续新增目录层级。

## 6. 任务卡

### TASK-REF-DIR-001：建立目录职责说明

优先级：P0

目标：新增目录职责文档，不移动代码。

允许修改：

```text
01document/energy_refactor_plan_20260618.md
03code/energy/src/main/java/com/shanhe/project/collector/battery/README.md（如不存在可新增）
```

禁止修改：

```text
03code/energy/src/main/java/**/*.java
03code/energy/src/main/resources/**
03code/energy/sql/rysqlite3.db
```

步骤：

1. 在 `collector/battery/README.md` 写清当前包职责和目标包职责。
2. 明确 `iot/battery`、`iot/CM03N` 是旧兼容入口。
3. 明确新采集、新后处理、新外部读取必须进入 `collector/battery`。
4. 不移动 Java 文件。

验证：

```bash
git diff --check
```

提交建议：

```text
docs: document battery collector package ownership
```

停止条件：

1. 发现 README 与本文目标目录冲突，先修本文。
2. 发现执行者想移动 Java 文件，停止任务并拆到后续任务。

### TASK-REF-COLLECTOR-001：抽取轮询循环服务

优先级：P1

目标：从 `BatteryCollectorService` 抽出通道轮询循环协调类，行为不变。

前置条件：优先完成 `TASK-REF-COLLECTOR-002`，至少先明确帧 I/O 的最小接口边界；若尚未完成，只允许抽循环编排，不允许同时抽帧 I/O。

建议新增目录：

```text
collector/battery/runtime
```

建议新增类：

```text
BatteryCollectorPollingService
```

允许修改：

```text
BatteryCollectorService.java
BatteryCollectorPollingService.java
BatteryCollectorServiceTest.java
BatteryCollectorPollingServiceTest.java（仅当核心行为需要）
```

禁止修改：

```text
BatteryModuleRealtimeConsumer.java
BatteryModuleRealtimeSnapshotService.java
BatteryCollectorCommandService.java
mapper XML
SQL
旧 iot 包
```

步骤：

1. 在 `runtime` 目录新增 `BatteryCollectorPollingService`。
2. 只迁移轮询循环相关方法，不迁移命令队列、不迁移快照、不迁移后处理。
3. `BatteryCollectorService` 保留原 public 方法，内部委托新服务。
4. 保持日志文案、异常处理、休眠间隔、重试逻辑不变。
5. 若方法参数过多，允许新增一个包内 request/context 对象，但不得改变字段含义。
6. 给新类和核心 public 方法补中文注释。
7. 暂不迁移 `BatteryCollectorService` 的包名；它是 controller、device、sync 等链路的稳定门面。
8. 超时判断和超时落状态的具体实现第一轮不迁移，仍留在 `BatteryCollectorService`；后续可由 `COMMAND-001` 迁移命令 pending 超时，或新增 `TASK-REF-COLLECTOR-003-TIMEOUT` 专项处理。

验证：

```bash
mvn -DskipTests compile
mvn -Dtest=BatteryCollectorServiceTest test
git diff --check
```

提交建议：

```text
refactor: extract battery collector polling service
```

停止条件：

1. 需要改协议解析时停止，另建任务。
2. 需要改数据库字段时停止，另建任务。
3. 单个 diff 超过约 800 行时停止并继续拆分。

### TASK-REF-COLLECTOR-002：抽取串口帧收发协调

优先级：P1

目标：将串口发送、接收缓冲、帧边界处理的协调逻辑从主服务分离。

执行定位：这是采集主流程拆分的首个代码任务，先抽最小帧 I/O 能力，为后续轮询编排和命令队列复用提供稳定接口。

建议目录：

```text
collector/battery/runtime
```

建议新增类：

```text
BatteryCollectorFrameIoService
```

允许修改：

```text
BatteryCollectorService.java
BatteryCollectorFrameIoService.java
BatteryCollectorProtocolLogService.java（仅调用点需要）
BatteryCollectorServiceTest.java
```

禁止修改：

```text
BatteryCollectorFrameCodec.java 的协议语义
BatteryDeviceProtocolCode.java
BatteryModuleFrameDispatcher.java 的消费语义
旧 iot 包
```

步骤：

1. 新类只负责协调 I/O，不负责解释业务数据。
2. 协议解析仍交给 `BatteryCollectorFrameCodec`、`BatteryModuleFrameDispatcher` 等已有类。
3. 原有日志调用保持原触发点。
4. 原有超时处理不在本任务迁移，除非只是为了编译传参。
5. 给新类和核心 public 方法补中文注释。

验证：

```bash
mvn -DskipTests compile
mvn -Dtest=BatteryCollectorServiceTest,BatteryCollectorProtocolLogServiceTest test
git diff --check
```

提交建议：

```text
refactor: extract battery collector frame io coordinator
```

停止条件：

1. 若需要改帧校验算法，停止。
2. 若发现串口库调用无法无行为迁移，停止并记录风险。

### TASK-REF-COMMAND-001：抽取命令队列执行服务

优先级：P1

目标：把 `BatteryCollectorService` 中模块命令队列执行、pending 请求登记、响应完成协调拆出。

建议新增目录：

```text
collector/battery/command
```

建议迁移或新增类：

```text
BatteryCollectorCommandQueueService
BatteryCollectorCommandLogService（保留原包名，后续单独评估）
BatteryCollectorCommandService（保留原包名，作为外部门面）
BatteryModuleControlCommandService（保留原包名，后续单独评估）
```

允许修改：

```text
BatteryCollectorService.java
BatteryCollectorCommandQueueService.java
BatteryCollectorCommandLogService.java
BatteryCollectorCommandService.java（仅必要 imports 或委托调整，不迁移包名）
BatteryModuleControlCommandService.java（仅必要 imports 或委托调整，不迁移包名）
相关 command 测试
```

禁止修改：

```text
BatteryDeviceProtocolCode.java 的命令码含义
ControlBatterySet.java 的业务语义
ModbusWriteMappingService.java 的业务语义
旧 iot 包
```

步骤：

1. 先新增 `BatteryCollectorCommandQueueService`，不要立刻移动所有 command 类。
2. 迁移队列出队、发送、pending 创建、响应匹配、命令完成回调。
3. `BatteryCollectorService` 仍作为 facade 暴露原 public 方法。
4. 保持 opt-log 创建和更新调用结果完全一致。
5. 保持无响应命令和有响应命令的原路径。
6. 给新类和核心 public 方法补中文注释。
7. `BatteryCollectorCommandService` 已被 controller、device、modbus、sync 多处引用（3 外部：`ControlBatterySet`、`ModbusWriteMappingService`、`BatterySyncHandler`），本任务不得改它的 package。
8. `BatteryCollectorCommandQueueService` 在关键节点调用 `BatteryCollectorCommandLogService`，保持命令状态和日志更新内聚；第一小步可暂留日志调用在 `BatteryCollectorService`，但必须在任务结果里写明后续迁移点。不把日志逻辑重新塞回 `BatteryCollectorService`。

验证：

```bash
mvn -DskipTests compile
mvn -Dtest=BatteryCollectorServiceTest,BatteryCollectorCommandServiceTest,BatteryModuleControlCommandServiceTest,BatteryCollectorCommandLogServiceTest test
git diff --check
```

提交建议：

```text
refactor: extract battery command queue execution
```

停止条件：

1. 命令状态码变化，停止。
2. opt-log 字段变化，停止。
3. 页面或 Modbus 写控制调用路径需要大改，停止并拆新任务。

### TASK-REF-COMMAND-001B：补齐命令队列执行抽取

优先级：P1

背景：`TASK-REF-COMMAND-001` 已创建 `BatteryCollectorCommandQueueService`，并抽出了 pending 构造、响应匹配和成功判断，但队列出队、发送协调、命令完成回调、超时完成和日志更新仍主要留在 `BatteryCollectorService`。因此 `COMMAND-001` 只能视为 partial，不能直接进入 `COMMAND-002` 包目录整理。

目标：在不改变命令行为的前提下，把命令队列执行协调继续从 `BatteryCollectorService` 移入 `BatteryCollectorCommandQueueService` 或其内部协作类。

允许修改：

```text
BatteryCollectorService.java
BatteryCollectorCommandQueueService.java
BatteryCollectorCommandLogService.java（仅调用点需要）
BatteryCollectorServiceTest.java
BatteryCollectorCommandLogServiceTest.java（仅调用点需要）
```

禁止修改：

```text
BatteryDeviceProtocolCode.java
BatteryAggregateCommandDefinition.java
BatteryModuleControlCommandService.java 的命令 payload 规则
ControlBatterySet.java
ModbusWriteMappingService.java
旧 iot 包
mapper XML
SQL
```

步骤：

1. 先用 `.codegraph` 或 `rg` 确认 `processQueuedModuleCommand`、`processQueuedModuleCommandsImmediately`、`handlePendingResponse`、`checkTimeout`、`markModeStopped` 的调用面。
2. 将命令出队、pending 创建、发送前状态切换、发送失败处理移入命令队列服务；帧实际写串口仍调用 `BatteryCollectorFrameIoService` 或由 `BatteryCollectorService` 以回调传入。
3. 将响应成功/失败完成协调移入命令队列服务；具体业务副作用如自动编号推进、连接条读电压推进可以先通过回调保留，避免一次性搬太多。
4. 将命令超时完成协调移入命令队列服务；轮询超时和通道状态持久化仍留在原位置或 `BatteryCollectorDeviceStateService`。
5. `BatteryCollectorCommandLogService` 继续负责 opt-log 字段构造和持久化；不要在 QueueService 中重新拼 opt-log 字段。
6. `BatteryCollectorService` 保留 public API，只作为门面和运行线程持有者。
7. 给新增 public 方法补中文注释。

验证：

```bash
mvn -DskipTests compile
mvn -Dtest=BatteryCollectorServiceTest,BatteryCollectorCommandLogServiceTest,BatteryCollectorCommandServiceTest,BatteryModuleControlCommandServiceTest test
git diff --check
```

提交建议：

```text
refactor: complete battery command queue extraction
```

停止条件：

1. 命令状态码、错误文案、opt-log 字段发生变化，停止。
2. 自动编号或连接条流程需要大改，停止并拆功能任务。
3. 单次 diff 超过约 700 行，停止并拆成 `001B-1/001B-2`。

**COMMAND-001 当前执行结果审查（2026-06-18）：**

状态：partial。

已完成：

1. 新增 `collector/battery/command/BatteryCollectorCommandQueueService`。
2. 抽出控制命令到 `BatteryPendingRequest` 的转换。
3. 抽出 pending 响应匹配和成功响应判断。
4. 已补充抽出命令完成快照、模式停止、显式命令超时收尾，超时日志更新由 `BatteryCollectorCommandQueueService` 调用 `BatteryCollectorCommandLogService`。
5. 已补充抽出命令出队与发送协调入口、无响应命令发送收尾、普通显式响应完成与日志更新入口。
6. 已删除 `BatteryCollectorService` 中重复的设备状态持久化实现，统一使用 `collector.battery.state.BatteryCollectorDeviceStateService`。
7. 已删除 `BatteryCollectorService` 中旧的轮询实现和地址缓存辅助方法，轮询编排统一由 `BatteryCollectorPollingService` 承担。
8. 已将自动编号协议推进迁入 `BatteryCollectorCommandQueueService`，主服务仅保留模式运行和地址缓存重置回调。
9. 已新增 `BatteryConnectResistanceCommandProcessor` 承担连接条电阻 0F/11/91 排队、解析、计算、最终日志和模式收尾。

未完成：

1. 帧实际写串口仍留在 `BatteryCollectorService`，这是有意保留的运行态边界。
2. `BatteryCollectorService` 仍保留串口收发门面、超时重试入口和响应分派入口，后续按功能分批评估。
3. 测试文件仍需后续适配，尤其是原来直接访问 `BatteryCollectorService` 包私有方法的用例。

后续优先继续观察 `TASK-REF-COMMAND-001B` 的剩余流程分支，确认自动编号/连接条是否还需要进一步拆分，再评估 `TASK-REF-COMMAND-002`。

### TASK-REF-COMMAND-002：整理命令包目录

优先级：P2

目标：在命令队列服务稳定后，评估命令相关类是否可以迁移到 `collector/battery/command`；默认保留外部门面包名。

允许评估或移动：

```text
BatteryCollectorCommandQueueService.java（可直接放 command 包）
BatteryCollectorCommandLogService.java（默认保留原包名；只有收益明确时才迁移）
BatteryModuleControlCommandService.java（默认保留原包名；优先继续作为命令构造 helper）
BatteryCollectorCommandResult.java（默认不移动）
BatteryModuleControlCommand.java（默认不移动）
BatteryPendingRequest.java（默认不移动）
BatteryAggregateCommandDefinition.java（可留在 protocol；若移动需说明原因）
```

引用面参考（rg 扫描 2026-06-18）：

```text
BatteryModuleControlCommandService → 外部引用少，但与 BatteryCollectorCommandService 命令映射强耦合；第一轮不为目录整齐迁移
BatteryCollectorCommandLogService → 外部引用少，但已是独立职责服务；第一轮不为目录整齐迁移
```

禁止修改：

```text
命令码枚举含义
命令 payload 构造规则
数据库日志字段
```

步骤：

1. 先用 `rg "BatteryCollectorCommandService|BatteryCollectorCommandResult|BatteryModuleControlCommand|BatteryPendingRequest"` 检查引用面。
2. 影响超过 20 个业务文件的类不移动，只在文档记录“保留门面”。
3. 一次提交最多移动 2 个低引用内部类；若只是为了目录整齐，不移动。
4. 仅修改 package 和 imports。
5. 编译失败再补调用点 imports，不改业务。
6. 如 `BatteryAggregateCommandDefinition` 与协议强相关，优先留在 `protocol`。

验证：

```bash
mvn -DskipTests compile
mvn -Dtest=BatteryCollectorCommandServiceTest,BatteryModuleControlCommandServiceTest,ModbusWriteMappingServiceTest,ControlBatterySetTest test
git diff --check
```

提交建议：

```text
refactor: move battery command classes to command package
```

停止条件：

1. 出现行为 diff，停止。
2. 一次移动导致超过 20 个业务文件修改，停止并缩小范围。
3. 需要移动 `BatteryCollectorCommandService` 或 `BatteryCollectorCommandResult` 时，停止并改为保留门面。

### TASK-REF-REALTIME-001：整理实时消费与快照目录

优先级：P1

目标：把实时入库、实时快照、实时视图、兼容填充归到清晰目录，避免继续堆在 `service`。

建议新增目录：

```text
collector/battery/realtime
```

建议迁移（按引用面分批）：

```text
第一轮可评估（引用面较小，但仍优先保留门面）：
  BatteryModuleRealtimeConsumer（仅 1 个主流程调用点，但与入库、快照、后处理时序强相关）
  BatteryModuleGroupCalculationService
  BatteryModuleGroupCompatibilityFillService
  BatteryModuleCellCompatibilityFillService

第一轮保留原包名（引用面大，作为稳定门面）：
  BatteryModuleRealtimeSnapshotService（10 引用，2 外部：BatteryPackServiceImpl、RestoreServiceImpl）
  BatteryModuleRealtimeSnapshot（跟随 SnapshotService 不动）

待评估：
  BatteryModuleRealtimeAdapterService
  BatteryModuleReportLogAdapterService
  BatteryCurrentStateService
  BatteryModuleCompatReportLogSyncService（已由 CONSOLIDATE-001 收缩到 CompatReportLogSyncProcessor）

暂不跟本任务：
  BatteryRealtimePostProcessContextFactory（更接近 realtime 消费侧组装逻辑，可在本任务评估但非必须）
```

引用面参考（rg 扫描 2026-06-18）：

```text
BatteryModuleRealtimeSnapshotService → 11 引用，2 外部（BatteryPackServiceImpl、RestoreServiceImpl）
BatteryModuleRealtimeConsumer → 2 引用，0 外部（仅 BatteryCollectorService）
BatteryModuleCompatReportLogSyncService → 已收缩到 CompatReportLogSyncProcessor
```

允许修改：

```text
上述类
上述类对应测试
调用这些类的 imports
```

禁止修改：

```text
实时表 mapper SQL
快照新鲜度规则
batSinSize 限制规则
后处理 processor 业务算法
```

步骤：

1. 先用 `rg` 或 `.codegraph` 检查 `BatteryModuleRealtimeSnapshotService`、`BatteryModuleRealtimeSnapshot`、`BatteryModuleRealtimeConsumer` 的引用面。
2. `BatteryModuleRealtimeSnapshotService` 已被页面、Modbus、device、collector 多处引用，第一轮不得改 package；如需整理，新增内部 helper 或保持旧门面委托。
3. 第一个提交只抽低引用内部 helper，或只补包说明，不迁移高引用门面；`BatteryModuleRealtimeConsumer` 即使引用面小，也必须确认不会同时改变入库、快照、后处理提交时序。
4. 第二个提交再评估 `BatteryModuleRealtimeConsumer` 和上下文工厂是否可迁移。
5. 第三个提交迁移兼容填充和 report-log adapter，但每次最多移动 2 个类。
6. 每个提交只改 package/imports 和必要注释。
7. 如发现类职责不清，只在文档记录，不在迁移提交里改逻辑。

验证：

```bash
mvn -DskipTests compile
mvn -Dtest=BatteryModuleRealtimeSnapshotServiceTest,BatteryModuleRealtimeConsumerTest,BatteryRealtimePostProcessContextFactoryTest,BatteryModuleGroupCalculationServiceTest test
git diff --check
```

提交建议：

```text
refactor: move battery realtime services to realtime package
```

停止条件：

1. 需要改快照合并逻辑，停止。
2. 需要改 mapper 查询，停止。
3. JSON/TCP 或 Modbus 输出变化，停止。
4. 需要移动 `BatteryModuleRealtimeSnapshotService` 或快照模型时，停止并改为保留门面。

**REALTIME-001 当前执行结果审查（2026-06-18）：**

状态：partial。

已完成：

1. 新增 `collector/battery/realtime/README.md`，记录 realtime 包职责。
2. `BatteryModuleGroupCalculationService` 已迁入 `collector/battery/realtime`。
3. `BatteryModuleGroupCompatibilityFillService` 已迁入 `collector/battery/realtime`。
4. 旧 `collector/battery/service` 下同名类已删除，避免 Spring 默认 bean name 冲突。

未完成：

1. `BatteryModuleRealtimeConsumer` 仍保留在 `service` 包，符合“先确认入库、快照、后处理时序”的限制。
2. `BatteryModuleRealtimeSnapshotService` 和 `BatteryModuleRealtimeSnapshot` 仍保留原位置，符合高引用门面保留规则。
3. `BatteryModuleCellCompatibilityFillService`、`BatteryModuleRealtimeAdapterService`、`BatteryModuleReportLogAdapterService`、`BatteryCurrentStateService` 仍待后续按引用面逐项评估；`BatteryModuleCompatReportLogSyncService` 已由 `CONSOLIDATE-001` 收缩，不再作为迁移候选。

后续不得重复迁移已完成的两个 group 服务；如继续执行本任务，应优先评估 `BatteryModuleCellCompatibilityFillService` 或仅补包说明，不要一次性移动高引用门面。

### TASK-REF-POSTPROCESS-001：后处理包边界整理

优先级：P2

目标：明确后处理只消费标准实时上下文，不反向控制采集轮询。

建议目录：

```text
collector/battery/postprocess
```

允许修改：

```text
collector/battery/postprocess/**
BatteryRealtimePostProcessContextFactory.java（仅 imports）
BatteryModuleRealtimeConsumer.java（仅 imports）
```

禁止修改：

```text
SOC/SOH/容量算法
告警规则
兼容 report-log 写入规则
轮询线程逻辑
```

步骤：

1. 目标包统一为 `collector/battery/postprocess`；当前主代码已迁入该包。
2. 第一小步补 `collector/battery/postprocess/README.md`，说明后处理只消费 `BatteryRealtimePostProcessContext`，不得反向访问串口、命令队列、轮询状态。
3. 第二小步迁移纯上下文/工具类：`BatteryRealtimePostProcessContext`、`PostProcessBatchGuard`、`RealtimeToReportLogAdapter`；只改 package/imports，不改字段和方法。
4. 第三小步开始迁移具体 processor，每次最多 2-3 个，例如先迁 `VoltageRangeProcessor`、`OnlineStatusProcessor`、`StatisticsProcessor` 这类低外部副作用处理器。
5. 告警、兼容 report-log、容量预测、内阻统计这类有外部服务依赖的 processor 单独分批迁移，迁移时只改 package/imports 和乱码注释/日志，不改业务逻辑。
6. `BatteryRealtimePostProcessContextFactory` 暂不跟本任务迁移；它更接近 realtime 消费侧的组装逻辑，可在 `REALTIME-001` 中评估。
7. 迁移测试文件时遵守 Q9：需要访问包私有成员才跟随迁移，否则只改 imports。

验证：

```bash
mvn -DskipTests compile
mvn -Dtest=BatteryRealtimePostProcessorsTest,AlarmContextProcessorTest test
git diff --check
```

提交建议：

```text
refactor: clarify battery postprocess package boundary
```

停止条件：

1. 发现测试类不存在时，不强行新增全量测试；改用已有相关测试和 compile。
2. 发现 processor 需要行为修复时，另建功能任务。
3. 若后续已新增 `CapacityPredictionProcessorTest`，可以把它加入验证命令；当前仓库未要求单独新增该测试。

**POSTPROCESS-001 当前执行结果审查（2026-06-18）：**

状态：completed。

已完成：

1. `BatteryRealtimePostProcessor` 已在 `collector/battery/postprocess`。
2. `BatteryRealtimePostProcessService` 已在 `collector/battery/postprocess`。
3. `BatteryRealtimePostProcessContext` 已在 `collector/battery/postprocess`。
4. `PostProcessBatchGuard`、`RealtimeToReportLogAdapter` 已在 `collector/battery/postprocess`。
5. 具体 processor 已迁入 `collector/battery/postprocess`，包括告警、统计、在线状态、操作日志、容量预测、内阻统计和兼容历史同步处理器。
6. 已新增 `collector/battery/postprocess/README.md`。

保留说明：

1. `BatteryRealtimePostProcessContextFactory` 仍在 `service` 包，按计划作为 realtime 消费侧上下文组装桥接类保留。
2. `BatteryRealtimePostProcessorsTest` 仍在 `service/postprocess` 测试包，按 Q9 不为目录整齐强制迁移测试；它已通过 imports 覆盖 `postprocess` 主代码。

结论：`battery.service.postprocess` 下的主代码后处理能力已迁入 `battery.postprocess`。后续不得重复执行迁包任务；如需处理测试目录，只允许单独做低风险测试包名整理，不改业务逻辑。

### TASK-REF-STATE-001：整理状态服务目录

优先级：P2

目标：明确设备状态、模式状态、状态常量、状态持久化的归属；第一轮优先保留高引用门面，必要时抽内部 helper。

建议处理：

```text
BatteryCollectorDeviceStateService（可评估抽 helper 或迁移；引用面较小）
BatteryDeviceStateService（保留原包名；影响告警、定时任务、Modbus、页面状态和恢复逻辑）
BatteryDeviceStateServiceImpl（跟随接口保留原包名；必要时只抽内部 helper）
BatteryModeStatusService（保留原包名；影响 command、恢复逻辑和旧 iot 兼容回包）
BatteryDeviceState（默认留在 model；除非单独任务确认引用面）
BatteryDeviceStateConstants（默认留在 model；避免常量 imports 大范围震荡）
```

不迁移（Q6 答复）：

```text
BatteryCollectorChannelState — 留在 collector/battery/model，它是运行态模型不是持久化设备状态模型
```

跨模块依赖风险提示：

```text
BatteryDeviceStateService 经 codegraph 检查影响面约 84 个符号，涉及告警、定时任务、Modbus、页面状态和恢复逻辑；第一轮不得改 package。
BatteryModeStatusService 影响 command、恢复逻辑和旧 iot 兼容回包；第一轮不得改 package。
BatteryCollectorChannelState 大量测试直接构造，不迁移。
```

允许修改：

```text
上述类
上述类对应测试
调用 imports（第一轮仅限低引用 helper 或包说明；不得批量更新外部 imports）
```

禁止修改：

```text
battery_device_state 表结构
状态码含义
状态去重 key 语义
```

步骤：

1. 第一提交只补包说明或抽 `BatteryCollectorDeviceStateService` 内部 helper，不迁移高引用接口。
2. 保持 mapper 包不动。
3. 保持状态持久化字段不变。
4. 给包说明写清状态来源：采集通道、模块响应、模式命令、组 246 新鲜度。
5. 不迁移 `BatteryDeviceStateService`、`BatteryDeviceStateServiceImpl`、`BatteryModeStatusService` 的 package；若后续必须迁移，先单独建任务并列出所有 imports。
6. 不迁移旧 iot 兼容链路依赖的状态服务，避免兼容回包行为变化。

验证：

```bash
mvn -DskipTests compile
mvn -Dtest=BatteryCollectorDeviceStateServiceTest,BatteryModeStatusServiceTest test
git diff --check
```

提交建议：

```text
refactor: move battery state services to state package
```

停止条件：

1. 任何状态码变化，停止。
2. 任何数据库字段变化，停止。
3. 若后续已新增 `BatteryDeviceStateServiceTest`，可以把它加入验证命令；当前仓库未要求单独新增该测试。
4. 需要迁移 `BatteryDeviceStateService` 或 `BatteryModeStatusService` 包名时，停止并改为单独评估任务。

**STATE-001 当前执行结果审查（2026-06-18）：**

状态：partial。

已完成：

1. `BatteryCollectorDeviceStateService` 已迁入 `collector/battery/state`。
2. 旧 `collector/battery/service` 下同名类已删除，避免 Spring 默认 bean name 冲突。

未完成：

1. `BatteryDeviceStateService`、`BatteryDeviceStateServiceImpl`、`BatteryModeStatusService` 仍保留原包名，符合高引用门面保留规则。
2. `BatteryDeviceState`、`BatteryDeviceStateConstants` 仍保留在 `model` 包，符合减少常量 imports 震荡的限制。

后续执行 `STATE-001` 时不得重复迁移 `BatteryCollectorDeviceStateService`；只能补包说明、内部 helper，或在单独任务中评估高引用状态门面。

### TASK-REF-EXTERNAL-001：整理外部读取适配边界

优先级：P2

目标：明确页面、JSON/TCP、Modbus 读取来源都应走标准实时模型和快照。

建议新增目录：

```text
collector/battery/external
collector/battery/external/modbus
collector/battery/external/jsontcp
collector/battery/external/page
```

首批建议迁移：

```text
BatteryModuleModbusReadMappingService -> 保留原包名，新增 external/modbus 内部解析/分组 helper
BatteryCurrentStateService -> external/page 或 realtime，根据职责二选一
```

允许修改：

```text
BatteryModuleModbusReadMappingService.java
BatteryCurrentStateService.java
ModbusRtuServer.java（仅 imports）
ModbusWriteMappingService.java（仅 imports）
相关测试
```

禁止修改：

```text
Modbus 寄存器地址
首次无数据异常语义
已有数据后缺字段填 0 语义
快照新鲜度规则
旧 JSON/TCP 上报行为
```

步骤：

1. 不迁移 `BatteryModuleModbusReadMappingService` 包名；它已被 RTU 服务和测试直接引用，先保留为稳定门面。
2. 如文件继续膨胀，可在 `external/modbus` 新增内部 helper，例如寄存器分组解析、状态寄存器读取、单体寄存器读取。
3. 确保高频读取仍优先使用快照服务。
4. 不在本任务新增 JSON/TCP 字段切换。
5. 文档写清外部读取只能依赖标准实时模型、快照、状态服务，不依赖 `dev_battery_report_log` 作为主来源。

验证：

```bash
mvn -DskipTests compile
mvn -Dtest=BatteryModuleModbusReadMappingServiceTest,ModbusRtuServerTest,ModbusWriteMappingServiceTest test
git diff --check
```

提交建议：

```text
refactor: move battery external read adapters
```

停止条件：

1. 寄存器输出变化，停止。
2. 高频读取重新访问 mapper，停止。
3. 需要移动 `BatteryModuleModbusReadMappingService` 包名时，停止并改为抽 helper。

### TASK-REF-LEGACY-001：标记旧兼容入口

优先级：P2

目标：降低后续执行者误把旧 `iot` 链路当主链路的概率。

允许修改：

```text
iot/battery/** 的类注释
iot/CM03N/BatteryHandler.java 的类注释
01document/energy_refactor_plan_20260618.md
```

禁止修改：

```text
旧 JSON/TCP 业务逻辑
旧 handler 方法签名
数据库写入行为
```

步骤：

1. 只加类级中文注释或包说明。
2. 注释写清：旧链路只做兼容入口，不承载新采集和新算法。
3. 不修改方法体。

验证：

```bash
mvn -DskipTests compile
git diff --check
```

提交建议：

```text
docs: mark legacy battery iot handlers
```

停止条件：

1. 需要改逻辑时停止。
2. 编码出现乱码时停止，先确认文件编码。

### TASK-REF-SCHEDULED-001：整理定时任务归属

优先级：P3

目标：把蓄电池相关定时任务归属写清，后续再决定是否移动目录。

允许修改：

```text
scheduled/** 中蓄电池相关类注释
01document/energy_refactor_plan_20260618.md
```

禁止修改：

```text
定时表达式
任务启停条件
业务逻辑
```

步骤：

1. 搜索 `battery`、`Battery`、`蓄电池` 相关定时任务。
2. 记录每个任务调用的新链路或旧链路。
3. 只补文档，不移动类。
4. 如确需移动，新增 `TASK-REF-SCHEDULED-002`。

验证：

```bash
git diff --check
```

提交建议：

```text
docs: document battery scheduled job ownership
```

停止条件：

1. 找不到调用链时不要猜，写成待核验项。
2. 不得修改 cron。

### TASK-REF-COMMON-001：公共工具候选清单

优先级：P3

目标：梳理可提取公共能力，不直接拆工具类。

候选：

```text
缓存 key 构造
数值缩放和单位换算
协议字节数组读写
日期格式化
批量列表安全处理
日志摘要格式化
```

允许修改：

```text
01document/energy_refactor_plan_20260618.md
```

禁止修改：

```text
ExcelUtil.java
CodingUtil.java
业务服务 Java 文件
```

步骤：

1. 搜索重复工具逻辑。
2. 写清候选位置、调用方、风险。
3. 给每个候选新增后续任务，不立即实现。

验证：

```bash
git diff --check
```

提交建议：

```text
docs: list common utility extraction candidates
```

停止条件：

1. 不允许在本任务直接拆工具类。
2. 不允许修改公共工具 API。

**COMMON-001 执行结果（2026-06-18）：**

公共工具候选清单（rg 扫描）：

| 候选 | 文件数 | 集中度 | 建议 |
|---|---|---|---|
| CacheUtils / CacheKeyEnum | 29 / 27 | 已集中 | 不需提取，已是公共能力 |
| CodingUtil（hex/byte 转换） | 18 | iot/battery 和 collector/battery | 已集中，不需额外提取 |
| 日期格式化（SimpleDateFormat/DateTimeFormatter/DateUtil） | 28 | StatBatteryResServiceImpl(7)、统计/容量服务 | 后续可统一为单一日期工具，风险低 |
| 协议字节数组读写（hexString2binaryString/bytesToHex） | 12 | BatteryParamsHandler(11)、BatteryAlarmHandler(8) | 旧 iot 链路集中，新链路使用 CodingUtil，暂不提取 |
| 列表安全处理（isEmpty/size/CollectionUtils） | 65 | 全项目分散 | 惯用 Java 写法，提取收益低，暂不处理 |

结论：当前无需立即提取公共工具。已集中的工具（CacheUtils、CodingUtil）保持现状；日期格式化可后续统一；列表安全和协议字节属于惯用模式，提取收益不明显。

### TASK-REF-CLEANUP-001：主流程死代码清理

优先级：P3

目标：在前面拆分完成后，删除 `BatteryCollectorService` 中确认无调用的私有方法、字段、重复导入。

允许修改：

```text
BatteryCollectorService.java
相关测试 imports
```

禁止修改：

```text
任何 public 方法签名
任何业务逻辑
任何新服务行为
```

步骤：

1. 使用 IDE 或编译器确认私有方法无调用。
2. 每次只删除一组明显无用字段或方法。
3. 不做格式化整文件。
4. 删除后运行编译和核心测试。

验证：

```bash
mvn -DskipTests compile
mvn -Dtest=BatteryCollectorServiceTest test
git diff --check
```

提交建议：

```text
refactor: remove unused battery collector internals
```

停止条件：

1. 任何 public API 受影响，停止。
2. 删除项无法确认无调用，停止。

### TASK-REF-CONSOLIDATE-001：收缩兼容历史同步薄服务

优先级：P2

背景：`CompatReportLogSyncProcessor` 只调用 `BatteryModuleCompatReportLogSyncService.sync`，而该 service 只服务后处理流水线，职责和生命周期完全依附 processor。类似薄包装会增加无意义 Bean、文件和测试维护成本。

目标：在不改变兼容历史写入行为的前提下，把 `BatteryModuleCompatReportLogSyncService.sync` 的逻辑收缩到 `CompatReportLogSyncProcessor` 内，删除薄 service。

允许修改：

```text
CompatReportLogSyncProcessor.java
BatteryModuleCompatReportLogSyncService.java（删除）
CompatReportLogSyncProcessorTest.java
BatteryModuleCompatReportLogSyncServiceTest.java（删除或迁移断言）
collector/battery/realtime/README.md（更新服务归属）
energy_refactor_plan_20260618.md（记录执行结果）
```

禁止修改：

```text
BatteryModuleReportLogAdapterService.java
BatteryReportLogService.java
DataService.java
dev_battery_report_log 相关 mapper/XML/SQL
CompatReportLogSyncProcessor 的 getName/getOrder
compatReportLogEnabled 开关语义
```

步骤：

1. 用 `.codegraph` 或 `rg` 确认 `BatteryModuleCompatReportLogSyncService` 只有 `CompatReportLogSyncProcessor` 和测试引用。
2. 将 `adapterService.buildReportLog`、空数据校验、`dataService.isInsert`、`batteryReportLogService.insert`、debug 日志搬入 `CompatReportLogSyncProcessor` 的私有方法。
3. `shouldProcess` 不再依赖 `compatReportLogSyncService != null`；改为校验 processor 自身必需依赖，或在 `process` 内 null 安全返回。
4. 把 `BatteryModuleCompatReportLogSyncServiceTest` 中的行为断言迁入 `CompatReportLogSyncProcessorTest`，不新增低价值方法级测试。
5. 删除 `BatteryModuleCompatReportLogSyncService` 和其测试。
6. 更新 README 中 `BatteryModuleCompatReportLogSyncService` 待评估/归属描述。
7. 不改兼容历史写入字段、不改插入间隔判断、不改异常吞吐口径。

验证：

```bash
mvn -DskipTests compile
mvn -Dtest=CompatReportLogSyncProcessorTest,BatteryRealtimePostProcessorsTest test
git diff --check
```

提交建议：

```text
refactor: inline compat report log sync processor dependency
```

停止条件：

1. 发现除 processor 和测试外还有生产代码引用该 service，停止并改为保留 service。
2. 需要改 `BatteryModuleReportLogAdapterService` 输出字段，停止并拆功能任务。
3. 兼容历史写入开关、插入判断或异常处理行为变化，停止。

**CONSOLIDATE-001 执行结果（2026-06-18）：**

状态：completed。

已完成：

1. `BatteryModuleCompatReportLogSyncService.sync` 逻辑已收缩进 `CompatReportLogSyncProcessor` 私有方法。
2. `BatteryModuleCompatReportLogSyncService` 及其测试已删除。
3. `CompatReportLogSyncProcessorTest` 已覆盖批次校验、开关关闭、旧历史写入调用等核心行为。
4. `CompatReportLogSyncProcessor` 的 `getName`、`getOrder`、`compatReportLogEnabled` 开关语义保持不变。

后续不得重新引入该薄 service；如兼容历史同步逻辑显著增长，再新建明确职责的内部 helper。

### TASK-REF-CONSOLIDATE-002：盘点并收缩蓄电池薄服务

优先级：P3

目标：系统盘点 `collector/battery` 中只被单一类调用、且没有独立业务生命周期的薄 service/helper，逐项决定保留、迁包或收缩。

候选方向：

```text
BatteryModuleCompatReportLogSyncService（已完成，见 CONSOLIDATE-001）
BatteryModuleCellCompatibilityFillService（与实时单体构建强绑定，评估是否保留）
BatteryCurrentStateService（页面/外部当前状态门面，默认保留）
BatteryModuleReportLogAdapterService（跨 device/opt/controller 引用，默认保留）
BatteryModuleControlCommandService（命令 payload helper，默认保留）
```

步骤：

1. 用 `.codegraph` 输出候选类的 callers/callees。
2. 建表记录：调用方、是否跨模块、是否有独立测试、是否是 Spring Bean、是否有开关/缓存/事务语义。
3. 只对“唯一生产调用方 + 无独立生命周期 + 逻辑短小”的类新增具体收缩任务。
4. 高引用门面和跨模块 adapter 只补 README，不为文件数量强行收缩。

验证：

```bash
git diff --check
```

提交建议：

```text
docs: audit battery thin service consolidation candidates
```

停止条件：

1. 候选类涉及 mapper/XML/SQL 或跨模块 public API，停止并标为保留。
2. 需要同时修改多个 processor 或主流程时，拆成独立任务。

**CONSOLIDATE-002 执行结果（2026-06-18）：**

盘点结果（codegraph 扫描）：

| 候选 | 生产调用方 | 跨模块 | 决策 |
|---|---|---|---|
| BatteryModuleCompatReportLogSyncService | 1（CompatReportLogSyncProcessor） | 否 | 已收缩（CONSOLIDATE-001） |
| BatteryModuleCellCompatibilityFillService | 3（CollectorService, Consumer, RestoreServiceImpl） | 是（device/opt） | 保留 |
| BatteryCurrentStateService | 0（仅 test） | 否 | 保留（页面/外部门面） |
| BatteryModuleReportLogAdapterService | 4（ControlBattery, ControlBatterySet, ReportLogController, CompatReportLogSyncProcessor） | 是（device/opt, device/config） | 保留 |
| BatteryModuleControlCommandService | 1（BatteryCollectorCommandService） | 否 | 保留（命令 payload helper） |

结论：除已完成的 CONSOLIDATE-001 外，无其他可收缩的薄服务。所有候选类均有多调用方或跨模块依赖。

### TASK-REF-ENERGY-DIR-001：energy 项目级目录盘点

优先级：P3

目标：把重构范围扩展到整个 `03code/energy`，先产出项目级目录职责图和候选迁移清单，不直接迁 Java 文件。

允许修改：

```text
01document/energy_refactor_plan_20260618.md
可新增 01document/energy_project_directory_audit_YYYYMMDD.md
```

禁止修改：

```text
03code/energy/src/main/java/**/*.java
03code/energy/src/main/resources/**
03code/energy/sql/rysqlite3.db
```

盘点范围：

```text
com.shanhe.project.collector
com.shanhe.project.device
com.shanhe.project.energy
com.shanhe.project.iot
com.shanhe.project.sync
com.shanhe.project.scheduled
com.shanhe.framework（仅记录，不纳入业务迁移）
```

步骤：

1. 用 `.codegraph` 或 `rg --files` 统计各顶级包的文件数、主要职责、明显混放点。
2. 标记旧兼容入口、主业务入口、跨域门面、mapper/domain/service/controller 边界。
3. 识别三类候选：目录迁移、薄服务收缩、公共能力提取。
4. 每个候选必须写“禁止事项”和“第一提交只允许什么”。
5. 不把所有候选都排进立即执行；按功能优先级排序，蓄电池主链路、外部读写、测试计划闭环优先。

验证：

```bash
git diff --check
```

提交建议：

```text
docs: audit energy project directory boundaries
```

停止条件：

1. 执行者开始移动 Java 文件，停止。
2. 无法判断业务归属时，标记为待确认，不自行迁移。

### TASK-REF-ENERGY-DIR-002：energy 项目级目录小步迁移

优先级：P4

前置条件：必须完成 `TASK-REF-ENERGY-DIR-001`，且候选项已写清引用面和停止条件。

目标：按 `ENERGY-DIR-001` 产出的清单，小步迁移低风险、低引用、边界清晰的目录。

执行规则：

1. 每次只迁一个职责组，最多 3 个生产类。
2. 不迁 controller URL、mapper XML、domain 表映射类，除非单独任务明确允许。
3. 高引用 service 先保留旧门面，新增内部实现或 README，不直接改 package。
4. 迁移测试遵循 Q9，不为目录整齐批量移动测试。
5. 每次提交必须包含 compile 和针对性测试结果。

验证：

```bash
mvn -DskipTests compile
git diff --check
```

提交建议：

```text
refactor: move energy <scope> classes to <target> package
```

停止条件：

1. 单次迁移影响超过 20 个 imports，停止并拆小。
2. 需要改 Spring Bean 名称、Mapper namespace、SQL 或 URL，停止。
3. 迁移导致功能测试需要大改，停止并改为保留门面。

## 7. 推荐执行顺序

第一批，只做边界和低风险结构：

1. `TASK-REF-DIR-001`
2. `TASK-REF-LEGACY-001`
3. `TASK-REF-COMMON-001`

第二批，拆采集主流程：

1. `TASK-REF-COLLECTOR-002`
2. `TASK-REF-COLLECTOR-001`
3. `TASK-REF-COMMAND-001`
4. `TASK-REF-COMMAND-001B`
5. `TASK-REF-CLEANUP-001`

第三批，整理包目录：

1. `TASK-REF-COMMAND-002`
2. `TASK-REF-REALTIME-001`
3. `TASK-REF-POSTPROCESS-001`
4. `TASK-REF-STATE-001`
5. `TASK-REF-EXTERNAL-001`

第四批，项目级清理：

1. `TASK-REF-SCHEDULED-001`
2. `TASK-REF-CONSOLIDATE-001`
3. `TASK-REF-CONSOLIDATE-002`
4. 后续按 `TASK-REF-COMMON-001` 产出的候选任务拆分执行。

第五批，energy 项目级目录重构：

1. `TASK-REF-ENERGY-DIR-001`
2. `TASK-REF-ENERGY-DIR-002`（必须等 `ENERGY-DIR-001` 产出候选清单后逐项执行）

执行优先级说明：

1. 功能开发和 M460 能力补齐优先于项目级目录迁移。
2. `CONSOLIDATE-001` 属于低风险收缩，可在 `POSTPROCESS-001` 完成后优先执行。
3. `ENERGY-DIR-001/002` 只作为项目级重构储备任务，不插队到采集主链路和外部读写功能之前。

## 8. 每个任务的固定完成模板

执行者完成任务后，必须在提交说明或任务文档里写：

```text
任务 ID：
变更文件：
是否改行为：是/否
如改行为，原因：
验证命令：
验证结果：
未处理风险：
下一步建议：
```

## 9. 审查清单

审查者按以下清单检查：

1. 是否只做了任务卡允许的范围。
2. 是否误提交 `rysqlite3.db` 或 `.codegraph/`。
3. 是否改变协议码、寄存器地址、状态码、快照新鲜度规则。
4. 是否把新业务写进旧 `iot` 链路。
5. 是否移动 mapper XML 或 SQL。
6. 是否保留原 public API 或明确说明兼容影响。
7. 是否新增了必要中文注释。
8. 是否避免了无意义全文件格式化。
9. 是否运行了任务卡要求的验证命令。
10. 是否没有为了简单目录移动而机械新增大量低价值单元测试。

## 10. 执行 AI 的硬性提示词

后续把任务交给其他 AI 时，可直接附加以下提示词：

```text
你只能执行指定 TASK，不得顺手做其他重构。
开始前先读 01document/energy_refactor_plan_20260618.md 的总执行规则和对应任务卡。
开始前运行 git status --short。
不要提交 03code/energy/sql/rysqlite3.db 和 .codegraph/。
不要修改旧 iot/battery 或 iot/CM03N 业务逻辑，除非任务卡明确允许。
不要改变协议码、寄存器地址、状态码、快照新鲜度、数据库字段。
每次最多移动一个职责组。
完成后运行任务卡指定命令和 git diff --check。
如果需要扩大范围，停止并说明原因，不要自行扩展。
```

## 11. 执行前待确认疑问答复（2026-06-18）

本节对执行者提出的疑问给出定论。以下内容不再作为人工阻塞项；执行时按答复落地，若代码现状与答复不一致，停止并更新任务卡。

### Q1. `BatteryCollectorService` 引用面只有 4 个文件

答复：仍需要拆 `COLLECTOR-001/002`，但不是为了减少外部引用，而是为了降低类内部职责复杂度。

执行边界：

1. `BatteryCollectorService` 保留原 package、类名和 public API，继续作为 controller、device、sync 等链路的稳定门面。
2. 拆分重点是方法级职责分离：轮询循环、帧 I/O、命令队列、超时、状态、日志逐步委托给内部服务。
3. 不以“引用面少”为理由取消拆分；大类风险来自内部状态和流程耦合，不只来自外部调用数量。
4. 不在第一轮把 `BatteryCollectorService` 移到 `runtime` 包。

### Q2. `COLLECTOR-001` 轮询循环抽取的具体边界

答复：`COLLECTOR-001` 只抽“轮询编排”，不抽帧 I/O 细节、不抽命令队列实现、不抽超时实现。

`COLLECTOR-001` 包含：

1. 通道轮询入口编排：遍历通道、判断是否需要轮询、触发单轮采集。
2. 轮询顺序编排：全量发现或活跃地址轮询、组 246、单体地址顺序。
3. 调用已有发送/接收/命令/状态/日志方法的顺序。
4. 保持 `BatteryCollectorService` 作为门面，实际循环委托给 `BatteryCollectorPollingService`。

`COLLECTOR-001` 不包含：

1. 串口打开/关闭实现。
2. `writeFrame`、`readOnce`、接收缓冲裁剪、帧边界处理。
3. 命令队列出队、pending 创建、响应匹配。
4. 超时判断和超时落状态的具体实现。
5. 地址缓存增删和重置规则。

超时归属：

1. 第一轮仍留在 `BatteryCollectorService`。
2. `COMMAND-001` 可以迁移“命令 pending 超时完成/失败”。
3. 后续如需要单独任务，新增 `TASK-REF-COLLECTOR-003-TIMEOUT`，专门抽取轮询超时、pending 超时和超时状态持久化协调。

### Q3. `COLLECTOR-002` 与 `COLLECTOR-001` 的职责切割点

答复：两者有明确切割，不能混做。

`COLLECTOR-001` 负责“什么时候调用”：

1. 什么时候打开通道。
2. 什么时候执行全量发现。
3. 什么时候轮询地址。
4. 什么时候插入命令检查。
5. 什么时候等待 pending 完成。

`COLLECTOR-002` 负责“怎么和串口/帧交互”：

1. 帧发送协调：把 `BatteryCollectorFrame` 写到串口。
2. 帧接收协调：读取 bytes、追加 receive buffer、提取完整帧。
3. 接收缓冲保护：裁剪过长 buffer、丢弃异常片段。
4. 协议日志触发点保持一致。

发送命令归属：

1. 自动轮询帧的“发送动作”归 `COLLECTOR-002`。
2. 命令队列的“选择哪条命令、构造 pending、标记完成”归 `COMMAND-001`。
3. 命令队列最终写串口时调用 `COLLECTOR-002` 的帧 I/O 能力，但命令业务不归 `COLLECTOR-002`。

执行顺序建议：

1. 先执行 `COLLECTOR-002`，抽出最小帧 I/O 接口。
2. 再执行 `COLLECTOR-001`，轮询编排直接调用该接口。
3. 如果已经先执行 `COLLECTOR-001`，也必须只做委托编排，不提前设计复杂 I/O 抽象。

### Q4. `COMMAND-001` 与已有 `BatteryCollectorCommandLogService` 的关系

答复：`BatteryCollectorCommandQueueService` 负责命令队列执行协调；`BatteryCollectorCommandLogService` 继续负责日志持久化，二者不要合并。

执行边界：

1. `BatteryCollectorCommandQueueService` 包含：出队、是否可发送、pending 创建、响应匹配、成功/失败/超时完成协调。
2. `BatteryCollectorCommandQueueService` 不直接拼装 opt-log 字段；日志字段仍由 `BatteryCollectorCommandLogService` 负责。
3. 日志调用点可有两种方式，优先级如下：
   - 优先：`BatteryCollectorCommandQueueService` 在关键节点调用 `BatteryCollectorCommandLogService`，这样命令状态和日志更新保持内聚。
   - 可接受：第一小步中日志调用暂留 `BatteryCollectorService`，但必须在任务结果里写明后续迁移点。
4. 不把日志逻辑重新塞回 `BatteryCollectorService`。
5. 不改变 opt-log 字段、状态码和错误文案。

### Q5. `REALTIME-001` 迁移 10 个类的影响面

答复：不按原计划直接迁移高引用类包名；先评估影响面，保留高引用门面。

定论：

1. `BatteryModuleRealtimeSnapshotService` 第一轮不改 package。它已被页面、Modbus、device、collector 和多组测试引用，作为稳定门面保留。
2. `BatteryModuleRealtimeSnapshot` 模型第一轮不移动，继续留在 `model` 包。
3. `BatteryModuleControlCommandService` 不属于 `REALTIME-001`，归 `COMMAND-002` 评估；但第一轮也不建议改 package。
4. `REALTIME-001` 优先抽低引用 helper，或迁移引用面小的兼容填充类；每次最多 2 个类。
5. 对于 `BatteryModuleRealtimeConsumer`，执行前必须先 `rg` 检查引用面；如果超过 20 个业务文件，保留原包名，只抽内部 helper。

建议改写 `REALTIME-001` 的第一步：

```text
先新增 collector/battery/realtime 包说明或内部 helper；
保留 BatteryModuleRealtimeSnapshotService 原包名；
仅当 helper 无外部引用或引用很少时再迁移。
```

### Q6. `STATE-001` 中 `BatteryCollectorChannelState` 的归属

答复：`BatteryCollectorChannelState` 当前不是只被两个类使用；它被主服务、运行视图、协议日志、状态服务、缓存服务、命令测试等大量引用。第一轮不要移动。

执行边界：

1. `BatteryCollectorChannelState` 继续留在 `collector/battery/model`。
2. 它是运行态模型，不是持久化设备状态模型；不归入 `state` 包。
3. `STATE-001` 只整理设备状态持久化相关服务和常量，例如 `BatteryCollectorDeviceStateService`、`BatteryDeviceStateService`、`BatteryModeStatusService`、`BatteryDeviceState`、`BatteryDeviceStateConstants`。
4. 若后续要移动 `BatteryCollectorChannelState`，必须单独新增任务，并先处理测试中大量直接构造该类的影响。

### Q7. `POSTPROCESS-001` 的范围

答复：不一次性迁移 13 个类。先冻结边界，再小步迁移。

当前事实：

1. `service/postprocess` 下主代码已迁入 `collector/battery/postprocess`。
2. `BatteryRealtimePostProcessContext.java` 当前已经在 `collector/battery/postprocess` 包内。
3. `BatteryRealtimePostProcessContextFactory.java` 当前在 `service` 包，属于实时消费到后处理的上下文构造桥接类。

执行边界：

1. 目标包统一为 `collector/battery/postprocess`；主代码不得再回到 `service/postprocess`。
2. `BatteryRealtimePostProcessContext`、`PostProcessBatchGuard`、`RealtimeToReportLogAdapter` 已完成迁移，不得重复移动。
3. 具体 processor 已完成迁移；后续只允许补 README、修注释或整理测试包名，不改 processor 顺序、`shouldProcess` 条件、写库逻辑或缓存逻辑。
4. `BatteryRealtimePostProcessContextFactory` 暂不跟随 `POSTPROCESS-001` 迁移；它更接近 realtime 消费侧的组装逻辑，可在 `REALTIME-001` 中评估。
5. 不在本任务改 processor 顺序、`shouldProcess` 条件、写库逻辑或缓存逻辑。

### Q8. 任务执行顺序中的依赖关系

答复：`COLLECTOR-001` 和 `COLLECTOR-002` 不建议并行。推荐先 `COLLECTOR-002`，再 `COLLECTOR-001`。

推荐依赖顺序改为：

```text
DIR-001
  ↓
LEGACY-001 / COMMON-001（可独立执行）
  ↓
COLLECTOR-002（先抽最小帧 I/O）
  ↓
COLLECTOR-001（轮询编排委托帧 I/O）
  ↓
COMMAND-001（命令队列执行）
  ↓
CLEANUP-001（只清理已确认无用代码）
  ↓
COMMAND-002 / REALTIME-001 / POSTPROCESS-001 / STATE-001 / EXTERNAL-001（按引用面小步执行）
```

执行原则：

1. 不并行修改 `BatteryCollectorService`。
2. 每个任务完成并通过测试后再开始下一个主流程任务。
3. `COMMAND-002`、`REALTIME-001`、`POSTPROCESS-001`、`STATE-001`、`EXTERNAL-001` 之间没有强制线性依赖，但都必须遵守高引用门面不改包名规则。
4. `CLEANUP-001` 必须放在对应抽取任务之后，不能提前执行。

### Q9. 测试文件的迁移

答复：测试文件默认不跟随被测类移动。只有当被测类 package 已经迁移且测试需要访问包私有成员时，才同步迁移测试 package。

执行边界：

1. 纯抽 helper、保留门面的任务：测试文件保留原 test 目录结构，只更新 imports 或新增少量 helper 测试。
2. 高引用门面不移动时，对应测试也不移动。
3. 如果低引用内部类迁移 package，优先新增该内部类的新测试；旧门面测试保留，用于证明兼容行为不变。
4. 不为了目录整齐批量移动测试。
5. 不为每个私有方法新增测试；只覆盖抽取后可能回归的核心行为。

### Q10. `.codegraph` 使用

答复：本项目当前会话已启用 `.codegraph`，执行者应优先使用 `.codegraph` 做影响面确认；若执行环境没有 `.codegraph`，才退回使用 `rg`。

执行边界：

1. 在 Codex 环境中优先使用 `codegraph_impact`、`codegraph_explore` 核对调用面。
2. 在不支持 `.codegraph` 的其他 AI 环境中，使用 `rg` 搜索类名、import 和测试引用。
3. 无论使用哪种工具，都不能只凭“引用文件数少”决定迁包；还要看是否跨 `device`、`modbus`、`sync`、`scheduled`、`iot` 等边界。

### 11.1 关键类引用面速查表（codegraph/rg 复核 2026-06-18）

| 类 | 总引用 | 外部引用 | 外部调用方 | 风险等级 |
|---|---|---|---|---|
| `BatteryCollectorService` | 多处测试和少量外部门面调用 | device/opt restore、controller、command facade | RestoreServiceImpl、BatteryCollectorCommandController、BatteryCollectorCommandService | 中（门面，保留原包） |
| `BatteryCollectorCommandService` | codegraph 约 106 符号 | controller、device、modbus、sync | BatteryCollectorCommandController、ControlBatterySet、ModbusWriteMappingService、BatterySyncHandler | 高（保留原包） |
| `BatteryModuleControlCommandService` | 低外部引用 | 主要 collector 内部 | — | 中（命令映射 helper，第一轮不为目录整齐迁移） |
| `BatteryModuleRealtimeSnapshotService` | codegraph 约 76 符号 | device、modbus、page、collector | BatteryPackServiceImpl、RestoreServiceImpl、Modbus 读映射、页面当前状态 | 高（保留原包） |
| `BatteryModuleRealtimeConsumer` | 少量直接引用 | 主要 collector 主流程 | BatteryCollectorService | 中（可抽 helper，迁移前确认入库/快照/后处理时序） |
| `BatteryDeviceStateService` | codegraph 约 84 符号 | alarm、scheduled、device、modbus、controller | AlarmLogServiceImpl、RestoreServiceImpl、DeviceOnlineJob、CleanLogJob、Modbus 读映射 | 高（保留原包） |
| `BatteryModeStatusService` | 多处 command、restore、旧 iot 引用 | device、iot、collector | ControlBatterySet、RestoreServiceImpl、BatteryOptResHandler、命令服务 | 高（保留原包） |
| `BatteryCollectorChannelState` | 大量主流程和测试直接构造 | 主要 collector 内部和测试 | — | 中（留在 model，不迁移） |
| `BatteryCollectorDeviceStateService` | collector 内部为主 | BatteryCollectorService 等 | — | 中（可抽 helper，谨慎迁移） |
| `BatteryCollectorCacheService` | 2 | 0 | — | 低 |
| `BatteryModuleCompatReportLogSyncService` | 已收缩 | 0 | — | 已完成（逻辑内联到 CompatReportLogSyncProcessor） |
| `BatteryCurrentStateService` | 2 | 0 | — | 低 |

新增服务（待创建，无引用面）：

```text
BatteryCollectorPollingService — COLLECTOR-001 新增
BatteryCollectorFrameIoService — COLLECTOR-002 新增
BatteryCollectorCommandQueueService — COMMAND-001 新增
```

### 11.2 任务卡与 Q 答复同步更新记录

| 更新项 | 任务卡 | 改动 |
|---|---|---|
| 超时归属 | COLLECTOR-001 | 新增步骤 8：超时第一轮不迁移 |
| QueueService/LogService 关系 | COMMAND-001 | 新增步骤 8：QueueService 在关键节点调用 LogService |
| BatteryModuleControlCommandService 引用面 | COMMAND-002 | 已修正为第一轮保留原包名，不为目录整齐迁移 |
| 迁移列表分批 | REALTIME-001 | 已修正为"可评估/保留门面/待评估/暂不跟"，不直接迁移高时序风险类 |
| ContextFactory 归属 | POSTPROCESS-001 | 步骤 4 明确暂不跟本任务 |
| BatteryCollectorChannelState 移除 | STATE-001 | 从迁移列表移除，新增"不迁移"说明 |
| 跨模块依赖风险 | STATE-001 | 已修正为保留高引用状态门面，不批量同步外部 imports |
| 引用面速查 | 11.1 | 新增全量引用面速查表 |
| 命令队列抽取补齐 | COMMAND-001B | 新增后续任务，明确 COMMAND-001 当前仅 partial，不能直接进入 COMMAND-002 |
| 后处理薄服务收缩 | CONSOLIDATE-001/002 | 新增薄 service 收缩任务，优先处理 CompatReportLogSyncProcessor 的唯一依赖 |
| energy 项目级目录 | ENERGY-DIR-001/002 | 新增全项目目录盘点和小步迁移任务，排在功能开发和蓄电池主链路稳定之后 |

### 11.3 二次审查修正记录（2026-06-18）

本次审查发现其他 AI 补充内容中有部分“低引用即可迁包”的判断过乐观，已按项目规则修正：

1. `TASK-REF-COMMAND-002` 中 `BatteryModuleControlCommandService`、`BatteryCollectorCommandLogService` 不再标为低风险可迁移；它们第一轮保留原包名，除非有明确收益。
2. `TASK-REF-REALTIME-001` 中 `BatteryModuleRealtimeConsumer` 不再直接标为可迁移；即使引用面小，也必须先确认入库、快照、后处理时序不变。
3. `TASK-REF-STATE-001` 中 `BatteryDeviceStateService`、`BatteryDeviceStateServiceImpl`、`BatteryModeStatusService` 不再允许第一轮迁包；这些类影响告警、定时任务、Modbus、恢复逻辑和旧 iot 兼容链路。
4. `Q10` 已修正为本项目优先使用 `.codegraph`；其他不支持 `.codegraph` 的执行环境才退回 `rg`。
5. 阶段 B 顺序已同步为先 `COLLECTOR-002` 帧 I/O，再 `COLLECTOR-001` 轮询编排。
6. `COMMAND-001` 当前执行结果已审查为 partial；新增 `COMMAND-001B` 补齐队列出队、发送协调、完成回调和超时完成抽取。
7. 根据后续审查，`POSTPROCESS-001` 主代码迁包已完成；新增 `CONSOLIDATE-001` 收缩 `BatteryModuleCompatReportLogSyncService` 薄包装。
8. 重构范围扩展到整个 `energy` 项目，但先执行 `ENERGY-DIR-001` 文档盘点，不直接迁 Java 文件。
