package com.shanhe.project.monitor.server.domain;

import java.lang.management.ManagementFactory;
import com.shanhe.common.utils.ArithmeticUtils;
import com.shanhe.common.utils.DateUtils;
import lombok.Data;

/**
 * JVM相关信息
 *
 * @author wjh
 * @since 2025/6/6
 */
@Data
public class Jvm
{
    /**
     * 当前JVM占用的内存总数(M)
     */
    private double total;

    /**
     * JVM最大可用内存总数(M)
     */
    private double max;

    /**
     * JVM空闲内存(M)
     */
    private double free;

    /**
     * JDK版本
     */
    private String version;

    /**
     * JDK路径
     */
    private String home;

    public double getTotal()
    {
        return ArithmeticUtils.div(total, (1024 * 1024), 2);
    }

    public double getMax()
    {
        return ArithmeticUtils.div(max, (1024 * 1024), 2);
    }

    public double getFree()
    {
        return ArithmeticUtils.div(free, (1024 * 1024), 2);
    }

    public double getUsed()
    {
        return ArithmeticUtils.div(total - free, (1024 * 1024), 2);
    }

    public double getUsage()
    {
        return ArithmeticUtils.mul(ArithmeticUtils.div(total - free, total, 4), 100);
    }
    /**
     * 获取JDK名称
     */
    public String getName()
    {
        return ManagementFactory.getRuntimeMXBean().getVmName();
    }
    /**
     * JDK启动时间
     */
    public String getStartTime()
    {
        return DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD_HH_MM_SS, DateUtils.getServerStartDate());
    }
    /**
     * JDK运行时间
     */
    public String getRunTime()
    {
        return DateUtils.getDatePoor(DateUtils.getNowDate(), DateUtils.getServerStartDate());
    }
}
