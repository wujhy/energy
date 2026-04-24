package com.shanhe.project.system.dict.domain;

import com.shanhe.framework.aspectj.lang.annotation.Excel;
import com.shanhe.framework.web.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
/**
 * 字典类型表
 *
 * @author wjh
 * @since 2025/6/11
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SysDictType extends BaseEntity {
    private static final long serialVersionUID = 1L;
    /**
     * 字典主键
     */
    @Excel(name = "字典主键")
    private Long dictId;
    /**
     * 字典名称
     */
    @Excel(name = "字典名称")
    private String dictName;
    /**
     * 字典类型
     */
    @Excel(name = "字典类型")
    private String dictType;
    /**
     * 状态（0正常 1停用）
     */
    @Excel(name = "状态", readConverterExp = "0=正常,1=停用")
    private String status;
    /**
     * 是否数据项
     */
    private String isItemData;
}
