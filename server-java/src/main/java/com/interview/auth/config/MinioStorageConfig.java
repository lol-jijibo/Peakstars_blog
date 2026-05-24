package com.interview.auth.config;

import io.minio.MinioClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 统一装配 MinIO 存储客户端。
 * 让依赖 MinIO 的上传、回源与历史资源代理都复用同一套连接配置。
 */
@Configuration
@ConditionalOnProperty(prefix = "app.storage.minio", name = "enabled", havingValue = "true")
public class MinioStorageConfig {

    /**
     * 创建全局复用的 MinIO 客户端实例。
     * 按配置中的地址与凭证建立连接，供存储服务和代理控制器统一访问对象存储。
     */
    @Bean
    public MinioClient minioClient(MinioProperties properties) {
        return MinioClient.builder()
            .endpoint(properties.getEndpoint())
            .credentials(properties.getAccessKey(), properties.getSecretKey())
            .build();
    }
}