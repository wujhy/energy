package com.shanhe.project.device.host.service;

import com.shanhe.framework.comm.tcp.model.DeviceData;
import com.shanhe.project.device.host.domain.Host;

/**
 * 主机Service接口
 * 
 * @author wjh
 * @since 2024-12-23
 */
public interface IHostService 
{
    /**
     * 查询主机
     * 
     * @return 主机
     */
    Host getDetail();

    /**
     * 查询主机
     *
     * @return 主机
     */
    Host onlineHost();

    /**
     * 修改主机
     *
     * @param host 主机
     */
    void updateHost(Host host);

    /**
     * 修改主机名
     *
     * @param name 主机
     */
    void updateName(String name);

    /**
     * 修改主机数据上报间隔时间
     *
     * @param spaceTime 间隔时间
     */
    void updateSpaceTime(Integer spaceTime);

    /**
     * 修改主机数据存储间隔时间
     *
     * @param storageTime 存储时间
     */
    void updateStorageTime(Integer storageTime);

    /**
     * 修改主机数据删除时间
     */
    void updateCleanLogDays(Integer cleanLogDays);

    /**
     * 同步服务器时间
     */
    void syncServerTime(String datetime);

    /**
     * 修改主机上报IP
     *
     * @param host 主机
     */
    void updateReportIp(Host host);

    /**
     * 上线
     */
    void online(DeviceData device);

    /**
     * 下线
     */
    void offline();

    /**
     * 更新缓存
     */
    Host updateCache();

    /**
     * 清理数据
     */
    void restore();
}
