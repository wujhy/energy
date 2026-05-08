package com.shanhe.project.iot.data;

import com.shanhe.project.device.config.mapper.BatteryReportLogMapper;
import com.shanhe.project.energy.stat.mapper.StatBatteryBatMapper;
import com.shanhe.project.energy.stat.mapper.StatBatteryPackMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author zhoubin
 * @date 2024/11/19
 */
@Component
public class StartupRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(StartupRunner.class);
    private static final ThreadPoolExecutor threadPoolExecutor = getThreadPoolExecutor();

    @Resource
    private BatteryReportLogMapper batteryReportLogMapper;
    @Resource
    private StatBatteryPackMapper statBatteryPackMapper;
    @Resource
    private StatBatteryBatMapper statBatteryBatMapper;


    /**
     * 线程数量需要根据服务器CPU核心数来设置，最大可以设置CPU个数
     * @return
     */
    public static ThreadPoolExecutor getThreadPoolExecutor() {
        return new ThreadPoolExecutor(1, 1,
                60, TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(1),
                new ThreadPoolExecutor.DiscardOldestPolicy());
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        //启动日志工厂
        MessageFactory.initQueue(10000, batteryReportLogMapper, statBatteryPackMapper, statBatteryBatMapper);
        threadPoolExecutor.execute(() -> MessageFactory.starGainData(1000));
    }
}
