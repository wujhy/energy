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

    // 求函数的根（解方程kx + b = 0）
    private double findRoot() {
        if (k == 0) {
            return b == 0 ? Double.POSITIVE_INFINITY : Double.NaN;
        }
        return -b / k;
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



    public static void main(String[] args) {
        System.out.println("一元一次函数计算器");
        System.out.println("请输入第一个函数的参数:");
        System.out.print("斜率k: ");
        double k1 = 0;
        System.out.print("截距b: ");
        double b1 = 0;
        
        System.out.println("请输入第二个函数的参数(用于比较和交点计算):");
        System.out.print("斜率k: ");
        double k2 = -1;
        System.out.print("截距b: ");
        double b2 = 2;
        
        LinearCalculator func1 = new LinearCalculator(k1, b1);
        LinearCalculator func2 = new LinearCalculator(k2, b2);
        
//        // 显示函数
//        System.out.print("函数1: ");
//        func1.displayFunction();
//        System.out.print("函数2: ");
//        func2.displayFunction();
//
//        // 计算函数值
//        System.out.print("\n请输入x值计算函数1的y值: ");
//        //double x = scanner.nextDouble();
//       // System.out.printf("f(%.2f) = %.2f\n", x, func1.calculate(x));
//
//        // 求根
//        System.out.printf("\n函数1的根: %.2f\n", func1.findRoot());
//
//        // 比较函数
//        System.out.println("\n函数比较:");
//        System.out.println("是否平行: " + func1.isParallelTo(func2));
//        System.out.println("是否相同: " + func1.isSameAs(func2));
        
        // 求交点
        double[] intersection = func1.findIntersection(func2);
        if (intersection != null) {
            System.out.printf("交点坐标: (%.2f, %.2f)\n", intersection[0], intersection[1]);
        }
    }
}