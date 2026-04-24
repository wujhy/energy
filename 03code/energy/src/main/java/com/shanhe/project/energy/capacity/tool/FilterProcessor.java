package com.shanhe.project.energy.capacity.tool;


import com.shanhe.project.energy.capacity.vo.DataPoint;

import java.util.ArrayList;
import java.util.List;

public class FilterProcessor {

    /**
     * 移动平均滤波
     * @param data 原始数据列表
     * @param windowSize 窗口大小
     * @return 滤波后的数据列表
     */
    public static List<DataPoint> movingAverageFilter(List<DataPoint> data, int windowSize) {
        if (data == null || data.size() < windowSize) {
            return new ArrayList<>();
        }
        List<DataPoint> filteredData = new ArrayList<>();
        DataPoint point;
        for (int i = 0; i < data.size(); i++) {
            double sum = 0;
            int count = 0;
            point = data.get(i);
            // 计算窗口内的平均值
            for (int j = Math.max(0, i - windowSize / 2);
                 j <= Math.min(data.size() - 1, i + windowSize / 2); j++) {
                sum += data.get(j).getVoltage();
                count++;
            }

            double average = sum / count;
            point.setVoltage(average);
            // 创建新的数据对象，保留原时间戳，更新电压值
            filteredData.add(point);
        }
        return filteredData;
    }

    /**
     * 中值滤波（有效去除脉冲噪声）
     * @param data 原始数据列表
     * @param windowSize 窗口大小
     * @return 滤波后的数据列表
     */
    public static List<DataPoint> medianFilter(List<DataPoint> data, int windowSize) {
        if (data == null || data.size() < windowSize) {
            return new ArrayList<>();
        }
        List<DataPoint> filteredData = new ArrayList<>();
        DataPoint point = null;
        for (int i = 0; i < data.size(); i++) {
            List<Double> windowValues = new ArrayList<>();

            // 收集窗口内的值
            for (int j = Math.max(0, i - windowSize / 2);
                 j <= Math.min(data.size() - 1, i + windowSize / 2); j++) {
                windowValues.add(data.get(j).getVoltage());
            }

            // 排序并取中值
            windowValues.sort(Double::compare);
            double median = windowValues.get(windowValues.size() / 2);
            point.setVoltage(median);
            // 创建新的数据对象，保留原时间戳，更新电压值
            filteredData.add(point);
        }

        return filteredData;
    }

    /**
     * 指数移动平均滤波（EMA）
     * @param data 原始数据列表
     * @param alpha 窗口大小
     * @return 滤波后的数据列表
     */
    public static List<DataPoint> exponentialMovingAverageFilter(List<DataPoint> data, double alpha) {
        if (data == null || data.isEmpty()) {
            return new ArrayList<>();
        }

        List<DataPoint> filteredData = new ArrayList<>();
        // 初始值
        double ema = data.get(0).getVoltage();

        for (int i = 0; i < data.size(); i++) {
            DataPoint current = data.get(i);
            ema = alpha * current.getVoltage() + (1 - alpha) * ema;
            current.setVoltage(ema);
            filteredData.add(current);
        }
        return filteredData;
    }
}