package com.shanhe.project.device.template.controller;

import java.util.List;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import com.shanhe.framework.aspectj.lang.annotation.Log;
import com.shanhe.framework.enums.BusinessType;
import com.shanhe.project.device.template.domain.TemplateAttribute;
import com.shanhe.project.device.template.service.ITemplateAttributeService;
import com.shanhe.framework.web.controller.BaseController;
import com.shanhe.framework.web.domain.AjaxResult;
import com.shanhe.common.utils.poi.ExcelUtil;
import com.shanhe.framework.web.page.TableDataInfo;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;

/**
 * 模板属性Controller
 * 
 * @author wjh
 * @since 2024-12-23
 */
@Controller
@RequestMapping("/device/template/attribute")
public class TemplateAttributeController extends BaseController
{
    private final String prefix = "device/template/attribute";

    @Resource
    private ITemplateAttributeService templateAttributeService;

    @GetMapping("/{tmplId}")
    public String attribute(@PathVariable("tmplId") Long tmplId, ModelMap mMap)
    {
        mMap.put("tmplId", tmplId);
        return String.format("%s/attribute", prefix);
    }

    /**
     * 查询模板属性列表
     */
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(TemplateAttribute templateAttribute)
    {
        startPage();
        List<TemplateAttribute> list = templateAttributeService.selectTemplateAttributeList(templateAttribute);
        return getDataTable(list);
    }

    /**
     * 导出模板属性列表
     */
    @Log(title = "模板属性", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    @ResponseBody
    public AjaxResult export(TemplateAttribute templateAttribute)
    {
        List<TemplateAttribute> list = templateAttributeService.selectTemplateAttributeList(templateAttribute);
        ExcelUtil<TemplateAttribute> util = new ExcelUtil<>(TemplateAttribute.class);
        return util.exportExcel(list, "模板属性数据");
    }

    /**
     * 导入模板属性列表
     */
    @Log(title = "模板属性", businessType = BusinessType.IMPORT)
    @PostMapping("/import/{tmplId}")
    @ResponseBody
    public AjaxResult importData(@PathVariable("tmplId") Long tmplId, MultipartFile file) throws Exception {
        ExcelUtil<TemplateAttribute> util = new ExcelUtil<>(TemplateAttribute.class);
        List<TemplateAttribute> list = util.importExcel(file.getInputStream());

        return success();
    }

    /**
     * 新增模板属性
     */
    @GetMapping("/add/{tmplId}")
    public String add(@PathVariable("tmplId") Long tmplId, ModelMap mMap)
    {
        mMap.put("tmplId", tmplId);
        return String.format("%s/add", prefix);
    }

    /**
     * 新增保存模板属性
     */
    @Log(title = "模板属性", businessType = BusinessType.INSERT)
    @PostMapping("/add")
    @ResponseBody
    public AjaxResult addSave(TemplateAttribute templateAttribute)
    {
        templateAttributeService.insertTemplateAttribute(templateAttribute);
        return success();
    }

    /**
     * 修改模板属性
     */
    @GetMapping("/edit/{tmplAttrId}")
    public String edit(@PathVariable("tmplAttrId") Long tmplAttrId, ModelMap mMap)
    {
        TemplateAttribute templateAttribute = templateAttributeService.selectTemplateAttributeByTmplAttrId(tmplAttrId);
        mMap.put("templateAttribute", templateAttribute);
        return String.format("%s/edit", prefix);
    }

    @GetMapping("/detail/{tmplAttrId}")
    @ResponseBody
    public AjaxResult detail(@PathVariable("tmplAttrId") Long tmplAttrId)
    {
        return success(templateAttributeService.selectTemplateAttributeByTmplAttrId(tmplAttrId));
    }

    /**
     * 修改保存模板属性
     */
    @Log(title = "模板属性", businessType = BusinessType.UPDATE)
    @PostMapping("/edit")
    @ResponseBody
    public AjaxResult editSave(TemplateAttribute templateAttribute)
    {
        templateAttributeService.updateTemplateAttribute(templateAttribute);
        return success();
    }

    /**
     * 删除模板属性
     */
    @Log(title = "模板属性", businessType = BusinessType.DELETE)
    @PostMapping( "/remove")
    @ResponseBody
    public AjaxResult remove(String ids)
    {
        templateAttributeService.deleteTemplateAttributeByTmplAttrIds(ids);
        return success();
    }
}
