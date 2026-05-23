package com.interview.auth.infrastructure.storage;

import com.aliyun.oss.OSS;
import com.aliyun.oss.model.GetObjectRequest;
import com.aliyun.oss.model.ObjectMetadata;
import com.aliyun.oss.model.PutObjectRequest;
import com.interview.auth.config.AliyunOssProperties;
import java.io.InputStream;
import java.net.URI;
import java.time.LocalDate;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

/**
 * 基于阿里云 OSS 落地内容资源上传能力。
 * 开启 OSS 后优先接管统一存储接口，将书籍、封面和正文资源上传到云端 Bucket。
 */
@Service
@Primary
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.storage.oss", name = "enabled", havingValue = "true")
public class AliyunOssContentStorageService implements ContentStorageService {

    private static final String PROXY_BASE_URL = "/uploads";

    private final OSS ossClient;
    private final AliyunOssProperties ossProperties;

    /**
     * 上传内容资源到阿里云 OSS。
     * 按业务前缀、日期和随机文件名生成对象键，并返回前端可访问的静态资源地址。
     */
    @Override
    public String upload(String objectPrefix, String fileName, InputStream inputStream, long size, String contentType) {
        String objectName = buildObjectName(objectPrefix, fileName);
        ObjectMetadata metadata = new ObjectMetadata();
        if (size > 0) {
            metadata.setContentLength(size);
        }
        if (contentType != null && !contentType.isBlank()) {
            metadata.setContentType(contentType);
        }
        ossClient.putObject(new PutObjectRequest(
            ossProperties.getBucket(),
            objectName,
            inputStream,
            metadata
        ));
        return buildPublicUrl(objectName);
    }

    /**
     * 从 OSS 下载已上传的资源文件。
     * 解析存储 URL 中的对象键，使用 OSS SDK 拉取完整对象内容。
     */
    @Override
    public InputStream download(String resourceUrl) {
        String objectName = extractObjectName(resourceUrl);
        return ossClient.getObject(new GetObjectRequest(ossProperties.getBucket(), objectName)).getObjectContent();
    }

    /**
     * 删除阿里云 OSS 中的单个资源对象。
     * 从代理地址或公开地址中还原对象键后执行删除，供后台彻底删除书籍时回收云端文件。
     */
    @Override
    public void delete(String resourceUrl) {
        String objectName = extractObjectName(resourceUrl);
        if (objectName == null || objectName.isBlank()) {
            return;
        }
        ossClient.deleteObject(ossProperties.getBucket(), objectName);
    }

    /**
     * 从公开访问地址中解析 OSS 对象键。
     * 兼容三种 URL 格式：
     * 1. /uploads/... 代理格式 → 去掉 /uploads/ 前缀得对象键
     * 2. publicBaseUrl 自定义域名格式
     * 3. OSS 默认 endpoint 格式（https://bucket.endpoint/objectKey）
     */
    private String extractObjectName(String resourceUrl) {
        String url = resourceUrl.trim();

        // 处理 /uploads/... 代理格式：数据库存储的封面和水印地址可能为这种相对路径
        if (url.startsWith("/uploads/")) {
            return url.substring("/uploads/".length());
        }

        String publicBaseUrl = normalizeBaseUrl(ossProperties.getPublicBaseUrl());
        if (!publicBaseUrl.isBlank() && url.startsWith(publicBaseUrl)) {
            return url.substring(publicBaseUrl.length() + 1);
        }
        // 从 OSS 默认 URL 格式中提取对象键：https://bucket.endpoint/objectKey
        URI uri = URI.create(url);
        String path = uri.getPath();
        return path.startsWith("/") ? path.substring(1) : path;
    }

    /**
     * 判断资源链接是否已经指向当前 OSS 存储。
     * 通过对比公开访问域名和 endpoint，避免导入资源被重复上传。
     */
    @Override
    public boolean isStorageUrl(String resourceUrl) {
        if (resourceUrl == null || resourceUrl.isBlank()) {
            return false;
        }
        String normalizedUrl = resourceUrl.trim().toLowerCase(Locale.ROOT);
        if (normalizedUrl.startsWith("/uploads/")) {
            return true;
        }
        String publicBaseUrl = normalizeBaseUrl(ossProperties.getPublicBaseUrl());
        String endpoint = normalizeBaseUrl(ossProperties.getEndpoint());
        return (!publicBaseUrl.isBlank() && normalizedUrl.startsWith(publicBaseUrl.toLowerCase(Locale.ROOT)))
            || (!endpoint.isBlank() && normalizedUrl.contains(endpoint.toLowerCase(Locale.ROOT)));
    }

    /**
     * 按日期分片生成 OSS 对象键。
     * 保留业务前缀和随机后缀，便于按书籍或模块清理资源并规避文件名冲突。
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
     * 统一拼接 OSS 对外访问地址。
     * 优先使用自定义域名或 CDN 域名，未配置时回退到 Bucket endpoint 访问地址。
     */
    private String buildPublicUrl(String objectName) {
        String publicBaseUrl = normalizeBaseUrl(ossProperties.getPublicBaseUrl());
        if (!publicBaseUrl.isBlank()) {
            return publicBaseUrl + "/" + objectName;
        }
        String endpoint = normalizeEndpointForUrl(ossProperties.getEndpoint());
        if (!endpoint.isBlank() && ossProperties.getBucket() != null && !ossProperties.getBucket().isBlank()) {
            return "https://" + ossProperties.getBucket() + "." + endpoint + "/" + objectName;
        }
        return PROXY_BASE_URL + "/" + objectName;
    }

    /**
     * 清洗对象路径片段。
     * 避免业务前缀混入特殊字符，保证 OSS 对象键稳定可控。
     */
    private String sanitizePathSegment(String value) {
        if (value == null || value.isBlank()) {
            return "content-import";
        }
        return value.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9/_-]+", "-");
    }

    /**
     * 提取上传文件扩展名。
     * 保留常见后缀用于静态资源识别，缺省时回落为无扩展对象。
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
     * 避免对象访问地址拼接时出现双斜杠，保证正文资源链接格式稳定。
     */
    private String normalizeBaseUrl(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    /**
     * 将 endpoint 转为可拼接的主机名。
     * 去掉协议头和尾部斜杠，供未配置公开域名时构造默认访问地址。
     */
    private String normalizeEndpointForUrl(String value) {
        String endpoint = normalizeBaseUrl(value);
        if (endpoint.startsWith("https://")) {
            return endpoint.substring("https://".length());
        }
        if (endpoint.startsWith("http://")) {
            return endpoint.substring("http://".length());
        }
        return endpoint;
    }
}
