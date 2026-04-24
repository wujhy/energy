package com.shanhe.project.monitor.operlog.service.impl;

import java.util.ArrayList;
import java.util.List;

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
 * @author ruoyi
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

    @Override
    public void deleteOperLog(Integer month) {
        operLogMapper.deleteOperLog(month);
    }

    @Override
    public void vacuum() {
        operLogMapper.vacuum();
    }

    @Override
    public String executeSql(String sql) {
        operLogMapper.executeSql(sql);
        return "";
    }

    private static final List<String> SQL_LIST = new ArrayList<>();
    static {
        SQL_LIST.add("DELETE FROM dev_battery_report_log;");
        SQL_LIST.add("DELETE FROM dev_alarm_log;");
        SQL_LIST.add("DELETE FROM dev_battery_opt;");
        SQL_LIST.add("DELETE FROM dev_battery_opt_log;");
        SQL_LIST.add("DELETE FROM dev_history_log;");
        SQL_LIST.add("DELETE FROM dev_opt_log;");
        SQL_LIST.add("DELETE FROM dev_patrol;");
        SQL_LIST.add("DELETE FROM sys_oper_log;");
        SQL_LIST.add("delete from stat_battery_bat;");
        SQL_LIST.add("delete from stat_battery_pack;");
        SQL_LIST.add("delete from stat_battery_res;");
        SQL_LIST.add("delete from pre_battery_group;");
        SQL_LIST.add("VACUUM;");
    }

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
