package com.shanhe.project.scheduled;

import com.shanhe.common.utils.CacheUtils;
import com.shanhe.common.utils.DateUtils;
import com.shanhe.framework.comm.CommServer;
import com.shanhe.framework.consts.DeviceCommConst;
import com.shanhe.framework.enums.CacheKeyEnum;
import com.shanhe.framework.enums.YesNoEnum;
import com.shanhe.project.device.config.domain.Config;
import com.shanhe.project.device.config.service.IConfigService;
import com.shanhe.project.device.host.domain.Host;
import com.shanhe.project.device.host.service.IHostService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 定时同步设备在线状态
 *
 * @author wjh
 * @since 2025/4/23
 */
@Slf4j
@Component
@EnableScheduling
public class DeviceOnlineJob {

    @Resource
    private DeviceCommConst deviceCommConst;
    // 最大校验次数
    @Value("${job.offlineNum:10}")
    private int maxOffline;
    @Resource
    private IHostService hostService;
    @Resource
    private IConfigService configService;
    // 主机离线校验次数
    private int offlineHostNum = 0;
    // 设备离线校验次数
    private final Map<Long, Integer> offlineDeviceNum = new HashMap<>();

    //服务是否第一次启动
    private boolean isStart = true;
    //第一次启动时间
    private final static Long SERVER_START_TIME = System.currentTimeMillis();
    //延迟2分钟，设备有可能2分钟后进行重连
    private static final long STARTUP_CHECK_DELAY = TimeUnit.MINUTES.toMillis(2);

    @Scheduled(cron = "${job.deviceOnline}")
    public void cmdDevice() {
        // 串口不处理
        if (deviceCommConst.isSerialPort()) {
            return;
        }

        try {

            log.debug("同步在线状态，开始同步");
            // 主要防止因为程序重启，时间过期导致数据设备状态被认为离线
            // 启动程序时，先要启动IOT采集程序
            if (isStart) {
                Long currentTime = System.currentTimeMillis();
                if (Math.abs(currentTime - SERVER_START_TIME) <= STARTUP_CHECK_DELAY) {
                    return;
                }
                isStart = false;
            }
            // 主机
            Host host = hostService.getDetail();

            // 主机未在线，不处理（主机在线通过心跳或注册）
            if (host == null) {
                log.info("同步在线状态，主机不在线或未注册");
                return;
            }

            // 判断该设备通道是否开启（通道未开启，循环指定次数后下线）
            if (!CommServer.isOpen()) {
                if (offlineHostNum > maxOffline) {
                    log.info("同步在线状态，通道未开启主机下线，offlineHostNum：{}", offlineHostNum);
                    // 主机下线
                    hostService.offline();
                    offlineHostNum = 0;
                }
                offlineHostNum++;
                return;
            } else {
                offlineHostNum = 0;
            }

            // 所有缓存的已开启设备
            List<Config> configList = configService.cacheConfigList();
            if (configList.isEmpty()) {
                log.debug("同步在线状态，无启用的设备");
                return;
            }

            int num;
            Date nowDate = new Date();
            for (Config config : configList) {
                // 上线缓存信息
                String key = String.format(CacheKeyEnum.CONFIG_ONLINE.getKey(), config.getType(), config.getPort(), config.getChannel());
                Object object = CacheUtils.get(key);
                log.info("同步在线状态，设备 {} 缓存 {} 上报时间为 {}", config.getName(), key, object);

                // 无上报数据
                if (object == null) {
                    // 校验次数达到做下线处理
                    num = offlineDeviceNum.get(config.getConfigId()) == null ? 0 : offlineDeviceNum.get(config.getConfigId());
                    if (num > maxOffline) {
                        log.info("同步在线状态，无上报数据，设备 {} 下线 {}", config.getName(), num);
                        configService.offline(config);
                        num = 0;
                    }
                    offlineDeviceNum.put(config.getConfigId(), ++num);
                    continue;
                }

                // 设备属性，在指定时间内未上报，则下线
                Date lastDate = (Date) object;
                num = DateUtils.differentMillsByMillisecond(lastDate, nowDate);
                if (num > maxOffline) {
                    configService.offline(config);
                    log.info("同步在线状态，设备 {} 下线，最后上报时间 {} 分钟，超过 {} 分钟", config.getName(), num, maxOffline);
                    // 超过指定时间没有上线记录，移除缓存
                    CacheUtils.remove(key);
                    continue;
                }

                // 设备为下线，且上报时间在指定时间内，上线
                if (Objects.equals(config.getOnline(), YesNoEnum.NO.getDictValue())) {
                    configService.online(config);
                    log.info("同步在线状态，设备 {} 上线， 最后上报时间 {} 分钟", config.getName(), num);
                }
            }
        } catch (Exception e) {
            log.error("同步在线状态异常：{}", e.getMessage(), e);
        } finally {
            log.debug("同步在线状态，同步完成");
        }
    }
}
