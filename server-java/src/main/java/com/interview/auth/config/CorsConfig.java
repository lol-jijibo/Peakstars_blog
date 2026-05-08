package com.interview.auth.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * CORS 配置。
 * 作用：允许前端开发环境直接访问 Java 认证服务。
 */
@Configuration
public class CorsConfig {

    @Value("${app.storage.local.upload-dir:${java.io.tmpdir}/peakstars-uploads}")
    private String uploadDir;

    @Bean
    public WebMvcConfigurer webMvcConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/api/**")
                    .allowedOriginPatterns("*")
                    .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                    .allowedHeaders("*");
            }
        };
    }

    /**
     * 仅在 MinIO 未启用时注册本地文件资源处理器。
     * MinIO 启用时由 StorageProxyController 接管 /uploads/** 路径。
     */
    @Bean
    @ConditionalOnProperty(prefix = "app.storage.minio", name = "enabled", havingValue = "false", matchIfMissing = true)
    public WebMvcConfigurer localUploadResourceConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addResourceHandlers(ResourceHandlerRegistry registry) {
                registry.addResourceHandler("/uploads/**")
                    .addResourceLocations("file:" + uploadDir + "/");
            }
        };
    }
}
