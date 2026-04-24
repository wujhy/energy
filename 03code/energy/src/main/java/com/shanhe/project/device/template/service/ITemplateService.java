package com.shanhe.project.device.template.service;

import java.util.List;

import com.shanhe.common.utils.bean.Dict;
import com.shanhe.project.device.template.domain.Template;
import com.shanhe.project.device.template.domain.TemplateCopyVO;
import com.shanhe.project.device.template.domain.TemplateDataVO;

/**
 * 模板Service接口
 * 
 * @author wjh
 * @since 2024-12-23
 */
public interface ITemplateService 
{
    /**
     * 查询模板
     * 
     * @param tmplId 模板主键
     * @return 模板
     */
    Template selectTemplateByTmplId(Long tmplId);

    /**
     * 查询模板列表
     * 
     * @param template 模板
     * @return 模板集合
     */
    List<Template> selectTemplateList(Template template);

    /**
     * 查询模板下拉列表
     *
     * @return 模板字典集合
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
     * 批量删除模板
     * 
     * @param tmplIds 需要删除的模板主键集合
     * @return 结果
     */
    int deleteTemplateByTmplIds(String tmplIds);

    /**
     * 删除模板信息
     * 
     * @param tmplId 模板主键
     * @return 结果
     */
    /**
     * 删除模板信息
     *
     * @param tmplId 模板主键
     */
    void deleteTemplateByTmplId(Long tmplId);

    /**
     * 导出模板列表
     */
    List<TemplateDataVO> exportTemplate(Template template);

    /**
     * 导入模板数据
     *
     * @param templateList 模板数据列表
     */
    void importTemplate(List<TemplateDataVO> templateList);
    /**
     * 拷贝模板
     *
     * @param templateCopyVO 目标模板
     */
    void copyTemplate(TemplateCopyVO templateCopyVO);
}
