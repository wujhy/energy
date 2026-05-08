package com.shanhe.project.device.config.service.impl;

import java.util.*;
import java.util.stream.Collectors;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.shanhe.common.constant.Constants;
import com.shanhe.common.exception.ServiceException;
import com.shanhe.common.utils.Crc16m;
import com.shanhe.common.utils.CacheUtils;
import com.shanhe.common.utils.CheckCode;
import com.shanhe.common.utils.uuid.IdUtils;
import com.shanhe.framework.comm.CommServer;
import com.shanhe.framework.enums.*;
import com.shanhe.framework.manager.AsyncTaskManager;
import com.shanhe.framework.comm.tcp.utils.CodingUtil;
import com.shanhe.project.device.alarm.domain.AlarmLog;
import com.shanhe.project.device.alarm.service.IAlarmLogService;
import com.shanhe.project.device.config.DefaultBatteryConfigRepository;
import com.shanhe.project.device.config.domain.*;
import com.shanhe.project.device.config.service.IBatteryPackService;
import com.shanhe.project.device.config.service.IConfigAttributeService;
import com.shanhe.project.device.config.service.IConfigProtocolService;
import com.shanhe.project.device.opt.service.ControlBattery;
import com.shanhe.project.device.opt.service.DeviceCmdService;
import com.shanhe.project.device.opt.service.OptLogService;
import com.shanhe.project.sync.service.ClientReportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import com.shanhe.project.device.config.service.IConfigService;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

/**
 * 设备Service业务层处理
 *
 * @author wjh
 * @since 2024-12-23
 */
@Service
public class ConfigServiceImpl implements IConfigService {
    protected static Logger logger = LoggerFactory.getLogger(IConfigService.class);
    @Resource
    private IConfigAttributeService configAttributeService;
    @Resource
    private IConfigProtocolService configProtocolService;
    @Resource
    private IBatteryPackService batteryPackService;
    @Resource
    private IAlarmLogService alarmLogService;
    @Resource
    private DeviceCmdService deviceCmdService;
    @Resource
    private ControlBattery controlBattery;
    @Resource
    private OptLogService optLogService;
    @Resource
    private BatteryPackAsync batteryPackAsync;
    @Resource
    private ClientReportService clientReportService;

    // 缓存枚举
    CacheKeyEnum configCache = CacheKeyEnum.CONFIG;

    @Override
    public Config selectConfigBy(Integer type, Integer port, Integer channel) {
        return DefaultBatteryConfigRepository.selectBy(type, port, channel);
    }

    /**
     * 查询设备
     *
     * @param configId 设备主键
     * @return 设备
     */
    @Override
    public Config selectConfigByConfigId(Long configId) {
        Config config = DefaultBatteryConfigRepository.selectByConfigId(configId);
        if (config == null) {
            return null;
        }

        if (Objects.equals(config.getType(), DeviceTypeEnum._1.getDictValue())) {
            config.setPackList(batteryPackService.selectBatteryPackListConfigId(config.getConfigId(), null));
        }
        return config;
    }

    @Override
    public Config getOnlineConfig(Long configId) {
        Config config = this.selectConfigByConfigId(configId);
        if (config == null
                || !Objects.equals(config.getStatus(), YesNoEnum.YES.getDictValue())
                || !Objects.equals(config.getOnline(), YesNoEnum.YES.getDictValue())) {
            throw new ServiceException("设备未上线！");
        }
        return config;
    }

