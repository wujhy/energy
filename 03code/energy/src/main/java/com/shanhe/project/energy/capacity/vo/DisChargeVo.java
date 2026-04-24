package com.shanhe.project.energy.capacity.vo;

import lombok.Data;

import java.util.Date;

/**
 * 放电时间记录
 */
@Data
public class DisChargeVo {
    private Date startTime; //开始时间
    private String status; //状态

    public DisChargeVo(Date startTime, String status) {
        this.startTime = startTime;
        this.status = status;
    }

    public DisChargeVo() {

    }
}
