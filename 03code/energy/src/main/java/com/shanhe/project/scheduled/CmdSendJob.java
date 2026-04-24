package com.shanhe.project.scheduled;

import com.shanhe.framework.comm.CommServer;
import com.shanhe.framework.enums.ConnectionStatusEnum;
import com.shanhe.project.device.config.service.IConfigService;
import com.shanhe.project.device.host.domain.Host;
import com.shanhe.project.device.host.service.IHostService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Objects;

/**
 * 定时执行设备指令下发
 *
 * @author wjh
 * @since 2025/4/8
 */
@Component
@EnableScheduling
public class CmdSendJob {

    protected static Logger logger = LoggerFactory.getLogger(CmdSendJob.class);

    @Resource
    private IHostService hostService;
    @Resource
    private IConfigService configService;

//    @Scheduled(cron = "${job.cmdSend}")
    public void cmdDevice() {
        try {
            logger.info("同步协议指令，开始同步");
            Host host = hostService.onlineHost();
            // 主机连接状态、已注册，通道已开启
            if (host == null) {
                logger.debug("同步协议指令，主机未注册或通道未开启");
                return;
            }

            // 所有已开启设备发送指令
            configService.sendAllStorageCmd();
        } catch (Exception e) {
            logger.error("同步协议指令异常：{}", e.getMessage());
        } finally {
            logger.info("同步协议指令，同步完成");
        }
    }
}
