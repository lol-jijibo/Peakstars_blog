package com.interview.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 统一承接对象存储接入配置。
 * 通过显式声明 MinIO 连接参数与对外访问域名，保证面经资源迁移后的图片和附件都能稳定回源。
 */
@ConfigurationProperties(prefix = "app.storage.minio")
public class MinioProperties {

    private boolean enabled = false;
    private String endpoint;
    private String accessKey;
    private String secretKey;
    private String bucket = "peakstars-content";
    private String publicBaseUrl;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public String getAccessKey() {
        return accessKey;
    }

    public void setAccessKey(String accessKey) {
        this.accessKey = accessKey;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    public String getBucket() {
        return bucket;
    }

    public void setBucket(String bucket) {
        this.bucket = bucket;
    }

    public String getPublicBaseUrl() {
        return publicBaseUrl;
    }

    public void setPublicBaseUrl(String publicBaseUrl) {
        this.publicBaseUrl = publicBaseUrl;
    }
}
