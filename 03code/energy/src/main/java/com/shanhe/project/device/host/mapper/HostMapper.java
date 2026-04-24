package com.shanhe.project.device.host.mapper;

import com.shanhe.project.device.host.domain.Host;
import org.apache.ibatis.annotations.Param;

/**
 * 主机Mapper接口
 * 
 * @author wjh
 * @since 2024-12-23
 */
public interface HostMapper 
{
    /**
     * 查询主机
     * 
     * @return 主机
     */
    Host getDetail();

    /**
     * 修改主机
     * 
     * @param host 主机
     * @return 结果
     */
    int updateHost(Host host);

    /**
     * 还原主机
     */
    void delete();

    void inset();
}
