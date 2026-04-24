package com.shanhe.project.device.config.domain;

import com.shanhe.framework.aspectj.lang.annotation.Excel;
import com.shanhe.framework.web.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;
import java.util.List;

/**
 * 设备对象 dev_config
 * 
 * @author wjh
 * @since 2024-12-23
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class Config extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 配置id */
    private Long configId;

    /** 模板id */
    @Excel(name = "模板id")
    private Long tmplId;

    /** 第三方配置id */
    @Excel(name = "第三方配置id")
    private Long deviceId;

    /** 设备名 */
    @Excel(name = "设备名")
    private String name;

    /** 设备类型 */
    @Excel(name = "设备类型")
    private Integer type;

    /** 子设备类型 */
    @Excel(name = "子设备类型")
    private String subType;

    /** 设备类型编码 */
    @Excel(name = "设备类型编码")
    private String typeCode;

    /** 序号 */
    @Excel(name = "序号")
    private Integer sort;

    /** 寄存器地址编号 */
    @Excel(name = "寄存器地址编号")
    private String sensorAddress;

    /** 硬件端口号 */
    @Excel(name = "串口号")
    private Integer port;

    /** 串口类型 0:无效 1: RS485 2: RS232  3: DI，DO  4: AI，AO */
    @Excel(name = "串口类型")
    private Integer portType;

    /** 通道 */
    @Excel(name = "通道号")
    private Integer channel;

    /** 波特率 */
    @Excel(name = "波特率")
    private Integer baudRate;

    /** 数据位 */
    @Excel(name = "数据位")
    private Integer dataBits;

    /** 停止位 0：1位,1：1.5位,2：2位*/
    @Excel(name = "停止位")
    private Integer stopBits;

    /** 奇偶位 0：None,1：Odd,2：Even,3：Mark,4：Space */
    @Excel(name = "奇偶校验位")
    private Integer parityBits;

    /** 间隔时间 */
    @Excel(name = "间隔时间ms")
    private Integer intervalTime;

    /** 协议解析 */
    @Excel(name = "协议解析")
    private Integer protocolType;

    /** 启用状态 0-是，1-否 */
    @Excel(name = "启用状态 0-是，1-否")
    private Integer status;

    /** 在线状态 0-是，1-否 */
    @Excel(name = "在线状态 0-是，1-否")
    private Integer online;

    private Date onlineTime;

    /** 是否告警 0-是，1-否 */
    private Integer alarm;
    /** 扩展 */
    private String extend3;

    /** 历史告警次数 */
    private Long alarmNum;

    /** 电池组 */
    List<BatteryPack> packList;

    @Excel(name = "电池组")
    private String packListJson;

    @Excel(name = "属性")
    private String attrListJson;

    @Excel(name = "协议")
    private String protocolListJson;
}
