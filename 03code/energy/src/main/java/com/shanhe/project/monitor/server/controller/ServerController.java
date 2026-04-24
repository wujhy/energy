package com.shanhe.project.monitor.server.controller;

import com.shanhe.framework.web.domain.AjaxResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import com.shanhe.framework.web.controller.BaseController;
import com.shanhe.project.monitor.server.domain.Server;
import org.springframework.web.bind.annotation.RestController;

/**
 * 服务器监控
 *
 * @author wjh
 * @since 2024/12/17
 */
@RestController
@RequestMapping("/monitor/server")
public class ServerController extends BaseController {

    @GetMapping()
    public AjaxResult server() throws Exception
    {
        Server server = new Server();
        server.copyTo();
        return success(server);
    }
}
