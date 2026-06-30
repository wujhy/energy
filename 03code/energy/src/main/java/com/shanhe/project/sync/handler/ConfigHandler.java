package com.shanhe.project.sync.handler;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSONObject;
import com.shanhe.common.exception.ServiceException;
import com.shanhe.project.manage.config.domain.Config;
import com.shanhe.project.manage.config.service.IBatteryPackService;
import com.shanhe.project.manage.config.service.IConfigService;
import com.shanhe.project.manage.host.service.IHostService;
import com.shanhe.project.sync.common.ConfigUtil;
import com.shanhe.project.sync.consts.MethodEnum;
import com.shanhe.project.sync.domain.DeviceVo;
import com.shanhe.project.sync.domain.RequestVo;
import com.shanhe.project.sync.domain.ResponseVo;
import com.shanhe.project.sync.service.ClientReportService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * 设备配置同步处理器，负责设备信息的同步、删除和上报
 *
 * @author wjh
 * @since 2026-05-25
 */
@Slf4j
@Service
public class ConfigHandler {

    @Resource
    IConfigService configService;
    @Resource
    IHostService hostService;
    @Resource
    IBatteryPackService batteryPackService;
    @Resource
    private ClientReportService clientReportService;

    /**
     * 同步设备信息
     *
     * @param request 请求信息
     * @return 响应结果
     */
    public ResponseVo synDev(RequestVo request) {
        String msg = null;
        try {
            String contentStr = JSONObject.toJSONString(request.getContent());
            log.debug("同步设备信息: {}", contentStr);
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

    /**
     * 删除设备
     *
     * @param request 请求信息
     * @return 响应结果
     */
    public ResponseVo delDev(RequestVo request) {
        String msg = null;
        try {
            String contentStr = JSONObject.toJSONString(request.getContent());
            log.debug("忽略删除静态默认设备配置: {}", contentStr);
        } catch (Exception e) {
            msg = String.format("删除设备异常：%s", e.getMessage());
            log.error(msg);
        }
        return new ResponseVo(request.getImei(), MethodEnum._8.getDictValue(), request.getBusinessId(), msg);
    }

    /**
     * 主动上报设备配置信息
     *
     * @param request 请求信息
     * @return 响应结果
     */
    public ResponseVo reportSynDev(RequestVo request) {
        String msg = null;
        try {
            if (!clientReportService.canSend()) {
                throw new ServiceException("未与服务端建立连接");
            }
            Config config = configService.selectDefaultConfig();
            config.setPackList(batteryPackService.selectBatteryPackListCache(null));
            clientReportService.uploadDev(config, request.getImei());
        } catch (Exception e) {
            msg = String.format("主动同步设备异常：%s", e.getMessage());
            log.error(msg);
        }
        return new ResponseVo(request.getImei(), request.getMethod(), request.getBusinessId(), msg);
    }
}
