package com.shanhe.project.energy.stat.mapper;


import com.shanhe.project.energy.stat.domain.StatBatteryBat;
import com.shanhe.project.energy.stat.domain.StatBatteryPack;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;


/**
 * 电池组统计Mapper接口
 *
 * @author zhoubin
 * @date 2025-07-15
 */
@Mapper
public interface StatBatteryPackMapper {
    /**
     * 查询电池组统计列表
     *
     */
    List<StatBatteryPack> selectList(StatBatteryPack statBatteryPack);

    /**
     * 新增电池组统计
     *
     * @param statBatteryPack 电池组统计
     */

    void insertOne(StatBatteryPack statBatteryPack);

    void insertList(@Param("list") List<StatBatteryPack> statBatteryPack);

    /**
     * 删除电池组统计
     *
     * @param configId 配置ID
     */
    void deleteByConfigId(@Param("configId") Long configId, @Param("packNum") Integer packNum);
    /**
     * 获取平均电流
     *
     * @param param
     * @return
     */
    Double getAvgCurrent(StatBatteryBat param);

    /**
     * 获取数量
     *
     * @param params
     * @return
     */
    Long selectCount(StatBatteryPack params);
}
