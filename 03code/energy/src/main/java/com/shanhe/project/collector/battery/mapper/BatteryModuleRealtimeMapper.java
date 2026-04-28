package com.shanhe.project.collector.battery.mapper;

import com.shanhe.project.collector.battery.model.BatteryModuleCellRealtime;
import com.shanhe.project.collector.battery.model.BatteryModuleGroupCalculation;
import com.shanhe.project.collector.battery.model.BatteryModuleGroupRealtime;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 600节模块端标准实时数据 Mapper。
 *
 * @author wjh
 * @since 2026-04-28
 */
@Mapper
public interface BatteryModuleRealtimeMapper {

    /**
     * 写入或更新单体实时数据。
     *
     * @param realtime 单体实时数据
     */
    void upsertCell(BatteryModuleCellRealtime realtime);

    /**
     * 批量写入或更新单体实时数据。
     *
     * @param cells 单体实时数据列表
     */
    void upsertCells(@Param("cells") List<BatteryModuleCellRealtime> cells);

    /**
     * 写入或更新组实时数据。
     *
     * @param realtime 组实时数据
     */
    void upsertGroup(BatteryModuleGroupRealtime realtime);

    /**
     * 批量写入或更新组实时数据。
     *
     * @param groups 组实时数据列表
     */
    void upsertGroups(@Param("groups") List<BatteryModuleGroupRealtime> groups);

    /**
     * 查询指定通道和组的单体实时数据。
     *
     * @param channelName 通道名称
     * @param batteryGroup 电池组编号
     * @return 单体实时数据列表
     */
    List<BatteryModuleCellRealtime> selectCells(@Param("channelName") String channelName,
                                                @Param("batteryGroup") Integer batteryGroup);

    /**
     * 查询指定通道和组的组实时数据。
     *
     * @param channelName 通道名称
     * @param batteryGroup 电池组编号
     * @return 组实时数据
     */
    BatteryModuleGroupRealtime selectGroup(@Param("channelName") String channelName,
                                           @Param("batteryGroup") Integer batteryGroup);

    /**
     * 写入或更新电池组计算结果。
     *
     * @param calculation 电池组计算结果
     */
    void upsertCalculation(BatteryModuleGroupCalculation calculation);
}
