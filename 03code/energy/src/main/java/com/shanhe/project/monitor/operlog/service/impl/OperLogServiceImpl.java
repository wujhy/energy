package com.shanhe.project.monitor.operlog.service.impl;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.shanhe.common.exception.ServiceException;
import com.shanhe.project.monitor.operlog.service.IOperLogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.shanhe.common.utils.text.Convert;
import com.shanhe.project.monitor.operlog.domain.OperLog;
import com.shanhe.project.monitor.operlog.mapper.OperLogMapper;

/**
 * 操作日志 服务层处理
 *
 * @author wjh
 * @since 2026-05-25
 */
@Slf4j
@Service
public class OperLogServiceImpl implements IOperLogService
{
    @Autowired
    private OperLogMapper operLogMapper;

    /**
     * 新增操作日志
     * 
     * @param operLog 操作日志对象
     */
    @Override
    public void insertOperlog(OperLog operLog)
    {
        operLogMapper.insertOperlog(operLog);
    }

    /**
     * 查询系统操作日志集合
     * 
     * @param operLog 操作日志对象
     * @return 操作日志集合
     */
    @Override
    public List<OperLog> selectOperLogList(OperLog operLog)
    {
        return operLogMapper.selectOperLogList(operLog);
    }

    /**
     * 批量删除系统操作日志
     * 
     * @param ids 需要删除的数据
     * @return
     */
    @Override
    public int deleteOperLogByIds(String ids)
    {
        return operLogMapper.deleteOperLogByIds(Convert.toStrArray(ids));
    }

    /**
     * 查询操作日志详细
     * 
     * @param operId 操作ID
     * @return 操作日志对象
     */
    @Override
    public OperLog selectOperLogById(Long operId)
    {
        return operLogMapper.selectOperLogById(operId);
    }
    
    /**
     * 清空操作日志
     */
    @Override
    public void cleanOperLog()
    {
        operLogMapper.cleanOperLog();
    }

    /**
     * 删除指定月份之前的操作日志
     *
     * @param month 月份数
     */
    @Override
    public void deleteOperLog(Integer month) {
        operLogMapper.deleteOperLog(month);
    }

    /**
     * 压缩数据库空间
     */
    @Override
    public void vacuum() {
        operLogMapper.vacuum();
    }

    /**
     * 执行SQL语句
     *
     * @param sql SQL语句
     * @return 执行结果
     */
    @Override
    public String executeSql(String sql) {
        // 只允许执行预定义的SQL语句，防止SQL注入
        boolean allowed = SQL_LIST.stream().anyMatch(s -> s.trim().equalsIgnoreCase(sql.trim()));
        if (!allowed) {
            throw new ServiceException("不允许执行非预定义SQL语句");
        }
        operLogMapper.executeSql(sql);
        return "";
    }

    private static final List<String> SQL_LIST = Collections.unmodifiableList(Arrays.asList(
            "DELETE FROM dev_battery_report_log;",
            "DELETE FROM dev_alarm_log;",
            "DELETE FROM dev_battery_opt;",
            "DELETE FROM dev_battery_opt_log;",
            "DELETE FROM dev_opt_log;",
            "DELETE FROM dev_patrol;",
            "DELETE FROM sys_oper_log;",
            "delete from stat_battery_bat;",
            "delete from stat_battery_pack;",
            "delete from stat_battery_res;",
            "delete from pre_battery_group;",
            "VACUUM;"
    ));

    /**
     * 执行初始化SQL脚本列表
     */
    @Override
    public void initSql() {
        for (String sql : SQL_LIST) {
            try {
                operLogMapper.executeSql(sql);
            } catch (Exception e) {
                log.info("执行sql失败:{}", sql);
            }
        }
    }
}
