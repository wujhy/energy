package com.shanhe.project.manage.opt.service;

import com.shanhe.framework.enums.BatteryPackStatusEnum;
import com.shanhe.framework.enums.BatteryTestEnum;
import com.shanhe.framework.enums.ResistanceTestStatusEnum;
import com.shanhe.framework.web.domain.AjaxResult;
import com.shanhe.project.collector.battery.config.BatteryCollectorProperties;
import com.shanhe.project.collector.battery.model.BatteryModeInfo;
import com.shanhe.project.collector.battery.model.BatteryModuleGroupRealtime;
import com.shanhe.project.collector.battery.model.BatteryModuleRealtimeSnapshot;
import com.shanhe.project.collector.battery.postprocess.BatteryRealtimePostProcessContext;
import com.shanhe.project.collector.battery.postprocess.BatteryRealtimePostProcessor;
import com.shanhe.project.collector.battery.postprocess.PostProcessBatchGuard;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.shanhe.project.collector.battery.service.BatteryModeStatusService;
import com.shanhe.project.collector.battery.service.BatteryModuleRealtimeSnapshotService;
import com.shanhe.project.manage.opt.domain.OptLog;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

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

    /** 各电池组实时状态首次离开 BACKUP 的时间；活跃状态帧会重置。 */
    private final Map<Integer, Long> backupEndFirstSeen = new ConcurrentHashMap<>();

    @Resource
    private OptLogService optLogService;
    @Resource
    private BatteryModeStatusService batteryModeStatusService;
    @Resource
    private BatteryCollectorProperties batteryCollectorProperties;
    @Resource
    private BatteryModuleRealtimeSnapshotService realtimeSnapshotService;
    @Resource
    private BatteryTestLifecycleService lifecycleService;
    @Resource
    private BackupExternalModuleControlService backupExternalModuleControlService;

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
        List<OptLog> running = optLogService.selectRunningList(null);
        if (running == null || running.isEmpty()) {
            return 0;
        }
        int recovered = 0;
        for (OptLog run : running) {
            if (BatteryTestEnum._5.getDictValue().equals(run.getType())) {
                try {
                    boolean restored = lifecycleService.interruptAfterRestore(run,
                            () -> isSuccess(backupExternalModuleControlService.stopBackup(run.getPackNum())));
                    if (restored) {
                        recovered++;
                    } else {
                        log.warn("启动恢复备电外设未确认成功, packNum={}", run.getPackNum());
                    }
                } catch (RuntimeException e) {
                    log.warn("启动恢复备电外设失败, packNum={}, 原因={}", run.getPackNum(), e.getMessage());
                }
            } else {
                lifecycleService.interrupt(run);
                recovered++;
            }
        }
        return recovered;
    }
    /** 采集状态确认已离开测试态时，关闭已有 running log，不创建、不续写新日志。 */
    public void closeByRealtimeStatus(Integer packNum, BatteryModuleGroupRealtime group) {
        if (packNum == null || group == null || optLogService == null) {
            return;
        }
        // 先查运行缓存；无运行中的测试任务直接返回，不做状态解析和数据库查询
        OptLog running = optLogService.selectRunningCacheLog(packNum);
        if (running == null || running.getType() == null) {
            backupEndFirstSeen.remove(packNum);
            return;
        }
        // 测试启动后采集数据存在滞后，宽限期内不按采集状态做自然结束补偿
        if (withinStartupGrace(running)) {
            log.debug("测试启动宽限期内跳过状态自然结束补偿, packNum={}, type={}, optLogId={}",
                    packNum, running.getType(), running.getId());
            return;
        }
        Integer type = running.getType();
        if (BatteryTestEnum._1.getDictValue().equals(type)) {
            closeResistanceLogIfEnded(running, group.getResistanceTestStatus());
            return;
        }
        if (BatteryTestEnum._3.getDictValue().equals(type)
                || BatteryTestEnum._5.getDictValue().equals(type)
                || BatteryTestEnum._7.getDictValue().equals(type)) {
            closeBatteryPackLogIfEnded(running, group);
        }
    }

    /**
     * 定时兜底：采集数据缺失时备电时长到期也能停止 `_5` 并关闭外设。
     * 截止电压依赖实时值，仍由采集后处理评估。
     *
     * @return 停止的运行数
     */
    public int autoStopExpiredBackupRuns() {
        if (optLogService == null || lifecycleService == null
                || !Boolean.TRUE.equals(batteryCollectorProperties.getBackupAutoStopEnabled())) {
            return 0;
        }
        List<OptLog> runningLogs = optLogService.selectRunningList(null);
        if (runningLogs == null || runningLogs.isEmpty()) {
            return 0;
        }
        int stopped = 0;
        for (OptLog running : runningLogs) {
            if (!BatteryTestEnum._5.getDictValue().equals(running.getType())
                    || running.getPackNum() == null || withinStartupGrace(running)) {
                continue;
            }
            String reason = resolveBackupStopReason(running, null);
            if (reason != null && stopBackupRun(running, reason)) {
                stopped++;
            }
        }
        return stopped;
    }

    private void closeBatteryPackLogIfEnded(OptLog running, BatteryModuleGroupRealtime group) {
        Integer packNum = running.getPackNum();
        Integer type = running.getType();
        String batteryPackStatus = Objects.toString(group.getBatteryPackStatus(), null);
        if (BatteryPackStatusEnum.find(batteryPackStatus) == null) {
            log.debug("操作日志运行态补偿跳过：电池组状态未确认, packNum={}, status={}", packNum, batteryPackStatus);
            return;
        }
        if (isActiveBatteryPackStatus(batteryPackStatus)) {
            backupEndFirstSeen.remove(packNum);
            if (BatteryTestEnum._5.getDictValue().equals(type)) {
                evaluateBackupAutoStop(running, group);
            }
            return;
        }
        if (BatteryTestEnum._5.getDictValue().equals(type)) {
            closeBackupIfConfirmedEnded(running);
            return;
        }
        closeRunningByRealtime(running);
    }

    /**
     * `_5` 自动停止评估：按本次运行快照的截止电压/备电时长（对应 M460 0x30 下层停止语义）主动停止，
     * 命中后按计划完成语义走 STOPPING 两阶段关闭并停止外部备电模块。
     */
    private void evaluateBackupAutoStop(OptLog running, BatteryModuleGroupRealtime group) {
        if (!Boolean.TRUE.equals(batteryCollectorProperties.getBackupAutoStopEnabled())
                || lifecycleService == null) {
            return;
        }
        String reason = resolveBackupStopReason(running, group);
        if (reason != null) {
            stopBackupRun(running, reason);
        }
    }

    /** 按完成语义停止备电运行并关闭外设；返回是否已确认停止。 */
    private boolean stopBackupRun(OptLog running, String reason) {
        Integer packNum = running.getPackNum();
        boolean stopped = lifecycleService.completeAfterRestore(running, null, null,
                () -> isSuccess(backupExternalModuleControlService.stopBackup(packNum)));
        if (stopped) {
            log.info("备电测试命中自动停止条件已关闭, packNum={}, optLogId={}, 原因={}", packNum, running.getId(), reason);
        } else {
            log.warn("备电测试命中自动停止条件但外设停止未确认, packNum={}, optLogId={}, 原因={}", packNum, running.getId(), reason);
        }
        return stopped;
    }

    /**
     * 返回命中的停止原因；未命中返回 null。停止参数来自本次运行的快照（`dev_opt_log.content`），
     * 手动与计划执行同等生效；无快照参数的运行不做自动停止。缺失实时值不参与判断，不用假默认值驱动停止。
     * `group` 为 null 时（采集缺失的定时兜底）只评估备电时长。
     */
    private String resolveBackupStopReason(OptLog running, BatteryModuleGroupRealtime group) {
        Map<String, Object> params = parseRunParams(running.getContent());
        if (params.isEmpty()) {
            return null;
        }
        if (group != null) {
            Double endVoltage = toDouble(params.get("endVoltage"));
            Double packVoltage = group.getPackVoltage() != null ? group.getPackVoltage() : group.getExternalVoltage();
            if (endVoltage != null && endVoltage > 0 && packVoltage != null && packVoltage <= endVoltage) {
                return String.format("组电压 %.3fV 已达截止电压 %.3fV", packVoltage, endVoltage);
            }
        }
        Integer dischargeTime = toInteger(params.get("dischargeTime"));
        Date createTime = running.getCreateTime();
        if (dischargeTime != null && dischargeTime > 0 && createTime != null
                && System.currentTimeMillis() - createTime.getTime() >= dischargeTime * MINUTE) {
            return String.format("备电时长已达计划 %d 分钟", dischargeTime);
        }
        return null;
    }

    /** 解析运行参数快照；内容缺失或非法时返回空 Map，不驱动停止。 */
    private Map<String, Object> parseRunParams(String content) {
        if (content == null || content.isEmpty()) {
            return Collections.emptyMap();
        }
        try {
            Map<String, Object> params = JSON.parseObject(content, new TypeReference<Map<String, Object>>() {});
            return params == null ? Collections.emptyMap() : params;
        } catch (RuntimeException e) {
            return Collections.emptyMap();
        }
    }

    private Double toDouble(Object value) {
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        try {
            return value == null ? null : Double.valueOf(String.valueOf(value));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Integer toInteger(Object value) {
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            return value == null ? null : Integer.valueOf(String.valueOf(value));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** 测试启动初期采集状态可能仍反映旧状态，宽限期内不做自然结束判断。 */
    private boolean withinStartupGrace(OptLog running) {
        Date createTime = running.getCreateTime();
        if (createTime == null) {
            return false;
        }
        long grace = orDefault(batteryCollectorProperties.getTestStartupStatusGraceMs(), 120_000L);
        return System.currentTimeMillis() - createTime.getTime() < grace;
    }

    /** `_5` 备电日志需实时状态持续离开 BACKUP 超过防抖窗口才关闭，单帧波动会被活跃状态重置。 */
    private void closeBackupIfConfirmedEnded(OptLog running) {
        Integer packNum = running.getPackNum();
        long now = System.currentTimeMillis();
        long firstSeen = backupEndFirstSeen.computeIfAbsent(packNum, key -> now);
        // 新启动的备电测试从日志创建时间起算，避免残留的离开标记立即关闭新任务
        Date createTime = running.getCreateTime();
        long baseline = createTime == null ? firstSeen : Math.max(firstSeen, createTime.getTime());
        long confirmWindow = orDefault(batteryCollectorProperties.getBackupEndConfirmMs(), 180_000L);
        if (now - baseline < confirmWindow) {
            log.debug("备电结束状态未持续超过防抖窗口，暂不补偿关闭, packNum={}, optLogId={}", packNum, running.getId());
            return;
        }
        backupEndFirstSeen.remove(packNum);
        closeRunningByRealtime(running);
    }

    private void closeResistanceLogIfEnded(OptLog running, Integer resistanceTestStatusValue) {
        String resistanceTestStatus = Objects.toString(resistanceTestStatusValue, null);
        if (ResistanceTestStatusEnum.find(resistanceTestStatus) == null) {
            return;
        }
        if (ResistanceTestStatusEnum.isCode(resistanceTestStatus, ResistanceTestStatusEnum.TESTING)) {
            return;
        }
        closeRunningByRealtime(running);
    }

    /** 按采集状态自然结束语义关闭已确认的运行日志。 */
    private void closeRunningByRealtime(OptLog running) {
        Integer packNum = running.getPackNum();
        Integer type = running.getType();
        try {
            if (lifecycleService != null) {
                if (BatteryTestEnum._5.getDictValue().equals(type)) {
                    boolean restored = lifecycleService.completeAfterRestore(running, null, null,
                            () -> isSuccess(backupExternalModuleControlService.stopBackup(packNum)));
                    if (!restored) {
                        log.warn("备电测试结束但外设恢复未确认, packNum={}", packNum);
                    }
                } else {
                    lifecycleService.complete(running.getId(), packNum, resolveMode(type),
                            running.getTargetAddress(), true);
                }
            } else {
                optLogService.doStopTest(packNum, type);
            }
        } catch (Exception e) {
            log.warn("操作日志运行态补偿关闭失败, packNum={}, type={}", packNum, type, e);
        }
    }
    private boolean isSuccess(AjaxResult result) {
        return result != null && Objects.equals(
                result.get(AjaxResult.CODE_TAG),
                AjaxResult.Type.SUCCESS.value());
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
                if (closeRunningLog(optLog, now)) {
                    recovered++;
                    log.warn("蓄电池测试运行态补偿关闭残留日志, packNum={}, type={}, optLogId={}",
                            optLog.getPackNum(), optLog.getType(), optLog.getId());
                } else {
                    log.warn("蓄电池测试运行态补偿保留：外设恢复未确认, packNum={}, type={}, optLogId={}",
                            optLog.getPackNum(), optLog.getType(), optLog.getId());
                }
            }
        }
        return recovered;
    }


    private boolean closeRunningLog(OptLog optLog, long now) {
        if (lifecycleService != null) {
            if (BatteryTestEnum._5.getDictValue().equals(optLog.getType())) {
                return lifecycleService.completeAfterRestore(optLog, null, null,
                        () -> isSuccess(backupExternalModuleControlService.stopBackup(optLog.getPackNum())));
            }
            lifecycleService.complete(optLog.getId(), optLog.getPackNum(), resolveMode(optLog.getType()),
                    optLog.getTargetAddress(), false);
            return true;
        }
        optLogService.update(optLog.getId(), 1, new Date(now));
        Integer mode = resolveMode(optLog.getType());
        if (mode != null && batteryModeStatusService != null) {
            batteryModeStatusService.markStopped(optLog.getPackNum(), mode, optLog.getTargetAddress(), false, optLog.getId());
        }
        return true;
    }
    private boolean shouldCloseRunningLog(OptLog log, long now) {
        if (log == null || log.getId() == null || log.getPackNum() == null) {
            return false;
        }
        Date progressTime = resolveProgressTime(log);
        if (progressTime == null || now - progressTime.getTime() < timeoutMillis(log.getType())) {
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

    /** 超时窗口按无进展时长判断：优先最后进展时间，缺失时退化到创建/更新时间。 */
    private Date resolveProgressTime(OptLog log) {
        Date progress = parseTime(log.getLastProgressAt());
        if (progress != null) {
            return progress;
        }
        return log.getCreateTime() == null ? log.getUpdateTime() : log.getCreateTime();
    }

    private Date parseTime(String text) {
        if (text == null || text.isEmpty()) {
            return null;
        }
        try {
            return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(text);
        } catch (ParseException e) {
            return null;
        }
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
            return orDefault(batteryCollectorProperties.getBackupRuntimeRecoveryConfirmMs(), 12L * HOUR);
        }
        if (BatteryTestEnum._6.getDictValue().equals(testType)) {
            return orDefault(batteryCollectorProperties.getSingleResistanceRuntimeRecoveryConfirmMs(), 30L * MINUTE);
        }
        if (BatteryTestEnum._1.getDictValue().equals(testType)
                || BatteryTestEnum._2.getDictValue().equals(testType)) {
            return orDefault(batteryCollectorProperties.getResistanceRuntimeRecoveryConfirmMs(), 6L * HOUR);
        }
        return 12L * HOUR;
    }

    private long orDefault(Long value, long defaultValue) {
        return value == null ? defaultValue : value;
    }
}
