package com.shanhe.project.energy.capacity.mapper;

import com.shanhe.project.energy.capacity.vo.PreBatteryGroup;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 组容量
 *
 * @author xuxw
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
    PreBatteryGroup selectLast(@Param("configId") Long configId, @Param("packNum") Integer packNum);

    /**
     * 根据设备id删除组容量信息
     */
    void deleteByConfigId(@Param("configId") Long configId, @Param("packNum") Integer packNum);
}
