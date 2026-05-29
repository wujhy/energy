package com.shanhe.project.device.alarm.domain;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

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

    /** 电池组编号。 */
    @ExcelProperty("电池组")
    private String packNum;
    /** 单体编号。 */
    @ExcelProperty("单体号")
    private Integer modelNum;

    /** 告警等级描述。 */
    @ExcelProperty("告警等级")
    private String alarmLevelStr;
    /** 告警数据信息。 */
    @ColumnWidth(80)
    @ExcelProperty("告警描述")
    private String dataInfo;

    /** 创建时间。 */
    @ExcelProperty("开始时间")
    private Date createTime;
    /** 更新时间。 */
    @ExcelProperty("结束时间")
    private Date updateTime;
    /** 持续时长描述。 */
    @ExcelProperty("持续时间 秒")
    private String durationStr;

    //  0-是，1-否
    @ExcelProperty("处置状态")
    private String statusStr;

}
