package com.shanhe.common.utils.bean;

import lombok.Data;

/**
 * 字典
 *
 * @author wjh
 * @since 2024/12/19
 */
@Data
public class Dict {

    /** 字典标签 */
    private String dictLabel;

    /** 字典键值 */
    private Object dictValue;

    public Dict() {}

    public Dict(String dictLabel, Object dictValue) {
        this.dictLabel = dictLabel;
        this.dictValue = dictValue;
    }
}
