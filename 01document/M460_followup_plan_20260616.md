# M460 follow-up plan refinement 2026-06-16

This note refines the remaining follow-up work after reviewing the project plan and M460
comparison documents. Items previously listed as manual confirmation are converted into
self-verification tasks when they can be checked against local M460 source code and project docs.

## 1. M460 self-verification tasks

| Priority | Task | Status | Scope |
|---|---|---|---|
| P0 | `TASK-BAT-M460-VERIFY-STATE-001` | Planned | Compare `rs485.c`, `modbus.c`, `M460字段来源矩阵.md`, and current realtime/group status code. Freeze the mapping for `Battery_State_Register`, `batteryPackStatus`, and `resistanceTestStatus`; add tests for unknown/no-source values. |
| P0 | `TASK-BAT-M460-VERIFY-MODBUS-001` | Planned | Compare `modbus.c` with `SH_Modbus寄存器映射草案.md` and `BatteryModuleModbusReadMappingService`. Verify first-data-not-ready exception, zero fill after ready, state register, alarm register, and high-frequency read cache behavior. |
| P1 | `TASK-BAT-M460-VERIFY-ALARM-001` | Planned | Compare `alarm.c`, `alarm.h`, `M460_87告警规则表.md`, and current alarm processors. Re-audit 87/8D bit-to-item mapping and freeze regression tests around unsupported bits. |
| P1 | `TASK-BAT-M460-VERIFY-CAPACITY-001` | Planned | Compare `soc.c`, `capacity.c`, and current `CapacityPredictionProcessor`/old predictor behavior. Decide which calculations are migrated to postprocess and which stay unsupported until reliable inputs exist. |
| P1 | `TASK-BAT-M460-VERIFY-COMMAND-001` | Planned | Compare `protocol_package.c`, `server.c`, and control services for `08/88`, `18/A8`, `03/83`, `12/92`, `76/F6`, `0F`, `11/91`. Verify response flags, byte order, and opt-log completion semantics. |
| P2 | `TASK-BAT-M460-VERIFY-JSONTCP-001` | Planned | Compare old JSON/TCP handlers with the standard realtime model. List each externally exposed field, its realtime/cache source, fallback rule, and switch flag. Do not add new logic to old handlers unless it is compatibility protection. |

## 2. Main-flow structure tasks

| Priority | Task | Status | Scope |
|---|---|---|---|
| P0 | `TASK-BAT-COLLECTOR-STRUCT-001` | Completed 2026-06-16 | Introduce a request object for `BatteryModuleRealtimeConsumer` postprocess flow so the main path does not carry repeated parameter groups. Behavior remains unchanged. |
| P1 | `TASK-BAT-COLLECTOR-STRUCT-002` | Planned | Split `BatteryCollectorService` by responsibility after current behavior is stable: polling loop, command queue, channel/device state, metrics/snapshot, and protocol logging. Keep public API stable and move one responsibility per commit. |
| P1 | `TASK-BAT-COLLECTOR-STRUCT-003` | Completed 2026-06-16 | Move realtime postprocess context construction into a small builder/factory if more processors require extra inputs. Avoid expanding `BatteryModuleRealtimeConsumer` with business rules. |

## 3. Priority execution order

1. Finish `TASK-BAT-COLLECTOR-STRUCT-001` because it is low risk and directly reduces main-flow parameter spread.
2. Execute `TASK-BAT-M460-VERIFY-STATE-001` before adding any new status-derived behavior.
3. Execute `TASK-BAT-M460-VERIFY-MODBUS-001` before enabling broader external high-frequency Modbus reads.
4. Continue `TASK-BAT-M460-POSTPIPE-002` slices only after status/source verification is frozen.

## 4. Field confirmation boundary

The following remain real field or hardware confirmations and should not be guessed from source code:

- Actual serial device names, baud rate overrides, timeout/retry values, and 6-channel pressure limits.
- Physical device response behavior under unplug, module replacement, and noisy RS485 lines.
- External platform acceptance for JSON/TCP and Modbus value timing.
- Runtime database upgrade and historical data retention policy.

