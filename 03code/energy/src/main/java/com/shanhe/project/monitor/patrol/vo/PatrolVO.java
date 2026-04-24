package com.shanhe.project.monitor.patrol.vo;

import com.shanhe.project.monitor.patrol.domain.PatrolContent;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 巡检对象
 *
 * @author wjh
 * @since 2025/5/16
 */
@Data
public class PatrolVO implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 主键 */
    private Long pId;
    /** 用户名 */
    private String userName;
    /** 巡检设备列表 */
    private List<PatrolContent> configList;
    /** 备注 */
    private String remark;
    public Long getpId() {
        return pId;
    }

    public void setpId(Long pId) {
        this.pId = pId;
    }

}
