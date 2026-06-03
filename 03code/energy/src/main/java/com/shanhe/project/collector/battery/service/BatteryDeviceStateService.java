package com.shanhe.project.collector.battery.service;

import com.shanhe.project.collector.battery.model.BatteryDeviceState;

import java.util.List;

/**
 * 蓄电池设备状态服务。
 *
 * @author wjh
 * @since 2026-05-29
 */
public interface BatteryDeviceStateService {

    /**
     * 插入或更新设备状态。
     *
     * @param state 设备状态
     */
    void upsert(BatteryDeviceState state);

    /**
     * 按作用域和状态编码查询。
     *
     * @param scopeType 作用域类型
     * @param scopeKey 作用域标识
     * @param stateCode 状态编码
     * @return 设备状态
     */
    BatteryDeviceState selectByScope(String scopeType, String scopeKey, String stateCode);

    /**
     * 按电池组编号和状态编码查询列表。
     *
     * @param packNum 电池组编号
     * @param stateCode 状态编码
     * @return 状态列表
     */
    List<BatteryDeviceState> selectByPackAndCode(Integer packNum, String stateCode);

    /**
     * 按通道名称和状态编码查询列表。
     *
     * @param channelName 通道名称
     * @param stateCode 状态编码
     * @return 状态列表
     */
    List<BatteryDeviceState> selectByChannelAndCode(String channelName, String stateCode);

    /**
     * 按电池组编号查询所有状态。
     *
     * @param packNum 电池组编号
     * @return 状态列表
     */
    List<BatteryDeviceState> selectByPackNum(Integer packNum);

    /**
     * 条件查询状态列表。
     *
     * @param state 查询条件
     * @return 状态列表
     */
    List<BatteryDeviceState> selectList(BatteryDeviceState state);

    /**
     * 按主键删除。
     *
     * @param stateId 主键
     */
    void deleteByStateId(Long stateId);

    /**
     * 按作用域删除所有状态。
     *
     * @param scopeType 作用域类型
     * @param scopeKey 作用域标识
     */
    void deleteByScope(String scopeType, String scopeKey);

    /**
     * 删除过期状态。
     *
     * @return 删除数量
     */
    int deleteExpired();

    /**
     * 按电池组编号删除所有状态。
     *
     * @param packNum 电池组编号
     */
    void deleteByPackNum(Integer packNum);

    /**
     * 删除全部设备状态。
     */
    void deleteAll();

    /**
     * 查询电池组当前态摘要（工作模式、在线状态、246 新鲜度）。
     *
     * @param packNum 电池组编号
     * @return 状态列表
     */
    List<BatteryDeviceState> getPackStatusSummary(Integer packNum);

    /**
     * 查询通道当前态摘要（串口状态、异常状态、超时模块列表、活跃模块列表）。
     *
     * @param channelName 通道名称
     * @return 状态列表
     */
    List<BatteryDeviceState> getChannelStatusSummary(String channelName);
}
