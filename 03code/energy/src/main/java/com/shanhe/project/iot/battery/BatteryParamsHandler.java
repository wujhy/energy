package com.shanhe.project.iot.battery;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSONObject;
import com.shanhe.common.utils.CacheUtils;
import com.shanhe.framework.enums.CacheKeyEnum;
import com.shanhe.framework.enums.ItemCode;
import com.shanhe.framework.enums.YesNoEnum;
import com.shanhe.framework.comm.tcp.model.BatteryParamsInfo;
import com.shanhe.framework.comm.tcp.model.DeviceData;
import com.shanhe.framework.comm.tcp.utils.CodingUtil;
import com.shanhe.project.device.config.domain.Config;
import com.shanhe.project.device.config.domain.ConfigAttribute;
import com.shanhe.project.device.config.service.IConfigAttributeService;
import com.shanhe.project.sync.domain.AlarmItemLevelVo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 上传电池组报警参数
 */
@Service
public class BatteryParamsHandler {
    protected static Logger logger = LoggerFactory.getLogger(BatteryParamsHandler.class);

    @Resource
    private IConfigAttributeService configAttributeService;

    /**
     * 上传电池组报警参数
     *
     * @param config 设备
     * @param deviceData 指令
     */
    public void uploadBatteryParamsData(Config config, DeviceData deviceData) {
        BatteryParamsInfo paramsInfo = toDecodeData(deviceData.getInfo());
        if(paramsInfo==null){
            logger.error("上传电池组报警参数响应结果出错！info={}", deviceData.getInfo());
            return;
        }
        //解析参数
        this.synBatteryParamsData(config, paramsInfo);
    }

