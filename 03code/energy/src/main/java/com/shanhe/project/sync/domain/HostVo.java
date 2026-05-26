package com.shanhe.project.sync.domain;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * 主机信息VO
 *
 * @author wjh
 * @since 2026-05-25
 */
@Data
@Accessors(chain = true)
public class HostVo implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 设备名称
     */
    private String devName;
    /**
     * 本地IP
     */
    private String ip;
    /**
     * 本地端口号
     */
    private Integer port;
    /**
     * MAC地址
     */
    private String macAddr;
    /**
     * 设备类型_2CM03N带屏动环主机
     */
    private String devType;
    /**
     * 软件版本
     */
    private String softNum;
    /**
     * 硬件版本
     */
    private String version;
}
