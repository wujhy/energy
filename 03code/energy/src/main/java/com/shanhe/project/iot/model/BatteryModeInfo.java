package com.shanhe.project.iot.model;

import lombok.Data;

import java.io.Serializable;

/**
 * 蓄电池模式
 *
 * @author wjh
 * @since 2025/8/8
 */
@Data
public class BatteryModeInfo implements Serializable
{
    private static final long serialVersionUID = 1L;

    /** 蓄电池主编号，1,2,3,4 */
    private Integer packNum;
    /** 执行结果 0正常 1异常 */
    private Integer result;
    /** 当前模式 0： 无测试 1： 自动编号 6： 内阻测试 10：连接条电阻测试 */
    private Integer mode;
    /** 当前状态 1：开始 0：停止 */
    private Integer status;
    /** 地址 */
    private Integer address;

    /** 最后一个编号，1,2,3,4 */
    private Integer lastPackNum;
    /** 最后一个模式 0： 无测试 1： 自动编号 6： 内阻测试 10：连接条电阻测试 */
    private Integer lastMode;
    /** 最后一个地址 */
    private Integer lastAddress;
}
