package com.shanhe.project.monitor.patrol.domain;

import com.shanhe.framework.web.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import javax.validation.constraints.NotNull;

/**
 * 巡检模板对象
 *
 * @author wjh
 * @since 2025/7/1
 */
@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
public class PatrolTemplate extends BaseEntity {
    private static final long serialVersionUID = 1L;

    /** 主键ID */
    private Long id;
    /** 模板名称 */
    @NotNull(message = "模板名称不能为空")
    private String name;
    /** 状态：0启用，1禁用 */
    @NotNull(message = "状态：0启用，1禁用不能为空")
    private Integer status;
    private Long timestamp;
}
