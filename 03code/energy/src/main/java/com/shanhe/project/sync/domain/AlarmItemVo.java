package com.shanhe.project.sync.domain;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.List;

/**
 * 告警项
 *
 * @author wjh
 * @since 2026-05-25
 */
@Data
@Accessors(chain = true)
public class AlarmItemVo implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 告警项ID
     */
    private Long itemId;
    /**
     * 设备ID
     */
    private Long devId;
    /**
     * 组
     */
    private Integer packNum;
    /**
     * 数据项编号
     */
    private String itemCode;

    /**
     * 数据项名称
     */
    private String itemName;

    /**
     * 数据项类型1开关量2模拟量
     */
    private Integer itemType;

    /**
     * 告警级别1一般告警2紧急告警3严重告警
     */
    private String alarmLevel;

    /**
     * 数据项分组0无，1 电池组，2单体电池
     */
    private Integer groupFlag;

    /**
     * 告警延迟(秒)
     */
    private Long alarmDelay;

    /**
     * 过滤间隔(分钟)
     */
    private Long alarmFilterTime;

    /**
     * 数据项单位
     */
    private String itemUnit;

    /**
     * 数据项小数点
     */
    private Integer itemPoint;

    /**
     * 回差保护值
     */
    private Double protValue;

    /**
     * 状态0不启用1启用
     */
    private Integer status;

    /**
     * 排序
     */
    private Integer sortNum;

    /**
     * 枚类类型
     */
    private String dictType;

    /**
     * 枚举量报警比对方式0等于 1存在，2不存在
     */
    private Integer bjFlag;

    /**
     * 枚举量报警值,用逗号隔开
     */
    private String bjVal;

    /**
     * 单体电池规格开
     */
    private Integer batSinModel;

    /**
     * 是否参与监控0否1是
     */
    private Integer isMonitor;

    /** 原始值0。 */
    private String val0;
    /** 原始值1。 */
    private String val1;
    /** 是否线性转换。 */
    private Integer isLinear=0;
    /** 原始范围最小值。 */
    private Double minOrigRange=0.0;
    /** 目标范围最小值。 */
    private Double minTargetRange;
    /** 原始范围最大值。 */
    private Double maxOrigRange=0.0;
    /** 目标范围最大值。 */
    private Double maxTargetRange;
    /** 线性系数K。 */
    private Double spk=0.0;
    /** 线性偏移B。 */
    private Double spb;
    /**
     * 告警等级
     */
    private List<AlarmItemLevelVo> listLevel;
    /**
     * 备注
     */
    private String remark;

    /** 组态是否展示主页：1-是，0-否 */
    private Integer displayHome;
}
