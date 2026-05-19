package com.shanhe.project.device.opt.service;

import com.shanhe.framework.enums.ItemCode;
import com.shanhe.framework.consts.SysConst;
import com.shanhe.framework.web.domain.AjaxResult;
import com.shanhe.project.collector.battery.model.BatteryCollectorCommandResult;
import com.shanhe.project.collector.battery.service.BatteryCollectorCommandService;
import com.shanhe.project.collector.battery.service.BatteryModeStatusService;
import com.shanhe.project.device.config.domain.BatteryPack;
import com.shanhe.project.device.config.domain.ConfigAttribute;
import com.shanhe.project.device.config.service.BatteryReportLogService;
import com.shanhe.project.device.config.service.IBatteryPackService;
import com.shanhe.project.device.config.service.IConfigAttributeService;
import com.shanhe.project.device.host.domain.Host;
import com.shanhe.project.device.host.service.IHostService;
import com.shanhe.project.device.opt.vo.BatterySetVO;
import com.shanhe.project.iot.model.BatteryModeInfo;
import com.shanhe.project.monitor.server.service.SystemService;
import com.shanhe.project.sync.domain.AlarmItemLevelVo;
import org.springframework.context.annotation.Lazy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 开关量控制类
 *
 * @author wjh
 * @since 2025/7/10
 */
@Service
public class ControlBatterySet extends ControlBase {

    protected static Logger logger = LoggerFactory.getLogger(ControlBatterySet.class);
    @Resource
    private BatteryReportLogService batteryReportLogService;
    @Resource
    private IConfigAttributeService configAttributeService;
    @Resource
    @Lazy
    private RestoreService restoreService;
    @Resource
    private BatteryCollectorCommandService batteryCollectorCommandService;
    @Resource
    private BatteryModeStatusService batteryModeStatusService;
    @Resource
    private IHostService hostService;
    @Resource
    private IBatteryPackService batteryPackService;

    public AjaxResult manualModelNum(BatterySetVO batterySetVO) {
        try {
            String channelName = resolveChannelName(batterySetVO.getPackNum());
            BatteryCollectorCommandResult result = batteryCollectorCommandService.manualSetSubmoduleAddress(
                    channelName,
                    batterySetVO.getPackNum(),
                    batterySetVO.getModelNum(),
                    batterySetVO.getNewModelNum(),
                    null);
            return toCommandAjaxResult(result);
        } catch (IllegalArgumentException e) {
            return AjaxResult.error(e.getMessage());
        }
    }

    public AjaxResult autoModelNum(BatterySetVO batterySetVO) {
        try {
            BatteryModeInfo modelResult = getModelResult(batterySetVO.getPackNum());
            if (modelResult != null && (modelResult.getMode() != 0 || modelResult.getStatus() != 0)) {
                String mode = modelResult.getMode() == 1 ? "自动编号" : modelResult.getMode() == 6 ? "内阻测试" : modelResult.getMode() == 10 ? "连接条电阻测试" : "无";
                return AjaxResult.error("正在进行" + mode + "，请勿进行其他操作");
            }
            BatteryPack batteryPack = batteryPackService.selectBatteryInfoByPackNum(batterySetVO.getPackNum());
            if (batteryPack == null) {
                return AjaxResult.error("未找到电池组配置，自动编号失败！");
            }
            if (batteryPack.getBatSinSize() == null || batteryPack.getBatSinModel() == null) {
                return AjaxResult.error("电池组单体数量或规格未配置，自动编号失败！");
            }
            String channelName = resolveChannelName(batterySetVO.getPackNum());
            BatteryCollectorCommandResult result = batteryCollectorCommandService.autoSetSubmoduleAddress(
                    channelName,
                    batterySetVO.getPackNum(),
                    batteryPack.getBatSinSize(),
                    batteryPack.getBatSinModel(),
                    null);
            return toCommandAjaxResult(result);
        } catch (IllegalArgumentException e) {
            return AjaxResult.error(e.getMessage());
        }
    }

    public AjaxResult batteryVersion(BatterySetVO batterySetVO) {
        Host host = hostService.getDetail();
        String version = firstNonBlank(
                host == null ? null : host.getVersion(),
                host == null ? null : host.getSoftVersion(),
                SysConst.version == null ? null : "V" + SysConst.version);
        return AjaxResult.success(version == null ? "" : version);
    }

    public AjaxResult refreshModelNum(BatterySetVO batterySetVO) {
        return AjaxResult.success(getModelResult(batterySetVO.getPackNum()));
    }

    public AjaxResult clearModelNum(BatterySetVO batterySetVO) {
        return clearModelNum(batterySetVO.getPackNum());
    }

