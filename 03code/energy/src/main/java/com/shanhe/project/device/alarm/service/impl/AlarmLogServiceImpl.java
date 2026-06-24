package com.shanhe.project.device.alarm.service.impl;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.*;

import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.excel.EasyExcel;
import com.google.common.collect.Lists;
import com.shanhe.common.constant.Constants;
import com.shanhe.common.exception.ServiceException;
import com.shanhe.common.utils.CacheUtils;
import com.shanhe.common.utils.DataUtils;
import com.shanhe.common.utils.DateUtils;
import com.shanhe.common.utils.file.FileUtils;
import com.shanhe.common.utils.spring.SpringUtils;
import com.shanhe.common.utils.text.Convert;
import com.shanhe.common.utils.uuid.IdUtils;
import com.shanhe.framework.enums.*;
import com.shanhe.project.device.alarm.domain.AlarmLogDTO;
import com.shanhe.project.device.alarm.service.AlarmLevelService;
import com.shanhe.project.device.config.domain.BatteryMonitor;
import com.shanhe.project.device.config.domain.BatteryReportLog;
import com.shanhe.project.device.config.domain.Config;
import com.shanhe.project.device.config.domain.ConfigAttribute;
import com.shanhe.project.device.config.service.IConfigAttributeService;
import com.shanhe.project.device.host.service.IHostService;
import com.shanhe.project.device.opt.service.ControlBatterySet;
import com.shanhe.project.device.opt.service.OptLogService;
import com.shanhe.project.collector.battery.model.BatteryDeviceState;
import com.shanhe.project.collector.battery.model.BatteryDeviceStateConstants;
import com.shanhe.project.collector.battery.service.BatteryDeviceStateService;
import com.shanhe.project.sync.domain.AlarmItemLevelVo;
import com.shanhe.project.sync.service.ClientReportService;
import org.springframework.stereotype.Service;
import com.shanhe.project.device.alarm.mapper.AlarmLogMapper;
import com.shanhe.project.device.alarm.domain.AlarmLog;
import com.shanhe.project.device.alarm.service.IAlarmLogService;

import javax.annotation.Resource;

/**
 * 设备历史记录Service业务层处理
 *
 * @author wjh
 * @since 2024-12-31
 */
@Service
public class AlarmLogServiceImpl implements IAlarmLogService {
    @Resource
    private AlarmLogMapper alarmLogMapper;

    @Resource
    private IHostService hostService;
    @Resource
    private IConfigAttributeService configAttributeService;
    @Resource
    private ClientReportService clientReportService;
    @Resource
    private OptLogService optLogService;
    @Resource
    private ControlBatterySet controlBatterySet;
    @Resource
    private BatteryDeviceStateService batteryDeviceStateService;

    // 缓存枚举
    CacheKeyEnum alarmCache = CacheKeyEnum.ALARM;

    /**
     * 从缓存获取告警记录
     *
     * @param packNum 电池组编号
     * @param modelNum 模块编号
     * @param itemCode 属性编码
     * @return 告警记录
     */
    @Override
    public AlarmLog getByCache(Integer packNum, Integer modelNum, String itemCode) {
        return (AlarmLog) CacheUtils.get(alarmCache.getCache(),
                String.format(alarmCache.getKey(), packNum, modelNum, itemCode));
    }

    /**
     * 查询设备历史记录
     *
     * @param alarmId 设备历史记录主键
     * @return 设备历史记录
     */
    @Override
    public AlarmLog selectAlarmLogByAlarmId(Long alarmId) {
        AlarmLog alarm = alarmLogMapper.selectAlarmLogByAlarmId(alarmId);
        // 告警类型是主机，补充
        this.setAlarmParam(alarm);
        return alarm;
    }

    /**
     * 判断是否存在未处理告警
     *
     * @return 是否存在告警，1-存在，0-不存在
     */
    @Override
    public Integer isAlarm() {
        return this.alarmNum() > 0 ? 1 : 0;
    }

    /**
     * 从缓存判断指定电池组是否存在未处理告警
     *
     * @param packNum 电池组编号，为null时查询所有
     * @return 是否存在告警
     */
    @Override
    public Integer isAlarmByCache(Integer packNum) {
        Set<String> keys = CacheUtils.getCacheKeys(alarmCache.getCache());
        Integer isAlarm = YesNoEnum.NO.getDictValue();
        String prefix = packNum != null ?
                String.format("alarm:%s:", packNum) :
                "alarm:";
        for (String key : keys) {
            if (!StrUtil.startWith(key, prefix)) {
                continue;
            }
            AlarmLog alarmLog = (AlarmLog) CacheUtils.get(alarmCache.getCache(), key);
            if (Objects.equals(alarmLog.getStatus(), YesNoEnum.NO.getDictValue())) {
                isAlarm = YesNoEnum.YES.getDictValue();
                break;
            }
        }
        if (Objects.equals(isAlarm, YesNoEnum.NO.getDictValue()) && hasStateCommunicationAlarm(packNum)) {
            isAlarm = YesNoEnum.YES.getDictValue();
        }
        return isAlarm;
    }

    /**
     * 获取蓄电池未处理告警数量
     *
     * @return 告警数量
     */
    @Override
    public Long batteryAlarmNum() {
        return selectBatteryAlarmLogListCache(null).stream()
                .filter(alarmLog -> alarmLog != null
                        && Objects.equals(alarmLog.getStatus(), YesNoEnum.NO.getDictValue()))
                .count();
    }

    /**
     * 获取当前未处理告警数量
     *
     * @return 告警数量
     */
    @Override
    public Long alarmNum() {
        Set<String> keys = CacheUtils.getCacheKeys(alarmCache.getCache());
        long num = 0L;
        // 前缀
        for (String key : keys) {
            AlarmLog alarmLog = (AlarmLog) CacheUtils.get(alarmCache.getCache(), key);
            if (Objects.equals(alarmLog.getStatus(), YesNoEnum.NO.getDictValue())) {
                num++;
            }
        }
        return num;
    }

    /**
     * 获取历史告警总数量
     *
     * @return 告警数量
     */
    @Override
    public Long alarmAllNum() {
        return alarmLogMapper.alarmAllNum();
    }

    /**
     * 获取告警设备数量
     *
     * @return 告警设备数量
     */
    @Override
    public Long alarmDeviceNum() {
        return alarmLogMapper.alarmDeviceNum();
    }

