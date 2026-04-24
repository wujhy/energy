package com.shanhe.project.device.opt.service;

import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.shanhe.common.exception.ServiceException;
import com.shanhe.common.utils.CacheUtils;
import com.shanhe.common.utils.DateUtils;
import com.shanhe.framework.comm.CommServer;
import com.shanhe.framework.enums.*;
import com.shanhe.framework.web.domain.AjaxResult;
import com.shanhe.project.device.alarm.domain.AlarmLog;
import com.shanhe.project.device.alarm.service.IAlarmLogService;
import com.shanhe.project.device.config.domain.*;
import com.shanhe.project.device.config.service.BatteryReportLogService;
import com.shanhe.project.device.config.service.IDevBatteryOptService;
import com.shanhe.project.device.opt.cmd.CmdBatteryControlService;
import com.shanhe.project.device.opt.domain.OptLog;
import com.shanhe.project.device.opt.vo.BatterySetVO;
import com.shanhe.project.iot.model.BatteryModeInfo;
import com.shanhe.project.sync.domain.AlarmItemLevelVo;
import com.shanhe.project.sync.service.ClientReportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import oshi.util.Util;

import javax.annotation.Resource;
import java.util.Date;
import java.util.Objects;

/**
 * 设备控制类
 */
@Service
public class ControlBattery extends ControlBase {

    protected static Logger logger = LoggerFactory.getLogger(ControlBattery.class);

    @Resource
    private CmdBatteryControlService cmdBatteryControlService;
    @Resource
    private OptLogService optLogService;
    @Resource
    private IDevBatteryOptService devBatteryOptService;
    @Resource
    private ClientReportService clientReportService;
    @Resource
    private BatteryReportLogService batteryReportLogService;
    @Resource
    private ControlBatterySet controlBatterySet;
    @Resource
    private IAlarmLogService alarmLogService;

    /** 缓存结果 **/
    CacheKeyEnum cacheKeyEnum = CacheKeyEnum.RESULT;

    /**
     * 蓄电池推送测试指令到终端设备
     */
    public AjaxResult toSendCmdToOat(DevBatteryOpt opt) {
        // 校验设备
        Config config = this.getConfig(opt);

        // 保存操作参数
        devBatteryOptService.insertDevBatteryOpt(opt);

        // 是否上报
        if (!opt.getIsSync()) {
            clientReportService.uploadBatteryOpt(opt);
        }

        // 时间格式化
        opt.setReplaceTime(DateUtils.parseDateToStr(DateUtils.YYYYMMDDHHMMSS, opt.getTestTime()));
        // 命令内容、动态指令号
        String dynCid, cmdStr;
        BatteryTestEnum testEnum = BatteryTestEnum.find(opt.getTestType());
        switch (testEnum) {
            case _1:  //内阻测试配置
                cmdStr = cmdBatteryControlService.getCmd32(config, opt);
                dynCid = BatteryCidEnum._E2.getDictValue();
                break;
            case _2:   //连接条电阻测试配置
                cmdStr = cmdBatteryControlService.getCmd33(config, opt);
                dynCid = BatteryCidEnum._E3.getDictValue();
                break;
            case _3://核容测试配置
                cmdStr = cmdBatteryControlService.getCmd34(config, opt);
                dynCid = BatteryCidEnum._E4.getDictValue();
                break;
            case _4:  //浮充管理配置
                cmdStr = cmdBatteryControlService.getCmd31(config, opt);
                dynCid = BatteryCidEnum._E1.getDictValue();
                break;
            case _5:  //备电时长测试配置
                cmdStr = cmdBatteryControlService.getCmd35(config, opt);
                dynCid = BatteryCidEnum._E5.getDictValue();
                break;
            default:
                return AjaxResult.error("下发蓄电池测试指令类型失败", 0);
        }
        if (StrUtil.isBlank(cmdStr)) {
            return AjaxResult.error("下发蓄电池测试指令失败，指令生成失败", 0);
        }

        // 是否重复请求
        String resultKey = super.setControlStatus(config, opt.getPackNum(), dynCid, cacheKeyEnum);
        // 下发指令
        CommServer.returnCmd(cmdStr);

        // 结果监控
        return super.getControlResult(resultKey, cacheKeyEnum);
    }

