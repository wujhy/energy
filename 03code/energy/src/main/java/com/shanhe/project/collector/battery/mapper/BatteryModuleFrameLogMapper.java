package com.shanhe.project.collector.battery.mapper;

import com.shanhe.project.collector.battery.model.BatteryModuleFrameLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 600节模块端采集帧日志 Mapper。
 */
@Mapper
public interface BatteryModuleFrameLogMapper {

    void insertOne(BatteryModuleFrameLog log);
}
