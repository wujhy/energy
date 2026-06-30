package com.shanhe.project.manage.alarm.domain;

import com.shanhe.framework.web.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 告警级别
 *
 * @author wjh
 * @since 2026-05-25
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class AlarmLevel extends BaseEntity {
    /** 告警等级ID。 */
    private Long id;

    /** 级别编号 */
    private String levelCode;

    /** 级别名称 */
    private String levelName;

    /** 序号 */
    private Integer sort;

    /** 颜色 */
    private String colour;

    /** 延迟时间(秒) */
    private Long delayTimeMinutes;

    /** 发送短信 */
    private Integer sendSmsOnAlert;
    /** 短信重发间隔（秒） */
    private Integer smsResendInterval;
    /** 短信重发次数 */
    private Integer smsResendTimes;
    /** 解除发送短信 */
    private Integer sendSmsOnClear;

    /** 发送Email */
    private Integer sendEmailOnAlert;
    /** 解除发送Email */
    private Integer sendEmailOnClear;
    /** 邮件重发间隔（秒） */
    private Integer emailResendInterval;
    /** 邮件重发次数 */
    private Integer emailResendTimes;

    /** 拨打电话 */
    private Integer callOnAlert;
    /** 电话重拨间隔（秒） */
    private Integer callResendInterval;
    /** 电话重拨次数 */
    private Integer callResendTimes;

    /** 解除拨打电话 */
    private Integer callOnClear;
}
