package com.shanhe.project.sync.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.shanhe.framework.comm.CommServer;
import com.shanhe.framework.comm.tcp.client.TcpClient;
import com.shanhe.framework.consts.SysConst;
import com.shanhe.framework.enums.YesNoEnum;
import com.shanhe.project.device.alarm.domain.AlarmLog;
import com.shanhe.project.device.config.domain.Config;
import com.shanhe.project.device.config.domain.ConfigAttribute;
import com.shanhe.project.device.config.domain.DevBatteryOpt;
import com.shanhe.project.device.host.domain.Host;
import com.shanhe.project.device.host.service.IHostService;
import com.shanhe.project.energy.capacity.vo.PreBatteryGroup;
import com.shanhe.project.energy.stat.domain.DevBatteryMonomer;
import com.shanhe.project.sync.common.AttributeUtil;
import com.shanhe.project.sync.common.ConfigUtil;
import com.shanhe.project.sync.consts.MethodEnum;
import com.shanhe.project.sync.domain.*;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;

/**
 * 客户端上报服务
 *
 * @author wjh
 * @since 2025/5/19
 */
@Service
public class ClientReportService {
    @Resource
    private IHostService hostService;
    @Resource
    private TcpClient tcpClient;

    /**
     * 需要上报判断
     */
    public Boolean needSend() {
        Host host = hostService.getDetail();
        return host != null && Objects.equals(host.getNeedReport(), YesNoEnum.YES.getDictValue());
    }

    /**
     * 上报判断
     */
    public Boolean canSend() {
        return this.needSend() && tcpClient.isOpen();
    }

    /**
     * 取当前主机imei
     */
    public String getImei() {
        // 是否同步、是否开启客户端
        if (!this.canSend()) {
            return null;
        }

        Host host = hostService.getDetail();
        // 主机是否在线、imei是否不为空，设备是否连接
        if (host == null || !CommServer.isOpen()) {
            return null;
        }

        return host.getImei();
    }

    /**
     * 注册
     *
     * @param host 主机信息
     * @param imei 设备
     */
    public void join(Host host, String imei) {
        if (StrUtil.isBlank(imei)) {
            imei = this.getImei();
        }
        if (StrUtil.isBlank(imei)) {
            return;
        }
        // 设备没有上线，不执行注册上报
        try {
            HostVo hostVo = new HostVo();
            hostVo.setDevName(host.getName());
            hostVo.setIp(host.getIp());
            hostVo.setPort(SysConst.port);
            hostVo.setDevType(host.getType());
            hostVo.setMacAddr(host.getMac());
            hostVo.setSoftNum(SysConst.version);
            hostVo.setVersion(host.getVersion());
            tcpClient.sendMsg(new RequestVo(imei, MethodEnum._1.getDictValue(), hostVo).toJsonString());
        } catch (Exception ignored) { }
    }

    /**
     * 心跳
     *
     * @param imei 设备
     */
    public void heartbeat(String imei) {
        tcpClient.sendMsg(new RequestVo(imei, MethodEnum._2.getDictValue(), null).toJsonString());
    }

    /**
     * 上报设备实时记录
     *
     * @param history 历史记录
     * @param imei 设备
     */
    public void uploadData(ConfigHistoryVo history, String imei) {
        tcpClient.sendMsg(new RequestVo(imei, MethodEnum._3.getDictValue(), history).toJsonString());
    }

    /**
     * 上报测试指令结果
     */
    public void updateCmdDebug(String imei, String info) {
        // 是否同步上报、是否已建立通道
        if (!this.canSend()) {
            return;
        }
        Map<String, Object> params = new HashMap<>(1);
        params.put("info", info);
        tcpClient.sendMsg(new RequestVo(imei, MethodEnum._41.getDictValue(), params).toJsonString());
    }

