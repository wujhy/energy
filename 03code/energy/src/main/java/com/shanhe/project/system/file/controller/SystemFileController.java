package com.shanhe.project.system.file.controller;

import com.shanhe.common.constant.Constants;
import com.shanhe.framework.web.controller.BaseController;
import com.shanhe.framework.web.domain.AjaxResult;
import com.shanhe.project.monitor.server.service.SystemService;
import com.shanhe.project.system.file.domain.SysLogFile;
import com.shanhe.project.system.file.service.ISysFileService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.List;

/**
 * @author zhoubin
 * @date 2025/10/20
 */
@Controller
@RequestMapping("/file")
public class SystemFileController extends BaseController {
    @Resource
    private ISysFileService sysFileService;

    @GetMapping("/listUsbDisk")
    @ResponseBody
    public AjaxResult listUsbDisk() throws IOException {
        if (SystemService.isWin()) {
            return AjaxResult.error("暂不支持window自动导出！！");
        }
        List<SysLogFile> list = sysFileService.listContents(Constants.USB_PATH);
        return success(list);
    }

    @GetMapping("/listFile")
    @ResponseBody
    public AjaxResult listFile(String path) throws IOException {
        if (SystemService.isWin()) {
            return AjaxResult.error("暂不支持window自动导出！！");
        }
        List<SysLogFile> list = sysFileService.listContents(path);
        return success(list);
    }
}
