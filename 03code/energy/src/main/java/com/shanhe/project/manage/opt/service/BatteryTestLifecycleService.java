package com.shanhe.project.manage.opt.service;

import com.shanhe.common.exception.ServiceException;
import com.shanhe.framework.enums.BatteryTestEnum;
import com.shanhe.project.collector.battery.service.BatteryModeStatusService;
import com.shanhe.project.manage.opt.domain.OptLog;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/** 统一维护测试业务日志及其模式状态投影。 */
@Service
public class BatteryTestLifecycleService {

    public static final String STARTING = "starting";
    public static final String RUNNING = "running";
    public static final String STOPPING = "stopping";
    public static final String SUCCEEDED = "success";
    public static final String FAILED = "failed";
    public static final String CANCELLED = "cancelled";
    public static final String INTERRUPTED = "interrupted";

    @Resource
    private OptLogService optLogService;
    @Resource
    private BatteryModeStatusService modeStatusService;

    public synchronized Long start(Integer packNum, Integer testType, String source) {
        List<OptLog> active = optLogService.selectRunningList(packNum);
        if (active != null && !active.isEmpty()) {
            throw new ServiceException("当前电池组已有测试运行中");
        }
        try {
            Long id = optLogService.insert(packNum, testType, null, source);
            optLogService.updateRuntime(id, STARTING, null);
            return id;
        } catch (RuntimeException e) {
            List<OptLog> concurrent = optLogService.selectRunningList(packNum);
            if (concurrent != null && !concurrent.isEmpty()) {
                throw new ServiceException("当前电池组已有测试运行中");
            }
            throw e;
        }
    }

    public void markRunning(Long businessOptLogId) {
        if (businessOptLogId != null) {
            optLogService.updateRuntime(businessOptLogId, RUNNING, null);
        }
    }

    /** 业务运行有实际推进时刷新进展时间；队列与 pending 只经此入口推进，不暴露内部命令字段。 */
    public void touchProgress(Long businessOptLogId) {
        if (businessOptLogId != null) {
            optLogService.touchProgress(businessOptLogId);
        }
    }

    public void complete(Long businessOptLogId, Integer packNum, Integer mode,
                         Integer address, boolean success) {
        if (businessOptLogId == null) {
            return;
        }
        optLogService.updateRuntime(businessOptLogId, success ? SUCCEEDED : FAILED, success ? 0 : 1);
        if (mode != null) {
            modeStatusService.markStopped(packNum, mode, address, success, businessOptLogId);
        }
    }

    public boolean stop(Integer packNum, Integer testType, Integer mode, Integer address) {
        return stop(packNum, testType, mode, address, () -> true);
    }

    public boolean stop(Integer packNum, Integer testType, Integer mode, Integer address,
                        StopAction action) {
        OptLog running = optLogService.getRunningOptLog(packNum, testType);
        return finishAfterAction(running, mode, address, CANCELLED, 1, true, action);
    }

    public boolean completeAfterRestore(OptLog running, Integer mode, Integer address,
                                        StopAction restoreAction) {
        return finishAfterAction(running, mode, address, SUCCEEDED, 0, true, restoreAction);
    }

    public boolean interruptAfterRestore(OptLog running, StopAction restoreAction) {
        return finishAfterAction(running, resolveMode(running == null ? null : running.getType()),
                running == null ? null : running.getTargetAddress(), INTERRUPTED, 1, false, restoreAction);
    }

    public void interrupt(OptLog log) {
        if (log == null || log.getId() == null) {
            return;
        }
        optLogService.updateRuntime(log.getId(), INTERRUPTED, 1);
        Integer mode = resolveMode(log.getType());
        if (mode != null) {
            modeStatusService.markStopped(log.getPackNum(), mode, log.getTargetAddress(), false, log.getId());
        }
    }

    private boolean finishAfterAction(OptLog running, Integer mode, Integer address,
                                      String finalStatus, Integer result, boolean modeSuccess,
                                      StopAction action) {
        if (running == null || running.getId() == null) {
            return false;
        }
        optLogService.updateRuntime(running.getId(), STOPPING, null);
        if (action != null && !action.execute()) {
            return false;
        }
        optLogService.updateRuntime(running.getId(), finalStatus, result);
        if (mode != null) {
            modeStatusService.markStopped(running.getPackNum(), mode, address, modeSuccess, running.getId());
        }
        return true;
    }

    public Integer resolveMode(Integer testType) {
        if (testType == null) {
            return null;
        }
        if (BatteryTestEnum._1.getDictValue().equals(testType)
                || BatteryTestEnum._6.getDictValue().equals(testType)) {
            return BatteryModeStatusService.MODE_INTERNAL_RESISTANCE;
        }
        if (BatteryTestEnum._2.getDictValue().equals(testType)) {
            return BatteryModeStatusService.MODE_CONNECT_RESISTANCE;
        }
        return null;
    }

    @FunctionalInterface
    public interface StopAction {
        boolean execute();
    }
}
