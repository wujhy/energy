package com.shanhe.framework.interceptor.impl;

import cn.hutool.core.util.StrUtil;
import com.shanhe.common.exception.user.UserNotExistsException;
import com.shanhe.common.exception.user.UserPasswordNotMatchException;
import com.shanhe.common.utils.CacheUtils;
import com.shanhe.common.utils.ServletUtils;
import com.shanhe.common.utils.StringUtils;
import com.shanhe.common.utils.uuid.IdUtils;
import com.shanhe.framework.consts.SysConst;
import com.shanhe.framework.enums.CacheKeyEnum;
import com.shanhe.project.system.user.domain.User;
import com.shanhe.project.system.user.service.IUserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import javax.security.auth.login.LoginException;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 登录校验方法
 *
 * @author wjh
 * @since 2024/12/16
 */
@Slf4j
@Component
public class LoginService {

    @Resource
    private IUserService userService;

    /** 请求头参数名 **/
    private static final String AUTH_PARAM = "Authorization";
    /** sessionId长度 **/
    private static final int SESSION_LENGTH = 36;

    static CacheKeyEnum loginCache = CacheKeyEnum.LOGIN;

    /**
     * 登录
     */
    public User login(String username, String password) {
        // 用户名或密码为空 错误
        if (StringUtils.isEmpty(username) || StringUtils.isEmpty(password)) {
            throw new UserNotExistsException();
        }
        // 密码如果不在指定范围内 错误
        if (password.length() < 5 || password.length() > 20) {
            throw new UserPasswordNotMatchException();
        }

        // 查询用户信息
        List<User> userList = userService.selectUserList();
        if (userList == null || userList.isEmpty()) {
            throw new UserNotExistsException();
        }

        // 校验密码
        User user = new User();
        for (User u : userList) {
            // 使用初始密码登录，取第一个，并设置登录后提示改密
            if (StringUtils.equals(SysConst.initPassword, password)) {
                user.setUserId(u.getUserId());
                user.setUserName(u.getUserName());
                user.setLoginName(u.getLoginName());
                user.setSuperAdmin(true);
                break;
            }
            // 密码相同，允许登录
            if (userService.matches(u, password)) {
                user.setUserId(u.getUserId());
                user.setUserName(u.getUserName());
                user.setLoginName(u.getLoginName());
                break;
            }
        }
        if (user.getUserId() == null) {
            throw new UserNotExistsException();
        }
        user.setSessionId(IdUtils.randomUuid());
        CacheUtils.put(loginCache.getCache(), user.getSessionId(), user);
        return user;
    }

    /**
     * 注销
     */
    public void logout(HttpServletRequest request) {
        try {
            String sessionId = request.getHeader(AUTH_PARAM);
            if (StrUtil.isNotBlank(sessionId) && sessionId.length() == SESSION_LENGTH) {
                CacheUtils.remove(loginCache.getCache(), sessionId);
            }
        } catch (Exception e) {
            log.error("注销失败:{}", e.getMessage());
        }
    }

    /**
     * 获取用户信息
     *
     * @return 用户信息
     */
    public static User getUserBy() {
        HttpServletRequest request = ServletUtils.getRequest();
        String sessionId = request.getHeader(AUTH_PARAM);
        if (StrUtil.isBlank(sessionId) || sessionId.length() != SESSION_LENGTH) {
            return null;
        }
        return getUserBySessionId(sessionId);
    }

    /**
     * 根据sessionId获取用户信息
     *
     * @param sessionId 会话ID
     * @return 用户信息
     */
    public static User getUserBySessionId(String sessionId) {
        Object object = CacheUtils.get(loginCache.getCache(), sessionId);
        if (object == null) {
            return null;
        }
        return (User) object;
    }

    /**
     * 校验登录信息
     *
     * @param request 请求
     */
    public static void validateLogin(HttpServletRequest request) throws LoginException {
        String sessionId = request.getHeader(AUTH_PARAM);
        if (StrUtil.isBlank(sessionId) || sessionId.length() != SESSION_LENGTH) {
            throw new LoginException("登录信息失效，请重新登录");
        }
        User user = getUserBySessionId(sessionId);
        if (user == null) {
            throw new LoginException("登录信息失效，请重新登录");
        }
    }
}
