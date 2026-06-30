package com.shanhe.project.manage.user.service.impl;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.shanhe.project.manage.user.service.IUserService;
import org.springframework.stereotype.Service;
import com.shanhe.project.manage.user.domain.User;
import com.shanhe.project.manage.user.mapper.UserMapper;

import javax.annotation.Resource;
import java.util.List;

/**
 * 用户 业务层处理
 *
 * @author wjh
 * @since 2024/12/16
 */
@Service
public class UserServiceImpl implements IUserService {

    /** 用户数据访问层。 */
    @Resource
    private UserMapper userMapper;

    /**
     * 查询用户列表
     *
     * @return 用户列表
     */
    @Override
    public List<User> selectUserList() {
        return userMapper.selectUserList();
    }

    /**
     * 根据用户ID查询用户
     *
     * @param userId 用户ID
     * @return 用户对象
     */
    @Override
    public User selectUserById(Long userId) {
        return userMapper.selectUserById(userId);
    }

    /**
     * 重置用户密码
     *
     * @param user 用户对象
     */
    @Override
    public void resetUserPwd(User user) {
        user.setSalt(this.randomSalt());
        user.setPassword(this.encryptPassword(user.getLoginName(), user.getPassword(), user.getSalt()));
        userMapper.updateUser(user);
    }

    /**
     * 验证用户密码是否匹配
     *
     * @param user 用户对象
     * @param newPassword 待验证密码
     * @return 是否匹配
     */
    @Override
    public boolean matches(User user, String newPassword) {
        return StrUtil.equals(user.getPassword(), this.encryptPassword(user.getLoginName(), newPassword, user.getSalt()));
    }

    /**
     * 生成随机盐
     *
     * @return 随机盐值
     */
    public String randomSalt() {
        // 一个Byte占两个字节，此处生成的3字节，字符串长度为6
        return RandomUtil.randomString(6);
    }

    /**
     * 生成密码
     *
     * @param loginName 登录名
     * @param password 密码
     * @param salt 盐值
     * @return 加密后的密码
     */
    public String encryptPassword(String loginName, String password, String salt) {
        return DigestUtil.md5Hex(loginName + password + salt);
    }
}
