package com.shanhe.project.manage.stat.domain;


import com.shanhe.project.manage.config.domain.MonitorData;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 单体电池统计对象 stat_battery
 *
 * @author wjh
 * @since 2026-05-25
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class StatBatteryBat extends MonitorData {
    private static final long serialVersionUID = 1L;

    /** 主键 */
    private Long id;

    /** 设备ID */
    private Long configId;

    /** 电池组数据包ID */
    private Long packId;

    /** 蓄电池组编号，1,2,3,4 */
    private Integer packNum;

    /** 单体电池编号 */
    private Integer batNum;

    /** 电压值 单位：V */
    private Double voltage;

    /** 内阻值 单位：uΩ */
    private Integer resistance;

    /** 温度值 单位：℃ */
    private Double temperature;

    /** 电池核容值 单位：AH */
    private Double bcapacity;

    /** 采集时间记录时间点，秒 */
    private int timeInSeconds;

    /** 排序方式 */
    private String isAsc = "desc";
}
