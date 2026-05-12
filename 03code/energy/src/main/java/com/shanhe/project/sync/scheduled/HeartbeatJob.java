package com.shanhe.project.sync.scheduled;

import cn.hutool.core.util.StrUtil;
import com.shanhe.common.utils.DateUtils;
import com.shanhe.framework.comm.CommServer;
import com.shanhe.framework.comm.tcp.client.TcpClient;
import com.shanhe.framework.enums.YesNoEnum;
import com.shanhe.project.device.host.domain.Host;
import com.shanhe.project.device.host.service.IHostService;
import com.shanhe.project.sync.service.ClientReportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Date;
import java.util.Objects;

/**
 * JSON/TCP 上报平台心跳定时任务。
 */
@Component
@EnableScheduling
public class HeartbeatJob {

    protected static Logger logger = LoggerFactory.getLogger(HeartbeatJob.class);

    @Resource
    private IHostService hostService;
    @Resource
    private TcpClient tcpClient;
    @Resource
    private ClientReportService clientReportService;

    @Scheduled(cron = "${report.heartbeat}")
    public void report() {
        try {
            Host host = hostService.getDetail();
            if (host == null || !Objects.equals(host.getNeedReport(), YesNoEnum.YES.getDictValue())) {
                return;
            }
            if (StrUtil.isBlank(host.getImei()) || !CommServer.isOpen()) {
                logger.info("上报平台心跳，主机不在线不执行");
                return;
            }
            if (!tcpClient.isOpen()) {
                tcpClient.getChannel();
                if (!tcpClient.isOpen()) {
                    logger.info("上报平台心跳，通道未建立不执行");
                    return;
                }
            }

            int updatedMinutes = DateUtils.differentMillsByMillisecond(host.getUpdateTime(), new Date());
            if (updatedMinutes < 5) {
                clientReportService.join(host, host.getImei());
            }

            clientReportService.heartbeat(host.getImei());
        } catch (Exception e) {
            logger.error("上报平台心跳异常：{}", e.getMessage());
        } finally {
            logger.debug("上报平台心跳完成");
        }
    }

    @Scheduled(cron = "${report.register}")
    public void reportRegister() {
        if (!clientReportService.canSend()) {
            return;
        }
        Host host = hostService.onlineHost();
        if (host == null) {
            logger.debug("主机未初始化不执行注册");
            return;
        }
        clientReportService.join(host, host.getImei());
    }
}
