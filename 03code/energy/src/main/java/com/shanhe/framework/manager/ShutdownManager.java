package com.shanhe.framework.manager;

import com.shanhe.common.utils.spring.SpringUtils;
import net.sf.ehcache.CacheManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import javax.annotation.PreDestroy;

/**
 * 确保应用退出时能关闭后台线程
 *
 * @author wjh
 * @since 2025/4/1
 */
@Component
public class ShutdownManager {
    private static final Logger logger = LoggerFactory.getLogger(ShutdownManager.class);

    private static final CacheManager CACHE_MANAGER = SpringUtils.getBean(CacheManager.class);

    @PreDestroy
    public void destroy()
    {
        shutdownAsyncManager();
        shutdownEhCacheManager();
    }

    private void shutdownAsyncManager() {
        try {
            logger.debug("====关闭后台任务任务线程池====");
            AsyncManager.me().shutdown();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    /**
     * 停止异步执行任务
     */
    private void shutdownEhCacheManager() {
        try {
            logger.debug("====关闭缓存====");
            CACHE_MANAGER.shutdown();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }
}
