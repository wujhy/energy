package com.shanhe.project.scheduled;

import com.shanhe.common.utils.DateUtils;
import com.shanhe.framework.comm.CommServer;
import com.shanhe.framework.enums.*;
import com.shanhe.framework.comm.tcp.utils.CodingUtil;
import com.shanhe.project.device.config.domain.BatteryPack;
import com.shanhe.project.device.config.domain.Config;
import com.shanhe.project.device.config.service.IConfigService;
import com.shanhe.project.device.host.domain.Host;
import com.shanhe.project.device.host.service.IHostService;
import com.shanhe.project.device.opt.cmd.DeviceModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;
import java.util.Objects;

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
        // 是否同步
        if (!isSynBattery) {
            return;
        }
        logger.info("同步蓄电池参数信息start {}", DateUtils.getDate());
        try {
            Host host = hostService.onlineHost();
            // 主机连接状态、已注册，通道已开启
            if (host == null) {
                logger.debug("主机未在线，同步蓄电池参数信息终止");
                return;
            }

            // 已开启蓄电池设备
            Config param = new Config();
            param.setType(DeviceTypeEnum._1.getDictValue());
            param.setStatus(YesNoEnum.YES.getDictValue());
            List<Config> list = configService.selectConfigList(param);
            if (list.isEmpty()) {
                logger.debug("不存在启用的蓄电池设备，同步终止");
                return;
            }

            for (Config config : list) {
                // 蓄电池同步指令下发
                configService.sendBatterySyncCmd(config);
            }
        } catch (Exception e) {
            logger.error("同步蓄电池参数信息异常：{}", e.getMessage());
        } finally {
            logger.info("同步蓄电池参数信息end");
        }
    }

    //    @Scheduled(cron = "${job.cmdSynBatteryStatus}")
    public void cmdDeviceStatus() {
        try {
            Host host = hostService.onlineHost();
            // 主机连接状态、已注册，通道已开启
            if (host == null) {
                logger.debug("主机未在线，同步蓄电池状态终止");
                return;
            }

            // 已开启蓄电池设备
            Config param = new Config();
            param.setType(DeviceTypeEnum._1.getDictValue());
            param.setStatus(YesNoEnum.YES.getDictValue());
            List<Config> list = configService.selectConfigList(param);
            if (list.isEmpty()) {
                logger.debug("不存在启用的蓄电池设备，同步状态终止");
                return;
            }

            for (Config config : list) {
                Config configCache = configService.getCacheBy(config.getType(), config.getPort(), config.getChannel());
                if (configCache != null && configCache.getPackList() != null && !configCache.getPackList().isEmpty()) {
                    for (BatteryPack pack : configCache.getPackList()) {
                        // 协议内容
                        StringBuilder info = new StringBuilder();
                        // 指令头、默认地址、指令编码
                        info.append(TcpCharEnum.HEAD_53.getDictValue());
                        info.append("01").append(BatteryCidEnum._3B.getDictValue()).append("01");
                        // 包序号
                        info.append(CodingUtil.integerToHexString(pack.getPackNum(), 2));
                        // 校验和
                        info.append(CodingUtil.energyCheckSum(info.substring(TcpCharEnum.HEAD_53.getDictValue().length())));
                        // 指令尾
                        info.append(TcpCharEnum.END_0D.getDictValue());
                        // 蓄电池同步状态指令下发
                        CommServer.returnCmd(DeviceModel.getCmd(host, config, info.toString(), TcpCidEnum._54.getDictValue(), BatteryCidEnum._EB.getDictValue()));
                    }
                }
            }
        } catch (Exception e) {
            logger.error("同步蓄电池状态信息异常：{}", e.getMessage());
        } finally {
            logger.info("同步蓄电池状态信息end");
        }
    }
}
