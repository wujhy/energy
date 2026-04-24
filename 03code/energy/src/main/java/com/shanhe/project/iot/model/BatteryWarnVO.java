package com.shanhe.project.iot.model;

import lombok.Data;

import java.util.Date;

/**
 * 单体电池报警实时对象
 */
@Data
public class BatteryWarnVO {
    /** 设备ID */
    private Long devId;
    /** 告警级别 */
    private String level;
    /** 单体电池编号 */
    private Integer modelNum;
    /** 单体电压过充告警 */
    private String dtdygc;
    /** 单体电压过放告警 */
    private String dtdygf;
    /** 内阻过大告警系数 */
    private String dtnzgd;
    /** 内阻过小告警系数 */
    private String dtnzgx;
    /** 内阻不均告警系数 */
    private String dtnzbj;
    /** 电池高温告警 */
    private String dtdcwdg;
    /** 电池低温告警 */
    private String dtdcwdd;
    /** 单体浮充电压过高告警 */
    private String dtfcdyg;
    /** 单体浮充电压过低告警 */
    private String dtfcdyd;
    /** 单体电压不均告警 */
    private String dtdybj;
    /** 单体电压极差告警 */
    private String dtdyjc;
    /** 连接条告警 */
    private String dtljtgj;
    /** 电池温度不均告警 */
    private String dtdcwdbj;
    /** 鼓包告警 */
    private String dtgb;
    /** 单体电池开路异常 */
    private String dtdckl;
    /** 单体通信异常 */
    private String dttxzt;
    /** 漏液告警 */
    private String dtlygj;
    /** 电池温度传感器故障 */
    private String dtwdcgqgz;
    /** 最后更新时间 */
    private Date updateDate;
}
