package com.shanhe.project.system.user.controller;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

import com.shanhe.framework.interceptor.impl.LoginService;
import com.shanhe.project.system.user.domain.LoginVo;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import com.shanhe.framework.web.controller.BaseController;
import com.shanhe.framework.web.domain.AjaxResult;

/**
 * 登录验证
 *
 * @author wjh
 * @since 2024/12/16
 */
@Controller
public class LoginController extends BaseController {

    @Resource
    private LoginService loginService;

    @PostMapping("/login")
    @ResponseBody
    public AjaxResult ajaxLogin(@RequestBody LoginVo loginVo) {
        try {
            return success(loginService.login(loginVo.getUsername(), loginVo.getPassword()));
        } catch (Exception e) {
            return error(e.getMessage());
        }
    }

    @GetMapping("/logouts")
    @ResponseBody
    public AjaxResult logout(HttpServletRequest request) {
        loginService.logout(request);
        return success();
    }
}
