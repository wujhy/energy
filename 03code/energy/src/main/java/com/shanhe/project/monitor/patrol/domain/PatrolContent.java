package com.shanhe.project.monitor.patrol.domain;

import lombok.Data;

import java.io.Serializable;

/**
 * 巡检设备对象
 *
 * @author wjh
 * @since 2025/7/1
 */
@Data
public class PatrolContent implements Serializable {
    private static final long serialVersionUID = 1L;
    /** 主键 */
    private Long id;
    /** 巡检主键 */
    private Long pId;
    private Long templateId;
    private String content;
    /** 巡检结果0正常1异常 */
    private Integer status;

    public Long getpId() {
        return pId;
    }

    public void setpId(Long pId) {
        this.pId = pId;
    }
}
