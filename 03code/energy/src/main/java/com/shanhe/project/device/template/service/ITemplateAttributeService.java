package com.shanhe.project.device.template.service;

import java.util.List;
import com.shanhe.project.device.template.domain.TemplateAttribute;

/**
 * 模板属性Service接口
 * 
 * @author wjh
 * @since 2024-12-23
 */
public interface ITemplateAttributeService 
{
    /**
     * 查询模板属性
     * 
     * @param tmplAttrId 模板属性主键
     * @return 模板属性
     */
    TemplateAttribute selectTemplateAttributeByTmplAttrId(Long tmplAttrId);

    /**
     * 查询模板属性列表
     * 
     * @param templateAttribute 模板属性
     * @return 模板属性集合
     */
    List<TemplateAttribute> selectTemplateAttributeList(TemplateAttribute templateAttribute);

    /**
     * 新增模板属性
     * 
     * @param templateAttribute 模板属性
     * @return 结果
     */
    int insertTemplateAttribute(TemplateAttribute templateAttribute);

    /**
     * 修改模板属性
     * 
     * @param templateAttribute 模板属性
     * @return 结果
     */
    int updateTemplateAttribute(TemplateAttribute templateAttribute);

    /**
     * 批量删除模板属性
     * 
     * @param tmplAttrIds 需要删除的模板属性主键集合
     * @return 结果
     */
    int deleteTemplateAttributeByTmplAttrIds(String tmplAttrIds);

    /**
     * 删除模板属性信息
     * 
     * @param tmplAttrId 模板属性主键
     * @return 结果
     */
    int deleteTemplateAttributeByTmplAttrId(Long tmplAttrId);
}
