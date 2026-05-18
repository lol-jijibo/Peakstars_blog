package com.interview.auth.infrastructure.storage;

import com.aliyun.oss.OSS;
import com.aliyun.oss.model.OSSObject;
import com.interview.auth.config.AliyunOssProperties;
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
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 统一代理后台上传资源的访问请求，兼容本地目录、MinIO 和 OSS 三类来源。
 * 先查本地文件，再按存储配置回源对象存储，保证前端始终通过 /uploads 稳定预览。
 */
@RestController
@Slf4j
@RequiredArgsConstructor
public class StorageProxyController {

    @Value("${app.storage.local.upload-dir:${java.io.tmpdir}/peakstars-uploads}")
    private String uploadDir;

    private final ObjectProvider<MinioClient> minioClientProvider;
    private final MinioProperties minioProperties;
    private final ObjectProvider<OSS> ossClientProvider;
    private final ObjectProvider<AliyunOssProperties> ossPropertiesProvider;

    /**
     * 统一处理 /uploads 下的资源访问，保证封面图和章节插图都能直接回显。
     * 先返回本地文件，未命中时依次回源 OSS 与 MinIO，最后再返回 404。
     */
    @GetMapping("/uploads/**")
    public ResponseEntity<?> serveUploadedFile(HttpServletRequest request) {
        String filePath = request.getRequestURI().substring("/uploads/".length());

        ResponseEntity<?> localResponse = tryServeFromLocal(filePath);
        if (localResponse != null) {
            return localResponse;
        }

        ResponseEntity<?> ossResponse = tryServeFromOss(filePath);
        if (ossResponse != null) {
            return ossResponse;
        }

        ResponseEntity<?> minioResponse = tryServeFromMinio(filePath);
        if (minioResponse != null) {
            return minioResponse;
        }

        return ResponseEntity.notFound().build();
    }

    /**
     * 从本地上传目录读取资源内容，兼容未启用对象存储时的开发环境预览。
     * 命中文件后直接回传内容流和媒体类型，读取异常时记录日志并继续尝试其他来源。
     */
    private ResponseEntity<?> tryServeFromLocal(String filePath) {
        Path localFile = Paths.get(uploadDir).resolve(filePath).normalize();
        if (!Files.exists(localFile) || !Files.isRegularFile(localFile)) {
            return null;
        }
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
            return null;
        }
    }

    /**
     * 从阿里云 OSS 回源代理资源，兼容私有桶下的封面与正文图片访问。
     * 仅在 OSS 配置启用时读取对象流，并把长度与媒体类型透传给前端浏览器。
     */
    private ResponseEntity<?> tryServeFromOss(String filePath) {
        OSS ossClient = ossClientProvider.getIfAvailable();
        AliyunOssProperties ossProperties = ossPropertiesProvider.getIfAvailable();
        if (ossClient == null || ossProperties == null || !ossProperties.isEnabled()) {
            return null;
        }
        try {
            OSSObject ossObject = ossClient.getObject(ossProperties.getBucket(), filePath);
            if (ossObject == null) {
                return null;
            }
            InputStream ossStream = ossObject.getObjectContent();
            long contentLength = ossObject.getObjectMetadata() == null ? -1L : ossObject.getObjectMetadata().getContentLength();
            String contentType = ossObject.getObjectMetadata() == null ? guessContentType(filePath) : ossObject.getObjectMetadata().getContentType();
            if (contentType == null || contentType.isBlank()) {
                contentType = guessContentType(filePath);
            }

            ResponseEntity.BodyBuilder builder = ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, contentType)
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=86400");
            if (contentLength >= 0) {
                builder.header(HttpHeaders.CONTENT_LENGTH, String.valueOf(contentLength));
            }
            return builder.body(new InputStreamResource(ossStream));
        } catch (Exception exception) {
            log.debug("OSS 中未找到资源: {}", filePath);
            return null;
        }
    }

    /**
     * 从 MinIO 回源代理资源，保持既有上传链路与图片访问方式兼容。
     * 仅在 MinIO 启用时拉取对象流，未命中则继续交给上层返回统一的 404。
     */
    private ResponseEntity<?> tryServeFromMinio(String filePath) {
        MinioClient minioClient = minioClientProvider.getIfAvailable();
        if (minioClient == null || minioProperties == null || !minioProperties.isEnabled()) {
            return null;
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
            return null;
        }
    }

    /**
     * 为代理返回的图片和附件推断稳定的媒体类型，避免浏览器把资源当成未知文件下载。
     * 优先使用 Spring 的类型推断，失败后再按常见图片扩展名回退补齐 MIME。
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
