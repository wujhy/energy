package com.shanhe.project.manage.capacity.service.impl;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import com.shanhe.common.constant.Constants;
import com.shanhe.common.utils.CacheUtils;
import com.shanhe.common.utils.uuid.IdUtils;
import com.shanhe.framework.enums.CacheKeyEnum;
import com.shanhe.project.manage.config.domain.BatteryPack;
import com.shanhe.project.manage.config.mapper.BatteryPackMapper;
import com.shanhe.project.manage.capacity.mapper.PreBatteryGroupMapper;
import com.shanhe.project.manage.capacity.service.PreBatteryGroupService;
import com.shanhe.project.manage.capacity.vo.PreBatteryGroup;
import com.shanhe.project.manage.capacity.vo.PreBatteryVo;
import com.shanhe.project.sync.service.ClientReportService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 预测电池组服务实现类
 *
 * @author wjh
 * @since 2026-05-25
 */
@Service
public class PreBatteryGroupServiceImpl implements PreBatteryGroupService {

    /** 预估电池组映射。 */
    @Resource
    private PreBatteryGroupMapper preBatteryGroupMapper;
    /** 客户端上报服务。 */
    @Resource
    private ClientReportService clientReportService;
    /** 电池组映射。 */
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
        String key = String.format(cache.getKey(), groupVo.getPackNum());
        CacheUtils.put(cache.getCache(), key, groupVo);

        clientReportService.uploadPreBatteryGroup(groupVo);
    }

    /**
     * 获取电池组最新预测缓存
     *
     * @param packNum 电池组编号
     * @return 预测电池组对象
     */
    @Override
    public PreBatteryGroup lastCache(Integer packNum) {
        String key = String.format(cache.getKey(), packNum);
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

    /**
     * 根据电池组编号删除预测数据
     *
     * @param packNum 电池组编号
     */
    @Override
    public void deleteByPackNum(Integer packNum) {
        preBatteryGroupMapper.deleteByPackNum(packNum);
    }

    /**
     * 更新预测电池组缓存
     */
    @Override
    public void updateCache() {
        // 旧缓存
        List<String> startKeys = new ArrayList<>();
        Set<String> oldKeys = CacheUtils.getCacheKeys(cache.getCache());

        // 蓄电池组
        List<BatteryPack> batteryPackList = batteryPackMapper.selectAllBattery();
        for (BatteryPack batteryPack : batteryPackList) {
            // 查询最新一条记录
            PreBatteryGroup reportLog = preBatteryGroupMapper.selectLast(batteryPack.getPackNum());
            if (reportLog == null) {
                continue;
            }

            /* 缓存 */
            String key = String.format(cache.getKey(), reportLog.getPackNum());
            CacheUtils.put(cache.getCache(), key, reportLog);
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
