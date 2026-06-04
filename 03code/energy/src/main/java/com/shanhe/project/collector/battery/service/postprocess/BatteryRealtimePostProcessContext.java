package com.shanhe.project.collector.battery.service.postprocess;

import com.shanhe.project.collector.battery.model.BatteryModuleCellRealtime;
import com.shanhe.project.collector.battery.model.BatteryModuleGroupRealtime;
import com.shanhe.project.device.config.domain.BatteryReportLog;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

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

    /** 旧报告日志（兼容模式下由 BatteryReportLog 提供）。 */
    private BatteryReportLog oldReportLog;

    /** 组参数映射（兼容模式下由 980 解析提供）。 */
    private Map<String, Object> packMap;

    /** 单体列表（兼容模式下由 980 解析提供）。 */
    private List<?> batteryList;

    /** 是否为兼容历史写入模式。 */
    private boolean compatInsert;
}
