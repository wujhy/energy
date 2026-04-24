package com.shanhe.project.sync.domain;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.List;

/**
 * 电池设备处理
 * @author Administrator
 */
@Data
@Accessors(chain = true)
public class BatteryMonomerPackVo implements Serializable {

    /**
     * 设备ID
     */
    private Long devId;
    /**
     * 组
     */
    private Integer packNum;
    /**
     * 内阻
     */
    private List<BatteryMonomerBatVo> childDev;
}
