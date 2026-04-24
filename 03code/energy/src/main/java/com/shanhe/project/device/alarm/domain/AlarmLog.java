package com.shanhe.project.device.alarm.domain;

import com.shanhe.framework.aspectj.lang.annotation.Excel;
import com.shanhe.framework.web.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;
import java.util.List;

/**
 * 设备历史记录对象 dev_alarm_log
 * 
 * @author wjh
 * @since 2024-12-31
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AlarmLog extends BaseEntity
{
    private static final long serialVersionUID = 1L;
    /** 告警记录id */
    private Long alarmId;
    /** 设备id */
    private Long configId;
    @Excel(name = "设备名称")
    private String configName;
    /** 设备类型 */
    private Integer type;
    /** 包序号 */
    private Integer packNum;
    /** 单体电池编号 */
    private Integer modelNum;
    /** 告警字段 */
    private String itemCode;
    /** 告警等级 */
    @Excel(name = "告警等级 1-一般，2-严重，3-紧急")
    private String alarmLevel;
    /** 持续时间 */
    @Excel(name = "持续时间 秒")
    private Long duration;
    /** 告警信息 */
    @Excel(name = "告警描述")
    private String dataInfo;
    /** 处理状态 0-是，1-否 */
    @Excel(name = "处置状态 0-是，1-否")
    private Integer status;
    /** 是否屏蔽 0-是，1-否 */
    private Integer shied;
    /** 屏蔽时间 */
    private Date shiedTime;
    private String shiedTimeStr;
    @Excel(name = "处理备注")
    private String remark;

    /** 排除的单体电池编号 */
    List<Integer> excludeModelNum;
    /** 包含字段 */
    List<String> includeItemCodes;
    /** 排除字段 */
    List<String> excludeItemCodes;


    // 导出路径
    private String exportPath;
}
