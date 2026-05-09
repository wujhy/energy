package com.shanhe.project.sync.service;

import cn.hutool.core.util.StrUtil;
import com.shanhe.framework.comm.tcp.client.TcpClient;
import com.shanhe.project.device.host.domain.Host;
import com.shanhe.project.device.host.service.IHostService;
import com.shanhe.project.sync.consts.MethodEnum;
import com.shanhe.project.sync.domain.RequestVo;
import com.shanhe.project.sync.domain.ResponseVo;
import com.shanhe.project.sync.handler.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * 客户端接收数据处理
 *
 * @author wjh
 * @since 2025/4/28
 */
@Service
public class ClientDeviceService {

    private static final Logger log = LoggerFactory.getLogger(ClientDeviceService.class);

    @Resource
    private IHostService hostService;
    @Resource
    private ConfigHandler configHandler;
    @Resource
    private HostHandler hostHandler;
    @Resource
    private AttributeHandler attributeHandler;
    @Resource
    private AlarmHandler alarmHandler;
    @Resource
    private BatterySyncHandler batterySyncHandler;
    @Resource
    private TcpClient tcpClient;

    /**
     * tcp解析
     */
    public void readByTcp(RequestVo request) {
        try {
            tcpClient.sendMsg(this.read(request));
        } catch (Exception e) {
            log.error("tcp解析异常", e);
        }
    }

    /**
     * http解析
     */
    public String readByHttp(RequestVo request) {
        // 认证token
        try {
            hostHandler.validToken(request);
        } catch (Exception e) {
            String msg = String.format("执行失败：%s", e.getMessage());
            log.error(msg);
            return new ResponseVo(request.getImei(), request.getMethod(), request.getBusinessId(), 2, msg).toJsonString();
        }

        // 处理请求
        try {
            return this.read(request);
        } catch (Exception e) {
            String msg = String.format("执行失败：%s", e.getMessage());
            log.error(msg);
            return new ResponseVo(request.getImei(), request.getMethod(), request.getBusinessId(), msg).toJsonString();
        }
    }

    public String read(RequestVo request) {
        Host host = hostService.getDetail();
        if (!StrUtil.equals(request.getImei(), host.getImei())) {
            String msg = String.format("与本机设备编码不一致（本机IMEI：%s，请求IMEI：%s），不执行", host.getImei(), request.getImei());
            log.info(msg);
            return new ResponseVo(host.getImei(), request.getMethod(), request.getBusinessId(), msg).toJsonString();
        }
        ResponseVo response;
        MethodEnum methodEnum = MethodEnum.fromCode(request.getMethod());
        switch (methodEnum) {
            case _5:
                // 同步设备信息
                response = configHandler.synDev(request);
                break;
            case _7:
                // 删除设备信息
                response = configHandler.delDev(request);
                break;
            case _11:
                // 修改设备IP
                response = hostHandler.editDevIp(request, host);
                break;
            case _13:
                // 修改设备服务器IP
                response = hostHandler.editServerIp(request, host);
                break;
            case _15:
                // 同步设备时间
                response = hostHandler.sysDevDate(request);
                break;
            case _17:
                // 同步测点参数
                response = attributeHandler.synAlarmConfigItem(request);
                break;
            case _19:
                // 删除测点参数
                response = attributeHandler.delAlarmConfigItem(request);
                break;
            case _21:
                // 同步设备指令
                response = new ResponseVo(host.getImei(), MethodEnum._22.getDictValue(), request.getBusinessId(), "协议同步已废弃");
                break;
            case _23:
                // 删除设备指令
                response = new ResponseVo(host.getImei(), MethodEnum._24.getDictValue(), request.getBusinessId(), "协议同步已废弃");
                break;
            case _25:
                // 屏蔽告警内容
                response = alarmHandler.shieldAlarm(request);
                break;
            case _27:
                // 控制指令
                response = hostHandler.controlDev(request);
                break;
            case _29:
                // 下发升级指令
                response = hostHandler.updateSoft(request);
                break;
            case _34:
                // 同步主机测点参数
                response = attributeHandler.reportSynAlarmConfigItem(request);
                break;
            case _35:
                // 同步设备数据
                response = configHandler.reportSynDev(request);
                break;
            case _43:
                // 同步测试计划
                response = batterySyncHandler.syncBatteryOpt(request);
                break;
            case _44:
                // 同步测试计划
                response = batterySyncHandler.syncBatteryMonomer(request);
                break;
            case _46:
                // 同步测试计划
                response = batterySyncHandler.reportSynBatteryMonomer(request);
                break;
            case _98:
                // 安全认证
                response = hostHandler.getToken(request, host);
                break;
            default:
                response = new ResponseVo(host.getImei(), request.getMethod(), request.getBusinessId(), "未找到对应处理方法");
                break;
        }
        log.debug("请求处理结果：{}", response);
        return response.toJsonString();
    }
}