    /**
     * 查询设备历史记录列表
     *
     * @param alarmLog 设备历史记录
     * @return 设备历史记录
     */
    @Override
    public List<AlarmLog> selectAlarmLogList(AlarmLog alarmLog) {
        List<AlarmLog> list = alarmLogMapper.selectAlarmLogList(alarmLog);
        for (AlarmLog alarm : list) {
            // 告警类型是主机，补充
            this.setAlarmParam(alarm);
        }
        return list;
    }

    /**
     * 获取缓存中所有告警记录
     *
     * @return 告警记录列表
     */
    @Override
    public List<AlarmLog> cacheAlarmList() {
        List<AlarmLog> alarmLogList = new ArrayList<>();
        Set<String> keys = CacheUtils.getCacheKeys(alarmCache.getCache());
        Set<String> existingKeys = new HashSet<>();
        for (String key : keys) {
            AlarmLog alarmLog = (AlarmLog) CacheUtils.get(alarmCache.getCache(), key);
            alarmLogList.add(alarmLog);
            if (alarmLog != null && StrUtil.startWith(key, "alarm:")) {
                existingKeys.add(communicationAlarmKey(alarmLog));
            }
        }
        alarmLogList.addAll(buildStateCommunicationAlarms(null, existingKeys));
        return alarmLogList;
    }

    /**
     * 设置主机参数
     */
    private void setAlarmParam(AlarmLog alarm) {
        // 主机信息
        if (Objects.equals(alarm.getConfigId(), 1L)) {
            alarm.setConfigName(hostService.getDetail().getName());
            alarm.setType(DeviceTypeEnum._0.getDictValue());
        }
        // 未处理，持续时间
        if (Objects.equals(alarm.getStatus(), YesNoEnum.NO.getDictValue()) && Objects.equals(alarm.getDuration(), 0L)) {
            alarm.setUpdateTime(new Date());
            alarm.setDuration((alarm.getUpdateTime().getTime() - alarm.getCreateTime().getTime()) / 1000);
        }
    }

    /**
     * 蓄电池告警处理
     *
     * @param config 设备配置
     * @param packNum 电池组编号
     * @param modelNum 模块编号
     * @param warnParam 告警参数
     * @param batteryReportLog 上报日志
     */
    @Override
    public void alarmBattery(Config config, Integer packNum, Integer modelNum, Map<String, String> warnParam, BatteryReportLog batteryReportLog) {
        if (batteryReportLog == null) {
            return;
        }
        // 单体实时记录
        BatteryMonitor batteryMonitor = null;
        if (modelNum != null && batteryReportLog.getBatteryList() != null && !batteryReportLog.getBatteryList().isEmpty()) {
            // 循环匹配modelNum
            batteryMonitor = batteryReportLog.getBatteryList().stream().filter(monitor -> monitor.getBatNum().equals(modelNum)).findFirst().orElse(null);
        }
        // 循环告警项
        for (String itemCode : warnParam.keySet()) {
            // 告警值
            String alarmValue = warnParam.get(itemCode);

            // 取缓存告警记录
            String key = String.format(alarmCache.getKey(), packNum, modelNum, itemCode);
            AlarmLog cacheLog = (AlarmLog) CacheUtils.get(alarmCache.getCache(), key);

            /* -----------------------------------------屏蔽处理-------------------------------------------- */
            // 存在告警记录，屏蔽处理
            if (cacheLog != null) {
                // 告警屏蔽时间内，不处理
                if (null != cacheLog.getShiedTime()
                        && cacheLog.getShiedTime().getTime() > System.currentTimeMillis()) {
                    continue;
                }
                // 告警记录是已处理的（屏蔽时间已过），删除缓存
                if (Objects.equals(cacheLog.getStatus(), YesNoEnum.YES.getDictValue())) {
                    cacheLog = null;
                    CacheUtils.remove(alarmCache.getCache(), key);
                }
            }

            /* ----------------------------------------是否告警-------------------------------------------- */
            // 最新告警值为0，不告警处理，退出
            if (StrUtil.isBlank(alarmValue) || StrUtil.equals(alarmValue, "0")) {
                if (cacheLog != null) {
                    // 存在记录需完成
                    cacheLog.setStatus(YesNoEnum.YES.getDictValue());
                    this.updateStatus(cacheLog, key);
                }
                continue;
            }

            // 属性配置校验
            ConfigAttribute configAttribute = configAttributeService.getCacheBy(packNum, itemCode);
            if (configAttribute == null
                    || Objects.equals(configAttribute.getStatus(), YesNoEnum.NO.getDictValue())
                    || Objects.equals(configAttribute.getAlarmConfig(), YesNoEnum.NO.getDictValue())) {
                if (cacheLog != null) {
                    // 属性不存在、不启用、无需告警，直接完成告警
                    cacheLog.setStatus(YesNoEnum.YES.getDictValue());
                    this.updateStatus(cacheLog, key);
                }
                continue;
            }

            /* -----------------------------------------恒告警处理-------------------------------------------- */
            // 取实时告警值
            if (modelNum != null) {
                // 单体
                alarmValue = this.getAlarmBatteryValue(configAttribute, itemCode, batteryMonitor);
            } else {
                // 组
                alarmValue = this.getAlarmBatteryPackValue(configAttribute, itemCode, batteryReportLog.getPackParam());
            }

            // 匹配告警等级
            AlarmItemLevelVo alarmItemLevel = null;
            if (StrUtil.isNotBlank(alarmValue)) {
                // 告警等级
                alarmItemLevel = this.getLevel(configAttribute, alarmValue);
            }
            // 告警值为空（模拟量未取得实时数据）、未匹配告警等级，取默认告警项
            if (alarmItemLevel == null) {
                alarmItemLevel = this.getDefaultLevel(configAttribute);
            }

            // 告警项未匹配、默认告警项也没有（应不存在，配置错误或同步错误，先置不处理）
            if (alarmItemLevel == null) {
                if (cacheLog != null) {
                    // 存在记录需完成
                    cacheLog.setStatus(YesNoEnum.YES.getDictValue());
                    this.updateStatus(cacheLog, key);
                }
                continue;
            }

            // 告警内容
            String dataInfo = this.getBatteryAlarmInfo(configAttribute, alarmItemLevel, modelNum, alarmValue);

            // 存在缓存告警，更新
            if (cacheLog != null) {
                cacheLog.setAlarmLevel(alarmItemLevel.getLevelCode());
                cacheLog.setDataInfo(dataInfo);
                this.updateStatus(cacheLog, key);
                continue;
            }

            // 创建告警记录
            AlarmLog alarmLog = new AlarmLog();
            alarmLog.setConfigId(configAttribute.getConfigId());
            alarmLog.setItemCode(configAttribute.getCode());
            alarmLog.setPackNum(packNum);
            alarmLog.setModelNum(modelNum);
            alarmLog.setAlarmLevel(alarmItemLevel.getLevelCode());
            // 告警则为未处理状态
            alarmLog.setStatus(YesNoEnum.NO.getDictValue());
            alarmLog.setDataInfo(dataInfo);

            // 滤波处理
            postfilterBattery(alarmLog);

            this.insertAlarm(alarmLog, key);
        }
    }

