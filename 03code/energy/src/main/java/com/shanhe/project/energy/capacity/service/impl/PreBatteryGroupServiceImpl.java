package com.shanhe.project.energy.capacity.service.impl;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import com.shanhe.common.constant.Constants;
import com.shanhe.common.utils.CacheUtils;
import com.shanhe.common.utils.uuid.IdUtils;
import com.shanhe.framework.enums.CacheKeyEnum;
import com.shanhe.project.device.config.domain.BatteryPack;
import com.shanhe.project.device.config.mapper.BatteryPackMapper;
import com.shanhe.project.energy.capacity.mapper.PreBatteryGroupMapper;
import com.shanhe.project.energy.capacity.service.PreBatteryGroupService;
import com.shanhe.project.energy.capacity.vo.PreBatteryGroup;
import com.shanhe.project.energy.capacity.vo.PreBatteryVo;
import com.shanhe.project.sync.service.ClientReportService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author zhoubin
 * @date 2025/10/10
 */
@Service
public class PreBatteryGroupServiceImpl implements PreBatteryGroupService {

    @Resource
    private PreBatteryGroupMapper preBatteryGroupMapper;
    @Resource
    private ClientReportService clientReportService;
    @Resource
    private BatteryPackMapper batteryPackMapper;

    CacheKeyEnum cache = CacheKeyEnum.STAT_BATTERY_GROUP;

    /**
     * 初始化 预测电池组对象
     */
    @Override
    public void insert(PreBatteryGroup groupVo) {
        groupVo.setConfigId(Constants.DEFAULT_CONFIG_ID);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        groupVo.setId(IdUtils.getSnowflakeId());
        groupVo.setStaticTimeStr(sdf.format(groupVo.getStaticTime()));
        groupVo.setStartTimeStr(sdf.format(groupVo.getStartTime()));
        groupVo.setEndTimeStr(sdf.format(groupVo.getEndTime()));
        preBatteryGroupMapper.insert(groupVo);
        String key = String.format(cache.getKey(), groupVo.getConfigId(), groupVo.getPackNum());
        CacheUtils.put(cache.getCache(), key, groupVo);

        clientReportService.uploadPreBatteryGroup(groupVo);
    }

    @Override
    public PreBatteryGroup lastCache(Integer packNum) {
        Long configId = Constants.DEFAULT_CONFIG_ID;
        String key = String.format(cache.getKey(), configId, packNum);
        Object log = CacheUtils.get(cache.getCache(), key);
        if (log == null) {
            return null;
        }
        PreBatteryGroup result = (PreBatteryGroup) log;

        // 包数据
        if (StrUtil.isNotBlank(result.getMapBatteryData())) {
            result.setMapBattery(JSON.parseObject(result.getMapBatteryData(), new TypeReference<Map<String, PreBatteryVo>>() {
            }));
        }
        return result;
    }

    @Override
    public void deleteByConfigId(Integer packNum) {
        Long configId = Constants.DEFAULT_CONFIG_ID;
        preBatteryGroupMapper.deleteByConfigId(configId, packNum);
    }

    @Override
    public void updateCache() {
        // 旧缓存
        List<String> startKeys = new ArrayList<>();
        Set<String> oldKeys = CacheUtils.getCacheKeys(cache.getCache());

        // 蓄电池组
        List<BatteryPack> batteryPackList = batteryPackMapper.selectAllBattery();
        for (BatteryPack batteryPack : batteryPackList) {
            // 查询最新一条记录
            PreBatteryGroup reportLog = preBatteryGroupMapper.selectLast(Constants.DEFAULT_CONFIG_ID, batteryPack.getPackNum());
            if (reportLog == null) {
                continue;
            }

            /* 缓存 */
            String key = String.format(cache.getKey(), Constants.DEFAULT_CONFIG_ID, reportLog.getPackNum());
            if (CacheUtils.get(cache.getCache(), key) == null) {
                CacheUtils.put(cache.getCache(), key, reportLog);
            }
            startKeys.add(key);
        }

        // 删除
        for (String key : oldKeys) {
            if (!startKeys.contains(key)) {
                CacheUtils.remove(cache.getCache(), key);
            }
        }
    }
}
