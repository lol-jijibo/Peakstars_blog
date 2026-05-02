package com.interview.auth;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * 认证服务启动入口
 * 负责启动 Spring Boot 容器并加载认证域与后台管理域的 MyBatis Mapper
 */
@SpringBootApplication
@ConfigurationPropertiesScan
@MapperScan({
    "com.interview.auth.infrastructure.mapper",
    "com.interview.auth.admin.mapper"
})
public class AuthApplication {

    /**
     * 启动认证服务
     * 接收命令行参数并完成服务主进程初始化
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        SpringApplication.run(AuthApplication.class, args);
    }
}
