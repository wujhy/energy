package com.shanhe.project.collector.battery.postprocess;

import com.shanhe.project.collector.battery.service.postprocess.BatteryRealtimePostProcessContext;

/**
 * 实时数据后处理器接口。
 * <p>
 * 每个处理器负责一个独立的后处理步骤，统一异常隔离和日志格式。
 *
 * @author wjh
 * @since 2026-06-04
 */
public interface BatteryRealtimePostProcessor {

    /**
     * 处理器名称，用于日志和排序。
     *
     * @return 处理器名称
     */
    String getName();

    /**
     * 处理顺序，数值越小越先执行。
     *
     * @return 排序值
     */
    default int getOrder() {
        return 100;
    }

    /**
     * 判断是否应该执行此处理器。
     *
     * @param context 后处理上下文
     * @return true 表示应该执行
     */
    default boolean shouldProcess(BatteryRealtimePostProcessContext context) {
        return true;
    }

    /**
     * 执行后处理。
     *
     * @param context 后处理上下文
     */
    void process(BatteryRealtimePostProcessContext context);
}
