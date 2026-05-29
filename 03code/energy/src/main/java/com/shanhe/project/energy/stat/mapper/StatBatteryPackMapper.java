package com.shanhe.project.energy.stat.mapper;


import com.shanhe.project.energy.stat.domain.StatBatteryBat;
import com.shanhe.project.energy.stat.domain.StatBatteryPack;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;


/**
 * 电池组统计Mapper接口
 *
 * @author wjh
 * @since 2026-05-25
 */
@Mapper
public interface StatBatteryPackMapper {
    /**
     * 查询电池组统计列表
     *
     * @param statBatteryPack 查询条件
     * @return 电池组统计列表
     */
    List<StatBatteryPack> selectList(StatBatteryPack statBatteryPack);

    /**
     * 新增电池组统计
     *
     * @param statBatteryPack 电池组统计
     */

    void insertOne(StatBatteryPack statBatteryPack);

    /**
     * 批量插入电池组统计数据
     *
     * @param statBatteryPack 统计数据列表
     */
    void insertList(@Param("list") List<StatBatteryPack> statBatteryPack);

    /**
     * 删除电池组统计
     *
     * @param packNum 电池组编号
     */
    void deleteByPackNum(@Param("packNum") Integer packNum);
    /**
     * 查询电池组平均电流
     *
     * @param param 查询条件
     * @return 平均电流
     */
    Double getAvgCurrent(StatBatteryBat param);

    /**
     * 查询统计数据条数
     *
     * @param params 查询条件
     * @return 条数
     */
    Long selectCount(StatBatteryPack params);
}
