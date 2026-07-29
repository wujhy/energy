package com.shanhe.project.collector.battery.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 600节模块端告警适配上下文。
 *
 * @author wjh
 * @since 2026-04-30
 */
@Data
public class BatteryAlarmEvaluationContext {

    /** 电池组编号。 */
    private Integer packNum;

    /** 组级告警候选，key 为旧告警 itemCode，value 为告警值。 */
    private Map<String, String> packWarnParam = new LinkedHashMap<>();

    /** 单体告警候选，第一层 key 为单体编号，第二层 key 为旧告警 itemCode。 */
    private Map<Integer, Map<String, String>> cellWarnParam = new LinkedHashMap<>();

    /** 组级状态告警候选。 */
    private Map<String, String> packStatusWarnParam = new LinkedHashMap<>();

    /** 组级阈值告警候选。 */
    private Map<String, String> packThresholdWarnParam = new LinkedHashMap<>();

    /** 单体状态告警候选。 */
    private Map<Integer, Map<String, String>> cellStatusWarnParam = new LinkedHashMap<>();

    /** 单体阈值告警候选。 */
    private Map<Integer, Map<String, String>> cellThresholdWarnParam = new LinkedHashMap<>();

    /** 用于阈值评估的实时快照是否新鲜。 */
    private Boolean snapshotFresh;

    /** 当前轮询批次上报的单体编号。 */
    private List<Integer> currentBatchCellNums = new ArrayList<>();

    /** 从旧数据保留、且本轮阈值评估跳过的单体编号。 */
    private List<Integer> staleCellNums = new ArrayList<>();

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
     * 增加组级状态告警候选。
     *
     * @param itemCode 旧告警编码
     * @param value 告警值
     */
    public void putPackStatusWarn(String itemCode, String value) {
        packStatusWarnParam.put(itemCode, value);
        putPackWarn(itemCode, value);
    }

    /**
     * 增加组级阈值告警候选。
     *
     * @param itemCode 旧告警编码
     * @param value 告警值
     */
    public void putPackThresholdWarn(String itemCode, String value) {
        packThresholdWarnParam.put(itemCode, value);
        putPackWarn(itemCode, value);
    }

    /**
     * 增加单体告警候选。
     *
     * @param batNum 单体编号
     * @param itemCode 旧告警编码
     * @param value 告警值
     */
    public void putCellWarn(Integer batNum, String itemCode, String value) {
        putCellWarn(cellWarnParam, batNum, itemCode, value);
    }

    /**
     * 增加单体状态告警候选。
     *
     * @param batNum 单体编号
     * @param itemCode 旧告警编码
     * @param value 告警值
     */
    public void putCellStatusWarn(Integer batNum, String itemCode, String value) {
        putCellWarn(cellStatusWarnParam, batNum, itemCode, value);
        putCellWarn(batNum, itemCode, value);
    }

    /**
     * 增加单体阈值告警候选。
     *
     * @param batNum 单体编号
     * @param itemCode 旧告警编码
     * @param value 告警值
     */
    public void putCellThresholdWarn(Integer batNum, String itemCode, String value) {
        putCellWarn(cellThresholdWarnParam, batNum, itemCode, value);
        putCellWarn(batNum, itemCode, value);
    }

    private void putCellWarn(Map<Integer, Map<String, String>> target, Integer batNum, String itemCode, String value) {
        if (batNum == null) {
            return;
        }
        target.computeIfAbsent(batNum, key -> new LinkedHashMap<>()).put(itemCode, value);
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
