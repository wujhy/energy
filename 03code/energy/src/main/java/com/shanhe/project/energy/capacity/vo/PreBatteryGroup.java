package com.shanhe.project.energy.capacity.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.Map;

/**
 * 电池预测组信息
 */
@Data
public class PreBatteryGroup implements Cloneable, Serializable {

    // 预测组ID
    private Long id;
    // 配置ID
    private Long configId;
    // 配置ID
    private Long devId;
    // 电池组编号
    private Integer packNum;
    //额定电压
    private Double acapacity;
    //电池组规格2V，12V
    private Double spec;
    //最低电池编号
    private Integer minVoltageNum;
    //放电开始时间
    private Date startTime;
    private String startTimeStr;
    //放电截止时间
    private Date endTime;
    private String endTimeStr;
    //当前电流
    private Double current;
    //预估容量
    private Double bcapacity;

    //预估备电时长
    private Integer backUpDuration;
    //统计时间
    private Date staticTime;
    private String staticTimeStr;

    // soh
    private Double soh;
    // 放电容量
    private Double dischargeCapacity;


    //单体电池
    private String mapBatteryData;
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
            e.printStackTrace();
        }
        return new PreBatteryGroup();
    }

}
