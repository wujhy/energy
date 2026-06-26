package com.shanhe.project.energy.stat.mapper;


import com.shanhe.project.energy.capacity.vo.DataPoint;
import com.shanhe.project.energy.stat.domain.StatBatteryBat;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;


/**
 * 单体电池统计Mapper接口
 *
 * @author wjh
 * @since 2026-05-25
 */
@Mapper
public interface StatBatteryBatMapper {
    /**
     * 查询单体电池统计列表
     *
     * @param packIds 电池组编号列表
     * @param startDateTime 开始时间
     * @param endDateTime 结束时间
     * @return 单体电池统计集合
     */
    List<StatBatteryBat> selectListByPackIds(@Param("packIds") List<Long> packIds,
                                             @Param("startDateTime") String startDateTime, @Param("endDateTime") String endDateTime);

    /**
     * 新增单体电池统计
     *
     * @param statBattery 单体电池统计列表
     */
    void insertList(@Param("list") List<StatBatteryBat> statBattery);

    /**
     * 查询单体电池组统计列表
     *
     * @param statBatteryBat 查询参数
     * @return 单体电池组统计集合
     */
    List<StatBatteryBat> selectList(StatBatteryBat statBatteryBat);


    /**
     * 删除单体电池统计
     *
     * @param packNum 电池组编号
     */
    void deleteByPackNum(@Param("packNum") Integer packNum);

    /**
     * 查询单体数据点列表
     *
     * @param packNum 电池组编号
     * @param batNum 单体编号
     * @param startDateTime 开始时间
     * @param endDateTime 结束时间
     * @return 数据点列表
     */
    List<DataPoint> selectDataPointList(@Param("packNum") Integer packNum,
                                        @Param("batNum") Integer batNum,
                                        @Param("startDateTime") String startDateTime, @Param("endDateTime") String endDateTime);
}
