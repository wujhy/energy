package com.shanhe.framework.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

/**
 * 应用配置类
 * 表示通过aop框架暴露该代理对象,AopContext能够访问
 * 指定要扫描的Mapper类的包的路径
 *
 * @author wjh
 * @since 2026/06/26
 */
@Configuration
@EnableAspectJAutoProxy(exposeProxy = true)
@MapperScan("com.shanhe.project.**.mapper")
public class ApplicationConfig {

}
