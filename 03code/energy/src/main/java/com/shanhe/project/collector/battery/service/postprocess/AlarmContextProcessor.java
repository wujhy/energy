package com.shanhe.project.collector.battery.service.postprocess;

import com.shanhe.project.collector.battery.model.BatteryModuleAlarmContext;
import com.shanhe.project.collector.battery.service.BatteryModuleAlarmAdaptService;
import com.shanhe.project.device.alarm.service.IAlarmLogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Map;

/**
 * 告警上下文适配处理器。
 * <p>
 * 从标准实时模型构建告警候选上下文，供 AlarmLogService 消费。
 *
 * @author wjh
 * @since 2026-06-04
 */
@Slf4j
@Component
public class AlarmContextProcessor implements BatteryRealtimePostProcessor {

    @Resource
    private BatteryModuleAlarmAdaptService alarmAdaptService;

    @Resource
    private IAlarmLogService alarmLogService;

    @Override
    public String getName() {
        return "alarmContext";
    }

    @Override
    public int getOrder() {
        return 50;
    }

    @Override
    public boolean shouldProcess(BatteryRealtimePostProcessContext context) {
        if (context == null) {
            return false;
        }
        if (context.getAlarmContext() != null && alarmLogService != null) {
            return true;
        }
        return alarmAdaptService != null && context.getPackNum() != null
                && ((context.getCells() != null && !context.getCells().isEmpty())
                || context.getChannelConfig() != null);
    }

    @Override
    public void process(BatteryRealtimePostProcessContext context) {
        if (context == null) {
            return;
        }
        try {
            BatteryModuleAlarmContext alarmContext = context.getAlarmContext();
            if (alarmContext == null && alarmAdaptService != null) {
                alarmContext = alarmAdaptService.buildContext(context.getGroup(), context.getCells());
            }
            if (alarmContext == null && alarmAdaptService != null) {
                alarmContext = new BatteryModuleAlarmContext();
                alarmContext.setPackNum(context.getPackNum());
            }
            if (alarmAdaptService != null && context.getChannelConfig() != null) {
                mergeAlarmContext(alarmContext, alarmAdaptService.buildCommunicationAlarmContext(
                        context.getPackNum(), context.getChannelConfig().getName()));
            }
            context.setAlarmContext(alarmContext);
            handleAlarmContext(context, alarmContext);
        } catch (Exception e) {
            log.warn("适配蓄电池模块告警上下文失败, 通道={}, 电池组={}",
                    context.getChannelConfig() == null ? null : context.getChannelConfig().getName(),
                    context.getPackNum(),
                    e);
        }
    }

    private void mergeAlarmContext(BatteryModuleAlarmContext target, BatteryModuleAlarmContext source) {
        if (target == null || source == null || source.isEmpty()) {
            return;
        }
        if (source.getPackWarnParam() != null) {
            target.getPackWarnParam().putAll(source.getPackWarnParam());
        }
        if (source.getCellWarnParam() == null || source.getCellWarnParam().isEmpty()) {
            return;
        }
        for (Map.Entry<Integer, Map<String, String>> entry : source.getCellWarnParam().entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null || entry.getValue().isEmpty()) {
                continue;
            }
            target.getCellWarnParam()
                    .computeIfAbsent(entry.getKey(), key -> new java.util.LinkedHashMap<>())
                    .putAll(entry.getValue());
        }
    }

    private void handleAlarmContext(BatteryRealtimePostProcessContext context,
                                    BatteryModuleAlarmContext alarmContext) {
        if (alarmLogService == null || alarmContext == null || alarmContext.isEmpty()) {
            return;
        }
        Integer packNum = alarmContext.getPackNum() == null ? context.getPackNum() : alarmContext.getPackNum();
        if (alarmContext.getPackWarnParam() != null && !alarmContext.getPackWarnParam().isEmpty()) {
            alarmLogService.alarmBatteryValue(null, packNum, null, alarmContext.getPackWarnParam());
        }
        if (alarmContext.getCellWarnParam() == null || alarmContext.getCellWarnParam().isEmpty()) {
            return;
        }
        for (Map.Entry<Integer, Map<String, String>> entry : alarmContext.getCellWarnParam().entrySet()) {
            Map<String, String> warnParam = entry.getValue();
            if (warnParam == null || warnParam.isEmpty()) {
                continue;
            }
            alarmLogService.alarmBatteryValue(null, packNum, entry.getKey(), warnParam);
        }
    }
}
