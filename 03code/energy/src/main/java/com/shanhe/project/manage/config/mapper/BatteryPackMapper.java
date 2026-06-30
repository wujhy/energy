package com.shanhe.project.manage.config.mapper;

import com.shanhe.project.manage.config.domain.BatteryPack;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;


/**
 * 蓄电池组Mapper接口
 *
 * @author wjh
 * @since 2024-08-29
 */
@Mapper
public interface BatteryPackMapper {

    /**
     * 查询蓄电池组
     *
     * @param packId 蓄电池组主键
     * @return 蓄电池组
     */
    BatteryPack selectBatteryPackByPackId(Long packId);

    /**
     * 根据电池组编号获取设备信息
     * @param packNum 编号
     * @return 电池组
     */
    BatteryPack selectBatteryInfoByPackNum(@Param("packNum") Integer packNum);


    /**
     * 查询默认设备蓄电池组列表
     *
     * @param isEnabled 是否启用
     * @return 蓄电池组集合
     */
    List<BatteryPack> selectDefaultDeviceBatteryPackList(@Param("isEnabled") Integer isEnabled);

    /**
     * 导入蓄电池组
     *
     * @param batteryPacks 蓄电池组
     * @return 结果
     */
    int importBatteryPack(@Param("batteryPacks") List<BatteryPack> batteryPacks);

    /**
     * 新增蓄电池组
     *
     * @param batteryPack 蓄电池组
     * @return 结果
     */
    int insertBatteryPack(BatteryPack batteryPack);

    /**
     * 修改蓄电池组
     *
     * @param batteryPack 蓄电池组
     */
    void update(BatteryPack batteryPack);

    /**
     * 删除默认设备电池组
     */
    void deleteDefaultDevicePacks();

    /**
     * 根据电池组ID批量删除
     *
     * @param packIds 电池组ID列表
     */
    void deleteBatteryPackByBatPackIds(@Param("packIds") List<Long> packIds);

    /**
     * 查询蓄电池组列表
     *
     * @param batteryPack 蓄电池组
     * @return 蓄电池组集合
     */
    List<BatteryPack> selectBatteryPackList(BatteryPack batteryPack);

    /**
     * 查询所有电池组
     *
     * @return 电池组列表
     */
    List<BatteryPack> selectAllBattery();

    /**
     * 根据电池组ID查询列表
     *
     * @param packIds 电池组ID列表
     * @return 电池组列表
     */
    List<BatteryPack> selectBatteryPackByPackIds(@Param("packIds") List<Long> packIds);

    /**
     * 获取电池组最大单体数量
     *
     * @param packNum 电池组编号
     * @return 最大单体数量
     */
    Integer getBatteryMaxNumber(@Param("packNum") Integer packNum);
}
