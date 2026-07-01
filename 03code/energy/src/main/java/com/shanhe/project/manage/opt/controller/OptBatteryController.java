package com.shanhe.project.manage.opt.controller;

import com.shanhe.framework.aspectj.lang.annotation.Log;
import com.shanhe.framework.enums.BusinessType;
import com.shanhe.framework.web.controller.BaseController;
import com.shanhe.framework.web.domain.AjaxResult;
import com.shanhe.framework.web.page.TableDataInfo;
import com.shanhe.project.manage.config.domain.DevBatteryOpt;
import com.shanhe.project.manage.config.service.IDevBatteryOptService;
import com.shanhe.project.manage.opt.service.BatteryOptExecuteType;
import com.shanhe.project.manage.opt.service.ControlBattery;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

/**
 * 蓄电池测试操作参数
 *
 * @author wjh
 * @since 2025/4/21
 */
@Slf4j
@RestController
@RequestMapping("/batteryOpt")
public class OptBatteryController extends BaseController {
    /** 蓄电池测试操作参数服务。 */
    @Resource
    private IDevBatteryOptService devBatteryOptService;
    /** 蓄电池设备控制服务。 */
    @Resource
    private ControlBattery controlBattery;

    /** 查询列表 */
    @GetMapping("/list")
    public TableDataInfo list(DevBatteryOpt devBatteryOpt) {
        startPage();
        List<DevBatteryOpt> list = devBatteryOptService.selectDevBatteryOptList(devBatteryOpt);
        return getDataTable(list);
    }

    /** 详细信息 */
    @GetMapping(value = "/info")
    public AjaxResult getInfo(@RequestParam(name = "configId", required = false) Long ignoredConfigId,
                              @RequestParam Integer packNum,
                              @RequestParam Integer testType) {
        return success(devBatteryOptService.selectDevBatteryOptByPackNum(packNum, testType));
    }

    /** 保存平台测试计划参数 */
    @Log(title = "蓄电池测试操作", businessType = BusinessType.UPDATE)
    @PostMapping("/edit")
    public AjaxResult edit(@RequestBody DevBatteryOpt devBatteryOpt) {
        devBatteryOptService.insertDevBatteryOpt(devBatteryOpt);
        return success();
    }

    /** 立即执行蓄电池测试操作 */
    @PostMapping("/doCmdOptBatteryTest")
    public AjaxResult doCmdOptBatteryTest(@RequestBody DevBatteryOpt devBatteryOpt) {
        // 立即执行只使用本次请求参数，不覆盖平台计划参数。
        return controlBattery.executeBatteryOpt(devBatteryOpt, BatteryOptExecuteType.MANUAL);
    }

    /** 停止操作 */
    @PostMapping("/doCmdStopBattery")
    public AjaxResult doCmdStopBattery(@RequestBody DevBatteryOpt devBatteryOpt) {
        //发送指令到终端设备
        return controlBattery.toSendStopBatteryCmdToOat(devBatteryOpt);
    }
}
