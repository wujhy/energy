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
        Long id = optLogService.insert(packNum, testType, null, source);
        optLogService.updateRuntime(id, STARTING, null);
        return id;
    }

    public void markRunning(Long businessOptLogId) {
        if (businessOptLogId != null) {
            optLogService.updateRuntime(businessOptLogId, RUNNING, null);
            optLogService.updateCache();
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
        optLogService.updateCache();
    }

    public boolean stop(Integer packNum, Integer testType, Integer mode, Integer address) {
        OptLog running = optLogService.getRunningOptLog(packNum, testType);
        if (running == null) {
            return false;
        }
        optLogService.updateRuntime(running.getId(), CANCELLED, 1);
        if (mode != null) {
            modeStatusService.markStopped(packNum, mode, address, true, running.getId());
        }
        optLogService.updateCache();
        return true;
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
}
