package com.shanhe.project.manage.opt.controller;

import com.shanhe.framework.aspectj.lang.annotation.Log;
import com.shanhe.framework.enums.BusinessType;
import com.shanhe.framework.web.controller.BaseController;
import com.shanhe.framework.web.domain.AjaxResult;
import com.shanhe.project.manage.opt.service.ControlBatterySet;
import com.shanhe.project.manage.opt.vo.BatterySetVO;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * 蓄电池设置
 *
 * @author wjh
 * @since 2025/7/11
 */
@RestController
@RequestMapping("/battery/set")
public class OptBatterySetController extends BaseController {

    @Resource
    private ControlBatterySet controlBatterySet;

    /**
     * 手动编号
     */
    @Log(title = "手动编号", businessType = BusinessType.UPDATE)
    @PostMapping("/manualModelNum")
    public AjaxResult manualModelNum(@RequestBody @Validated(value = BatterySetVO.cmd08.class) BatterySetVO batterySetVO) {
        return controlBatterySet.manualModelNum(batterySetVO);
    }

    /**
     * 自动编号
     */
    @Log(title = "自动编号", businessType = BusinessType.UPDATE)
    @PostMapping("/autoModelNum")
    public AjaxResult autoModelNum(@RequestBody @Validated(value = BatterySetVO.cmd18.class) BatterySetVO batterySetVO) {
        return controlBatterySet.autoModelNum(batterySetVO);
    }

    /**
     * 获取电池版本号
     */
    @PostMapping("/batteryVersion")
    public AjaxResult batteryVersion(@RequestBody @Validated(value = BatterySetVO.cmd.class) BatterySetVO batterySetVO) {
        return controlBatterySet.batteryVersion(batterySetVO);
    }

    /**
     * 获取编号状态
     */
    @PostMapping("/getModelNum")
    public AjaxResult getModelNum(@RequestBody @Validated(value = BatterySetVO.cmd18.class) BatterySetVO batterySetVO) {
        return controlBatterySet.refreshModelNum(batterySetVO);
    }

    /**
     * 清除编号数据
     */
    @PostMapping("/clearModelNum")
    public AjaxResult clearModelNum(@RequestBody @Validated(value = BatterySetVO.cmd18.class) BatterySetVO batterySetVO) {
        return controlBatterySet.clearModelNum(batterySetVO);
    }

    /**
     * 设置内阻系数
     */
    @Log(title = "内阻系数", businessType = BusinessType.UPDATE)
    @PostMapping("/resistance")
    public AjaxResult resistance(@RequestBody @Validated(value = BatterySetVO.cmd19.class) BatterySetVO batterySetVO) {
        return controlBatterySet.resistance(batterySetVO);
    }

    /** 获取内阻基准值 */
    @PostMapping("/resistanceDefaultValue")
    public AjaxResult resistanceDefaultValue(@RequestBody @Validated(value = BatterySetVO.cmd1.class) BatterySetVO batterySetVO) {
        return controlBatterySet.resistanceDefaultValue(batterySetVO);
    }

    /** 获取内阻基准值 */
    @PostMapping("/resistanceValue")
    public AjaxResult resistanceValue(@RequestBody @Validated(value = BatterySetVO.cmd1.class) BatterySetVO batterySetVO) {
        return controlBatterySet.resistanceValue(batterySetVO);
    }

    /**
     * 设置内阻基准值
     */
    @Log(title = "内阻基准值", businessType = BusinessType.UPDATE)
    @PostMapping("/resistanceValueSet")
    public AjaxResult resistanceValueSet(@RequestBody @Validated(value = BatterySetVO.cmd119.class) BatterySetVO batterySetVO) {
        return controlBatterySet.resistanceValueSet(batterySetVO);
    }