    public AjaxResult resistance(BatterySetVO batterySetVO) {
        try {
            int resistance = toResistanceCoefficient(batterySetVO.getResistance());
            String channelName = resolveChannelName(batterySetVO.getPackNum());
            int moduleAddress = batterySetVO.getModelNum() == null ? 0 : batterySetVO.getModelNum();
            BatteryCollectorCommandResult result = batteryCollectorCommandService.setInternalResistanceCoefficient(
                    channelName,
                    batterySetVO.getPackNum(),
                    moduleAddress,
                    resistance,
                    null);
            return toCommandAjaxResult(result);
        } catch (IllegalArgumentException e) {
            return AjaxResult.error(e.getMessage());
        }
    }

    public AjaxResult resistanceDefaultValue(BatterySetVO batterySetVO) {
        ConfigAttribute configAttribute = configAttributeService.getBy(batterySetVO.getPackNum(), ItemCode.DTNZGD.getCode());
        if (configAttribute == null) {
            configAttribute = configAttributeService.getBy(batterySetVO.getPackNum(), ItemCode.DTNZGX.getCode());
        }
        if (configAttribute == null || configAttribute.getListLevel() == null || configAttribute.getListLevel().isEmpty()) {
            return AjaxResult.success(0L);
        }
        return AjaxResult.success(configAttribute.getListLevel().get(0).getStandValue());
    }

    public AjaxResult resistanceValue(BatterySetVO batterySetVO) {
        return AjaxResult.success(batteryReportLogService.resistanceValue(batterySetVO.getPackNum()));
    }

    public AjaxResult resistanceValueSet(BatterySetVO batterySetVO) {
        try {
            updateResistanceStandValue(batterySetVO.getPackNum(), ItemCode.DTNZGD, batterySetVO.getResistanceStandValue());
            updateResistanceStandValue(batterySetVO.getPackNum(), ItemCode.DTNZGX, batterySetVO.getResistanceStandValue());
            configAttributeService.updateCache(1);
            return AjaxResult.success();
        } catch (IllegalArgumentException e) {
            return AjaxResult.error(e.getMessage());
        }
    }

    public AjaxResult delGb(BatterySetVO batterySetVO) {
        // M460 source command: 0x20/0x2A, swollen voltage reference for a battery group.
        // energy already collects gbvoltage/swollenVoltage from 600 module 01/81, but the
        // threshold source is not confirmed. Keep this as an internal extension point.
        return reservedM460Migration("delGb", "M460 0x20/0x2A swollen voltage reference");
    }

    public AjaxResult getBalanced(BatterySetVO batterySetVO) {
        Map<String, Object> mapAll = hostService.getExtend();
        Map<String, Object> map = new HashMap<>();
        map.put("autoBalanced", mapAll != null && mapAll.get("autoBalanced") != null ? mapAll.get("autoBalanced") : 0);
        map.put("manualBalanced", mapAll != null && mapAll.get("manualBalanced") != null ? mapAll.get("manualBalanced") : 0);
        map.put("buzzerStatus", mapAll != null && mapAll.get("buzzerStatus") != null ? mapAll.get("buzzerStatus") : 0);
        return AjaxResult.success(map);
    }

    public AjaxResult balanced(BatterySetVO batterySetVO) {
        try {
            saveBalancedStatus(
                    batterySetVO.getAutoBalanced(),
                    batterySetVO.getManualBalanced());
            return AjaxResult.success();
        } catch (IllegalArgumentException e) {
            return AjaxResult.error(e.getMessage());
        }
    }

    public AjaxResult delPack(BatterySetVO batterySetVO) {
        try {
            restoreService.delPack(batterySetVO);
            String channelName = resolveChannelName(batterySetVO.getPackNum());
            BatteryCollectorCommandResult result = batteryCollectorCommandService.clearBatteryGroupDebugData(
                    channelName,
                    batterySetVO.getPackNum(),
                    null);
            return toCommandAjaxResult(result);
        } catch (IllegalArgumentException e) {
            return AjaxResult.error(e.getMessage());
        }
    }

    public AjaxResult delHost(BatterySetVO batterySetVO) {
        // M460 source command: 0x79/0xF9, clear host data. In energy this maps to local
        // host reset and cache refresh instead of the old M460 aggregate protocol.
        hostService.restore();
        return AjaxResult.success();
    }

    public AjaxResult reset(BatterySetVO batterySetVO) {
        // M460 source command: 0x05 system state, param 0x71=0x08 means device reset.
        // energy has no confirmed safe whole-device reset implementation yet.
        return reservedM460Migration("reset", "M460 0x05 system reset 0x71/0x08");
    }

    public AjaxResult restore(BatterySetVO batterySetVO) {
        // M460 source command: 0x75/0xF5 restore factory defaults. RestoreServiceImpl
        // owns the energy-native local cleanup flow, so no M460 passthrough is sent here.
        restoreService.restore(batterySetVO);
        return AjaxResult.success();
    }

