package com.shanhe.project.system.user.domain;

import lombok.Data;

/**
 * @author zhoubin
 * @date 2025/9/17
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
