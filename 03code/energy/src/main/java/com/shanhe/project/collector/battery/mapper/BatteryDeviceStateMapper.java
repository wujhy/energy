package com.shanhe.project.collector.battery.mapper;

import com.shanhe.project.collector.battery.model.BatteryDeviceState;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 蓄电池设备状态 Mapper。
 *
 * @author wjh
 * @since 2026-05-29
 */
public interface BatteryDeviceStateMapper {

    /**
     * 插入或更新设备状态（按 scope_type + scope_key + state_code 唯一键冲突更新）。
     *
     * @param state 设备状态
     * @return 影响行数
     */
    int upsert(BatteryDeviceState state);

    /**
     * 按作用域和状态编码查询。
     *
     * @param scopeType 作用域类型
     * @param scopeKey 作用域标识
     * @param stateCode 状态编码
     * @return 设备状态
     */
    BatteryDeviceState selectByScope(@Param("scopeType") String scopeType,
                                     @Param("scopeKey") String scopeKey,
                                     @Param("stateCode") String stateCode);

    /**
     * 按电池组编号和状态编码查询列表。
     *
     * @param packNum 电池组编号
     * @param stateCode 状态编码
     * @return 状态列表
     */
    List<BatteryDeviceState> selectByPackAndCode(@Param("packNum") Integer packNum,
                                                  @Param("stateCode") String stateCode);

    /**
     * 按通道名称和状态编码查询列表。
     *
     * @param channelName 通道名称
     * @param stateCode 状态编码
     * @return 状态列表
     */
    List<BatteryDeviceState> selectByChannelAndCode(@Param("channelName") String channelName,
                                                     @Param("stateCode") String stateCode);

    /**
     * 按电池组编号查询所有状态。
     *
     * @param packNum 电池组编号
     * @return 状态列表
     */
    List<BatteryDeviceState> selectByPackNum(@Param("packNum") Integer packNum);

    /**
     * 查询指定条件的状态列表。
     *
     * @param state 查询条件
     * @return 状态列表
     */
    List<BatteryDeviceState> selectList(BatteryDeviceState state);

    /**
     * 按主键删除。
     *
     * @param stateId 主键
     * @return 影响行数
     */
    int deleteByStateId(@Param("stateId") Long stateId);

    /**
     * 按作用域删除所有状态。
     *
     * @param scopeType 作用域类型
     * @param scopeKey 作用域标识
     * @return 影响行数
     */
    int deleteByScope(@Param("scopeType") String scopeType, @Param("scopeKey") String scopeKey);

    /**
     * 删除过期状态。
     *
     * @return 影响行数
     */
    int deleteExpired();

    /**
     * 按电池组编号删除所有状态。
     *
     * @param packNum 电池组编号
     * @return 影响行数
     */
    int deleteByPackNum(@Param("packNum") Integer packNum);

    /**
     * 删除全部设备状态。
     *
     * @return 影响行数
     */
    int deleteAll();
}
