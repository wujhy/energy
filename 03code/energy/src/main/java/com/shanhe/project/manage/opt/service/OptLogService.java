package com.shanhe.project.manage.opt.service;

import com.shanhe.project.manage.opt.domain.OptLog;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 设备操作日志
 *
 * @author wjh
 * @since 2025/7/9
 */
public interface OptLogService {

    /**
     * 插入操作日志
     *
     * @param packNum 组序号
     * @param type 操作类型
     * @param result 结果
     * @return 记录ID
     */
    Long insert(Integer packNum, Integer type, Integer result);

    /**
     * 插入操作日志。
     *
     * @param packNum 组序号
     * @param type 操作类型
     * @param result 结果
     * @param source 执行来源
     * @return 记录ID
     */
    Long insert(Integer packNum, Integer type, Integer result, String source);

    /**
     * 插入操作日志
     *
     * @param params 操作参数
     * @param result 结果
     * @return 记录ID
     */
    Long insert(Map<String, Object> params, Integer result);


    /**
     * 按标准实时状态插入蓄电池操作日志。
     *
     * @param packNum 组序号
     * @param batteryPackStatus 电池组状态
     * @param resistanceTestStatus 内阻测试状态
     * @param previousRealtimeTime 上一标准实时快照时间
     */
    void insertBatteryRealtime(Integer packNum, Integer batteryPackStatus, Integer resistanceTestStatus, Date previousRealtimeTime);

    /**
     * 更新操作日志
     *
     * @param id 记录id
     * @param result 操作结果
     * @param updateTime 更新时间
     */
    void update(Long id, Integer result, Date updateTime);

    /**
     * 查询操作日志
     *
     * @param optLog 查询条件
     * @return 操作日志列表
     */
    List<OptLog> select(OptLog optLog);

    /**
     * 查询运行中的操作日志。
     *
     * @param packNum 电池组编号；为空时查询全部
     * @return 运行中日志列表
     */
    List<OptLog> selectRunningList(Integer packNum);

    /**
     * 删除记录
     *
     * @param ids 记录id
     * @return 删除结果
     */
    int deleteByIds(String ids);

    /**
     * 删除历史记录
     *
     */
    void deleteDefaultDeviceLogs();

    /** 更新缓存 */
    void updateCache();

    /**
     * 查询未完成缓存日志
     *
     * @param packNum 组序号
     * @param type 操作类型
     * @return 操作日志
     */
    OptLog selectNotFinishedCacheLog(Integer packNum, Integer type);

    /**
     * 查询设备是否正在执行测试操作
     *
     * @param packNum 组序号
     * @param type 操作类型
     * @return 正在执行的操作日志
     */
    OptLog getRunningOptLog(Integer packNum, Integer type);

    /**
     * 统计操作日志
     *
     * @param packNum 组序号
     * @param types 操作类型列表
     * @return 统计数量
     */
    Integer count(Integer packNum, List<Integer> types);

    /**
     * 更新最后一次放电记录的 预估容量、放电电流
     *
     * @param optId 操作记录ID
     * @param dischargeCapacity 放电容量
     * @param bcapacity 预估容量
     * @param current 放电电流
     * @param endTime 结束时间
     */
    void updateBatteryBcapacity(Long optId, Double dischargeCapacity, Double bcapacity, Double current, Date endTime);

    /**
     * 获取最后一次操作记录
     *
     * @param packNum 组序号
     * @param type 操作类型
     * @return 最后一次操作记录
     */
    OptLog lastType(Integer packNum, int type);

    /**
     * 删除组操作记录
     *
     * @param packNum 组序号
     */
    void deleteByPackNum(Integer packNum);

    /**
     * 关闭组操作记录
     *
     * @param packNum 组序号
     */
    void closeOptLog(Integer packNum);

    /**
     * 停止测试
     *
     * @param packNum 组序号
     * @param type 操作类型
     */
    void doStopTest(Integer packNum, Integer type);
}
