package com.shanhe.project.device.config.mapper;

import com.shanhe.project.device.config.domain.BatteryPack;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;


/**
 * 蓄电池组Mapper接口
 *
 * @author zhoubin
 * &#064;date  2024-08-29
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
     * 查询蓄电池组列表
     *
     * @param configId 蓄电池组
     * @return 蓄电池组集合
     */
    List<BatteryPack> selectBatteryPackListConfigId(@Param("configId") Long configId, @Param("isEnabled") Integer isEnabled);

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

    void deleteByConfigIds(@Param("configIds") String[] configIds);
    void deleteBatteryPackByBatPackIds(@Param("packIds") List<Long> packIds);

    List<BatteryPack> selectBatteryPackByConfigIds(@Param("configIds") List<Long> configIds);

    /**
     * 查询蓄电池组列表
     *
     * @param batteryPack 蓄电池组
     * @return 蓄电池组集合
     */
    List<BatteryPack> selectBatteryPackList(BatteryPack batteryPack);

    List<BatteryPack> selectAllBattery();

    List<BatteryPack> selectBatteryPackByPackIds(@Param("packIds") List<Long> packIds);

    Integer getBatteryMaxNumber(@Param("configId") Long configId, @Param("packNum") Integer packNum);
}
