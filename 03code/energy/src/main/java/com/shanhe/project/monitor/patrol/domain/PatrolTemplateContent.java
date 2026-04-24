package com.shanhe.project.monitor.patrol.domain;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * 巡检模板内容
 *
 * @author wjh
 * @since 2025/7/1
 */
@Data
@Accessors(chain = true)
public class PatrolTemplateContent implements Serializable {
    private static final long serialVersionUID = 1L;
    /** 主键 */
    private Long id;
    /** 模板id */
    private Long templateId;
    /** 巡检内容 */
    private String content;
}