    /**
     * 上报告警记录
     *
     * @param alarmLog 告警记录
     */
    public void uploadAlarm(AlarmLog alarmLog, String imei) {
        if (!this.canSend()) {
            return;
        }

        if (StrUtil.isBlank(imei)) {
            imei = this.getImei();
        }
        if (StrUtil.isBlank(imei)) {
            return;
        }

        tcpClient.sendMsg(new RequestVo(imei, MethodEnum._4.getDictValue(), ArmRecordInfo.of(alarmLog)).toJsonString());
    }

    /**
     * 上报测点数据
     *
     * @param configAttribute 测点数据
     */
    public void uploadAlarmConfigItem(ConfigAttribute configAttribute, String imei) {
        if (StrUtil.isBlank(imei)) {
            imei = this.getImei();
        }
        if (StrUtil.isBlank(imei)) {
            return;
        }

        tcpClient.sendMsg(new RequestVo(imei, MethodEnum._33.getDictValue(), AttributeUtil.uploadItem(configAttribute)).toJsonString());
    }

    /**
     * 上报设备配置
     *
     * @param config 设备配置
     * @param imei 设备
     */
    public void uploadDev(Config config, String imei) {
        if (StrUtil.isBlank(imei)) {
            imei = this.getImei();
        }
        if (StrUtil.isBlank(imei)) {
            return;
        }
        tcpClient.sendMsg(new RequestVo(imei, MethodEnum._36.getDictValue(), ConfigUtil.uploadConfig(config)).toJsonString());
    }

    /**
     * 升级结果
     *
     * @param imei 设备
     * @param softVersion 软件版本
     * @param msg 升级结果
     */
    public void updateSoftResult(String imei, String softVersion, String msg) {
        if (!this.canSend()) {
            return;
        }
        Map<String, Object> result = new HashMap<>(3);
        result.put("msg", msg);
        result.put("status", msg == null ? 1 : 0);
        result.put("softVersion", softVersion);
        tcpClient.sendMsg(new RequestVo(imei, MethodEnum._31.getDictValue(), result).toJsonString());
    }

    /**
     * 上报蓄电池测试
     *
     * @param devBatteryOpt 参数
     */
    public void uploadBatteryOpt(DevBatteryOpt devBatteryOpt) {
        String imei = this.getImei();
        BatteryOptVo optVo = BeanUtil.copyProperties(devBatteryOpt, BatteryOptVo.class);
        optVo.setDevId(devBatteryOpt.getConfigId());
        optVo.setIsNow(YesNoEnum.YES.getDictValue());
        tcpClient.sendMsg(new RequestVo(imei, MethodEnum._42.getDictValue(), optVo).toJsonString());
    }

    /**
     * 上报内阻初装
     */
    public void uploadBatteryMonomer(Long configId, Integer packNum, List<DevBatteryMonomer> devBatteryMonomers, String imei) {
        if (StrUtil.isBlank(imei)) {
            imei = this.getImei();
        }
        if (StrUtil.isBlank(imei)) {
            return;
        }
        BatteryMonomerPackVo packVo = new BatteryMonomerPackVo();
        packVo.setDevId(configId);
        packVo.setPackNum(packNum);
        List<BatteryMonomerBatVo> list = new ArrayList<>();
        devBatteryMonomers.forEach(monomer -> {
            BatteryMonomerBatVo batVo = new BatteryMonomerBatVo();
            batVo.setBatNum(monomer.getBatNum());
            batVo.setResistance(monomer.getResistance().doubleValue());
            list.add(batVo);
        });
        packVo.setChildDev(list);


        tcpClient.sendMsg(new RequestVo(imei, MethodEnum._48.getDictValue(), packVo).toJsonString());
    }

    /**
     * 上报预估容量
     */
    public void uploadPreBatteryGroup(PreBatteryGroup groupVo) {
        String imei = this.getImei();
        groupVo.setDevId(groupVo.getConfigId());
        tcpClient.sendMsg(new RequestVo(imei, MethodEnum._49.getDictValue(), groupVo).toJsonString());
    }
}
