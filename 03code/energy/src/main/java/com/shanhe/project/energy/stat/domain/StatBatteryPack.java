package com.shanhe.project.energy.stat.domain;

import com.shanhe.project.device.config.domain.MonitorData;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 电池组统计对象 stat_battery_pack
 *
 * @author zhoubin
 * @date 2025-07-15
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class StatBatteryPack extends MonitorData {
    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    private Long id;
    /**
     * 配置ID
     */
    private Long configId;

    // 蓄电池主编号，1,2,3,4
    private Integer packNum;

    // 电池组组压  单位：V
    private Double packVoltage;

    // 电池组充放电电流 单位：A
    private Double packCurrent;

    // 电池组浮充电流 单位：A
    private Double batteryPackFloatCurrent;

    // 环境温度1单位：℃
    private Double environmentTemperature1;

    // 环境温度2,单位：℃
    private Double environmentTemperature2;

    // 电池组容量
    private Double bcapacity;

    // 氢气浓度  单位：%
    private Double hydrogenConcentration;

    // 单体数据
    private List<StatBatteryBat> statBatteryList;


    private String isAsc = "desc";
    private Integer type;


    // 导出路径
    private String exportPath;
}
