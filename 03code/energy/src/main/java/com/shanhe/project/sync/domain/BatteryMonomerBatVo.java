package com.shanhe.project.sync.domain;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * 电池单体内阻VO
 *
 * @author wjh
 * @since 2026-05-25
 */
@Data
@Accessors(chain = true)
public class BatteryMonomerBatVo implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 组
     */
    private Integer batNum;
    /**
     * 内阻
     */
    private Double resistance;
}
