package com.shanhe.project.sync.scheduled;

import cn.hutool.core.util.StrUtil;
import com.shanhe.common.utils.DateUtils;
import com.shanhe.framework.comm.CommServer;
import com.shanhe.framework.consts.SysConst;
import com.shanhe.framework.enums.ConnectionStatusEnum;
import com.shanhe.framework.enums.YesNoEnum;
import com.shanhe.framework.comm.tcp.client.TcpClient;
import com.shanhe.project.device.host.domain.Host;
import com.shanhe.project.device.host.service.IHostService;
import com.shanhe.project.sync.domain.HostVo;
import com.shanhe.project.sync.service.ClientReportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.*;

/**
 * 心跳定时任务
 *
 * @author wjh
 * @since 2025/5/20
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

    /**
     * 每分钟发起一次心跳
     */
    @Scheduled(cron = "${report.heartbeat}")
    public void report() {
        try {
            Host host = hostService.getDetail();
            // 是否同步上报
            if (host == null || !Objects.equals(host.getNeedReport(), YesNoEnum.YES.getDictValue())) {
                return;
            }
            // 是否已建立通道
            if (StrUtil.isBlank(host.getImei()) || !CommServer.isOpen()) {
                logger.info("上报平台数据，主机不在线不执行");
                return;
            }
            // 是否已建立通道
            if (!tcpClient.isOpen()) {
                // 未连接则尝试建立
                tcpClient.getChannel();
                // 未能建立，不执行后续操作
                if (!tcpClient.isOpen()) {
                    logger.info("上报平台数据，通道未建立不执行");
                    return;
                }
            }
            // 五分钟内主机信息有更新，则发起注册
            int num = DateUtils.differentMillsByMillisecond(host.getUpdateTime(), new Date());
            if (num < 5) {
                // 注册
                clientReportService.join(host, host.getImei());
            }

            // 心跳
            this.heartbeat(host);
        } catch (Exception e) {
            logger.error("上报平台数据，同步异常：{}", e.getMessage());
        } finally {
            // 退出上报状态
            logger.debug("上报平台数据，同步完成");
        }
    }

    /**
     * 每十分钟发起一次注册
     */
    @Scheduled(cron = "${report.register}")
    public void reportRegister() {
        // 是否同步上报、是否已建立通道
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

    /**
     * 每天执行巡检清单同步
     */
    @Scheduled(cron = "0 40 23 * * ?")
    public void getPatrolTemplate() {
        try {
            clientReportService.getPatrolTemplate();
        } catch (Exception e) {
            logger.error("请求巡检清单异常：{}", e.getMessage());
        }
    }

    /**
     * 心跳
     */
    private void heartbeat(Host host) {
        try {
            clientReportService.heartbeat(host.getImei());
        } catch (Exception e) {
            logger.error("上报平台心跳异常：{}", e.getMessage());
        }
    }
}
