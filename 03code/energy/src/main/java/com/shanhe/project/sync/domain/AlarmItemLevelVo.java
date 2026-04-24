package com.shanhe.project.sync.domain;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * 告警级别
 */
@Data
@Accessors(chain = true)
public class AlarmItemLevelVo implements Serializable {
    /**
     * 主键ID
     */
    private Long id;
    /**
     * 告警项ID
     */
    private Long itemId;
    /**
     * 告警级别编号
     */
    private String levelCode;
    /**
     * 级别名称
     */
    private String levelCodeName;
    /**
     * 基准值
     */
    private Double standValue;
    /**
     * 大于值
     */
    private Double hightValue;
    /**
     * 小于值
     */
    private Double lowValue;
    /**
     * 恢复值比对方式1大于2小于3等于
     */
    private Integer recFlag;
    /**
     * 恢复值
     */
    private Double recValue;
    /**
     * 枚举id
     */
    private String dictId;
    /**
     * 枚举名称
     */
    private String dictName;
    /**
     * 告警描述
     */
    private String alarmDesc;
}