    /**
     * 解析数据
     *
     * @param dataInfo 指令
     */
    private BatteryParamsInfo toDecodeData(String dataInfo){
        String info = dataInfo.substring(16, dataInfo.length() - 4);
        String binary8B = CodingUtil.hexString2binaryString(info.substring(0, 2));
        //应答结果
        String res = String.valueOf(CodingUtil.binaryToDecimal(binary8B.substring(0, 4)));
        if(StrUtil.equals(res, "1")) {
            return null;
        }
        BatteryParamsInfo batteryParamsInfo = new BatteryParamsInfo();

        //电池组编号
        batteryParamsInfo.setBatteryPackNumber(CodingUtil.binaryToDecimal(binary8B.substring(4, 8)));
        //参数报警等级
        batteryParamsInfo.setAlarmLevel(String.valueOf(Integer.parseInt(info.substring(2, 4), 16)));

        /*电池组参数值*/
        //单体电压过充告警值
        String batteryOverchargeAlarm = CodingUtil.decimal(Integer.parseInt(CodingUtil.hexStringToString(info.substring(4, 8))), "#.000", 1000);
        batteryParamsInfo.setBatteryOverchargeAlarm(batteryOverchargeAlarm);

        //单体电压过充告警恢复值
        String batteryOverchargeRestore = CodingUtil.decimal(Integer.parseInt(CodingUtil.hexStringToString(info.substring(8, 12))), "#.000", 1000);
        batteryParamsInfo.setBatteryOverchargeRestore(batteryOverchargeRestore);

        //单体电压过放告警值
        String batteryOverdischargeAlarm = CodingUtil.decimal(Integer.parseInt(CodingUtil.hexStringToString(info.substring(12, 16))), "#.000", 1000);
        batteryParamsInfo.setBatteryOverdischargeAlarm(batteryOverdischargeAlarm);

        //单体电压过放告警恢复值
        String batteryOverdischargeRestore = CodingUtil.decimal(Integer.parseInt(CodingUtil.hexStringToString(info.substring(16, 20))), "#.000", 1000);
        batteryParamsInfo.setBatteryOverdischargeRestore(batteryOverdischargeRestore);

        //单体浮充电压过高告警值
        String batteryFloatChargeOvertopAlarm = CodingUtil.decimal(Integer.parseInt(CodingUtil.hexStringToString(info.substring(20, 24))), "#.000", 1000);
        batteryParamsInfo.setBatteryFloatChargeOvertopAlarm(batteryFloatChargeOvertopAlarm);

        //单体浮充电压过高告警恢复值
        String batteryFloatChargeOvertopRestore = CodingUtil.decimal(Integer.parseInt(CodingUtil.hexStringToString(info.substring(24, 28))), "#.000", 1000);
        batteryParamsInfo.setBatteryFloatChargeOvertopRestore(batteryFloatChargeOvertopRestore);

        //单体浮充电压过低告警值
        String batteryFloatChargeTooLowAlarm = CodingUtil.decimal(Integer.parseInt(CodingUtil.hexStringToString(info.substring(28, 32))), "#.000", 1000);
        batteryParamsInfo.setBatteryFloatChargeTooLowAlarm(batteryFloatChargeTooLowAlarm);

        //单体浮充电压过低告警恢复值
        String batteryFloatChargeTooLowRestore = CodingUtil.decimal(Integer.parseInt(CodingUtil.hexStringToString(info.substring(32, 36))), "#.000", 1000);
        batteryParamsInfo.setBatteryFloatChargeTooLowRestore(batteryFloatChargeTooLowRestore);

        //单体电压不均告警值
        String batteryVoltageUnevennessAlarm = CodingUtil.decimal(Integer.parseInt(CodingUtil.hexStringToString(info.substring(36, 40))), "#.000", 1000);
        batteryParamsInfo.setBatteryVoltageUnevennessAlarm(batteryVoltageUnevennessAlarm);

        //单体电压不均告警恢复值
        String batteryVoltageUnevennessRestore = CodingUtil.decimal(Integer.parseInt(CodingUtil.hexStringToString(info.substring(40, 44))), "#.000", 1000);
        batteryParamsInfo.setBatteryVoltageUnevennessRestore(batteryVoltageUnevennessRestore);

        //单体电压极差值告警值
        String batteryVoltageRangeAlarm = CodingUtil.decimal(Integer.parseInt(CodingUtil.hexStringToString(info.substring(44, 48))), "#.000", 1000);
        batteryParamsInfo.setBatteryVoltageRangeAlarm(batteryVoltageRangeAlarm);

        //单体电压极差值告警恢复值
        String batteryVoltageRangeRestore = CodingUtil.decimal(Integer.parseInt(CodingUtil.hexStringToString(info.substring(48, 52))), "#.000", 1000);
        batteryParamsInfo.setBatteryVoltageRangeRestore(batteryVoltageRangeRestore);

        //总体电压过充告警值
        String batteryPackOverchargeAlarm = CodingUtil.decimal(Integer.parseInt(CodingUtil.hexStringToString(info.substring(52, 56))), "#.0", 10);
        batteryParamsInfo.setBatteryPackOverchargeAlarm(batteryPackOverchargeAlarm);

        //总体电压过充告警恢复值
        String batteryPackOverchargeRestore = CodingUtil.decimal(Integer.parseInt(CodingUtil.hexStringToString(info.substring(56, 60))), "#.0", 10);
        batteryParamsInfo.setBatteryPackOverchargeRestore(batteryPackOverchargeRestore);

        //总体电压过放告警值
        String batteryPackOverdischargeAlarm = CodingUtil.decimal(Integer.parseInt(CodingUtil.hexStringToString(info.substring(60, 64))), "#.0", 10);
        batteryParamsInfo.setBatteryPackOverdischargeAlarm(batteryPackOverdischargeAlarm);

        //总体电压过放告警恢复值
        String batteryPackOverdischargeRestore = CodingUtil.decimal(Integer.parseInt(CodingUtil.hexStringToString(info.substring(64, 68))), "#.0", 10);
        batteryParamsInfo.setBatteryPackOverdischargeRestore(batteryPackOverdischargeRestore);

        //总体浮充电压过高保护警值
        String batteryPackFloatChargeOvertopAlarm = CodingUtil.decimal(Integer.parseInt(CodingUtil.hexStringToString(info.substring(68, 72))), "#.0", 10);
        batteryParamsInfo.setBatteryPackFloatChargeOvertopAlarm(batteryPackFloatChargeOvertopAlarm);

        //总体浮充电压过高保护警恢复值
        String batteryPackFloatChargeOvertopRestore = CodingUtil.decimal(Integer.parseInt(CodingUtil.hexStringToString(info.substring(72, 76))), "#.0", 10);
        batteryParamsInfo.setBatteryPackFloatChargeOvertopRestore(batteryPackFloatChargeOvertopRestore);

        //总体浮充电压过低告警值
        String batteryPackFloatChargeTooLowAlarm = CodingUtil.decimal(Integer.parseInt(CodingUtil.hexStringToString(info.substring(76, 80))), "#.0", 10);
        batteryParamsInfo.setBatteryPackFloatChargeTooLowAlarm(batteryPackFloatChargeTooLowAlarm);

        //总体浮充电压过低告警恢复值
        String batteryPackFloatChargeTooLowRestore = CodingUtil.decimal(Integer.parseInt(CodingUtil.hexStringToString(info.substring(80, 84))), "#.0", 10);
        batteryParamsInfo.setBatteryPackFloatChargeTooLowRestore(batteryPackFloatChargeTooLowRestore);

        //充过流告警值
        String chargeOvercurrentAlarm = CodingUtil.decimal(Integer.parseInt(CodingUtil.hexStringToString(info.substring(84, 88))), "#.0", 10);
        batteryParamsInfo.setChargeOvercurrentAlarm(chargeOvercurrentAlarm);

        //充过流告警恢复值
        String chargeOvercurrentRestore = CodingUtil.decimal(Integer.parseInt(CodingUtil.hexStringToString(info.substring(88, 92))), "#.0", 10);
        batteryParamsInfo.setChargeOvercurrentRestore(chargeOvercurrentRestore);

        //放过流告警值
        String dischargeOvercurrentAlarm = CodingUtil.decimal(Integer.parseInt(CodingUtil.hexStringToString(info.substring(92, 96))), "#.0", 10);
        batteryParamsInfo.setDischargeOvercurrentAlarm(dischargeOvercurrentAlarm);

        //放过流告警恢复值
        String dischargeOvercurrentRestore = CodingUtil.decimal(Integer.parseInt(CodingUtil.hexStringToString(info.substring(96, 100))), "#.0", 10);
        batteryParamsInfo.setDischargeOvercurrentRestore(dischargeOvercurrentRestore);

        //环境高温告警值
        String environmentHighTemperatureAlarm = CodingUtil.decimal(Integer.parseInt(CodingUtil.hexStringToString(info.substring(100, 104))), "#.0", 10);
        batteryParamsInfo.setEnvironmentHighTemperatureAlarm(environmentHighTemperatureAlarm);

        //环境高温告警恢复值
        String environmentHighTemperatureRestore = CodingUtil.decimal(Integer.parseInt(CodingUtil.hexStringToString(info.substring(104, 108))), "#.0", 10);
        batteryParamsInfo.setEnvironmentHighTemperatureRestore(environmentHighTemperatureRestore);

        //环境低温告警值
        String environmentLowTemperatureAlarm = CodingUtil.decimal(Integer.parseInt(CodingUtil.hexStringToString(info.substring(108, 112))), "#.0", 10);
        batteryParamsInfo.setEnvironmentLowTemperatureAlarm(environmentLowTemperatureAlarm);

        //环境低温告警恢复值
        String environmentLowTemperatureRestore = CodingUtil.decimal(Integer.parseInt(CodingUtil.hexStringToString(info.substring(112, 116))), "#.0", 10);
        batteryParamsInfo.setEnvironmentLowTemperatureRestore(environmentLowTemperatureRestore);

        //电池高温告警值
        String batteryHighTemperatureAlarm = CodingUtil.decimal(Integer.parseInt(CodingUtil.hexStringToString(info.substring(116, 120))), "#.0", 10);
        batteryParamsInfo.setBatteryHighTemperatureAlarm(batteryHighTemperatureAlarm);

        //电池高温告警恢复值
        String batteryHighTemperatureRestore = CodingUtil.decimal(Integer.parseInt(CodingUtil.hexStringToString(info.substring(120, 124))), "#.0", 10);
        batteryParamsInfo.setBatteryHighTemperatureRestore(batteryHighTemperatureRestore);

        //电池低温告警值
        String batteryLowTemperatureAlarm = CodingUtil.decimal(Integer.parseInt(CodingUtil.hexStringToString(info.substring(124, 128))), "#.0", 10);
        batteryParamsInfo.setBatteryLowTemperatureAlarm(batteryLowTemperatureAlarm);

        //电池低温告警恢复值
        String batteryLowTemperatureRestore = CodingUtil.decimal(Integer.parseInt(CodingUtil.hexStringToString(info.substring(128, 132))), "#.0", 10);
        batteryParamsInfo.setBatteryLowTemperatureRestore(batteryLowTemperatureRestore);

        //电池温度不均告警值
        String batteryTemperatureUnevennessAlarm = CodingUtil.decimal(Integer.parseInt(CodingUtil.hexStringToString(info.substring(132, 136))), "#.0", 10);
        batteryParamsInfo.setBatteryTemperatureUnevennessAlarm(batteryTemperatureUnevennessAlarm);

        //电池温度不均告警恢复值
        String batteryTemperatureUnevennessRestore = CodingUtil.decimal(Integer.parseInt(CodingUtil.hexStringToString(info.substring(136, 140))), "#.0", 10);
        batteryParamsInfo.setBatteryTemperatureUnevennessRestore(batteryTemperatureUnevennessRestore);

        //内阻过大告警系数
        String resistanceTooBigAlarm = CodingUtil.decimal(Integer.parseInt(CodingUtil.hexStringToString(info.substring(140, 144))), "#.00", 100);
        batteryParamsInfo.setResistanceTooBigAlarm(resistanceTooBigAlarm);

        //内阻过大告警系数恢复值
        String resistanceTooBigRestore = CodingUtil.decimal(Integer.parseInt(CodingUtil.hexStringToString(info.substring(144, 148))), "#.00", 100);
        batteryParamsInfo.setResistanceTooBigRestore(resistanceTooBigRestore);

        //内阻不均告警系数
        String resistanceUnevennessAlarm = CodingUtil.decimal(Integer.parseInt(CodingUtil.hexStringToString(info.substring(148, 152))), "#.00", 100);
        batteryParamsInfo.setResistanceUnevennessAlarm(resistanceUnevennessAlarm);

        //内阻不均告警恢复系数
        String resistanceUnevennessRestore = CodingUtil.decimal(Integer.parseInt(CodingUtil.hexStringToString(info.substring(152, 156))), "#.00", 100);
        batteryParamsInfo.setResistanceUnevennessRestore(resistanceUnevennessRestore);

        //内阻过小告警系数
        String resistanceTooSmallAlarm = CodingUtil.decimal(Integer.parseInt(CodingUtil.hexStringToString(info.substring(156, 160))), "#.00", 100);
        batteryParamsInfo.setResistanceTooSmallAlarm(resistanceTooSmallAlarm);

        //内阻过小告警恢复系数
        String resistanceTooSmallRestore = CodingUtil.decimal(Integer.parseInt(CodingUtil.hexStringToString(info.substring(160, 164))), "#.00", 100);
        batteryParamsInfo.setResistanceTooSmallRestore(resistanceTooSmallRestore);

        //连接条，氢气比例上限告警值
        String hydrogenPercentageUpperLimitAlarm = CodingUtil.decimal(Integer.parseInt(CodingUtil.hexStringToString(info.substring(164, 168))), "#", 1);
        batteryParamsInfo.setHydrogenPercentageUpperLimitAlarm(hydrogenPercentageUpperLimitAlarm);

        //连接条，氢气比例上限告警恢复值
        String hydrogenPercentageUpperLimitRestore = CodingUtil.decimal(Integer.parseInt(CodingUtil.hexStringToString(info.substring(168, 172))), "#", 1);
        batteryParamsInfo.setHydrogenPercentageUpperLimitRestore(hydrogenPercentageUpperLimitRestore);

        //SOC低告警值
        String socLowAlarm = CodingUtil.decimal(Integer.parseInt(CodingUtil.hexStringToString(info.substring(172, 176))), "#.0", 10);
        batteryParamsInfo.setSocLowAlarm(socLowAlarm);

        //SOC低告警恢复值
        String socLowRestore = CodingUtil.decimal(Integer.parseInt(CodingUtil.hexStringToString(info.substring(176, 180))), "#.0", 10);
        batteryParamsInfo.setSocLowRestore(socLowRestore);

        //SOH低告警值
        String sohLowAlarm = CodingUtil.decimal(Integer.parseInt(CodingUtil.hexStringToString(info.substring(180, 184))), "#.0", 10);
        batteryParamsInfo.setSohLowAlarm(sohLowAlarm);

        //SOH低告警恢复值
        String sohLowRestore = CodingUtil.decimal(Integer.parseInt(CodingUtil.hexStringToString(info.substring(184, 188))), "#.0", 10);
        batteryParamsInfo.setSohLowRestore(sohLowRestore);

        //内阻基准值
        String resistanceReferenceValue = String.valueOf(Integer.parseInt(CodingUtil.hexStringToString(info.substring(188,192))));
        batteryParamsInfo.setResistanceReferenceValue(resistanceReferenceValue);

        return batteryParamsInfo;
    }

