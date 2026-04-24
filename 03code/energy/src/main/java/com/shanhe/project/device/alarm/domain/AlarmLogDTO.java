package com.shanhe.project.device.alarm.domain;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.shanhe.framework.aspectj.lang.annotation.Excel;
import com.shanhe.framework.web.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 设备历史记录对象 dev_alarm_log
 *
 * @author wjh
 * @since 2024-12-31
 */
@Data
@ColumnWidth(20)
public class AlarmLogDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    @ExcelProperty("电池组")
    private String packNum;
    @ExcelProperty("单体号")
    private Integer modelNum;

    @ExcelProperty("告警等级")
    private String alarmLevelStr;
    @ColumnWidth(80)
    @ExcelProperty("告警描述")
    private String dataInfo;

    @ExcelProperty("开始时间")
    private Date createTime;
    @ExcelProperty("结束时间")
    private Date updateTime;
    @ExcelProperty("持续时间 秒")
    private String durationStr;

    //  0-是，1-否
    @ExcelProperty("处置状态")
    private String statusStr;

}
