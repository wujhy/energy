package com.shanhe.project.collector.battery.model;

/**
 * battery_device_state 和 dev_opt_log 状态相关常量。
 * <p>
 * 消除裸字符串扩散，为状态入库、页面和 Modbus 读取提供稳定语义。
 *
 * @author wjh
 * @since 2026-06-02
 */
public final class BatteryDeviceStateConstants {

    private BatteryDeviceStateConstants() {
    }

    /**
     * 作用域类型。
     */
    public static final class ScopeType {
        /** 系统级。 */
        public static final String SYSTEM = "system";
        /** 采集通道级。 */
        public static final String CHANNEL = "channel";
        /** 电池组级。 */
        public static final String PACK = "pack";
        /** 模块级。 */
        public static final String MODULE = "module";

        private ScopeType() {
        }
    }

    /**
     * 状态码。
     */
    public static final class StateCode {
        /** 设备在线状态。 */
        public static final String ONLINE = "ONLINE";
        /** 通道串口状态。 */
        public static final String CHANNEL_OPEN = "CHANNEL_OPEN";
        /** 通道异常。 */
        public static final String CHANNEL_ERROR = "CHANNEL_ERROR";
        /** 通道轮询超时累计计数。 */
        public static final String CHANNEL_TIMEOUT_COUNT = "CHANNEL_TIMEOUT_COUNT";
        /** 模块超时/无响应。 */
        public static final String MODULE_TIMEOUT = "MODULE_TIMEOUT";
        /** 模块活跃状态。 */
        public static final String MODULE_ACTIVE = "MODULE_ACTIVE";
        /** 246 组模块新鲜度。 */
        public static final String GROUP_246_FRESHNESS = "GROUP_246_FRESHNESS";
        /** 工作模式。 */
        public static final String WORK_MODE = "WORK_MODE";
        /** 显式命令状态。 */
        public static final String COMMAND_STATUS = "COMMAND_STATUS";

        private StateCode() {
        }
    }

    /**
     * 状态来源。
     */
    public static final class Source {
        /** 600 采集模块端。 */
        public static final String COLLECTOR = "collector";
        /** 设备在线检测任务。 */
        public static final String DEVICE_ONLINE_JOB = "DeviceOnlineJob";
        /** 工作模式状态服务。 */
        public static final String MODE_STATUS = "BatteryModeStatusService";
        /** Web 页面。 */
        public static final String WEB = "web";
        /** JSON/TCP 协议。 */
        public static final String JSON_TCP = "json_tcp";
        /** Modbus 协议。 */
        public static final String MODBUS = "modbus";
        /** 自动任务。 */
        public static final String AUTO = "auto";

        private Source() {
        }
    }

    /**
     * 命令状态（dev_opt_log.status）。
     */
    public static final class CommandStatus {
        /** 等待执行。 */
        public static final String PENDING = "pending";
        /** 执行成功。 */
        public static final String SUCCESS = "success";
        /** 执行失败。 */
        public static final String FAILED = "failed";
        /** 响应超时。 */
        public static final String TIMEOUT = "timeout";
        /** 队列拒绝。 */
        public static final String REJECTED = "rejected";
        /** 已取消。 */
        public static final String CANCELLED = "cancelled";

        private CommandStatus() {
        }
    }

    /**
     * 状态等级。
     */
    public static final class StateLevel {
        /** 正常。 */
        public static final String NORMAL = "normal";
        /** 告警。 */
        public static final String WARN = "warn";
        /** 错误。 */
        public static final String ERROR = "error";
        /** 运行中。 */
        public static final String RUNNING = "running";

        private StateLevel() {
        }
    }
}
