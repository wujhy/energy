package com.shanhe.project.device.history.controller;

import java.util.List;
import java.util.Objects;

import com.shanhe.common.utils.poi.ExcelUtil;
import com.shanhe.framework.aspectj.lang.annotation.Log;
import com.shanhe.framework.enums.BusinessType;
import com.shanhe.framework.enums.YesNoEnum;
import com.shanhe.framework.web.controller.BaseController;
import com.shanhe.framework.web.domain.AjaxResult;
import com.shanhe.framework.web.page.TableDataInfo;
import com.shanhe.project.device.config.domain.ConfigAttribute;
import com.shanhe.project.device.config.service.IConfigAttributeService;
import com.shanhe.project.monitor.server.service.SystemService;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;
import com.shanhe.project.device.history.domain.HistoryLog;
import com.shanhe.project.device.history.service.IHistoryLogService;

import javax.annotation.Resource;

/**
 * 设备历史记录Controller
 * 
 * @author wjh
 * @since 2024-12-31
 */
@RestController
@RequestMapping("/device/history")
public class HistoryLogController extends BaseController
{
    private final String prefix = "device/history";

    @Resource
    private IHistoryLogService historyLogService;
    @Resource
    private IConfigAttributeService configAttributeService;

    @GetMapping()
    public String history()
    {
        return String.format("%s/history", prefix);
    }

    @GetMapping("/attribute/{configId}")
    public AjaxResult attribute(@PathVariable Long configId) {
        ConfigAttribute configAttribute = new ConfigAttribute();
        configAttribute.setConfigId(configId);
//        configAttribute.setPack(YesNoEnum.YES.getDictValue());
        configAttribute.setStatus(YesNoEnum.YES.getDictValue());
//        configAttribute.setTrack(YesNoEnum.YES.getDictValue());
        return success(configAttributeService.viewList(configAttribute));
    }

    @GetMapping("/attribute/{configId}/{packNum}")
    public AjaxResult attributePack(@PathVariable Long configId, @PathVariable Integer packNum) {
        ConfigAttribute configAttribute = new ConfigAttribute();
        configAttribute.setConfigId(configId);
        configAttribute.setPackNum(packNum);
//        configAttribute.setPack(YesNoEnum.YES.getDictValue());
        configAttribute.setStatus(YesNoEnum.YES.getDictValue());
//        configAttribute.setTrack(YesNoEnum.YES.getDictValue());
        return success(configAttributeService.viewList(configAttribute));
    }

    /**
     * 查询设备历史记录列表
     */
    @PostMapping("/lastList")
    @ResponseBody
    public AjaxResult lastList(HistoryLog historyLog)
    {
        if (Objects.isNull(historyLog.getConfigId())) {
            return error("设备ID不可为空");
        }
        return success(historyLogService.lastList(historyLog));
    }

    /**
     * 查询设备历史记录列表
     */
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(HistoryLog historyLog)
    {
        startPage();
        List<HistoryLog> list = historyLogService.selectHistoryLogList(historyLog);
        return getDataTable(list);
    }

    /**
     * 查询设备历史记录列表（精简）
     */
    @PostMapping("/simpleList")
    @ResponseBody
    public TableDataInfo simpleList(HistoryLog historyLog)
    {
        return getDataTable(historyLogService.simpleList(historyLog));
    }

    /**
     * 导出设备历史记录列表
     */
    @PostMapping("/export")
    @ResponseBody
    public AjaxResult export(HistoryLog historyLog)
    {
        if (SystemService.isWin()) {
            return error("WINDOWS 暂不支持");
        }
        historyLogService.export(historyLog);
        return success();
    }

    /**
     * 删除设备历史记录
     */
    @Log(title = "设备历史记录", businessType = BusinessType.DELETE)
    @PostMapping( "/remove")
    @ResponseBody
    public AjaxResult remove(String ids)
    {
        historyLogService.deleteHistoryLogByHistoryIds(ids);
        return success();
    }
}
