package com.shanhe.project.energy.stat.service.impl;


import cn.hutool.core.collection.CollectionUtil;
import com.shanhe.common.constant.Constants;
import com.shanhe.project.energy.stat.domain.StatBatteryBat;
import com.shanhe.project.energy.stat.mapper.StatBatteryBatMapper;
import com.shanhe.project.energy.stat.service.IStatBatteryBatService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * 单体电池统计Service业务层处理
 *
 * @author wjh
 * @since 2026-05-25
 */
@Service
public class StatBatteryBatServiceImpl implements IStatBatteryBatService {

    @Resource
    private StatBatteryBatMapper statBatteryBatMapper;

    /**
     * 根据电池组ID列表和时间范围查询单体统计
     *
     * @param packIds 电池组ID列表
     * @param startDateTime 开始时间
     * @param endDateTime 结束时间
     * @return 单体统计数据列表
     */
    @Override
    public List<StatBatteryBat> selectList(List<Long> packIds, Date startDateTime, Date endDateTime) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String startDateTimeStr = startDateTime != null ? sdf.format(startDateTime) : null;
        String endDateTimeStr = endDateTime != null ? sdf.format(endDateTime) : null;

        return statBatteryBatMapper.selectListByPackIds(packIds, startDateTimeStr, endDateTimeStr);
    }

    /**
     * 根据条件查询单体统计数据
     *
     * @param params 查询参数
     * @return 单体统计数据列表
     */
    @Override
    public List<StatBatteryBat> selectList(StatBatteryBat params) {
        params.setConfigId(Constants.DEFAULT_CONFIG_ID);
        return statBatteryBatMapper.selectList(params);
    }

    /**
     * 批量插入单体统计数据
     *
     * @param statBatteryList 单体统计数据列表
     */
    @Override
    public void insertList(List<StatBatteryBat> statBatteryList) {
        if (CollectionUtil.isEmpty(statBatteryList)) {
            return;
        }
        statBatteryBatMapper.insertList(statBatteryList);
    }

    /**
     * 根据电池组编号删除单体统计数据
     *
     * @param packNum 电池组编号
     */
    @Override
    public void deleteByPackNum(Integer packNum) {
        statBatteryBatMapper.deleteByPackNum(packNum);
    }

}
