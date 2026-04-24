package com.shanhe.project.device.config.domain;

import com.shanhe.framework.aspectj.lang.annotation.Excel;
import lombok.Data;

import java.io.Serializable;

/**
 * 设备属性展示
 * 
 * @author wjh
 * @since 2024-12-23
 */
@Data
public class ConfigAttributeListVO implements Serializable
{
    private static final long serialVersionUID = 1L;

    /** 属性编码 */
    @Excel(name = "属性编码")
    private String code;

    /** 属性名 */
    @Excel(name = "属性名")
    private String name;
}
