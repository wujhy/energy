package com.shanhe.project.device.config.domain;

import com.shanhe.framework.aspectj.lang.annotation.Excel;
import com.shanhe.project.sync.domain.AlarmItemLevelVo;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 设备属性展示
 * 
 * @author wjh
 * @since 2024-12-23
 */
@Data
public class ConfigAttributeVO implements Serializable
{
    private static final long serialVersionUID = 1L;

    /** 设备属性id */
    private Long configAttrId;

    /** 设备id */
    private Long configId;

    /** 包序号 */
    @Excel(name = "包序号")
    private Long packNum;

    /** 属性编码 */
    @Excel(name = "属性编码")
    private String code;

    /** 属性名 */
    @Excel(name = "属性名")
    private String name;

    /** 属性单位 */
    @Excel(name = "属性单位")
    private String unit;

    /** 数据类型 */
    @Excel(name = "数据类型 1:开关量 2:模拟量")
    private Integer type;

    /** 序号 */
    @Excel(name = "序号")
    private Integer sort;

    /** 大屏显示 0-是，1-否 */
    @Excel(name = "大屏显示 0-是，1-否")
    private Integer screenDisplay;

    /** 启用状态 0-是，1-否 */
    @Excel(name = "启用状态 0-是，1-否")
    private Integer status;

    /** 是否告警 0-是，1-否 */
    @Excel(name = "是否告警 0-是，1-否")
    private Integer alarm;

    /** 备注 */
    private String remark;

    /** 告警配置列表 */
    List<AlarmItemLevelVo> listLevel;
    /** 属性值 */
    private String value;
    /** 属性值 */
    private String dataInfo;
    /** 值创建时间 */
    private Date valueCreateTime;
    /** 值更新时间 */
    private Date valueUpdateTime;
}
