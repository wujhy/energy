package com.shanhe.project.manage.opt.service;

import com.shanhe.framework.enums.BatteryPackStatusEnum;
import com.shanhe.framework.enums.BatteryTestEnum;
import com.shanhe.framework.enums.ResistanceTestStatusEnum;
import com.shanhe.project.collector.battery.config.BatteryCollectorProperties;
import com.shanhe.project.collector.battery.model.BatteryModeInfo;
import com.shanhe.project.collector.battery.model.BatteryModuleGroupRealtime;
import com.shanhe.project.collector.battery.model.BatteryModuleRealtimeSnapshot;
import com.shanhe.project.collector.battery.postprocess.BatteryRealtimePostProcessContext;
import com.shanhe.project.collector.battery.postprocess.BatteryRealtimePostProcessor;
import com.shanhe.project.collector.battery.postprocess.PostProcessBatchGuard;
import com.shanhe.project.collector.battery.service.BatteryModeStatusService;
import com.shanhe.project.collector.battery.service.BatteryModuleRealtimeSnapshotService;
import com.shanhe.project.manage.opt.domain.OptLog;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;
import java.util.Objects;

/**
 * 收口崩溃、异常停止和采集状态自然结束后的蓄电池测试运行态。
 *
 * @author wjh
 * @since 2026/7/8
 */
@Slf4j
@Service
public class BatteryOptRuntimeRecoveryService implements BatteryRealtimePostProcessor {

    private static final long MINUTE = 60_000L;
    private static final long HOUR = 60L * MINUTE;

    @Resource
    private OptLogService optLogService;
    @Resource
    private BatteryModeStatusService batteryModeStatusService;
    @Resource
    private BatteryCollectorProperties batteryCollectorProperties;
    @Resource
    private BatteryModuleRealtimeSnapshotService realtimeSnapshotService;

    @Override
    public String getName() {
        return "operationLogRuntimeRecovery";
    }

    @Override
    public int getOrder() {
        return 250;
    }

    @Override
    public boolean shouldProcess(BatteryRealtimePostProcessContext context) {
        return context != null
                && context.getPackNum() != null
                && context.getGroup() != null
                && PostProcessBatchGuard.sameRealtimeBatch(context);
    }

    @Override
    public void process(BatteryRealtimePostProcessContext context) {
        closeByRealtimeStatus(context.getPackNum(), context.getGroup());
    }

    /** 调度前补偿指定电池组的残留运行态。 */
    public int recoverPack(Integer packNum) {
        if (packNum == null || optLogService == null) {
            return 0;
        }
        return recoverRunningLogs(optLogService.selectRunningList(packNum), false);
    }

    /** 应用启动后补偿全部残留运行态。 */
    public int recoverAll() {
        if (optLogService == null) {
            return 0;
        }
        return recoverRunningLogs(optLogService.selectRunningList(null), true);
    }

    /** 采集状态确认已离开测试态时，关闭已有 running log，不创建、不续写新日志。 */
    public void closeByRealtimeStatus(Integer packNum, BatteryModuleGroupRealtime group) {
        if (packNum == null || group == null || optLogService == null) {
            return;
        }
        closeBatteryPackLogsIfEnded(packNum, group.getBatteryPackStatus());
        closeResistanceLogIfEnded(packNum, group.getResistanceTestStatus());
    }

    private void closeBatteryPackLogsIfEnded(Integer packNum, Integer batteryPackStatusValue) {
        String batteryPackStatus = Objects.toString(batteryPackStatusValue, null);
        if (BatteryPackStatusEnum.find(batteryPackStatus) == null) {
            log.debug("操作日志运行态补偿跳过：电池组状态未确认, packNum={}, status={}", packNum, batteryPackStatus);
            return;
        }
        if (isActiveBatteryPackStatus(batteryPackStatus)) {
            return;
        }
        closeIfRunning(packNum, BatteryTestEnum._3.getDictValue());
        closeIfRunning(packNum, BatteryTestEnum._5.getDictValue());
        closeIfRunning(packNum, BatteryTestEnum._7.getDictValue());
    }

    private void closeResistanceLogIfEnded(Integer packNum, Integer resistanceTestStatusValue) {
        String resistanceTestStatus = Objects.toString(resistanceTestStatusValue, null);
        if (ResistanceTestStatusEnum.find(resistanceTestStatus) == null) {
            return;
        }
        if (ResistanceTestStatusEnum.isCode(resistanceTestStatus, ResistanceTestStatusEnum.TESTING)) {
            return;
        }
        closeIfRunning(packNum, BatteryTestEnum._1.getDictValue());
    }

