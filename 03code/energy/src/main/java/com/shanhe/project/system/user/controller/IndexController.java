package com.shanhe.project.system.user.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import com.shanhe.common.utils.ServletUtils;
import com.shanhe.framework.web.controller.BaseController;
import com.shanhe.framework.web.domain.AjaxResult;
import org.springframework.web.bind.annotation.RestController;

/**
 * 首页设置
 *
 * @author wjh
 * @since 2024/12/16
 */
@RestController
public class IndexController extends BaseController {

    // 锁定屏幕
    @GetMapping("/lockscreen")
    public AjaxResult lockscreen() {
        ServletUtils.getSession().setAttribute("lockscreen", true);
        return AjaxResult.success();
    }

    // 解锁屏幕
    @PostMapping("/unLockscreen")
    @ResponseBody
    public AjaxResult unLockscreen() {
        ServletUtils.getSession().removeAttribute("lockscreen");
        return AjaxResult.success();
    }
}
