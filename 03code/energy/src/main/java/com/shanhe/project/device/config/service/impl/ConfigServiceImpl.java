package com.shanhe.project.device.config.service.impl;

import java.util.*;
import java.util.stream.Collectors;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.shanhe.common.exception.ServiceException;
import com.shanhe.common.utils.Crc16m;
import com.shanhe.common.utils.CacheUtils;
import com.shanhe.common.utils.CheckCode;
import com.shanhe.common.utils.StringUtils;
import com.shanhe.common.utils.uuid.IdUtils;
import com.shanhe.framework.comm.CommServer;
import com.shanhe.framework.enums.*;
import com.shanhe.framework.manager.AsyncTaskManager;
import com.shanhe.framework.comm.tcp.utils.CodingUtil;
import com.shanhe.project.device.alarm.domain.AlarmLog;
import com.shanhe.project.device.alarm.service.IAlarmLogService;
import com.shanhe.project.device.config.domain.*;
import com.shanhe.project.device.config.service.IBatteryPackService;
import com.shanhe.project.device.config.service.IConfigAttributeService;
import com.shanhe.project.device.config.service.IConfigProtocolService;
import com.shanhe.project.device.opt.service.ControlBattery;
import com.shanhe.project.device.opt.service.ControlBatterySet;
import com.shanhe.project.device.opt.service.DeviceCmdService;
import com.shanhe.project.device.opt.service.OptLogService;
import com.shanhe.project.device.opt.vo.BatterySetVO;
import com.shanhe.project.device.template.domain.Template;
import com.shanhe.project.device.template.domain.TemplateAttribute;
import com.shanhe.project.device.template.mapper.TemplateAttributeMapper;
import com.shanhe.project.device.template.mapper.TemplateMapper;
import com.shanhe.project.sync.service.ClientReportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import com.shanhe.project.device.config.mapper.ConfigMapper;
import com.shanhe.project.device.config.service.IConfigService;
import com.shanhe.common.utils.text.Convert;
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
    private ConfigMapper configMapper;
    @Resource
    private IConfigAttributeService configAttributeService;
    @Resource
    private IConfigProtocolService configProtocolService;
    @Resource
    private TemplateMapper templateMapper;
    @Resource
    private TemplateAttributeMapper templateAttributeMapper;
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
    private ControlBatterySet controlBatterySet;
    @Resource
    private ClientReportService clientReportService;

    // 缓存枚举
    CacheKeyEnum configCache = CacheKeyEnum.CONFIG;

    @Override
    public Config selectConfigBy(Integer type, Integer port, Integer channel) {
        return configMapper.selectConfigBy(type, port, channel);
    }

    /**
     * 查询设备
     *
     * @param configId 设备主键
     * @return 设备
     */
    @Override
    public Config selectConfigByConfigId(Long configId) {
        Config config = configMapper.selectConfigByConfigId(configId);
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
        List<Config> configs = configMapper.selectConfigList(config);
        configs.forEach(item -> {
            if (Objects.equals(item.getType(), DeviceTypeEnum._1.getDictValue())) {
                item.setPackList(batteryPackService.selectBatteryPackListConfigId(item.getConfigId(), null));
            }
        });
        return configs;
    }

    @Override
    public List<Config> reportConfigList() {
        List<Config> list = configMapper.selectConfigList(new Config());
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
        List<Config> list = configMapper.selectConfigList(config);
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
        if (list == null || list.isEmpty()) {
            return;
        }

        for (Config importConfig : list) {
            // 配置
            importConfig.setConfigId(IdUtils.getSnowflakeId());
            configMapper.insertConfig(importConfig);

            // 电池组
            if (StringUtils.isNotBlank(importConfig.getPackListJson())) {
                List<BatteryPack> packList = JSON.parseArray(importConfig.getPackListJson(), BatteryPack.class);
                packList.forEach(entity -> {
                    entity.setConfigId(importConfig.getConfigId());
                    entity.setPackId(IdUtils.getSnowflakeId());
                });
                batteryPackService.importBatteryPack(packList);
            }

            // 属性
            if (StringUtils.isNotBlank(importConfig.getAttrListJson())) {
                List<ConfigAttribute> attributeList = JSON.parseArray(importConfig.getAttrListJson(), ConfigAttribute.class);
                attributeList.forEach(entity -> {
                    entity.setConfigId(importConfig.getConfigId());
                    entity.setConfigAttrId(IdUtils.getSnowflakeId());
                });
                configAttributeService.importAttribute(attributeList);
            }

            // 协议
            if (StringUtils.isNotBlank(importConfig.getProtocolListJson())) {
                List<ConfigProtocol> protocolList = JSON.parseArray(importConfig.getProtocolListJson(), ConfigProtocol.class);
                for (ConfigProtocol protocol : protocolList) {
                    protocol.setConfigId(importConfig.getConfigId());
                    configProtocolService.insertConfigProtocol(protocol);
                }
            }
        }
    }

    @Override
    public void insertConfig(Config config) {
        //校验
        this.deviceValid(config);
        //类型
        Template template = templateMapper.selectTemplateByTmplId(config.getTmplId());
        if (template == null) {
            throw new ServiceException(String.format("%1$s模板不存在！", config.getTmplId()));
        }
        config.setType(template.getType());
        config.setSubType(template.getSubType());
        config.setTypeCode(template.getTypeCode());
        //id
        config.setConfigId(IdUtils.getSnowflakeId());
        config.setOnline(YesNoEnum.NO.getDictValue());
        config.setStatus(YesNoEnum.NO.getDictValue());
        configMapper.insertConfig(config);
        // 蓄电池
        if (Objects.equals(config.getType(), DeviceTypeEnum._1.getDictValue())) {
            // 插入蓄电池组
            this.insertPack(config);
        } else {
            // 同步属性
            this.insertAttribute(config, null, null);
            // 同步协议
            this.insertProtocol(config);
        }
        // 下发指令
        this.sendCmdAsync(config.getConfigId());
    }

    @Override
    public void insertConfigBySync(Config config) {
        config.setOnline(YesNoEnum.NO.getDictValue());
        configMapper.insertConfig(config);
        // 蓄电池
        if (Objects.equals(config.getType(), DeviceTypeEnum._1.getDictValue())
                && config.getPackList() != null) {
            // 插入蓄电池组
            for (BatteryPack batteryPack : config.getPackList()) {
                batteryPack.setConfigId(config.getConfigId());
                batteryPack.setPackId(IdUtils.getSnowflakeId());
                batteryPackService.insertBatteryPack(batteryPack);
                // 属性
                this.insertAttribute(config, batteryPack.getPackNum(), batteryPack.getBatSinModel());
            }
        }
        // 下发指令
        this.sendCmdAsync(config.getConfigId());
    }

    /**
     * 修改设备
     *
     * @param config 设备
     * @return 结果
     */
    @Override
    public int updateConfig(Config config) {
        // 校验参数
        this.deviceValid(config);

        // 更新包
        this.updatePack(config);

        // 保存设备信息
        configMapper.updateConfig(config);

        // 下发指令
        this.sendCmdAsync(config.getConfigId());
        return 1;
    }

    @Override
    public void updateConfigBySync(Config config) {
        // 更新包
        this.updatePack(config, true);

        // 保存设备信息
        configMapper.updateConfig(config);

        // 下发指令
        this.sendCmdAsync(config.getConfigId());
    }

    @Override
    public void updatePost(Config config) {
        Config query = new Config();
        query.setType(config.getType());
        query.setPort(config.getPort());
        query.setChannel(config.getChannel());
        List<Config> oldConfigList = configMapper.selectConfigList(query);
        if (oldConfigList == null || oldConfigList.isEmpty()) {
            return;
        }

        // 保存设备信息
        config.setConfigId(oldConfigList.get(0).getConfigId());
        configMapper.updatePost(config);
    }

    @Override
    public int updateStatus(Long configId, Integer status) {
        Config config = this.selectConfigByConfigId(configId);
        if (config == null) {
            throw new ServiceException("设备不存在");
        }
        // 更新设备状态
        config.setStatus(status);
        configMapper.updateConfig(config);

        // 下发串口配置指令，更新缓存
        this.sendCmd(config);

        return 1;
    }

    /**
     * 异步执行下发指令
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

    /**
     * 下发串口指令
     *
     * @param config 配置
     */
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
        String[] configIdArr = Convert.toStrArray(configIds);
        if (configIdArr.length == 0) {
            throw new ServiceException("设备ID不可为空");
        }
        // 删除包
        batteryPackService.deleteByConfigIds(configIdArr);
        // 删除属性
        configAttributeService.deleteConfigAttributeByConfigIds(configIdArr);
        // 删除协议
        configProtocolService.deleteConfigProtocolByConfigIds(configIds);
        // 删除告警
        alarmLogService.deleteAlarmLogByConfigIds(configIdArr);
        // 删除操作日志
        optLogService.deleteByConfigIds(configIdArr);
        // 删除设备
        configMapper.deleteConfigByConfigIds(configIdArr);
        // 更新缓存
        this.updateCache();
        return 1;
    }

    @Override
    public void deleteConfig(Config config) {
        // 通道开启且设备删除前为开启状态，需要删除缓存及设备协议
        if (CommServer.isOpen() && Objects.equals(config.getStatus(), YesNoEnum.YES.getDictValue())) {
            // 下发删除指令（通过缓存获取设备）
            configProtocolService.cmdStorageDelByConfig(config);
        }
        String[] configIdArr = Convert.toStrArray(String.valueOf(config.getConfigId()));
        // 删除包
        batteryPackService.deleteByConfigIds(configIdArr);
        // 删除属性
        configAttributeService.deleteConfigAttributeByConfigIds(configIdArr);
        // 删除协议
        configProtocolService.deleteConfigProtocolByConfigIds(String.valueOf(config.getConfigId()));
        // 删除告警
        alarmLogService.deleteAlarmLogByConfigIds(configIdArr);
        // 删除操作日志
        optLogService.deleteByConfigIds(configIdArr);
        // 删除设备
        configMapper.deleteConfigByConfigIds(configIdArr);
        // 更新缓存
        this.updateCache();
    }

    @Override
    public void updateCache() {
        try {
            // 配置键
            List<String> startKeys = new ArrayList<>();
            Set<String> oldKeys = CacheUtils.getCacheKeys(configCache.getCache());

            // 查所有启用的配置（缓存全部，是否启用通过使用端判断）
            List<Config> list = configMapper.selectConfigList(new Config());
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
        configMapper.online(config.getConfigId());
        // 更新缓存
        this.updateCache(config);
        // 更新告警
        this.updateTxAlarm(config);
    }

    @Override
    public void offline(Config config) {
        if (Objects.equals(config.getOnline(), YesNoEnum.YES.getDictValue())) {
            config.setOnline(YesNoEnum.NO.getDictValue());
            // 设备下线
            configMapper.offline(config.getConfigId());
            // 更新缓存
            this.updateCache(config);
        }
        // 更新告警
        this.updateTxAlarm(config);
    }

    @Override
    public void offlineAll() {
        // 所有设备下线
        configMapper.offline(null);

        // 更新缓存
        this.updateCache();
    }

    @Override
    public void updateExtend(Long configId, Map<String, Object> map) {
        Config config = configMapper.selectConfigByConfigId(configId);
        if (config == null) {
            return;
        }
        Map<String, Object> mapAll = StrUtil.isNotBlank(config.getExtend3()) ? JSON.parseObject(config.getExtend3()) : new HashMap<>();
        mapAll.putAll(map);
        config.setExtend3(JSON.toJSONString(mapAll));
        configMapper.updateConfig(config);
    }

    @Override
    public Map<String, Object> getExtend(Long configId) {
        Config config = configMapper.selectConfigByConfigId(configId);
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
        // 所有缓存的已开启设备
        List<Config> configList = configMapper.selectConfigList(new Config());
        if (configList.isEmpty()) {
            return;
        }
        try {
            // 通道开启且设备删除前为开启状态，需要删除缓存及设备协议
            if (CommServer.isOpen()) {
                for (Config config : configList) {
                    // 蓄电池，下发出厂指令
                    if (Objects.equals(config.getStatus(), YesNoEnum.YES.getDictValue())
                            && Objects.equals(config.getType(), DeviceTypeEnum._1.getDictValue())) {
                        BatterySetVO batterySetVO = new BatterySetVO();
                        batterySetVO.setConfigId(config.getConfigId());
                        batterySetVO.setNeedDynResult(false);
                        controlBatterySet.doSet(batterySetVO, BatteryCidEnum._75);
                    }
                }
                // 删除所有存储指令
                deviceCmdService.cmdDelAll();
            }
        } catch (Exception ignored) { }

        String[] configIdArr = configList.stream().map(Config::getConfigId).map(String::valueOf).toArray(String[]::new);
        // 删除包
        batteryPackService.deleteByConfigIds(configIdArr);
        // 删除属性
        configAttributeService.deleteConfigAttributeByConfigIds(configIdArr);
        // 删除协议
        configProtocolService.deleteConfigProtocolByConfigIds(configIdArr);
        // 删除告警
        alarmLogService.deleteAlarmLogByConfigIds(configIdArr);
        // 删除操作日志
        optLogService.deleteByConfigIds(configIdArr);
        // 删除设备
        configMapper.deleteConfigByConfigIds(configIdArr);
        // 更新缓存
        this.updateCache();
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
        if (config.getTmplId() == null) {
            return;
        }
        // 同步创建属性
        TemplateAttribute query = new TemplateAttribute();
        query.setTmplId(config.getTmplId());
        query.setModel(model);
        List<TemplateAttribute> templateAttributeList = templateAttributeMapper.selectTemplateAttributeList(query);
        if (templateAttributeList == null || templateAttributeList.isEmpty()) {
            return;
        }

        List<ConfigAttribute> configAttributeList = new ArrayList<>(templateAttributeList.size());
        for (TemplateAttribute templateAttribute : templateAttributeList) {
            ConfigAttribute configAttribute = BeanUtil.copyProperties(templateAttribute, ConfigAttribute.class);
            configAttribute.setConfigAttrId(IdUtils.getSnowflakeId());
            configAttribute.setConfigId(config.getConfigId());
            configAttribute.setPackNum(packNum);
            configAttributeList.add(configAttribute);
        }
        configAttributeService.insertBatchConfigAttribute(configAttributeList);
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

        // 重名校验（校验通道号及串口号）
        Long hasName = configMapper.hasName(config.getConfigId(), config.getPort(), config.getChannel(), type);
        if (hasName > 0) {
            throw new ServiceException("串口号、通道号重复，操作失败！");
        }
    }


}
