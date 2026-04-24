package com.shanhe.framework.interceptor;

import cn.hutool.core.util.StrUtil;
import com.shanhe.framework.interceptor.impl.LoginService;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.List;

/**
 * 登录拦截器
 *
 * @author wjh
 * @since 2025/11/24
 */
@Component
public class LoginInterceptor implements HandlerInterceptor
{
    private static final List<String> excludeSessionUrl =
            Arrays.asList("/vite.svg", "/index", "/login", "/logout", "/assets/", "/dict/", "/patrol/", "/screen/", "/alarm/",
                    "/device/alarm/", "/alarm/level/list", "/device/history/", "/battery/log/", "/battery/pack/list",
                    "/battery/monitor/", "/shim/southData", "/configuration/battery/", "/stat/battery/", "/opt/log/",
                    "/batteryOpt/", "/battery/set/", "/patrol/", "/optCmd/", "/file/");

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception
    {
        if (StrUtil.equals("/", request.getRequestURI())) {
            return true;
        }
        boolean startsWithAny = excludeSessionUrl.stream().anyMatch(request.getRequestURI()::startsWith);
        if (startsWithAny) {
            return true;
        }

        LoginService.validateLogin(request);
        return true;
    }
}
