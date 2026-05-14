package com.shanhe.project.device.config.controller;

import com.shanhe.framework.web.controller.BaseController;
import com.shanhe.framework.web.page.TableDataInfo;
import com.shanhe.project.device.config.domain.Config;
import com.shanhe.project.device.config.service.IConfigService;
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

    @Resource
    private IConfigService configService;

    @PostMapping("/list")
    public TableDataInfo list() {
        startPage();
        List<Config> list = configService.selectConfigList();
        return getDataTable(list);
    }

}
