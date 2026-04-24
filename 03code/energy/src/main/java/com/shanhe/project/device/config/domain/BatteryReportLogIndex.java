package com.shanhe.project.device.config.domain;

import com.shanhe.framework.web.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Map;

/**
 * 蓄电池上报日志
 *
 * @author wjh
 * @since 2025/7/9
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class BatteryReportLogIndex extends BaseEntity {
    /**
     * 主键
     */
    private Long id;
    /**
     * 设备ID
     */
    private Long configId;
    /**
     * 包序号
     */
    private Integer packNum;

    /**
     * 包参数
     */
    private Map<String, Object> packParam;

    /** 是否告警 0-是，1-否 */
    private Integer alarm;
}