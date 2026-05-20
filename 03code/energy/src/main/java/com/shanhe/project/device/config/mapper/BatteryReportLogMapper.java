package com.shanhe.project.device.config.mapper;

import com.shanhe.project.device.config.domain.BatteryReportLog;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 蓄电池上报日志
 *
 * @author wjh
 * @since 2025/7/9
 */
public interface BatteryReportLogMapper {

    /**
     * 插入记录
     */
    void insert(BatteryReportLog batteryReportLog);
    void insertList(List<BatteryReportLog> list);

    /**
     * 获取设备最新电池记录
     *
     * @param packNum 电池组编号
     * @return 电池列表
     */
    BatteryReportLog selectLast(@Param("packNum") Integer packNum);

    /**
     * 查电池历史
     *
     * @param batteryReportLog 电池
     * @return 结果
     */
    List<BatteryReportLog> selectBatteryReportLog(BatteryReportLog batteryReportLog);

    /**
     * 删除记录
     *
     * @param ids 记录id
     * @return 删除结果
     */
    int deleteByIds(String[] ids);

    /**
     * 删除设备历史记录
     *
     * @param dayNum 天数
     */
    void deleteByDays(Integer dayNum);

    /**
     * 删除设备告警记录
     *
     * @param configId 设备id
     */
    void deleteByConfigId(@Param("configId") Long configId, @Param("packNum") Integer packNum);

    /**
     * 计数
     */
    Long selectCount(BatteryReportLog params);
}
