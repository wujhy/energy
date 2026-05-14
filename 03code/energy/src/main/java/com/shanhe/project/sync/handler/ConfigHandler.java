package com.shanhe.project.sync.handler;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSONObject;
import com.shanhe.common.exception.ServiceException;
import com.shanhe.project.device.config.domain.Config;
import com.shanhe.project.device.config.service.IConfigService;
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

@Service
public class ConfigHandler {

    private static final Logger log = LoggerFactory.getLogger(ConfigHandler.class);

    @Resource
    IConfigService configService;
    @Resource
    IHostService hostService;
    @Resource
    private ClientReportService clientReportService;

    public ResponseVo synDev(RequestVo request) {
        String msg = null;
        try {
            String contentStr = JSONObject.toJSONString(request.getContent());
            log.debug("sync device info: {}", contentStr);
            DeviceVo device = JSONObject.parseObject(contentStr, DeviceVo.class);
            if (StrUtil.equals(device.getClassId(), "0")) {
                hostService.updateName(device.getDevName());
                return new ResponseVo(request.getImei(), MethodEnum._6.getDictValue(), request.getBusinessId(), msg);
            }
            if (!StrUtil.equals(device.getClassId(), "1")) {
                return new ResponseVo(request.getImei(), MethodEnum._6.getDictValue(), request.getBusinessId(), msg);
            }

            Config config = configService.selectDefaultConfig();
            ConfigUtil.setConfigParam(config, device);
            configService.updatePack(config);
        } catch (Exception e) {
            msg = String.format("同步设备信息异常：%s", e.getMessage());
            log.error(msg);
        }
        return new ResponseVo(request.getImei(), MethodEnum._6.getDictValue(), request.getBusinessId(), msg);
    }

    public ResponseVo delDev(RequestVo request) {
        String msg = null;
        try {
            String contentStr = JSONObject.toJSONString(request.getContent());
            log.debug("ignore delete static default device config: {}", contentStr);
        } catch (Exception e) {
            msg = String.format("删除设备异常：%s", e.getMessage());
            log.error(msg);
        }
        return new ResponseVo(request.getImei(), MethodEnum._8.getDictValue(), request.getBusinessId(), msg);
    }

    public ResponseVo reportSynDev(RequestVo request) {
        String msg = null;
        try {
            if (!clientReportService.canSend()) {
                throw new ServiceException("未与服务端建立连接");
            }
            clientReportService.uploadDev(configService.getCache(), request.getImei());
        } catch (Exception e) {
            msg = String.format("主动同步设备异常：%s", e.getMessage());
            log.error(msg);
        }
        return new ResponseVo(request.getImei(), request.getMethod(), request.getBusinessId(), msg);
    }
}
