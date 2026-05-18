package com.interview.auth.infrastructure.storage;

import com.interview.auth.config.MinioProperties;
import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import jakarta.annotation.PostConstruct;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * 基于 MinIO 落地后台导入资源上传能力。
 * 统一将外部编辑器中的图片和附件迁移到自有对象存储，并返回稳定可访问的静态资源地址。
 */
@Service
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.storage.minio", name = "enabled", havingValue = "true")
public class MinioContentStorageService implements ContentStorageService {

    @Value("${app.storage.proxy.base-url:/uploads}")
    private String proxyBaseUrl;

    private final MinioClient minioClient;
    private final MinioProperties minioProperties;

    private volatile boolean bucketReady = false;

    /**
     * 启动时校验对象存储桶可用。
     * 提前完成桶存在性检查与兜底创建，初始化失败时仅记录警告不阻断应用启动，
     * 后续上传操作会在首次成功连接后自动重试初始化。
     */
    @PostConstruct
    public void ensureBucketReady() {
        tryInitializeBucket();
    }

    /**
     * 尝试初始化桶，失败时仅记录警告，不抛出异常。
     * 避免凭证配置错误或 MinIO 短暂不可用时导致整个应用无法启动。
     */
    private void tryInitializeBucket() {
        try {
            boolean bucketExists = minioClient.bucketExists(
                BucketExistsArgs.builder().bucket(minioProperties.getBucket()).build()
            );
            if (!bucketExists) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(minioProperties.getBucket()).build());
            }
            bucketReady = true;
            log.info("MinIO bucket '{}' is ready", minioProperties.getBucket());
        } catch (Exception exception) {
            bucketReady = false;
            log.warn("MinIO bucket initialization failed, file uploads will fail until MinIO is available. " +
                "Please check MinIO endpoint/credentials configuration. Error: {}", exception.getMessage());
        }
    }

    /**
     * 上传内容导入资源到 MinIO。
     * 统一按业务类型和日期生成对象路径，保证资源目录结构稳定且可回溯。
     * 若桶尚未就绪会自动重试初始化，仍失败时抛出明确异常。
     */
    @Override
    public String upload(String objectPrefix, String fileName, InputStream inputStream, long size, String contentType) throws Exception {
        if (!bucketReady) {
            tryInitializeBucket();
        }
        if (!bucketReady) {
            throw new IllegalStateException("MinIO is not available. Please check MinIO service status and credentials configuration.");
        }
        String objectName = buildObjectName(objectPrefix, fileName);
        minioClient.putObject(
            PutObjectArgs.builder()
                .bucket(minioProperties.getBucket())
                .object(objectName)
                .stream(inputStream, size, -1)
                .contentType(contentType)
                .build()
        );
        return buildPublicUrl(objectName);
    }

    @Override
    public InputStream download(String resourceUrl) throws Exception {
        if (!bucketReady) {
            tryInitializeBucket();
        }
        String objectName = extractObjectName(resourceUrl);
        return minioClient.getObject(
            GetObjectArgs.builder()
                .bucket(minioProperties.getBucket())
                .object(objectName)
                .build()
        );
    }

    /**
     * 从公开访问地址中解析 MinIO 对象键。
     * 同时兼容 proxyBaseUrl、publicBaseUrl 和 endpoint 构造的地址。
     */
    private String extractObjectName(String resourceUrl) {
        String url = resourceUrl.trim();
        String proxyUrl = normalizeBaseUrl(proxyBaseUrl);
        if (!proxyUrl.isBlank() && url.startsWith(proxyUrl)) {
            return url.substring(proxyUrl.length() + 1);
        }
        String publicBaseUrl = normalizeBaseUrl(minioProperties.getPublicBaseUrl());
        if (!publicBaseUrl.isBlank() && url.startsWith(publicBaseUrl)) {
            return url.substring(publicBaseUrl.length() + 1);
        }
        java.net.URI uri = java.net.URI.create(url);
        String path = uri.getPath();
        return path.startsWith("/") ? path.substring(1) : path;
    }

    /**
     * 判断资源链接是否已经指向当前对象存储。
     * 通过比对对外访问域名与 MinIO 服务地址，避免正文保存时重复上传同一份资源。
     */
    @Override
    public boolean isStorageUrl(String resourceUrl) {
        if (resourceUrl == null || resourceUrl.isBlank()) {
            return false;
        }

        String normalizedUrl = resourceUrl.trim().toLowerCase(Locale.ROOT);
        String publicBaseUrl = normalizeBaseUrl(minioProperties.getPublicBaseUrl());
        String endpoint = normalizeBaseUrl(minioProperties.getEndpoint());
        String proxyUrl = normalizeBaseUrl(proxyBaseUrl);
        return (!publicBaseUrl.isBlank() && normalizedUrl.startsWith(publicBaseUrl.toLowerCase(Locale.ROOT)))
            || (!endpoint.isBlank() && normalizedUrl.startsWith(endpoint.toLowerCase(Locale.ROOT)))
            || (!proxyUrl.isBlank() && normalizedUrl.startsWith(proxyUrl.toLowerCase(Locale.ROOT)));
    }

    /**
     * 按日期分片生成对象存储路径。
     * 保留业务前缀和随机后缀，便于后续按模块清理资源并规避文件名冲突。
     */
    private String buildObjectName(String objectPrefix, String fileName) {
        LocalDate today = LocalDate.now();
        String safePrefix = sanitizePathSegment(objectPrefix);
        String extension = extractExtension(fileName);
        String randomName = UUID.randomUUID().toString().replace("-", "");
        return "%s/%d/%02d/%02d/%s%s".formatted(
            safePrefix,
            today.getYear(),
            today.getMonthValue(),
            today.getDayOfMonth(),
            randomName,
            extension
        );
    }

    /**
     * 统一拼接资源对外访问地址。
     * 优先使用显式配置的静态域名，未配置时退回 MinIO 服务地址以保证导入预览仍可访问。
     */
    private String buildPublicUrl(String objectName) {
        return normalizeBaseUrl(proxyBaseUrl) + "/" + objectName;
    }

    /**
     * 清洗对象路径片段。
     * 避免来源类型或模块名中混入特殊字符，确保 MinIO 对象键稳定可控。
     */
    private String sanitizePathSegment(String value) {
        if (value == null || value.isBlank()) {
            return "content-import";
        }
        return value.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9/_-]+", "-");
    }

    /**
     * 提取上传文件扩展名。
     * 保留常见后缀用于静态资源识别，缺省时回落为无扩展对象以兼容未知附件。
     */
    private String extractExtension(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return "";
        }
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(dotIndex).toLowerCase(Locale.ROOT);
    }

    /**
     * 统一清理 URL 末尾斜杠。
     * 避免对象访问地址拼接时出现双斜杠，保证正文中的资源链接格式稳定。
     */
    private String normalizeBaseUrl(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}
