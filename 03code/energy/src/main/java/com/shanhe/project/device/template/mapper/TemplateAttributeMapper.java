package com.shanhe.project.device.template.mapper;

import java.util.List;

import com.shanhe.project.device.template.domain.TemplateAttribute;

/**
 * 模板属性Mapper接口
 * 
 * @author wjh
 * @since 2024-12-23
 */
public interface TemplateAttributeMapper 
{
    /**
     * 查询模板属性
     * 
     * @param tmplAttrId 模板属性主键
     * @return 模板属性
     */
    TemplateAttribute selectTemplateAttributeByTmplAttrId(Long tmplAttrId);

    /**
     * 校验名称是否存在
     *
     * @param templateAttribute 模板属性
     * @return 数量
     */
    Long hasName(TemplateAttribute templateAttribute);

    /**
     * 查询模板属性列表
     * 
     * @param templateAttribute 模板属性
     * @return 模板属性集合
     */
    List<TemplateAttribute> selectTemplateAttributeList(TemplateAttribute templateAttribute);

    /**
     * 查询模板属性列表
     *
     * @param tmplId 模板属性
     * @return 模板属性集合
     */
    List<TemplateAttribute> selectByTmplId(Long tmplId);

    /**
     * 新增模板属性
     * 
     * @param templateAttribute 模板属性
     * @return 结果
     */
    int insertTemplateAttribute(TemplateAttribute templateAttribute);

    /**
     * 新增模板属性
     */
    void insertList(List<TemplateAttribute> list);

    /**
     * 修改模板属性
     * 
     * @param templateAttribute 模板属性
     * @return 结果
     */
    int updateTemplateAttribute(TemplateAttribute templateAttribute);

    /**
     * 删除模板属性
     * 
     * @param tmplAttrId 模板属性主键
     * @return 结果
     */
    int deleteTemplateAttributeByTmplAttrId(Long tmplAttrId);

    /**
     * 批量删除模板属性
     * 
     * @param tmplAttrIds 需要删除的数据主键集合
     * @return 结果
     */
    int deleteTemplateAttributeByTmplAttrIds(String[] tmplAttrIds);

    /**
     * 删除模板属性
     *
     * @param tmplId 模板主键
     * @return 结果
     */
    void deleteTemplateAttributeByTmplId(Long tmplId);

    /**
     * 批量删除模板属性
     *
     * @param tmplIds 需要删除的数据主键集合
     * @return 结果
     */
    int deleteTemplateAttributeByTmplIds(String[] tmplIds);
}
