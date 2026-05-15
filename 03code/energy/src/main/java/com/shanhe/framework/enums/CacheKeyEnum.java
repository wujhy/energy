package com.shanhe.framework.enums;

import lombok.Getter;

/**
 * 系统缓存键
 *
 * @author wjh
 * @since 2025/3/18
 */
@Getter
public enum CacheKeyEnum {

    LOGIN("sys-authCache", "login:%s", "登录信息"),
    HOST("sys-cache", "device:host", "站点信息"),
    HOST_TOKEN("sys-cache", "device:host:token", "站点token信息"),
    WARN("device-warn", "device:warn", "站点告警"),
    /* config.类型.端口号.通道号 */
    CONFIG_ONLINE("sys-cache", "config:%s:%s:%s", "设备在线"),
    BATTERY_ONLINE("sys-cache", "battery:online:%s", "电池组在线"),
    /* attribute.配置id.组.属性编码 */
    ATTRIBUTE("device-attribute", "attribute:%s:%s:%s", "设备属性"),
    /* alarm.配置id.包编号.模块编号.属性编码 */
    ALARM("device-alarm", "alarm:%s:%s:%s:%s", "设备告警"),
    /* log:configId:packNum:type */
    OPT_LOG("device-log", "log:%s:%s:%s", "操作日志处理"),
    /* alarm.level */
    ALARM_LEVEL("sys-cache", "alarm:level", "告警等级"),
    STORAGE_TIME("data-storage-time", "storageTime:%s:%s", "数据最后存储时间"),
    /* battery.配置id.包编号.模块编号 */
    BATTERY("device-battery", "battery:%s:%s:%s", "单体电池"),
    /* battery.配置id.包编号 */
    BATTERY_REPORT("device-battery-report", "battery:%s:%s", "蓄电池实时日志"),
    /* comm:sticky */
    STICKY("comm-data", "comm:sticky", "粘包数据处理"),
    /* device:result:configId:packNum:C3 */
    RESULT("device-result", "device:result:%s:%s:%s", "返回结果处理"),
    /* result:C0:C1:C2:C3 */
    RESULT_CX("device-result", "result:%s:%s:%s:%s", "返回结果处理"),
    RESULT_DEBUG("device-result", "result:debug", "返回测试结果处理"),
    /* result:packNum:C0:C1:C2:C3:packNum */
    RESULT_PACK_NUM("device-result", "result:%s:%s:%s:%s:%s", "蓄电池返回结果处理"),
    /* device:result:alarm:configId:packNum */
    RESULT_SYN_ALARM("device-result", "result:alarm:%s:%s", "返回同步告警结果处理"),
    /* deploy:download */
    DEPLOY_DOWNLOAD("sys-cache", "deploy:download", "软件升级下载"),
    /* deploy:download */
    DEPLOY_STATUS("sys-cache", "deploy:status", "软件升级状态"),
    /* battery.设备ID.包编号 */
    BATTERY_PACK_INFO("device-battery", "battery:info:%s:%s", "蓄电池组数据"),
    STAT_BATTERY_GROUP("stat-battery", "stat:battery:group:%s:%s", "电池组预估容量");

    private final String cache;
    private final String key;
    private final String msg;

    CacheKeyEnum(String cache, String key, String msg) {
        this.cache = cache;
        this.key = key;
        this.msg = msg;
    }
}
