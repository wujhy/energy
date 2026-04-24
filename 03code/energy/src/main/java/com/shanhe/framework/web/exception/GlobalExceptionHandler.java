package com.shanhe.framework.web.exception;

import javax.security.auth.login.LoginException;
import javax.servlet.http.HttpServletRequest;

import org.mybatis.spring.MyBatisSystemException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.dao.DataAccessException;
import org.springframework.validation.BindException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.HttpSessionRequiredException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import com.shanhe.common.exception.ServiceException;
import com.shanhe.framework.web.domain.AjaxResult;
import org.springframework.web.multipart.MultipartException;
import org.sqlite.SQLiteException;

import java.io.IOException;

/**
 * 全局异常处理器
 *
 * @author ruoyi
 */
@RestControllerAdvice
public class GlobalExceptionHandler
{
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 请求方式不支持
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public AjaxResult handleHttpRequestMethodNotSupported(HttpRequestMethodNotSupportedException e,
                                                          HttpServletRequest request)
    {
        log.error("请求地址'{}',不支持'{}'请求", request.getRequestURI(), e.getMethod());
        return AjaxResult.error(e.getMessage());
    }

    /**
     * 拦截未知的运行时异常
     */
    @ExceptionHandler(RuntimeException.class)
    public AjaxResult handleRuntimeException(RuntimeException e, HttpServletRequest request)
    {
        Throwable rootCause = NestedExceptionUtils.getRootCause(e);
        if (rootCause instanceof DataAccessException) {
            log.error("请求地址异常'{}':数据库操作异常", request.getRequestURI());
            return AjaxResult.error("数据库操作异常，请联系管理员");
        } else if (rootCause instanceof SQLiteException) {
            log.error("请求地址异常'{}':数据操作异常", request.getRequestURI());
            return AjaxResult.error("数据操作异常，请稍后重试");
        } else if (rootCause instanceof MultipartException) {
            log.error("请求地址异常'{}':请求参数错误", request.getRequestURI());
        } else if (rootCause instanceof IOException) {
            log.error("请求地址异常'{}'：{}", request.getRequestURI(), e.getMessage());
        } else {
            log.error("请求地址异常'{}'", request.getRequestURI(), e);
        }
        return AjaxResult.error(e.getMessage());
    }

    /**
     * 系统异常
     */
    @ExceptionHandler(Exception.class)
    public AjaxResult handleException(Exception e, HttpServletRequest request)
    {
        Throwable rootCause = NestedExceptionUtils.getRootCause(e);
        if (rootCause instanceof IOException) {
            log.error("请求地址IO异常'{}'：{}", request.getRequestURI(), e.getMessage());
        } else {
            log.error("请求地址异常'{}'", request.getRequestURI(), e);
        }

        return AjaxResult.error(e.getMessage());
    }

    /**
     * 系统异常
     */
    @ExceptionHandler(MyBatisSystemException.class)
    public AjaxResult handleMyBatisSystemException(MyBatisSystemException e, HttpServletRequest request)
    {
        log.error("MyBatis系统异常 - 请求URI: {}", request.getRequestURI(), e);
        return AjaxResult.error("数据库操作异常，请稍后重试");
    }

    /**
     * 业务异常
     */
    @ExceptionHandler(ServiceException.class)
    public Object handleServiceException(ServiceException e, HttpServletRequest request)
    {
        log.error(e.getMessage());
        return AjaxResult.error(e.getMessage());
    }

    /**
     * 自定义验证异常
     */
    @ExceptionHandler(BindException.class)
    public AjaxResult handleBindException(BindException e)
    {
//        log.error(e.getMessage(), e);
        String message = e.getAllErrors().get(0).getDefaultMessage();
        return AjaxResult.error(message);
    }


    @ExceptionHandler(HttpSessionRequiredException.class)
    public AjaxResult handleHttpSessionRequiredException(HttpSessionRequiredException e, HttpServletRequest request)
    {
        log.error("会话过期，请重新登录");
        return AjaxResult.error(e.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    public AjaxResult handleIllegalStateException(IllegalStateException e)
    {
        return AjaxResult.error(e.getMessage());
    }

    @ExceptionHandler(LoginException.class)
    public AjaxResult handleLoginException(LoginException e)
    {
        AjaxResult ajaxResult = new AjaxResult();
        ajaxResult.put("code", 99);
        ajaxResult.put("msg", "未登录或登录超时，请重新登录");
        return ajaxResult;
    }
}
