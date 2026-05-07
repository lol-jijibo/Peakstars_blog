package com.interview.auth.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 统一承接后台内容导入链路的运行参数。
 * 通过集中配置资源抓取、白名单清洗与附件识别规则，保证不同来源的面经导入都走同一套企业级标准。
 */
@ConfigurationProperties(prefix = "app.content-import")
public class ContentImportProperties {

    private boolean enabled = true;
    private boolean migrateAssets = true;
    private boolean strictAssetMigration = false;
    private int connectTimeoutMillis = 5000;
    private int readTimeoutMillis = 15000;
    private long maxAssetSizeBytes = 10L * 1024 * 1024;
    private String uploadPrefix = "content-import";
    private List<String> allowedImageContentTypes = List.of(
        "image/png",
        "image/jpeg",
        "image/jpg",
        "image/gif",
        "image/webp",
        "image/svg+xml"
    );
    private List<String> allowedAttachmentContentTypes = List.of(
        "application/pdf",
        "application/zip",
        "application/x-zip-compressed",
        "application/msword",
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
        "application/vnd.ms-excel",
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
        "application/vnd.ms-powerpoint",
        "application/vnd.openxmlformats-officedocument.presentationml.presentation",
        "text/plain"
    );
    private List<String> attachmentExtensions = List.of(
        "pdf",
        "zip",
        "doc",
        "docx",
        "xls",
        "xlsx",
        "ppt",
        "pptx",
        "txt",
        "md"
    );

    /**
     * 本地静态资源目录，用于将相对路径（如 /peakstars-blog-icon.jpg）的图片上传到对象存储。
     * 默认指向项目根目录下的 public 文件夹。
     */
    private String localStaticDir = "public";

    /**
     * 是否启用本地相对路径资源迁移到对象存储。
     * 开启后，封面图或正文中的 /xxx.jpg 形式路径会从本地目录读取并上传到 MinIO。
     */
    private boolean migrateLocalAssets = true;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isMigrateAssets() {
        return migrateAssets;
    }

    public void setMigrateAssets(boolean migrateAssets) {
        this.migrateAssets = migrateAssets;
    }

    public boolean isStrictAssetMigration() {
        return strictAssetMigration;
    }

    public void setStrictAssetMigration(boolean strictAssetMigration) {
        this.strictAssetMigration = strictAssetMigration;
    }

    public int getConnectTimeoutMillis() {
        return connectTimeoutMillis;
    }

    public void setConnectTimeoutMillis(int connectTimeoutMillis) {
        this.connectTimeoutMillis = connectTimeoutMillis;
    }

    public int getReadTimeoutMillis() {
        return readTimeoutMillis;
    }

    public void setReadTimeoutMillis(int readTimeoutMillis) {
        this.readTimeoutMillis = readTimeoutMillis;
    }

    public long getMaxAssetSizeBytes() {
        return maxAssetSizeBytes;
    }

    public void setMaxAssetSizeBytes(long maxAssetSizeBytes) {
        this.maxAssetSizeBytes = maxAssetSizeBytes;
    }

    public String getUploadPrefix() {
        return uploadPrefix;
    }

    public void setUploadPrefix(String uploadPrefix) {
        this.uploadPrefix = uploadPrefix;
    }

    public List<String> getAllowedImageContentTypes() {
        return allowedImageContentTypes;
    }

    public void setAllowedImageContentTypes(List<String> allowedImageContentTypes) {
        this.allowedImageContentTypes = allowedImageContentTypes;
    }

    public List<String> getAllowedAttachmentContentTypes() {
        return allowedAttachmentContentTypes;
    }

    public void setAllowedAttachmentContentTypes(List<String> allowedAttachmentContentTypes) {
        this.allowedAttachmentContentTypes = allowedAttachmentContentTypes;
    }

    public List<String> getAttachmentExtensions() {
        return attachmentExtensions;
    }

    public void setAttachmentExtensions(List<String> attachmentExtensions) {
        this.attachmentExtensions = attachmentExtensions;
    }

    public String getLocalStaticDir() {
        return localStaticDir;
    }

    public void setLocalStaticDir(String localStaticDir) {
        this.localStaticDir = localStaticDir;
    }

    public boolean isMigrateLocalAssets() {
        return migrateLocalAssets;
    }

    public void setMigrateLocalAssets(boolean migrateLocalAssets) {
        this.migrateLocalAssets = migrateLocalAssets;
    }
}
