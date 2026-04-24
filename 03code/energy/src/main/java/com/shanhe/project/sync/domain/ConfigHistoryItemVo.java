package com.shanhe.project.sync.domain;

import lombok.Data;

import java.io.Serializable;

/**
 * 历史数据
 *
 * @author wjh
 * @since 2025/5/19
 */
@Data
public class ConfigHistoryItemVo implements Serializable {
    private static final long serialVersionUID = 1L;
    /**
     * 设备主键ID
     */
    private String itemCode;
    /**
     * 蓄电池组编号，1,2,3,4
     */
    private String itemValue;

    public ConfigHistoryItemVo() {}

    public ConfigHistoryItemVo(String itemCode, String itemValue) {
        this.itemCode = itemCode;
        this.itemValue = itemValue;
    }
}
