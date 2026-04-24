package com.shanhe.project.device.config.service.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import cn.hutool.core.util.StrUtil;
import com.shanhe.common.exception.ServiceException;
import com.shanhe.common.utils.CacheUtils;
import com.shanhe.common.utils.uuid.IdUtils;
import com.shanhe.framework.enums.CacheKeyEnum;
import com.shanhe.framework.enums.DeviceTypeEnum;
import com.shanhe.framework.enums.YesNoEnum;
import com.shanhe.framework.comm.tcp.utils.CodingUtil;
import com.shanhe.project.device.config.domain.Config;
import com.shanhe.project.device.config.domain.ConfigProtocolAttribute;
import com.shanhe.project.device.config.mapper.ConfigProtocolAttributeMapper;
import com.shanhe.project.device.opt.cmd.CmdBatteryService;
import com.shanhe.project.device.opt.service.DeviceCmdService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import com.shanhe.project.device.config.mapper.ConfigProtocolMapper;
import com.shanhe.project.device.config.domain.ConfigProtocol;
import com.shanhe.project.device.config.service.IConfigProtocolService;
import com.shanhe.common.utils.text.Convert;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

/**
 * 设备协议Service业务层处理
 *
 * @author wjh
 * @since 2024-12-23
 */
@Service
public class ConfigProtocolServiceImpl implements IConfigProtocolService
{
    protected static Logger logger = LoggerFactory.getLogger(IConfigProtocolService.class);
    @Resource
    private ConfigProtocolMapper configProtocolMapper;
    @Resource
    private ConfigProtocolAttributeMapper configProtocolAttributeMapper;
    @Resource
    private DeviceCmdService deviceCmdService;
    @Resource
    private CmdBatteryService cmdBatteryService;

    CacheKeyEnum protocolCache = CacheKeyEnum.PROTOCOL;

    @Override
    public ConfigProtocol findByProtocolId(Long protocolId) {
        return configProtocolMapper.selectConfigProtocolByProtocolId(protocolId);
    }

    /**
     * 查询设备协议
     *
     * @param protocolId 设备协议主键
     * @return 设备协议
     */
    @Override
    public ConfigProtocol selectConfigProtocolByProtocolId(Long protocolId)
    {
        ConfigProtocol configProtocol = this.getConfigProtocol(protocolId);
        // 映射
        configProtocol.setAttributeList(configProtocolAttributeMapper.selectConfigProtocolAttributeByProtocolId(configProtocol.getProtocolId()));
        return configProtocol;
    }

    @Override
    public ConfigProtocol selectBy(Long configId, String protocolCode) {
        return configProtocolMapper.selectBy(configId, protocolCode);
    }

    /**
     * 查询设备协议列表
     *
     * @param configProtocol 设备协议
     * @return 设备协议
     */
    @Override
    public List<ConfigProtocol> selectConfigProtocolList(ConfigProtocol configProtocol)
    {
        return configProtocolMapper.selectConfigProtocolList(configProtocol);
    }

    @Override
    public List<ConfigProtocol> exportByConfigId(Long configId) {
        List<ConfigProtocol> list = configProtocolMapper.selectByConfigId(configId);
        for (ConfigProtocol protocol : list) {
            protocol.setCreateTime(null);
            protocol.setAttributeList(configProtocolAttributeMapper.selectConfigProtocolAttributeByProtocolId(protocol.getProtocolId()));
        }
        return list;
    }

    @Override
    public void cmdStorageSendByConfig(Config config) {
        if (config == null) {
            return;
        }

        // 如果是蓄电池，不走协议配置，通过内置处理（后续扩展config.getProtocolType()即可）
        if (Objects.equals(config.getType(), DeviceTypeEnum._1.getDictValue())) {
            // 存储指令
            cmdBatteryService.sendCmd52(config);
            return;
        }

        List<ConfigProtocol> configProtocolList = this.findOpenList(config.getConfigId());
        if (configProtocolList.isEmpty()) {
            logger.debug("同步协议指令，设备 {} 无启用协议", config.getName());
            return;
        }

        for (ConfigProtocol protocol : configProtocolList) {
            deviceCmdService.cmd(config, protocol);
        }
    }

