package com.shanhe.project.collector.battery.service;

import com.shanhe.project.collector.battery.model.BatteryModulePollContext;

/**
 * 600节模块端轮询上下文持有器。
 *
 * @author wjh
 * @since 2026-04-28
 */
public final class BatteryModulePollContextHolder {

    /**
     * 当前轮询线程上下文。
     */
    private static final ThreadLocal<BatteryModulePollContext> CONTEXT = new ThreadLocal<>();

    private BatteryModulePollContextHolder() {
    }

    /**
     * 设置当前线程轮询上下文。
     *
     * @param context 轮询上下文
     */
    public static void set(BatteryModulePollContext context) {
        CONTEXT.set(context);
    }

    /**
     * 获取当前线程轮询上下文。
     *
     * @return 轮询上下文
     */
    public static BatteryModulePollContext get() {
        return CONTEXT.get();
    }

    /**
     * 清理当前线程轮询上下文。
     */
    public static void clear() {
        CONTEXT.remove();
    }
}
