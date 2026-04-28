package com.interview.auth;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * 认证服务启动类。
 * 作用：作为 Java 认证服务的程序入口，负责启动 Spring Boot 容器并扫描 MyBatis Mapper。
 */
@SpringBootApplication
@ConfigurationPropertiesScan
// 业务目的：只扫描真正承载 SQL 映射的 Mapper 包，避免把 Service 接口误注册成 MyBatis 代理。
// 业务逻辑：认证域和后台管理域分别维护独立 Mapper 包，启动时按精确包名加载即可满足全部 SQL 映射需求。
@MapperScan({
    "com.interview.auth.infrastructure.mapper",
    "com.interview.auth.admin.mapper"
})
public class AuthApplication {

    /**
     * 启动认证服务。
     *
     * @param args 启动参数，当前项目未使用额外命令行参数
     */
    public static void main(String[] args) {
        SpringApplication.run(AuthApplication.class, args);
    }
}