    /**
     * 获取蓄电池告警属性值
     *
     * @param configAttribute 属性
     * @param itemCode 告警字段名
     * @param packData 电池组实时数据
     * @return 告警值
     */
    private String getAlarmBatteryPackValue(ConfigAttribute configAttribute, String itemCode, Map<String, Object> packData) {
        // 非模拟量，直接返回 1 告警
        if (!Objects.equals(configAttribute.getType(), DataTypeEnum._2.getDictValue())) {
            return "1";
        }

        // 电池组告警
        if (itemCode.contains("dy")) {
            // 组电压
            String voltage = Objects.toString(packData.get("batteryPackOuterVoltage"), null);
            if (voltage != null && Double.parseDouble(voltage) != 0) {
                return voltage;
            }
            return Objects.toString(packData.get("packVoltage"), null);
        }
        if (itemCode.contains("dl")) {
            // 组电流
            return Objects.toString(packData.get("packCurrent"), null);
        }
        if (itemCode.contains("wd")) {
            // 组温度
            return Objects.toString(packData.get("environmentTemperature1"), null);
        }
        if (itemCode.contains("soc")) {
            // soc
            return Objects.toString(packData.get("batteryPackSoc"), null);
        }
        if (itemCode.contains("soh")) {
            // soh
            return Objects.toString(packData.get("batteryPackSoh"), null);
        }
        return null;
    }

    /**
     * 获取蓄电池告警属性值
     *
     * @param configAttribute 属性
     * @param itemCode 告警字段名
     * @param battery 单体实时数据
     * @return 告警值
     */
    private String getAlarmBatteryValue(ConfigAttribute configAttribute, String itemCode, BatteryMonitor battery) {
        // 非模拟量，直接返回 1 告警
        if (!Objects.equals(configAttribute.getType(), DataTypeEnum._2.getDictValue())) {
            return "1";
        }

        if (battery == null) {
            return null;
        }
        if (itemCode.contains("dy")) {
            // 电压
            return battery.getVoltage() != null ? String.valueOf(battery.getVoltage()) : null;
        } else if (itemCode.contains("nz")) {
            // 内阻
            return battery.getResistance() != null ? String.valueOf(battery.getResistance()) : null;
        } else if (itemCode.contains("wd")) {
            // 温度
            return battery.getTemperature() != null ? String.valueOf(battery.getTemperature()) : null;
        } else if (StrUtil.equals(ItemCode.DTLJTGJ.getCode(), itemCode)) {
            // 连接条
            return battery.getResistancerageslip() != null ? String.valueOf(battery.getResistancerageslip()) : null;
        }
        return null;
    }

    /**
     * 蓄电池告警值校验
     *
     * @param config 设备配置
     * @param packNum 电池组编号
     * @param modelNum 模块编号
     * @param warnParam 告警参数
     */
    @Override
    public void alarmBatteryValue(Config config, Integer packNum, Integer modelNum, Map<String, String> warnParam) {
        Integer type = config == null ? null : config.getType();
        for (String keyParam : warnParam.keySet()) {
            // 属性配置
            ConfigAttribute configAttribute = configAttributeService.getCacheBy(packNum, keyParam);
            if (configAttribute == null) {
                continue;
            }

            this.alarmValid(configAttribute, modelNum, warnParam.get(keyParam), type);
        }
    }

    /**
     * 修复指定电池组的告警记录
     *
     * @param packNum 电池组编号
     * @param isModel 是否为单体告警
     * @param excludeModelNum 排除的模块编号列表
     * @param includeItemCode 包含的属性编码列表
     */
    @Override
    public void alarmFix(Integer packNum, Boolean isModel, List<Integer> excludeModelNum, List<String> includeItemCode) {
        // 查设备组下还在告警的记录
        AlarmLog alarmLog = new AlarmLog();
        alarmLog.setConfigId(Constants.DEFAULT_CONFIG_ID);
        alarmLog.setPackNum(packNum);
        alarmLog.setExcludeModelNum(excludeModelNum);
        alarmLog.setIncludeItemCodes(includeItemCode);
        alarmLog.setStatus(YesNoEnum.NO.getDictValue());
        List<AlarmLog> list = alarmLogMapper.selectAlarmLogList(alarmLog);
        if (list == null || list.isEmpty()) {
            return;
        }
        for (AlarmLog log : list) {
            // 指定单体属性
            if (isModel && log.getModelNum() == null) {
                continue;
            }
            // 屏蔽处理
            if (log.getShiedTime() != null
                    && log.getShiedTime().getTime() > System.currentTimeMillis()) {
                continue;
            }
            // 处理完成
            log.setStatus(YesNoEnum.YES.getDictValue());
            this.updateStatus(log, String.format(alarmCache.getKey(), log.getPackNum(), log.getModelNum(), log.getItemCode()));
        }
    }

    /**
     * 校验属性告警
     *
     * @param configAttribute 设备属性
     * @param value 属性值
     */
    @Override
    public void alarmValid(ConfigAttribute configAttribute, String value) {
        this.alarmValid(configAttribute, null, value, null);
    }

