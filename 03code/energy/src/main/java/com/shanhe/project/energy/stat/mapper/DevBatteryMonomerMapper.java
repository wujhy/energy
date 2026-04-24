package com.shanhe.project.energy.stat.mapper;


import com.shanhe.project.energy.stat.domain.DevBatteryMonomer;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 电池单体配置信息Mapper接口
 *
 * @author zhoubin
 * @date 2025-07-16
 */
@Mapper
public interface DevBatteryMonomerMapper {
    /**
     * 查询电池单体配置信息列表
     *
     * @param packId 电池组ID
     * @return 电池单体配置信息集合
     */
    List<DevBatteryMonomer> selectList(@Param("packId") Long packId);

    /**
     * 新增电池单体配置信息
     *
     * @param devBatteryMonomer 电池单体配置信息
     */
    void insertList(@Param("list") List<DevBatteryMonomer> devBatteryMonomer);

    /**
     * 根据电池组ID删除电池单体配置信息
     *
     * @param packId 电池组ID
     */
    void deleteByPackId(Long packId);

    /**
     * 根据设备ID删除电池单体配置信息
     *
     */
    void delete();
}

