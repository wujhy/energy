package com.shanhe.project.energy.stat.service;


import com.shanhe.project.device.config.domain.BatteryPack;
import com.shanhe.project.energy.stat.domain.DevBatteryMonomer;
import com.shanhe.project.sync.domain.BatteryMonomerBatVo;

import java.util.List;


/**
 * 电池单体配置信息Service接口
 *
 * @author zhoubin
 * @date 2025-07-16
 */
public interface IDevBatteryMonomerService {
    /**
     * 查询电池单体配置信息列表
     * @return 电池单体配置信息集合
     */
    List<DevBatteryMonomer> selectList(Long configId, Integer packNum);

    /**
     * 初始化
     */
    void init(Long configId, Integer packNum);

    /**
     * 删除
     */
    void delete();

    /**
     * 获取最大内阻变化率
     * @return 最大内阻变化率
     */
    Double getMaxResistance(Long configId, Integer packNum);

    /**
     * 删除
     */
    void deleteByPackId(Long packId);

    /**
     * 初始化
     */
    void init(BatteryPack batteryPack, List<BatteryMonomerBatVo> childDev);
}
