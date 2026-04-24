package com.shanhe.project.sync.handler;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSONObject;
import com.shanhe.common.exception.ServiceException;
import com.shanhe.project.device.config.domain.Config;
import com.shanhe.project.device.config.service.IConfigService;
import com.shanhe.project.device.host.domain.Host;
import com.shanhe.project.device.host.service.IHostService;
import com.shanhe.project.sync.common.ConfigUtil;
import com.shanhe.project.sync.consts.MethodEnum;
import com.shanhe.project.sync.domain.DeviceVo;
import com.shanhe.project.sync.domain.RequestVo;
import com.shanhe.project.sync.domain.ResponseVo;
import com.shanhe.project.sync.service.ClientReportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.Objects;

/**
 * 设备处理类
 *
 * @author wjh
 * @since 2025/5/23
 */
@Service
public class ConfigHandler {

    private static final Logger log = LoggerFactory.getLogger(ConfigHandler.class);

    @Resource
    IConfigService configService;
    @Resource
    IHostService hostService;
    @Resource
    private ClientReportService clientReportService;

    /**
     * 同步设备
     *
     * @param request 请求信息
     */
    public ResponseVo synDev(RequestVo request) {
        String msg = null;
        try {
            String contentStr = JSONObject.toJSONString(request.getContent());
            log.debug("同步设备信息：{}", contentStr);
            DeviceVo device = JSONObject.parseObject(contentStr, DeviceVo.class);
            // 主机
            if (StrUtil.equals(device.getClassId(), "0")) {
                this.synHost(device);
                return new ResponseVo(request.getImei(), MethodEnum._6.getDictValue(), request.getBusinessId(), msg);
            }
            // 只同步蓄电池
            if (!StrUtil.equals(device.getClassId(), "1")) {
                return new ResponseVo(request.getImei(), MethodEnum._6.getDictValue(), request.getBusinessId(), msg);
            }

            boolean isUpdate = false;
            // 通过类型、串口、通道号定位唯一设备
            Config config = configService.selectConfigBy(Integer.valueOf(device.getClassId()), device.getParentSn(), device.getSonSn());
            // 设备存在
            if (config != null) {
                // 如果设备ID不一致，删除旧设备，新建
                if (!Objects.equals(config.getConfigId(), device.getDevId())) {
                    // 删除设备
                    configService.deleteConfig(config);
                } else {
                    // ID一致，只需更新
                    isUpdate = true;
                }
            }

            // 设备不存在，通过设备ID再确认设备是否存在
            if (!isUpdate) {
                config = configService.selectConfigByConfigId(device.getDevId());
                // 设备存在，只做更新
                isUpdate = config != null;
            }
            if (config == null) {
                config = new Config();
            }

            // 设置参数
            ConfigUtil.setConfigParam(config, device);

            // 更新、保存设备
            if (isUpdate) {
                configService.updateConfigBySync(config);
            } else {
                configService.insertConfigBySync(config);
            }
        } catch (Exception e) {
            msg = String.format("同步设备信息异常：%s", e.getMessage());
            log.error(msg);
        }
        return new ResponseVo(request.getImei(), MethodEnum._6.getDictValue(), request.getBusinessId(), msg);
    }

    /**
     * 同步主机信息
     *
     * @param device 远程
     */
    private void synHost(DeviceVo device) {
        hostService.updateName(device.getDevName());
    }

    /**
     * 删除设备
     */
    public ResponseVo delDev(RequestVo request) {
        String msg = null;
        try {
            String contentStr = JSONObject.toJSONString(request.getContent());
            log.debug("删除设备信息：{}", contentStr);
            JSONObject param = JSONObject.parseObject(contentStr);
            Long configId = param.getLong("devId");
            Config config = configService.selectConfigByConfigId(configId);
            if (config != null) {
                // 删除设备
                configService.deleteConfig(config);
            } else {
                msg = String.format("设备ID不存在【%s】", configId);
            }
        } catch (Exception e) {
            msg = String.format("删除设备异常：%s", e.getMessage());
            log.error(msg);
        }
        return new ResponseVo(request.getImei(), MethodEnum._8.getDictValue(), request.getBusinessId(), msg);
    }

    /**
     * 同步设备
     */
    public ResponseVo reportSynDev(RequestVo request) {
        String msg = null;
        try {
            if (!clientReportService.canSend()) {
                throw new ServiceException("未与服务端建立连接");
            }
            List<Config> configList = configService.reportConfigList();
            if (configList != null && !configList.isEmpty()) {
                for (Config config : configList) {
                    clientReportService.uploadDev(config, request.getImei());
                }
            }
        } catch (Exception e) {
            msg = String.format("主动同步设备异常：%s", e.getMessage());
            log.error(msg);
        }
        return new ResponseVo(request.getImei(), request.getMethod(), request.getBusinessId(), msg);
    }
}
