package com.shanhe.project.collector.battery.postprocess;

import com.shanhe.project.collector.battery.service.postprocess.BatteryRealtimePostProcessContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Comparator;
import java.util.List;

/**
 * 实时数据后处理流水线服务。
 * <p>
 * 按顺序执行所有注册的后处理器，每个处理器独立异常隔离。
 *
 * @author wjh
 * @since 2026-06-04
 */
@Slf4j
@Service
public class BatteryRealtimePostProcessService {

    @Resource
    private List<BatteryRealtimePostProcessor> processors;

    /**
     * 执行后处理流水线。
     *
     * @param context 后处理上下文
     */
    public void execute(BatteryRealtimePostProcessContext context) {
        if (context == null) {
            return;
        }
        processors.stream()
                .sorted(Comparator.comparingInt(BatteryRealtimePostProcessor::getOrder))
                .filter(p -> p.shouldProcess(context))
                .forEach(p -> executeProcessor(p, context));
    }

    private void executeProcessor(BatteryRealtimePostProcessor processor,
                                  BatteryRealtimePostProcessContext context) {
        try {
            processor.process(context);
        } catch (Exception e) {
            log.warn("后处理器执行失败, processor={}, packNum={}, 原因={}",
                    processor.getName(), context.getPackNum(), e.getMessage());
        }
    }
}
