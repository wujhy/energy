package com.shanhe.project.monitor.cache;

import org.springframework.web.bind.annotation.*;
import com.shanhe.framework.web.controller.BaseController;
import com.shanhe.framework.web.domain.AjaxResult;
import com.shanhe.framework.web.service.CacheService;

import javax.annotation.Resource;

/**
 * 缓存监控
 *
 * @author wjh
 * @since 2024/12/16
 */
@RestController
@RequestMapping("/monitor/cache")
public class CacheController extends BaseController {

    @Resource
    private CacheService cacheService;

    /**
     * 获取缓存名称列表
     *
     * @return 缓存名称列表
     */
    @GetMapping("/getNames")
    public AjaxResult getCacheNames() {
        return AjaxResult.success(cacheService.getCacheNames());
    }

    /**
     * 获取指定缓存的所有键
     *
     * @param cacheName 缓存名称
     * @return 缓存键列表
     */
    @PostMapping("/getKeys")
    public AjaxResult getCacheKeys(String cacheName) {
        return AjaxResult.success(cacheService.getCacheKeys(cacheName));
    }

    /**
     * 获取指定缓存键的值
     *
     * @param cacheName 缓存名称
     * @param cacheKey 缓存键
     * @return 缓存值
     */
    @PostMapping("/getValue")
    public AjaxResult getCacheValue(String cacheName, String cacheKey) {
        return AjaxResult.success(cacheService.getCacheValue(cacheName, cacheKey));
    }

    /**
     * 清空指定缓存名称的所有数据
     *
     * @param cacheName 缓存名称
     * @return 操作结果
     */
    @PostMapping("/clearCacheName")
    @ResponseBody
    public AjaxResult clearCacheName(String cacheName) {
        cacheService.clearCacheName(cacheName);
        return AjaxResult.success();
    }

    /**
     * 清除指定缓存键
     *
     * @param cacheName 缓存名称
     * @param cacheKey 缓存键
     * @return 操作结果
     */
    @PostMapping("/clearCacheKey")
    @ResponseBody
    public AjaxResult clearCacheKey(String cacheName, String cacheKey) {
        cacheService.clearCacheKey(cacheName, cacheKey);
        return AjaxResult.success();
    }

    /**
     * 清除所有缓存
     *
     * @return 操作结果
     */
    @GetMapping("/clearAll")
    @ResponseBody
    public AjaxResult clearAll() {
        cacheService.clearAll();
        return AjaxResult.success();
    }
}
