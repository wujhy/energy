package com.shanhe.project.device.config.mapper;

import com.shanhe.project.device.config.domain.BatteryMonitor;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 单体电池
 *
 * @author wjh
 * @since 2025/3/4
 */
@Mapper
public interface BatteryMonitorMapper {

    /**
     * 新增【单体电池主机实时数据】
     *
     * @param listBattery 【单体电池主机实时数据】
     * @return 结果
     */
    int insertBatchBatteryMonitor(@Param("listBattery") List<BatteryMonitor> listBattery);

    /**
     * 更新记录
     *
     * @param ids id列表
     */
    void updateMonitor(@Param("ids") List<Long> ids);

    /**
     * 查电池历史
     *
     * @param batteryMonitor 电池
     * @return 结果
     */
    List<BatteryMonitor> selectBatteryMonitor(BatteryMonitor batteryMonitor);

    /**
     * 获取设备最新电池记录
     *
     * @param configId 设备id
     * @param packNum 电池组编号
     * @return 电池列表
     */
    List<BatteryMonitor> selectLast(@Param("configId") Long configId, @Param("packNum") Integer packNum, @Param("batSinSize") Integer batSinSize);

    /**
     * 最新电池记录
     *
     * @return 列表
     */
    List<BatteryMonitor> lastMonitor();

    /**
     * 查电池记录
     *
     * @return 列表
     */
    List<BatteryMonitor> listByIds(String[] ids);

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
    void deleteBatteryDays(Integer dayNum);
}
