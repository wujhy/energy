package com.shanhe.project.energy.stat.domain;


import com.shanhe.project.device.config.domain.BatteryMonitor;
import com.shanhe.project.device.config.domain.MonitorData;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 单体电池统计对象 stat_battery
 *
 * @author zhoubin
 * @date 2025-07-15
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class StatBatteryBat extends MonitorData {
    private static final long serialVersionUID = 1L;

    private Long id;

    // 电池组数据包ID
    private Long configId;

    // 电池组数据包ID
    private Long packId;

    // 蓄电池组编号，1,2,3,4
    private Integer packNum;

    // 单体电池编号
    private Integer batNum;

    // 电压值（ 2 字节）单位：V
    private Double voltage;

    // 内阻值（ 2 字节）单位：uΩ
    private Integer resistance;

    // 温度值（ 2 字节）单位：℃
    private Double temperature;

    // 电池核容值  （ 2 字节，1 位小数，单位 AH）
    private Double bcapacity;


    /**
     * 采集时间记录时间点，秒
     */
    private int timeInSeconds;



    private String isAsc = "desc";

    public static StatBatteryBat of(BatteryMonitor batteryInfo) {
        StatBatteryBat statBattery = new StatBatteryBat();
        statBattery.setConfigId(batteryInfo.getConfigId());
        statBattery.setPackNum(batteryInfo.getPackNum());
        statBattery.setBatNum(batteryInfo.getBatNum());
        statBattery.setVoltage(batteryInfo.getVoltage());
        statBattery.setResistance(batteryInfo.getResistance());
        statBattery.setTemperature(batteryInfo.getTemperature());
        statBattery.setBcapacity(batteryInfo.getBcapacity());
        return statBattery;
    }
}
