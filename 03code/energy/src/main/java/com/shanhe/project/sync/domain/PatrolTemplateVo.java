package com.shanhe.project.sync.domain;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.List;

/**
 * 巡检清单
 *
 * @author wjh
 * @since 2025/7/2
 */
@Data
@Accessors(chain = true)
public class PatrolTemplateVo implements Serializable {
    /**
     * 设备主键ID
     */
    private Long id;

    /**
     * 设备编号
     */
    private String name;

    private Long timestamp;

    private List<String> childDev;

}
