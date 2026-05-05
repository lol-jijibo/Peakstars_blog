package com.interview.auth.infrastructure.storage;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.Locale;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * 本地文件系统存储实现。
 * 当 MinIO 未启用时自动激活，将上传文件保存到本地目录并返回相对访问路径。
 * 适用于本地开发和未部署对象存储的环境。
 */
@Service
@Slf4j
@ConditionalOnProperty(prefix = "app.storage.minio", name = "enabled", havingValue = "false", matchIfMissing = true)
public class LocalContentStorageService implements ContentStorageService {

    @Value("${app.storage.local.upload-dir:${java.io.tmpdir}/peakstars-uploads}")
    private String uploadDir;

    @Value("${app.storage.local.base-url:/uploads}")
    private String baseUrl;

    @PostConstruct
    public void init() {
        try {
            Files.createDirectories(Paths.get(uploadDir));
            log.info("本地文件存储已启用，上传目录: {}", uploadDir);
        } catch (IOException e) {
            throw new IllegalStateException("无法创建上传目录: " + uploadDir, e);
        }
    }

    @Override
    public String upload(String objectPrefix, String fileName, InputStream inputStream, long size, String contentType) throws Exception {
        LocalDate today = LocalDate.now();
        String safePrefix = sanitizePathSegment(objectPrefix);
        String extension = extractExtension(fileName);
        String randomName = UUID.randomUUID().toString().replace("-", "");

        String relativePath = String.format("%s/%d/%02d/%02d/%s%s",
            safePrefix, today.getYear(), today.getMonthValue(), today.getDayOfMonth(), randomName, extension);

        Path targetFile = Paths.get(uploadDir).resolve(relativePath);
        Files.createDirectories(targetFile.getParent());
        Files.copy(inputStream, targetFile, StandardCopyOption.REPLACE_EXISTING);

        log.debug("文件已保存到本地: {}", targetFile);
        return baseUrl + "/" + relativePath;
    }

    @Override
    public boolean isStorageUrl(String resourceUrl) {
        if (resourceUrl == null || resourceUrl.isBlank()) {
            return false;
        }
        return resourceUrl.trim().startsWith(baseUrl);
    }

    private String sanitizePathSegment(String value) {
        if (value == null || value.isBlank()) {
            return "upload";
        }
        return value.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9/_-]+", "-");
    }

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
}
