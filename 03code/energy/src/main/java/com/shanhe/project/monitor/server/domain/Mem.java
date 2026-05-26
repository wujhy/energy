package com.shanhe.project.monitor.server.domain;

import com.shanhe.common.utils.ArithmeticUtils;

/**
 * 內存相关信息
 *
 * @author wjh
 * @since 2026-05-25
 */
public class Mem
{
    /**
     * 内存总量
     */
    private double total;

    /**
     * 已用内存
     */
    private double used;

    /**
     * 剩余内存
     */
    private double free;

    /**
     * 获取内存总量（GB）
     *
     * @return 内存总量
     */
    public double getTotal()
    {
        return ArithmeticUtils.div(total, (1024 * 1024 * 1024), 2);
    }

    public void setTotal(long total)
    {
        this.total = total;
    }

    /**
     * 获取已用内存（GB）
     *
     * @return 已用内存
     */
    public double getUsed()
    {
        return ArithmeticUtils.div(used, (1024 * 1024 * 1024), 2);
    }

    public void setUsed(long used)
    {
        this.used = used;
    }

    /**
     * 获取剩余内存（GB）
     *
     * @return 剩余内存
     */
    public double getFree()
    {
        return ArithmeticUtils.div(free, (1024 * 1024 * 1024), 2);
    }

    public void setFree(long free)
    {
        this.free = free;
    }

    /**
     * 获取内存使用率（%）
     *
     * @return 内存使用率
     */
    public double getUsage()
    {
        return ArithmeticUtils.mul(ArithmeticUtils.div(used, total, 4), 100);
    }
}
