package com.shanhe.project.system.user.service;

import com.shanhe.project.system.user.domain.User;

import java.util.List;

/**
 * 用户 业务层
 *
 * @author wjh
 * @since 2024/12/17
 */
public interface IUserService {

    /**
     * 查全部用户
     *
     * @return 用户列表
     */
    List<User> selectUserList();

    /**
     * 通过用户ID查询用户
     *
     * @param userId 用户ID
     * @return 用户对象信息
     */
    User selectUserById(Long userId);

    /**
     * 修改用户密码信息
     *
     * @param user 用户信息
     */
    void resetUserPwd(User user);

    /**
     * 用户名密码匹配
     *
     * @param user 用户信息
     * @param password 密码
     * @return boolean
     */
    boolean matches(User user, String password);
}
