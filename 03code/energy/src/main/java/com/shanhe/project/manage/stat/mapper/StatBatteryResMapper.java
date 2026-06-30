package com.shanhe.project.manage.stat.mapper;

import com.shanhe.project.manage.stat.domain.StatBatteryRes;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;


/**
 * 单体内阻变化统计（内阻测试后）Mapper接口
 *
 * @author wjh
 * @since 2026-05-25
 */
@Mapper
public interface StatBatteryResMapper {
    /**
     * 查询单体内阻变化统计（内阻测试后）列表
     *
     * @param packNum 电池组编号
     * @param batNum 单体编号
     * @return 单体内阻变化统计（内阻测试后）集合
     */
    List<StatBatteryRes> selectList(@Param("packNum") Integer packNum, @Param("batNum") Integer batNum);

    /**
     * 新增单体内阻变化统计（内阻测试后）
     *
     * @param statBatteryRes 单体内阻变化统计（内阻测试后）
     */
    void insertList(@Param("list") List<StatBatteryRes> statBatteryRes);

    /**
     * 根据电池组编号删除单体内阻变化统计（内阻测试后）
     *
     * @param packNum 电池组编号
     */
    void deleteByPackNum(@Param("packNum") Integer packNum);
}
