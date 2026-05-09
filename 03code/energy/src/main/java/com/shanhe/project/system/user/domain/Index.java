package com.shanhe.project.system.user.domain;

import lombok.Data;

/**
 * 首页信息
 *
 * @author wjh
 * @since 2025/2/17
 */
@Data
public class Index {
    /**
     * 系统名称
     */
    String name;
    /**
     * 系统版本
     */
    String version;
    /**
     * 安全天数
     */
    Long safeDays;
    /**
     * 是否有巡检
     */
    /**
     * 用户管理-账号初始密码
     */
    String initPassword;
    /**
     * 用户管理-密码字符范围
     */
    String chrType;
    /**
     * 告警数量
     */
    Long alarmNum;
    /**
     * 告警设备数量
     */
    Long alarmDeviceNum;
    /**
     * 温度
     */
    String wd;
    /**
     * 湿度
     */
    String sd;
}
