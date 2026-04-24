package com.shanhe.project.energy.stat.domain;

import com.shanhe.framework.web.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 电池单体配置信息对象 dev_battery_monomer
 *
 * @author zhoubin
 * @date 2025-07-16
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class DevBatteryMonomer extends BaseEntity {
    private static final long serialVersionUID = 1L;

    /**
     * 蓄电池组ID
     */
    private Long id;

    /**
     * 蓄电池组ID
     */
    private Long packId;

    /**
     * 单体电池编号
     */
    private Integer batNum;

    /**
     * 电池 初装内阻值（ 2 字节）单位：uΩ
     */
    private Integer resistance;

    public DevBatteryMonomer() {
    }

    public DevBatteryMonomer(Long packId, Integer batNum, Integer resistance) {
        this.packId = packId;
        this.batNum = batNum;
        this.resistance = resistance;
    }
}
