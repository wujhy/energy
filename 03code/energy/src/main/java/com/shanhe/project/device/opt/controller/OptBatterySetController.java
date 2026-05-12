package com.shanhe.project.device.opt.controller;

import com.shanhe.framework.aspectj.lang.annotation.Log;
import com.shanhe.framework.enums.BatteryCidEnum;
import com.shanhe.framework.enums.BusinessType;
import com.shanhe.framework.enums.DeviceTypeEnum;
import com.shanhe.framework.enums.ItemCode;
import com.shanhe.framework.comm.tcp.utils.CodingUtil;
import com.shanhe.framework.manager.AsyncTaskManager;
import com.shanhe.framework.web.controller.BaseController;
import com.shanhe.framework.web.domain.AjaxResult;
import com.shanhe.project.device.config.domain.Config;
import com.shanhe.project.device.config.domain.ConfigAttribute;
import com.shanhe.project.device.config.service.*;
import com.shanhe.project.device.opt.service.ControlBattery;
import com.shanhe.project.device.opt.service.ControlBatterySet;
import com.shanhe.project.device.opt.service.RestoreService;
import com.shanhe.project.device.opt.vo.BatterySetVO;
import com.shanhe.project.iot.model.BatteryModeInfo;
import com.shanhe.project.monitor.server.service.SystemService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.*;

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
    @Resource
    private BatteryReportLogService batteryReportLogService;
    @Resource
    private IConfigAttributeService configAttributeService;
    @Resource
    public IConfigService configService;
    @Resource
    private ControlBattery controlBattery;
    @Resource
    private RestoreService restoreService;

    @Log(title = "手动编号", businessType = BusinessType.UPDATE)
    @PostMapping("/manualModelNum")
    public AjaxResult manualModelNum(@RequestBody @Validated(value = BatterySetVO.cmd08.class) BatterySetVO batterySetVO) {
        batterySetVO.setNeedDynResult(false);
        return controlBatterySet.doSet(batterySetVO, BatteryCidEnum._08);
    }

    @Log(title = "自动编号", businessType = BusinessType.UPDATE)
    @PostMapping("/autoModelNum")
    public AjaxResult autoModelNum(@RequestBody @Validated(value = BatterySetVO.cmd18.class) BatterySetVO batterySetVO) {
        BatteryModeInfo modelResult = controlBatterySet.getModelResult(batterySetVO);
        if (modelResult != null) {
            if (modelResult.getMode() == 0 && modelResult.getStatus() == 0) {
            } else {
                // 当前模式 0： 无测试 1： 自动编号 6： 内阻测试 10：连接条电阻测试 */
                String mode = modelResult.getMode() == 1 ? "自动编号" : modelResult.getMode() == 6 ? "内阻测试" : modelResult.getMode() == 10 ? "连接条电阻测试" : "无";
                return error("正在进行" + mode + "，请勿进行其他操作");
            }
        }

        batterySetVO.setNeedDynResult(false);
        return controlBatterySet.doSet(batterySetVO, BatteryCidEnum._18);
    }

    @PostMapping("/batteryVersion")
    public AjaxResult batteryVersion(@RequestBody @Validated(value = BatterySetVO.cmd.class) BatterySetVO batterySetVO) {
        // 下发指令
        return controlBatterySet.doSet(batterySetVO, BatteryCidEnum._0E);
    }

    @PostMapping("/getModelNum")
    public AjaxResult getModelNum(@RequestBody @Validated(value = BatterySetVO.cmd18.class) BatterySetVO batterySetVO) {
        AsyncTaskManager.me().execute(
                new TimerTask() {
                    @Override
                    public void run() {
                        try {
                            batterySetVO.setNeedDynResult(false);
                            controlBatterySet.doSet(batterySetVO, BatteryCidEnum._3B);
                        } catch (Exception  e) {
                            logger.error("设备缓存出错：{}", e.getMessage(), e);
                        }
                    }
                }
        );
        // 直接获取结果
        return success(controlBatterySet.getModelResult(batterySetVO));
    }

    @PostMapping("/clearModelNum")
    public AjaxResult clearModelNum(@RequestBody @Validated(value = BatterySetVO.cmd18.class) BatterySetVO batterySetVO) {
        // 直接获取结果
        return controlBatterySet.clearModelNum(batterySetVO.getConfigId(), null);
    }

    @Log(title = "内阻系数", businessType = BusinessType.UPDATE)
    @PostMapping("/resistance")
    public AjaxResult resistance(@RequestBody @Validated(value = BatterySetVO.cmd19.class) BatterySetVO batterySetVO) {
        batterySetVO.setNeedDynResult(false);
        return controlBatterySet.doSet(batterySetVO, BatteryCidEnum._19);
    }

    /** 获取内阻基准值 */
    @PostMapping("/resistanceDefaultValue")
    public AjaxResult resistanceDefaultValue(@RequestBody @Validated(value = BatterySetVO.cmd1.class) BatterySetVO batterySetVO) {
        // 内阻过大告警系数、最小告警系数
        ConfigAttribute configAttribute = configAttributeService.getBy(batterySetVO.getConfigId(), batterySetVO.getPackNum(), ItemCode.DTNZGD.getCode());
        if (configAttribute == null) {
            configAttribute = configAttributeService.getBy(batterySetVO.getConfigId(), batterySetVO.getPackNum(), ItemCode.DTNZGX.getCode());
        }
        if (configAttribute == null || configAttribute.getListLevel() == null || configAttribute.getListLevel().isEmpty()) {
            return success(0L);
        }
        // 首个告警等级基准值
        return success(configAttribute.getListLevel().get(0).getStandValue());
    }

    /** 获取内阻基准值 */
    @PostMapping("/resistanceValue")
    public AjaxResult resistanceValue(@RequestBody @Validated(value = BatterySetVO.cmd1.class) BatterySetVO batterySetVO) {
        return success(batteryReportLogService.resistanceValue(batterySetVO.getConfigId(), batterySetVO.getPackNum()));
    }

    @Log(title = "内阻基准值", businessType = BusinessType.UPDATE)
    @PostMapping("/resistanceValueSet")
    public AjaxResult resistanceValueSet(@RequestBody @Validated(value = BatterySetVO.cmd119.class) BatterySetVO batterySetVO) {
        AjaxResult ajaxResult = null;
        batterySetVO.setParamNum("53");
        if (batterySetVO.getConfigId() == null || Objects.equals(batterySetVO.getConfigId(), 0L)) {
            return error("设备ID不能为空！！！");
        }
        Config config = configService.selectConfigByConfigId(batterySetVO.getConfigId());
        if (config == null || !Objects.equals(config.getType(), DeviceTypeEnum._1.getDictValue())) {
            return error("非蓄电池设备，同步失败！！！");
        }

        batterySetVO.setParamValue(CodingUtil.integerToHexString(batterySetVO.getResistanceStandValue(), 4));
        for (int i = 1; i < 4; i++) {
            batterySetVO.setLevel(i);
            batterySetVO.setNeedDynResult(false);
            ajaxResult = controlBatterySet.doSet(batterySetVO, BatteryCidEnum._03);
        }

        // 同步
        controlBattery.doSynBatteryAlarm(config, batterySetVO.getPackNum(), false);
        return ajaxResult;
    }

    @Log(title = "清鼓包数据", businessType = BusinessType.UPDATE)
    @PostMapping("/delGb")
    public AjaxResult delGb(@RequestBody @Validated(value = BatterySetVO.cmd20.class) BatterySetVO batterySetVO) {
        return controlBatterySet.doSet(batterySetVO, BatteryCidEnum._20);
    }

    @PostMapping("/getBalanced")
    public AjaxResult getBalanced(@RequestBody @Validated(value = BatterySetVO.cmd.class) BatterySetVO batterySetVO) {
        Map<String, Object> mapAll = configService.getExtend(batterySetVO.getConfigId());
        Map<String, Object> map = new HashMap<>();
        map.put("autoBalanced", mapAll != null && mapAll.get("autoBalanced") != null ? mapAll.get("autoBalanced") : 0);
        map.put("manualBalanced", mapAll != null && mapAll.get("manualBalanced") != null ? mapAll.get("manualBalanced") : 0);
        map.put("buzzerStatus", mapAll != null && mapAll.get("buzzerStatus") != null ? mapAll.get("buzzerStatus") : 0);
        return success(map);
    }

    @Log(title = "组均衡", businessType = BusinessType.UPDATE)
    @PostMapping("/balanced")
    public AjaxResult balanced(@RequestBody @Validated(value = BatterySetVO.cmd38.class) BatterySetVO batterySetVO) {
        AjaxResult result = controlBatterySet.doSet(batterySetVO, BatteryCidEnum._38);

        Map<String, Object> mapAll = configService.getExtend(batterySetVO.getConfigId());
        mapAll = mapAll == null ? new HashMap<>() : mapAll;

        // 均衡设置成功，更新
        if (Objects.equals(result.get(AjaxResult.CODE_TAG), AjaxResult.Type.SUCCESS.value())) {
            mapAll.put("autoBalanced", batterySetVO.getAutoBalanced());
            mapAll.put("manualBalanced", batterySetVO.getManualBalanced());
            configService.updateExtend(batterySetVO.getConfigId(), mapAll);
        }
        return result;
    }

    @Log(title = "清组数据", businessType = BusinessType.UPDATE)
    @PostMapping("/delPack")
    public AjaxResult delPack(@RequestBody @Validated(value = BatterySetVO.cmd78.class) BatterySetVO batterySetVO) {
        restoreService.delPack(batterySetVO);
        return success();
    }

    @Log(title = "清主机数据", businessType = BusinessType.UPDATE)
    @PostMapping("/delHost")
    public AjaxResult delHost(@RequestBody @Validated(value = BatterySetVO.cmd.class) BatterySetVO batterySetVO) {
        batterySetVO.setNeedDynResult(false);
        return controlBatterySet.doSet(batterySetVO, BatteryCidEnum._79);
    }

    @Log(title = "设备复位", businessType = BusinessType.UPDATE)
    @PostMapping("/reset")
    public AjaxResult reset(@RequestBody @Validated(value = BatterySetVO.cmd.class) BatterySetVO batterySetVO) {
        batterySetVO.setNeedDynResult(false);
        batterySetVO.setParamNum("71");
        batterySetVO.setParamValue("08");
        return controlBatterySet.doSet(batterySetVO, BatteryCidEnum._05);
    }

    @Log(title = "出厂设置", businessType = BusinessType.UPDATE)
    @PostMapping("/restore")
    public AjaxResult restore(@RequestBody @Validated(value = BatterySetVO.cmd.class) BatterySetVO batterySetVO) {
        restoreService.restore(batterySetVO);
        return success();
    }

    @Log(title = "时间同步", businessType = BusinessType.UPDATE)
    @PostMapping("/syncTime")
    public AjaxResult syncTime(@RequestBody @Validated(value = BatterySetVO.cmd37.class) BatterySetVO batterySetVO) {
        SystemService.syncServerTime(batterySetVO.getDatetime());
        return success();
    }

    @Log(title = "蜂鸣器", businessType = BusinessType.UPDATE)
    @PostMapping("/buzzer")
    public AjaxResult buzzerStatus(@RequestBody @Validated(value = BatterySetVO.cmd.class) BatterySetVO batterySetVO) {
        batterySetVO.setNeedDynResult(false);
        batterySetVO.setParamNum("70");
        batterySetVO.setParamValue(batterySetVO.getBuzzerStatus() == 1 ? "01" : "00");
        AjaxResult result = controlBatterySet.doSet(batterySetVO, BatteryCidEnum._05);


        Map<String, Object> mapAll = configService.getExtend(batterySetVO.getConfigId());
        mapAll = mapAll == null ? new HashMap<>() : mapAll;

        // 均衡设置成功，更新
        if (Objects.equals(result.get(AjaxResult.CODE_TAG), AjaxResult.Type.SUCCESS.value())) {
            mapAll.put("buzzerStatus", batterySetVO.getBuzzerStatus());
            configService.updateExtend(batterySetVO.getConfigId(), mapAll);
        }

        return result;
    }

    // 电池数据校正
    @Log(title = "电池数据校正", businessType = BusinessType.UPDATE)
    @PostMapping("/correct")
    public AjaxResult correct(@RequestBody @Validated(value = BatterySetVO.cmd76.class) BatterySetVO batterySetVO) {
        batterySetVO.setNeedDynResult(false);
        Config config = new Config();
        config.setType(DeviceTypeEnum._1.getDictValue());
        List<Config> configs = configService.selectConfigList(config);
        if (configs.isEmpty()) {
            return error("请先添加设备");
        }
        batterySetVO.setConfigId(configs.get(0).getConfigId());
        return controlBatterySet.doSet(batterySetVO, BatteryCidEnum._76);
    }
}
