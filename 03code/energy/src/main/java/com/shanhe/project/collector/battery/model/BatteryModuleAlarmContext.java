package com.shanhe.project.collector.battery.model;

import lombok.Data;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 600节模块端告警适配上下文。
 *
 * @author wjh
 * @since 2026-04-30
 */
@Data
public class BatteryModuleAlarmContext {

    /**
     * 电池组编号。
     */
    private Integer packNum;

    /**
     * 组级告警候选，key 为旧告警 itemCode，value 为告警值。
     */
    private Map<String, String> packWarnParam = new LinkedHashMap<>();

    /**
     * 单体告警候选，第一层 key 为单体编号，第二层 key 为旧告警 itemCode。
     */
    private Map<Integer, Map<String, String>> cellWarnParam = new LinkedHashMap<>();

    /**
     * 增加组级告警候选。
     *
     * @param itemCode 旧告警编码
     * @param value 告警值
     */
    public void putPackWarn(String itemCode, String value) {
        packWarnParam.put(itemCode, value);
    }

    /**
     * 增加单体告警候选。
     *
     * @param batNum 单体编号
     * @param itemCode 旧告警编码
     * @param value 告警值
     */
    public void putCellWarn(Integer batNum, String itemCode, String value) {
        if (batNum == null) {
            return;
        }
        cellWarnParam.computeIfAbsent(batNum, key -> new LinkedHashMap<>()).put(itemCode, value);
    }

    /**
     * 是否不存在告警候选。
     *
     * @return 是否为空
     */
    public boolean isEmpty() {
        return packWarnParam.isEmpty() && cellWarnParam.isEmpty();
    }
}
