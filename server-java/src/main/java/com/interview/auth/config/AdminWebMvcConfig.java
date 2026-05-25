package com.interview.auth.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 集中注册后台管理模块的 Web 拦截规则。
 * 将所有后台接口交给管理员鉴权拦截器处理，避免留下可探测入口。
 */
@Configuration
@RequiredArgsConstructor
public class AdminWebMvcConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;

    /**
     * 注册后台管理接口的访问拦截路径。
     * 统一覆盖 /api/admin/**，由拦截器继续校验登录态和管理员角色。
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
            .addPathPatterns("/api/admin/**");
    }
}
