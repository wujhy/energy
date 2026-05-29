package com.shanhe.project.collector.battery.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 蓄电池设备状态持久化实体。
 *
 * @author wjh
 * @since 2026-05-29
 */
@Data
public class BatteryDeviceState implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键。 */
    private Long stateId;

    /** 作用域类型（如 module、channel、group）。 */
    private String scopeType;

    /** 作用域标识。 */
    private String scopeKey;

    /** 电池组编号。 */
    private Integer packNum;

    /** 通道名称。 */
    private String channelName;

    /** 单体编号。 */
    private Integer modelNum;

    /** 状态编码。 */
    private String stateCode;

    /** 状态值。 */
    private String stateValue;

    /** 状态等级。 */
    private String stateLevel;

    /** 来源。 */
    private String source;

    /** 来源引用ID。 */
    private String sourceRefId;

    /** 工作模式。 */
    private Integer mode;

    /** 关联操作日志ID。 */
    private Long optLogId;

    /** 首次出现时间。 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date firstSeenTime;

    /** 最近变更时间。 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date lastChangeTime;

    /** 最近更新时间。 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date lastUpdateTime;

    /** 过期时间。 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date expireTime;

    /** 详情。 */
    private String detail;

    /** 备注。 */
    private String remark;
}
