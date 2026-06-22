# energy 重构当前权威计划

更新时间：2026-06-22

本文由 Codex 维护，作为后续重构执行的权威入口。历史文件 `energy_refactor_plan_20260618.md` 保留归档价值，但后续执行以本文和 `energy_project_directory_audit_20260618.md` 为准。

## 1. 分工规则

Codex 负责：

1. 文档修订、任务规划、全局目录盘点。
2. 跨模块影响判断。
3. 高引用门面是否迁包的决策。
4. `BatteryCollectorService` 主流程继续拆分的审查。

其他 AI 只执行 Codex 拆出的明确代码任务，且任务必须满足：

1. 只改 1 到 3 个明确文件。
2. 指定类名、方法名和允许修改范围。
3. 给出停止条件和验证命令。
4. 不提交 `03code/energy/sql/rysqlite3.db`。
5. 不提交 `.codegraph/`。
6. 不使用 PowerShell `Get-Content` / `Set-Content` 写中文文件。

## 2. 已完成状态

| 任务 | 状态 | 结果 |
|---|---|---|
| `TASK-REF-DIR-001` | 已完成 | 已建立 `collector/battery` 包职责说明 |
| `TASK-REF-COLLECTOR-001` | 已完成 | 已抽取 `BatteryCollectorPollingService` |
| `TASK-REF-COLLECTOR-002` | 已完成 | 已抽取 `BatteryCollectorFrameIoService` |
| `TASK-REF-COLLECTOR-003-TIMEOUT` | 已完成 | 已抽取 `BatteryCollectorTimeoutService` |
| `TASK-REF-COMMAND-001` | 已完成 | 已抽取 `BatteryCollectorCommandQueueService` 基础能力 |
| `TASK-REF-COMMAND-001B` | 已完成 | 已补齐出队、发送协调、完成回调、超时收尾 |
| 连接条电阻流程拆分 | 已完成 | 已新增 `BatteryConnectResistanceCommandProcessor` |
| `TASK-REF-POSTPROCESS-001` | 已完成 | 后处理主代码已迁入 `collector/battery/postprocess` |
| `TASK-REF-STATE-001` | 部分完成 | `BatteryCollectorDeviceStateService` 已迁入 `state`，高引用状态门面保留 |
| `TASK-REF-REALTIME-001` | 部分完成 | 组级计算与兼容填充已迁入 `realtime`，高引用实时门面保留 |
| `TASK-REF-CONSOLIDATE-001` | 已完成 | 兼容历史同步薄服务已收缩 |
| `TASK-REF-CONSOLIDATE-002` | 已完成 | 已盘点，无其他立即收缩候选 |
| `TASK-REF-EXTERNAL-001` | 已完成 | 已补外部读取边界说明 |
| `TASK-REF-SCHEDULED-001` | 已完成 | 已补定时任务归属说明 |
| `TASK-REF-ENERGY-DIR-001` | 已完成 | 已重写项目级目录盘点文档 |

## 3. 当前不执行的迁包

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

原因：这些类是跨模块门面或高引用服务，涉及页面、旧 iot、Modbus、sync、scheduled、device 等边界。迁包收益不足，破坏面大。

## 4. 后续任务池

### TASK-REF-DOC-001：清理历史计划过期描述

执行者：Codex。

目标：

1. 将 `energy_refactor_plan_20260618.md` 中 `COMMAND-001B`、`COLLECTOR-003-TIMEOUT` 的旧状态修正。
2. 删除或标注“测试仍需适配”等过期结论。
3. 保留历史任务卡，不重写整篇文档。

### TASK-REF-COLLECTOR-004：主流程剩余职责审查

执行者：Codex。

只审查，不改代码：

1. `BatteryCollectorService.readOnce`
2. `BatteryCollectorService.handleCompletedPendingResponse`
3. `BatteryCollectorService.writeFrame`
4. `BatteryCollectorService.writeFrameWithoutPending`
5. `BatteryCollectorService.closeQuietly`