## 5. Execution result: TASK-BAT-COLLECTOR-STRUCT-001

- Date: 2026-06-16
- Changed files:
  - `BatteryRealtimePostProcessRequest`
  - `BatteryModuleRealtimeConsumer`
  - `BatteryModuleRealtimeConsumerTest`
  - `M460_followup_plan_20260616.md`
- Behavior change: none intended. The realtime postprocess path now carries its related inputs through one request object while preserving the existing snapshot preference and poll-context fallback.
- Verification:
  - `mvn "-DskipTests=false" "-Dmaven.test.skip=false" "-Dtest=BatteryModuleRealtimeConsumerTest,BatteryRealtimePostProcessorsTest" test`
  - `git diff --check`

## 6. Execution result: TASK-BAT-M460-VERIFY-STATE-001

- Date: 2026-06-16
- Status: completed
- M460 source findings:
  - `rs485.h` defines `BatteryStatus` as the low 4 bits in the status byte and `InternalResistanceTestStatus` as an independent byte.
  - `rs485.c`, `modbus.c`, and `server.c` build `Battery_State_Register` as `(temp << 8) | Status->InternalResistanceTestStatus`.
  - `rs485.c` and `server.c` output the high byte first, then the low byte for item 36.
- Energy findings:
  - `BatteryModuleStatusRegisterCodec` already matches the M460 register layout and masks the high byte to 4 bits plus low byte to 8 bits.
  - `BatteryModuleModbusReadMappingService` uses that codec for register `411762`; missing status fields intentionally read as `0` for Modbus output compatibility.
  - `BatteryModuleRealtimeConsumer.buildGroup` does not parse or invent `batteryPackStatus`/`resistanceTestStatus` from the 600 `01/81` group frame.
  - `BatteryModuleGroupCalculationService` only carries status fields forward when they already exist on the fresh group realtime record.
  - `BatteryModuleGroupCompatibilityFillService` may still derive compatibility status after group calculation from current direction and default resistance-test status, matching the earlier `POSTPIPE-006` compatibility strategy; this is not treated as a raw 600 frame source.
- Added regression coverage:
  - Raw group frame build leaves M460 status fields null.
  - Group calculation does not create status fields when no source exists.
  - Group calculation copies status fields only from a fresh group module record.
- Verification:
  - `mvn "-DskipTests=false" "-Dmaven.test.skip=false" "-Dtest=BatteryModuleRealtimeConsumerTest,BatteryModuleGroupCalculationServiceTest,BatteryModuleModbusReadMappingServiceTest,BatteryModuleStatusRegisterCodecTest,RealtimeToReportLogAdapterTest" test`
  - `git diff --check`

## 7. Execution result: TASK-BAT-M460-VERIFY-MODBUS-001

- Date: 2026-06-16
- Status: completed
- M460 source findings:
  - `modbus.c` serves read requests from cached battery array data and returns Modbus exception semantics when the requested data is not ready.
  - `modbus.c` uses `Battery_State_Register` for the group state register, matching the state-register verification in section 6.
  - Alarm and fault values in M460 are produced by the alarm pipeline and then exposed as cached register/payload data; energy keeps those as standard alarm/device-state sources instead of reading old 980 payload directly.
- Energy findings:
  - `BatteryModuleModbusReadMappingService.readHoldingRegisters` loads one `ModbusReadSnapshot` per request, then resolves all requested registers from that in-memory snapshot.
  - If neither cell nor group realtime data exists, the service throws `IllegalStateException`; the RTU layer maps this to the existing Modbus exception path.
  - After data is ready, missing cells, missing group fields, and unsupported optional values resolve to `0`, matching the M460 empty-register compatibility behavior.
  - Register `411762` uses `BatteryModuleStatusRegisterCodec` for `(batteryPackStatus << 8) | resistanceTestStatus`.
  - Device-state registers `411483..411488` read `battery_device_state` and return `0` when status or state service is absent.
  - When `BatteryModuleRealtimeSnapshotService` is available, Modbus reads use the cached realtime snapshot and avoid mapper queries, which is the intended high-frequency request path.
