package com.shanhe.project.device.config.domain;

import lombok.Data;
import lombok.EqualsAndHashCode;
import com.shanhe.framework.aspectj.lang.annotation.Excel;
import com.shanhe.framework.web.domain.BaseEntity;

import java.util.List;

/**
 * 设备协议对象 dev_config_protocol
 * 
 * @author wjh
 * @since 2024-12-23
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ConfigProtocol extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 协议id */
    private Long protocolId;

    /** 协议编号 */
    @Excel(name = "协议编号")
    private String protocolCode;

    /** 协议类型 1自定义协议 2modbus协议 */
    @Excel(name = "协议类型1自定义协议2modbus协议")
    private Integer protocolType;

    /** 校验算法 */
    private Integer checksumAlgorithm;

    /** 设备id */
    @Excel(name = "设备id")
    private Long configId;

    /** 指令名称 */
    @Excel(name = "指令名称")
    private String cmdName;

    /** 指令类型52设置串口存储指令包，54串口读指令，57读取开关量 */
    @Excel(name = "指令类型52设置串口存储指令包，54串口读指令，57读取开关量")
    private String cmdType;

    @Excel(name = "指令内容")
    private String cmdContent;

    /** 启用状态 0-是，1-否 */
    @Excel(name = "启用状态 0-是，1-否")
    private Integer status;

    /** 模板 0-是，1-否 */
    @Excel(name = "模板 0-是，1-否")
    private Integer template;

    /** 协议属性映射 */
    List<ConfigProtocolAttribute> attributeList;
}