    /**
     * 立即执行蓄电池操作
     */
    public AjaxResult toSendBatteryCmdToOat(DevBatteryOpt opt) {
        // 校验设备
        Config config = this.getConfig(opt);
        BatteryTestEnum testEnum = BatteryTestEnum.find(opt.getTestType());

        BatteryReportLog batteryReportLog = batteryReportLogService.lastCache(config.getConfigId(), opt.getPackNum());
        if (null == batteryReportLog) {
            return AjaxResult.error("暂无上报数据", 0);
        }
        if (null == batteryReportLog.getPackParam()) {
            return AjaxResult.error("暂无上报数据", 0);
        }
        AlarmLog alarmLog = alarmLogService.getByCache(opt.getConfigId(),opt.getPackNum(),null, ItemCode.TXZT.getCode());
        if (null != alarmLog) {
            if (ObjUtil.equals(YesNoEnum.NO.getDictValue(), alarmLog.getStatus())) {
                return AjaxResult.error(alarmLog.getDataInfo(), 0);
            }
        }

        //连接条测试
        if (BatteryTestEnum._2.getDictValue().equals(testEnum.getDictValue())) {

            Double current = MapUtil.getDouble(batteryReportLog.getPackParam(), "packCurrent");
            //电池组充放电电流
            if (current != null && Math.abs(current) < 5) {
                throw new RuntimeException("电池组未到达测试条件，需组电流超过±5A才可以进行连接条测试！");
            }
        } else {

            // 电池状态0：监控1：充电2：停电3：核容4：未连接5：备电6：空闲
            String batteryPackStatus = (String) batteryReportLog.getPackParam().get("batteryPackStatus");
            if (!StrUtil.equals("6", batteryPackStatus)) {
                return AjaxResult.error("电池组处于非空闲状态，不允许测试！", 0);
            }

        }

        // 默认需要等待执行结果，不需要记录操作日志
        boolean needWait = true, needLog = false;
        // 命令内容、动态指令号
        String cmdStr, dynCid;
        switch (testEnum) {
            case _1:  //立即执行内阻测试

                BatterySetVO batterySetVO = new BatterySetVO();
                batterySetVO.setConfigId(opt.getConfigId());
                batterySetVO.setPackNum(opt.getPackNum());
                BatteryModeInfo modelResult = controlBatterySet.getModelResult(batterySetVO);
                if (modelResult != null) {
                    if (modelResult.getMode() == 0 && modelResult.getStatus() == 0) {
                    } else {
                        // 当前模式 0： 无测试 1： 自动编号 6： 内阻测试 10：连接条电阻测试 */
                        String mode = modelResult.getMode() == 1 ? "自动编号" : modelResult.getMode() == 6 ? "内阻测试" : modelResult.getMode() == 10 ? "连接条电阻测试" : "无";
                        return AjaxResult.error("正在进行" + mode + "，请勿进行其他操作");
                    }
                }

                OptLog optLog = optLogService.lastType(config.getConfigId(), opt.getPackNum(), BatteryTestEnum._1.getDictValue());
                // 5 分钟内不允许测试
                if (null != optLog) {
                    if (null == optLog.getUpdateTime()) {
                        return AjaxResult.error("正在内阻测试中", 0);
                    }
                    if (System.currentTimeMillis() - optLog.getUpdateTime().getTime() < 5 * 60 * 1000) {
                        return AjaxResult.error("5分钟内不允许重复测试内阻", 0);
                    }
                }
                cmdStr = cmdBatteryControlService.genCmd05(config, "79", String.valueOf(opt.getPackNum()));
                dynCid = BatteryCidEnum._85.getDictValue();
                needWait = false;
                break;
            case _2:   //立即执行连接条电阻测试
                cmdStr = cmdBatteryControlService.genCmd0F(config, opt);
                dynCid = BatteryCidEnum._8F.getDictValue();
                needLog = true;
                break;
            case _3:  //立即执行核容测试
                cmdStr = cmdBatteryControlService.genCmd30(config, opt.getPackNum(), "2", opt.getDischargeTime(), opt.getEndVoltage());
                dynCid = BatteryCidEnum._E0.getDictValue();
                break;
            case _5:  //立即执行备电时长测试
                cmdStr = cmdBatteryControlService.genCmd30(config, opt.getPackNum(), "1", opt.getDischargeTime(), opt.getEndVoltage());
                dynCid = BatteryCidEnum._E0.getDictValue();
                break;
            case _6:  //单节内阻测试
                cmdStr = cmdBatteryControlService.getCmd36(config, opt);
                dynCid = BatteryCidEnum._E6.getDictValue();
                break;
            default:
                return AjaxResult.error("下发蓄电池测试指令类型失败", 0);
        }
        if (StrUtil.isBlank(cmdStr)) {
            return AjaxResult.error("下发蓄电池测试指令失败，指令生成失败", 0);
        }

        // 是否重复请求
        String resultKey = super.setControlStatus(config, opt.getPackNum(), dynCid, cacheKeyEnum);
        // 记录操作日志
        Long optLogId = null;
        if (needLog) {
            optLogId = optLogService.insert(config.getConfigId(), opt.getPackNum(), opt.getTestType(), null);
        }

        // 下发指令
        CommServer.returnCmd(cmdStr);

        AjaxResult ajaxResult = AjaxResult.success();
        //延迟等待设备响应
        if (needWait) {
            ajaxResult = super.getControlResult(resultKey, cacheKeyEnum);
        }
        // 更新日志结果
        if (needLog && optLogId != null) {
            optLogService.update(optLogId, Objects.equals(ajaxResult.get(AjaxResult.CODE_TAG), AjaxResult.Type.SUCCESS.value()) ? 0 : 1, null);
        }
        return ajaxResult;
    }

