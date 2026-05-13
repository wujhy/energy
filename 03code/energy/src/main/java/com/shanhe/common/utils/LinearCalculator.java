package com.shanhe.common.utils;

import cn.hutool.core.util.StrUtil;

/**
 * 构造一元一次函数
 */
public class LinearCalculator {

    /* 斜率 */
    private final double k;
    /* 截距 */
    private final double b;

    // 构造函数
    public LinearCalculator(double k, double b) {
        this.k = k;
        this.b = b;
    }

    /**
     * 计算给定x对应的y值
     * y = kx + b
     */
    public double calculate(double x) {
        return k * x + b;
    }

    /**
     * 计算给定x对应的y值
     * y = kx + b
     */
    public static String calculate(double k, double b, String x) {
        if (StrUtil.isBlank(x)) {
            return x;
        }
        return Double.toString(k * Double.parseDouble(x) + b);
    }

    /**
     * 给出两个坐标，求出斜率值k，b
     * y = kx + b
     *
     * @param x1 原量程最小值
     * @param y1 目标量程最小值
     * @param x2 原量程最大值
     * @param y2 目标量程最大值
     * @return 斜率、斜率修正值
     */
    public static double[] findIntersection(double x1, double y1, double x2, double y2){
        LinearCalculator func1 = new LinearCalculator(-1 * x1, y1);
        LinearCalculator func2 = new LinearCalculator(-1 * x2, y2);
        return func1.findIntersection(func2);
    }

    // 判断两条直线是否平行
    private boolean isParallelTo(LinearCalculator other) {
        return this.k == other.k;
    }

    // 判断两条直线是否相同
    private boolean isSameAs(LinearCalculator other) {
        return this.k == other.k && this.b == other.b;
    }

    // 求两条直线的交点
    private double[] findIntersection(LinearCalculator other) {
        if (this.isParallelTo(other)) {
            if (this.isSameAs(other)) {
                System.out.println("两条直线重合，有无限多交点");
            } else {
                System.out.println("两条直线平行，无交点");
            }
            return null;
        }

        double x = (other.b - this.b) / (this.k - other.k);
        double y = this.calculate(x);
        return new double[]{x, y};
    }
}
