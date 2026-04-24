package com.shanhe.project.sync.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.shanhe.framework.enums.YesNoEnum;
import com.shanhe.project.device.alarm.domain.AlarmLog;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.Objects;

/**
 * 告警记录对象
 *
 * @author wjh
 * @since 2025/5/17
 */
@Data
public class ArmRecordInfo implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 告警记录ID
     */
    private Long alarmId;
    /**
     * 设备主键ID
     */
    private Long devId;
    /**
     * 报警数据项编号
     */
    private String itemCode;
    /**
     * 蓄电池组编号，1,2,3,4
     */
    private Integer packNum;
    /**
     * 模块地址(单体电池编号，设备端口编号)
     */
    private Integer modelNum;

    /**
     * 告警级别1一般告警2紧急告警3严重告警
     */
    private String alarmLevel;
    /**
     * 告警描述
     */
    private String description;
    /**
     * 告警开始时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date startTime;
    /**
     * 告警结束时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date endTime;
    /**
     * 报警持续时间(秒)
     */
    private Long duration;
    /**
     * 报警状态0未处置，1已处置
     */
    private Integer status;
    /**
     * 处理时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date dealTime;
    /**
     * 处置方式 0 自动 1 屏蔽
     */
    private Integer dealMethod;
    /**
     * 屏蔽结束时间yyyy-MM-dd HH:mm:ss
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date shieldEndTime;
    /**
     * 报警处理人员姓名
     */
    private String dealUserName;

    /**
     * 创建实体对象
     */
    public static ArmRecordInfo of(AlarmLog alarm) {
        ArmRecordInfo info = new ArmRecordInfo();
        info.setAlarmId(alarm.getAlarmId());
        info.setDevId(alarm.getConfigId());
        info.setItemCode(alarm.getItemCode());
        info.setPackNum(alarm.getPackNum());
        info.setModelNum(alarm.getModelNum());
        info.setAlarmLevel(alarm.getAlarmLevel());
        info.setDescription(alarm.getDataInfo());
        info.setStartTime(alarm.getCreateTime());
        info.setStatus(Objects.equals(alarm.getStatus(), YesNoEnum.YES.getDictValue()) ?
                YesNoEnum.NO.getDictValue() : YesNoEnum.YES.getDictValue());
        info.setShieldEndTime(alarm.getShiedTime());
        return info;
    }
}