输出：是否值得继续抽取、影响面、停止条件、验证命令。

### TASK-REF-README-001：补齐 collector/battery README

执行者：可交给其他 AI。

允许修改：

1. `03code/energy/src/main/java/com/shanhe/project/collector/battery/README.md`

要求：

1. 补充 `runtime`、`command`、`state`、`postprocess`、`realtime` 的当前职责。
2. 不改 Java。
3. 不新增测试。

验证：

```powershell
git diff --check 03code/energy/src/main/java/com/shanhe/project/collector/battery/README.md
```

### TASK-REF-README-002：补齐 runtime 包说明

执行者：可交给其他 AI。

允许修改：

1. `03code/energy/src/main/java/com/shanhe/project/collector/battery/runtime/README.md`

若文件不存在，可以新增。

要求：

1. 说明 `BatteryCollectorFrameIoService`、`BatteryCollectorPollingService`、`BatteryCollectorTimeoutService` 的职责。
2. 写清三者不负责的内容：
   - 不负责业务命令选择。
   - 不负责后处理。
   - 不负责页面/Modbus 查询。
3. 不改 Java。

验证：

```powershell
git diff --check 03code/energy/src/main/java/com/shanhe/project/collector/battery/runtime/README.md
```

### TASK-REF-README-003：补齐 command 包说明

执行者：可交给其他 AI。

允许修改：

1. `03code/energy/src/main/java/com/shanhe/project/collector/battery/command/README.md`

若文件不存在，可以新增。

要求：

1. 说明 `BatteryCollectorCommandQueueService` 的边界。
2. 说明 `BatteryConnectResistanceCommandProcessor` 只处理连接条电阻 0F/11/91 流程。
3. 明确 `BatteryCollectorCommandLogService` 第一轮保留在 `service` 包。
4. 不改 Java。

验证：

```powershell
git diff --check 03code/energy/src/main/java/com/shanhe/project/collector/battery/command/README.md
```

### TASK-REF-TEST-001：测试包名整理评估

执行者：Codex。

目标：只评估是否需要移动测试 package，不直接改测试。

审查对象：

1. `BatteryCollectorServiceTest`
2. `BatteryCollectorCommandLogServiceTest`
3. `BatteryDeviceStateServiceTest`
4. `BatteryModuleGroupCalculationServiceTest` 的删除历史

输出：

1. 哪些测试仍应保留在 `service` 测试包。
2. 哪些新增 runtime/command 测试值得补。
3. 哪些测试不需要补，避免过度测试。

### TASK-REF-TEST-002：补一个核心 runtime 超时测试

执行者：可交给其他 AI。

前置条件：必须先由 Codex 完成 `TASK-REF-TEST-001` 并明确允许。

允许新增：

1. `03code/energy/src/test/java/com/shanhe/project/collector/battery/runtime/BatteryCollectorTimeoutServiceTest.java`

只覆盖核心行为：

1. 未超时不调用重试。
2. 超时且未达到最大重试次数时调用重试 writer。
3. 达到最大重试次数时清理 pending 并递增 timeoutCount。

禁止：

1. 不 mock 串口。
2. 不测每个私有方法。
3. 不修改主代码。

验证：

```powershell
mvn "-DskipTests=false" "-Dmaven.test.skip=false" "-Dtest=BatteryCollectorTimeoutServiceTest,BatteryCollectorServiceTest,BatteryCollectorCommandLogServiceTest" test
```

### TASK-REF-COLLECTOR-005：响应完成分派审查

执行者：Codex。

目标：审查 `BatteryCollectorService.handleCompletedPendingResponse` 是否值得拆成更小 processor。

审查维度：

1. 普通命令响应。
2. 自动编号响应。
3. 连接条电阻响应。
4. 地址缓存重置副作用。
5. 模式状态停止副作用。

输出：

