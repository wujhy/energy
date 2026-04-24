package com.shanhe.project.device.opt.domain;

import com.shanhe.framework.web.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Map;

/**
 * 通用设备操作日志
 *
 * @author wjh
 * @since 2025/7/9
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class OptLog extends BaseEntity {
    /**
     * 主键
     */
    private Long id;
    /**
     * 设备ID
     */
    private Long configId;
    /**
     * 组序号
     */
    private Integer packNum;
    /**
     * 操作类型
     */
    private Integer type;
    /**
     * 内容参数
     */
    private String content;
    /**
     * 内容参数
     */
    private Map params;
    /**
     * 操作结果
     */
    private Integer result;
    /**
     * 额定容量 单位 A
     */
    private Double batCapacity;

    /**
     * 预估容量 单位 AH
     */
    private Double bcapacity;

    // 放电容量
    private Double dischargeCapacity;

    /**
     * 平均电流 单位 A
     */
    private Double current;

    /** 更新时间 */
    private String createTimeStr;


    ///////////////////////////////////////// 缓存
    // 是否保存
    private boolean isSave = false;
    // 次数
    private int count = 0;


}