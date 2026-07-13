package com.shanhe.project.manage.opt.service;

import com.shanhe.framework.enums.BatteryPackStatusEnum;
import com.shanhe.framework.enums.BatteryTestEnum;
import com.shanhe.project.collector.battery.config.BatteryCollectorProperties;
import com.shanhe.project.collector.battery.service.BatteryModeStatusService;
import com.shanhe.project.collector.battery.model.BatteryModuleGroupRealtime;
import com.shanhe.project.collector.battery.model.BatteryModuleRealtimeSnapshot;
import com.shanhe.project.collector.battery.service.BatteryModuleRealtimeSnapshotService;
import com.shanhe.project.collector.battery.model.BatteryModeInfo;
import com.shanhe.project.manage.opt.domain.OptLog;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 保守收口崩溃或异常停止后残留的蓄电池测试运行态
 *
 * @author wjh
 * @since 2026/7/8
 */
@Slf4j
@Service
public class BatteryOptRuntimeRecoveryService {

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
