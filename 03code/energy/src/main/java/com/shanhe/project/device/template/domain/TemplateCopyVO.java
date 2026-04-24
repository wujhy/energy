package com.shanhe.project.device.template.domain;

import lombok.Data;

import java.io.Serializable;

/**
 * 模板拷贝
 *
 * @author wjh
 * @since 2025/11/14
 */
@Data
public class TemplateCopyVO implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 模板id */
    private Long tmplId;

    /** 模板名称 */
    private String name;

    /** 设备类型编码 */
    private String typeCode;
}
