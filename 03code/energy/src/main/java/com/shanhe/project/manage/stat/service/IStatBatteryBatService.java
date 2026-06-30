package com.shanhe.project.manage.stat.service;


import com.shanhe.project.manage.stat.domain.StatBatteryBat;

import java.util.Date;
import java.util.List;


/**
 * 单体电池统计Service接口
 *
 * @author wjh
 * @since 2026-05-25
 */
public interface IStatBatteryBatService {
    /**
     * 查询单体电池统计列表
     *
     * @param packIds 电池组ID列表
     * @param startDateTime 开始时间
     * @param endDateTime 结束时间
     * @return 单体电池统计集合
     */
    List<StatBatteryBat> selectList(List<Long> packIds, Date startDateTime, Date endDateTime);

    /**
     * 查询电池组统计列表
     *
     * @param statBatteryBat 查询条件
     * @return 统计列表
     */
    List<StatBatteryBat> selectList(StatBatteryBat statBatteryBat);


    /**
     * 新增单体电池统计
     *
     * @param statBatterys 单体统计数据列表
     */
    void insertList(List<StatBatteryBat> statBatterys);

    /**
     * 删除单体电池统计
     *
     * @param packNum 电池组编号
     */
    void deleteByPackNum(Integer packNum);
}
