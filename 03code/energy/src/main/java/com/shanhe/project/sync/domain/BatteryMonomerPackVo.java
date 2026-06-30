package com.shanhe.project.sync.domain;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.List;

/**
 * 电池组内阻数据包VO
 *
 * @author wjh
 * @since 2026-05-25
 */
@Data
@Accessors(chain = true)
public class BatteryMonomerPackVo implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 设备ID */
    private Long devId;
    /** 组 */
    private Integer packNum;
    /** 内阻 */
    private List<BatteryMonomerBatVo> childDev;
}
