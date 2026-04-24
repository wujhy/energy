package com.shanhe.project.energy.capacity.tool;

import com.shanhe.common.utils.StringUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * 转换器
 */
public class RateCapacityConverter {
    // 铅酸电池典型Peukert常数
    private static final double PEUKERT_CONSTANT = 1.02;

    /**
     * 预定义的转换系数表（提高计算效率）
     */
    private static final Map<Double, Double> CONVERSION_TABLE = createConversionTable();

    private static Map<Double, Double> createConversionTable() {
        Map<Double, Double> table = new HashMap<>();
        // 基准倍率
        table.put(0.10, 1.000);
        // 高倍率区间（大于0.1C）
        table.put(0.12, 1.004);
        table.put(0.20, 1.015);
        table.put(0.30, 1.023);
        return table;
    }

    /**
     * 主转换方法 - 使用查表法（快速）
     */
    public static double convertTo01C(double actualCapacity, double actualRate) {
        if(actualRate<0){
            actualRate = Math.abs(actualRate);
        }
        Double factor = CONVERSION_TABLE.get(actualRate);
        if (factor != null) {
            return actualCapacity * factor;
        }

        // 如果表中没有，使用公式计算
        return calculateByFormula(actualCapacity, actualRate);
    }

    /**
     * 公式计算法（适用于任意倍率）
     */
    public static double calculateByFormula(double actualCapacity, double actualRate) {
        double rateRatio = actualRate / 0.1;
        double factor = Math.pow(rateRatio, PEUKERT_CONSTANT - 1);

        double bCapacity = actualCapacity * factor;
        return StringUtils.formatToDouble(bCapacity,1);
    }

    /**
     * 批量转换方法
     */
    public static Map<Double, Double> convertBatch(Map<Double, Double> rateCapacityMap) {
        Map<Double, Double> result = new HashMap<>();

        for (Map.Entry<Double, Double> entry : rateCapacityMap.entrySet()) {
            double standardizedCapacity = convertTo01C(entry.getValue(), entry.getKey());
            result.put(entry.getKey(), standardizedCapacity);
        }

        return result;
    }

    /**
     * 计算不同倍率下的放电斜率关系
     * @param lowRate 低倍率
     * @param highRate 高倍率
     * @return
     */
    public static double calculateSlopeRelationship(
            double lowRate,
            double highRate) {
        double lowCurrent = lowRate ;
        double highCurrent = highRate ;
        double slopeRatio = lowCurrent / highCurrent;
        return Math.abs(slopeRatio);
    }
}
