package com.shanhe.project.device.host.controller;

import cn.hutool.core.util.StrUtil;
import com.shanhe.common.exception.ServiceException;
import com.shanhe.common.utils.CacheUtils;
import com.shanhe.common.utils.file.JarUploadUtils;
import com.shanhe.common.utils.uuid.IdUtils;
import com.shanhe.framework.enums.CacheKeyEnum;
import com.shanhe.project.monitor.server.service.SystemService;
import org.springframework.web.bind.annotation.*;
import com.shanhe.framework.aspectj.lang.annotation.Log;
import com.shanhe.framework.enums.BusinessType;
import com.shanhe.project.device.host.domain.Host;
import com.shanhe.project.device.host.service.IHostService;
import com.shanhe.framework.web.controller.BaseController;
import com.shanhe.framework.web.domain.AjaxResult;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.util.Objects;

/**
 * 主机Controller
 * 
 * @author wjh
 * @since 2024-12-23
 */
@RestController
@RequestMapping("/device/host")
public class HostController extends BaseController
{
    @Resource
    private IHostService hostService;

    @GetMapping("/detail")
    public AjaxResult detail() {
        Host host = hostService.getDetail();
        return success(host);
    }

    @Log(title = "修改主机", businessType = BusinessType.UPDATE)
    @PostMapping("/edit")
    @ResponseBody
    public AjaxResult editSave(Host host) {
        hostService.updateHost(host);
        return success();
    }

    @Log(title = "更新设备上报间隔时间", businessType = BusinessType.UPDATE)
    @GetMapping("/spaceTime/{spaceTime}")
    @ResponseBody
    public AjaxResult updateSpaceTime(@PathVariable("spaceTime") Integer spaceTime) {
        hostService.updateSpaceTime(spaceTime);
        return success();
    }

    @Log(title = "更新设备数据存储间隔时间", businessType = BusinessType.UPDATE)
    @GetMapping("/storageTime/{storageTime}")
    @ResponseBody
    public AjaxResult updateStorageTime(@PathVariable("storageTime") Integer storageTime) {
        hostService.updateStorageTime(storageTime);
        return success();
    }

    @Log(title = "更新设备数据删除时间", businessType = BusinessType.UPDATE)
    @GetMapping("/cleanLogDays/{cleanLogDays}")
    @ResponseBody
    public AjaxResult updateCleanLogDays(@PathVariable("cleanLogDays") Integer cleanLogDays) {
        hostService.updateCleanLogDays(cleanLogDays);
        return success();
    }

    @GetMapping("/syncSpaceTime")
    @ResponseBody
    public AjaxResult syncSpaceTime() {
        hostService.syncSpaceTime();
        return success();
    }

    @Log(title = "更新主机IP", businessType = BusinessType.UPDATE)
    @PostMapping("/editIp")
    @ResponseBody
    public AjaxResult editIp(Host host) {
        hostService.updateIp(host);
        return success();
    }

    @GetMapping("/syncIp")
    @ResponseBody
    public AjaxResult syncIp()
    {
        hostService.syncIp();
        return success();
    }

    @Log(title = "更新主机服务IP", businessType = BusinessType.UPDATE)
    @PostMapping("/editReportIp")
    @ResponseBody
    public AjaxResult editReportIp(Host host)
    {
        hostService.updateReportIp(host);
        return success();
    }

    @GetMapping("/syncReportIp")
    @ResponseBody
    public AjaxResult syncReportIp()
    {
        hostService.syncReportIp();
        return success();
    }

    /** 更新服务器时间 */
    @GetMapping("/syncServerTime")
    public AjaxResult syncServerTime(@RequestParam("datetime") String datetime) {
        if (StrUtil.isBlank(datetime)) {
            throw new ServiceException("服务器时间不能为空！");
        }
        hostService.syncServerTime(datetime);
        return success();
    }

    /** 获取随机imei */
    @GetMapping("/getImei")
    public AjaxResult getImei() {
        return success(IdUtils.genImei());
    }

    /** 上传升级包 */
    @PostMapping("/uploadSoft")
    @ResponseBody
    public AjaxResult uploadSoft(MultipartFile file) {
        try {
            // 上传并返回新文件名称
            JarUploadUtils.uploadSoft(file);
            // 标记缓存
            CacheUtils.put(CacheKeyEnum.DEPLOY_STATUS.getCache(), CacheKeyEnum.DEPLOY_STATUS.getKey(), 1);
            return success();
        } catch (Exception e) {
            return AjaxResult.error(e.getMessage());
        }
    }

    @Log(title = "软件升级", businessType = BusinessType.UPDATE)
    @GetMapping("/deploy")
    @ResponseBody
    public AjaxResult deploy() {
        if (!Objects.equals(CacheUtils.get(CacheKeyEnum.DEPLOY_STATUS.getCache(), CacheKeyEnum.DEPLOY_STATUS.getKey()), 1)) {
            return AjaxResult.error("请先上传升级包！！");
        }
        CacheUtils.remove(CacheKeyEnum.DEPLOY_STATUS.getCache(), CacheKeyEnum.DEPLOY_STATUS.getKey());
        if (SystemService.isWin()) {
            return AjaxResult.error("暂不支持window自动升级！！");
        }
        SystemService.deploy();
        return success();
    }
}
