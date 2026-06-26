package com.shanhe.project.energy.capacity.service;

import com.shanhe.project.energy.capacity.vo.PreBatteryGroup;

/**
 * 预测电池组服务接口
 *
 * @author wjh
 * @since 2026-05-25
 */
public interface PreBatteryGroupService {
    /**
     * 插入记录
     *
     * @param groupVo 组容量信息
     */
    void insert(PreBatteryGroup groupVo);

    /**
     * 获取电池组最新记录（缓存）
     *
     * @param packNum 电池组编号
     * @return 电池组
     */
    PreBatteryGroup lastCache(Integer packNum);

    /**
     * 删除记录
     *
     * @param packNum 电池组编号；为空时删除默认设备全部预测容量
     */
    void deleteByPackNum(Integer packNum);

    /**
     * 更新缓存
     */
    void updateCache();

}
