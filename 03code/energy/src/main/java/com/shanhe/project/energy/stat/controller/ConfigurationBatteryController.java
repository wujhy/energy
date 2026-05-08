package com.shanhe.project.energy.stat.controller;

import com.shanhe.common.constant.Constants;
import com.shanhe.framework.web.controller.BaseController;
import com.shanhe.framework.web.domain.AjaxResult;
import com.shanhe.project.device.config.service.IBatteryPackService;
import com.shanhe.project.energy.capacity.service.PreBatteryGroupService;
import com.shanhe.project.energy.stat.domain.DevBatteryMonomer;
import com.shanhe.project.energy.stat.service.IConfigurationBatteryService;
import com.shanhe.project.energy.stat.service.IDevBatteryMonomerService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

/**
 * @author zhoubin
 * @date 2025/8/14
 */
@RestController
@RequestMapping("/configuration/battery")
public class ConfigurationBatteryController extends BaseController {
    @Resource
    private IDevBatteryMonomerService devBatteryMonomerService;
    @Resource
    private IConfigurationBatteryService configurationBatteryService;
    @Resource
    private IBatteryPackService batteryPackService;
    @Resource
    private PreBatteryGroupService preBatteryGroupService;

    /**
     * 查询设备列表
     */
    @GetMapping("/listMonomer")
    public AjaxResult list(@RequestParam(required = false) Long configId, @RequestParam Integer packNum) {
        List<DevBatteryMonomer> list = devBatteryMonomerService.selectList(Constants.DEFAULT_CONFIG_ID, packNum);
        return success(list);
    }

    /**
     * 内阻警戒线
     */
    @GetMapping("/getResWarnLine")
    public AjaxResult getResWarnLine(@RequestParam(required = false) Long configId, @RequestParam Integer packNum) {
        return success(configurationBatteryService.getResWarnLine(Constants.DEFAULT_CONFIG_ID, packNum));
    }

    /**
     * 温度警戒线
     */
    @GetMapping("/getTempWarnLine")
    public AjaxResult getTempWarnLine(@RequestParam(required = false) Long configId, @RequestParam Integer packNum) {
        return success(configurationBatteryService.getTempWarnLine(Constants.DEFAULT_CONFIG_ID, packNum));
    }

    /**
     * 健康报告
     */
    @GetMapping("/getBatteryHealthReport")
    public AjaxResult getBatteryHealthReport(@RequestParam(required = false) Long configId, @RequestParam Integer packNum) {
        return success(configurationBatteryService.getBatteryHealthReport(Constants.DEFAULT_CONFIG_ID, packNum));
    }

    /**
     * 电池信息
     */
    @GetMapping("/getBatteryPack")
    public AjaxResult getBatteryPack(@RequestParam(required = false) Long configId, @RequestParam Integer packNum) {
        return success(batteryPackService.selectBatteryInfoByPackNum(Constants.DEFAULT_CONFIG_ID, packNum));
    }

    /**
     * 电池信息
     */
    @GetMapping("/getVoltageBalance")
    public AjaxResult getVoltageBalance(@RequestParam(required = false) Long configId, @RequestParam Integer packNum) {
        return success(batteryPackService.getVoltageBalance(Constants.DEFAULT_CONFIG_ID, packNum));
    }
    /**
     * 清除预估容量
     */
    @GetMapping("/clearPreBatteryGroup")
    public AjaxResult clearPreBatteryGroup(@RequestParam(required = false) Long configId, @RequestParam Integer packNum) {
        preBatteryGroupService.deleteByConfigId(Constants.DEFAULT_CONFIG_ID, packNum);
        preBatteryGroupService.updateCache();
        return success();
    }
}
