package com.shanhe.framework.manager;

import com.shanhe.common.utils.spring.SpringUtils;
import net.sf.ehcache.CacheManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import javax.annotation.PreDestroy;

/**
 * 确保应用退出时能关闭后台线程
 *
 * @author wjh
 * @since 2025/4/1
 */
@Slf4j
@Component
public class ShutdownManager {

    /** EhCache 缓存管理器实例，用于应用关闭时释放缓存资源。 */
    private static final CacheManager CACHE_MANAGER = SpringUtils.getBean(CacheManager.class);

    @PreDestroy
    public void destroy()
    {
        shutdownAsyncManager();
        shutdownEhCacheManager();
    }

    private void shutdownAsyncManager() {
        try {
            log.debug("====关闭后台任务任务线程池====");
            AsyncManager.me().shutdown();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    /** 停止异步执行任务 */
    private void shutdownEhCacheManager() {
        try {
            log.debug("====关闭缓存====");
            CACHE_MANAGER.shutdown();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }
}
