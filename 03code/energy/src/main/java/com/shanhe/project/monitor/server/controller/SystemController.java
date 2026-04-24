package com.shanhe.project.monitor.server.controller;

import com.shanhe.framework.web.controller.BaseController;
import com.shanhe.framework.web.domain.AjaxResult;
import com.shanhe.project.monitor.server.service.SystemService;
import org.springframework.web.bind.annotation.*;

/**
 * 服务器设置
 *
 * @author wjh
 * @since 2025/5/6
 */
@RestController
public class SystemController extends BaseController {
    /**
     * 触发升级
     */
    @GetMapping("deploy")
    private AjaxResult deploy() {
        SystemService.deploy();
        return success();
    }

    /**
     * 用来检查服务是否正常
     */
    @GetMapping("deployParam")
    private AjaxResult getParam() {
        return success(SystemService.getParam());
    }

}