1. 是否需要新增 `BatteryAutoAddressCommandProcessor`。
2. 是否继续保留在 `BatteryCollectorService`。
3. 如果拆，明确允许修改文件和验证命令。

### TASK-REF-COLLECTOR-006：串口读取分派审查

执行者：Codex。

目标：审查 `BatteryCollectorService.readOnce` 是否继续拆分。

当前默认结论：暂不拆。

只有满足以下条件才新建代码任务：

1. 能保持 `BatteryCollectorService` 外部行为不变。
2. 不改变 receive buffer 截断策略。
3. 不改变 `moduleFrameDispatcher.dispatch` 调用顺序。
4. 不改变 pending 匹配规则。

### TASK-REF-CLEANUP-002：删除过期注释和无效 README 断言

执行者：可交给其他 AI。

允许修改：

1. `03code/energy/src/main/java/com/shanhe/project/collector/battery/README.md`
2. `03code/energy/src/main/java/com/shanhe/project/collector/battery/postprocess/README.md`
3. `03code/energy/src/main/java/com/shanhe/project/collector/battery/realtime/README.md`

要求：

1. 只删除与当前代码明显矛盾的句子。
2. 不新增架构判断。
3. 不改 Java。

验证：

```powershell
git diff --check 03code/energy/src/main/java/com/shanhe/project/collector/battery/README.md 03code/energy/src/main/java/com/shanhe/project/collector/battery/postprocess/README.md 03code/energy/src/main/java/com/shanhe/project/collector/battery/realtime/README.md
```

### TASK-REF-EXTERNAL-002：外部读取缓存边界复核

执行者：Codex。

目标：复核 JSON/TCP、Modbus、页面查询是否都通过实时缓存/快照读取，不直接访问过期后处理表。

审查对象：

1. `BatteryModuleModbusReadMappingService`
2. `BatteryAlarmHandler`
3. `BatteryPackHandler`
4. 页面当前状态查询相关 service

输出：

1. 查询入口清单。
2. 是否存在绕过实时缓存的路径。
3. 如需改代码，另拆明确任务。

### TASK-REF-M460-001：M460 剩余能力复核

执行者：Codex。

目标：复核 M460 能力是否仍有未整合项，更新 `M460未整合能力全局盘点_20260618.md`。

要求：

1. 只做盘点和任务拆分。
2. 不改 Java。
3. 每个未整合能力写清旧入口、energy 当前入口、是否需要整合、优先级。

### TASK-REF-DATA-001：禁止提交本地数据库文件

执行者：可交给其他 AI。

目标：避免 `03code/energy/sql/rysqlite3.db` 反复出现在工作树。

允许修改：

1. `.gitignore`

前置条件：Codex 先确认该数据库文件是否应纳入版本管理。未确认前不得执行。

验证：

```powershell
git status --short
```

### TASK-REF-CODEGRAPH-001：codegraph 使用说明

执行者：可交给其他 AI。

允许修改：

1. `01document/energy_refactor_current_plan_20260622.md`

要求：

1. 补充”执行代码任务前优先用 codegraph 或 rg 查引用面”。
2. 不改其他文档。
3. 不改 Java。

**CODEGRAPH-001 执行结果（2026-06-22）：**

执行代码任务前必须先查引用面：

1. 优先使用 `codegraph_callers`、`codegraph_impact`、`codegraph_explore` 确认影响面。
2. 若 codegraph MCP 未集成，退回使用 `rg -l “ClassName”` 搜索引用。
3. 不能只凭”引用文件数少”决定迁包；还要看是否跨 `device`、`modbus`、`sync`、`scheduled`、`iot` 等边界。
4. 影响超过 20 个业务文件的类不迁移，改为保留原门面。

## 5. 暂不执行任务

以下任务暂不执行，除非 Codex 重新开任务：

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

## 6. 固定验证命令

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

乱码扫描由 Codex 执行，避免把扫描规则自身写入普通任务文档后造成误报。