    /**
     * 校验属性告警
     *
     * @param configAttribute 设备属性
     * @param modelNum 模块编号
     * @param value 属性值
     * @param type 设备类型
     */
    @Override
    public void alarmValid(ConfigAttribute configAttribute, Integer modelNum, String value, Integer type) {
        // 取缓存告警记录
        String key = String.format(alarmCache.getKey(), configAttribute.getPackNum(), modelNum, configAttribute.getCode());
        AlarmLog cacheLog = (AlarmLog) CacheUtils.get(alarmCache.getCache(), key);
        // 未启用 不参与告警
        if (configAttribute.getStatus() == 1 || configAttribute.getAlarmConfig() == 1) {
            if (cacheLog != null) {
                cacheLog.setStatus(YesNoEnum.YES.getDictValue());
                this.updateStatus(cacheLog, key);
            }
            return;
        }
        // 屏蔽处理
        if (cacheLog != null) {
            // 告警屏蔽时间内，不处理
            if (null != cacheLog.getShiedTime()
                    && cacheLog.getShiedTime().getTime() > System.currentTimeMillis()) {
                return;
            }
            // 告警记录是已处理的（屏蔽时间已过），删除缓存
            if (Objects.equals(cacheLog.getStatus(), YesNoEnum.YES.getDictValue())) {
                cacheLog = null;
                CacheUtils.remove(alarmCache.getCache(), key);
            }
        }

        // 未设置告警，告警等级为空
        if (!Objects.equals(YesNoEnum.YES.getDictValue(), configAttribute.getAlarmConfig())
                || configAttribute.getListLevel() == null || configAttribute.getListLevel().isEmpty()) {
            // 存在未处理完成的，更新记录并删除缓存，避免脏数据
            if (cacheLog != null) {
                cacheLog.setStatus(YesNoEnum.YES.getDictValue());
                this.updateStatus(cacheLog, key);
            }
            return;
        }

        // 告警等级
        AlarmItemLevelVo alarmItemLevelVo = this.getLevel(configAttribute, value);
        // 告警内容
        String alarmInfo = this.getAlarmInfo(configAttribute, alarmItemLevelVo, modelNum, value, type);
        // 存在记录
        if (cacheLog != null) {
            // 无告警，将已有记录更新为已完成，退出
            if (alarmItemLevelVo == null) {
                cacheLog.setStatus(YesNoEnum.YES.getDictValue());
                this.updateStatus(cacheLog, key);
                return;
            }

            //告警级别发生变化或者告警内容发生变化
            if (!StrUtil.equals(cacheLog.getAlarmLevel(), alarmItemLevelVo.getLevelCode())
                    || !StrUtil.equals(cacheLog.getDataInfo(), alarmInfo)) {
                // 有告警，告警内容或告警等级改变，更新
                cacheLog.setAlarmLevel(alarmItemLevelVo.getLevelCode());
                cacheLog.setDataInfo(alarmInfo);
                this.updateStatus(cacheLog, key);
            }
            return;
        }
        // 无缓存记录，最新记录不告警，则不需处理
        if (alarmItemLevelVo == null) {
            return;
        }
        // 创建告警记录
        AlarmLog alarmLog = new AlarmLog();
        alarmLog.setConfigId(configAttribute.getConfigId());
        alarmLog.setItemCode(configAttribute.getCode());
        alarmLog.setPackNum(configAttribute.getPackNum());
        alarmLog.setModelNum(modelNum);
        alarmLog.setAlarmLevel(alarmItemLevelVo.getLevelCode());
        // 告警则为未处理状态
        alarmLog.setStatus(YesNoEnum.NO.getDictValue());
        // 告警内容
        alarmLog.setDataInfo(alarmInfo);
        // 保存告警记录
        this.insertAlarm(alarmLog, key);
    }

    /**
     * 默认告警等级
     */
    private AlarmItemLevelVo getDefaultLevel(ConfigAttribute configAttribute) {
        for (AlarmItemLevelVo levelVo : configAttribute.getListLevel()) {
            // 不需告警
            if (StrUtil.isBlank(levelVo.getLevelCode()) || StrUtil.equals(levelVo.getLevelCode(), AlarmLevelEnum._0.getDictValue())) {
                continue;
            }
            return levelVo;
        }
        return null;
    }

    /**
     * 匹配告警等级
     */
    private AlarmItemLevelVo getLevel(ConfigAttribute configAttribute, String value) {
        if (StrUtil.isBlank(value)) {
            return null;
        }
        // 数据处理类型
        DataTypeEnum dataTypeEnum = DataTypeEnum.find(configAttribute.getType());
        switch (dataTypeEnum) {
            case _2:
                // 模拟量
                Double valueDouble = Double.parseDouble(value);
                Double standValue = null;
                for (AlarmItemLevelVo levelVo : configAttribute.getListLevel()) {
                    // 不需告警
                    if (StrUtil.isBlank(levelVo.getLevelCode()) || StrUtil.equals(levelVo.getLevelCode(), AlarmLevelEnum._0.getDictValue())) {
                        continue;
                    }

                    standValue = levelVo.getStandValue() != null ? levelVo.getStandValue() : standValue;
                    if (DataUtils.isInRange(levelVo.getHightValue(), levelVo.getLowValue(), standValue, valueDouble)) {
                        return levelVo;
                    }
                }
                break;
            case _1:
            case _3:
                // 开关量、枚举量
                for (AlarmItemLevelVo levelVo : configAttribute.getListLevel()) {
                    // 不需告警
                    if (StrUtil.isBlank(levelVo.getLevelCode()) || StrUtil.equals(levelVo.getLevelCode(), AlarmLevelEnum._0.getDictValue())) {
                        continue;
                    }
                    if (StrUtil.equals(value, levelVo.getDictId())) {
                        return levelVo;
                    }
                }
                break;
            default:
                break;
        }
        return null;
    }

