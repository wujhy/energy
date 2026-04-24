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
    private int batteryPackNumber;  //电池组编号
    private String batteryPackSum;  //电池组数量
    private String shieldAlarmStatus;  //屏蔽告警状态
    private String captcha;  //验证码
    private Integer alarmBatterySum;  //单体电池报警数量
    private JSONObject packStatus;  //组状态
    private JSONArray packBatteryStatus;  //组电池状态
    private JSONArray deviceFaultBatteryStatus;  //设备故障电池状态
    private String deviceFaultStatus;  //设备故障状态
}