- Added regression coverage:
  - A multi-register read loads mapper cells/group only once.
  - A snapshot-backed read uses `BatteryModuleRealtimeSnapshotService` and does not query `BatteryModuleRealtimeMapper`.
- Verification:
  - `mvn "-DskipTests=false" "-Dmaven.test.skip=false" "-Dtest=BatteryModuleModbusReadMappingServiceTest,ModbusRtuServerTest" test`
  - `git diff --check`

## 8. Execution result: TASK-BAT-M460-VERIFY-ALARM-001

- Date: 2026-06-16
- Status: completed
- M460 source findings:
  - `alarm.h` defines `Battery_Array_Alarm_Real_Result` as group-level commonly/abnormal/critical bytes followed by repeated monomer alarm structs.
  - `server.c` `Server_Get_Battery_Array_Warning_State_Function` copies `Battery_Array_Alarm_Real_Result` directly into the 87 payload with `memcpy`; no bit reordering is performed by M460.
  - `alarm.h` `BATTERY_TYPE_MAKE` uses the high 2 bits of status byte 1 as alarm level/type markers, so effective 87 alarm bits are status1 low 6 bits plus status2 8 bits.
  - 8D fault payload uses device fault result structures and the same byte-level physical-bit orientation; energy's binary-string index must continue to reverse physical bit order for hex string parsing.
- Energy findings:
  - `BatteryAlarmBitMapping` already centralizes physical-bit to binary-string-index conversion and 87 effective-bit slicing.
  - `BatteryAlarmHandler` tests already cover group 87, single-cell 87, group 8D, and single-cell 8D mappings to item codes.
  - New collector/postprocess alarm flow remains standard-model based and does not consume old 87/8D payload directly.
- Added regression coverage:
  - 87 decoder now has a payload-order test matching the M460 struct copy layout: group commonly/abnormal/critical pairs first, then each monomer's commonly/abnormal/critical triplets.
- Verification:
  - `mvn "-DskipTests=false" "-Dmaven.test.skip=false" "-Dtest=BatteryAlarmBitMappingTest,BatteryAlarmHandlerTest,AlarmContextProcessorTest,BatteryModuleAlarmAdaptServiceTest" test`
  - `git diff --check`

## 9. Execution result: TASK-BAT-M460-VERIFY-CAPACITY-001

- Date: 2026-06-16
- Status: completed
- M460 source findings:
  - M460 group SOC comes from `SOC_tableSox.averageSoc_perc` and is written as one-decimal percent.
  - M460 SOH/capacity/backup/discharge values depend on `Verify_Capacity_Array_Type`, `Verify_Capacity_Data_Type`, configured rated capacity, discharge current, and capacity-test flags.
  - When capacity test is active, M460 uses measured `Array_Capacity`, `Array_Support_Time`, `Time_Array_Capacity`, and `Discharge_Time`; otherwise it falls back to configured rated capacity plus `StateOfHealth`.
  - These inputs are not fully present in the 600 `01/81` group frame currently parsed by energy.
- Energy findings:
  - `CapacityPredictionProcessor` correctly stays in the postprocess pipeline and delegates to the existing `BatteryPredictorService`, instead of implementing SOC/SOH/capacity math in the collector polling thread.
  - `BatteryModuleGroupCompatibilityFillService` copies group SOH/capacity/backup/discharge values only from `PreBatteryGroupService.lastCache`.
  - `BatteryModuleCellCompatibilityFillService` copies cell capacity only from `PreBatteryGroup.mapBattery`.
  - `BatteryModuleReportLogAdapterService` and realtime adapters do not synthesize SOC/SOH/capacity when compatibility fields are missing.
