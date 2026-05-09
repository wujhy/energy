package com.shanhe.project.device.config.controller;

import com.shanhe.framework.aspectj.lang.annotation.Log;
import com.shanhe.framework.comm.CommServer;
import com.shanhe.framework.enums.BusinessType;
import com.shanhe.framework.web.controller.BaseController;
import com.shanhe.framework.web.domain.AjaxResult;
import com.shanhe.framework.web.page.TableDataInfo;
import com.shanhe.project.device.config.domain.Config;
import com.shanhe.project.device.config.service.IConfigService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

/**
 * 设备Controller
 *
 * @author wjh
 * @since 2024-12-23
 */
@RestController
@RequestMapping("/device/config")
public class ConfigController extends BaseController {

    private final String prefix = "device/config";

    @Resource
    private IConfigService configService;

    @GetMapping()
    public String config() {
        return String.format("%s/config", prefix);
    }

    @PostMapping("/list")
    public TableDataInfo list(Config config) {
        startPage();
        List<Config> list = configService.selectConfigList(config);
        return getDataTable(list);
    }

    @Log(title = "执行指令", businessType = BusinessType.OTHER)
    @PostMapping("/cmd")
    public AjaxResult cmd(String cmd) {
        CommServer.returnCmd(cmd);
        return success();
    }
}