    /**
     * 解析电池组报警参数
     *
     * @param config 设备信息
     * @param paramsInfo 电池告警参数
     */
    private void synBatteryParamsData(Config config, BatteryParamsInfo paramsInfo) {
        if (StrUtil.equals(paramsInfo.getChargeOvercurrentAlarm(), "6553.5")
                || StrUtil.equals(paramsInfo.getEnvironmentHighTemperatureAlarm(), "6553.5") ) {
            logger.error("{}：{} 组告警参数异常 => {}", config.getName(), paramsInfo.getBatteryPackNumber(), JSONObject.toJSONString(paramsInfo));
        }
        // 查设备包下所有属性列表
        ConfigAttribute query = new ConfigAttribute();
        query.setConfigId(config.getConfigId());
        query.setPackNum(paramsInfo.getBatteryPackNumber());
        List<ConfigAttribute> attributeList = configAttributeService.selectConfigAttributeList(query);
        if (attributeList == null || attributeList.isEmpty()) {
            return;
        }

        for (ConfigAttribute attribute : attributeList) {
            this.doUpdateData(attribute, paramsInfo);
        }


        // 更新属性缓存
        configAttributeService.updateCache(config.getConfigId(), YesNoEnum.YES.getDictValue());
    }

    /**
     * 解析数据
     *
     * @param attribute 属性
     * @param paramsInfo 告警参数
     */
    private void doUpdateData(ConfigAttribute attribute, BatteryParamsInfo paramsInfo) {

        ItemCode itemCode = ItemCode.fromCode(attribute.getCode());
        if (itemCode == null) {
            return;
        }

        String hightValue = null;
        String lowValue = null;
        String recValue = null;
        //恢复值比对方式1大于2等于3小于
        Integer recOpt = null;
        Double standValue = null;
        switch (itemCode) {
            case ZDYGC: //组电压过充告警
                hightValue = paramsInfo.getBatteryPackOverchargeAlarm();
                recValue = paramsInfo.getBatteryPackOverchargeRestore();
                break;
            case ZDYGF: //组电压过放告警
                lowValue = paramsInfo.getBatteryPackOverdischargeAlarm();
                recValue = paramsInfo.getBatteryPackOverdischargeRestore();
                break;
            case ZWDG: //环境高温告警
                hightValue = paramsInfo.getEnvironmentHighTemperatureAlarm();
                recValue = paramsInfo.getEnvironmentHighTemperatureRestore();
                break;
            case ZWDD: //环境低温告警
                lowValue = paramsInfo.getEnvironmentLowTemperatureAlarm();
                recValue = paramsInfo.getEnvironmentLowTemperatureRestore();
                break;
            case ZFCDYGG: //总体浮充电压过高告警
                hightValue = paramsInfo.getBatteryPackFloatChargeOvertopAlarm();
                recValue = paramsInfo.getBatteryPackFloatChargeOvertopRestore();
                break;
            case ZFCDYGD: //总体浮充电压过低告警
                lowValue = paramsInfo.getBatteryPackFloatChargeTooLowAlarm();
                recValue = paramsInfo.getBatteryPackFloatChargeTooLowRestore();
                break;
            case ZCGDLGJ: //电流过充告警
                hightValue = paramsInfo.getChargeOvercurrentAlarm();
                recValue = paramsInfo.getChargeOvercurrentRestore();
                break;
            case ZSOCDGJ: //SOC低告警
                lowValue = paramsInfo.getSocLowAlarm();
                recValue = paramsInfo.getSocLowRestore();
                break;
//            case ZSOHDGJ: //SOH低告警
            case DTGB: //鼓包
                hightValue = paramsInfo.getSohLowAlarm();
                recValue = paramsInfo.getSohLowRestore();
                break;
            case DTDYGC: //单体电压过充告警
                hightValue = paramsInfo.getBatteryOverchargeAlarm();
                recValue = paramsInfo.getBatteryOverchargeRestore();
                break;
            case DTDYGF: //单体电压过放告警
                lowValue = paramsInfo.getBatteryOverdischargeAlarm();
                recValue = paramsInfo.getBatteryOverdischargeRestore();
                break;
            case DTNZGD: //内阻过大告警系数
                hightValue =paramsInfo.getResistanceTooBigAlarm();
                recValue = paramsInfo.getResistanceTooBigRestore();
                if (StrUtil.isNotBlank(paramsInfo.getResistanceReferenceValue())) {
                    standValue = Double.parseDouble(paramsInfo.getResistanceReferenceValue());
                }
                break;
            case DTNZGX: //内阻过小告警系数
                lowValue = paramsInfo.getResistanceTooSmallAlarm();
                recValue = paramsInfo.getResistanceTooSmallRestore();
                if (StrUtil.isNotBlank(paramsInfo.getResistanceReferenceValue())) {
                    standValue = Double.parseDouble(paramsInfo.getResistanceReferenceValue());
                }
                break;
            case DTNZBJ: //内阻不均告警系数
                hightValue = paramsInfo.getResistanceUnevennessAlarm();
                recValue = paramsInfo.getResistanceUnevennessRestore();
                break;
            case DTDCWDG: //电池高温告警
                hightValue = paramsInfo.getBatteryHighTemperatureAlarm();
                recValue = paramsInfo.getBatteryHighTemperatureRestore();
                break;
            case DTDCWDD: //电池低温告警
                lowValue = paramsInfo.getBatteryLowTemperatureAlarm();
                recValue = paramsInfo.getBatteryLowTemperatureRestore();
                break;
            case DTFCDYG: //单体浮充电压过高告警
                hightValue = paramsInfo.getBatteryFloatChargeOvertopAlarm();
                recValue = paramsInfo.getBatteryFloatChargeOvertopRestore();
                break;
            case DTFCDYD: //单体浮充电压过低告警
                lowValue = paramsInfo.getBatteryFloatChargeTooLowAlarm();
                recValue = paramsInfo.getBatteryFloatChargeTooLowRestore();
                break;
            case DTDYBJ: //单体电压不均告警
                hightValue = paramsInfo.getBatteryVoltageUnevennessAlarm();
                recValue = paramsInfo.getBatteryVoltageUnevennessRestore();
                break;
            case DTDYJC: //单体电压极差告警
                hightValue = paramsInfo.getBatteryVoltageRangeAlarm();
                recValue = paramsInfo.getBatteryVoltageRangeRestore();
                break;
            case DTDCWDBJ: //电池温度不均告警
                hightValue = paramsInfo.getBatteryTemperatureUnevennessAlarm();
                recValue = paramsInfo.getBatteryTemperatureUnevennessRestore();
                break;
            case DTLJTGJ: //连接条
                hightValue = paramsInfo.getHydrogenPercentageUpperLimitAlarm();
                recValue = paramsInfo.getHydrogenPercentageUpperLimitRestore();
                break;
            default:
                break;
        }

        // 无匹配值，不处理
        if (hightValue == null && lowValue == null) {
            return;
        }

        Double min = null;
        if (StrUtil.isNotEmpty(hightValue)) {
            min = Double.parseDouble(hightValue);
            if (StrUtil.isNotBlank(recValue)) {
                recOpt = 2;
            }
        }
        Double max = null;
        if (StrUtil.isNotEmpty(lowValue)) {
            max = Double.parseDouble(lowValue);
            if (StrUtil.isNotBlank(recValue)) {
                recOpt = 1;
            }
        }

        // 告警等级列表
        List<AlarmItemLevelVo> levelList = attribute.getListLevel() == null ? new ArrayList<>() : attribute.getListLevel();
        AlarmItemLevelVo levelVo;
        if (!levelList.isEmpty()) {
            levelVo = levelList.get(0);
            // 匹配告警等级，更新
            levelVo.setHightValue(min);
            levelVo.setLowValue(max);
            levelVo.setStandValue(standValue);
            if (StrUtil.isNotBlank(recValue)) {
                levelVo.setRecFlag(recOpt);
                levelVo.setRecValue(Double.parseDouble(recValue));
            }
            levelList.set(0, levelVo);
        } else {
            // 无告警配置，新增
            levelVo = new AlarmItemLevelVo();
            levelVo.setLevelCode(paramsInfo.getAlarmLevel());
            levelVo.setHightValue(min);
            levelVo.setLowValue(max);
            levelVo.setStandValue(standValue);
            if (StrUtil.isNotBlank(recValue)) {
                levelVo.setRecFlag(recOpt);
                levelVo.setRecValue(Double.parseDouble(recValue));
            }
            levelList.add(levelVo);
        }
        attribute.setListLevel(levelList);

//        // 告警等级
//        if (Objects.equals(paramsInfo.getAlarmLevel(), AlarmLevelEnum._1.getDictValue())) {
//            attribute.setGeneralMin(min);
//            attribute.setGeneralMax(max);
//            if (StrUtil.isNotBlank(recValue)) {
//                attribute.setGeneralRecFlag(recOpt);
//                attribute.setGeneralRecValue(Double.parseDouble(recValue));
//            }
//        } else if (Objects.equals(paramsInfo.getAlarmLevel(), AlarmLevelEnum._2.getDictValue())) {
//            attribute.setImportantMin(min);
//            attribute.setImportantMax(max);
//            if (StrUtil.isNotBlank(recValue)) {
//                attribute.setImportantRecFlag(recOpt);
//                attribute.setImportantRecValue(Double.parseDouble(recValue));
//            }
//        } else if (Objects.equals(paramsInfo.getAlarmLevel(), AlarmLevelEnum._3.getDictValue())) {
//            attribute.setEmergencyMin(min);
//            attribute.setEmergencyMax(max);
//            if (StrUtil.isNotBlank(recValue)) {
//                attribute.setEmergencyRecFlag(recOpt);
//                attribute.setEmergencyRecValue(Double.parseDouble(recValue));
//            }
//        }

        // 更新属性
        configAttributeService.updateConfigAttributeAlarm(attribute);
    }

