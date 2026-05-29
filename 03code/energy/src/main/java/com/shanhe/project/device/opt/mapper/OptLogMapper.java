package com.shanhe.project.device.opt.mapper;

import com.shanhe.project.device.opt.domain.OptLog;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 设备操作日志
 *
 * @author wjh
 * @since 2025/7/9
 */
public interface OptLogMapper {

    /**
     * 插入操作日志
     */
    Long insert(OptLog optLog);

    /**
     * 更新操作日志
     *
     * @param id 记录id
     * @param result 操作结果
     */
    void update(@Param("id") Long id, @Param("result") Integer result, @Param("updateTimeStr") String updateTimeStr);

    /**
     * 查询操作日志
     */
    List<OptLog> select(OptLog optLog);

    /**
     * 删除记录
     *
     * @param ids 记录id
     * @return 删除结果
     */
    int deleteByIds(String[] ids);

    /**
     * 删除默认设备历史记录
     */
    void deleteDefaultDeviceLogs();

    /**
     * 查询运行中的操作日志
     */
    List<OptLog> findRunningList();

    /**
     * 更新操作日志
     */
    void updateBattery(@Param("id") Long id,
                       @Param("dischargeCapacity") Double dischargeCapacity, @Param("bcapacity") Double bcapacity,
                       @Param("current") Double current, @Param("endTimeStr") String endTimeStr);

    /**
     * 查询正在运行的操作日志
     *
     * @param packNum 电池组编号
     * @param type 测试类型
     * @return 操作日志
     */
    OptLog getRunningOptLog(@Param("packNum") Integer packNum, @Param("type") Integer type);

    /**
     * 查询设备操作日志数量
     */
    Integer count(@Param("packNum") Integer packNum, @Param("types") List<Integer> types);

    /**
     * 获取指定类型的最后操作日志
     */
    OptLog lastByType(@Param("packNum") Integer packNum, @Param("type") Integer type);

    /**
     * 根据电池组编号删除操作日志
     *
     * @param packNum 电池组编号
     */
    void deleteByPackNum(@Param("packNum") Integer packNum);
}
