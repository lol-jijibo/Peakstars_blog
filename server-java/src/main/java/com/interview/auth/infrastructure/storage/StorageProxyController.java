package com.interview.auth.infrastructure.storage;

import com.interview.auth.config.MinioProperties;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.StatObjectArgs;
import jakarta.servlet.http.HttpServletRequest;
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
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 业务目的：统一代理后台上传资源访问，兼容本地文件与 MinIO 对象存储两套来源。
 * 业务逻辑：优先读取本地上传目录，找不到时再回退到 MinIO，保证前端始终通过 /uploads 路径稳定访问。
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
     * 业务目的：代理 /uploads/ 路径下的资源请求，保证编辑器与前台页面都能直接预览上传图片。
     * 业务逻辑：本地存在则直接返回本地文件流，不存在则拉取 MinIO 对象流并透传响应头。
     */
    @GetMapping("/uploads/**")
    public ResponseEntity<?> serveUploadedFile(HttpServletRequest request) {
        String filePath = request.getRequestURI().substring("/uploads/".length());

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
                    .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(Files.size(localFile)))
                    .body(new InputStreamResource(Files.newInputStream(localFile)));
            } catch (Exception exception) {
                log.warn("读取本地上传文件失败: {}", localFile, exception);
            }
        }

        try {
            InputStream minioStream = minioClient.getObject(
                GetObjectArgs.builder()
                    .bucket(minioProperties.getBucket())
                    .object(filePath)
                    .build()
            );
            long contentLength = minioClient.statObject(
                StatObjectArgs.builder()
                    .bucket(minioProperties.getBucket())
                    .object(filePath)
                    .build()
            ).size();
            String contentType = guessContentType(filePath);

            return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, contentType)
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=86400")
                .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(contentLength))
                .body(new InputStreamResource(minioStream));
        } catch (Exception exception) {
            log.debug("MinIO 中未找到资源: {}", filePath);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 业务目的：为代理返回的图片资源补齐稳定的媒体类型，避免编辑区回显时被浏览器按未知文件处理。
     * 业务逻辑：优先按文件名后缀推断类型，推断失败时再兜底到常见图片 MIME。
     */
    private String guessContentType(String filePath) {
        String inferredContentType = MediaTypeFactory.getMediaType(filePath)
            .map(MediaType::toString)
            .orElse(null);
        if (inferredContentType != null) {
            return inferredContentType;
        }

        if (filePath.endsWith(".png")) return MediaType.IMAGE_PNG_VALUE;
        if (filePath.endsWith(".jpg") || filePath.endsWith(".jpeg")) return MediaType.IMAGE_JPEG_VALUE;
        if (filePath.endsWith(".gif")) return MediaType.IMAGE_GIF_VALUE;
        if (filePath.endsWith(".webp")) return "image/webp";
        if (filePath.endsWith(".svg")) return "image/svg+xml";
        return MediaType.APPLICATION_OCTET_STREAM_VALUE;
    }
}
