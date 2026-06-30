package com.shanhe.project.iot.data;

import com.shanhe.project.manage.config.mapper.BatteryReportLogMapper;
import com.shanhe.project.manage.stat.mapper.StatBatteryBatMapper;
import com.shanhe.project.manage.stat.mapper.StatBatteryPackMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 启动运行器，初始化线程池并启动数据处理任务。
 *
 * @author wjh
 * @since 2026-05-25
 */
@Slf4j
@Component
public class StartupRunner implements ApplicationRunner {

    private static final ThreadPoolExecutor THREAD_POOL_EXECUTOR = getThreadPoolExecutor();

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
        THREAD_POOL_EXECUTOR.execute(() -> MessageFactory.starGainData(1000));
    }
}