    private void closeIfRunning(Integer packNum, Integer type) {
        try {
            optLogService.doStopTest(packNum, type);
        } catch (Exception e) {
            log.warn("操作日志运行态补偿关闭失败, packNum={}, type={}", packNum, type, e);
        }
    }

    private boolean isActiveBatteryPackStatus(String status) {
        return BatteryPackStatusEnum.isCode(status, BatteryPackStatusEnum.CHARGE)
                || BatteryPackStatusEnum.isCode(status, BatteryPackStatusEnum.CAPACITY_TEST)
                || BatteryPackStatusEnum.isCode(status, BatteryPackStatusEnum.BACKUP);
    }

    private int recoverRunningLogs(List<OptLog> runningLogs, boolean forceClose) {
        if (runningLogs == null || runningLogs.isEmpty()) {
            return 0;
        }
        long now = System.currentTimeMillis();
        int recovered = 0;
        for (OptLog optLog : runningLogs) {
            if (forceClose || shouldCloseRunningLog(optLog, now)) {
                closeRunningLog(optLog, now);
                recovered++;
                log.warn("蓄电池测试运行态补偿关闭残留日志, packNum={}, type={}, optLogId={}",
                        optLog.getPackNum(), optLog.getType(), optLog.getId());
            }
        }
        if (recovered > 0) {
            optLogService.updateCache();
        }
        return recovered;
    }

    private void closeRunningLog(OptLog optLog, long now) {
        optLogService.update(optLog.getId(), 1, new Date(now));
        Integer mode = resolveMode(optLog.getType());
        if (mode != null && batteryModeStatusService != null) {
            batteryModeStatusService.markStopped(
                    optLog.getPackNum(),
                    mode,
                    optLog.getTargetAddress(),
                    false,
                    optLog.getId());
        }
    }

    private boolean shouldCloseRunningLog(OptLog log, long now) {
        if (log == null || log.getId() == null || log.getPackNum() == null) {
            return false;
        }
        Date startTime = log.getCreateTime() == null ? log.getUpdateTime() : log.getCreateTime();
        if (startTime == null || now - startTime.getTime() < timeoutMillis(log.getType())) {
            return false;
        }
        if (BatteryTestEnum._5.getDictValue().equals(log.getType())) {
            Boolean realtimeBackup = isRealtimeBackup(log.getPackNum());
            return Boolean.FALSE.equals(realtimeBackup);
        }
        BatteryModeInfo modeInfo = batteryModeStatusService == null ? null : batteryModeStatusService.get(log.getPackNum());
        return modeInfo == null
                || !Objects.equals(modeInfo.getPackNum(), log.getPackNum())
                || !Objects.equals(modeInfo.getStatus(), 1);
    }

    private Boolean isRealtimeBackup(Integer packNum) {
        BatteryModuleRealtimeSnapshot snapshot = realtimeSnapshotService == null
                ? null : realtimeSnapshotService.getCachedSnapshot(packNum);
        BatteryModuleGroupRealtime group = snapshot == null ? null : snapshot.getGroup();
        if (group == null || group.getBatteryPackStatus() == null) {
            return null;
        }
        return BatteryPackStatusEnum.isCode(String.valueOf(group.getBatteryPackStatus()), BatteryPackStatusEnum.BACKUP);
    }

    private Integer resolveMode(Integer testType) {
        if (BatteryTestEnum._1.getDictValue().equals(testType)
                || BatteryTestEnum._6.getDictValue().equals(testType)) {
            return BatteryModeStatusService.MODE_INTERNAL_RESISTANCE;
        }
        if (BatteryTestEnum._2.getDictValue().equals(testType)) {
            return BatteryModeStatusService.MODE_CONNECT_RESISTANCE;
        }
        return null;
    }

    private long timeoutMillis(Integer testType) {
        if (BatteryTestEnum._5.getDictValue().equals(testType)) {
            return batteryCollectorProperties.getBackupRuntimeRecoveryConfirmMs() == null ?
                    12L * HOUR : batteryCollectorProperties.getBackupRuntimeRecoveryConfirmMs();
        }
        if (BatteryTestEnum._6.getDictValue().equals(testType)) {
            return 30L * MINUTE;
        }
        if (BatteryTestEnum._1.getDictValue().equals(testType)
                || BatteryTestEnum._2.getDictValue().equals(testType)) {
            return 6L * HOUR;
        }
        return 12L * HOUR;
    }
}
