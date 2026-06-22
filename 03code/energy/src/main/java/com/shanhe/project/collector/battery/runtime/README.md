# collector.battery.runtime 包职责

采集运行态服务。

## 类清单

| 类 | 职责 |
|---|---|
| BatteryCollectorFrameIoService | 串口帧收发协调：帧发送、字节接收、接收缓冲管理 |
| BatteryCollectorPollingService | 轮询循环编排：通道遍历、全量发现、地址列表、批次管理 |
| BatteryCollectorTimeoutService | 超时判断与重试协调：pending 超时检测、重试发送、超时清理 |

## 不负责

- 不负责业务命令选择（归 command 包）
- 不负责后处理（归 postprocess 包）
- 不负责页面/Modbus 查询（归 external 包）
- 不负责设备状态持久化（归 state 包）
