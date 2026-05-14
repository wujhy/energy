package com.shanhe.project.device.opt.service;

import com.shanhe.common.constant.Constants;
import com.shanhe.framework.comm.CommServer;
import com.shanhe.framework.enums.BatteryCidEnum;
import com.shanhe.framework.enums.CacheKeyEnum;
import com.shanhe.framework.enums.ItemCode;
import com.shanhe.framework.enums.TcpCharEnum;
import com.shanhe.framework.enums.TcpCidEnum;
import com.shanhe.framework.comm.tcp.utils.CodingUtil;
import com.shanhe.framework.consts.SysConst;
import com.shanhe.framework.web.domain.AjaxResult;
import com.shanhe.project.collector.battery.model.BatteryCollectorCommandResult;
import com.shanhe.project.collector.battery.service.BatteryCollectorCommandService;
import com.shanhe.project.collector.battery.service.BatteryModeStatusService;
import com.shanhe.project.device.config.domain.BatteryPack;
import com.shanhe.project.device.config.domain.Config;
import com.shanhe.project.device.config.domain.ConfigAttribute;
import com.shanhe.project.device.config.service.BatteryReportLogService;
import com.shanhe.project.device.config.service.IBatteryPackService;
import com.shanhe.project.device.config.service.IConfigAttributeService;
import com.shanhe.project.device.host.domain.Host;
import com.shanhe.project.device.host.service.IHostService;
import com.shanhe.project.device.opt.cmd.DeviceModel;
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
    /** 缓存结果 **/
    CacheKeyEnum cacheKeyEnum = CacheKeyEnum.RESULT;
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
            applyDefaultConfigId(batterySetVO);
            String channelName = resolveChannelName(batterySetVO);
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
            applyDefaultConfigId(batterySetVO);
            BatteryModeInfo modelResult = getModelResult(batterySetVO);
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
            String channelName = resolveChannelName(batterySetVO);
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
        applyDefaultConfigId(batterySetVO);
        return AjaxResult.success(getModelResult(batterySetVO));
    }

    public AjaxResult clearModelNum(BatterySetVO batterySetVO) {
        applyDefaultConfigId(batterySetVO);
        return clearModelNum(batterySetVO.getPackNum());
    }

    public AjaxResult resistance(BatterySetVO batterySetVO) {
        try {
            applyDefaultConfigId(batterySetVO);
            int resistance = toResistanceCoefficient(batterySetVO.getResistance());
            String channelName = resolveChannelName(batterySetVO);
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
        applyDefaultConfigId(batterySetVO);
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
        applyDefaultConfigId(batterySetVO);
        return AjaxResult.success(batteryReportLogService.resistanceValue(batterySetVO.getPackNum()));
    }

    public AjaxResult resistanceValueSet(BatterySetVO batterySetVO) {
        try {
            applyDefaultConfigId(batterySetVO);
            updateResistanceStandValue(batterySetVO.getPackNum(), ItemCode.DTNZGD, batterySetVO.getResistanceStandValue());
            updateResistanceStandValue(batterySetVO.getPackNum(), ItemCode.DTNZGX, batterySetVO.getResistanceStandValue());
            configAttributeService.updateCache(1);
            return AjaxResult.success();
        } catch (IllegalArgumentException e) {
            return AjaxResult.error(e.getMessage());
        }
    }

    public AjaxResult delGb(BatterySetVO batterySetVO) {
        applyDefaultConfigId(batterySetVO);
        return sendM460Command(batterySetVO, BatteryCidEnum._20);
    }

    public AjaxResult getBalanced(BatterySetVO batterySetVO) {
        applyDefaultConfigId(batterySetVO);
        Map<String, Object> mapAll = configService.getExtend();
        Map<String, Object> map = new HashMap<>();
        map.put("autoBalanced", mapAll != null && mapAll.get("autoBalanced") != null ? mapAll.get("autoBalanced") : 0);
        map.put("manualBalanced", mapAll != null && mapAll.get("manualBalanced") != null ? mapAll.get("manualBalanced") : 0);
        map.put("buzzerStatus", mapAll != null && mapAll.get("buzzerStatus") != null ? mapAll.get("buzzerStatus") : 0);
        return AjaxResult.success(map);
    }

    public AjaxResult balanced(BatterySetVO batterySetVO) {
        try {
            applyDefaultConfigId(batterySetVO);
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
            applyDefaultConfigId(batterySetVO);
            restoreService.delPack(batterySetVO);
            String channelName = resolveChannelName(batterySetVO);
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
        applyDefaultConfigId(batterySetVO);
        batterySetVO.setNeedDynResult(false);
        return sendM460Command(batterySetVO, BatteryCidEnum._79);
    }

    public AjaxResult reset(BatterySetVO batterySetVO) {
        applyDefaultConfigId(batterySetVO);
        batterySetVO.setNeedDynResult(false);
        batterySetVO.setParamNum("71");
        batterySetVO.setParamValue("08");
        return sendM460Command(batterySetVO, BatteryCidEnum._05);
    }

    public AjaxResult restore(BatterySetVO batterySetVO) {
        applyDefaultConfigId(batterySetVO);
        restoreService.restore(batterySetVO);
        return AjaxResult.success();
    }

    public AjaxResult syncTime(BatterySetVO batterySetVO) {
        SystemService.syncServerTime(batterySetVO.getDatetime());
        return AjaxResult.success();
    }

    public AjaxResult buzzerStatus(BatterySetVO batterySetVO) {
        try {
            applyDefaultConfigId(batterySetVO);
            saveBuzzerStatus(batterySetVO.getBuzzerStatus());
            return AjaxResult.success();
        } catch (IllegalArgumentException e) {
            return AjaxResult.error(e.getMessage());
        }
    }

    public AjaxResult correct(BatterySetVO batterySetVO) {
        try {
            applyDefaultConfigId(batterySetVO);
            batterySetVO.setNeedDynResult(false);
            String channelName = resolveChannelName(batterySetVO);
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

    /**
     * 设置电池组告警参数
     */
    public AjaxResult doSet(BatterySetVO batterySetVO, BatteryCidEnum cidEnum) {
        return sendM460Command(batterySetVO, cidEnum);
    }

    /**
     * 发送旧 M460 聚合协议指令。后续 /battery/set 接口按能力逐项替换为本地能力或 600 模块端控制。
     */
    private AjaxResult sendM460Command(BatterySetVO batterySetVO, BatteryCidEnum cidEnum) {
        applyDefaultConfigId(batterySetVO);
        // 主机信息
        Host host = super.getHost();
        // 设备信息
        Config config = super.getConfig();
        // 协议内容
        StringBuilder info = new StringBuilder();
        // 指令头、默认地址、指令编码
        info.append(TcpCharEnum.HEAD_53.getDictValue());
        info.append("01").append(cidEnum.getDictValue());
        // 响应动态指令
        String dynCid;
        switch (cidEnum) {
            case _05:
                batterySetVO.setNeedDynResult(false);
                // 设置系统状态响应
                dynCid = BatteryCidEnum._85.getDictValue();
                // 长度
                info.append("02");
                // 参数号、参数值
                info.append(batterySetVO.getParamNum());
                info.append(batterySetVO.getParamValue());
                break;
            case _20:
                // 清鼓包数据
                dynCid = BatteryCidEnum._2A.getDictValue();
                // 长度
                info.append("01");
                // 包序号
                info.append(CodingUtil.integerToHexString(batterySetVO.getPackNum(), 2));
                break;
            case _75:
                // 恢复出厂设置
                dynCid = BatteryCidEnum._F5.getDictValue();
                // 长度
                info.append("00");
                break;
            case _79:
                // 清主机数据
                dynCid = BatteryCidEnum._F9.getDictValue();
                // 长度
                info.append("01").append("01");
                break;
            default:
                return AjaxResult.error("指令异常！");
        }
        // 校验和
        info.append(CodingUtil.energyCheckSum(info.substring(TcpCharEnum.HEAD_53.getDictValue().length())));
        // 指令尾
        info.append(TcpCharEnum.END_0D.getDictValue());

        // 日志
        logger.debug("下发指令：{}", info);

        // 是否重复请求
        String resultKey = "";
        if (batterySetVO.getNeedDynResult()) {
            resultKey = super.setControlStatus(config, batterySetVO.getPackNum(), dynCid, cacheKeyEnum);
        }

        // 下发指令
        CommServer.returnCmd(DeviceModel.getCmd(host, config, info.toString(), TcpCidEnum._54.getDictValue(), dynCid));

        if (batterySetVO.getNeedDynResult()) {
            return super.getControlResult(resultKey, cacheKeyEnum);
        } else {
            return AjaxResult.success();
        }
    }

    /**
     * 监听控制指令执行结果
     */
    public BatteryModeInfo getModelResult(BatterySetVO batterySetVO) {
        return batteryModeStatusService.get(batterySetVO.getPackNum());
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
        Map<String, Object> mapAll = configService.getExtend();
        mapAll = mapAll == null ? new HashMap<>() : mapAll;
        mapAll.put("autoBalanced", autoBalanced == null ? 0 : autoBalanced);
        mapAll.put("manualBalanced", manualBalanced == null ? 0 : manualBalanced);
        configService.updateExtend(mapAll);
    }

    public void saveBuzzerStatus(Integer buzzerStatus) {
        Map<String, Object> mapAll = configService.getExtend();
        mapAll = mapAll == null ? new HashMap<>() : mapAll;
        mapAll.put("buzzerStatus", buzzerStatus == null ? 0 : buzzerStatus);
        configService.updateExtend(mapAll);
    }

    private void applyDefaultConfigId(BatterySetVO batterySetVO) {
        if (batterySetVO != null) {
            batterySetVO.setConfigId(Constants.DEFAULT_CONFIG_ID);
        }
    }

    private String resolveChannelName(BatterySetVO batterySetVO) {
        String channelName = batteryCollectorCommandService.resolveChannelName(null, batterySetVO.getPackNum());
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
