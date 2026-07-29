package com.shanhe.project.collector.battery.postprocess;

import com.shanhe.project.collector.battery.model.BatteryModuleCellRealtime;
import com.shanhe.project.collector.battery.model.BatteryAlarmEvaluationContext;
import com.shanhe.project.collector.battery.model.BatteryModuleGroupRealtime;
import com.shanhe.project.collector.battery.model.BatteryCollectorChannelConfig;
import com.shanhe.project.collector.battery.model.BatteryModuleRealtimeSnapshot;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 实时数据后处理上下文。
 *
 * @author wjh
 * @since 2026-06-04
 */
@Data
@Builder
public class BatteryRealtimePostProcessContext {

    /** 电池组编号。 */
    private Integer packNum;

    /** 数据来源：collector（600采集）/ jsonTcp / modbus / auto。 */
    private String source;

    /** 采集批次号。 */
    private String pollBatchNo;

    /** 单体实时数据列表。 */
    private List<BatteryModuleCellRealtime> cells;

    /** 组实时数据。 */
    private BatteryModuleGroupRealtime group;

    /** 采集通道配置。 */
    private BatteryCollectorChannelConfig channelConfig;

    /** 标准实时有效快照。 */
    private BatteryModuleRealtimeSnapshot realtimeSnapshot;

    /** 告警候选上下文。 */
    private BatteryAlarmEvaluationContext alarmContext;
}
