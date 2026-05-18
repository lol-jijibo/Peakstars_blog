package com.interview.auth.config;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 集中装配阿里云 OSS 客户端。
 * 仅在显式开启 OSS 时创建连接，避免未配置云存储时影响本地开发启动。
 */
@Configuration
public class AliyunOssStorageConfig {

    /**
     * 构建阿里云 OSS Java 客户端。
     * 复用统一配置对接目标 Bucket，供导入书籍、封面和正文资源上传使用。
     */
    @Bean(destroyMethod = "shutdown")
    @ConditionalOnProperty(prefix = "app.storage.oss", name = "enabled", havingValue = "true")
    public OSS aliyunOssClient(AliyunOssProperties properties) {
        return new OSSClientBuilder().build(
            properties.getEndpoint(),
            properties.getAccessKeyId(),
            properties.getAccessKeySecret()
        );
    }
}