- Decision:
  - Keep M460-style capacity prediction as postprocess/cache-backed behavior for now.
  - Do not port `soc.c`/`capacity.c` algorithms into `BatteryModuleRealtimeConsumer`, frame parsing, or the polling loop until the equivalent inputs and state-machine transitions are explicitly modeled.
- Added regression coverage:
  - Cell capacity is filled only when the prediction cache has the matching `CAP_BAT + batNum` entry.
  - Missing prediction cache leaves cell capacity null.
- Verification:
  - `mvn "-DskipTests=false" "-Dmaven.test.skip=false" "-Dtest=BatteryModuleCellCompatibilityFillServiceTest,BatteryModuleGroupCompatibilityFillServiceTest,BatteryModuleReportLogAdapterServiceTest,BatteryRealtimePostProcessorsTest" test`
  - `git diff --check`

## 10. Execution result: TASK-BAT-M460-VERIFY-COMMAND-001

- Date: 2026-06-16
- Status: completed
- M460 source findings:
  - `rs485.h` defines the module-side command matrix as `01/81`, `02/82`, `03/83`, `08/88`, `0A`, `0F`, `11/91`, `12/92`, `18/A8`, and `76/F6`.
  - `protocol_package.c` handles upper `SYS_CONNECT_RESISTANCE_TEST` by starting connection-resistance flow and then using module-side `11/91` voltage reads per cell.
  - `protocol_package.c` handles `SYS_AUTOMATIC_SET_SUBMODULE_ADDRESS` as a multi-step `18/A8` flow, with stop frames sent after the final cell response.
  - Upper aggregate internal-resistance coefficient codes `19/99` are not module-side protocol codes; the module command is `12/92`.
- Energy findings:
  - `BatteryDeviceProtocolCode` matches the M460 module-side command matrix, including no-response commands for `0A`/`0F` and status-response commands for `02/82`, `03/83`, `08/88`, `12/92`, and `76/F6`.
  - `BatteryCollectorService.processQueuedModuleCommand` sends no-response commands without pending response and sends response commands through the pending-request path.
  - `CONNECT_STRIP_RESISTANCE_TEST` keeps mode running after the `0F` start frame and queues `GET_CONNECT_STRIP_RESISTANCE_VOLTAGE` reads until the configured max address.
  - `AUTO_SET_MODULE_ADDRESS` keeps the mode running across intermediate `18/A8` responses, queues next address steps, and only stops after the stop-group frame path.
  - `BatteryCollectorCommandService` converts the old integer internal-resistance coefficient to the M460 module float payload and keeps aggregate `19/99` out of `BatteryDeviceProtocolCode`.
- Added regression coverage:
  - Full M460 module-side command matrix is asserted in `BatteryDeviceProtocolCodeTest`.
  - Request and response lookup coverage now includes all command pairs, not only common responses.
  - Aggregate `19/99` internal-resistance coefficient codes are explicitly rejected as module-side protocol codes.
- Verification:
  - `mvn "-DskipTests=false" "-Dmaven.test.skip=false" "-Dtest=BatteryDeviceProtocolCodeTest,BatteryModuleControlCommandServiceTest,BatteryCollectorCommandServiceTest,BatteryCollectorServiceTest" test`
  - `git diff --check`

## 11. Execution result: TASK-BAT-COLLECTOR-STRUCT-003

- Date: 2026-06-16
- Status: completed
- Changed files:
  - `BatteryRealtimePostProcessContextFactory`
  - `BatteryModuleRealtimeConsumer`
  - `BatteryRealtimePostProcessContextFactoryTest`
  - `BatteryModuleRealtimeConsumerTest`
- Behavior change: none intended. `BatteryModuleRealtimeConsumer` still controls flush, snapshot refresh, and asynchronous postprocess submission, while request snapshotting and `BatteryRealtimePostProcessContext` construction now live in a focused factory.
- Added regression coverage:
  - Async postprocess request snapshot copies poll cells/groups before execution.
  - Context construction falls back to poll-context cells/calculation when no realtime snapshot exists.
  - Context construction prefers `BatteryModuleRealtimeSnapshot` cells/group when available.
