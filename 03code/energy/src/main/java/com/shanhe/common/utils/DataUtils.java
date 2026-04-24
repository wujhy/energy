package com.shanhe.common.utils;

import java.util.Objects;

/**
 * 数据工具类
 *
 * @author wjh
 * @since 2025/6/23
 */
public class DataUtils {

    static final double EPSILON = 1e-10;

    /**
     * 校验是否在告警区间内
     *
     * @param min 最小值
     * @param max 最大值
     * @param standValue 基准值
     * @param value 值
     * @return 结果 0-是 1-否
     */
    public static boolean isInRange(Double min, Double max, Double standValue, Double value) {
        // 基准值处理
        if (!Objects.isNull(standValue) && Math.abs(value) >= EPSILON) {
            if (min != null) {
                min *= standValue;
            }
            if (max != null) {
                max *= standValue;
            }
        }
        // 值比较
        if (min != null && max != null) {
            return Double.compare(value, min) >= 0 && Double.compare(value, max) < 0;
        }
        if (min != null) {
            return Double.compare(value, min) >= 0;
        }
        if (max != null) {
            return Double.compare(value, max) < 0;
        }
        return false;
    }
}
