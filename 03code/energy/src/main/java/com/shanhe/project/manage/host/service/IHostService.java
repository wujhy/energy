package com.shanhe.project.manage.host.service;

import com.shanhe.project.manage.host.domain.Host;

import java.util.Map;

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
     *
     * @param cleanLogDays 日志清理天数
     */
    void updateCleanLogDays(Integer cleanLogDays);

    /**
     * 同步服务器时间
     *
     * @param datetime 服务器时间
     */
    void syncServerTime(String datetime);

    /**
     * 修改主机上报IP
     *
     * @param host 主机
     */
    void updateReportIp(Host host);

    /**
     * 更新主机扩展信息
     *
     * @param map 扩展信息
     */
    void updateExtend(Map<String, Object> map);

    /**
     * 获取主机扩展信息
     *
     * @return 扩展信息Map
     */
    Map<String, Object> getExtend();

    /** 上线 */

    /** 下线 */

    /**
     * 更新缓存
     *
     * @return 主机信息
     */
    Host updateCache();

    /** 清理数据 */
    void restore();
}
