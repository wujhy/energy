package com.shanhe.project.device.template.domain;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.shanhe.framework.aspectj.lang.annotation.Excel;
import com.shanhe.framework.web.domain.BaseEntity;

/**
 * 模板对象 dev_template
 * 
 * @author wjh
 * @since 2024-12-23
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class Template extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 模板id */
    @Excel(name = "模板id")
    private Long tmplId;

    /** 模板名称 */
    @Excel(name = "模板名称")
    private String name;

    /** 模板类型 */
    @Excel(name = "模板类型")
    private Integer type;

    /** 子设备类型 */
    @Excel(name = "子设备类型")
    private String subType;

    /** 设备类型编码 */
    @Excel(name = "设备类型编码")
    private String typeCode;

    /** 串口类型 0:无效 1: RS485 2: RS232  3: DI，DO  4: AI，AO */
    @Excel(name = "串口类型")
    private Integer portType;

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

    /** 模板状态 0-启用，1-停用 */
    @Excel(name = "模板状态 0-启用，1-停用")
    private Long status;

    /** 是否内置，0-内置，1-自定义 */
    @Excel(name = "是否内置，0-内置，1-自定义")
    private Long innerFlag;

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("tmplId", getTmplId())
            .append("name", getName())
            .append("type", getType())
            .append("status", getStatus())
            .append("innerFlag", getInnerFlag())
            .append("remark", getRemark())
            .append("createTime", getCreateTime())
            .toString();
    }
}
