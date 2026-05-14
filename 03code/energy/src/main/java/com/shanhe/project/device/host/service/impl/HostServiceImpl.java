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
import com.shanhe.project.device.host.domain.Host;
import com.shanhe.project.device.host.mapper.HostMapper;
import com.shanhe.project.device.host.service.IHostService;
import com.shanhe.project.monitor.server.service.SystemService;
import com.shanhe.project.sync.service.ClientReportService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.Objects;

/**
 * 主机Service业务层处理。
 *
 * @author wjh
 * @since 2024-12-23
 */
@Slf4j
@Service
public class HostServiceImpl implements IHostService {
    @Resource
    private HostMapper hostMapper;
    private TcpClient tcpClient;
    @Resource
    private ClientReportService clientReportService;

    @Value("${job.offlineNum:10}")
    private int maxOffline;

    CacheKeyEnum hostCache = CacheKeyEnum.HOST;

    @Override
    public Host getDetail() {
        Object object = CacheUtils.get(hostCache.getCache(), hostCache.getKey());
        if (object == null) {
            return this.updateCache();
        }
        return (Host) object;
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
        if (StrUtil.isNotBlank(host.getImei())
                && StrUtil.isNotBlank(hostVO.getImei())
                && !StrUtil.equals(host.getImei(), hostVO.getImei())) {
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
        this.updateCache();
    }

    @Override
    public void updateName(String name) {
        Host host = this.getDetail();
        host.setName(name);
        hostMapper.updateHost(host);
        this.updateCache();
    }

    @Override
    public void updateSpaceTime(Integer spaceTime) {
        if (Objects.isNull(spaceTime)) {
            throw new ServiceException("数据上报间隔时间不能为空！");
        }
        int maxOffline = this.maxOffline * 60;
        if (maxOffline <= spaceTime) {
            throw new ServiceException(String.format("数据上报间隔时间不能大于离线校验 %s 秒！", maxOffline));
        }

        Host host = this.getDetail();
        host.setDeviceSpaceTime(spaceTime);
        hostMapper.updateHost(host);
        this.updateCache();
    }

    @Override
    public void updateCleanLogDays(Integer cleanLogDays) {
        Host host = this.getDetail();
        host.setCleanLogDays(cleanLogDays);
        hostMapper.updateHost(host);
        this.updateCache();
    }

    @Override
    public void updateStorageTime(Integer storageTime) {
        Host host = this.getDetail();
        host.setStorageTime(storageTime);
        hostMapper.updateHost(host);
        this.updateCache();
    }

    @Override
    public void syncServerTime(String datetime) {
        if (StrUtil.isNotBlank(datetime)) {
            SystemService.syncServerTime(datetime);
        }
    }

    @Override
    public void updateReportIp(Host update) {
        Host host = this.getDetail();
        if (Objects.equals(update.getNeedReport(), YesNoEnum.YES.getDictValue())) {
            if (StrUtil.isBlank(update.getReportIp()) || Objects.isNull(update.getReportPort())) {
                throw new ServiceException("服务器IP及端口不可为空！");
            }

            if (!Objects.equals(update.getNeedReport(), host.getNeedReport())
                    || !StrUtil.equals(update.getReportIp(), host.getReportIp())
                    || !Objects.equals(update.getReportPort(), host.getReportPort())) {
                boolean isTrue = tcpClient.getChannel(update.getReportIp(), update.getReportPort());
                if (!isTrue) {
                    log.error("更新服务端{}:{}错误，无法连接！", update.getReportIp(), update.getReportPort());
                }
                host.setReportIp(update.getReportIp());
                host.setReportPort(update.getReportPort());
            }

            if (Objects.isNull(update.getSpaceTime())) {
                throw new ServiceException("数据上报间隔时间不可为空！");
            }
            host.setSpaceTime(update.getSpaceTime());

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

        SystemService.updateConnectionIp(host);
        SystemService.getIp(host);
        this.updateHost(host);
    }



    @Override
    public Host updateCache() {
        Host host = hostMapper.getDetail();
        if (host == null) {
            throw new ServiceException("主机配置不存在！");
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
    }

    /**
     * 更新主机告警项。
     */
}
