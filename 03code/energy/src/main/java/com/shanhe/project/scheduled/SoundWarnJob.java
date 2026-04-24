package com.shanhe.project.scheduled;

import com.shanhe.common.utils.CacheUtils;
import com.shanhe.framework.comm.CommServer;
import com.shanhe.framework.enums.CacheKeyEnum;
import com.shanhe.framework.enums.ConnectionStatusEnum;
import com.shanhe.project.device.alarm.service.IAlarmLogService;
import com.shanhe.project.device.host.domain.Host;
import com.shanhe.project.device.host.service.IHostService;
import com.shanhe.project.device.opt.service.ControlSwitch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Objects;

/**
 * 声光告警
 */
@Component
@EnableScheduling
public class SoundWarnJob {
    protected static Logger logger = LoggerFactory.getLogger(SoundWarnJob.class);
    @Resource
    private IAlarmLogService alarmLogService;
    @Resource
    private ControlSwitch controlSwitch;
    @Resource
    private IHostService hostService;
    @Value("${job.isOpenSoundWarn:false}")
    private boolean isOpenSoundWarn;

    @Value("${job.portSoundWarn:5}")
    private Integer portSoundWarn;

    CacheKeyEnum warnCache = CacheKeyEnum.WARN;

    /**
     * 每隔5秒执行
     */
//    @Scheduled(cron = "${job.checkSoundWarnCron}")
    public void doJob() {
        if (!isOpenSoundWarn) {
            return;
        }
        if (!CommServer.isOpen()) {
            return;
        }
        try {
            // 主机未在线，不处理（主机在线通过心跳或注册）
            Host host = hostService.onlineHost();
            if (host == null) {
                return;
            }

            Integer cacheAlarm = (Integer) CacheUtils.get(warnCache.getCache(), warnCache.getKey());
            Integer isAlarm = alarmLogService.isAlarm();
            // 数据相同，避免重复下发指令
            if (Objects.equals(cacheAlarm, isAlarm)) {
                // 缓存告警记录，缓存存在过期，导致重复下发指令
                CacheUtils.put(warnCache.getCache(), warnCache.getKey(), isAlarm);
                return;
            }
            // 缓存告警记录
            CacheUtils.put(warnCache.getCache(), warnCache.getKey(), isAlarm);
            // 下发指令
            controlSwitch.doControlSwitch(portSoundWarn, isAlarm);
        } catch (Exception e) {
            logger.error("声光告警异常：{}", e.getMessage(), e);
        }
    }
}
