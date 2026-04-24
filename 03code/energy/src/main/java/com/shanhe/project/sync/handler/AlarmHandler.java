package com.shanhe.project.sync.handler;

import com.alibaba.fastjson.JSONObject;
import com.shanhe.common.utils.DateUtils;
import com.shanhe.framework.enums.YesNoEnum;
import com.shanhe.project.device.alarm.domain.AlarmLog;
import com.shanhe.project.device.alarm.service.IAlarmLogService;
import com.shanhe.project.sync.consts.MethodEnum;
import com.shanhe.project.sync.domain.RequestVo;
import com.shanhe.project.sync.domain.ResponseVo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * 设备告警处理类
 *
 * @author wjh
 * @since 2025/5/23
 */
@Service
public class AlarmHandler {

    private static final Logger log = LoggerFactory.getLogger(AlarmHandler.class);

    @Resource
    private IAlarmLogService alarmLogService;

    /**
     * 屏蔽告警
     */
    public ResponseVo shieldAlarm(RequestVo request) {
        String msg = null;
        try {
            String contentStr = JSONObject.toJSONString(request.getContent());
            log.debug("屏蔽告警信息：{}", contentStr);
            JSONObject param = JSONObject.parseObject(contentStr);
            AlarmLog alarmLog = new AlarmLog();
            alarmLog.setStatus(YesNoEnum.YES.getDictValue());
            alarmLog.setShied(YesNoEnum.YES.getDictValue());
            alarmLog.setAlarmId(Long.valueOf(param.getString("alarmId")));
            alarmLog.setShiedTimeStr(param.getString("shieldEndTime"));
            alarmLog.setShiedTime(DateUtils.dateTime(DateUtils.YYYY_MM_DD_HH_MM_SS, alarmLog.getShiedTimeStr()));
            alarmLogService.shiedAlarmLog(alarmLog);
        } catch (Exception e) {
            msg = String.format("屏蔽告警信息异常：%s", e.getMessage());
            log.error(msg);
        }
        return new ResponseVo(request.getImei(), MethodEnum._26.getDictValue(), request.getBusinessId(), msg);
    }
}
