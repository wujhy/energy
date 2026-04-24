package com.shanhe.framework.aspectj;

import java.util.Collection;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import cn.hutool.core.util.StrUtil;
import com.shanhe.common.utils.IpUtils;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.validation.BindingResult;
import org.springframework.web.multipart.MultipartFile;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.support.spring.PropertyPreFilters;
import com.shanhe.common.utils.ServletUtils;
import com.shanhe.common.utils.StringUtils;
import com.shanhe.framework.aspectj.lang.annotation.Log;
import com.shanhe.framework.enums.BusinessStatus;
import com.shanhe.framework.manager.AsyncManager;
import com.shanhe.framework.manager.factory.AsyncFactory;
import com.shanhe.project.monitor.operlog.domain.OperLog;

/**
 * 操作日志记录处理
 *
 * @author wjh
 * @since 2024/12/13
 */
@Slf4j
@Aspect
@Component
public class LogAspect {

    /** 排除敏感属性字段 */
    public static final String[] EXCLUDE_PROPERTIES = { "password", "oldPassword", "newPassword", "confirmPassword" };

    /**
     * 处理完请求后执行
     *
     * @param joinPoint 切点
     */
    @AfterReturning(pointcut = "@annotation(controllerLog)", returning = "jsonResult")
    public void doAfterReturning(JoinPoint joinPoint, Log controllerLog, Object jsonResult) {
        handleLog(joinPoint, controllerLog, null, jsonResult);
    }

    /**
     * 拦截异常操作
     *
     * @param joinPoint 切点
     * @param e 异常
     */
    @AfterThrowing(value = "@annotation(controllerLog)", throwing = "e")
    public void doAfterThrowing(JoinPoint joinPoint, Log controllerLog, Exception e) {
        handleLog(joinPoint, controllerLog, e, null);
    }

    protected void handleLog(final JoinPoint joinPoint, Log controllerLog, final Exception e, Object jsonResult) {
        try {
            // *========数据库日志=========*//
            OperLog operLog = new OperLog();
            operLog.setStatus(BusinessStatus.SUCCESS.ordinal());

            HttpServletRequest request = ServletUtils.getRequest();

            operLog.setOperIp(IpUtils.getIpAddr(request));
            operLog.setOperUrl(request.getRequestURI());
            operLog.setOperName("admin");

            if (e != null) {
                operLog.setStatus(BusinessStatus.FAIL.ordinal());
                operLog.setErrorMsg(StringUtils.substring(e.getMessage(), 0, 2000));
            }
            // 设置方法名称
            String className = joinPoint.getTarget().getClass().getName();
            String methodName = joinPoint.getSignature().getName();
            operLog.setMethod(className + "." + methodName + "()");
            // 设置请求方式
            operLog.setRequestMethod(request.getMethod());
            // 处理设置注解上的参数
            getControllerMethodDescription(joinPoint, controllerLog, operLog, jsonResult);
            // 保存数据库
            AsyncManager.me().execute(AsyncFactory.recordOpera(operLog));
        } catch (Exception exp) {
            log.error("前置通知异常，异常信息:{}", exp.getMessage(), e);
        }
    }

    /**
     * 获取注解中对方法的描述信息 用于Controller层注解
     *
     * @param log 日志
     * @param operLog 操作日志
     */
    public void getControllerMethodDescription(JoinPoint joinPoint, Log log, OperLog operLog, Object jsonResult) {
        // 设置action动作
        operLog.setBusinessType(log.businessType().ordinal());
        // 设置标题
        operLog.setTitle(log.title());
        // 设置操作人类别
        operLog.setOperatorType(log.operatorType().ordinal());
        // 是否需要保存request，参数和值
        if (log.isSaveRequestData()) {
            // 获取参数的信息，传入到数据库中。
            setRequestValue(joinPoint, operLog);
        }
        // 是否需要保存response，参数和值
        if (log.isSaveResponseData() && StringUtils.isNotNull(jsonResult)) {
            operLog.setJsonResult(StringUtils.substring(JSONObject.toJSONString(jsonResult), 0, 2000));
        }
    }

    /**
     * 获取请求的参数，放到log中
     *
     * @param joinPoint 切入点
     * @param operLog 日志
     */
    private void setRequestValue(JoinPoint joinPoint, OperLog operLog) {
        Map<String, String[]> map = ServletUtils.getRequest().getParameterMap();
        if (StringUtils.isNotEmpty(map)) {
            String params = JSONObject.toJSONString(map, excludePropertyPreFilter());
            operLog.setOperParam(StringUtils.substring(params, 0, 2000));
        } else {
            Object args = joinPoint.getArgs();
            if (StringUtils.isNotNull(args)) {
                String params = argsArrayToString(joinPoint.getArgs());
                operLog.setOperParam(StringUtils.substring(params, 0, 2000));
            }
        }
        Object deviceCmd = ServletUtils.getRequest().getAttribute("deviceCmd");
        if (deviceCmd != null) {
            operLog.setOperParam(StrUtil.isNotBlank(operLog.getOperParam()) ? operLog.getOperParam() + "  " + deviceCmd : deviceCmd.toString());
        }
    }

    /**
     * 忽略敏感属性
     */
    public PropertyPreFilters.MySimplePropertyPreFilter excludePropertyPreFilter() {
        return new PropertyPreFilters().addFilter().addExcludes(EXCLUDE_PROPERTIES);
    }

    /**
     * 参数拼装
     */
    private String argsArrayToString(Object[] paramsArray) {
        if (paramsArray == null) {
            return "";
        }

        StringBuilder params = new StringBuilder();
        for (Object o : paramsArray) {
            if (StringUtils.isNotNull(o) && !isFilterObject(o)) {
                try {
                    Object jsonObj = JSONObject.toJSONString(o, excludePropertyPreFilter());
                    params.append(jsonObj).append(" ");
                } catch (Exception ignored) { }
            }
        }
        return params.toString().trim();
    }

    /**
     * 判断是否需要过滤的对象。
     *
     * @param o 对象信息。
     * @return 如果是需要过滤的对象，则返回true；否则返回false。
     */
    @SuppressWarnings("rawtypes")
    public boolean isFilterObject(final Object o) {
        Class<?> clazz = o.getClass();
        if (clazz.isArray()) {
            return clazz.getComponentType().isAssignableFrom(MultipartFile.class);
        } else if (Collection.class.isAssignableFrom(clazz)) {
            Collection collection = (Collection) o;
            for (Object value : collection) {
                return value instanceof MultipartFile;
            }
        } else if (Map.class.isAssignableFrom(clazz)) {
            Map map = (Map) o;
            for (Object value : map.entrySet()) {
                Map.Entry entry = (Map.Entry) value;
                return entry.getValue() instanceof MultipartFile;
            }
        }
        return o instanceof MultipartFile || o instanceof HttpServletRequest || o instanceof HttpServletResponse
                || o instanceof BindingResult;
    }
}
