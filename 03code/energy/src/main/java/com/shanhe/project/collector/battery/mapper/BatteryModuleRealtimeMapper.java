package com.shanhe.project.collector.battery.mapper;

import com.shanhe.project.collector.battery.model.BatteryModuleCellRealtime;
import com.shanhe.project.collector.battery.model.BatteryModuleGroupCalculation;
import com.shanhe.project.collector.battery.model.BatteryModuleGroupRealtime;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 600节模块端标准实时数据 Mapper。
 */
@Mapper
public interface BatteryModuleRealtimeMapper {

    void upsertCell(BatteryModuleCellRealtime realtime);

    void upsertCells(@Param("cells") List<BatteryModuleCellRealtime> cells);

    void upsertGroup(BatteryModuleGroupRealtime realtime);

    void upsertGroups(@Param("groups") List<BatteryModuleGroupRealtime> groups);

    List<BatteryModuleCellRealtime> selectCells(@Param("channelName") String channelName,
                                                @Param("batteryGroup") Integer batteryGroup);

    BatteryModuleGroupRealtime selectGroup(@Param("channelName") String channelName,
                                           @Param("batteryGroup") Integer batteryGroup);

    void upsertCalculation(BatteryModuleGroupCalculation calculation);
}
