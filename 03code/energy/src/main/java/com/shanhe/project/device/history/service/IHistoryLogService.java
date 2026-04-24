package com.shanhe.project.device.history.service;

import java.util.List;

import com.shanhe.project.device.config.domain.ConfigAttribute;
import com.shanhe.project.device.history.domain.HistoryLog;

/**
 * 设备历史记录Service接口
 * 
 * @author wjh
 * @since 2024-12-31
 */
public interface IHistoryLogService 
{
    /**
     * 查询设备历史记录
     * 
     * @param historyId 设备历史记录主键
     * @return 设备历史记录
     */
    HistoryLog selectHistoryLogByHistoryId(Long historyId);

    /**
     * 查询设备最新记录列表
     *
     * @param historyLog 设备历史记录
     * @return 设备最新记录集合
     */
    List<HistoryLog> lastList(HistoryLog historyLog);

    /**
     * 查询设备历史记录列表
     * 
     * @param historyLog 设备历史记录
     * @return 设备历史记录集合
     */
    List<HistoryLog> selectHistoryLogList(HistoryLog historyLog);

    /**
     * 查询设备历史记录列表
     *
     * @param historyLog 设备历史记录
     * @return 设备历史记录集合
     */
    List<HistoryLog> simpleList(HistoryLog historyLog);

    /**
     * 新增设备历史记录
     *
     * @param configAttribute 设备属性
     * @param value 属性值
     */
    void insertHistoryLog(ConfigAttribute configAttribute, String value, boolean isInsert);

    /**
     * 批量删除设备历史记录
     * 
     * @param historyIds 需要删除的设备历史记录主键集合
     * @return 结果
     */
    int deleteHistoryLogByHistoryIds(String historyIds);

    /**
     * 更新告警缓存
     */
    void updateCache();

    /**
     * 获取历史最新值
     *
     * @param attribute 属性
     * @return 值
     */
    HistoryLog lastValue(ConfigAttribute attribute);

    /**
     * 缓存获取最新值
     *
     * @param configId 配置id
     * @param packNum 包序号
     * @param code 属性编码
     * @return 值
     */
    String getCacheBy(Long configId, Integer packNum, String code);

    /**
     * 删除指定月份前日志
     *
     * @param dayNum 天数
     */
    void deleteHistoryLog(Integer dayNum);

    /**
     * 删除指定配置id日志
     *
     * @param configId 配置id
     */
    void deleteByConfigId(Long configId);

    /**
     * 导出
     */
    void export(HistoryLog historyLog);
}
