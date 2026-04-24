package com.shanhe.project.sync.controller;

import com.shanhe.project.sync.domain.RequestVo;
import com.shanhe.project.sync.service.ClientDeviceService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;

/**
 * 资源数据
 *
 * @author wjh
 * @since 2025/6/16
 */
@Controller
public class SouthDataController {

    @Resource
    ClientDeviceService clientDeviceService;

    @PostMapping("/shim/southData")
    @ResponseBody
    public String southData(@RequestBody RequestVo request) {
        return clientDeviceService.readByHttp(request);
    }


}
