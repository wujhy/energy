package com.shanhe.project.monitor.operlog.domain;

import lombok.Data;

import java.io.Serializable;

/**
 * @author zhoubin
 * @date 2025/12/2
 */
@Data
public class UpgradeSql implements Serializable {
    private static final long serialVersionUID = 1L;
    private String sql;
}
