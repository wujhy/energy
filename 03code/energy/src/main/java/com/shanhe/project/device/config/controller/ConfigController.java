package com.shanhe.project.device.config.controller;

import java.util.List;
import java.util.Objects;

import com.shanhe.common.constant.Constants;
import com.shanhe.framework.comm.CommServer;
import com.shanhe.framework.enums.DeviceTypeEnum;
import org.springframework.web.bind.annotation.*;
import com.shanhe.framework.aspectj.lang.annotation.Log;
import com.shanhe.framework.enums.BusinessType;
import com.shanhe.project.device.config.domain.Config;
import com.shanhe.project.device.config.service.IConfigService;
import com.shanhe.framework.web.controller.BaseController;
import com.shanhe.framework.web.domain.AjaxResult;
import com.shanhe.framework.web.page.TableDataInfo;

import javax.annotation.Resource;

/**
 * 设备Controller
 * 
 * @author wjh
 * @since 2024-12-23
 */
@RestController
@RequestMapping("/device/config")
public class ConfigController extends BaseController
{
    private final String prefix = "device/config";

    @Resource
    private IConfigService configService;

    @GetMapping()
    public String config()
    {
        return String.format("%s/config", prefix);
    }

    /**
     * 查询设备列表
     */
    @PostMapping("/list")
    public TableDataInfo list(Config config)
    {
        startPage();
        List<Config> list = configService.selectConfigList(config);
        return getDataTable(list);
    }

//    /**
//     * 导出设备列表
//     */
//    @PostMapping("/export")
//    public AjaxResult export(Config config)
//    {
//        List<Config> list = configService.export(config);
//        ExcelUtil<Config> util = new ExcelUtil<>(Config.class);
//        return util.exportExcel(list, "设备数据");
//    }
//
//    /**
//     * 导入
//     */
//    @PostMapping("/import")
//    public AjaxResult importData(MultipartFile file) throws Exception {
//        ExcelUtil<Config> util = new ExcelUtil<>(Config.class);
//        List<Config> list = util.importExcel(file.getInputStream());
//        configService.importConfig(list);
//        return success();
//    }
//
//    @Log(title = "设备新增", businessType = BusinessType.INSERT)
//    @PostMapping("/add")
//    public AjaxResult addSave(@RequestBody Config config)
//    {
//        return toAjax(configService.insertConfig(config));
//    }
//
//    @GetMapping("/detail/{configId}")
//    public AjaxResult detail(@PathVariable("configId") Long configId)
//    {
//        return success(configService.selectConfigByConfigId(configId));
//    }
//
//    @GetMapping("/edit/{configId}")
//    public String edit(@PathVariable("configId") Long configId, ModelMap mMap)
//    {
//        Config config = configService.selectConfigByConfigId(configId);
//        mMap.put("config", config);
//        return String.format("%s/edit", prefix);
//    }
//
//    @Log(title = "设备编辑", businessType = BusinessType.UPDATE)
//    @PostMapping("/edit")
//    public AjaxResult editSave(@RequestBody Config config)
//    {
//        return toAjax(configService.updateConfig(config));
//    }
//
//    @Log(title = "设备状态", businessType = BusinessType.UPDATE)
//    @GetMapping("/status/{configId}/{status}")
//    public AjaxResult status(@PathVariable("configId") Long configId, @PathVariable("status") Integer status)
//    {
//        return toAjax(configService.updateStatus(configId, status));
//    }
//
//    @Log(title = "删除设备", businessType = BusinessType.DELETE)
//    @PostMapping( "/remove")
//    public AjaxResult remove(String ids)
//    {
//        return toAjax(configService.deleteConfigByConfigIds(ids));
//    }

    @Log(title = "执行指令", businessType = BusinessType.OTHER)
    @PostMapping( "/cmd")
    public AjaxResult cmd(String cmd)
    {
        CommServer.returnCmd(cmd);
        return success();
    }

    @Log(title = "下发存储指令", businessType = BusinessType.OTHER)
    @GetMapping( "/sendProtocol")
    public AjaxResult sendProtocol()
    {
        configService.sendAllProtocolCmd();
        return success();
    }

    @Log(title = "下发串口指令", businessType = BusinessType.OTHER)
    @GetMapping( "/sendPort")
    public AjaxResult sendPort()
    {
        configService.sendAllPortCmd();
        return success();
    }

    @Log(title = "下发电池组同步", businessType = BusinessType.OTHER)
    @GetMapping( "/sendBatterySync/{configId}")
    public AjaxResult sendBatterySync(@PathVariable Long configId)
    {
        Config config = configService.selectConfigByConfigId(Constants.DEFAULT_CONFIG_ID);
        if (config == null || !Objects.equals(config.getType(), DeviceTypeEnum._1.getDictValue())) {
            return error("非蓄电池设备，同步失败！！！");
        }
        configService.sendBatterySyncCmd(config);
        return success();
    }

    @GetMapping( "/sendBatterySync")
    public AjaxResult sendBatterySync()
    {
        return sendBatterySync(Constants.DEFAULT_CONFIG_ID);
    }
}
