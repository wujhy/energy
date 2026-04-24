package com.shanhe.project.monitor.server.domain;

import com.shanhe.common.utils.ArithmeticUtils;
import lombok.Setter;

/**
 * CPU相关信息
 *
 * @author wjh
 * @since 2025/6/6
 */
@Setter
public class Cpu
{
    /**
     * 核心数
     */
    private int cpuNum;

    /**
     * CPU总的使用率
     */
    private double total;

    /**
     * CPU系统使用率
     */
    private double sys;

    /**
     * CPU用户使用率
     */
    private double used;

    /**
     * CPU当前等待率
     */
    private double wait;

    /**
     * CPU当前空闲率
     */
    private double free;

    public double getTotal()
    {
        return ArithmeticUtils.round(ArithmeticUtils.mul(total, 100), 2);
    }

    public double getSys()
    {
        return ArithmeticUtils.round(ArithmeticUtils.mul(sys / total, 100), 2);
    }

    public double getUsed()
    {
        return ArithmeticUtils.round(ArithmeticUtils.mul(used / total, 100), 2);
    }

    public double getWait()
    {
        return ArithmeticUtils.round(ArithmeticUtils.mul(wait / total, 100), 2);
    }

    public double getFree()
    {
        return ArithmeticUtils.round(ArithmeticUtils.mul(free / total, 100), 2);
    }

}