    /**
     * 统一截取蓄电池info
     *
     * @param deviceData 设备信息
     */
    private void responseResult(DeviceData deviceData) {
        // 蓄电池消息内容
        String info = this.getInfo(deviceData);
        // 响应
        String binary = CodingUtil.hexString2binaryString(info.substring(0, 2));
        // 结果
        Integer result = CodingUtil.binaryToDecimal(binary.substring(0, 4));
        // 电池组
        String packNum = String.valueOf(CodingUtil.binaryToDecimal(binary.substring(4, 8)));

        // 缓存响应结果
        String key = String.format(CacheKeyEnum.RESULT_PACK_NUM.getKey(), deviceData.getC0(), deviceData.getC1(), deviceData.getC2(), deviceData.getC3(), packNum);
        if (result != 0) {
            logger.error("key：{}，返回的结果：{}", key, result);
        }
        CacheUtils.put(CacheKeyEnum.RESULT_PACK_NUM.getCache(), key, Objects.equals(result, 0) ? 0 : 1);
    }

    /**
     * 83设置电池组报警参数响应
     *
     * @param deviceData 响应指令
     */
    public void signParamsResponse(DeviceData deviceData) {
        this.responseResult(deviceData);
    }

    /**
     * 8A 当个设置屏蔽报警应答
     *
     * @param deviceData 响应指令
     */
    public void shieldAlarmReply(DeviceData deviceData) {
        this.responseResult(deviceData);
    }

