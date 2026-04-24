package com.shanhe.project.device.config.controller;

import java.util.List;
import org.springframework.ui.ModelMap;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import com.shanhe.framework.aspectj.lang.annotation.Log;
import com.shanhe.framework.enums.BusinessType;
import com.shanhe.project.device.config.domain.ConfigProtocol;
import com.shanhe.project.device.config.service.IConfigProtocolService;
import com.shanhe.framework.web.controller.BaseController;
import com.shanhe.framework.web.domain.AjaxResult;
import com.shanhe.common.utils.poi.ExcelUtil;
import com.shanhe.framework.web.page.TableDataInfo;

import javax.annotation.Resource;

/**
 * 设备协议Controller
 * 
 * @author wjh
 * @since 2024-12-23
 */
@RestController
@RequestMapping("/device/config/protocol")
public class ConfigProtocolController extends BaseController
{
    private final String prefix = "device/config/protocol";
    // 非模板
    private final Integer template = 1;
    @Resource
    private IConfigProtocolService configProtocolService;

    @GetMapping("/{configId}")
    public String protocol(@PathVariable("configId") Long configId, ModelMap mMap)
    {
        mMap.put("configId", configId);
        return String.format("%s/protocol", prefix);
    }

    /**
     * 查询设备协议列表
     */
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(ConfigProtocol configProtocol)
    {
        configProtocol.setTemplate(template);
        startPage();
        List<ConfigProtocol> list = configProtocolService.selectConfigProtocolList(configProtocol);
        return getDataTable(list);
    }

    /**
     * 导出设备协议列表
     */
    @Log(title = "设备协议", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    @ResponseBody
    public AjaxResult export(ConfigProtocol configProtocol)
    {
        List<ConfigProtocol> list = configProtocolService.selectConfigProtocolList(configProtocol);
        ExcelUtil<ConfigProtocol> util = new ExcelUtil<>(ConfigProtocol.class);
        return util.exportExcel(list, "设备协议数据");
    }

    /**
     * 新增设备协议
     */
    @GetMapping("/add/{configId}")
    public String add(@PathVariable("configId") Long configId, ModelMap mMap)
    {
        mMap.put("configId", configId);
        return String.format("%s/add", prefix);
    }

    /**
     * 新增保存设备协议
     */
    @Log(title = "设备协议", businessType = BusinessType.INSERT)
    @PostMapping("/add")
    @ResponseBody
    public AjaxResult addSave(@RequestBody @Validated ConfigProtocol configProtocol)
    {
        configProtocol.setTemplate(template);
        configProtocolService.insertConfigProtocol(configProtocol);
        return success();
    }

    @GetMapping("/detail/{protocolId}")
    public AjaxResult detail(@PathVariable("protocolId") Long protocolId)
    {
        return success(configProtocolService.selectConfigProtocolByProtocolId(protocolId));
    }

    /**
     * 修改设备协议
     */
    @GetMapping("/edit/{protocolId}")
    public String edit(@PathVariable("protocolId") Long protocolId, ModelMap mmap)
    {
        ConfigProtocol configProtocol = configProtocolService.selectConfigProtocolByProtocolId(protocolId);
        mmap.put("configProtocol", configProtocol);
        return String.format("%s/edit", prefix);
    }

    /**
     * 修改保存设备协议
     */
    @Log(title = "设备协议", businessType = BusinessType.UPDATE)
    @PostMapping("/edit")
    @ResponseBody
    public AjaxResult editSave(@RequestBody @Validated ConfigProtocol configProtocol)
    {
        configProtocolService.updateConfigProtocol(configProtocol);
        return success();
    }

    /**
     * 删除设备协议
     */
    @Log(title = "设备协议", businessType = BusinessType.DELETE)
    @PostMapping( "/remove")
    @ResponseBody
    public AjaxResult remove(String ids)
    {
        configProtocolService.deleteConfigProtocolByProtocolIds(ids);
        return success();
    }
}
