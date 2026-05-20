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
 * @author zhoubin
 * @date 2025-07-15
 */
@Service
public class StatBatteryBatServiceImpl implements IStatBatteryBatService {

    @Resource
    private StatBatteryBatMapper statBatteryBatMapper;

    @Override
    public List<StatBatteryBat> selectList(List<Long> packIds, Date startDateTime, Date endDateTime) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String startDateTimeStr = startDateTime != null ? sdf.format(startDateTime) : null;
        String endDateTimeStr = endDateTime != null ? sdf.format(endDateTime) : null;

        return statBatteryBatMapper.selectListByPackIds(packIds, startDateTimeStr, endDateTimeStr);
    }

    @Override
    public List<StatBatteryBat> selectList(StatBatteryBat params) {
        params.setConfigId(Constants.DEFAULT_CONFIG_ID);
        return statBatteryBatMapper.selectList(params);
    }

    @Override
    public void insertList(List<StatBatteryBat> statBatteryList) {
        if (CollectionUtil.isEmpty(statBatteryList)) {
            return;
        }
        statBatteryBatMapper.insertList(statBatteryList);
    }

    @Override
    public void deleteByPackNum(Integer packNum) {
        statBatteryBatMapper.deleteByConfigId(Constants.DEFAULT_CONFIG_ID, packNum);
    }

}