- Verification:
  - `mvn "-DskipTests=false" "-Dmaven.test.skip=false" "-Dtest=BatteryModuleRealtimeConsumerTest,BatteryRealtimePostProcessContextFactoryTest,BatteryRealtimePostProcessorsTest" test`
  - `git diff --check`

## 12. Execution result: TASK-BAT-COLLECTOR-STRUCT-002 slice 1

- Date: 2026-06-16
- Status: completed slice; parent task remains planned for further command/polling splits.
- Changed files:
  - `BatteryCollectorRuntimeViewService`
  - `BatteryCollectorService`
  - `BatteryCollectorRuntimeViewServiceTest`
  - `BatteryCollectorServiceTest`
- Behavior change: none intended. Status and metrics endpoints still call `BatteryCollectorService`, but DTO construction has moved into `BatteryCollectorRuntimeViewService`.
- Scope:
  - Extracted channel status snapshot construction.
  - Extracted collector/channel metrics construction and cached realtime snapshot metric fill.
  - Kept polling, command queue, timeout, address-cache, and protocol logging logic inside `BatteryCollectorService` for later smaller slices.
- Added regression coverage:
  - Channel snapshot maps runtime state, pending command, active addresses, and queued command count.
  - Metrics aggregate channel counts, active address counts, queue counts, timeout counts, and cached realtime snapshot freshness counts.
  - `BatteryCollectorService.getMetrics` remains covered as the public facade.
- Verification:
  - `mvn "-DskipTests=false" "-Dmaven.test.skip=false" "-Dtest=BatteryCollectorServiceTest,BatteryCollectorRuntimeViewServiceTest" test`
  - `git diff --check`

## 13. Execution result: TASK-BAT-COLLECTOR-STRUCT-002 slice 2

- Date: 2026-06-16
- Status: completed slice; parent task remains planned for further command/polling splits.
- Changed files:
  - `BatteryCollectorCacheService`
  - `BatteryCollectorService`
  - `BatteryCollectorCacheServiceTest`
  - `BatteryCollectorServiceTest`
- Behavior change: none intended. `BatteryCollectorService` still exposes the same reset and dedup-cache facade methods, while cache mutation details moved into `BatteryCollectorCacheService`.
- Scope:
  - Extracted module address cache reset by channel.
  - Extracted module address cache reset by battery group.
  - Extracted realtime snapshot eviction coupled to address-cache reset.
  - Extracted device-state dedup cache cleanup by battery group.
  - Kept polling, command queue, timeout, and protocol logging logic inside `BatteryCollectorService` for later slices.
- Added regression coverage:
  - Selected channel reset clears active addresses, miss counts, full-discovery flag, and evicts only that pack snapshot.
  - Selected battery group reset does not touch other groups.
  - Dedup cache cleanup removes pack-scope and channel-scope keys for the selected group only.
  - Missing battery group clears all dedup cache entries.
- Verification:
  - `mvn "-DskipTests=false" "-Dmaven.test.skip=false" "-Dtest=BatteryCollectorServiceTest,BatteryCollectorCacheServiceTest" test`
  - `git diff --check`

## 14. Execution result: TASK-BAT-COLLECTOR-STRUCT-002 slice 3

- Date: 2026-06-16
- Status: completed slice; parent task remains planned for further command/polling, state persistence, and command-log splits.
- Changed files:
  - `BatteryCollectorProtocolLogService`
  - `BatteryCollectorService`
  - `BatteryCollectorProtocolLogServiceTest`
  - `BatteryCollectorServiceTest`
- Behavior change: none intended. Protocol send/receive logs and poll summary logs keep the same trigger points, while log formatting and debug-channel filtering now live in a focused protocol log service.
- Scope:
  - Extracted protocol tx/rx/retry logging from the main collector flow.
  - Extracted poll summary logging and command-list summarization.
  - Kept the public collector service API unchanged.
  - Added Chinese class and method comments for the new protocol log service and its tests.
