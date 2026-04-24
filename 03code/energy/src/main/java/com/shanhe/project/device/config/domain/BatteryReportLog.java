package com.shanhe.project.device.config.domain;

import com.shanhe.project.device.alarm.domain.AlarmLog;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;
import java.util.Map;

/**
 * 蓄电池上报日志
 *
 * @author wjh
 * @since 2025/7/9
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class BatteryReportLog extends MonitorData {
    /**
     * 主键
     */
    private Long id;
    /**
     * 设备ID
     */
    private Long configId;
    /**
     * 包序号
     */
    private Integer packNum;

    /**
     * 包参数
     */
    private String packData;
    private Map<String, Object> packParam;
    /**
     * 单体参数
     */
    private String monitorData;
    private List<BatteryMonitor> batteryList;
    /**
     * 扩展记录
     */
    private String extendData;

    /** 是否告警 0-是，1-否 */
    private Integer alarm;

    /** 告警列表 */
    private List<AlarmLog> alarmList;





    // 导出路径
    private String exportPath;
}