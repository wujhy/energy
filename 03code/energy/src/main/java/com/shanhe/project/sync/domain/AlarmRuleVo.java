package com.shanhe.project.sync.domain;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * 指令解析规则
 */
@Data
@Accessors(chain = true)
public class AlarmRuleVo implements Serializable {

    /** 解析规则ID */
    private Long ruleId;

    /** 指令模版ID */
    private Long temCmdId;

    /** 告警项ID */
    private Long itemId;

    /** 告警项编号 */
    private String itemCode;

    /** 数据进制类型( 0 十六进制，1 十进制，2 二进制) */
    private Integer dataType;


    /** 是否解析小数点0否1是 */
    private Integer hasPoint;

    /** 开始位置 */
    private Integer startPoint;

    /** 结束位置 */
    private Integer endPoint;

    /**
     * 解析方式0默认1自定义公式，2指定解析类
     */
    private Integer anyFlag;

    /**
     * 是否补码0否1是
     */
    private Integer isComplement;


    /**
     * 解析数据表达式
     */
    private String anyExpress;

}
