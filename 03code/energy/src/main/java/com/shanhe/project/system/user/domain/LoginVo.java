package com.shanhe.project.system.user.domain;

import lombok.Data;

/**
 * 登录请求对象
 *
 * @author wjh
 * @since 2026-05-25
 */
@Data
public class LoginVo {
    /**
     * 用户名
     */
    private String username;
    /**
     * 密码
     */
    private String password;
}
