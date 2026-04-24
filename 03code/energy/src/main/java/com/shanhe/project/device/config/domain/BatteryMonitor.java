package com.shanhe.project.device.config.domain;

import com.shanhe.framework.web.domain.BaseEntity;
import com.shanhe.project.device.alarm.domain.AlarmLog;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 单体电池数据
 *
 * @author wjh
 * @since 2025/3/4
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class BatteryMonitor extends BaseEntity {
    private static final long serialVersionUID = 1L;

    /** 电池ID */
    private Long id;
    /** 设备id */
    private Long configId;
    /** 蓄电池组编号，1,2,3,4 */
    private Integer packNum;
    /** 单体电池编号 */
    private Integer batNum;
    /** 哈希值 */
    private String hashCode;
    /** 电压值（2字节）单位：V */
    private Double voltage;
    /** 内阻值（2字节）单位：uΩ */
    private Integer resistance;
    /** 温度值（2字节）单位：℃ */
    private Double temperature;
    /** 电池核容值  （2字节，1位小数，单位 AH） */
    private Double bcapacity;
    /** 电池连接条电阻（2字节）, 单位：uΩ */
    private Double resistancerageslip;
    /** 内阻变化率（2字节）, 单位：uΩ */
    private Double resistanceRateChange;
    /** 电池鼓包电压值（2字节）, 单位：mV,3000是未接鼓包传感器 */
    private Double gbvoltage;

    /** 告警列表 */
    private List<AlarmLog> alarmList;

    /* 值
    public String getHashCode() {
        if (StrUtil.isBlank(this.hashCode)) {
            String hashData = String.format("%s,%s,%s,%s,%s,%s,%s,%s", configId, packNum, batNum, voltage, resistance, temperature, bcapacity, resistancerageslip);
            this.hashCode = String.valueOf(hashData.hashCode());
        }
        return hashCode;
    }*/
}
