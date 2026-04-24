package com.shanhe.project.system.user.domain;

import java.io.Serializable;

import lombok.Data;

/**
 * 用户对象 sys_user
 *
 * @author wjh
 * @since 2024/12/16
 */
@Data
public class User implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 用户ID */
    private Long userId;

    /** 登录名称 */
    private String loginName;

    /** 用户名称 */
    private String userName;

    /** 密码 */
    private String password;

    /** 盐加密 */
    private String salt;

    /** 超级管理员 */
    private Boolean superAdmin;

    /** SESSION_ID */
    private String sessionId;
}
