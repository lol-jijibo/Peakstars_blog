package com.interview.auth.infrastructure.storage;

import com.interview.auth.common.BusinessException;
import com.interview.auth.config.AliyunOssProperties;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

/**
 * 统一管理不同业务模块对应的对象存储路由规则。
 * 按模块返回 OSS 或默认存储实现，保证后台图片和附件统一沉淀到阿里云 OSS。
 */
@Service
@RequiredArgsConstructor
public class StorageRoutingService {

    private final ObjectProvider<MinioContentStorageService> minioStorageProvider;
    private final ObjectProvider<AliyunOssContentStorageService> ossStorageProvider;
    private final ObjectProvider<ContentStorageService> contentStorageProvider;
    private final ObjectProvider<AliyunOssProperties> ossPropertiesProvider;

    /**
     * 按业务模块选择目标存储服务。
     * 面经、技术文章、书籍等上传入口固定走 OSS，其余模块回退到默认存储。
     */
    public ContentStorageService resolveForModule(String moduleType) {
        StorageTarget target = resolveTarget(moduleType);
        return switch (target) {
            case MINIO -> requireStorage(minioStorageProvider.getIfAvailable(), "MinIO");
            case OSS -> requireStorage(ossStorageProvider.getIfAvailable(), "阿里云 OSS");
            case AUTO -> requireStorage(contentStorageProvider.getIfAvailable(), "默认对象存储");
        };
    }

    /**
     * 判断当前模块应该路由到哪类对象存储。
     * 通过稳定的模块标识做映射，避免前后端各自维护一套分流规则。
     */
    public StorageTarget resolveTarget(String moduleType) {
        String normalized = moduleType == null ? "" : moduleType.trim().toLowerCase(Locale.ROOT);
        AliyunOssProperties ossProperties = ossPropertiesProvider.getIfAvailable();
        if (ossProperties == null || !ossProperties.isEnabled()) {
            return StorageTarget.AUTO;
        }
        return switch (normalized) {
            case "interview", "interview-rich-text", "interview-cover",
                 "tech", "tech-article", "tech-article-cover", "tech-article-rich-text",
                 "ai",
                 "book", "book-cover", "book-rich-text", "book-import", "book-source" -> StorageTarget.OSS;
            default -> StorageTarget.AUTO;
        };
    }

    /**
     * 校验目标存储实现是否已就绪。
     * 在业务明确要求某类存储时直接抛出异常，避免资源悄悄写入错误的对象存储中。
     */
    private ContentStorageService requireStorage(ContentStorageService storageService, String storageName) {
        if (storageService == null) {
            throw new BusinessException(500, storageName + " 未启用或配置缺失");
        }
        return storageService;
    }
}