    /**
     * 蓄电池告警内容
     */
    private String getBatteryAlarmInfo(ConfigAttribute configAttribute, AlarmItemLevelVo alarmItemLevelVo, Integer modelNum, String value) {
        // 未匹配中告警等级，则无告警内容
        if (alarmItemLevelVo == null || StrUtil.equals(alarmItemLevelVo.getLevelCode(), AlarmLevelEnum._0.getDictValue())) {
            return null;
        }

        // 告警内容
        StringBuilder alarmInfo = new StringBuilder();

        // 电池编号
        if (modelNum != null) {
            alarmInfo.append("单体电池(").append(configAttribute.getPackNum()).append("组，").append(modelNum).append("号)");
        } else {
            alarmInfo.append("电池组(").append(configAttribute.getPackNum()).append("组)");
        }

        // 告警描述（等级描述 --> 属性备注 --> 属性名）
        alarmInfo.append(StrUtil.isNotBlank(alarmItemLevelVo.getAlarmDesc()) ? alarmItemLevelVo.getAlarmDesc() :
                StrUtil.isNotBlank(configAttribute.getRemark()) ? configAttribute.getRemark() : configAttribute.getName());

        // 未匹配值
        if (StrUtil.isBlank(value)) {
            return alarmInfo.toString();
        }
        // 数据处理类型
        DataTypeEnum dataTypeEnum = DataTypeEnum.find(configAttribute.getType());
        switch (dataTypeEnum) {
            case _2:
                // 模拟量
                alarmInfo.append("，当前值：").append(value).append(getUnit(configAttribute));
                break;
            case _3:
                if(alarmItemLevelVo!=null && StrUtil.isBlank(alarmItemLevelVo.getAlarmDesc())) {
                    // 枚举量
                    alarmInfo.append("，当前值：");
                    alarmInfo.append(StrUtil.isNotBlank(alarmItemLevelVo.getDictName()) ? alarmItemLevelVo.getDictName() : value);
                }
                break;
            case _1:
            default:
                break;
        }
        return alarmInfo.toString();
    }

    /**
     * 告警内容
     */
    private String getAlarmInfo(ConfigAttribute configAttribute, AlarmItemLevelVo alarmItemLevelVo, Integer modelNum, String value, Integer type) {
        // 蓄电池单独处理
        if (Objects.equals(type, DeviceTypeEnum._1.getDictValue())) {
            // 单体
            if (modelNum != null) {
                return String.format("单体电池(%s组,%s号)%s", configAttribute.getPackNum(), modelNum, configAttribute.getName());
            }
            return String.format("电池组(%s组)%s", configAttribute.getPackNum(), configAttribute.getName());
        }

        // 未匹配中告警等级，则无告警内容
        if (alarmItemLevelVo == null || StrUtil.equals(alarmItemLevelVo.getLevelCode(), AlarmLevelEnum._0.getDictValue())) {
            return null;
        }

        if (HostAlarmItemEnum._6.getCode().equals(configAttribute.getCode())) {
            return configAttribute.getName() + "通讯异常！";
        }

        // 告警内容
        StringBuilder alarmInfo = new StringBuilder();

        // 告警描述或属性名
        alarmInfo.append(StrUtil.isNotBlank(alarmItemLevelVo.getAlarmDesc()) ?
                alarmItemLevelVo.getAlarmDesc() : configAttribute.getName());

        // 数据处理类型
        DataTypeEnum dataTypeEnum = DataTypeEnum.find(configAttribute.getType());
        switch (dataTypeEnum) {
            case _1:
                // 开关量 告警值
                String desc = alarmInfo.toString();
                if (!desc.contains("告警") && !desc.contains("异常")) {
                    alarmInfo.append("异常");
                }
                if(alarmItemLevelVo!=null && StrUtil.isBlank(alarmItemLevelVo.getAlarmDesc())) {
                    // 枚举量
                    alarmInfo.append("，当前值：");
                    alarmInfo.append(StrUtil.isNotBlank(alarmItemLevelVo.getDictName()) ? alarmItemLevelVo.getDictName() : value);
                }
                break;
            case _2:
                // 模拟量
                alarmInfo.append("，当前值：").append(value).append(getUnit(configAttribute));
                break;
            case _3:
                // 枚举量
                if(alarmItemLevelVo!=null && StrUtil.isBlank(alarmItemLevelVo.getAlarmDesc())) {
                    // 枚举量
                    alarmInfo.append("，当前值：");
                    alarmInfo.append(StrUtil.isNotBlank(alarmItemLevelVo.getDictName()) ? alarmItemLevelVo.getDictName() : value);
                }
                break;
            default:
                alarmInfo.append("，当前值：").append(value);
        }
        return alarmInfo.toString();
    }

    /**
     *  单位
     */
    private String getUnit(ConfigAttribute attribute) {
        if (StrUtil.isNotBlank(attribute.getUnit())) {
            return attribute.getUnit();
        }
        List<String> nzs = Lists.newArrayList(ItemCode.DTNZGX.getCode(), ItemCode.DTNZGD.getCode(), ItemCode.DTNZBJ.getCode(), ItemCode.DTLJTGJ.getCode());
        if (nzs.contains(attribute.getCode())) {
            return "uΩ";
        }
        return "";
    }

    /**
     * 判断指定属性是否存在未处理告警
     *
     * @param attribute 设备属性
     * @return 是否存在告警
     */
    @Override
    public Integer isAlarm(ConfigAttribute attribute) {
        String key = String.format(alarmCache.getKey(), attribute.getPackNum(), null, attribute.getCode());
        AlarmLog cacheLog = (AlarmLog) CacheUtils.get(alarmCache.getCache(), key);
        return cacheLog != null && Objects.equals(cacheLog.getStatus(), YesNoEnum.NO.getDictValue()) ? YesNoEnum.YES.getDictValue() : YesNoEnum.NO.getDictValue();
    }

    /**
     * 关闭指定属性的告警日志
     *
     * @param attribute 设备属性
     */
    @Override
    public void closeAlarmLog(ConfigAttribute attribute) {
        String key = String.format(alarmCache.getKey(), attribute.getPackNum(), null, attribute.getCode());
        Object object = CacheUtils.get(alarmCache.getCache(), key);
        if (object != null) {
            AlarmLog cacheLog = (AlarmLog) object;
            cacheLog.setStatus(YesNoEnum.YES.getDictValue());
            this.updateStatus(cacheLog, key);
        }
    }

    /**
     * 关闭默认设备所有告警日志
     */
    @Override
    public void closeDefaultDeviceAlarmLog() {
        Set<String> keys = CacheUtils.getCacheKeys(alarmCache.getCache());
        // 前缀
        String prefix = "alarm:";
        for (String key : keys) {
            if (!StrUtil.startWith(key, prefix)) {
                continue;
            }
            Object object = CacheUtils.get(alarmCache.getCache(), key);
            if (object != null) {
                AlarmLog cacheLog = (AlarmLog) object;
                cacheLog.setStatus(YesNoEnum.YES.getDictValue());
                this.updateStatus(cacheLog, key);
            }
        }
    }