    public AjaxResult syncTime(BatterySetVO batterySetVO) {
        SystemService.syncServerTime(batterySetVO.getDatetime());
        return AjaxResult.success();
    }

    public AjaxResult buzzerStatus(BatterySetVO batterySetVO) {
        try {
            saveBuzzerStatus(batterySetVO.getBuzzerStatus());
            return AjaxResult.success();
        } catch (IllegalArgumentException e) {
            return AjaxResult.error(e.getMessage());
        }
    }

    public AjaxResult correct(BatterySetVO batterySetVO) {
        try {
            batterySetVO.setNeedDynResult(false);
            String channelName = resolveChannelName(batterySetVO.getPackNum());
            BatteryCollectorCommandResult result = batteryCollectorCommandService.setCalibrationParameter(
                    channelName,
                    batterySetVO.getPackNum(),
                    batterySetVO.getModelNum(),
                    batterySetVO.getDataType(),
                    batterySetVO.getDataStatus(),
                    batterySetVO.getDataInfo(),
                    null);
            return toCommandAjaxResult(result);
        } catch (IllegalArgumentException e) {
            return AjaxResult.error(e.getMessage());
        }
    }

    private AjaxResult reservedM460Migration(String action, String m460Command) {
        logger.info("battery set action reserved for energy migration, action={}, source={}", action, m460Command);
        return AjaxResult.success();
    }

    /**
     * 监听控制指令执行结果
     */
    public BatteryModeInfo getModelResult(Integer packNum) {
        return batteryModeStatusService.get(packNum);
    }

    /**
     * 清除编号数据
     */
    public AjaxResult clearModelNum(Integer packNum) {
        batteryModeStatusService.clear(packNum);
        return AjaxResult.success();
    }

    private void updateResistanceStandValue(Integer packNum, ItemCode itemCode, Integer resistanceStandValue) {
        ConfigAttribute attribute = configAttributeService.getBy(packNum, itemCode.getCode());
        if (attribute == null) {
            logger.info("电池组{}未配置{}，跳过内阻基准值更新", packNum, itemCode.getCode());
            return;
        }

        List<AlarmItemLevelVo> levelList = attribute.getListLevel() == null ? new ArrayList<>() : new ArrayList<>(attribute.getListLevel());
        AlarmItemLevelVo levelVo = levelList.isEmpty() ? new AlarmItemLevelVo() : levelList.get(0);
        levelVo.setStandValue(resistanceStandValue == null ? null : resistanceStandValue.doubleValue());
        if (levelList.isEmpty()) {
            levelList.add(levelVo);
        } else {
            levelList.set(0, levelVo);
        }

        attribute.setListLevel(levelList);
        configAttributeService.updateConfigAttributeAlarm(attribute);
    }

    public void saveBalancedStatus(Integer autoBalanced, Integer manualBalanced) {
        Map<String, Object> mapAll = hostService.getExtend();
        mapAll = mapAll == null ? new HashMap<>() : mapAll;
        mapAll.put("autoBalanced", autoBalanced == null ? 0 : autoBalanced);
        mapAll.put("manualBalanced", manualBalanced == null ? 0 : manualBalanced);
        hostService.updateExtend(mapAll);
    }

    public void saveBuzzerStatus(Integer buzzerStatus) {
        Map<String, Object> mapAll = hostService.getExtend();
        mapAll = mapAll == null ? new HashMap<>() : mapAll;
        mapAll.put("buzzerStatus", buzzerStatus == null ? 0 : buzzerStatus);
        hostService.updateExtend(mapAll);
    }

    private String resolveChannelName(Integer packNum) {
        String channelName = batteryCollectorCommandService.resolveChannelName(packNum);
        if (channelName == null) {
            throw new IllegalArgumentException("未找到对应的蓄电池采集通道，操作执行失败！");
        }
        return channelName;
    }

    private AjaxResult toCommandAjaxResult(BatteryCollectorCommandResult result) {
        if (result == null) {
            return AjaxResult.error("指令下发失败！");
        }
        if (!result.isSuccess()) {
            return AjaxResult.error(result.getMessage() == null ? "指令下发失败！" : result.getMessage());
        }
        return AjaxResult.success();
    }

    private int toResistanceCoefficient(Float resistanceValue) {
        if (resistanceValue == null) {
            throw new IllegalArgumentException("内阻系数不能为空！");
        }
        int resistance = (int) (resistanceValue * 1000);
        if (resistance < 0 || resistance > 65535) {
            throw new IllegalArgumentException("内阻系数过大！");
        }
        return resistance;
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) {
                return value;
            }
        }
        return null;
    }
}
