package com.shanhe.project.iot.model;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import lombok.Data;

import java.io.Serializable;

/**
 * 蓄电池告警
 *
 * @author wjh
 * @since 2025/4/14
 */
@Data
public class BatteryWarnInfo implements Serializable
{
    private static final long serialVersionUID = 1L;

    /** 电池组编号 */
    private int batteryPackNumber;
    /** 电池组数量 */
    private String batteryPackSum;
    /** 屏蔽告警状态 */
    private String shieldAlarmStatus;
    /** 验证码 */
    private String captcha;
    /** 单体电池报警数量 */
    private Integer alarmBatterySum;
    /** 组状态 */
    private JSONObject packStatus;
    /** 组电池状态 */
    private JSONArray packBatteryStatus;
    /** 设备故障电池状态 */
    private JSONArray deviceFaultBatteryStatus;
    /** 设备故障状态 */
    private String deviceFaultStatus;
}
