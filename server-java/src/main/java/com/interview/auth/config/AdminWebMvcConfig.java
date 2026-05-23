package com.interview.auth.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 注册全局拦截器，将管理后台鉴权拦截器绑定到 /api/admin/** 路径。
 */
@Configuration
@RequiredArgsConstructor
public class AdminWebMvcConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
            .addPathPatterns("/api/admin/**")
            .excludePathPatterns("/api/admin/heartbeat");
    }
}
