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

    /**
     * 删除指定天数之前的原始帧日志。
     *
     * @param days 保留天数
     * @return 删除数量
     */
    int deleteByDays(int days);

    /**
     * 统计原始帧日志总数。
     *
     * @return 日志总数
     */
    long count();
}
