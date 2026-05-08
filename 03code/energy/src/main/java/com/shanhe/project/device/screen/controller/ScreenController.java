package com.shanhe.project.device.screen.controller;

import com.shanhe.common.constant.Constants;
import com.shanhe.framework.aspectj.lang.annotation.Log;
import com.shanhe.framework.comm.CommServer;
import com.shanhe.framework.enums.BusinessType;
import com.shanhe.framework.web.controller.BaseController;
import com.shanhe.framework.web.domain.AjaxResult;
import com.shanhe.project.device.screen.service.ScreenService;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * 首页
 *
 * @author wjh
 * @since 2024-12-23
 */
@RestController
@RequestMapping("/screen")
public class ScreenController extends BaseController {

    @Resource
    private ScreenService screenService;

    // 系统介绍
    @GetMapping("/main")
    public AjaxResult screenMain() {
        return AjaxResult.success(screenService.main());
    }

    /**
     * 网点信息
     */
    @GetMapping("/host")
    public AjaxResult host() {
        return success(screenService.host());
    }

    /**
     * 设备列表
     */
    @GetMapping("/config")
    public AjaxResult config() {
        return success(screenService.configList());
    }

    /**
     * 设备详情
     */
    @GetMapping("/configDetail")
    public AjaxResult configDetail(@RequestParam(required = false) Long configId) {
        return success(screenService.config(Constants.DEFAULT_CONFIG_ID));
    }

    /**
     * 设备属性列表
     */
    @GetMapping("/attribute")
    public AjaxResult viewList(@RequestParam(required = false) Long configId, @RequestParam(required = false) Integer packNum, @RequestParam(required = false) Integer screen) {
        return success(screenService.attribute(Constants.DEFAULT_CONFIG_ID, packNum, screen));
    }

    /**
     * 设备属性下拉列表
     */
    @GetMapping("/attributeSelect")
    public AjaxResult selectList(@RequestParam(required = false) Long configId,
                                 @RequestParam(required = false) Integer packNum,
                                 @RequestParam(required = false) Integer screen,
                                 @RequestParam(required = false) Integer track) {
        return success(screenService.attributeSelect(Constants.DEFAULT_CONFIG_ID, packNum, screen, track));
    }

    // 系统首页
    @GetMapping()
    public String screen(ModelMap mMap) {
        mMap.put("host", screenService.host());
        return "screen";
    }

    /**
     * 电池
     */
    @GetMapping("/batteryList")
    @ResponseBody
    public AjaxResult batteryList()
    {
        return success(screenService.batteryList());
    }

    @Log(title = "执行指令", businessType = BusinessType.OTHER)
    @GetMapping( "/cmd")
    public AjaxResult cmd(String q)
    {
        CommServer.returnCmd(q);
        return success();
    }

    /**
     * 网点信息
     */
    @GetMapping("/alarmCount")
    @ResponseBody
    public AjaxResult alarmCount() {
        return success(screenService.alarmCount());
    }

}