    @Override
    public void cmdStorageDelByConfig(Config config) {
        if (config == null) {
            return;
        }
        // 如果是蓄电池，不走协议配置，通过内置处理（后续扩展config.getProtocolType()即可）
        if (Objects.equals(config.getType(), DeviceTypeEnum._1.getDictValue())) {
            // 存储指令
            cmdBatteryService.sendCmd58(config);
            return;
        }

        List<ConfigProtocol> configProtocolList = this.findOpenList(config.getConfigId());
        if (configProtocolList.isEmpty()) {
            logger.debug("同步协议指令，设备 {} 无启用协议", config.getName());
            return;
        }

        for (ConfigProtocol protocol : configProtocolList) {
            deviceCmdService.cmdDel(config, protocol.getProtocolCode());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int insertConfigProtocol(ConfigProtocol configProtocol)
    {
        // 生成协议编码
        ConfigProtocol query = new ConfigProtocol();
        query.setTemplate(configProtocol.getTemplate());
        List<ConfigProtocol> protocolList = configProtocolMapper.selectConfigProtocolList(query);
        List<String> oldCodes = protocolList.stream().map(ConfigProtocol::getProtocolCode).collect(Collectors.toList());
        configProtocol.setProtocolCode(CodingUtil.generateHexCode(oldCodes, 2));
        // 生成协议ID
        configProtocol.setProtocolId(IdUtils.getSnowflakeId());
        // 创建映射
        if (configProtocol.getAttributeList() != null && !configProtocol.getAttributeList().isEmpty()) {
            List<ConfigProtocolAttribute> list = configProtocol.getAttributeList();
            list.forEach(entity -> entity.setProtocolId(configProtocol.getProtocolId()));
            configProtocolAttributeMapper.insertList(list);
        }
        return configProtocolMapper.insertConfigProtocol(configProtocol);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void insertBySync(Config config, ConfigProtocol configProtocol) {
        // 创建映射
        if (configProtocol.getAttributeList() != null && !configProtocol.getAttributeList().isEmpty()) {
            configProtocolAttributeMapper.insertList(configProtocol.getAttributeList());
        }
        // 保存记录
        configProtocolMapper.insertConfigProtocol(configProtocol);

        // 协议是否开启
        if (!Objects.equals(configProtocol.getStatus(), YesNoEnum.YES.getDictValue())) {
            return;
        }

        // 如果是蓄电池，不走协议配置，通过内置处理（后续扩展config.getProtocolType()即可）
        if (Objects.equals(config.getType(), DeviceTypeEnum._1.getDictValue())) {
            return;
        }

        // 普通协议，更新缓存
        String key = String.format(protocolCache.getKey(), configProtocol.getConfigId(), configProtocol.getProtocolCode());
        CacheUtils.put(protocolCache.getCache(), key, configProtocol);

        // 下发指令
        deviceCmdService.cmd(config, configProtocol);
    }

    /**
     * 修改设备协议
     *
     * @param configProtocol 设备协议
     * @return 结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateConfigProtocol(ConfigProtocol configProtocol)
    {
        ConfigProtocol configProtocolOld = this.getConfigProtocol(configProtocol.getProtocolId());
        // 协议编码不同，校验
        if (!StrUtil.equals(configProtocolOld.getProtocolCode(), configProtocol.getProtocolCode())) {
            Long hasNum = configProtocolMapper.hasNum(configProtocol);
            if (hasNum > 0) {
                throw new ServiceException("协议编码重复！");
            }
        }
        // 先删除
        configProtocolAttributeMapper.deleteByProtocolId(configProtocolOld.getProtocolId());

        List<ConfigProtocolAttribute> list = configProtocol.getAttributeList();
        for (ConfigProtocolAttribute configProtocolAttribute : list) {
            configProtocolAttribute.setProtocolId(configProtocol.getProtocolId());
        }
        // 创建映射
        configProtocolAttributeMapper.insertList(list);
        return configProtocolMapper.updateConfigProtocol(configProtocol);
    }

    /**
     * 批量删除设备协议
     *
     * @param protocolIds 需要删除的设备协议主键
     * @return 结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteConfigProtocolByProtocolIds(String protocolIds)
    {
        if (StrUtil.isBlank(protocolIds)) {
            return 0;
        }

        // 删除协议
        configProtocolAttributeMapper.deleteByProtocolIds(Convert.toStrArray(protocolIds));
        configProtocolMapper.deleteConfigProtocolByProtocolIds(Convert.toStrArray(protocolIds));
        return 1;
    }

    @Override
    public void deleteBySync(Long protocolId) {
        // 删除协议
        configProtocolAttributeMapper.deleteByProtocolId(protocolId);
        configProtocolMapper.deleteConfigProtocolByProtocolId(protocolId);
    }

    @Override
    public void deleteConfigProtocolByConfigIds(String configIds) {
        if (StrUtil.isBlank(configIds)) {
            return;
        }
        String[] configIdArr = Convert.toStrArray(configIds);
        this.deleteConfigProtocolByConfigIds(configIdArr);
    }

    @Override
    public void deleteConfigProtocolByConfigIds(String[] configIdArr) {
        if (configIdArr == null) {
            return;
        }
        for (String configId : configIdArr) {
            List<ConfigProtocol> protocolList = configProtocolMapper.selectByConfigId(Long.valueOf(configId));
            if (protocolList.isEmpty()) {
                continue;
            }

            for (ConfigProtocol protocol : protocolList) {
                // 删除协议
                configProtocolAttributeMapper.deleteByProtocolId(protocol.getProtocolId());
                configProtocolMapper.deleteConfigProtocolByProtocolId(protocol.getProtocolId());
            }
        }

    }

    @Override
    public ConfigProtocol getCacheBy(Long configId, String code) {
        String key = String.format(protocolCache.getKey(), configId, code);
        return (ConfigProtocol) CacheUtils.get(protocolCache.getCache(), key);
    }

    @Override
    public void updateCache() {
        // 属性键
        List<String> startKeys = new ArrayList<>();
        Set<String> oldKeys = CacheUtils.getCacheKeys(protocolCache.getCache());

        // 所有启用的非模板协议
        List<ConfigProtocol> list = this.findOpenList(null);
        for (ConfigProtocol protocol : list) {
            // 补充协议属性映射
            protocol.setAttributeList(configProtocolAttributeMapper.selectConfigProtocolAttributeByProtocolId(protocol.getProtocolId()));
            // 缓存
            String key = String.format(protocolCache.getKey(), protocol.getConfigId(), protocol.getProtocolCode());
            CacheUtils.put(protocolCache.getCache(), key, protocol);
            startKeys.add(key);
        }

        // 删除
        for (String key : oldKeys) {
            if (!startKeys.contains(key)) {
                CacheUtils.remove(protocolCache.getCache(), key);
            }
        }
    }

    @Override
    public void updateCache(Long configId, Integer isUpdate) {
        List<ConfigProtocol> list = this.findOpenList(configId);
        for (ConfigProtocol protocol : list) {
            // 缓存
            String key = String.format(protocolCache.getKey(), protocol.getConfigId(), protocol.getProtocolCode());
            if (Objects.equals(isUpdate, YesNoEnum.YES.getDictValue())) {
                // 补充协议属性映射
                protocol.setAttributeList(configProtocolAttributeMapper.selectConfigProtocolAttributeByProtocolId(protocol.getProtocolId()));
                CacheUtils.put(protocolCache.getCache(), key, protocol);
            } else {
                CacheUtils.remove(protocolCache.getCache(), key);
            }
        }
    }

    /**
     * 获取协议
     */
    private ConfigProtocol getConfigProtocol(Long protocolId) {
        ConfigProtocol configProtocol = configProtocolMapper.selectConfigProtocolByProtocolId(protocolId);
        if (configProtocol == null || configProtocol.getProtocolId() == null) {
            throw new ServiceException(String.format("%1$s协议不存在！", protocolId));
        }
        return configProtocol;
    }

    /**
     * 设备下启用的可执行协议
     *
     * @param configId 设备ID
     * @return 列表
     */
    private List<ConfigProtocol> findOpenList(Long configId) {
        ConfigProtocol configProtocol = new ConfigProtocol();
        configProtocol.setStatus(YesNoEnum.YES.getDictValue());
        configProtocol.setTemplate(YesNoEnum.NO.getDictValue());
        // 未指定则全部
        configProtocol.setConfigId(configId);
        return configProtocolMapper.selectConfigProtocolList(configProtocol);
    }
}
