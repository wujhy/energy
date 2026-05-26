package com.shanhe.project.energy.stat.service;


import com.shanhe.project.device.config.domain.BatteryPack;
import com.shanhe.project.energy.stat.domain.DevBatteryMonomer;
import com.shanhe.project.sync.domain.BatteryMonomerBatVo;

import java.util.List;


/**
 * 电池单体配置信息Service接口
 *
 * @author wjh
 * @since 2026-05-25
 */
public interface IDevBatteryMonomerService {
    /**
     * 查询电池单体配置信息列表
     *
     * @param packNum 电池组编号
     * @return 电池单体配置信息集合
     */
    List<DevBatteryMonomer> selectList(Integer packNum);

    /**
     * 初始化
     *
     * @param packNum 电池组编号
     */
    void init(Integer packNum);

    /**
     * 删除
     */
    void delete();

    /**
     * 获取最大内阻变化率
     *
     * @param packNum 电池组编号
     * @return 最大内阻变化率
     */
    Double getMaxResistance(Integer packNum);

    /**
     * 删除
     */
    void deleteByPackId(Long packId);

    /**
     * 初始化
     *
     * @param batteryPack 电池组
     * @param childDev 单体内阻数据
     */
    void init(BatteryPack batteryPack, List<BatteryMonomerBatVo> childDev);
}
