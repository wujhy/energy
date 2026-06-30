package com.shanhe.project.collector.battery.postprocess;


import com.shanhe.framework.enums.BatteryPackStatusEnum;
import com.shanhe.project.manage.config.domain.BatteryReportLog;
import com.shanhe.project.manage.config.service.BatteryReportLogService;
import com.shanhe.project.manage.opt.service.OptLogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 操作日志后处理器。
 * <p>
 * 将标准实时模型适配为旧操作日志服务入参，仅在电池组状态已明确时触发。
 *
 * @author wjh
 * @since 2026-06-05
 */
@Slf4j
@Component
public class OperationLogProcessor implements BatteryRealtimePostProcessor {

    @Resource
    private OptLogService optLogService;

    @Resource
    private BatteryReportLogService batteryReportLogService;

    /** 上次标准实时快照缓存，用于关闭兼容历史写入后仍能判断测试结束时间。 */
    private final Map<Integer, BatteryReportLog> lastReportLogCache = new ConcurrentHashMap<>();

    @Override
    public String getName() {
        return "operationLog";
    }

    @Override
    public int getOrder() {
        return 250;
    }

    @Override
    public boolean shouldProcess(BatteryRealtimePostProcessContext context) {
        return context != null
                && context.getPackNum() != null
                && PostProcessBatchGuard.sameRealtimeBatch(context);
    }

    @Override
    public void process(BatteryRealtimePostProcessContext context) {
        Integer packNum = context.getPackNum();
        BatteryReportLog report = RealtimeToReportLogAdapter.adapt(
                packNum, context.getGroup(), context.getCells());
        Map<String, Object> packParam = report.getPackParam();
        String batteryPackStatus = Objects.toString(packParam.get("batteryPackStatus"), null);
        if (!isKnownBatteryPackStatus(batteryPackStatus)) {
            log.debug("操作日志后处理跳过：电池组状态未确认, packNum={}, status={}", packNum, batteryPackStatus);
            return;
        }

        try {
            BatteryReportLog oldInfo = resolveOldInfo(packNum);
            optLogService.insertBattery(packNum, packParam, oldInfo);
            lastReportLogCache.put(packNum, report);
        } catch (Exception e) {
            log.warn("操作日志后处理失败, packNum={}", packNum, e);
        }
    }

    /** 优先使用上一标准实时快照；服务刚启动无缓存时回退旧兼容缓存。 */
    private BatteryReportLog resolveOldInfo(Integer packNum) {
        BatteryReportLog oldInfo = lastReportLogCache.get(packNum);
        if (oldInfo != null || batteryReportLogService == null) {
            return oldInfo;
        }
        return batteryReportLogService.lastCache(packNum);
    }

    /** 判断电池组状态是否为已知枚举值，未知状态跳过操作日志写入。 */
    private boolean isKnownBatteryPackStatus(String status) {
        return BatteryPackStatusEnum.find(status) != null;
    }

}
