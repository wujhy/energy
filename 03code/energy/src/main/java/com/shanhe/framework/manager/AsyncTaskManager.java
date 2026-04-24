package com.shanhe.framework.manager;

import com.shanhe.common.utils.spring.SpringUtils;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * 封装iot线程池，避免频繁创建销毁线程池
 *
 * @author wjh
 * @since 2024/12/20
 */
public class AsyncTaskManager {

    /**
     * 异步IOT操作任务调度线程池
     */
    private final ThreadPoolTaskExecutor executor = SpringUtils.getBean("threadPoolTaskExecutor");

    /**
     * 单例模式
     */
    private AsyncTaskManager() {}

    private static final AsyncTaskManager ME = new AsyncTaskManager();

    public static AsyncTaskManager me()
    {
        return ME;
    }

    /**
     * 执行任务（操作延迟10毫秒）
     * 
     * @param runnable 任务
     */
    public void execute(Runnable runnable) {
        executor.execute(runnable);
    }

    /**
     * 停止任务线程池
     */
    public void shutdown() {
        executor.shutdown();
    }
}
