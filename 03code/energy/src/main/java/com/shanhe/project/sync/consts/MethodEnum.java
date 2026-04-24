package com.shanhe.project.sync.consts;

import cn.hutool.core.util.StrUtil;
import lombok.Getter;

/**
 * 告警等级
 *
 * @author wjh
 * @since 2024/12/19
 */
@Getter
public enum MethodEnum {

    _1("join", "注册"),
    _2("heartbeat", "心跳"),
    _3("uploadData", "上传实时数据"),
    _4("uploadAlarm", "上传告警数据"),
    _5("synDev", "同步设备信息"),
    _6("synDevRes", "同步设备信息响应"),
    _7("delDev", "删除设备信息"),
    _8("delDevRes", "删除设备信息响应"),
    _9("synDevPort", "同步串口信息"),
    _10("synDevPortRes", "同步串口信息响应"),
    _11("editDevIp", "修改设备IP"),
    _12("editDevIpRes", "修改设备IP响应"),
    _13("editServerIp", "修改设备服务器IP"),
    _14("editServerIpRes", "修改设备服务器IP响应"),
    _15("sysDevDate", "同步设备时间"),
    _16("sysDevDateRes", "同步设备时间响应"),
    _17("synAlarmConfigItem", "同步测点参数"),
    _18("editAlarmConfigItemRes", "同步测点参数响应"),
    _19("delAlarmConfigItem", "删除测点参数"),
    _20("delAlarmConfigItemRes", "删除测点参数响应"),
    _21("synAlarmOrder", "同步设备指令"),
    _22("synAlarmOrderRes", "同步设备指令响应"),
    _23("delAlarmOrder", "删除设备指令"),
    _24("delAlarmOrderRes", "删除设备指令响应"),
    _25("shieldAlarm", "屏蔽告警内容"),
    _26("shieldAlarmRes", "屏蔽告警内容响应"),
    _27("controlDev", "控制设备"),
    _28("controlDevRes", "控制设备响应"),
    _29("updateSoft", "下发升级指令"),
    _30("updateSoftRes", "下发升级指令响应"),
    _31("updateSoftResult", "升级成功"),
    _32("uploadPatrol", "上报巡检结果"),
    _33("uploadAlarmConfigItem", "上报测点数据"),
    _34("reportSynAlarmConfigItem", "同步主机测点参数"),
    _35("reportSynDev", "同步设备信息"),
    _36("uploadDev", "上传设备数据"),
    _37("reportSynAlarmConfigItemRes", "同步主机测点参数响应"),
    _38("reportSynDevRes", "同步设备信息响应"),
    _39("getPatrolTemplate", "获取巡检清单"),
    _40("getPatrolTemplateRes", "下发巡检清单"),
    _41("updateCmdDebug", "上报调试指令结果"),
    _42("uploadBatteryOpt", "上报测试计划"),
    _43("syncBatteryOpt", "下发测试计划"),

    _44("syncBatteryMonomer", "下发内阻初装值"),
    _45("syncBatteryMonomerRes", "下发内阻初装值响应"),

    _46("reportSynBatteryMonomer", "同步内阻初装值"),
    _47("reportSynBatteryMonomerRes", "同步内阻初装值响应"),

    _48("uploadBatteryMonomer", "上报内阻初装值"),
    _49("uploadPreBatteryGroup", "上报预估容量"),
    _98("sign", "安全认证"),
    _99("other", "其他");

    private final String dictValue;

    private final String dictLabel;

    MethodEnum(String dictValue, String dictLabel) {
        this.dictValue = dictValue;
        this.dictLabel = dictLabel;
    }

    /**
     * 转枚举
     */
    public static MethodEnum fromCode(String code) {
        if (StrUtil.isBlank(code)) {
            return _99;
        }
        for (MethodEnum item : values()) {
            if (item.getDictValue().equals(code)) {
                return item;
            }
        }
        return _99;
    }
}
