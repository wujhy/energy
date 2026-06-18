# scheduled 包 — 蓄电池相关定时任务归属

更新时间：2026-06-18

## 蓄电池相关任务

| 任务 | 类 | 调用链路 | 说明 |
|---|---|---|---|
| 设备在线检测 | DeviceOnlineJob | 新链路（BatteryDeviceStateService）+ 旧链路（BatteryReportLogService） | 读取 battery_device_state 判断组在线状态；告警上下文使用 BatteryReportLogService |
| 日志清理 | CleanLogJob | 新链路（BatteryDeviceStateService）+ 旧链路（BatteryReportLogService） | 清理过期状态记录和旧报告日志 |
| 缓存初始化 | CacheInit | 旧链路（BatteryReportLogService） | 启动时初始化电池组缓存 |
| 缓存刷新 | CacheJob | 旧链路（BatteryReportLogService） | 定期刷新电池组缓存 |

## 新链路依赖

- `BatteryDeviceStateService` — 设备当前态查询和清理
- `BatteryCollectorDeviceStateService` — 采集通道状态持久化（间接通过 BatteryDeviceStateService）

## 旧链路依赖

- `BatteryReportLogService` — 旧 `dev_battery_report_log` 兼容历史查询

## 后续方向

- DeviceOnlineJob 的告警上下文应逐步从 BatteryReportLogService 切到标准实时模型
- CleanLogJob 的旧报告日志清理应配合 `compatReportLogEnabled` 开关
- CacheInit/CacheJob 的缓存逻辑应评估是否仍需要
