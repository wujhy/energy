package com.shanhe.project.energy.capacity.service;

import com.shanhe.project.energy.capacity.vo.PreBatteryGroup;

/**
 * @author zhoubin
 * @date 2025/10/10
 */
public interface PreBatteryGroupService {
    /**
     * 插入记录
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
    void deleteByConfigId(Integer packNum);

    /**
     * 更新缓存
     */
    void updateCache();

}
