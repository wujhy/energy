package com.shanhe.project.system.user.controller;

import com.shanhe.common.utils.StringUtils;
import org.springframework.web.bind.annotation.*;
import com.shanhe.framework.aspectj.lang.annotation.Log;
import com.shanhe.framework.enums.BusinessType;
import com.shanhe.framework.web.controller.BaseController;
import com.shanhe.framework.web.domain.AjaxResult;
import com.shanhe.project.system.user.domain.User;
import com.shanhe.project.system.user.service.IUserService;

import javax.annotation.Resource;

/**
 * 修改密码
 *
 * @author wjh
 * @since 2024/12/16
 */
@RestController
@RequestMapping("/system/user")
public class ProfileController extends BaseController {

    @Resource
    private IUserService userService;

    @GetMapping("/info")
    public AjaxResult info() {
        User user = getSysUser();
        if (StringUtils.isNull(user)) {
            return error("服务器超时，请重新登录");
        }
        return success(userService.selectUserById(user.getUserId()));
    }

    @Log(title = "重置密码", businessType = BusinessType.UPDATE)
    @PostMapping("/resetPwd")
    @ResponseBody
    public AjaxResult resetPwd(String newPassword) {
        User user = getSysUser();
        user.setPassword(newPassword);
        userService.resetUserPwd(user);
        return success();
    }
}