    /**
     * 清除鼓包数据
     */
    @Log(title = "清鼓包数据", businessType = BusinessType.UPDATE)
    @PostMapping("/delGb")
    public AjaxResult delGb(@RequestBody @Validated(value = BatterySetVO.cmd20.class) BatterySetVO batterySetVO) {
        return controlBatterySet.delGb(batterySetVO);
    }

    /**
     * 获取均衡状态
     */
    @PostMapping("/getBalanced")
    public AjaxResult getBalanced(@RequestBody @Validated(value = BatterySetVO.cmd.class) BatterySetVO batterySetVO) {
        return controlBatterySet.getBalanced(batterySetVO);
    }

    /**
     * 设置组均衡
     */
    @Log(title = "组均衡", businessType = BusinessType.UPDATE)
    @PostMapping("/balanced")
    public AjaxResult balanced(@RequestBody @Validated(value = BatterySetVO.cmd38.class) BatterySetVO batterySetVO) {
        return controlBatterySet.balanced(batterySetVO);
    }

    /**
     * 单体均衡控制（600 03/83）。
     */
    @Log(title = "单体均衡", businessType = BusinessType.UPDATE)
    @PostMapping("/singleBalance")
    public AjaxResult singleBalance(@RequestBody BatterySetVO batterySetVO) {
        return controlBatterySet.singleBatteryBalance(
                batterySetVO.getPackNum(),
                batterySetVO.getModelNum(),
                batterySetVO.getBalancedValue());
    }

    /**
     * 清除电池组数据
     */
    @Log(title = "清组数据", businessType = BusinessType.UPDATE)
    @PostMapping("/delPack")
    public AjaxResult delPack(@RequestBody @Validated(value = BatterySetVO.cmd78.class) BatterySetVO batterySetVO) {
        return controlBatterySet.delPack(batterySetVO);
    }

    /**
     * 清除主机数据
     */
    @Log(title = "清主机数据", businessType = BusinessType.UPDATE)
    @PostMapping("/delHost")
    public AjaxResult delHost(@RequestBody @Validated(value = BatterySetVO.cmd.class) BatterySetVO batterySetVO) {
        return controlBatterySet.delHost(batterySetVO);
    }

    /**
     * 设备复位
     */
    @Log(title = "设备复位", businessType = BusinessType.UPDATE)
    @PostMapping("/reset")
    public AjaxResult reset(@RequestBody @Validated(value = BatterySetVO.cmd.class) BatterySetVO batterySetVO) {
        return controlBatterySet.reset(batterySetVO);
    }

    /**
     * 恢复出厂设置
     */
    @Log(title = "出厂设置", businessType = BusinessType.UPDATE)
    @PostMapping("/restore")
    public AjaxResult restore(@RequestBody @Validated(value = BatterySetVO.cmd.class) BatterySetVO batterySetVO) {
        return controlBatterySet.restore(batterySetVO);
    }

    /**
     * 时间同步
     */
    @Log(title = "时间同步", businessType = BusinessType.UPDATE)
    @PostMapping("/syncTime")
    public AjaxResult syncTime(@RequestBody @Validated(value = BatterySetVO.cmd37.class) BatterySetVO batterySetVO) {
        return controlBatterySet.syncTime(batterySetVO);
    }

    /**
     * 设置蜂鸣器状态
     */
    @Log(title = "蜂鸣器", businessType = BusinessType.UPDATE)
    @PostMapping("/buzzer")
    public AjaxResult buzzerStatus(@RequestBody @Validated(value = BatterySetVO.cmd39.class) BatterySetVO batterySetVO) {
        return controlBatterySet.buzzerStatus(batterySetVO);
    }

    /**
     * 电池数据校正
     */
    @Log(title = "电池数据校正", businessType = BusinessType.UPDATE)
    @PostMapping("/correct")
    public AjaxResult correct(@RequestBody @Validated(value = BatterySetVO.cmd76.class) BatterySetVO batterySetVO) {
        return controlBatterySet.correct(batterySetVO);
    }
}
