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

    @GetMapping("/getNames")
    public AjaxResult getCacheNames() {
        return AjaxResult.success(cacheService.getCacheNames());
    }

    @PostMapping("/getKeys")
    public AjaxResult getCacheKeys(String cacheName) {
        return AjaxResult.success(cacheService.getCacheKeys(cacheName));
    }

    @PostMapping("/getValue")
    public AjaxResult getCacheValue(String cacheName, String cacheKey) {
        return AjaxResult.success(cacheService.getCacheValue(cacheName, cacheKey));
    }

    @PostMapping("/clearCacheName")
    @ResponseBody
    public AjaxResult clearCacheName(String cacheName) {
        cacheService.clearCacheName(cacheName);
        return AjaxResult.success();
    }

    @PostMapping("/clearCacheKey")
    @ResponseBody
    public AjaxResult clearCacheKey(String cacheName, String cacheKey) {
        cacheService.clearCacheKey(cacheName, cacheKey);
        return AjaxResult.success();
    }

    @GetMapping("/clearAll")
    @ResponseBody
    public AjaxResult clearAll() {
        cacheService.clearAll();
        return AjaxResult.success();
    }
}