    @Override
    public Config getCacheBy(String type, String port, String channel) {
        try {
            String key = String.format(configCache.getKey(),
                    StrUtil.isNotBlank(type) ? Integer.parseInt(type) : 0,
                    StrUtil.isNotBlank(port) ? Integer.parseInt(port) : 0,
                    StrUtil.isNotBlank(channel) ? Integer.parseInt(channel) : 1);
            return (Config) CacheUtils.get(configCache.getCache(), key);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public Config getCacheBy(Integer type, Integer port, Integer channel) {
        try {
            return (Config) CacheUtils.get(configCache.getCache(),
                    String.format(configCache.getKey(), type, port, channel));
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 查询设备列表
     *
     * @param config 设备
     * @return 设备
     */
    @Override
    public List<Config> selectConfigList(Config config) {
        List<Config> configs = DefaultBatteryConfigRepository.selectList(config);
        configs.forEach(item -> {
            if (Objects.equals(item.getType(), DeviceTypeEnum._1.getDictValue())) {
                item.setPackList(batteryPackService.selectBatteryPackListConfigId(item.getConfigId(), null));
            }
        });
        return configs;
    }

    @Override
    public List<Config> reportConfigList() {
        List<Config> list = DefaultBatteryConfigRepository.selectList(new Config());
        for (Config configDTO : list) {
            // 电池类型补充包列表
            if (Objects.equals(configDTO.getType(), DeviceTypeEnum._1.getDictValue())) {
                configDTO.setPackList(batteryPackService.selectBatteryPackListConfigId(configDTO.getConfigId(), null));
            }
        }
        return list;
    }

    @Override
    public List<Config> screenConfigList() {
        Config config = new Config();
        config.setStatus(YesNoEnum.YES.getDictValue());
        List<Config> list = DefaultBatteryConfigRepository.selectList(config);
        for (Config configDTO : list) {
            // 电池类型补充包列表
            if (Objects.equals(configDTO.getType(), DeviceTypeEnum._1.getDictValue())) {
                List<BatteryPack> v = new ArrayList<>();
                List<BatteryPack> batteryPackList = batteryPackService.selectBatteryPackListConfigId(configDTO.getConfigId(), config.getStatus());
                for (BatteryPack batteryPack : batteryPackList) {
                    if (Objects.equals(batteryPack.getIsEnabled(), YesNoEnum.NO.getDictValue())) {
                        // 蓄电池组是否告警
                        continue;
                    }
                    // 蓄电池组是否告警
                    batteryPack.setAlarm(alarmLogService.isAlarmByCache(configDTO.getConfigId(), batteryPack.getPackNum()));
                    v.add(batteryPack);
                }
                configDTO.setPackList(v);
            }
            // 是否告警
            configDTO.setAlarm(alarmLogService.isAlarmByCache(configDTO.getConfigId(), null));
        }
        return list;
    }

    @Override
    public List<Config> cacheConfigList() {
        List<Config> configList = new ArrayList<>();
        Set<String> keys = CacheUtils.getCacheKeys(configCache.getCache());
        for (String key : keys) {
            // 获取缓存中开启的设备
            Object object = CacheUtils.get(configCache.getCache(), key);
            if (object == null) {
                continue;
            }
            Config config = (Config) object;
            if (Objects.equals(config.getStatus(), YesNoEnum.YES.getDictValue())) {
                configList.add(config);
            }
        }
        return configList;
    }

    @Override
    public void sendAllStorageCmd() {
        // 先删除所有存储指令
        deviceCmdService.cmdDelAll();

        // 所有缓存的已开启设备
        List<Config> configList = this.cacheConfigList();
        if (configList.isEmpty()) {
            logger.debug("同步协议指令，无启用的设备");
            return;
        }

        for (Config config : configList) {
            // 下发串口信息
            deviceCmdService.cmdPort(config);
            // 下发指令
            configProtocolService.cmdStorageSendByConfig(config);
        }
    }

    @Override
    public void sendAllPortCmd() {
        // 所有缓存的已开启设备
        List<Config> configList = this.cacheConfigList();
        if (configList.isEmpty()) {
            return;
        }

        for (Config config : configList) {
            deviceCmdService.cmdPort(config);
        }
    }

    @Override
    public void sendAllProtocolCmd() {
        // 先删除所有存储指令
        deviceCmdService.cmdDelAll();

        // 所有缓存的已开启设备
        List<Config> configList = this.cacheConfigList();
        if (configList.isEmpty()) {
            return;
        }

        for (Config config : configList) {
            configProtocolService.cmdStorageSendByConfig(config);
        }
    }

    @Override
    public void sendBatterySyncCmd(Config config) {
        // 同步电池组信息
        controlBattery.doUploadBattery(config);
        // 同步蓄电池时间
        controlBattery.doSynBatteryDate(config);

        // 已开启电池组
        List<BatteryPack> batteryPackList = batteryPackService.selectBatteryPackListConfigId(config.getConfigId(), YesNoEnum.YES.getDictValue());
        if (batteryPackList.isEmpty()) {
            logger.debug("蓄电池设备 {} 未设置电池组，同步终止", config.getName());
            return;
        }
        for (BatteryPack batteryPack : batteryPackList) {
            try {
                // 同步告警配置
                controlBattery.doSynBatteryAlarm(config, batteryPack.getPackNum(), false);
            } catch (Exception e) {
                logger.error("同步蓄电池设备 {} 参数失败：{}", config.getName(), e.getMessage());
            }
        }
    }

    @Override
    public Config screenConfig(Long configId) {
        Config config = this.selectConfigByConfigId(configId);
        if (config == null) {
            return null;
        }
        // 是否告警
        config.setAlarm(alarmLogService.isAlarmByCache(config.getConfigId(), null));
        config.setAlarmNum(alarmLogService.alarmNum(config.getConfigId()));
        return config;
    }

    @Override
    public List<Config> export(Config config) {
        List<Config> list = this.selectConfigList(config);
        for (Config configExport : list) {
            // 电池组
            if (Objects.equals(configExport.getType(), DeviceTypeEnum._1.getDictValue())) {
                List<BatteryPack> packList = batteryPackService.selectBatteryPackListConfigId(configExport.getConfigId(), config.getStatus());
                if (packList != null && !packList.isEmpty()) {
                    configExport.setPackListJson(JSON.toJSONString(packList));
                }
            }
            // 属性
            List<ConfigAttribute> attributeList = configAttributeService.selectByConfigId(configExport.getConfigId());
            if (attributeList != null && !attributeList.isEmpty()) {
                configExport.setAttrListJson(JSON.toJSONString(attributeList));
            }
            // 协议
            List<ConfigProtocol> protocolList = configProtocolService.exportByConfigId(configExport.getConfigId());
            if (protocolList != null && !protocolList.isEmpty()) {
                configExport.setProtocolListJson(JSON.toJSONString(protocolList));
            }
        }
        return list;
    }

    @Override
    public void importConfig(List<Config> list) {
        throw new ServiceException("默认蓄电池配置为静态资源，不支持导入维护");
    }

    @Override
    public void insertConfig(Config config) {
        throw new ServiceException("默认蓄电池配置为静态资源，不支持新增维护");
    }
    @Override
    public void insertConfigBySync(Config config) {
        throw new ServiceException("默认蓄电池配置为静态资源，不支持同步新增");
    }


    @Override
    public int updateConfig(Config config) {
        throw new ServiceException("默认蓄电池配置为静态资源，不支持更新维护");
    }

    @Override
    public void updateConfigBySync(Config config) {
        throw new ServiceException("默认蓄电池配置为静态资源，不支持同步更新");
    }

    @Override
    public void updatePost(Config config) {
        this.updateCache(DefaultBatteryConfigRepository.selectByConfigId(Constants.DEFAULT_CONFIG_ID));
    }

    @Override
    public int updateStatus(Long configId, Integer status) {
        throw new ServiceException("默认蓄电池配置为静态资源，不支持状态维护");
    }
    /**
     * 异步执行下发指令。
     */
    @Override
    public void sendCmdAsync(Long configId) {
        AsyncTaskManager.me().execute(
                new TimerTask() {
                    @Override
                    public void run() {
                        try {
                            sendCmd(selectConfigByConfigId(configId));
                        } catch (Exception e) {
                            logger.error("设备缓存出错：{}", e.getMessage(), e);
                        }
                    }
                }
        );
    }

    private void sendCmd(Config config) {
        if (config == null) {
            return;
        }
        // 更新缓存
        this.updateCache(config);

        // 上报设备信息至服务端
        if (clientReportService.canSend()) {
            try {
                clientReportService.uploadDev(config, null);
            } catch (Exception e) {
                logger.error("上报设备失败：{}", e.getMessage());
            }
        }

        // 通道未连接，不下发指令
        if (!CommServer.isOpen()) {
            return;
        }

        // 只要通道开启，均下发串口配置
        deviceCmdService.cmdPort(config);

        // 蓄电池，下发蓄电池组配置
        if (Objects.equals(config.getType(), DeviceTypeEnum._1.getDictValue())
                && config.getPackList() != null && !config.getPackList().isEmpty()) {
            batteryPackService.cmdBatteryPack(config.getConfigId(), config.getPackList());
        }

        // 设备未开启时
        if (Objects.equals(config.getStatus(), YesNoEnum.NO.getDictValue())) {
            // 删除存储指令
            configProtocolService.cmdStorageDelByConfig(config);
            return;
        }

        // ------------------ 设备开启时 -----------------------
        // 同步蓄电池信息
        if (Objects.equals(config.getType(), DeviceTypeEnum._1.getDictValue())) {
            this.sendBatterySyncCmd(config);
        }

        // 下发存储指令
        configProtocolService.cmdStorageSendByConfig(config);
    }

    /**
     * 批量删除设备
     *
     * @param configIds 需要删除的设备主键
     * @return 结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteConfigByConfigIds(String configIds) {
        throw new ServiceException("默认蓄电池配置为静态资源，不支持删除维护");
    }

    @Override
    public void deleteConfig(Config config) {
        throw new ServiceException("默认蓄电池配置为静态资源，不支持删除维护");
    }

    @Override
    public void updateCache() {
        try {
            // 配置键
            List<String> startKeys = new ArrayList<>();
            Set<String> oldKeys = CacheUtils.getCacheKeys(configCache.getCache());

            // 查所有启用的配置（缓存全部，是否启用通过使用端判断）
            List<Config> list = DefaultBatteryConfigRepository.selectList(new Config());
            for (Config config : list) {
                // 蓄电池，补充包属性
                if (Objects.equals(config.getType(), DeviceTypeEnum._1.getDictValue())) {
                    config.setPackList(batteryPackService.selectBatteryPackListConfigId(config.getConfigId(), config.getStatus()));
                }
                String key = String.format(configCache.getKey(), config.getType(), config.getPort(), config.getChannel());
                CacheUtils.put(configCache.getCache(), key, config);
                startKeys.add(key);
            }

            // 删除无效记录
            for (String key : oldKeys) {
                if (!startKeys.contains(key)) {
                    CacheUtils.remove(configCache.getCache(), key);
                }
            }
        } catch (Exception e) {
            logger.error("更新设备缓存失败", e);
        }

        // 更新全部属性
        try {
            configAttributeService.updateCache();
        } catch (Exception e) {
            logger.error("更新设备属性缓存失败", e);
        }

        // 更新全部协议
        try {
            configProtocolService.updateCache();
        } catch (Exception e) {
            logger.error("更新设备协议缓存失败", e);
        }

        // 更新全部操作日志
        try {
            optLogService.updateCache();
        } catch (Exception e) {
            logger.error("更新设备操作日志缓存失败", e);
        }
    }

    @Override
    public void online(Config config) {
        config.setOnline(YesNoEnum.YES.getDictValue());
        DefaultBatteryConfigRepository.updateOnline(YesNoEnum.YES.getDictValue());
        // 更新缓存
        this.updateCache(config);
        // 更新告警
        this.updateTxAlarm(config);
    }

    @Override
    public void offline(Config config) {
        if (Objects.equals(config.getOnline(), YesNoEnum.YES.getDictValue())) {
            config.setOnline(YesNoEnum.NO.getDictValue());
            DefaultBatteryConfigRepository.updateOnline(YesNoEnum.NO.getDictValue());
            // 更新缓存
            this.updateCache(config);
        }
        // 更新告警
        this.updateTxAlarm(config);
    }

    @Override
    public void offlineAll() {
        DefaultBatteryConfigRepository.updateOnline(YesNoEnum.NO.getDictValue());
        this.updateCache();
    }

    @Override
    public void updateExtend(Long configId, Map<String, Object> map) {
        Config config = DefaultBatteryConfigRepository.selectByConfigId(configId);
        if (config == null) {
            return;
        }
        Map<String, Object> mapAll = StrUtil.isNotBlank(config.getExtend3()) ? JSON.parseObject(config.getExtend3()) : new HashMap<>();
        mapAll.putAll(map);
        DefaultBatteryConfigRepository.updateExtend(mapAll);
    }

    @Override
    public Map<String, Object> getExtend(Long configId) {
        Config config = DefaultBatteryConfigRepository.selectByConfigId(configId);
        if (config == null) {
            return null;
        }
        return StrUtil.isNotBlank(config.getExtend3()) ? JSON.parseObject(config.getExtend3()) : null;
    }

    @Override
    public void updatePack(Config config) {
        this.updatePack(config, true);
    }

    @Override
    public void deleteAll() {
        throw new ServiceException("默认蓄电池配置为静态资源，不支持清空维护");
    }

    public void updatePack(Config config, boolean syncAttribute) {
        if (!Objects.equals(config.getType(), DeviceTypeEnum._1.getDictValue())) {
            return;
        }
        // 旧电池组处理
        List<BatteryPack> oldPackList = batteryPackService.selectBatteryPackListConfigId(config.getConfigId(), null);
        List<Long> oldPackIds = oldPackList.stream().map(BatteryPack::getPackId).collect(Collectors.toList());
        List<BatteryPack> newPackList = config.getPackList();

        if (newPackList == null || newPackList.isEmpty()) {
            if (!oldPackList.isEmpty()) {
                batteryPackService.deleteBatteryPackByBatPackIds(oldPackIds);
                List<Integer> deletePackNums = new ArrayList<>();
                oldPackList.forEach(oldPack -> {
                    alarmLogService.alarmFix(config.getConfigId(), oldPack.getPackNum(), false, null, null);
                    deletePackNums.add(oldPack.getPackNum());
                });

                if (!deletePackNums.isEmpty()) {
                    configAttributeService.deleteConfigAttributeByPackNums(config.getConfigId(), deletePackNums);
                    deletePackNums.forEach(packNum -> {
                        // 删除指令
                        batteryPackAsync.delSendCmd(config, packNum);
                    });
                }
            }
            return;
        }

        // 删旧包属性
        Set<Integer> deletePackNums = new HashSet<>();
        List<Long> deletePackIds = new ArrayList<>();
        // 删除修改组
        boolean needDel;

        for (BatteryPack oldPack : oldPackList) {
            needDel = true;
            for (BatteryPack newPack : newPackList) {
                if (Objects.equals(oldPack.getPackNum(), newPack.getPackNum())) {
                    newPack.setPackId(oldPack.getPackId());
                    needDel = false;
                }
            }
            if (needDel) {
                deletePackNums.add(oldPack.getPackNum());
                deletePackIds.add(oldPack.getPackId());
            }
        }

        batteryPackService.deleteBatteryPackByBatPackIds(deletePackIds);
        if (!deletePackNums.isEmpty()) {
            configAttributeService.deleteConfigAttributeByPackNums(config.getConfigId(), new ArrayList<>(deletePackNums));
            // 删除告警
            deletePackNums.forEach(packNum -> {
                alarmLogService.alarmFix(config.getConfigId(), packNum, false, null, null);
            });
            // 删除指令
            deletePackNums.forEach(packNum -> {
                batteryPackAsync.delSendCmd(config, packNum);
            });
        }

        // 更新或者创建新记录

        for (BatteryPack batteryPack : newPackList) {
            if (batteryPack.getPackId() == null || !oldPackIds.contains(batteryPack.getPackId())) {
                batteryPack.setConfigId(config.getConfigId());
                batteryPack.setPackId(IdUtils.getSnowflakeId());
                batteryPackService.insertBatteryPack(batteryPack);
                // 设备来自同步，不初始属性
                if (syncAttribute) {
                    // 属性挂电池组
                    this.insertAttribute(config, batteryPack.getPackNum(), batteryPack.getBatSinModel());
                }
            } else {
                // 更新
                batteryPackService.update(batteryPack);
            }
        }
    }

    /**
     * 更新设备通讯状态
     *
     * @param config 设备
     */
    public void updateTxAlarm(Config config) {
        // 通讯状态
        if (Objects.equals(config.getType(), DeviceTypeEnum._1.getDictValue())) {
            // 蓄电池
            AlarmLog alarmLog = new AlarmLog();
            alarmLog.setConfigId(config.getConfigId());
            alarmLog.setType(config.getType());
            alarmLog.setStatus(config.getOnline());
            HostAlarmItemEnum alarmItemEnum = HostAlarmItemEnum._6;
            alarmLog.setItemCode(alarmItemEnum.getCode());
            alarmLog.setDataInfo(config.getName() + "通讯异常！");
            alarmLog.setAlarmLevel(alarmItemEnum.getLevel());
            alarmLogService.insertAlarmLog(alarmLog);
        } else {
            // 默认按配置属性校验
            ConfigAttribute txAttribute = configAttributeService.getCacheBy(config.getConfigId(), HostAlarmItemEnum._6.getCode());
            if (txAttribute == null) {
                return;
            }
            txAttribute.setName(config.getName());
            // 0：在线，1：不在线 -- 0：正常，1：告警
            alarmLogService.alarmValid(txAttribute, String.valueOf(config.getOnline()));
        }
    }

    /**
     * 更新指定设备缓存
     *
     * @param config 设备
     */
    private void updateCache(Config config) {
        // 设备任何错都更新缓存
        String key = String.format(configCache.getKey(), config.getType(), config.getPort(), config.getChannel());
        CacheUtils.put(configCache.getCache(), key, config);

        if (Objects.equals(config.getStatus(), YesNoEnum.YES.getDictValue())) {
            // 更新属性
            configAttributeService.updateCache(config.getConfigId(), YesNoEnum.YES.getDictValue());
            // 更新协议
            configProtocolService.updateCache(config.getConfigId(), YesNoEnum.YES.getDictValue());
        } else {
            // 删除属性
            configAttributeService.updateCache(config.getConfigId(), YesNoEnum.NO.getDictValue());
            // 更新协议
            configProtocolService.updateCache(config.getConfigId(), YesNoEnum.NO.getDictValue());
            // 关闭告警
            alarmLogService.closeAlarmLog(config.getConfigId());
        }
    }

    /**
     * 同步更新电池组
     *
     * @param config 配置id
     */
    private void insertPack(Config config) {
        if (config.getPackList() == null || config.getPackList().isEmpty()) {
            return;
        }
        for (BatteryPack batteryPack : config.getPackList()) {
            batteryPack.setConfigId(config.getConfigId());
            batteryPack.setPackId(IdUtils.getSnowflakeId());
            batteryPackService.insertBatteryPack(batteryPack);

            // 属性挂电池组
            this.insertAttribute(config, batteryPack.getPackNum(), batteryPack.getBatSinModel());
        }
    }

    /**
     * 同步更新属性
     *
     * @param config 配置id
     */
    private void insertAttribute(Config config, Integer packNum, Integer model) {
        configAttributeService.insertByTemplateAttribute(config.getConfigId(), packNum, model);
    }

    /**
     * 同步模板协议
     *
     * @param config 设备
     */
    private void insertProtocol(Config config) {
        if (config.getTmplId() == null) {
            return;
        }
        // 获取模板协议
        List<ConfigProtocol> tmpProtocolList = configProtocolService.exportByConfigId(config.getTmplId());
        if (tmpProtocolList == null || tmpProtocolList.isEmpty()) {
            return;
        }
        // 替换通道号
        String channel = CodingUtil.integerToHexString(config.getChannel(), 2);

        for (ConfigProtocol protocol : tmpProtocolList) {
            // 替换内容
            String cmdContent = protocol.getCmdContent().replace(ProtocolParamEnum.CHANNEL.getDictValue(), channel);
            // 校验码设置
            protocol.setCmdContent(this.getCheckCode(protocol.getChecksumAlgorithm(), cmdContent));
            protocol.setProtocolId(null);
            protocol.setTemplate(YesNoEnum.NO.getDictValue());
            protocol.setConfigId(config.getConfigId());

            // 批量保存
            configProtocolService.insertConfigProtocol(protocol);
        }
    }

    /**
     * 替换校验码占位符
     *
     * @param cmdContent 协议内容
     * @return 协议
     */
    private String getCheckCode(Integer checksumAlgorithm, String cmdContent) {
        ProtocolAlgorithmEnum algorithmEnum = ProtocolAlgorithmEnum.find(checksumAlgorithm);
        switch (algorithmEnum) {
            case _1:
                cmdContent = cmdContent.replace(ProtocolParamEnum.CHECK_SUM.getDictValue(), "");
                return Crc16m.getBufHexStr(Crc16m.getSendBuf(cmdContent));
            case _3:
                cmdContent = cmdContent.replace(ProtocolParamEnum.CHECK_SUM.getDictValue(), "");
                return cmdContent + CheckCode.check256(cmdContent);
            default:
                return cmdContent;
        }
    }

    /**
     * 设备信息校验
     */
    private void deviceValid(Config config) {
        // 蓄电池需要传组信息
        if (Objects.equals(config.getType(), DeviceTypeEnum._1.getDictValue())) {
            if (!config.getPackList().isEmpty()) {
                List<Integer> packNumList = new ArrayList<>();
                for (BatteryPack batteryPack : config.getPackList()) {
                    if (!packNumList.contains(batteryPack.getPackNum())) {
                        packNumList.add(batteryPack.getPackNum());
                    } else {
                        throw new ServiceException(String.format("电池组序号 %s 重复", batteryPack.getPackNum()));
                    }
                }
            }
        }

        if (config.getPort() == null) {
            throw new ServiceException("串口号不可为空！");
        }

        int type = 1;
        // 如果为模拟量、开关量，只区分串口号，通道号设置100用于区分设备
        if (Objects.equals(config.getPortType(), PortTypeEnum._1.getDictValue())
                || Objects.equals(config.getPortType(), PortTypeEnum._2.getDictValue())) {
            if (config.getChannel() == null) {
                throw new ServiceException("通道号不可为空！");
            }
        } else {
            config.setChannel(1);
            type = 2;
        }
    }
}
