package com.shanhe.project.device.alarm.domain;

import com.shanhe.framework.web.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class AlarmLevel extends BaseEntity {
    /**
     * $column.columnComment
     */
    private Long id;

    /**
     * 级别编号
     */
    private String levelCode;

    /**
     * 级别名称
     */
    private String levelName;

    /** 序号 */
    private Integer sort;

    /** 颜色 */
    private String colour;

    /**
     * 延迟时间(秒)
     */
    private Long delayTimeMinutes;

    /**
     * 发送短信
     */
    private Integer sendSmsOnAlert;
    private Integer smsResendInterval;
    private Integer smsResendTimes;
    /**
     * 解除发送短信
     */
    private Integer sendSmsOnClear;

    /**
     * 发送Email
     */
    private Integer sendEmailOnAlert;
    /**
     * 解除发送Email
     */
    private Integer sendEmailOnClear;
    private Integer emailResendInterval;
    private Integer emailResendTimes;

    /**
     * 拨打电话
     */
    private Integer callOnAlert;
    private Integer callResendInterval;
    private Integer callResendTimes;

    /**
     * 解除拨打电话
     */
    private Integer callOnClear;
}
