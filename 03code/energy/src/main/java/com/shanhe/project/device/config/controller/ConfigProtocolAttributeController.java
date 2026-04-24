package com.shanhe.project.device.config.controller;

import java.util.List;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;
import com.shanhe.framework.aspectj.lang.annotation.Log;
import com.shanhe.framework.enums.BusinessType;
import com.shanhe.project.device.config.domain.ConfigProtocolAttribute;
import com.shanhe.project.device.config.service.IConfigProtocolAttributeService;
import com.shanhe.framework.web.controller.BaseController;
import com.shanhe.framework.web.domain.AjaxResult;
import com.shanhe.common.utils.poi.ExcelUtil;
import com.shanhe.framework.web.page.TableDataInfo;

import javax.annotation.Resource;

/**
 * 设备协议属性映射Controller
 * 
 * @author wjh
 * @since 2024-12-23
 */
@RestController
@RequestMapping("/device/config/protocolAttribute")
public class ConfigProtocolAttributeController extends BaseController
{
    private final String prefix = "device/config/protocol";

    @Resource
    private IConfigProtocolAttributeService configProtocolAttributeService;

    @GetMapping("/{protocolId}")
    public String attribute(@PathVariable("protocolId") Long protocolId, ModelMap mMap)
    {
        mMap.put("protocolId", protocolId);
        return String.format("%s/attribute", prefix);
    }

    /**
     * 查询设备协议属性映射列表
     */
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(ConfigProtocolAttribute configProtocolAttribute)
    {
        startPage();
        List<ConfigProtocolAttribute> list = configProtocolAttributeService.selectConfigProtocolAttributeList(configProtocolAttribute);
        return getDataTable(list);
    }

    /**
     * 导出设备协议属性映射列表
     */
    @Log(title = "设备协议属性映射", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    @ResponseBody
    public AjaxResult export(ConfigProtocolAttribute configProtocolAttribute)
    {
        List<ConfigProtocolAttribute> list = configProtocolAttributeService.selectConfigProtocolAttributeList(configProtocolAttribute);
        ExcelUtil<ConfigProtocolAttribute> util = new ExcelUtil<>(ConfigProtocolAttribute.class);
        return util.exportExcel(list, "设备协议属性映射数据");
    }

    /**
     * 新增设备协议属性映射
     */
    @GetMapping("/add")
    public String add()
    {
        return prefix + "/add";
    }

    /**
     * 新增保存设备协议属性映射
     */
    @Log(title = "设备协议属性映射", businessType = BusinessType.INSERT)
    @PostMapping("/add")
    @ResponseBody
    public AjaxResult addSave(ConfigProtocolAttribute configProtocolAttribute)
    {
        configProtocolAttributeService.insertConfigProtocolAttribute(configProtocolAttribute);
        return success();
    }

    /**
     * 修改设备协议属性映射
     */
    @GetMapping("/edit/{protocolAttrId}")
    public String edit(@PathVariable("protocolAttrId") Long protocolAttrId, ModelMap mmap)
    {
        ConfigProtocolAttribute configProtocolAttribute = configProtocolAttributeService.selectConfigProtocolAttributeByProtocolAttrId(protocolAttrId);
        mmap.put("configProtocolAttribute", configProtocolAttribute);
        return prefix + "/edit";
    }

    /**
     * 修改保存设备协议属性映射
     */
    @Log(title = "设备协议属性映射", businessType = BusinessType.UPDATE)
    @PostMapping("/edit")
    @ResponseBody
    public AjaxResult editSave(ConfigProtocolAttribute configProtocolAttribute)
    {
        configProtocolAttributeService.updateConfigProtocolAttribute(configProtocolAttribute);
        return success();
    }

    /**
     * 删除设备协议属性映射
     */
    @Log(title = "设备协议属性映射", businessType = BusinessType.DELETE)
    @PostMapping( "/remove")
    @ResponseBody
    public AjaxResult remove(String ids)
    {
        configProtocolAttributeService.deleteConfigProtocolAttributeByProtocolAttrIds(ids);
        return success();
    }
}
