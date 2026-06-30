package com.shanhe.project.manage.opt.controller;

import com.shanhe.common.constant.Constants;
import com.shanhe.framework.aspectj.lang.annotation.Log;
import com.shanhe.framework.enums.BatteryTestEnum;
import com.shanhe.framework.enums.BusinessType;
import com.shanhe.framework.web.controller.BaseController;
import com.shanhe.framework.web.domain.AjaxResult;
import com.shanhe.framework.web.page.TableDataInfo;
import com.shanhe.project.manage.config.domain.DevBatteryOpt;
import com.shanhe.project.manage.config.service.IDevBatteryOptService;
import com.shanhe.project.manage.opt.domain.OptLog;
import com.shanhe.project.manage.opt.service.BatteryOptExecuteType;
import com.shanhe.project.manage.opt.service.ControlBattery;
import com.shanhe.project.manage.opt.service.OptLogService;
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
    @Resource
    private IDevBatteryOptService devBatteryOptService;
    @Resource
    private ControlBattery controlBattery;
    @Resource
    private OptLogService optLogService;

    /**
     * 查询【蓄电池测试操作参数】列表
     */
    @GetMapping("/list")
    public TableDataInfo list(DevBatteryOpt devBatteryOpt) {
        startPage();
        List<DevBatteryOpt> list = devBatteryOptService.selectDevBatteryOptList(devBatteryOpt);
        return getDataTable(list);
    }

    /**
     * 获取【蓄电池测试操作参数】详细信息
     */
    @GetMapping(value = "/info")
    public AjaxResult getInfo(@RequestParam(name = "configId", required = false) Long ignoredConfigId,
                              @RequestParam Integer packNum,
                              @RequestParam Integer testType) {
        return success(devBatteryOptService.selectDevBatteryOptByPackNum(packNum, testType));
    }

    /**
     * 计划执行任务
     */
    @Log(title = "蓄电池测试操作", businessType = BusinessType.UPDATE)
    @PostMapping("/edit")
    public AjaxResult edit(@RequestBody DevBatteryOpt devBatteryOpt) {
        normalizeLocalOpt(devBatteryOpt);
        devBatteryOptService.insertDevBatteryOpt(devBatteryOpt);
        return success();
    }

    /**
     * 立即执行蓄电池测试操作
     */
    @PostMapping("/doCmdOptBatteryTest")
    public AjaxResult doCmdOptBatteryTest(@RequestBody DevBatteryOpt devBatteryOpt) {
        normalizeLocalOpt(devBatteryOpt);
        BatteryTestEnum testEnum = BatteryTestEnum.find(devBatteryOpt.getTestType());
        if (testEnum == null || BatteryTestEnum._99.equals(testEnum)) {
            return AjaxResult.error("下发蓄电池测试指令类型失败", 0);
        }
        OptLog opt = optLogService.getRunningOptLog(devBatteryOpt.getPackNum(), testEnum.getDictValue());
        if (opt != null) {
            return AjaxResult.error("蓄电池正在执行测试工作，请稍后再试！");
        }
        devBatteryOptService.insertDevBatteryOpt(devBatteryOpt);
        // 发送指令到终端设备
        return controlBattery.executeBatteryOpt(devBatteryOpt, BatteryOptExecuteType.MANUAL);
    }

    /** 统一页面入口写库字段，避免计划保存和立即执行使用不同默认值。 */
    private void normalizeLocalOpt(DevBatteryOpt devBatteryOpt) {
        devBatteryOpt.setConfigId(Constants.DEFAULT_CONFIG_ID);
        devBatteryOpt.setIsSync(false);
    }

    /**
     * 停止操作
     */
    @PostMapping("/doCmdStopBattery")
    public AjaxResult doCmdStopBattery(@RequestBody DevBatteryOpt devBatteryOpt) {
        //发送指令到终端设备
        devBatteryOpt.setConfigId(Constants.DEFAULT_CONFIG_ID);
        return controlBattery.toSendStopBatteryCmdToOat(devBatteryOpt);
    }
}
