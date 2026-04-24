package com.shanhe.project.sync.domain;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * 电池设备处理
 * @author Administrator
 */
@Data
@Accessors(chain = true)
public class BatteryMonomerBatVo implements Serializable {

    /**
     * 组
     */
    private Integer batNum;
    /**
     * 内阻
     */
    private Double resistance;
}
