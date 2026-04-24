package com.shanhe.framework.config;

import com.shanhe.framework.consts.SysConst;
import com.shanhe.framework.interceptor.LoginInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.io.ClassPathResource;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import com.shanhe.common.constant.Constants;
import com.shanhe.framework.interceptor.BaseRepeatSubmitInterceptor;

import javax.annotation.Resource;

/**
 * 通用配置
 *
 * @author ruoyi
 */
@Configuration
public class ResourcesConfig implements WebMvcConfigurer
{

    @Resource
    private BaseRepeatSubmitInterceptor baseRepeatSubmitInterceptor;
    @Resource
    private LoginInterceptor loginInterceptor;

    /**
     * 视图跳转控制配置
     * 默认首页的设置，当输入域名时自动跳转到默认指定的网页
     */
    @Override
    public void addViewControllers(ViewControllerRegistry registry)
    {
        registry.addViewController("/index").setViewName(Constants.INDEX);
        registry.addViewController("/").setViewName(Constants.INDEX);
        registry.setOrder(Ordered.HIGHEST_PRECEDENCE);
    }

    /**
     * 静态资源配置
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        /* 本地文件上传路径 */
        registry.addResourceHandler("/**", Constants.RESOURCE_PREFIX + "/**")
                .addResourceLocations("classpath:/dist/", "file:" + SysConst.profile + "/")
                /* 自定义 ClassPathResource 实现类，在前端请求的地址匹配不到对应的路径时，强制使用 /dist/index.html 资源
                   本质上，等价于 nginx 在处理不到 Vue 的请求地址时，try_files 到 index.html 地址 */
                .addResourceLocations(new ClassPathResource("/dist/index.html"));
        /* 页面静态化 */
        registry.addResourceHandler("/**").addResourceLocations("classpath:/dist/");
    }

    /**
     * 拦截器配置
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(baseRepeatSubmitInterceptor).addPathPatterns("/**").order(1);
        registry.addInterceptor(loginInterceptor).addPathPatterns("/**").order(1);
    }

    /**
     * 跨域配置
     */
    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(true);
        // 设置访问源地址
        config.addAllowedOriginPattern("*");
        // 设置访问源请求头
        config.addAllowedHeader("*");
        // 设置访问源请求方法
        config.addAllowedMethod("*");
        // 有效期 1800秒
        config.setMaxAge(1800L);
        // 添加映射路径，拦截一切请求
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        // 返回新的CorsFilter
        return new CorsFilter(source);
    }
}