    /**
     * 新增设备历史记录
     *
     * @param alarmLog 设备历史记录
     * @return 结果
     */
    @Override
    public int insertAlarmLog(AlarmLog alarmLog) {
        String key = String.format(alarmCache.getKey(), alarmLog.getPackNum(), alarmLog.getModelNum(), alarmLog.getItemCode());
        AlarmLog cacheLog = (AlarmLog) CacheUtils.get(alarmCache.getCache(), key);

        // 屏蔽处理
        if (cacheLog != null) {
            // 告警屏蔽时间内，不处理
            if (null != cacheLog.getShiedTime()
                    && cacheLog.getShiedTime().getTime() > System.currentTimeMillis()) {
                return 1;
            }
            // 告警记录是已处理的（屏蔽时间已过），删除缓存
            if (Objects.equals(cacheLog.getStatus(), YesNoEnum.YES.getDictValue())) {
                cacheLog = null;
                CacheUtils.remove(alarmCache.getCache(), key);
            }
        }

        // 告警记录存在，更新
        if (cacheLog != null) {
            cacheLog.setStatus(alarmLog.getStatus());
            this.updateStatus(cacheLog, key);
        } else {
            // 保存告警记录
            if (Objects.equals(alarmLog.getStatus(), YesNoEnum.NO.getDictValue())) {
                this.insertAlarm(alarmLog, key);
            }
        }

        return 1;
    }

    /**
     * 修改设备历史记录
     *
     * @param alarmLog 设备历史记录
     * @return 结果
     */
    @Override
    public int updateAlarmLog(AlarmLog alarmLog) {
        alarmLog.setUpdateTime(new Date());
        alarmLog.setDuration((alarmLog.getUpdateTime().getTime() - alarmLog.getCreateTime().getTime()) / 1000);
        return alarmLogMapper.updateAlarmLog(alarmLog);
    }

    /**
     * 屏蔽告警日志
     *
     * @param shiedAlarm 屏蔽告警参数
     * @return 结果
     */
    @Override
    public int shiedAlarmLog(AlarmLog shiedAlarm) {
        AlarmLog alarmLog = alarmLogMapper.selectAlarmLogByAlarmId(shiedAlarm.getAlarmId());
        if (alarmLog == null) {
            throw new ServiceException("告警信息不存在");
        }
        // 持续时间
        alarmLog.setUpdateTime(new Date());
        alarmLog.setDuration((alarmLog.getUpdateTime().getTime() - alarmLog.getCreateTime().getTime()) / 1000);

        // 是否屏蔽
        alarmLog.setShied(YesNoEnum.YES.getDictValue());
        alarmLog.setShiedTime(shiedAlarm.getShiedTime());
        alarmLog.setShiedTimeStr(DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD_HH_MM_SS, alarmLog.getShiedTime()));

        // 处理信息
        alarmLog.setStatus(YesNoEnum.YES.getDictValue());
        alarmLog.setRemark(shiedAlarm.getRemark());