    /**
     * 截取蓄电池响应内容
     *
     * @param deviceData 设备消息
     * @return 响应内容
     */
    private String getInfo(DeviceData deviceData) {
        return deviceData.getInfo().substring(16, deviceData.getInfo().length() - 4);
    }

    /**
     *  E9 屏蔽报警应答,获取屏蔽报警参数
     *
     * @author wjh
     * @since 2025/4/15
     */
    public void alarmMsgSave(Config config, DeviceData deviceData) {
        String info = this.getInfo(deviceData);
        String binary9A = CodingUtil.hexString2binaryString(info.substring(0, 2));
        //应答结果
        String res = String.valueOf(CodingUtil.binaryToDecimal(binary9A.substring(0, 4)));
        if(StrUtil.equals(res, "1")) {
            logger.error("上传电池组报警参数响应结果出错！devId:{}", deviceData.getImei());
            return ;
        }

        // 电池组编码
        Integer batteryPackNumber = CodingUtil.binaryToDecimal(binary9A.substring(4, 8));

        // 查电池组所有属性
        ConfigAttribute query = new ConfigAttribute();
        query.setConfigId(config.getConfigId());
        query.setPackNum(batteryPackNumber);
        List<ConfigAttribute> attributeList = configAttributeService.selectConfigAttributeList(query);
        if (attributeList == null || attributeList.isEmpty()) {
            return;
        }

        //1111111111111111001111000011000011001100000011000000111100110000
        String paramFirStr = info.substring(2, 18);
        StringBuilder b1 = new StringBuilder();
        b1.append(new StringBuffer(CodingUtil.hexString2binaryString(paramFirStr.substring(0, 2))).reverse());
        b1.append(new StringBuffer(CodingUtil.hexString2binaryString(paramFirStr.substring(2, 4))).reverse());
        b1.append(new StringBuffer(CodingUtil.hexString2binaryString(paramFirStr.substring(4, 6))).reverse());
        b1.append(new StringBuffer(CodingUtil.hexString2binaryString(paramFirStr.substring(6, 8))).reverse());
        b1.append(new StringBuffer(CodingUtil.hexString2binaryString(paramFirStr.substring(8, 10))).reverse());
        b1.append(new StringBuffer(CodingUtil.hexString2binaryString(paramFirStr.substring(10, 12))).reverse());
        b1.append(new StringBuffer(CodingUtil.hexString2binaryString(paramFirStr.substring(12, 14))).reverse());
        b1.append(new StringBuffer(CodingUtil.hexString2binaryString(paramFirStr.substring(14, 16))).reverse());

        /*
         String paramSedStr = info.substring(18, 34);
        StringBuffer b2 = new StringBuffer();
        b2.append(new StringBuffer(CodingUtil.hexString2binaryString(paramSedStr.substring(0,2))).reverse());
        b2.append(new StringBuffer(CodingUtil.hexString2binaryString(paramSedStr.substring(2,4))).reverse());
        b2.append(new StringBuffer(CodingUtil.hexString2binaryString(paramSedStr.substring(4,6))).reverse());
        b2.append(new StringBuffer(CodingUtil.hexString2binaryString(paramSedStr.substring(6,8))).reverse());
        b2.append(new StringBuffer(CodingUtil.hexString2binaryString(paramSedStr.substring(8,10))).reverse());
        b2.append(new StringBuffer(CodingUtil.hexString2binaryString(paramSedStr.substring(10,12))).reverse());
        b2.append(new StringBuffer(CodingUtil.hexString2binaryString(paramSedStr.substring(12,14))).reverse());
        b2.append(new StringBuffer(CodingUtil.hexString2binaryString(paramSedStr.substring(14,16))).reverse());

         String paramThirdStr = info.substring(34, 50);
        StringBuffer b3 = new StringBuffer();
        b3.append(new StringBuffer(CodingUtil.hexString2binaryString(paramThirdStr.substring(0,2))).reverse());
        b3.append(new StringBuffer(CodingUtil.hexString2binaryString(paramThirdStr.substring(2,4))).reverse());
        b3.append(new StringBuffer(CodingUtil.hexString2binaryString(paramThirdStr.substring(4,6))).reverse());
        b3.append(new StringBuffer(CodingUtil.hexString2binaryString(paramThirdStr.substring(6,8))).reverse());
        b3.append(new StringBuffer(CodingUtil.hexString2binaryString(paramThirdStr.substring(8,10))).reverse());
        b3.append(new StringBuffer(CodingUtil.hexString2binaryString(paramThirdStr.substring(10,12))).reverse());
        b3.append(new StringBuffer(CodingUtil.hexString2binaryString(paramThirdStr.substring(12,14))).reverse());
        b3.append(new StringBuffer(CodingUtil.hexString2binaryString(paramThirdStr.substring(14,16))).reverse());
        */
        //处理一般告警参数，传递上来的数据就是一般告警数据
        for (ConfigAttribute attribute : attributeList) {
            this.doUpdateAlarmData(attribute, b1.toString());
        }
        // 更新属性缓存
        configAttributeService.updateCache(config.getConfigId(), YesNoEnum.YES.getDictValue());
    }

    /**
     * 更新屏蔽报警
     */
    private void doUpdateAlarmData(ConfigAttribute attribute, String msg) {
        if (null == msg) {
            return;
        }
        Integer sta = null;
        ItemCode itemCode = ItemCode.fromCode(attribute.getCode());
        if (null == itemCode) {
            return;
        }
        if (msg.length() < itemCode.getEnd()) {
            logger.error("Error processing alarm code {} : Message is null or too short", attribute.getCode());
            return;
        }
        try {
            sta = Integer.parseInt(msg.substring(itemCode.getStart(), itemCode.getEnd()));
        } catch (Exception e) {
            logger.error("Error processing alarm code {} , {}", attribute.getCode(), e.getMessage());
        }

        if (sta != null && Objects.equals(attribute.getStatus(), sta)) {
//            attribute.setAlarmConfig(sta > 0 ? 0 : 1);
            attribute.setStatus(sta > 0 ? 0 : 1);
            configAttributeService.updateConfigAttributeAlarm(attribute);
        }
    }
}
