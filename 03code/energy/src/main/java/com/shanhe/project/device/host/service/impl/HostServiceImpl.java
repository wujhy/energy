package com.shanhe.project.device.host.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.shanhe.common.exception.ServiceException;
import com.shanhe.common.utils.CacheUtils;
import com.shanhe.common.utils.DateUtils;
import com.shanhe.framework.comm.CommServer;
import com.shanhe.framework.comm.tcp.client.TcpClient;
import com.shanhe.framework.consts.SysConst;
import com.shanhe.framework.enums.*;
import com.shanhe.framework.comm.tcp.model.DeviceData;
import com.shanhe.framework.comm.tcp.utils.CodingUtil;
import com.shanhe.project.device.alarm.domain.AlarmLog;
import com.shanhe.project.device.alarm.service.IAlarmLogService;
import com.shanhe.project.device.config.domain.Config;
import com.shanhe.project.device.config.service.IConfigService;
import com.shanhe.project.device.opt.cmd.CmdHostService;
import com.shanhe.project.device.opt.service.ControlBattery;
import com.shanhe.project.monitor.server.service.SystemService;
import com.shanhe.project.sync.service.ClientReportService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.shanhe.project.device.host.mapper.HostMapper;
import com.shanhe.project.device.host.domain.Host;
import com.shanhe.project.device.host.service.IHostService;
import org.springframework.transaction.annotation.Transactional;
import oshi.util.Util;

import javax.annotation.Resource;
import java.util.List;
import java.util.Objects;

/**
 * 主机Service业务层处理
 *
 * @author wjh
 * @since 2024-12-23
 */
@Slf4j
@Service
public class HostServiceImpl implements IHostService {
    @Resource
    private HostMapper hostMapper;
    @Resource
    private IConfigService configService;
    @Resource
    private IAlarmLogService alarmLogService;
    @Resource
    private CmdHostService cmdHostService;
    @Resource
    private ControlBattery controlBattery;
    @Resource
    private TcpClient tcpClient;
    @Resource
    private ClientReportService clientReportService;

    // 最大校验次数
    @Value("${job.offlineNum:10}")
    private int maxOffline;

    // 缓存枚举
    CacheKeyEnum hostCache = CacheKeyEnum.HOST;
    CacheKeyEnum resultCache = CacheKeyEnum.RESULT_CX;

    @Override
    public Host getDetail() {
        Object object = CacheUtils.get(hostCache.getCache(), hostCache.getKey());
        if (object == null) {
            return this.updateCache();
        } else {
            return (Host) object;
        }
    }

    @Override
    public Host onlineHost() {
        Host host = this.getDetail();
        if (host == null || !CommServer.isOpen()) {
            return null;
        }
        return host;
    }

