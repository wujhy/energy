package com.shanhe.project.collector.battery.mapper;

import com.shanhe.project.collector.battery.model.BatteryModuleFrameLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

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

    /**
     * 条件查询原始帧日志。
     *
     * @param channelName 通道名称（可为 null）
     * @param batteryGroup 电池组编号（可为 null）
     * @param commandCode 命令码（可为 null）
     * @param limit 最大返回条数
     * @return 日志列表
     */
    List<BatteryModuleFrameLog> selectList(@Param("channelName") String channelName,
                                           @Param("batteryGroup") Integer batteryGroup,
                                           @Param("commandCode") String commandCode,
                                           @Param("limit") int limit);
}
