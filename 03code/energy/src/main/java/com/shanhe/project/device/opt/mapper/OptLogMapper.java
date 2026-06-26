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
     *
     * @param optLog 操作日志
     * @return 记录ID
     */
    Long insert(OptLog optLog);

    /**
     * 更新操作日志
     *
     * @param id 记录id
     * @param result 操作结果
     * @param updateTimeStr 更新时间字符串
     */
    void update(@Param("id") Long id, @Param("result") Integer result, @Param("updateTimeStr") String updateTimeStr);

    /**
     * 查询操作日志
     *
     * @param optLog 查询参数
     * @return 操作日志列表
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
     *
     * @return 操作日志列表
     */
    List<OptLog> findRunningList();

    /**
     * 查询指定电池组运行中的操作日志
     *
     * @param packNum 电池组编号
     * @return 操作日志列表
     */
    List<OptLog> selectRunningList(@Param("packNum") Integer packNum);

    /**
     * 更新操作日志
     *
     * @param id 记录ID
     * @param dischargeCapacity 放电容量
     * @param bcapacity 预估容量
     * @param current 电流
     * @param endTimeStr 结束时间字符串
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
     *
     * @param packNum 电池组编号
     * @param types 测试类型列表
     * @return 日志数量
     */
    Integer count(@Param("packNum") Integer packNum, @Param("types") List<Integer> types);

    /**
     * 获取指定类型的最后操作日志
     *
     * @param packNum 电池组编号
     * @param type 测试类型
     * @return 操作日志
     */
    OptLog lastByType(@Param("packNum") Integer packNum, @Param("type") Integer type);

    /**
     * 根据电池组编号删除操作日志
     *
     * @param packNum 电池组编号
     */
    void deleteByPackNum(@Param("packNum") Integer packNum);

    /**
     * 更新600模块命令执行日志状态
     *
     * @param id 记录ID
     * @param status 命令状态
     * @param result 执行结果
     * @param responseCode 响应码
     * @param endedAt 结束时间
     * @param errorMessage 错误信息
     * @param responsePayload 响应载荷hex（可为null）
     */
    void updateCommandStatus(@Param("id") Long id,
                             @Param("status") String status,
                             @Param("result") Integer result,
                             @Param("responseCode") Integer responseCode,
                             @Param("endedAt") String endedAt,
                             @Param("errorMessage") String errorMessage,
                             @Param("responsePayload") String responsePayload);
}