        // 缓存处理
        String key = String.format(alarmCache.getKey(), alarmLog.getPackNum(), alarmLog.getModelNum(), alarmLog.getItemCode());
        // 如果已处理，且屏蔽时间小于当前时间，删除缓存
        if (Objects.equals(alarmLog.getStatus(), YesNoEnum.YES.getDictValue())
                && (Objects.isNull(alarmLog.getShiedTime()) || alarmLog.getShiedTime().getTime() < System.currentTimeMillis())) {
            CacheUtils.remove(alarmCache.getCache(), key);
        } else {
            CacheUtils.put(alarmCache.getCache(), key, alarmLog);
        }
        return alarmLogMapper.updateAlarmLog(alarmLog);
    }

    /**
     * 批量删除设备历史记录
     *
     * @param alarmIds 需要删除的设备历史记录主键
     * @return 结果
     */
    @Override
    public int deleteAlarmLogByAlarmIds(String alarmIds) {
        String[] alarmIdArr = Convert.toStrArray(alarmIds);
        for (String alarmId : alarmIdArr) {
            this.deleteAlarmLogByAlarmId(Long.valueOf(alarmId));
        }
        return 1;
    }

    /**
     * 删除默认设备所有告警日志
     */
    @Override
    public void deleteDefaultDeviceAlarmLogs() {
        alarmLogMapper.deleteDefaultDeviceAlarmLogs();
        this.updateCache();
    }

    /**
     * 根据告警ID删除告警日志
     *
     * @param alarmId 告警主键
     */
    @Override
    public void deleteAlarmLogByAlarmId(Long alarmId) {
        if (alarmId == null) {
            return;
        }
        AlarmLog alarmLog = this.selectAlarmLogByAlarmId(alarmId);
        if (alarmLog == null) {
            return;
        }
        // 删除缓存
        this.removeCache(alarmLog);
        // 删除告警日志
        alarmLogMapper.deleteAlarmLogByAlarmId(alarmLog.getAlarmId());
    }

    /**
     * 更新告警缓存
     */
    @Override
    public void updateCache() {
        List<String> startKeys = new ArrayList<>();
        Set<String> oldKeys = CacheUtils.getCacheKeys(alarmCache.getCache());
        // 查所有未处理的告警
        List<AlarmLog> alarmLogList = alarmLogMapper.allAlarmLog();
        for (AlarmLog alarmLog : alarmLogList) {
            /* alarm.配置id.包编号.模块编号.属性编码 */
            String key = String.format(alarmCache.getKey(), alarmLog.getPackNum(), alarmLog.getModelNum(), alarmLog.getItemCode());
            // 告警记录存在重复，则把旧告警处理
            if (!startKeys.contains(key)) {
                startKeys.add(key);
                CacheUtils.put(alarmCache.getCache(), key, alarmLog);
            } else {
                // 重复记录处理
                alarmLog.setStatus(YesNoEnum.YES.getDictValue());
                this.updateStatus(alarmLog, key);
            }
        }

        // 删除
        for (String key : oldKeys) {
            if (!startKeys.contains(key)) {
                CacheUtils.remove(alarmCache.getCache(), key);
            }
        }
    }

    /**
     * 从缓存获取指定电池组的告警记录列表
     *
     * @param packNum 电池组编号，为null时查询所有
     * @return 告警记录列表
     */
    @Override
    public List<AlarmLog> selectBatteryAlarmLogListCache(Integer packNum) {
        // 前缀
        String prefix = packNum != null ?
                String.format("alarm:%s:", packNum) :
                "alarm:";

        Set<String> keys = CacheUtils.getCacheKeys(alarmCache.getCache());
        List<AlarmLog> alarmLogs = new ArrayList<>();
        Set<String> existingKeys = new HashSet<>();
        for (String key : keys) {
            if (StrUtil.startWith(key, prefix)) {
                AlarmLog alarmLog = (AlarmLog) CacheUtils.get(alarmCache.getCache(), key);
                alarmLogs.add(alarmLog);
                if (alarmLog != null) {
                    existingKeys.add(communicationAlarmKey(alarmLog));
                }
            }
        }
        alarmLogs.addAll(buildStateCommunicationAlarms(packNum, existingKeys));
        return alarmLogs;
    }

    private boolean hasStateCommunicationAlarm(Integer packNum) {
        return !buildStateCommunicationAlarms(packNum, Collections.emptySet()).isEmpty();
    }

    private List<AlarmLog> buildStateCommunicationAlarms(Integer packNum, Set<String> excludedKeys) {
        if (batteryDeviceStateService == null) {
            return Collections.emptyList();
        }
        List<BatteryDeviceState> states = packNum == null ?
                batteryDeviceStateService.selectList(new BatteryDeviceState()) :
                batteryDeviceStateService.selectByPackNum(packNum);
        if (states == null || states.isEmpty()) {
            return Collections.emptyList();
        }
        List<AlarmLog> alarmLogs = new ArrayList<>();
        Set<String> dedupe = new HashSet<>();
        for (BatteryDeviceState state : states) {
            AlarmLog alarmLog = toCommunicationAlarmLog(state);
            if (alarmLog == null) {
                continue;
            }
            String key = communicationAlarmKey(alarmLog);
            if (excludedKeys != null && excludedKeys.contains(key)) {
                continue;
            }
            if (dedupe.add(key)) {
                alarmLogs.add(alarmLog);
            }
        }
        return alarmLogs;
    }

    private String communicationAlarmKey(AlarmLog alarmLog) {
        return alarmLog.getPackNum() + ":" + alarmLog.getModelNum() + ":" + alarmLog.getItemCode();
    }

    private AlarmLog toCommunicationAlarmLog(BatteryDeviceState state) {
        if (state == null || state.getPackNum() == null || !isActiveCommunicationState(state)) {
            return null;
        }
        String itemCode = toCommunicationItemCode(state.getStateCode());
        if (itemCode == null) {
            return null;
        }
        AlarmLog alarmLog = new AlarmLog();
        alarmLog.setConfigId(Constants.DEFAULT_CONFIG_ID);
        alarmLog.setConfigName("蓄电池");
        alarmLog.setType(DeviceTypeEnum._1.getDictValue());
        alarmLog.setPackNum(state.getPackNum());
        alarmLog.setModelNum(state.getModelNum());
        alarmLog.setItemCode(itemCode);
        alarmLog.setAlarmLevel("2");
        alarmLog.setStatus(YesNoEnum.NO.getDictValue());
        alarmLog.setDuration(0L);
        alarmLog.setDataInfo(toCommunicationAlarmInfo(state, itemCode));
        alarmLog.setCreateTime(state.getFirstSeenTime() == null ? state.getLastChangeTime() : state.getFirstSeenTime());
        alarmLog.setUpdateTime(state.getLastUpdateTime() == null ? state.getLastChangeTime() : state.getLastUpdateTime());
        return alarmLog;
    }

    private boolean isActiveCommunicationState(BatteryDeviceState state) {
        String code = state.getStateCode();
        String value = state.getStateValue();
        String level = state.getStateLevel();
        if (BatteryDeviceStateConstants.StateCode.CHANNEL_OPEN.equals(code)) {
            return BatteryDeviceStateConstants.StateLevel.ERROR.equals(level) || "closed".equals(value);
        }
        if (BatteryDeviceStateConstants.StateCode.CHANNEL_ERROR.equals(code)) {
            return BatteryDeviceStateConstants.StateLevel.ERROR.equals(level);
        }
        if (BatteryDeviceStateConstants.StateCode.CHANNEL_TIMEOUT_COUNT.equals(code)) {
            return BatteryDeviceStateConstants.StateLevel.WARN.equals(level)
                    || BatteryDeviceStateConstants.StateLevel.ERROR.equals(level);
        }
        if (BatteryDeviceStateConstants.StateCode.MODULE_TIMEOUT.equals(code)) {
            return !BatteryDeviceStateConstants.StateLevel.NORMAL.equals(level) && !"recovered".equals(value);
        }
        if (BatteryDeviceStateConstants.StateCode.MODULE_ACTIVE.equals(code)) {
            return "inactive".equals(value);
        }
        if (BatteryDeviceStateConstants.StateCode.GROUP_246_FRESHNESS.equals(code)) {
            return "stale".equals(value);
        }
        return false;
    }

    private String toCommunicationItemCode(String stateCode) {
        if (BatteryDeviceStateConstants.StateCode.CHANNEL_OPEN.equals(stateCode)
                || BatteryDeviceStateConstants.StateCode.CHANNEL_ERROR.equals(stateCode)) {
            return ItemCode.DTTXZT.getCode();
        }
        if (BatteryDeviceStateConstants.StateCode.CHANNEL_TIMEOUT_COUNT.equals(stateCode)
                || BatteryDeviceStateConstants.StateCode.MODULE_TIMEOUT.equals(stateCode)
                || BatteryDeviceStateConstants.StateCode.MODULE_ACTIVE.equals(stateCode)
                || BatteryDeviceStateConstants.StateCode.GROUP_246_FRESHNESS.equals(stateCode)) {
            return ItemCode.TXZT.getCode();
        }
        return null;
    }

    private String toCommunicationAlarmInfo(BatteryDeviceState state, String itemCode) {
        String detail = StrUtil.isNotBlank(state.getDetail()) ? state.getDetail() : state.getStateValue();
        String prefix = ItemCode.DTTXZT.getCode().equals(itemCode) ? "单体通信异常" : "组压通讯异常";
        return StrUtil.isBlank(detail) ? prefix : prefix + "：" + detail;
    }

    /**
     * 获取当前属性编码是否存在未处理告警
     *
     * @param itemCode 属性编码
     * @return 是否存在告警，1-存在，0-不存在
     */
    @Override
    public Long getCurrentIsAlarm(String itemCode) {
        Set<String> keys = CacheUtils.getCacheKeys(alarmCache.getCache());
        if (keys == null || keys.isEmpty()) {
            return 0L;
        }
        // 前缀
        for (String key : keys) {
            if (!StrUtil.endWith(key, itemCode)) {
                continue;
            }
            AlarmLog alarmLog = (AlarmLog) CacheUtils.get(alarmCache.getCache(), key);
            if (Objects.equals(alarmLog.getStatus(), YesNoEnum.NO.getDictValue())) {
                return 1L;
            }
        }
        return 0L;
    }

    /**
     * 删除所有告警日志并清空缓存
     */
    @Override
    public void deleteAll() {
        alarmLogMapper.deleteAll();
        CacheUtils.removeAll(alarmCache.getCache());
    }

    /**
     * 删除指定电池组的告警日志
     *
     * @param packNum 电池组编号
     */
    @Override
    public void deleteBatteryAlarmLogByPackNum(Integer packNum) {
        alarmLogMapper.deleteBatteryAlarmLogByPackNum(packNum);
    }


    /**
     * 导出告警数据到Excel
     *
     * @param alarmLog 查询条件
     */
    @Override
    public void export(AlarmLog alarmLog) {
        List<AlarmLog> list = selectAlarmLogList(alarmLog);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");

        String fileName = FileUtils.getUsbPath(alarmLog.getExportPath()) + "告警数据_" + sdf.format(new Date()) + ".xlsx";
        // 确保目录存在
        File file = new File(fileName);
        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }
        EasyExcel.write(fileName, AlarmLogDTO.class).sheet("告警数据").doWrite(data(list));
    }

    private List<AlarmLogDTO> data(List<AlarmLog> list) {
        AlarmLevelService alarmLevelService = SpringUtils.getBean(AlarmLevelService.class);

        Map<String, String> map = alarmLevelService.map();

        List<AlarmLogDTO> listDTO = new ArrayList<>();
        for (AlarmLog alarmLog : list) {
            AlarmLogDTO alarmLogDTO = new AlarmLogDTO();
            if (alarmLog.getPackNum() != null) {
                alarmLogDTO.setPackNum("第" + alarmLog.getPackNum() + "组");
            }
            alarmLogDTO.setModelNum(alarmLog.getModelNum());

            alarmLogDTO.setDataInfo(alarmLog.getDataInfo());
            alarmLogDTO.setCreateTime(alarmLog.getCreateTime());

            // 0-是，1-否
            if (YesNoEnum.YES.getDictValue().equals(alarmLog.getStatus())) {
                alarmLogDTO.setStatusStr("已处置");
                alarmLogDTO.setUpdateTime(alarmLog.getUpdateTime());
            } else {
                alarmLogDTO.setStatusStr("未处置");
            }

            alarmLogDTO.setAlarmLevelStr(map.getOrDefault(alarmLog.getAlarmLevel(), ""));

            // 将 alarmLog.getDuration() + "" 替换为格式化方法

            Long duration = (alarmLog.getUpdateTime().getTime() - alarmLog.getCreateTime().getTime()) / 1000;
            alarmLogDTO.setDurationStr(formatDuration(duration));

            listDTO.add(alarmLogDTO);
        }
        return listDTO;
    }

    // 添加格式化方法
    private static String formatDuration(Long duration) {
        if (duration == null || duration <= 0) {
            return "0秒";
        }

        long days = duration / (24 * 60 * 60);
        long hours = (duration % (24 * 60 * 60)) / (60 * 60);
        long minutes = (duration % (60 * 60)) / 60;
        long seconds = duration % 60;

        StringBuilder sb = new StringBuilder();
        if (days > 0) {
            sb.append(days).append("天 ");
        }
        sb.append(String.format("%02d:%02d:%02d", hours, minutes, seconds));

        return sb.toString();
    }


    /**
     * 删除缓存
     */
    private void removeCache(AlarmLog alarmLog) {
        String key = String.format(alarmCache.getKey(), alarmLog.getPackNum(), alarmLog.getModelNum(), alarmLog.getItemCode());
        AlarmLog cacheLog = (AlarmLog) CacheUtils.get(alarmCache.getCache(), key);
        if (cacheLog != null && Objects.equals(cacheLog.getAlarmId(), alarmLog.getAlarmId())) {
            CacheUtils.remove(alarmCache.getCache(), key);
        }
    }

    /**
     * 更新告警状态
     *
     * @param alarmLog 告警日志
     * @param key 缓存key
     */
    private void updateStatus(AlarmLog alarmLog, String key) {
        alarmLog.setUpdateTime(new Date());
        // 如果状态处理完成，则删除缓存
        if (Objects.equals(alarmLog.getStatus(), YesNoEnum.YES.getDictValue())) {
            // 保存持续时间
            alarmLog.setDuration((alarmLog.getUpdateTime().getTime() - alarmLog.getCreateTime().getTime()) / 1000);
            CacheUtils.remove(alarmCache.getCache(), key);
        } else {
            CacheUtils.put(alarmCache.getCache(), key, alarmLog);
        }
        alarmLogMapper.updateAlarmLog(alarmLog);
        // 上报
        this.alarmReport(alarmLog);
    }

    /**
     * 插入告警记录
     *
     * @param alarmLog 告警日志
     */
    private void insertAlarm(AlarmLog alarmLog, String key) {
        // 告警记录不存在，新增，加缓存
        alarmLog.setAlarmId(IdUtils.getSnowflakeId());
        alarmLog.setCreateTime(new Date());
        alarmLogMapper.insertAlarmLog(alarmLog);

        // 缓存
        CacheUtils.put(alarmCache.getCache(), key, alarmLog);

        // 上报
        this.alarmReport(alarmLog);
    }

    private void postfilterBattery(AlarmLog alarmLog) {

        if (ObjUtil.equals(alarmLog.getItemCode(), ItemCode.TXZT.getCode())) {
            // 1、结束测试记录
            optLogService.closeOptLog(alarmLog.getPackNum());
            // 2、自动编号、内阻测试状态缓存重置
            controlBatterySet.clearModelNum(alarmLog.getPackNum());
        }
    }

    /**
     * 上报告警数据
     *
     * @param alarmLog 告警数据
     */
    private void alarmReport(AlarmLog alarmLog) {
        clientReportService.uploadAlarm(alarmLog, null);
    }
}
