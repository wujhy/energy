package com.shanhe.project.device.history.mapper;

import java.util.List;
import com.shanhe.project.device.history.domain.HistoryLog;
import org.apache.ibatis.annotations.Param;

/**
 * 设备历史记录Mapper接口
 * 
 * @author wjh
 * @since 2024-12-31
 */
public interface HistoryLogMapper 
{
    /**
     * 查询设备历史记录
     * 
     * @param historyId 设备历史记录主键
     * @return 设备历史记录
     */
    HistoryLog selectHistoryLogByHistoryId(Long historyId);

    /**
     * 最后一行设备历史记录
     *
     * @param configId 配置id
     * @param packNum 包序号
     * @param itemCode 属性编码
     * @return 设备历史记录
     */
    HistoryLog lastHistoryLog(@Param("configId") Long configId, @Param("packNum") Integer packNum, @Param("itemCode") String itemCode);

    /**
     * 缓存当天最新设备历史记录
     *
     * @return 设备历史记录
     */
    List<HistoryLog> cacheHistoryLog();

    /**
     * 查询设备历史记录列表
     *
     * @param historyLog 设备历史记录
     * @return 设备历史记录集合
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
     * @param historyLog 设备历史记录
     * @return 结果
     */
    int insertHistoryLog(HistoryLog historyLog);
    int insertList(List<HistoryLog> list);

    /**
     * 修改设备历史记录
     * 
     * @param historyLog 设备历史记录
     * @return 结果
     */
    int updateHistoryLog(HistoryLog historyLog);

    /**
     * 更新设备历史记录时间
     *
     * @param historyId 设备历史记录
     * @return 结果
     */
    int updateTime(Long historyId);

    /**
     * 删除设备历史记录
     * 
     * @param historyId 设备历史记录主键
     * @return 结果
     */
    int deleteHistoryLogByHistoryId(Long historyId);

    /**
     * 批量删除设备历史记录
     * 
     * @param historyIds 需要删除的数据主键集合
     * @return 结果
     */
    int deleteHistoryLogByHistoryIds(String[] historyIds);

    /**
     * 删除设备历史记录
     *
     * @param dayNum 天数
     */
    void deleteHistoryLog(Integer dayNum);

    /**
     * 删除设备历史记录
     *
     * @param configId 配置id
     */
    void deleteByConfigId(@Param("configId") Long configId);

    /**
     * 查询设备历史记录数量
     *
     * @param params 查询参数
     * @return 数量
     */
    Long selectCount(HistoryLog params);
}
