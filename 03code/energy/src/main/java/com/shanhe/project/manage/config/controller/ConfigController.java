package com.shanhe.project.manage.config.controller;

import com.shanhe.framework.web.controller.BaseController;
import com.shanhe.framework.web.page.TableDataInfo;
import com.shanhe.project.manage.config.domain.Config;
import com.shanhe.project.manage.config.service.IConfigService;
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

    /** 设备配置服务。 */
    @Resource
    private IConfigService configService;

    /** 查询设备配置列表。 */
    @PostMapping("/list")
    public TableDataInfo list() {
        startPage();
        List<Config> list = configService.selectConfigList();
        return getDataTable(list);
    }

}