    /**
     * 立即执行停止备电操作
     */
    public AjaxResult toSendStopBatteryCmdToOat(DevBatteryOpt opt) {
        // 校验设备
        Config config = this.getConfig(opt);

        // 停止内阻测试
        if (Objects.equals(opt.getTestType(), BatteryTestEnum._1.getDictValue())) {

            BatteryReportLog batteryReportLog = batteryReportLogService.lastCache(config.getConfigId(), opt.getPackNum());

            // 无数据上报结束
            if (null == batteryReportLog || null == batteryReportLog.getPackParam()) {
                optLogService.doStopTest(opt.getConfigId(), opt.getPackNum(), BatteryTestEnum._1.getDictValue());
                return AjaxResult.success();
            }

            // 当前不在内阻测试状态
            String resistanceTestStatus = (String) batteryReportLog.getPackParam().get("resistanceTestStatus");
            if (!StrUtil.equals("6", resistanceTestStatus)) {
                optLogService.doStopTest(opt.getConfigId(), opt.getPackNum(), BatteryTestEnum._1.getDictValue());
                return AjaxResult.success();
            }

            // 上报时间超过 3 分钟
            int diff = DateUtils.differentMillsByMillisecond(batteryReportLog.getCreateTime(), new Date());
            if (diff > 3) {
                optLogService.doStopTest(opt.getConfigId(), opt.getPackNum(), BatteryTestEnum._1.getDictValue());
                return AjaxResult.success();
            }
            return AjaxResult.success();
        }

        String cmdStr = cmdBatteryControlService.genCmd30(config, opt.getPackNum(), "4", 0, 0D);
        if (StrUtil.isBlank(cmdStr)) {
            return AjaxResult.error("下发蓄电池停止备电失败，指令生成失败", 0);
        }

        // 下发指令
        CommServer.returnCmd(cmdStr);
        return AjaxResult.success();
    }

    /**
     * 校验设备信息
     */
    private Config getConfig(DevBatteryOpt devBatteryOpt) {
        // 设备
        Config config = configService.selectConfigByConfigId(devBatteryOpt.getConfigId());
        if (config == null) {
            throw new ServiceException("设备不存在，操作执行失败！");
        }
        if (!Objects.equals(config.getType(), DeviceTypeEnum._1.getDictValue())) {
            throw new ServiceException("非蓄电池设备，操作执行失败！");
        }

        // 蓄电池组
        BatteryPack batteryPack = config.getPackList().stream()
                .filter(p -> Objects.equals(p.getPackNum(), devBatteryOpt.getPackNum())).findFirst().orElse(null);
        if (batteryPack == null) {
            throw new ServiceException("电池组不存在，操作执行失败！");
        }
        if (Objects.equals(devBatteryOpt.getTestType(), BatteryTestEnum._5.getDictValue())
                && Objects.equals(batteryPack.getIsAllowPower(), YesNoEnum.NO.getDictValue())) {
            throw new ServiceException("该电池组不允许测试！");
        }
        return config;
    }

