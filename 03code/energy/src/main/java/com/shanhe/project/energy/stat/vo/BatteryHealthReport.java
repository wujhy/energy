package com.shanhe.project.energy.stat.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 电池健康报告
 *
 * @author wjh
 * @since 2026-05-25
 */
@Data
public class BatteryHealthReport implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 设备配置ID */
    private Long configId;
    /** 蓄电池组编号 */
    private Integer packNum;
    /** 蓄电池品牌 */
    private String batBrand;
    /** 蓄电池型号 */
    private String batModel;
    /** 单体规格 */
    private Integer batSinModel;
    /** 蓄电池额定容量 */
    private Double batCapacity;
    /** 蓄电池单体个数 */
    private Integer batSinSize;
    /** 投产时间 */
    private String productionTime;

    /** 预估备电时长 */
    private Integer backupDuration;
    /** 评估建议 */
    private String assessAdvice;
    /** SOH健康度 */
    private Double soh;
    /** SOH告警状态：0-告警，1-到达阈值，2-正常 */
    private Integer sohAlarm;

    /** 是否鼓包漏液告警 */
    private Integer isGblyAlarm;
    /** 是否鼓包告警 */
    private Integer isGbAlarm;
    /** 是否漏液告警 */
    private Integer isLyAlarm;

    /** 是否组高温告警 */
    private Integer isZwdgAlarm;
    /** 是否单体高温告警 */
    private Integer isBatWdgAlarm;
    /** 是否电压失衡告警 */
    private Integer isVoltageAlarm;
    /** 是否内阻超限告警 */
    private Integer isResistance;
    /** 是否内阻变化告警 */
    private Integer isResistanceChange;
    /** 是否内阻超限告警 */
    private Integer isResistanceTransfinite;

    /** 告警数量 */
    private Integer alarmCount;

    /** 评估因子 */
    private List<EvaluationFactors> evaluationFactors;

}
