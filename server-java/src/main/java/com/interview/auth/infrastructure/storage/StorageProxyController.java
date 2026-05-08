package com.interview.auth.infrastructure.storage;

import com.interview.auth.config.MinioProperties;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 上传文件代理控制器。
 * 优先从本地文件系统读取，本地文件不存在时回退到 MinIO 对象存储，
 * 解决本地存储切换 MinIO 后旧数据中 /uploads/ 路径图片 404 的问题。
 */
@RestController
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.storage.minio", name = "enabled", havingValue = "true")
public class StorageProxyController {

    @Value("${app.storage.local.upload-dir:${java.io.tmpdir}/peakstars-uploads}")
    private String uploadDir;

    private final MinioClient minioClient;
    private final MinioProperties minioProperties;

    /**
     * 代理 /uploads/ 路径下的文件请求。
     * 先查本地文件系统，找不到时再从 MinIO 拉取并返回。
     */
    @GetMapping("/uploads/**")
    public ResponseEntity<?> serveUploadedFile(HttpServletRequest request) {
        String filePath = request.getRequestURI().substring("/uploads/".length());

        // 1. 优先从本地文件系统读取
        Path localFile = Paths.get(uploadDir).resolve(filePath).normalize();
        if (Files.exists(localFile) && Files.isRegularFile(localFile)) {
            try {
                String contentType = Files.probeContentType(localFile);
                if (contentType == null) {
                    contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
                }
                return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_TYPE, contentType)
                    .header(HttpHeaders.CACHE_CONTROL, "public, max-age=86400")
                    .body(new InputStreamResource(Files.newInputStream(localFile)));
            } catch (Exception e) {
                log.warn("读取本地文件失败: {}", localFile, e);
            }
        }

        // 2. 本地不存在时从 MinIO 拉取
        try (InputStream minioStream = minioClient.getObject(
            GetObjectArgs.builder()
                .bucket(minioProperties.getBucket())
                .object(filePath)
                .build()
        )) {
            String contentType = guessContentType(filePath);
            return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, contentType)
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=86400")
                .body(new InputStreamResource(minioStream));
        } catch (Exception e) {
            log.debug("MinIO 中也未找到文件: {}", filePath);
            return ResponseEntity.notFound().build();
        }
    }
    // 根据文件后缀猜测 Content-Type,手动判断文件 ContentType（媒体类型）
    private String guessContentType(String filePath) {
        if (filePath.endsWith(".png")) return MediaType.IMAGE_PNG_VALUE;
        if (filePath.endsWith(".jpg") || filePath.endsWith(".jpeg")) return MediaType.IMAGE_JPEG_VALUE;
        if (filePath.endsWith(".gif")) return MediaType.IMAGE_GIF_VALUE;
        if (filePath.endsWith(".webp")) return "image/webp";
        if (filePath.endsWith(".svg")) return "image/svg+xml";
        return MediaType.APPLICATION_OCTET_STREAM_VALUE;
    }
}
