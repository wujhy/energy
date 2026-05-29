package com.shanhe.project.energy.capacity.mapper;

import com.shanhe.project.energy.capacity.vo.PreBatteryGroup;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 组容量
 *
 * @author wjh
 * @since 2026-05-25
 */
@Mapper
public interface PreBatteryGroupMapper {

    /**
     * 插入组容量信息
     */
    void insert(PreBatteryGroup groupVo);

    /**
     * 查询最新的组容量信息
     */
    PreBatteryGroup selectLast(@Param("packNum") Integer packNum);

    /**
     * 根据电池组编号删除组容量信息
     */
    void deleteByPackNum(@Param("packNum") Integer packNum);
}
