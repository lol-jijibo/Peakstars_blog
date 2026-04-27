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
@MapperScan("com.interview.auth")
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
