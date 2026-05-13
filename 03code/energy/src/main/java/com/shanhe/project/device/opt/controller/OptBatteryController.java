package com.shanhe.project.device.opt.controller;

import com.shanhe.common.constant.Constants;
import com.shanhe.framework.aspectj.lang.annotation.Log;
import com.shanhe.framework.enums.BatteryTestEnum;
import com.shanhe.framework.enums.BusinessType;
import com.shanhe.framework.web.controller.BaseController;
import com.shanhe.framework.web.domain.AjaxResult;
import com.shanhe.framework.web.page.TableDataInfo;
import com.shanhe.project.collector.battery.service.BatteryModeStatusService;
import com.shanhe.project.device.config.domain.DevBatteryOpt;
import com.shanhe.project.device.config.service.IDevBatteryOptService;
import com.shanhe.project.device.opt.domain.OptLog;
import com.shanhe.project.device.opt.service.ControlBattery;
import com.shanhe.project.device.opt.service.OptLogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

/**
 * 蓄电池测试操作参数
 *
 * @author wjh
 * @since 2025/4/21
 */
@RestController
@RequestMapping("/batteryOpt")
public class OptBatteryController extends BaseController {
    protected static Logger logger = LoggerFactory.getLogger(OptBatteryController.class);
    @Resource
    private IDevBatteryOptService devBatteryOptService;
    @Resource
    private ControlBattery controlBattery;
    @Resource
    private OptLogService optLogService;
    @Resource
    private BatteryModeStatusService batteryModeStatusService;

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
    public AjaxResult getInfo(@RequestParam(required = false) Long configId,
                              @RequestParam Integer packNum,
                              @RequestParam Integer testType) {
        return success(devBatteryOptService.selectDevBatteryOptByPackNum(Constants.DEFAULT_CONFIG_ID, packNum, testType));
    }

    /**
     * 计划执行任务
     */
    @Log(title = "蓄电池测试操作", businessType = BusinessType.UPDATE)
    @PostMapping("/edit")
    public AjaxResult edit(@RequestBody DevBatteryOpt devBatteryOpt) {
        applyDefaultConfigId(devBatteryOpt);
        devBatteryOpt.setIsSync(false);
        //发送指令到终端设备
        return controlBattery.toSendCmdToOat(devBatteryOpt);
    }

    /**
     * 立即执行蓄电池测试操作
     */
    @PostMapping("/doCmdOptBatteryTest")
    public AjaxResult doCmdOptBatteryTest(@RequestBody DevBatteryOpt devBatteryOpt) {
        applyDefaultConfigId(devBatteryOpt);
        BatteryTestEnum testEnum = BatteryTestEnum.find(devBatteryOpt.getTestType());
        OptLog opt = optLogService.getRunningOptLog(devBatteryOpt.getConfigId(),null,testEnum.getDictValue());
        if(opt!=null){
            return AjaxResult.error("蓄电池正在执行测试工作，请稍后再试！");
        }
        // 发送指令到终端设备
        AjaxResult result = controlBattery.toSendBatteryCmdToOat(devBatteryOpt);
        if ("0".equals(result.get("code").toString())) {
            //立即执行内阻测试，默认设置第一个电池在测试,后续异步实时查询结果更新结果内容
            if(testEnum == BatteryTestEnum._1){
                batteryModeStatusService.markRunning(
                        devBatteryOpt.getPackNum(),
                        BatteryModeStatusService.MODE_INTERNAL_RESISTANCE,
                        1);
            }
        }
        return result;
    }

    /**
     * 停止操作
     */
    @PostMapping("/doCmdStopBattery")
    public AjaxResult doCmdStopBattery(@RequestBody DevBatteryOpt devBatteryOpt) {
        //发送指令到终端设备
        applyDefaultConfigId(devBatteryOpt);
        return controlBattery.toSendStopBatteryCmdToOat(devBatteryOpt);
    }

    private void applyDefaultConfigId(DevBatteryOpt devBatteryOpt) {
        devBatteryOpt.setConfigId(Constants.DEFAULT_CONFIG_ID);
    }
}
