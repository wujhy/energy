package com.shanhe.project.energy.stat.mapper;


import com.shanhe.project.energy.capacity.vo.DataPoint;
import com.shanhe.project.energy.stat.domain.StatBatteryBat;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;


/**
 * 单体电池统计Mapper接口
 *
 * @author zhoubin
 * @date 2025-07-15
 */
@Mapper
public interface StatBatteryBatMapper {
    /**
     * 查询单体电池统计列表
     *
     * @return 单体电池统计集合
     */
    List<StatBatteryBat> selectListByPackIds(@Param("packIds") List<Long> packIds,
                                             @Param("startDateTime") String startDateTime, @Param("endDateTime") String endDateTime);

    /**
     * 新增单体电池统计
     *
     * @param statBattery 单体电池统计
     */
    void insertList(@Param("list") List<StatBatteryBat> statBattery);

    /**
     * 查询单体电池组统计列表
     *
     * @return 单体电池组统计集合
     */
    List<StatBatteryBat> selectList(StatBatteryBat statBatteryBat);


    /**
     * 删除单体电池统计
     *
     * @param configId 设备ID
     */
    void deleteByConfigId(@Param("configId") Long configId, @Param("packNum") Integer packNum);

    List<DataPoint> selectDataPointList(@Param("configId") Long configId, @Param("packNum") Integer packNum,
                                        @Param("batNum") Integer batNum,
                                        @Param("startDateTime") String startDateTime, @Param("endDateTime") String endDateTime);
}
