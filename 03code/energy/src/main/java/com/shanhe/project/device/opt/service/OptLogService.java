package com.shanhe.project.device.opt.service;

import com.shanhe.project.device.config.domain.BatteryReportLog;
import com.shanhe.project.device.opt.domain.OptLog;

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
     * @param configId 设备配置id
     * @param packNum 组序号
     * @param type 操作类型
     * @param result 结果
     */
    Long insert(Long configId, Integer packNum, Integer type, Integer result);

    /**
     * 插入操作日志
     *
     * @param configId 设备配置id
     * @param params 操作参数
     * @param result 结果
     */
    Long insert(Long configId, Map<String, Object> params, Integer result);

    /**
     * 插入蓄电池日志
     *
     * @param configId 设备配置id
     * @param packNum 组序号
     * @param packMap 操作参数
     */
    void insertBattery(Long configId, Integer packNum, Map<String, Object> packMap, BatteryReportLog oldInfo);

    /**
     * 更新操作日志
     *
     * @param id 记录id
     * @param result 操作结果
     */
    void update(Long id, Integer result, Date updateTime);

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
    int deleteByIds(String ids);

    /**
     * 删除历史记录
     *
     * @param configIds 设备ID
     */
    void deleteByConfigIds(String[] configIds);

    /**
     * 更新缓存
     */
    void updateCache();

    /**
     * 查询未完成缓存日志
     *
     * @param configId 设备ID
     * @param packNum 组序号
     * @param type 操作类型
     * @return 操作日志
     */
    OptLog selectNotFinishedCacheLog(Long configId, Integer packNum, Integer type);

    /**
     * 查询设备是否正在执行测试操作
     */
    OptLog getRunningOptLog(Long configId, Integer packNum, Integer type);

    /**
     * 统计操作日志
     */
    Integer count(Long configId, Integer packNum, List<Integer> types);

    /**
     * 更新最后一次放电记录的 预估容量、放电电流
     */
    void updateBatteryBcapacity(Long optId, Double dischargeCapacity, Double bcapacity, Double current, Date endTime);

    /**
     * 获取最后一次操作记录
     */
    OptLog lastType(Long configId, Integer packNum, int type);

    /**
     * 删除组操作记录
     */
    void deleteByConfigIdPackNum(Long configId, Integer packNum);

    /**
     * 关闭组操作记录
     */
    void closeOptLog(Long configId, Integer packNum);

    /**
     * 停止测试
     */
    void doStopTest(Long configId, Integer packNum, Integer type);
}
