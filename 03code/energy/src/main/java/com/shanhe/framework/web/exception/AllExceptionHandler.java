package com.shanhe.framework.web.exception;

import com.alibaba.fastjson.JSON;
import com.shanhe.framework.web.domain.AjaxResult;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Objects;

/**
 * TODO
 *
 * @author wjh
 * @since 2025/11/21
 */
@Aspect
@Component
public class AllExceptionHandler {

    @Pointcut("execution(* com.shanhe.project..controller.*(..))")
    public void pointcut(){}

    @AfterThrowing(pointcut = "pointcut()", throwing = "e")
    public void exceptionHandler(Throwable e){
        writeJson();
    }

    // 写入结果集
    private void writeJson(){
        HttpServletResponse response = ((ServletRequestAttributes) Objects.requireNonNull(RequestContextHolder.getRequestAttributes())).getResponse();
        if (response == null) {
            return;
        }
        response.setContentType("application/json; charset=utf-8");
        AjaxResult ajaxResult = new AjaxResult();
        ajaxResult.put("code", 99);
        ajaxResult.put("msg", "未登录或登录超时，请重新登录");
        PrintWriter writer = null;
        try {
            writer = response.getWriter();
            writer.print(JSON.toJSON(ajaxResult));
        } catch (IOException ignored) {;
        } finally {
            if (writer != null) {
                writer.flush();
            }
            if (writer != null) {
                writer.close();
            }
        }
    }
}
