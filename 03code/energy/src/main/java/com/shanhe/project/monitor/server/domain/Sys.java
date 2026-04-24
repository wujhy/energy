package com.shanhe.project.monitor.server.domain;

import lombok.Data;

/**
 * 系统相关信息
 *
 * @author wjh
 * @since 2025/6/6
 */
@Data
public class Sys
{
    /**
     * 服务器名称
     */
    private String computerName;

    /**
     * 服务器Ip
     */
    private String computerIp;

    /**
     * 项目路径
     */
    private String userDir;

    /**
     * 操作系统
     */
    private String osName;

    /**
     * 系统架构
     */
    private String osArch;

}