- Added regression coverage:
  - Protocol logs are skipped when debug logging is disabled.
  - Debug channel filtering suppresses non-selected channels.
  - Matching debug channels emit protocol logs.
  - Poll summary keeps long completed-command list truncation.
- Verification:
  - `mvn "-DskipTests=false" "-Dmaven.test.skip=false" "-Dtest=BatteryCollectorServiceTest,BatteryCollectorProtocolLogServiceTest" test`

## 15. Execution result: TASK-BAT-COLLECTOR-STRUCT-002 slice 4

- Date: 2026-06-16
- Status: completed slice; parent task remains planned for further command/polling and state persistence splits.
- Changed files:
  - `BatteryCollectorCommandLogService`
  - `BatteryCollectorService`
  - `BatteryCollectorCommandLogServiceTest`
  - `BatteryCollectorServiceTest`
- Behavior change: none intended. Explicit module command opt-log creation and status updates keep the same call sites and field mapping, while mapper interaction and timestamp formatting moved into a focused command log service.
- Scope:
  - Extracted command opt-log creation for queued 600-module commands.
  - Extracted command opt-log status updates for rejected, success, failed, and timeout outcomes.
  - Kept module command queueing and response handling in `BatteryCollectorService`.
  - Added Chinese class and method comments for the new command log service.
- Added regression coverage:
  - Command opt-log field mapping for module command creation.
  - Empty request payload remains null in the opt-log record.
  - Timeout status writes the expected error message.
  - Null opt-log id skips update.
- Verification:
  - `mvn "-DskipTests=false" "-Dmaven.test.skip=false" "-Dtest=BatteryCollectorServiceTest,BatteryCollectorProtocolLogServiceTest,BatteryCollectorCommandLogServiceTest" test`

## 16. Execution result: TASK-BAT-COLLECTOR-STRUCT-002 slice 5

- Date: 2026-06-16
- Status: completed slice; parent task remains planned for further command/polling splits and final dead-code cleanup.
- Changed files:
  - `BatteryCollectorDeviceStateService`
  - `BatteryCollectorService`
  - `BatteryCollectorDeviceStateServiceTest`
  - `BatteryCollectorServiceTest`
- Behavior change: none intended. Device-state persistence, deduplication, module timeout recovery, module active state, and group-246 freshness state keep the same value mapping and call sites, while state construction and dedup cache ownership moved into a focused device-state service.
- Scope:
  - Extracted channel error/open/timeout state persistence.
  - Extracted module timeout, module active, and group-246 freshness state persistence.
  - Moved device-state dedup cache ownership out of `BatteryCollectorService`.
  - Kept polling, command execution, and address-cache mutation in `BatteryCollectorService`.
  - Added Chinese class and method comments for the new device-state service.
- Added regression coverage:
  - Serial-port state deduplication and recovery boundary.
  - Channel error deduplication and clear-after-open behavior.
  - Module timeout deduplication and recovered status after response.
  - Battery-group-scoped dedup cache cleanup.
  - High-cardinality state-cache key classification.
- Verification:
  - `mvn "-DskipTests=false" "-Dmaven.test.skip=false" "-Dtest=BatteryCollectorServiceTest,BatteryCollectorCacheServiceTest,BatteryCollectorDeviceStateServiceTest,BatteryCollectorProtocolLogServiceTest,BatteryCollectorCommandLogServiceTest" test`

## 17. Functional-first execution plan update

- Date: 2026-06-17
- Decision: switch the next execution focus from structural refactoring to feature delivery. Keep already completed structure slices, but postpone additional collector main-flow splits, dead-code cleanup, and directory reshaping unless they are necessary to implement a functional task safely.
- Directory rule for new functions: if a feature adds meaningful behavior, create or reuse a focused service/package by capability instead of expanding `BatteryCollectorService`.
  - External read/cache features: `collector/battery/service/snapshot` or focused `*Snapshot*Service`.
  - JSON/TCP adaptation features: `collector/battery/service/jsontcp` or focused adapter services.
  - Modbus mapping/read features: existing `BatteryModuleModbusReadMappingService` plus focused helper classes only when register groups grow.
  - Postprocess features: existing `collector/battery/service/postprocess`.
  - Command/control features: existing command services; avoid putting new command semantics into the polling loop.

