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

    /** 作用域类型。 */
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

    /** 状态码。 */
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

    /** 状态来源。 */
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

    /** 命令状态（dev_opt_log.status）。 */
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

    /** 状态等级。 */
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

    /**
     * 命令日志错误原因常量（dev_opt_log.error_message）。
     * <p>
     * 集中管理 600 模块命令失败/取消/超时时写入 error_message 的文案，
     * 替换散落在各服务中的硬编码字符串。
     */
    public static final class CommandErrorReason {
        /** 命令响应超时（pending 等待超过 timeoutMs）。 */
        public static final String TIMEOUT = "命令响应超时";
        /** 命令队列拒绝（队列已满或 offer 失败）。 */
        public static final String REJECTED = "命令队列拒绝";
        /** 命令已取消（手动停止或队列清理）。 */
        public static final String CANCELLED = "命令已取消";
        /** 命令响应失败（响应码不匹配或 payload 失败标志）。 */
        public static final String FAILED = "命令响应失败";
        /** 命令执行失败（兜底，未知原因）。 */
        public static final String GENERIC_FAILED = "命令执行失败";
        /** 采集通道关闭，pending 命令未完成。 */
        public static final String CHANNEL_CLOSED_PENDING = "采集通道关闭，命令未完成";
        /** 采集通道关闭，队列命令未下发。 */
        public static final String CHANNEL_CLOSED_QUEUED = "采集通道关闭，命令未下发";

        private CommandErrorReason() {
        }

        /**
         * 拼接基础原因和可选的响应码/payload 后缀。
         *
         * @param baseReason     基础原因文案
         * @param responseCode   响应码（可为 null）
         * @param responsePayload 响应 payload hex（可为 null）
         * @return 拼接后的完整错误原因
         */
        public static String compose(String baseReason, Integer responseCode, String responsePayload) {
            StringBuilder sb = new StringBuilder(baseReason);
            if (responseCode != null) {
                sb.append(", responseCode=").append(responseCode);
            }
            if (responsePayload != null) {
                sb.append(", payload=").append(responsePayload.trim());
            }
            return sb.toString();
        }
    }
}
