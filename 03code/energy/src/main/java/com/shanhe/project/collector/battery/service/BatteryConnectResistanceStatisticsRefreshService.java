package com.shanhe.project.collector.battery.service;

import com.shanhe.project.collector.battery.realtime.BatteryModuleGroupCalculationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * 连接条测试结束后的轻量统计刷新触发器。
 * <p>连接条 91 结果已写入标准单体实时表后，仅触发组实时计算和快照刷新，
 * 不把连接条结果伪装成旧整组内阻测试统计。</p>
 *
 * @author wjh
 * @since 2026-06-30
 */
@Slf4j
@Service
public class BatteryConnectResistanceStatisticsRefreshService {

    /** 模块组计算服务。 */
    @Resource
    private BatteryModuleGroupCalculationService groupCalculationService;

    /** 实时快照服务。 */
    @Resource
    private BatteryModuleRealtimeSnapshotService snapshotService;

    /** 线程池任务执行器。 */
    @Autowired(required = false)
    @Qualifier("threadPoolTaskExecutor")
    private TaskExecutor taskExecutor;

    /**
     * 连接条测试最终成功后触发刷新。
     *
     * @param packNum 电池组编号
     */
    public void refreshAfterCompletedTest(Integer packNum) {
        if (packNum == null) {
            return;
        }
        Runnable task = () -> refresh(packNum);
        if (taskExecutor != null) {
            taskExecutor.execute(task);
            return;
        }
        task.run();
    }

    private void refresh(Integer packNum) {
        try {
            if (groupCalculationService != null) {
                groupCalculationService.calculateAndSave(packNum);
            }
            if (snapshotService != null) {
                snapshotService.evict(packNum);
            }
            log.info("连接条测试结束后已触发实时统计刷新, packNum={}", packNum);
        } catch (Exception e) {
            log.warn("连接条测试结束后实时统计刷新失败, packNum={}, reason={}", packNum, e.getMessage());
        }
    }
}