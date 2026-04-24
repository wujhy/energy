package com.shanhe.project.system.user.service.impl;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.shanhe.project.system.user.service.IUserService;
import org.springframework.stereotype.Service;
import com.shanhe.project.system.user.domain.User;
import com.shanhe.project.system.user.mapper.UserMapper;

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

    @Resource
    private UserMapper userMapper;

    @Override
    public List<User> selectUserList() {
        return userMapper.selectUserList();
    }

    @Override
    public User selectUserById(Long userId) {
        return userMapper.selectUserById(userId);
    }

    @Override
    public void resetUserPwd(User user) {
        user.setSalt(this.randomSalt());
        user.setPassword(this.encryptPassword(user.getLoginName(), user.getPassword(), user.getSalt()));
        userMapper.updateUser(user);
    }

    @Override
    public boolean matches(User user, String newPassword) {
        return StrUtil.equals(user.getPassword(), this.encryptPassword(user.getLoginName(), newPassword, user.getSalt()));
    }

    /**
     * 生成随机盐
     */
    public String randomSalt() {
        // 一个Byte占两个字节，此处生成的3字节，字符串长度为6
        return RandomUtil.randomString(6);
    }

    /**
     * 生成密码
     */
    public String encryptPassword(String loginName, String password, String salt) {
        return DigestUtil.md5Hex(loginName + password + salt);
    }
}
