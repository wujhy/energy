package com.shanhe.project.energy.stat.service;


import com.shanhe.project.energy.stat.domain.StatBatteryBat;

import java.util.Date;
import java.util.List;


/**
 * 单体电池统计Service接口
 *
 * @author zhoubin
 * @date 2025-07-15
 */
public interface IStatBatteryBatService {
    /**
     * 查询单体电池统计列表
     *
     * @return 单体电池统计集合
     */
    List<StatBatteryBat> selectList(List<Long> packIds, Date startDateTime, Date endDateTime);

    /**
     * 查询电池组统计列表
     *
     */
    List<StatBatteryBat> selectList(StatBatteryBat statBatteryBat);


    /**
     * 新增单体电池统计
     *
     */
    void insertList(List<StatBatteryBat> statBatterys);

    /**
     * 删除单体电池统计
     *
     */
    void deleteByConfigId(Long configId, Integer packNum);
}