    @Override
    public void updateHost(Host hostVO) {
        Host host = this.getDetail();
        if (StrUtil.isNotBlank(host.getImei()) && StrUtil.isNotBlank(hostVO.getImei()) && !StrUtil.equals(host.getImei(), hostVO.getImei())) {
            host.setImei(hostVO.getImei());
        } else {
            BeanUtil.copyProperties(hostVO, host, "hostId");
            host.setName(StrUtil.isNotBlank(host.getName()) ? host.getName() : "");
            host.setContacts(StrUtil.isNotBlank(host.getContacts()) ? host.getContacts() : "");
            host.setPhone(StrUtil.isNotBlank(host.getPhone()) ? host.getPhone() : "");
            if (host.getCreateTime() != null) {
                host.setCreateTimeStr(DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD_HH_MM_SS, host.getCreateTime()));
            }
        }
        hostMapper.updateHost(host);
        // 更新主机缓存
        this.updateCache();
    }

    @Override
    public void updateName(String name) {
        Host host = this.getDetail();
        host.setName(name);
        hostMapper.updateHost(host);
        // 更新主机缓存
        this.updateCache();
    }

    @Override
    public void updateSpaceTime(Integer spaceTime) {
        if (Objects.isNull(spaceTime)) {
            throw new ServiceException("数据上报间隔时间不能为空！");
        }
        // 校验间隔时间
        int maxOffline = this.maxOffline * 60;
        if (maxOffline <= spaceTime) {
            throw new ServiceException(String.format("数据上报间隔时间不能大于离线校验 %s 秒！", maxOffline));
        }

        // 下发指令前校验
        Host host = this.getDetail();
        String keyCache = this.getKeyCache(host.getDeviceType(), TcpCidEnum._D0.getDictValue());
        this.sendCmdValid(keyCache);

        host.setDeviceSpaceTime(spaceTime);

        // 下发指令
        cmdHostService.cmd50(host);

        // 异步监听
        this.sendResponse(keyCache);

        // 更新记录
        hostMapper.updateHost(host);
    }

    @Override
    public void updateCleanLogDays(Integer cleanLogDays) {
        Host host = this.getDetail();
        host.setCleanLogDays(cleanLogDays);
        hostMapper.updateHost(host);
    }

    @Override
    public void updateStorageTime(Integer storageTime) {
        Host host = this.getDetail();
        host.setStorageTime(storageTime);
        hostMapper.updateHost(host);
    }

    @Override
    public void syncSpaceTime() {
        // 下发指令前校验
        Host host = this.getDetail();
        String keyCache = this.getKeyCache(host.getDeviceType(), TcpCidEnum._B3.getDictValue());
        this.sendCmdValid(keyCache);

        // 下发指令
        cmdHostService.cmd63(host);

        // 异步监听
        this.sendResponse(keyCache);
    }

    @Override
    public void syncServerTime(String datetime) {
        // 是否同步本机服务时间
        if (StrUtil.isNotBlank(datetime)) {
            SystemService.syncServerTime(datetime);
            // 休眠
            Util.sleep(500L);
        }

        Host host = this.getDetail();
        // 主机连接状态、已注册，通道已开启
        if (!CommServer.isOpen()) {
            return;
        }

        // 同步主机时间
        cmdHostService.cmd37(host);

        // 同步设备时间
        Config param = new Config();
        param.setType(DeviceTypeEnum._1.getDictValue());
        param.setStatus(YesNoEnum.YES.getDictValue());
        List<Config> list = configService.selectConfigList(param);
        if (list.isEmpty()) {
            return;
        }
        for (Config config : list) {
            // 同步蓄电池时间
            controlBattery.doSynBatteryDate(config);
        }
    }

    @Override
    public void updateIp(Host update) {
        if (StrUtil.isBlank(update.getIp())
                || StrUtil.isBlank(update.getSubIp())
                || StrUtil.isBlank(update.getNetIp())
                || Objects.isNull(update.getPort())) {
            throw new ServiceException("IP及端口不可为空！");
        }

        // 下发指令前校验
        Host host = this.getDetail();

        host.setIp(update.getIp());
        host.setSubIp(update.getSubIp());
        host.setNetIp(update.getNetIp());
        host.setPort(update.getPort());
        host.setDeviceIp(update.getDeviceIp());
        host.setDevicePort(update.getDevicePort());

        // 更新记录
        this.updateHost(host);
    }

    @Override
    public void syncIp() {
        // 下发指令前校验
        Host host = this.getDetail();
        String keyCache = this.getKeyCache(host.getDeviceType(), TcpCidEnum._B1.getDictValue());
        this.sendCmdValid(keyCache);

        // 同步设备IP下发指令
        cmdHostService.cmd61(host);

        // 异步监听
        this.sendResponse(keyCache);
    }

    @Override
    public void updateReportIp(Host update) {
        Host host = this.getDetail();
        if (Objects.equals(update.getNeedReport(), YesNoEnum.YES.getDictValue())) {
            if (StrUtil.isBlank(update.getReportIp())
                    || Objects.isNull(update.getReportPort())) {
                throw new ServiceException("服务器IP及端口不可为空！");
            }

            // 服务器IP及端口
            if (!Objects.equals(update.getNeedReport(), host.getNeedReport())
                    || !StrUtil.equals(update.getReportIp(), host.getReportIp())
                    || !Objects.equals(update.getReportPort(), host.getReportPort())) {
                // 校验是否可以连接
                boolean isTrue = tcpClient.getChannel(update.getReportIp(), update.getReportPort());
                if (!isTrue) {
                    log.error("更新服务端{}:{}错误，无法连接！", update.getReportIp(), update.getReportPort());
//                    throw new ServiceException(String.format("服务端%s:%s无法连接，更新失败！", update.getReportIp(), update.getReportPort()));
                }
                host.setReportIp(update.getReportIp());
                host.setReportPort(update.getReportPort());
            }

            // 数据上报间隔时间
            if (Objects.isNull(update.getSpaceTime())) {
                throw new ServiceException("数据上报间隔时间不可为空！");
            }
            host.setSpaceTime(update.getSpaceTime());

            // 服务端账号密码
            if (StrUtil.isBlank(update.getAccount()) || StrUtil.isBlank(update.getPassword())) {
                throw new ServiceException("账号或密码不可为空！");
            }
            host.setAccount(update.getAccount());
            host.setPassword(update.getPassword());
        }
        host.setNeedReport(update.getNeedReport());

        if (StrUtil.isBlank(update.getIp())
                || StrUtil.isBlank(update.getSubIp())
                || StrUtil.isBlank(update.getNetIp())) {
            throw new ServiceException("本机IP配置信息不可为空！");
        }
        host.setIp(update.getIp());
        host.setIpAddr(update.getIpAddr());
        host.setSubIp(update.getSubIp());
        host.setNetIp(update.getNetIp());
        host.setPort(update.getPort());
        // 更新网卡IP
        SystemService.updateConnectionIp(host);

        // 更新设备云IP
        try {
            cmdHostService.cmd5F(host);
        } catch (Exception ignored) {
        }

        // 获取最新网卡信息
        SystemService.getIp(host);
        this.updateHost(host);

    }

    @Override
    public void syncReportIp() {
        // 下发指令前校验
        Host host = this.getDetail();
        String keyCache = this.getKeyCache(host.getDeviceType(), TcpCidEnum._B2.getDictValue());
        this.sendCmdValid(keyCache);

        // 下发指令
        cmdHostService.cmd62(host);

        // 异步监听
        this.sendResponse(keyCache);
    }

    @Override
    public void online(DeviceData device) {
        Host host = this.getDetail();
        boolean isUpdate = true;
        // 解析注册信息
        if (StrUtil.equals(device.getCid(), TcpCidEnum._88.getDictValue())
                && StrUtil.isNotBlank(device.getInfo()) && device.getInfo().length() >= 20) {
            String num = String.valueOf(CodingUtil.hexParseInt(device.getInfo().substring(0, 2)));
            // 数据库类型 是 integer  实际 model = 00，存储后是 0
            String model = Integer.parseInt(device.getInfo().substring(14, 16)) + "";
            String version = String.format("V%s.%s",
                    CodingUtil.hexParseInt(device.getInfo().substring(16, 18)),
                    CodingUtil.hexParseInt(device.getInfo().substring(18, 20)));

            isUpdate = num.equals(host.getNum())
                    && model.equals(host.getModel())
                    && version.equals(host.getVersion());

            host.setNum(num);
            host.setModel(model);
            host.setVersion(version);
        }
        // 已上线且设备IMEI一样则不处理
        if (Objects.equals(ConnectionStatusEnum._1.getDictValue(), host.getStatus()) && isUpdate) {
            return;
        }

        // 更新上线信息
        host.setStatus(ConnectionStatusEnum._1.getDictValue());
        host.setDeviceType(device.getC0());

        this.updateHost(host);

        // 更新通讯告警
        this.updateHostAlarm(YesNoEnum.YES.getDictValue(), HostAlarmItemEnum._1);

        // 同步上报间隔时间
        cmdHostService.cmd63(host);
        // 下发所有已开启设备指令
    }

    @Override
    public void offline() {
        // 判断缓存的主机状态是否下线
        Host host = this.getDetail();
        if (Objects.equals(host.getStatus(), ConnectionStatusEnum._0.getDictValue())) {
            // 如果本身是下线状态的，只需更新告警
            this.updateHostAlarm(YesNoEnum.NO.getDictValue(), HostAlarmItemEnum._1);
            return;
        }

        // 更新下线
        host.setStatus(ConnectionStatusEnum._0.getDictValue());
        this.updateHost(host);

        // 更新通讯告警
        this.updateHostAlarm(YesNoEnum.NO.getDictValue(), HostAlarmItemEnum._1);

        // 主机下所有设备置为下线
        configService.offlineAll();
    }

    @Override
    public Host updateCache() {
        Host host = hostMapper.getDetail();
        if (host == null) {
            throw new ServiceException("主机配置不存在");
        }
        host.setSoftVersion("V" + SysConst.version);
        CacheUtils.put(hostCache.getCache(), hostCache.getKey(), host);
        clientReportService.join(host, null);
        return host;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void restore() {
        hostMapper.delete();
        hostMapper.inset();

        Host host = hostMapper.getDetail();
        SystemService.getIp(host);
        updateHost(host);

        updateCache();

        // 间隔时间
        try {
            cmdHostService.cmd50(hostMapper.getDetail());
        } catch (Exception e) {
            log.error("更新时间间隔失败！", e);
        }
    }

    /**
     * 更新主机告警项
     *
     * @param status 告警处理状态
     * @param hostAlarm 告警项
     */
    public void updateHostAlarm(Integer status, HostAlarmItemEnum hostAlarm) {
        // 更新告警
        AlarmLog alarmLog = new AlarmLog();
        alarmLog.setConfigId(this.getDetail().getHostId());
        alarmLog.setType(DeviceTypeEnum._0.getDictValue());
        alarmLog.setStatus(status);
        alarmLog.setItemCode(hostAlarm.getCode());
        alarmLog.setDataInfo(hostAlarm.getName());
        alarmLog.setAlarmLevel(hostAlarm.getLevel());
        alarmLogService.insertAlarmLog(alarmLog);
    }

    /**
     * 取得缓存key
     *
     * @param c0 设备类型
     * @param c3 指令回调编号
     * @return key
     */
    private String getKeyCache(Integer c0, String c3) {
        return String.format(resultCache.getKey(), "0", "0", "0", c3);
    }

    /**
     * 下发指令前先校验
     */
    private void sendCmdValid(String keyCache) {
        if (!CommServer.isOpen()) {
            throw new ServiceException("主机未在线，下发指令失败！");
        }

        Object object = CacheUtils.get(resultCache.getCache(), keyCache);
        if (object != null) {
            Integer result = (Integer) object;
            if (Objects.equals(result, -1)) {
                throw new ServiceException("指令已下发，请不要重复操作");
            } else if (Objects.equals(result, 0)) {
                throw new ServiceException("存在指令下发成功记录，请一分钟后再提交请求");
            } else if (Objects.equals(result, 1)) {
                throw new ServiceException("存在指令下发失败记录，请一分钟后再提交请求");
            }
        }

        // 初始结果缓存
        CacheUtils.put(resultCache.getCache(), keyCache, -1);
    }

    /**
     * 下发指令后监听
     *
     * @param keyCache 缓存key
     */
    private void sendResponse(String keyCache) {
        String msg;
        //延迟等待设备响应
        boolean isSuccess = true;
        while (true) {
            Object result = CacheUtils.get(resultCache.getCache(), keyCache);
            if (result == null) {
                isSuccess = false;
                msg = "指令响应超时！";
                break;
            } else {
                if (!Objects.equals(result, -1)) {
                    if (Objects.equals(result, 1)) {
                        isSuccess = false;
                        msg = "指令设置失败！";
                    } else {
                        msg = "指令设置成功！";
                    }
                    // 响应处理完成，清除缓存
                    CacheUtils.remove(resultCache.getCache(), keyCache);
                    break;
                }
            }
            Util.sleep(500L);
        }

        if (!isSuccess) {
            throw new ServiceException(msg);
        }
    }
}
