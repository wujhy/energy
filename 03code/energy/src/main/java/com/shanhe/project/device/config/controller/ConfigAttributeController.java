package com.shanhe.project.device.config.controller;

import java.util.List;

import com.google.common.collect.Lists;
import com.shanhe.common.utils.StringUtils;
import com.shanhe.framework.enums.ItemCode;
import com.shanhe.project.system.user.domain.User;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;
import com.shanhe.framework.aspectj.lang.annotation.Log;
import com.shanhe.framework.enums.BusinessType;
import com.shanhe.project.device.config.domain.ConfigAttribute;
import com.shanhe.project.device.config.service.IConfigAttributeService;
import com.shanhe.framework.web.controller.BaseController;
import com.shanhe.framework.web.domain.AjaxResult;
import com.shanhe.common.utils.poi.ExcelUtil;
import com.shanhe.framework.web.page.TableDataInfo;

import javax.annotation.Resource;

/**
 * 设备属性Controller
 *
 * @author wjh
 * @since 2024-12-23
 */
@RestController
@RequestMapping("/device/config/attribute")
public class ConfigAttributeController extends BaseController {
    private final String prefix = "device/config/attribute";

    @Resource
    private IConfigAttributeService configAttributeService;

    @GetMapping("/{configId}")
    public String attribute(@PathVariable("configId") Long configId, ModelMap mMap) {
        mMap.put("configId", configId);
        return String.format("%s/attribute", prefix);
    }

    /**
     * 查询设备属性列表
     */
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(ConfigAttribute configAttribute) {
        User user = getSysUser();
        if (StringUtils.isNull(user)) {
            throw new RuntimeException("服务器超时，请重新登录");
        }
        if (null == user.getSuperAdmin() || !user.getSuperAdmin()) {
            // DTNZGX,DTNZBJ,DTDCWDD,DTFCDYD,DTFCDYG,DTDYJC,DTDCWDBJ,DTDCKL,DTWDCGQGZ,ZFCDYGG,ZFCDYGD,DTLJTGJ,ZWDCGQ1GZ,ZWDCGQ2GZ,ZTDGJ,ZSOCDGJ,ZSOHDGJ
            List<String> itemCodes = Lists.newArrayList(ItemCode.DTNZGX.getCode(), ItemCode.DTNZBJ.getCode(),
                    ItemCode.DTDCWDD.getCode(), ItemCode.DTFCDYD.getCode(), ItemCode.DTFCDYG.getCode(),
                    ItemCode.DTDYJC.getCode(), ItemCode.DTDCWDBJ.getCode(), ItemCode.DTDCKL.getCode(),
                    ItemCode.DTWDCGQGZ.getCode(), ItemCode.ZFCDYGG.getCode(),
                    ItemCode.ZFCDYGD.getCode(), ItemCode.DTLJTGJ.getCode(),
                    ItemCode.ZWDCGQ1GZ.getCode(), ItemCode.ZWDCGQ2GZ.getCode(), ItemCode.ZTDGJ.getCode(),
                    ItemCode.ZSOCDGJ.getCode(), ItemCode.ZSOHDGJ.getCode());
            configAttribute.setExcludeCodes(itemCodes);
        }
        startPage();
        List<ConfigAttribute> list = configAttributeService.selectConfigAttributeList(configAttribute);
        return getDataTable(list);
    }

    /**
     * 导出设备属性列表
     */
    @Log(title = "设备属性", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    @ResponseBody
    public AjaxResult export(ConfigAttribute configAttribute) {
        List<ConfigAttribute> list = configAttributeService.selectConfigAttributeList(configAttribute);
        ExcelUtil<ConfigAttribute> util = new ExcelUtil<>(ConfigAttribute.class);
        return util.exportExcel(list, "设备属性数据");
    }

    /**
     * 新增设备属性
     */
    @GetMapping("/add/{configId}")
    public String add(@PathVariable("configId") Long configId, ModelMap mMap) {
        mMap.put("configId", configId);
        return String.format("%s/add", prefix);
    }

    /**
     * 新增保存设备属性
     */
    @Log(title = "设备属性", businessType = BusinessType.INSERT)
    @PostMapping("/add")
    @ResponseBody
    public AjaxResult addSave(ConfigAttribute configAttribute) {
        configAttributeService.insertConfigAttribute(configAttribute, true);
        return success();
    }

    /**
     * 修改设备属性
     */
    @GetMapping("/edit/{configAttrId}")
    public String edit(@PathVariable("configAttrId") Long configAttrId, ModelMap mMap) {
        ConfigAttribute configAttribute = configAttributeService.selectConfigAttributeByConfigAttrId(configAttrId);
        mMap.put("configAttribute", configAttribute);
        return String.format("%s/edit", prefix);
    }

    /**
     * 修改设备属性
     */
    @GetMapping("/detail/{configAttrId}")
    @ResponseBody
    public AjaxResult detail(@PathVariable("configAttrId") Long configAttrId) {
        return success(configAttributeService.selectConfigAttributeByConfigAttrId(configAttrId));
    }

    /**
     * 修改保存设备属性
     */
    @Log(title = "设备属性", businessType = BusinessType.UPDATE)
    @PostMapping("/edit")
    @ResponseBody
    public AjaxResult editSave(ConfigAttribute configAttribute) {
        configAttributeService.updateConfigAttribute(configAttribute);
        return success();
    }

    /**
     * 删除设备属性
     */
    @Log(title = "设备属性", businessType = BusinessType.DELETE)
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult remove(String ids) {
        configAttributeService.deleteConfigAttributeByConfigAttrIds(ids);
        return success();
    }
}
