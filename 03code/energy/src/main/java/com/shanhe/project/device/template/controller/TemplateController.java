package com.shanhe.project.device.template.controller;

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.shanhe.project.device.template.domain.TemplateCopyVO;
import com.shanhe.project.device.template.domain.TemplateDataVO;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import com.shanhe.framework.aspectj.lang.annotation.Log;
import com.shanhe.framework.enums.BusinessType;
import com.shanhe.project.device.template.domain.Template;
import com.shanhe.project.device.template.service.ITemplateService;
import com.shanhe.framework.web.controller.BaseController;
import com.shanhe.framework.web.domain.AjaxResult;
import com.shanhe.framework.web.page.TableDataInfo;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;

/**
 * 模板Controller
 *
 * @author wjh
 * @since 2024-12-23
 */
@Controller
@RequestMapping("/device/template")
public class TemplateController extends BaseController
{
    private final String prefix = "device/template";

    @Resource
    private ITemplateService templateService;

    @GetMapping()
    public String template()
    {
        return String.format("%s/template", prefix);
    }

    /**
     * 查询模板列表
     */
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(Template template)
    {
        startPage();
        List<Template> list = templateService.selectTemplateList(template);
        return getDataTable(list);
    }

    /**
     * 导出模板列表
     */
    @Log(title = "设备模板", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    @ResponseBody
    public AjaxResult export(Template template)
    {
        return success(templateService.exportTemplate(template));
//        ExcelUtil<TemplateDataVO> util = new ExcelUtil<>(TemplateDataVO.class);
//        return util.exportExcel(list, "设备模板数据");
    }

    @Log(title = "模板", businessType = BusinessType.INSERT)
    @PostMapping("/copy")
    @ResponseBody
    public AjaxResult copy(TemplateCopyVO templateCopyVO)
    {
        templateService.copyTemplate(templateCopyVO);
        return success();
    }

    @PostMapping("/import")
    @ResponseBody
    public AjaxResult importData(MultipartFile file) throws Exception {
        if (file.isEmpty()) {
            throw new Exception("文件为空！");
        }
        if (file.getOriginalFilename() == null || !file.getOriginalFilename().endsWith(".json")) {
            throw new Exception("文件格式不正确！");
        }
        if (file.getSize() > 1024 * 1024) {
            throw new Exception("文件大小不能超过1MB！");
        }
        try {
            Reader reader = new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8);
            Gson gson = new Gson();
            TypeToken<List<TemplateDataVO>> typeToken = new TypeToken<List<TemplateDataVO>>() {};
            List<TemplateDataVO> list = gson.fromJson(reader, typeToken);
            templateService.importTemplate(list);
        } catch (Exception e) {
            return error("导入失败：" + e.getMessage());
        }

//        ExcelUtil<TemplateDataVO> util = new ExcelUtil<>(TemplateDataVO.class);
//        List<TemplateDataVO> list = util.importExcel(file.getInputStream());
//        templateService.importTemplate(list);
        return success();
    }

    /**
     * 新增模板
     */
    @GetMapping("/add")
    public String add()
    {
        return String.format("%s/add", prefix);
    }

    /**
     * 新增保存模板
     */
    @Log(title = "模板", businessType = BusinessType.INSERT)
    @PostMapping("/add")
    @ResponseBody
    public AjaxResult addSave(Template template)
    {
        templateService.insertTemplate(template);
        return success();
    }

    /**
     * 修改模板
     */
    @GetMapping("/edit/{tmplId}")
    public String edit(@PathVariable("tmplId") Long tmplId, ModelMap mMap)
    {
        Template template = templateService.selectTemplateByTmplId(tmplId);
        mMap.put("template", template);
        return String.format("%s/edit", prefix);
    }

    @GetMapping("/detail/{tmplId}")
    @ResponseBody
    public AjaxResult detail(@PathVariable("tmplId") Long tmplId)
    {
        return success(templateService.selectTemplateByTmplId(tmplId));
    }

    /**
     * 修改保存模板
     */
    @Log(title = "模板", businessType = BusinessType.UPDATE)
    @PostMapping("/edit")
    @ResponseBody
    public AjaxResult editSave(Template template)
    {
        templateService.updateTemplate(template);
        return success();
    }

    /**
     * 删除模板
     */
    @Log(title = "模板", businessType = BusinessType.DELETE)
    @PostMapping( "/remove")
    @ResponseBody
    public AjaxResult remove(String ids)
    {
        templateService.deleteTemplateByTmplIds(ids);
        return success();
    }
}
