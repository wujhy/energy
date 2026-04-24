package com.shanhe.project.energy.capacity.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 测试数据点
 *
 * @author xuxw
 */
@Data
public class DataPoint implements Serializable {
    /**
     * 电压
     */
    private Double voltage;
    /**
     * 电流
     */
    private Double current;
    /**
     * 内阻值（2字节）单位：uΩ
     */
    private Double resistance;

    /**
     * 温度值（2字节）单位：℃
     */
    private Double temperature;

    /**
     * 采集时间
     */
    private Date createTime;

}
