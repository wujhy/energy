package com.shanhe.project.monitor.operlog.domain;

import lombok.Data;

import java.io.Serializable;

/**
 * 升级SQL
 *
 * @author wjh
 * @since 2025/12/2
 */
@Data
public class UpgradeSql implements Serializable {
    private static final long serialVersionUID = 1L;
    /** SQL语句。 */
    private String sql;
}
