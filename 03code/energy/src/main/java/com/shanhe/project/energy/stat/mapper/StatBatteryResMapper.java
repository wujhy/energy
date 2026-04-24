package com.shanhe.project.energy.stat.mapper;

import com.shanhe.project.energy.stat.domain.StatBatteryRes;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;


/**
 * 单体内阻变化统计（内阻测试后）Mapper接口
 *
 * @author zhoubin
 * @date 2025-07-21
 */
@Mapper
public interface StatBatteryResMapper {
    /**
     * 查询单体内阻变化统计（内阻测试后）列表
     *
     * @return 单体内阻变化统计（内阻测试后）集合
     */
    List<StatBatteryRes> selectList(@Param("configId") Long configId, @Param("packNum") Integer packNum, @Param("batNum") Integer batNum);

    /**
     * 新增单体内阻变化统计（内阻测试后）
     *
     * @param statBatteryRes 单体内阻变化统计（内阻测试后）
     */
    void insertList(@Param("list") List<StatBatteryRes> statBatteryRes);

    /**
     * 根据配置ID删除单体内阻变化统计（内阻测试后）
     *
     * @param configId 配置ID
     */
    void deleteByConfigId(@Param("configId") Long configId, @Param("packNum") Integer packNum);
}
