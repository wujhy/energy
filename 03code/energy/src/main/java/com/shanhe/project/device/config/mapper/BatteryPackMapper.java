package com.shanhe.project.device.config.mapper;

import com.shanhe.project.device.config.domain.BatteryPack;
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
     * @param configId 配置id
     * @param packNum 编号
     * @return 电池组
     */
    BatteryPack selectBatteryInfoByPackNum(@Param("configId") Long configId, @Param("packNum") Integer packNum);


    /**
     * 查询默认设备蓄电池组列表
     *
     * @param configId 默认设备ID
     * @param isEnabled 是否启用
     * @return 蓄电池组集合
     */
    List<BatteryPack> selectBatteryPackListByConfigId(@Param("configId") Long configId, @Param("isEnabled") Integer isEnabled);

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
     * 根据配置ID批量删除电池组
     *
     * @param configIds 配置ID数组
     */
    void deleteByConfigIds(@Param("configIds") String[] configIds);

    /**
     * 根据电池组ID批量删除
     *
     * @param packIds 电池组ID列表
     */
    void deleteBatteryPackByBatPackIds(@Param("packIds") List<Long> packIds);

    /**
     * 根据配置ID查询电池组列表
     *
     * @param configIds 配置ID数组
     * @return 电池组列表
     */
    List<BatteryPack> selectBatteryPackByConfigIds(@Param("configIds") List<Long> configIds);

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
     * @param configId 配置ID
     * @param packNum 电池组编号
     * @return 最大单体数量
     */
    Integer getBatteryMaxNumber(@Param("configId") Long configId, @Param("packNum") Integer packNum);
}
