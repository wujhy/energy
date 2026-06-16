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
| P1 | `TASK-BAT-COLLECTOR-STRUCT-003` | Planned | Move realtime postprocess context construction into a small builder/factory if more processors require extra inputs. Avoid expanding `BatteryModuleRealtimeConsumer` with business rules. |

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
