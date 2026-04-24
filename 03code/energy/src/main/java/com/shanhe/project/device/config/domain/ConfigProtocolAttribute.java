package com.shanhe.project.device.config.domain;

import com.shanhe.framework.aspectj.lang.annotation.Excel;
import com.shanhe.framework.web.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 设备协议属性映射对象 dev_config_protocol_attribute
 * 
 * @author wjh
 * @since 2024-12-23
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ConfigProtocolAttribute extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 协议属性映射id */
    private Long protocolAttrId;

    /** 设备协议id */
    @Excel(name = "设备协议id")
    private Long protocolId;

    /** 设备属性编码 */
    @Excel(name = "设备属性编码")
    private String attrCode;

    /** 数据进制类型 */
    @Excel(name = "数据进制类型(0十六进制，1十进制，2二进制)")
    private Integer dataType;

    /** 开始位置 */
    @Excel(name = "开始位置")
    private Integer startPoint;

    /** 结束位置 */
    @Excel(name = "结束位置")
    private Integer endPoint;

    /** 解析方式0默认1自定义公式，2指定解析类 */
    @Excel(name = "解析方式0默认1自定义公式，2指定解析类")
    private Integer anyFlag;

    /** 解析数据表达式 */
    @Excel(name = "解析数据表达式")
    private String anyExpress;

    /** 是否解析小数点0否1是 */
    @Excel(name = "是否解析小数点")
    private Integer hasPoint;

    /** 是否补码0否1是 */
    @Excel(name = "是否补码0否1是")
    private Integer isComplement;
}
