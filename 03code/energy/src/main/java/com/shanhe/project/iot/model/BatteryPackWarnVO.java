package com.shanhe.project.iot.model;

import lombok.Data;

import java.util.Date;

/**
 * 电池组报警实时对象
 */
@Data
public class BatteryPackWarnVO {
    /** 设备ID */
    private Long devId;
    /** 告警级别 */
    private String level;
    /** 组电压过充告警 */
    private String zdygc;
    /** 组电压过放告警 */
    private String zdygf;
    /** 环境高温告警 */
    private String zwdg;
    /** 环境低温告警 */
    private String zwdd;
    /** 总体浮充电压过高告警 */
    private String zfcdygg;
    /** 总体浮充电压过低告警 */
    private String zfcdygd;
    /** 电流过充告警 */
    private String zcgdlgj;
    /** SOC低告警 */
    private String zsocdgj;
    /** SOH低告警 */
    private String zsohdgj;
    /** 组通信异常 */
    private String txzt;
    /** 环境温度传感器1故障 */
    private String zwdcgq1gz;
    /** 环境温度传感器2故障 */
    private String zwdcgq2gz;
    /** 网络故障 */
    private String zwlgz;
    /** 停电告警 */
    private String ztdgj;
    /** 蓄电池脱离母线 */
    private String ztlmxgj;
    /** 最后更新时间 */
    private Date updateDate;
}
