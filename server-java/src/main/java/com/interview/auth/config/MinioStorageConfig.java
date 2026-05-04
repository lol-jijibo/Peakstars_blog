package com.interview.auth.config;

import io.minio.MinioClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 统一装配对象存储客户端。
 * 仅在显式开启 MinIO 时创建连接，避免本地未配置对象存储时影响后台其他功能启动。
 */
@Configuration
public class MinioStorageConfig {

    /**
     * 构建 MinIO Java 客户端。
     * 复用统一配置对接对象存储，给正文图片与附件迁移链路提供底层上传能力。
     *
     * @param properties MinIO 连接配置
     * @return MinIO 客户端实例
     */
    @Bean
    @ConditionalOnProperty(prefix = "app.storage.minio", name = "enabled", havingValue = "true")
    public MinioClient minioClient(MinioProperties properties) {
        return MinioClient.builder()
            .endpoint(properties.getEndpoint())
            .credentials(properties.getAccessKey(), properties.getSecretKey())
            .build();
    }
}