### 17.1 Functional priority queue

| Priority | Task | Status | Scope |
|---|---|---|---|
| P0 | `TASK-BAT-FUNC-SNAPSHOT-001` | Planned | Finish the external-read realtime snapshot path: after collection persists realtime data, refresh a high-frequency readable cache; read paths should prefer fresh cache and fall back by freshness rules. |
| P0 | `TASK-BAT-FUNC-FRESHNESS-001` | Planned | Lock the cell freshness rule: when current round misses cells, fill missing modelNum entries from the previous valid round, limited by `BatteryPack.batSinSize`; select by modelNum order and do not renumber cells. |
| P0 | `TASK-BAT-FUNC-MODBUS-CACHE-001` | Planned | Ensure Modbus high-frequency reads use the snapshot/cache path without repeated mapper hits; preserve first-data-not-ready exception and zero-fill-after-ready behavior. |
| P1 | `TASK-BAT-FUNC-JSONTCP-001` | Planned | Compare JSON/TCP exposed fields with the standard realtime model, then implement guarded read-source switching behind `jsonTcpRealtimeSourceEnabled`; keep fallback to old compatible data. |
| P1 | `TASK-BAT-FUNC-STATUS-001` | Planned | Use `battery_device_state` and realtime snapshot consistently for channel/module/group status exposed to page, JSON/TCP, and Modbus; avoid temporary in-memory caches as external contracts. |
| P1 | `TASK-BAT-FUNC-ALARM-001` | Planned | Complete alarm output consistency from standard alarm/postprocess results to external reads; do not reintroduce old 87/8D payload parsing into new read paths. |
| P2 | `TASK-BAT-FUNC-CAPACITY-001` | Planned | Keep SOC/SOH/capacity values postprocess/cache-backed; only add feature work where existing predictor/cache inputs are present and testable. |
| P2 | `TASK-BAT-FUNC-COMMAND-001` | Planned | Fill any remaining command result/status gaps found by functional tests, especially opt-log and mode-state consistency for JSON/TCP and future Modbus control entry points. |

### 17.2 Refactor backlog, explicitly postponed

| Priority | Task | Status | Scope |
|---|---|---|---|
| P3 | `TASK-BAT-COLLECTOR-STRUCT-002-CLEANUP` | Deferred | Remove old private methods and unused fields left in `BatteryCollectorService` after service extraction; do this only after current functional slices are stable. |
| P3 | `TASK-BAT-COLLECTOR-STRUCT-002-POLLING` | Deferred | Split polling-loop orchestration from `BatteryCollectorService`; postpone unless a functional change needs it. |
| P3 | `TASK-BAT-COLLECTOR-STRUCT-002-COMMANDQUEUE` | Deferred | Split command queue execution from `BatteryCollectorService`; postpone unless command feature work becomes risky without it. |
| P3 | `TASK-BAT-DIR-STRUCT-001` | Deferred | Broader package/directory reshaping; only apply incrementally when adding new feature files, not as a standalone reshuffle. |

### 17.3 Next recommended task

Start with `TASK-BAT-FUNC-SNAPSHOT-001` and `TASK-BAT-FUNC-FRESHNESS-001` as one small functional slice if the existing snapshot service already has the needed extension points. If the current code requires larger changes, split them:

1. `TASK-BAT-FUNC-FRESHNESS-001A`: add unit-tested cell merge/fill helper with `batSinSize` and modelNum ordering.
2. `TASK-BAT-FUNC-SNAPSHOT-001A`: wire the helper into snapshot refresh after realtime persistence.
3. `TASK-BAT-FUNC-MODBUS-CACHE-001A`: verify Modbus reads consume the refreshed snapshot under high-frequency access.
