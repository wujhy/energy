package com.shanhe.project.device.config.service;

import com.shanhe.project.device.config.domain.BatteryPack;
import com.shanhe.project.device.config.domain.Config;

import java.util.List;

/**
 * 设备Service接口
 * 
 * @author wjh
 * @since 2024-12-23
 */
public interface IBatteryPackService
{
    /**
     * 查询蓄电池组
     *
     * @param packId 蓄电池组主键
     * @return 蓄电池组
     */
    BatteryPack selectBatteryPackByPackId(Long packId);

    /**
     * 查询设备列表
     * 
     * @param isEnabled 是否启用
     * @return 设备集合
     */
    List<BatteryPack> selectBatteryPackListConfigId(Integer isEnabled);

    /**
     * 根据电池组编号获取设备信息
     * @param packNum 编号
     * @return 电池组
     */
    BatteryPack selectBatteryInfoByPackNum(Integer packNum);

    /**
     * 新增电池组
     *
     * @param batteryPack 新增电池组
     */
    void insertBatteryPack(BatteryPack batteryPack);

    /**
     * 导入电池组
     *
     * @param list 电池组
     */
    void importBatteryPack(List<BatteryPack> list);

    /**
     * 修改蓄电池组
     *
     * @param batteryPack 蓄电池组
     */
    void update(BatteryPack batteryPack);

    /**
     * 批量删除电池组
     * 
     * @param configIds 需要删除的设备id
     */
    void deleteDefaultDevicePacks();

    /**
     * 批量删除电池组
     *
     * @param packIds 需要删除的电池组id
     */
    void deleteBatteryPackByBatPackIds(List<Long> packIds);

    /**
     * 更新缓存
     */
    void updateCache();

    /**
     * 电压均衡标称值
     *
     * @param packNum 组编号
     * @return 电压均衡
     */
    Integer getVoltageBalance(Integer packNum);

    /**
     * 删除电池组
     *
     * @param id 蓄电池组id
     */
    void deleteBatteryPackByBatPackId(Long id);

    /**
     * 修改
     */
    void updateNew(BatteryPack pack);

    /**
     * 新增
     */
    void insertBatteryPackNew(BatteryPack pack);

    /**
     * 获取sin数量
     * @param packNum 组编号
     * @return sin数量
     */
    Integer getBatteryMaxNumber(Integer packNum);
}
