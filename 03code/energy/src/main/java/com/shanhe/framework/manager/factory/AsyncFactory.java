package com.shanhe.framework.manager.factory;

import java.util.TimerTask;
import com.shanhe.common.utils.AddressUtils;
import com.shanhe.common.utils.spring.SpringUtils;
import com.shanhe.project.monitor.operlog.domain.OperLog;
import com.shanhe.project.monitor.operlog.service.IOperLogService;

/**
 * 异步工厂（产生任务用）
 *
 * @author wjh
 * @since 2024/12/19
 */
public class AsyncFactory {
    /**
     * 操作日志记录
     * 
     * @param operLog 操作日志信息
     * @return 任务task
     */
    public static TimerTask recordOpera(final OperLog operLog) {
        return new TimerTask()
        {
            @Override
            public void run()
            {
                // 远程查询操作地点
                operLog.setOperLocation(AddressUtils.getRealAddressByIp(operLog.getOperIp()));
                SpringUtils.getBean(IOperLogService.class).insertOperlog(operLog);
            }
        };
    }
}
