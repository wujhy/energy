package com.shanhe.project.energy.stat.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.shanhe.framework.web.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 单体内阻变化统计（内阻测试后）对象 stat_battery_res
 *
 * @author zhoubin
 * @date 2025-07-21
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class StatBatteryRes extends BaseEntity {
    private static final long serialVersionUID = 1L;

    /** 统计ID */
    private Long id;

    @JsonFormat(pattern = "yyyy-MM-dd")

    // 设备ID
    private Long configId;

    // 蓄电池组编号，1,2,3,4
    private Integer packNum;

    // 单体电池编号
    private Integer batNum;

    // 内阻值（  2 字节）单位：uΩ
    private Integer resistance;

}
