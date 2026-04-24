package com.shanhe.common.utils;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.shanhe.common.exception.ServiceException;
import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.shanhe.common.utils.spring.SpringUtils;

/**
 * Cache工具类
 *
 * @author ruoyi
 */
public class CacheUtils {
    private static final Logger logger = LoggerFactory.getLogger(CacheUtils.class);

    private static final CacheManager CACHE_MANAGER = SpringUtils.getBean(CacheManager.class);

    private static final String SYS_CACHE = "sys-cache";

    /**
     * 获取SYS_CACHE缓存
     */
    public static Object get(String key) {
        return get(SYS_CACHE, key);
    }

    /**
     * 获取SYS_CACHE缓存（无则使用默认值）
     */
    public static Object get(String key, Object defaultValue) {
        Object value = get(key);
        return value != null ? value : defaultValue;
    }

    /**
     * 写入SYS_CACHE缓存
     */
    public static void put(String key, Object value) {
        put(SYS_CACHE, key, value);
    }

    /**
     * 从SYS_CACHE缓存中移除
     */
    public static void remove(String key) {
        remove(SYS_CACHE, key);
    }

    /**
     * 获取缓存
     */
    public static Object get(String cacheName, String key) {
        Element element = getCache(cacheName).get(key);
        if (element == null) {
            return null;
        }
        return element.getObjectValue();
    }

    /**
     * 获取缓存
     */
    public static Object get(String cacheName, String key, Object defaultValue) {
        Object value = get(cacheName, key);
        return value != null ? value : defaultValue;
    }

    /**
     * 写入缓存
     */
    public static void put(String cacheName, String key, Object value) {
        getCache(cacheName).put(new Element(key, value));
    }

    /**
     * 从缓存中移除
     */
    public static void remove(String cacheName, String key) {
        getCache(cacheName).remove(key);
    }

    /**
     * 从缓存中移除所有
     */
    public static void removeAll(String cacheName) {
        Cache cache = getCache(cacheName);
        cache.removeAll();
    }

    /**
     * 从缓存中移除指定key
     */
    public static void removeByKeys(Set<String> keys) {
        removeByKeys(SYS_CACHE, keys);
    }

    /**
     * 从缓存中移除指定key
     */
    public static void removeByKeys(String cacheName, Set<String> keys) {
        for (String key : keys) {
            remove(cacheName, key);
        }
        logger.debug("清理缓存： {} => {}", cacheName, keys);
    }

    /**
     * 获得一个Cache，没有则显示日志。
     */
    public static Cache getCache(String cacheName) {
        Cache cache = CACHE_MANAGER.getCache(cacheName);
        if (cache == null) {
            throw new ServiceException("当前系统中没有定义“" + cacheName + "”这个缓存。");
        }
        return cache;
    }


    /**
     * 获得一个Cache，没有则显示日志。
     */
    public static Set<String> getCacheKeys(String cacheName) {
        Cache cache = getCache(cacheName);
        List<String> keys = cache.getKeys();
        if (keys == null || keys.isEmpty()) {
            return Collections.emptySet();
        }
        return Collections.unmodifiableSet(new LinkedHashSet<>(keys));
    }

    /**
     * 获取所有缓存
     *
     * @return 缓存组
     */
    public static String[] getCacheNames() {
        return CACHE_MANAGER.getCacheNames();
    }
}
