# scheduled 包 — 蓄电池相关定时任务归属

更新时间：2026-07-29

## 蓄电池相关任务

| 任务 | 类 | 调用链路 | 说明 |
|---|---|---|---|
| 设备在线检测 | DeviceOnlineJob | 新链路（BatteryDeviceStateService）+ 告警服务 | 读取并写入 battery_device_state；离线时通过 alarmBatteryValue 触发 TXZT 告警落表，禁用或恢复在线时通过 alarmFix 恢复 |
| 日志清理 | CleanLogJob | 新链路（BatteryDeviceStateService）+ 旧链路（BatteryReportLogService） | 清理过期状态记录和旧报告日志 |
| 缓存初始化 | CacheInit | 旧链路（BatteryReportLogService） | 启动时初始化电池组缓存 |
| 缓存刷新 | CacheJob | 旧链路（BatteryReportLogService） | 定期刷新电池组缓存 |

## 新链路依赖

- `BatteryDeviceStateService` — 设备当前态查询和清理
- `BatteryCollectorDeviceStateService` — 采集通道状态持久化（间接通过 BatteryDeviceStateService）

## 旧链路依赖

- `BatteryReportLogService` — `dev_battery_report_log` 历史查询和清理

## 后续方向

- DeviceOnlineJob 已不依赖 BatteryReportLogService 构建告警上下文；后续通讯/离线告警继续按 ConfigAttribute 配置走 alarmValid 生命周期
- CleanLogJob 继续按历史保留天数清理 `dev_battery_report_log`，该表由实时采集固定写入用于历史回查
- CacheInit/CacheJob 的缓存逻辑应评估是否仍需要