    /**
     * 修改设备时间
     */
    public void doSynBatteryDate(Config config) {
        String cmdStr = cmdBatteryControlService.genCmd37(config);
        if (cmdStr == null) {
            logger.error("37修改设备时间，生成指令失败！");
            return;
        }
        // 下发指令
        CommServer.returnCmd(cmdStr);
    }

    /**
     * 同步参数信息
     *
     * @param config 设备
     * @param batteryPackNumber 电池组
     * @param needWait 是否等待结果
     */
    public void doSynBatteryAlarm(Config config, Integer batteryPackNumber, boolean needWait) {
        // 判断是否存在记录
        String cacheKey = String.format(CacheKeyEnum.RESULT_SYN_ALARM.getKey(), config.getConfigId(), batteryPackNumber);
        Object hasCache = CacheUtils.get(CacheKeyEnum.RESULT_SYN_ALARM.getCache(), cacheKey);
        if (hasCache != null) {
            throw new ServiceException("正在同步参数中，请稍后再试！");
        }
        // 生成指令
        String cmdStr = cmdBatteryControlService.genCmd39(config, batteryPackNumber);
        if (StrUtil.isBlank(cmdStr)) {
            logger.error("39获取屏蔽报警参数，生成指令失败！");
            return;
        }
        // 缓存
        CacheUtils.put(CacheKeyEnum.RESULT_SYN_ALARM.getCache(), cacheKey, 1);
        // 下发指令
        CommServer.returnCmd(cmdStr);

//        int[] alarmLevel = {1, 2, 3};  //参数报警等级
        int[] alarmLevel = {1};  //参数报警等级，后面两个等级设备没开放
        for (int level : alarmLevel) {
            cmdStr = cmdBatteryControlService.genCmd0B(config, batteryPackNumber, level);
            if (StrUtil.isBlank(cmdStr)) {
                logger.error("0B读取电池组报警参数，生成指令失败！");
                continue;
            }
            // 下发指令
            CommServer.returnCmd(cmdStr);
        }

        if (needWait) {
            //延迟等待设备响应
            while (true) {
                hasCache = CacheUtils.get(CacheKeyEnum.RESULT_SYN_ALARM.getCache(), cacheKey);
                if (hasCache == null) {
                    break;
                }
                Util.sleep(500L);
            }
        }
    }

    /**
     * 读取电池组信息
     *
     * @param config 设备
     */
    public void doUploadBattery(Config config) {
        String cmdStr = cmdBatteryControlService.genCmd06(config);
        if (StrUtil.isBlank(cmdStr)) {
            logger.error("06屏蔽电池组报警参数，生成指令失败！");
            return;
        }
        // 下发指令
        CommServer.returnCmd(cmdStr);
    }

    /**
     * 读取设备版本
     *
     * @param config 设备配置
     */
    public void doUploadSoftNum(Config config) {
        String cmdStr = cmdBatteryControlService.genCmd0E(config);
        if (StrUtil.isBlank(cmdStr)) {
            logger.error("0E读取设备版本，生成指令失败！");
            return;
        }
        // 下发指令
        CommServer.returnCmd(cmdStr);
    }

