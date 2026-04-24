package com.shanhe.project.device.template.domain;

import com.shanhe.framework.aspectj.lang.annotation.Excel;
import com.shanhe.project.device.config.domain.ConfigProtocol;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 模板对象数据
 * 
 * @author wjh
 * @since 2024-12-23
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class TemplateDataVO extends Template
{
    private static final long serialVersionUID = 1L;

    @Excel(name = "属性列表")
    private List<TemplateAttribute> attributeList;

    @Excel(name = "协议列表")
    private List<ConfigProtocol> protocolList;
}
