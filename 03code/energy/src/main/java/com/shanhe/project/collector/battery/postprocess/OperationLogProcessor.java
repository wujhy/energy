package com.shanhe.project.collector.battery.postprocess;


import com.shanhe.framework.enums.BatteryPackStatusEnum;
import com.shanhe.project.collector.battery.model.BatteryModuleGroupRealtime;
import com.shanhe.project.manage.opt.service.OptLogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Date;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 操作日志后处理器。
 * <p>
 * 直接使用标准实时模型状态驱动操作日志，仅在电池组状态已明确时触发。
 *
 * @author wjh
 * @since 2026-06-05
 */
@Slf4j
@Component
public class OperationLogProcessor implements BatteryRealtimePostProcessor {

    @Resource
    private OptLogService optLogService;
    /** 上次标准实时快照时间缓存，用于判断测试结束时间。 */
    private final Map<Integer, Date> lastRealtimeTimeCache = new ConcurrentHashMap<>();

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
        BatteryModuleGroupRealtime group = context.getGroup();
        Integer batteryPackStatusValue = group == null ? null : group.getBatteryPackStatus();
        String batteryPackStatus = Objects.toString(batteryPackStatusValue, null);
        if (!isKnownBatteryPackStatus(batteryPackStatus)) {
            log.debug("操作日志后处理跳过：电池组状态未确认, packNum={}, status={}", packNum, batteryPackStatus);
            return;
        }

        try {
            optLogService.insertBatteryRealtime(packNum,
                    batteryPackStatusValue,
                    group.getResistanceTestStatus(),
                    lastRealtimeTimeCache.get(packNum));
            if (group.getCreateTime() != null) {
                lastRealtimeTimeCache.put(packNum, group.getCreateTime());
            }
        } catch (Exception e) {
            log.warn("操作日志后处理失败, packNum={}", packNum, e);
        }
    }

    /** 判断电池组状态是否为已知枚举值，未知状态跳过操作日志写入。 */
    private boolean isKnownBatteryPackStatus(String status) {
        return BatteryPackStatusEnum.find(status) != null;
    }

}
