package com.shanhe.project.device.config.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.shanhe.common.utils.DateUtils;
import com.shanhe.framework.web.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
* 【蓄电池测试操作参数】对象 dev_battery_opt
*
* @author wjh
* @since 2025/5/7
*/
@Data
@EqualsAndHashCode(callSuper = true)
public class DevBatteryOpt extends BaseEntity {
    private static final long serialVersionUID = 1L;

    /** 日志记录ID */
    private Long logId;

    /** 测试蓄电池ID */
    private Long optId;


    /** 设备主键ID，蓄电池ID */
    private Long configId;

    /** 蓄电池组编号 */
    private Integer packNum;

    /** 测试类型1内阻测试，2连接条测试，3容量测试，4浮充测试，5备电时长测试，6.单体内阻测试 */
    private Integer testType;

    /** 是否开启0否1是 */
    private Integer isEnabled;

    /** 测试时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date testTime;
    private String testTimeStr;
    private String replaceTime;

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
    /** 单体编号 */
    private Integer modelNum;

    /** 操作指令，根据参数配置生成指令 */
    private String optCommand;
    /** 是否来源同步 */
    private Boolean isSync;

    public String getTestTimeStr() {
        if (this.testTime != null) {
            this.testTimeStr = DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD_HH_MM_SS, this.testTime);
        }
        return testTimeStr;
    }
}
