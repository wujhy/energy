package com.shanhe.project.manage.capacity.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.Map;

/**
 * 电池预测组信息
 *
 * @author wjh
 * @since 2026-05-25
 */
@Data
public class PreBatteryGroup implements Cloneable, Serializable {

    /** 预测组ID */
    private Long id;
    /** 配置ID */
    private Long configId;
    /** 设备ID */
    private Long devId;
    /** 电池组编号 */
    private Integer packNum;
    /** 额定电压 */
    private Double acapacity;
    /** 电池组规格2V，12V */
    private Double spec;
    /** 最低电池编号 */
    private Integer minVoltageNum;
    /** 放电开始时间 */
    private Date startTime;
    /** 开始时间字符串。 */
    private String startTimeStr;
    /** 放电截止时间 */
    private Date endTime;
    /** 结束时间字符串。 */
    private String endTimeStr;
    /** 当前电流 */
    private Double current;
    /** 预估容量 */
    private Double bcapacity;

    /** 预估备电时长 */
    private Integer backUpDuration;
    /** 统计时间 */
    private Date staticTime;
    /** 静态时间字符串。 */
    private String staticTimeStr;

    /** SOH健康度 */
    private Double soh;
    /** 放电容量 */
    private Double dischargeCapacity;

    /** 单体电池数据JSON */
    private String mapBatteryData;
    /** 单体电池数据Map */
    private Map<String, PreBatteryVo> mapBattery;

    private static PreBatteryGroup preBatteryGroupInfo = new PreBatteryGroup();

    /**
     * 复制克隆对象
     *
     * @return
     */
    public static PreBatteryGroup getNewPreBatteryGroupInfo() {
        try {
            return (PreBatteryGroup) preBatteryGroupInfo.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }

}
