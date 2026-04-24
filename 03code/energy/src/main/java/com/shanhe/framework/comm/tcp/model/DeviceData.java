package com.shanhe.framework.comm.tcp.model;

import lombok.Data;

import java.io.Serializable;

/**
 * 数据驱动
 */
@Data
public class DeviceData implements Serializable
{
    private static final long serialVersionUID = 1L;
    /** 设备型号，这个对接协议处理类指定 */
    private String deviceModel;
    /** 设备类型00主机，01蓄电池，02UPS，03温湿度04空调，05开关06漏水07烟感，08市电监测 */
    private Integer c0;
    /** 端口号 port */
    private Integer c1;
    /** 设备子模块地址 channel */
    private Integer c2;
    /** 指令编号 协议编码 */
    private String c3;
    /** 设备ID（5字节）（无小数）（ID为全数字，大小在0-2^32之间） */
    private String imei;
    /** 命令 */
    private String cid;
    /** 指令内容信息 */
    private String info;
    /** IP */
    private String ip;
    /** 端口 */
    private Integer port;
    /** info长度,16进制字符 */
    private String length;
    /** 数据时间 */
    private Long dataTime;
}
