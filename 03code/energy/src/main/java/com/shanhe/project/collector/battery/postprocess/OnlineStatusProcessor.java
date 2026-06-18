package com.shanhe.project.collector.battery.postprocess;


import com.shanhe.common.utils.CacheUtils;
import com.shanhe.framework.enums.CacheKeyEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * 在线状态缓存更新处理器。
 * <p>
 * 每次收到实时数据时更新电池组在线状态缓存。
 *
 * @author wjh
 * @since 2026-06-04
 */
@Slf4j
@Component
public class OnlineStatusProcessor implements BatteryRealtimePostProcessor {

    @Override
    public String getName() {
        return "onlineStatus";
    }

    @Override
    public int getOrder() {
        return 10;
    }

    @Override
    public boolean shouldProcess(BatteryRealtimePostProcessContext context) {
        return context.getPackNum() != null;
    }

    @Override
    public void process(BatteryRealtimePostProcessContext context) {
        Integer packNum = context.getPackNum();
        String key = String.format(CacheKeyEnum.BATTERY_ONLINE.getKey(), packNum);
        CacheUtils.put(CacheKeyEnum.BATTERY_ONLINE.getCache(), key, new Date());
    }
}
