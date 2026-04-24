package com.shanhe.project.device.template.mapper;

import java.util.List;

import com.shanhe.common.utils.bean.Dict;
import com.shanhe.project.device.template.domain.Template;

/**
 * 模板Mapper接口
 * 
 * @author wjh
 * @since 2024-12-23
 */
public interface TemplateMapper 
{
    /**
     * 查询模板
     * 
     * @param tmplId 模板主键
     * @return 模板
     */
    Template selectTemplateByTmplId(Long tmplId);

    /**
     * 校验名称是否存在
     *
     * @param template 模板
     * @return 数量
     */
    Long hasName(Template template);

    /**
     * 查询模板列表
     * 
     * @param template 模板
     * @return 模板集合
     */
    List<Template> selectTemplateList(Template template);

    /**
     * 模版下拉列表
     *
     * @return 字典列表
     */
    List<Dict> selectDictList();

    /**
     * 新增模板
     * 
     * @param template 模板
     * @return 结果
     */
    int insertTemplate(Template template);

    /**
     * 修改模板
     * 
     * @param template 模板
     * @return 结果
     */
    int updateTemplate(Template template);

    /**
     * 删除模板
     * 
     * @param tmplId 模板主键
     * @return 结果
     */
    int deleteTemplateByTmplId(Long tmplId);

    /**
     * 批量删除模板
     * 
     * @param tmplIds 需要删除的数据主键集合
     * @return 结果
     */
    int deleteTemplateByTmplIds(String[] tmplIds);
}
