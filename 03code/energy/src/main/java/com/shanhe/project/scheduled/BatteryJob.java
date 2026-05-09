package com.shanhe.project.scheduled;

import com.shanhe.common.utils.DateUtils;
import com.shanhe.project.device.config.domain.Config;
import com.shanhe.project.device.config.service.IConfigService;
import com.shanhe.project.device.host.domain.Host;
import com.shanhe.project.device.host.service.IHostService;
import com.shanhe.project.device.opt.cmd.DeviceModel;
import com.shanhe.framework.comm.CommServer;
import com.shanhe.framework.enums.BatteryCidEnum;
import com.shanhe.framework.enums.DeviceTypeEnum;
import com.shanhe.framework.enums.TcpCidEnum;
import com.shanhe.framework.enums.YesNoEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;

/**
 * 蓄电池任务
 *
 * @author wjh
 * @since 2025/4/22
 */
@Component
@EnableScheduling
public class BatteryJob {
    protected static Logger logger = LoggerFactory.getLogger(BatteryJob.class);
    @Value("${job.isSynBattery:false}")
    private Boolean isSynBattery;
    @Resource
    private IHostService hostService;
    @Resource
    private IConfigService configService;

    //    @Scheduled(cron = "${job.cmdSynBattery}")
    public void cmdDevice() {
        if (!isSynBattery) {
            return;
        }
        logger.info("同步蓄电池参数信息start {}", DateUtils.getDate());
        try {
            Host host = hostService.onlineHost();
            if (host == null) {
                logger.debug("主机未在线，同步蓄电池参数信息终止");
                return;
            }

            Config param = new Config();
            param.setType(DeviceTypeEnum._1.getDictValue());
            param.setStatus(YesNoEnum.YES.getDictValue());
            List<Config> list = configService.selectConfigList(param);
            if (list.isEmpty()) {
                logger.debug("不存在启用的蓄电池设备，同步终止");
                return;
            }

            for (Config config : list) {
                configService.sendBatterySyncCmd(config);
            }
        } catch (Exception e) {
            logger.error("同步蓄电池参数信息异常：{}", e.getMessage());
        } finally {
            logger.info("同步蓄电池参数信息end");
        }
    }
}