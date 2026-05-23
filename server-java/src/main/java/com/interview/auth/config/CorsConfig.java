package com.interview.auth.config;

import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * CORS 配置。
 * 允许的来源通过环境变量 CORS_ALLOWED_ORIGINS 以逗号分隔配置，
 * 开发环境默认开放 localhost 常见端口，生产环境必须显式指定前端域名。
 */
@Configuration
public class CorsConfig {

    @Value("${app.storage.local.upload-dir:${java.io.tmpdir}/peakstars-uploads}")
    private String uploadDir;

    @Value("${app.cors.allowed-origins:http://localhost:5173,http://localhost:3000}")
    private List<String> allowedOrigins;

    @Bean
    public WebMvcConfigurer corsWebMvcConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/api/**")
                    .allowedOriginPatterns(allowedOrigins.toArray(new String[0]))
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
