package com.shanhe.project.system.user.mapper;

import com.shanhe.project.system.user.domain.User;
import java.util.List;

/**
 * 用户表 数据层
 *
 * @author wjh
 * @since 2024/12/17
 */
public interface UserMapper {

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
     * 更新密码
     *
     * @param user 用户信息
     * @return 处理结果
     */
    int updateUser(User user);
}
