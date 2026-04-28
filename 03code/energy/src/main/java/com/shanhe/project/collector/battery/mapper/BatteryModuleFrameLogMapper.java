package com.shanhe.project.collector.battery.mapper;

import com.shanhe.project.collector.battery.model.BatteryModuleFrameLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 600节模块端采集帧日志 Mapper。
 *
 * @author wjh
 * @since 2026-04-28
 */
@Mapper
public interface BatteryModuleFrameLogMapper {

    /**
     * 写入一条采集原始帧日志。
     *
     * @param log 原始帧日志
     */
    void insertOne(BatteryModuleFrameLog log);
}
