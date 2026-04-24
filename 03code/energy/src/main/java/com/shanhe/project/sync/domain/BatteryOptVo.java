package com.shanhe.project.sync.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.Date;

/**
 * 电池操作VO
 */
@Data
@Accessors(chain = true)
public class BatteryOptVo implements Serializable {

    /** 立即执行 */
    private Integer isNow;

    /** 设备主键ID，蓄电池ID */
    private Long devId;

    /** 蓄电池组编号 */
    private Integer packNum;

    /** 测试类型1内阻测试2连接条测试，3容量测试，4浮充测试，5备电时长测试 */
    private Integer testType;

    /** 是否开启0否1是 */
    private Integer isEnabled;

    /** 测试时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date testTime;

    /** 截止电压 */
    private Double endVoltage;

    /** 恢复电压 */
    private Double recVoltage;

    /** 间隔天数 */
    private Integer intervalDays;

    /** 执行次数 */
    private Integer execCount;

    /** 放电时长 */
    private Integer dischargeTime;

    /** 操作指令，根据参数配置生成指令 */
    private String optCommand;
}