    /**
     * 下发指令，修改参数内容
     *
     * @param configAttribute 设备属性
     */
    public void doUpdateParameter(ConfigAttribute configAttribute) {
        Config config = configService.selectConfigByConfigId(configAttribute.getConfigId());
        // 设备启用、在线、且为蓄电池
        if (config == null
                || !Objects.equals(config.getType(), DeviceTypeEnum._1.getDictValue())) {
            return;
        }

        // 为指定属性
        String paramNumber = ItemCode.getParamsNumber(configAttribute.getCode());
        if (paramNumber == null || paramNumber.isEmpty()) {
            logger.info("{}【{}】参数编号不存在，无需下发指令！", configAttribute.getName(), configAttribute.getCode());
            return;
        }

        // 若属性未开启、告警未配置，则下发告警屏蔽
        if (Objects.equals(configAttribute.getStatus(), YesNoEnum.NO.getDictValue())
                || Objects.equals(configAttribute.getAlarmConfig(), YesNoEnum.NO.getDictValue())) {
            logger.info("{}【{}】参数编号{}存在，下发屏蔽告警指令！", configAttribute.getName(), configAttribute.getCode(), paramNumber);
            this.updateParameterByAlarm(config, configAttribute, paramNumber, true);
            return;
        }

        // 告警解除屏蔽
        logger.info("{}【{}】参数编号{}存在，下发解除屏蔽告警指令！", configAttribute.getName(), configAttribute.getCode(), paramNumber);
        this.updateParameterByAlarm(config, configAttribute, paramNumber, false);

        // 告警配置设置
        if (configAttribute.getListLevel() == null || configAttribute.getListLevel().isEmpty()) {
            logger.info("{}【{}】告警等级配置不存在，无需下发告警参数指令！", configAttribute.getName(), configAttribute.getCode());
            return;
        }

        // 告警参数设置（首条记录 --> 设备三个告警等级）
        AlarmItemLevelVo levelVo = configAttribute.getListLevel().get(0);
        for (int i = 1; i <= 3; i++) {
            this.updateParameterByLevel(config, configAttribute.getPackNum(), paramNumber, String.valueOf(i),
                    levelVo.getHightValue(), levelVo.getLowValue(), levelVo.getRecValue());
        }
//        for (AlarmItemLevelVo levelVo : configAttribute.getListLevel()) {
//            this.updateParameterByLevel(config, configAttribute.getPackNum(), paramNumber, levelVo.getLevelCode(),
//                    levelVo.getHightValue(), levelVo.getLowValue(), levelVo.getRecValue());
//        }
    }

    /**
     * 下发指令，是否开启告警配置
     *
     * @param config 设备
     * @param configAttribute 参数项
     */
    public void updateParameterByAlarm(Config config, ConfigAttribute configAttribute, String paramNumber, Boolean isShield) {
        //指令组装
        String cmdStr = cmdBatteryControlService.genCmd0A(config, configAttribute.getPackNum(), 1, isShield ? 1 : 0, paramNumber);
        if (StrUtil.isBlank(cmdStr)) {
            logger.error("0A屏蔽电池组报警参数，生成指令失败！");
            return;
        }
        // 下发指令
        CommServer.returnCmd(cmdStr);
    }

    /**
     *
     * 更新属性告警参数等级
     *
     * @param config 设备
     * @param packNum 电池组编号
     * @param paramNumber 参数编号
     * @param level 告警等级
     * @param minValue 最小值
     * @param maxValue 最大值
     * @param recValue 恢复值
    、     */
    private void updateParameterByLevel(Config config, Integer packNum, String paramNumber, String level, Double minValue, Double maxValue, Double recValue) {
        // 告警等级不是1、2、3，则不处理
//        if (!StrUtil.equals(level, AlarmLevelEnum._1.getDictValue())
//                || !StrUtil.equals(level, AlarmLevelEnum._2.getDictValue())
//                || !StrUtil.equals(level, AlarmLevelEnum._3.getDictValue())) {
//            return;
//        }

        // 告警值
        Double value = minValue != null ? minValue : maxValue;
        if (value == null) {
            logger.info("参数编号{}，告警等级{}，告警值不存在，无需下发配置！", paramNumber, level);
            return;
        }

        //指令组装
        String cmdStr = cmdBatteryControlService.genCmd03(config, packNum, level, paramNumber, value);
        if (StrUtil.isBlank(cmdStr)) {
            logger.info("03设置电池组报警参数，生成指令失败！");
            return;
        }
        // 下发指令
        CommServer.returnCmd(cmdStr);

        // 恢复值
        if (recValue != null) {
            String recParamNum = String.format("%02d", Integer.parseInt(paramNumber) + 1);
            cmdStr = cmdBatteryControlService.genCmd03(config, packNum, level, recParamNum, recValue);
            // 下发指令
            if (StrUtil.isNotBlank(cmdStr)) {
                CommServer.returnCmd(cmdStr);
            }
        }

        // 基准值（内阻系数过大过小设置基准值）
//        if((StrUtil.equals(ItemCode.DTNZGD.getParamsNumber(), paramNumber)
//                || StrUtil.equals(ItemCode.DTNZGX.getParamsNumber(), paramNumber)) && standValue != null) {
//            cmdStr = cmdBatteryControlService.genCmd03(config, packNum, level, "53", standValue);
//            if (StrUtil.isNotBlank(cmdStr)) {
//                CommServer.sendCmd(cmdStr);
//            }
//        }
    }
}
