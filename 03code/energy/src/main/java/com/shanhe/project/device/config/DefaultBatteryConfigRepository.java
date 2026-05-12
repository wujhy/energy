package com.shanhe.project.device.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import cn.hutool.core.bean.BeanUtil;
import com.alibaba.fastjson.JSON;
import com.shanhe.common.constant.Constants;
import com.shanhe.framework.enums.DeviceTypeEnum;
import com.shanhe.framework.enums.YesNoEnum;
import com.shanhe.project.device.config.domain.Config;

/**
 * 默认蓄电池配置静态资源。
 *
 * @author wjh
 * @since 2026-05-08
 */
public final class DefaultBatteryConfigRepository
{
    private static final Config DEFAULT_CONFIG = buildDefaultConfig();

    private DefaultBatteryConfigRepository()
    {
    }

    /**
     * 按默认配置ID查询蓄电池配置。
     *
     * @param configId 配置ID
     * @return 蓄电池配置
     */
    public static Config selectByConfigId(Long configId)
    {
        if (configId != null && !Objects.equals(configId, Constants.DEFAULT_CONFIG_ID)) {
            return null;
        }
        return copy();
    }

    /**
     * 按设备定位条件查询蓄电池配置。
     *
     * @param type 设备类型
     * @param port 串口
     * @param channel 通道
     * @return 蓄电池配置
     */
    public static Config selectBy(Integer type, Integer port, Integer channel)
    {
        Config config = DEFAULT_CONFIG;
        if (type != null && !Objects.equals(type, config.getType())) {
            return null;
        }
        if (port != null && !Objects.equals(port, config.getPort())) {
            return null;
        }
        if (channel != null && !Objects.equals(channel, config.getChannel())) {
            return null;
        }
        return copy();
    }

    /**
     * 查询蓄电池配置列表。
     *
     * @param query 查询条件
     * @return 配置集合
     */
    public static List<Config> selectList(Config query)
    {
        List<Config> list = new ArrayList<>();
        Config config = DEFAULT_CONFIG;
        if (query != null) {
            if (query.getConfigId() != null && !Objects.equals(query.getConfigId(), config.getConfigId())) {
                return list;
            }
            if (query.getTmplId() != null && !Objects.equals(query.getTmplId(), config.getTmplId())) {
                return list;
            }
            if (query.getType() != null && !Objects.equals(query.getType(), config.getType())) {
                return list;
            }
            if (query.getPortType() != null && !Objects.equals(query.getPortType(), config.getPortType())) {
                return list;
            }
            if (query.getPort() != null && !Objects.equals(query.getPort(), config.getPort())) {
                return list;
            }
            if (query.getChannel() != null && !Objects.equals(query.getChannel(), config.getChannel())) {
                return list;
            }
            if (query.getStatus() != null && !Objects.equals(query.getStatus(), config.getStatus())) {
                return list;
            }
        }
        list.add(copy());
        return list;
    }

    /**
     * 更新默认配置启用状态。
     *
     * @param status 启用状态
     */
    public static void updateStatus(Integer status)
    {
        DEFAULT_CONFIG.setStatus(status);
    }

    /**
     * 更新默认配置在线状态。
     *
     * @param online 在线状态
     */
    public static void updateOnline(Integer online)
    {
        DEFAULT_CONFIG.setOnline(online);
    }

    /**
     * 更新默认配置扩展字段。
     *
     * @param map 扩展字段
     */
    public static void updateExtend(Map<String, Object> map)
    {
        DEFAULT_CONFIG.setExtend3(JSON.toJSONString(map));
    }

    private static Config copy()
    {
        return BeanUtil.copyProperties(DEFAULT_CONFIG, Config.class);
    }

    private static Config buildDefaultConfig()
    {
        Config config = new Config();
        config.setConfigId(Constants.DEFAULT_CONFIG_ID);
        config.setTmplId(Constants.DEFAULT_TEMPLATE_ID);
        config.setName("\u84c4\u7535\u6c60");
        config.setType(DeviceTypeEnum._1.getDictValue());
        config.setSubType("0");
        config.setSort(1);
        config.setPort(10);
        config.setPortType(1);
        config.setChannel(1);
        config.setBaudRate(115200);
        config.setDataBits(3);
        config.setStopBits(0);
        config.setIntervalTime(5000);
        config.setParityBits(0);
        config.setStatus(YesNoEnum.YES.getDictValue());
        config.setOnline(YesNoEnum.NO.getDictValue());
        return config;
    }
}
