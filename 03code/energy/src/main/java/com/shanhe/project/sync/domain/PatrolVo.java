package com.shanhe.project.sync.domain;

import com.shanhe.project.monitor.patrol.domain.PatrolContent;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 告警项
 */
@Data
@Accessors(chain = true)
public class PatrolVo implements Serializable {

    /** 主键 */
    private Long pId;
    /** 主机 */
    private String imei;
    /** 用户名 */
    private String userName;
    /** 巡检时间 */
    private Date pDate;
    /** 巡检结果0正常1异常 */
    private Integer pResult;
    /** 备注 */
    private String remark;
    /** 巡检列表 */
    private List<PatrolContent> listData;

    public Long getpId() {
        return pId;
    }

    public void setpId(Long pId) {
        this.pId = pId;
    }

    public Date getpDate() {
        return pDate;
    }

    public void setpDate(Date pDate) {
        this.pDate = pDate;
    }

    public Integer getpResult() {
        return pResult;
    }

    public void setpResult(Integer pResult) {
        this.pResult = pResult;
    }

}
