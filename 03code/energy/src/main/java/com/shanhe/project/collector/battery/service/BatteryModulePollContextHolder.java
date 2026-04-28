package com.shanhe.project.collector.battery.service;

import com.shanhe.project.collector.battery.model.BatteryModulePollContext;

/**
 * Thread-local 600-module polling context.
 */
public final class BatteryModulePollContextHolder {

    private static final ThreadLocal<BatteryModulePollContext> CONTEXT = new ThreadLocal<>();

    private BatteryModulePollContextHolder() {
    }

    public static void set(BatteryModulePollContext context) {
        CONTEXT.set(context);
    }

    public static BatteryModulePollContext get() {
        return CONTEXT.get();
    }

    public static void clear() {
        CONTEXT.remove();
    }
}
