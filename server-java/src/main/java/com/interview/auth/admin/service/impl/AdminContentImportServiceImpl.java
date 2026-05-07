package com.interview.auth.admin.service.impl;

import com.interview.auth.admin.dto.request.AdminContentImportPreviewRequest;
import com.interview.auth.admin.dto.response.AdminContentImportPreviewResponse;
import com.interview.auth.admin.dto.response.AdminImportedAssetResponse;
import com.interview.auth.admin.service.AdminContentImportService;
import com.interview.auth.common.BusinessException;
import com.interview.auth.config.ContentImportProperties;
import com.interview.auth.infrastructure.storage.ContentStorageService;
import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLConnection;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.safety.Cleaner;
import org.jsoup.safety.Safelist;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

/**
 * 统一承接后台外部内容导入标准化实现。
 * 使用 jsoup 做 HTML 白名单清洗，并在保存前将图片和附件迁移到 MinIO，确保面经内容格式稳定可控。
 */
@Service
@RequiredArgsConstructor
public class AdminContentImportServiceImpl implements AdminContentImportService {

    private static final String IMAGE = "image";
    private static final String ATTACHMENT = "attachment";
    private static final Set<String> ALLOWED_LINK_PROTOCOLS = Set.of("http", "https", "mailto");

    private final ContentImportProperties contentImportProperties;
    private final ObjectProvider<ContentStorageService> storageServiceProvider;

    /**
     * 预处理外部内容导入请求。
     * 统一复用正式保存链路的标准化规则，保证后台预览结果与最终入库内容保持一致。
     */
    @Override
    public AdminContentImportPreviewResponse preview(String contentType, AdminContentImportPreviewRequest request) {
        return normalizeHtml(
            contentType,
            request.getContentHtml(),
            request.getSourceType(),
            request.getSourceUrl(),
            Boolean.TRUE.equals(request.getMigrateAssets())
        );
    }

    /**
     * 标准化正文 HTML。
     * 先解析并迁移图片附件，再执行 jsoup 白名单清洗，最后输出适合前台直接渲染的标准 HTML。
     */
    @Override
    public AdminContentImportPreviewResponse normalizeHtml(
        String contentType,
        String html,
        String sourceType,
        String sourceUrl,
        boolean migrateAssets
    ) {
        if (html == null || html.isBlank()) {
            throw new BusinessException(400, "导入内容不能为空");
        }

        // 预处理：将连续的换行段落转为 <p> 标签，避免纯文本换行在 HTML 中被折叠成一行
        String preprocessedHtml = ensureParagraphStructure(html);
        Document rawDocument = Jsoup.parseBodyFragment(preprocessedHtml);
        AdminContentImportPreviewResponse response = new AdminContentImportPreviewResponse();
        response.setSourceType(defaultString(sourceType, "html"));

        if (contentImportProperties.isEnabled()) {
            migrateDocumentImages(contentType, rawDocument, sourceUrl, migrateAssets, response);
            migrateDocumentAttachments(contentType, rawDocument, sourceUrl, migrateAssets, response);
        }

        Document sanitizedDocument = new Cleaner(buildSafelist()).clean(rawDocument);
        removeEmptyNodes(sanitizedDocument);
        response.setNormalizedHtml(sanitizedDocument.body().html());
        response.setPlainText(sanitizedDocument.text());
        return response;
    }

    /**
     * 标准化封面图链接。
     * 仅处理明显的外部资源地址，迁移失败时回退原链接并保留正文保存能力。
     */
    @Override
    public String normalizeCoverUrl(String contentType, String coverUrl) {
        if (coverUrl == null || coverUrl.isBlank()) {
            return coverUrl;
        }

        try {
            AssetMigrationResult result = migrateAsset(contentType, coverUrl, null, IMAGE, true);
            return result.targetUrl();
        } catch (Exception exception) {
            return coverUrl;
        }
    }

    /**
     * 迁移正文中的图片资源。
     * 统一扫描 img 节点并回填 MinIO 链接，确保前台详情页不再依赖外部图床。
     */
    private void migrateDocumentImages(
        String contentType,
        Document document,
        String sourceUrl,
        boolean migrateAssets,
        AdminContentImportPreviewResponse response
    ) {
        for (Element image : document.select("img[src]")) {
            String originalSrc = image.attr("src");
            if (originalSrc == null || originalSrc.isBlank()) {
                continue;
            }

            try {
                AssetMigrationResult result = migrateAsset(contentType, originalSrc, sourceUrl, IMAGE, migrateAssets);
                image.attr("src", result.targetUrl());
                response.getAssets().add(toAssetResponse(result, IMAGE));
            } catch (Exception exception) {
                handleAssetFailure(originalSrc, IMAGE, response, exception);
            }
        }
    }

    /**
     * 迁移正文中的附件资源。
     * 仅处理明显的文件下载链接，避免普通网页跳转被误判为附件而影响原文链接语义。
     */
    private void migrateDocumentAttachments(
        String contentType,
        Document document,
        String sourceUrl,
        boolean migrateAssets,
        AdminContentImportPreviewResponse response
    ) {
        for (Element link : document.select("a[href]")) {
            String originalHref = link.attr("href");
            if (!isAttachmentCandidate(link, originalHref)) {
                continue;
            }

            try {
                AssetMigrationResult result = migrateAsset(contentType, originalHref, sourceUrl, ATTACHMENT, migrateAssets);
                link.attr("href", result.targetUrl());
                link.attr("target", "_blank");
                link.attr("rel", "noopener noreferrer");
                response.getAssets().add(toAssetResponse(result, ATTACHMENT));
            } catch (Exception exception) {
                handleAssetFailure(originalHref, ATTACHMENT, response, exception);
            }
        }
    }

    /**
     * 迁移单个资源。
     * 同时兼容 data URL、本地相对路径、HTTP 外链与已存在的自有存储链接，并在需要时执行对象存储上传。
     */
    private AssetMigrationResult migrateAsset(
        String contentType,
        String originalUrl,
        String sourceUrl,
        String assetType,
        boolean migrateAssets
    ) throws Exception {
        String resolvedUrl = resolveResourceUrl(originalUrl, sourceUrl);
        ContentStorageService storageService = storageServiceProvider.getIfAvailable();

        if (!shouldMigrateAsset(migrateAssets, storageService, resolvedUrl)) {
            return new AssetMigrationResult(originalUrl, resolvedUrl, "skipped", "资源迁移已跳过");
        }

        // 优先判断本地相对路径资源（如 /peakstars-blog-icon.jpg），从本地静态目录读取并上传到对象存储
        if (isLocalRelativePath(resolvedUrl) && contentImportProperties.isMigrateLocalAssets()) {
            return migrateLocalAsset(contentType, resolvedUrl, assetType, storageService, originalUrl);
        }

        DownloadedAsset asset = resolvedUrl.startsWith("data:")
            ? decodeDataUrl(resolvedUrl, assetType)
            : downloadRemoteAsset(resolvedUrl, assetType);

        validateAsset(asset, assetType, resolvedUrl);
        String targetUrl = storageService.upload(
            buildObjectPrefix(contentType, assetType),
            asset.fileName(),
            asset.inputStream(),
            asset.size(),
            asset.contentType()
        );
        return new AssetMigrationResult(originalUrl, targetUrl, "migrated", "资源已迁移到对象存储");
    }

    /**
     * 判断 URL 是否为本地相对路径。
     * 以 / 开头且不含协议前缀的路径视为本地资源，如 /peakstars-blog-icon.jpg。
     */
    private boolean isLocalRelativePath(String url) {
        if (url == null || url.isBlank()) {
            return false;
        }
        String trimmed = url.trim();
        return trimmed.startsWith("/") && !trimmed.startsWith("//");
    }

    /**
     * 从本地静态资源目录读取文件并上传到对象存储。
     * 将项目 public 目录下的相对路径资源迁移到 MinIO，实现封面图和正文图片的统一对象存储管理。
     */
    private AssetMigrationResult migrateLocalAsset(
        String contentType,
        String localPath,
        String assetType,
        ContentStorageService storageService,
        String originalUrl
    ) throws Exception {
        String staticDir = contentImportProperties.getLocalStaticDir();
        Path filePath = Paths.get(staticDir, localPath).normalize();

        if (!Files.exists(filePath) || !Files.isRegularFile(filePath)) {
            return new AssetMigrationResult(originalUrl, localPath, "skipped", "本地资源文件不存在: " + localPath);
        }

        long fileSize = Files.size(filePath);
        if (fileSize > contentImportProperties.getMaxAssetSizeBytes()) {
            return new AssetMigrationResult(originalUrl, localPath, "skipped", "本地资源超过大小限制: " + localPath);
        }

        String fileName = filePath.getFileName().toString();
        String mimeType = Files.probeContentType(filePath);
        if (mimeType == null || mimeType.isBlank()) {
            mimeType = URLConnection.guessContentTypeFromName(fileName);
        }
        if (mimeType == null || mimeType.isBlank()) {
            mimeType = IMAGE.equals(assetType) ? "application/octet-stream" : "application/octet-stream";
        }

        try (var inputStream = Files.newInputStream(filePath)) {
            String targetUrl = storageService.upload(
                buildObjectPrefix(contentType, assetType),
                fileName,
                inputStream,
                fileSize,
                mimeType
            );
            return new AssetMigrationResult(originalUrl, targetUrl, "migrated", "本地资源已迁移到对象存储");
        }
    }

    /**
     * 判断当前资源是否需要迁移。
     * 当导入策略关闭资源迁移或对象存储未启用时直接保留原链接，避免后台基础内容编辑能力被阻断。
     */
    private boolean shouldMigrateAsset(boolean migrateAssets, ContentStorageService storageService, String resolvedUrl) {
        return contentImportProperties.isMigrateAssets()
            && migrateAssets
            && storageService != null
            && resolvedUrl != null
            && !resolvedUrl.isBlank()
            && !storageService.isStorageUrl(resolvedUrl);
    }

    /**
     * 解析资源真实访问地址。
     * 对相对路径资源根据来源页面补齐绝对地址，兼容语雀等编辑器导出的站内相对资源链接。
     */
    private String resolveResourceUrl(String resourceUrl, String sourceUrl) {
        if (resourceUrl == null || resourceUrl.isBlank()) {
            return resourceUrl;
        }
        if (resourceUrl.startsWith("data:")) {
            return resourceUrl;
        }
        try {
            URI uri = new URI(resourceUrl);
            if (uri.isAbsolute() || sourceUrl == null || sourceUrl.isBlank()) {
                return resourceUrl;
            }
            return new URI(sourceUrl).resolve(uri).toString();
        } catch (URISyntaxException exception) {
            return resourceUrl;
        }
    }

    /**
     * 下载外部图片或附件。
     * 使用 Java 17 原生 HttpClient 执行资源抓取，并统一应用超时与大小限制策略。
     */
    private DownloadedAsset downloadRemoteAsset(String resourceUrl, String assetType) throws Exception {
        HttpClient httpClient = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(Duration.ofMillis(contentImportProperties.getConnectTimeoutMillis()))
            .build();

        HttpRequest request = HttpRequest.newBuilder(URI.create(resourceUrl))
            .GET()
            .timeout(Duration.ofMillis(contentImportProperties.getReadTimeoutMillis()))
            .header("User-Agent", "PeakStars-Content-Importer/1.0")
            .build();

        HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new BusinessException(400, "外部资源下载失败: " + response.statusCode());
        }

        byte[] bytes = response.body();
        if (bytes.length == 0) {
            throw new BusinessException(400, "外部资源内容为空");
        }

        String contentType = firstHeader(response.headers().map(), "content-type");
        String fileName = extractFileName(resourceUrl, contentType, assetType);
        return new DownloadedAsset(bytes, contentType, fileName);
    }

    /**
     * 解析 Base64 内嵌资源。
     * 将富文本中的 data URL 解码为标准二进制资源，再复用统一对象存储上传链路。
     */
    private DownloadedAsset decodeDataUrl(String dataUrl, String assetType) {
        int commaIndex = dataUrl.indexOf(',');
        if (commaIndex < 0) {
            throw new BusinessException(400, "Base64 资源格式不合法");
        }

        String meta = dataUrl.substring(5, commaIndex);
        String payload = dataUrl.substring(commaIndex + 1);
        boolean base64Encoded = meta.contains(";base64");
        String contentType = meta.split(";")[0];
        byte[] bytes = base64Encoded
            ? Base64.getDecoder().decode(payload)
            : payload.getBytes(StandardCharsets.UTF_8);
        String extension = guessExtension(contentType, assetType);
        String fileName = "embedded-" + UUID.randomUUID().toString().replace("-", "") + extension;
        return new DownloadedAsset(bytes, contentType, fileName);
    }

    /**
     * 校验迁移资源是否合法。
     * 统一约束大小、MIME 类型与附件白名单，避免不受控外部内容直接进入对象存储。
     */
    private void validateAsset(DownloadedAsset asset, String assetType, String resourceUrl) {
        if (asset.size() > contentImportProperties.getMaxAssetSizeBytes()) {
            throw new BusinessException(400, "资源大小超过限制: " + resourceUrl);
        }

        String normalizedContentType = defaultString(asset.contentType(), URLConnection.guessContentTypeFromName(asset.fileName()));
        if (IMAGE.equals(assetType)) {
            if (!matchesAllowedContentType(normalizedContentType, contentImportProperties.getAllowedImageContentTypes())) {
                throw new BusinessException(400, "图片类型不在白名单内: " + normalizedContentType);
            }
            return;
        }

        boolean matchesMimeType = matchesAllowedContentType(normalizedContentType, contentImportProperties.getAllowedAttachmentContentTypes());
        boolean matchesExtension = matchesAttachmentExtension(asset.fileName());
        if (!matchesMimeType && !matchesExtension) {
            throw new BusinessException(400, "附件类型不在白名单内: " + resourceUrl);
        }
    }

    /**
     * 识别链接是否属于附件候选。
     * 通过 download 属性、常见文件扩展名和链接语义缩小迁移范围，避免把普通站点跳转误当成附件处理。
     */
    private boolean isAttachmentCandidate(Element link, String href) {
        if (href == null || href.isBlank()) {
            return false;
        }

        if (link.hasAttr("download")) {
            return true;
        }

        String normalizedHref = href.toLowerCase(Locale.ROOT);
        return contentImportProperties.getAttachmentExtensions().stream()
            .map(extension -> "." + extension.toLowerCase(Locale.ROOT))
            .anyMatch(normalizedHref::contains);
    }

    /**
     * 构建 HTML 白名单规则。
     * 在保留标题、列表、代码块、表格、图片和附件链接等核心格式的同时，过滤脚本类危险标签。
     */
    private Safelist buildSafelist() {
        Safelist safelist = Safelist.none()
            .addTags(
                "p", "br", "div", "span",
                "h1", "h2", "h3", "h4", "h5", "h6",
                "blockquote", "pre", "code",
                "ul", "ol", "li",
                "strong", "b", "em", "i", "u", "s", "del",
                "table", "thead", "tbody", "tfoot", "tr", "th", "td",
                "hr", "img", "a", "figure", "figcaption"
            )
            .addAttributes(":all", "class", "style", "id", "title")
            .addAttributes("img", "src", "alt", "width", "height")
            .addAttributes("a", "href", "target", "rel", "download")
            .addAttributes("code", "data-language")
            .addProtocols("img", "src", "http", "https", "data");

        for (String protocol : ALLOWED_LINK_PROTOCOLS) {
            safelist.addProtocols("a", "href", protocol);
        }
        safelist.preserveRelativeLinks(true);
        return safelist;
    }

    /**
     * 统一处理资源迁移失败的兜底结果。
     * 将失败原因回传给后台预览界面，并按配置决定是仅保留告警还是中断导入流程。
     */
    private void handleAssetFailure(
        String sourceUrl,
        String assetType,
        AdminContentImportPreviewResponse response,
        Exception exception
    ) {
        String message = defaultString(exception.getMessage(), "资源迁移失败");
        AdminImportedAssetResponse assetResponse = new AdminImportedAssetResponse();
        assetResponse.setAssetType(assetType);
        assetResponse.setSourceUrl(sourceUrl);
        assetResponse.setTargetUrl(sourceUrl);
        assetResponse.setStatus("failed");
        assetResponse.setMessage(message);
        response.getAssets().add(assetResponse);
        response.getWarnings().add("%s 迁移失败: %s".formatted(assetType, sourceUrl));
        if (contentImportProperties.isStrictAssetMigration()) {
            throw new BusinessException(400, message);
        }
    }

    /**
     * 将内部资源迁移结果转换为返回对象。
     * 统一透出资源类型、原始地址与目标地址，便于后台在导入预览中展示迁移明细。
     */
    private AdminImportedAssetResponse toAssetResponse(AssetMigrationResult result, String assetType) {
        AdminImportedAssetResponse response = new AdminImportedAssetResponse();
        response.setAssetType(assetType);
        response.setSourceUrl(result.sourceUrl());
        response.setTargetUrl(result.targetUrl());
        response.setStatus(result.status());
        response.setMessage(result.message());
        return response;
    }

    /**
     * 构建对象存储业务目录前缀。
     * 使用统一业务前缀加内容类型和资源类型分层，便于后续按模块治理对象存储目录。
     */
    private String buildObjectPrefix(String contentType, String assetType) {
        return "%s/%s/%s".formatted(
            defaultString(contentImportProperties.getUploadPrefix(), "content-import"),
            defaultString(contentType, "common"),
            assetType
        );
    }

    /**
     * 从响应头中读取首个指定值。
     * 统一兜底内容类型解析，减少外部资源响应头大小写差异带来的兼容问题。
     */
    private String firstHeader(Map<String, List<String>> headers, String headerName) {
        return headers.entrySet().stream()
            .filter(entry -> Objects.equals(entry.getKey().toLowerCase(Locale.ROOT), headerName.toLowerCase(Locale.ROOT)))
            .flatMap(entry -> entry.getValue().stream())
            .findFirst()
            .map(value -> value.split(";")[0].trim())
            .orElse("");
    }

    /**
     * 提取资源文件名。
     * 优先从 URL 路径识别文件名，缺失时再根据资源类型和 MIME 自动推断后缀。
     */
    private String extractFileName(String resourceUrl, String contentType, String assetType) {
        try {
            URI uri = URI.create(resourceUrl);
            String path = Optional.ofNullable(uri.getPath()).orElse("");
            if (!path.isBlank() && path.contains("/")) {
                String candidate = path.substring(path.lastIndexOf('/') + 1);
                if (!candidate.isBlank()) {
                    return candidate;
                }
            }
        } catch (Exception ignored) {
            // 解析失败时走兜底文件名。
        }

        return "resource-" + UUID.randomUUID().toString().replace("-", "") + guessExtension(contentType, assetType);
    }

    /**
     * 根据 MIME 类型推断文件扩展名。
     * 在外部资源缺失标准文件名时补齐常见后缀，确保对象存储资源更容易被浏览器识别。
     */
    private String guessExtension(String contentType, String assetType) {
        if (contentType == null || contentType.isBlank()) {
            return IMAGE.equals(assetType) ? ".png" : ".bin";
        }

        Map<String, String> extensionMap = Map.ofEntries(
            Map.entry("image/png", ".png"),
            Map.entry("image/jpeg", ".jpg"),
            Map.entry("image/jpg", ".jpg"),
            Map.entry("image/gif", ".gif"),
            Map.entry("image/webp", ".webp"),
            Map.entry("image/svg+xml", ".svg"),
            Map.entry("application/pdf", ".pdf"),
            Map.entry("application/zip", ".zip"),
            Map.entry("application/x-zip-compressed", ".zip"),
            Map.entry("application/msword", ".doc"),
            Map.entry("application/vnd.openxmlformats-officedocument.wordprocessingml.document", ".docx"),
            Map.entry("application/vnd.ms-excel", ".xls"),
            Map.entry("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", ".xlsx"),
            Map.entry("application/vnd.ms-powerpoint", ".ppt"),
            Map.entry("application/vnd.openxmlformats-officedocument.presentationml.presentation", ".pptx"),
            Map.entry("text/plain", ".txt")
        );
        return extensionMap.getOrDefault(contentType.toLowerCase(Locale.ROOT), IMAGE.equals(assetType) ? ".png" : ".bin");
    }

    /**
     * 校验内容类型是否命中白名单。
     * 通过统一前缀匹配适配带 charset 的响应头返回值，减少第三方资源类型写法差异导致的误判。
     */
    private boolean matchesAllowedContentType(String contentType, List<String> allowedContentTypes) {
        if (contentType == null || contentType.isBlank()) {
            return false;
        }
        String normalizedType = contentType.toLowerCase(Locale.ROOT);
        return allowedContentTypes.stream()
            .map(item -> item.toLowerCase(Locale.ROOT))
            .anyMatch(normalizedType::startsWith);
    }

    /**
     * 校验附件扩展名是否在白名单内。
     * 作为附件 MIME 缺失时的兜底策略，保证常见文档资源仍可顺利迁移。
     */
    private boolean matchesAttachmentExtension(String fileName) {
        if (fileName == null || fileName.isBlank() || !fileName.contains(".")) {
            return false;
        }
        String extension = fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
        return contentImportProperties.getAttachmentExtensions().stream()
            .map(item -> item.toLowerCase(Locale.ROOT))
            .anyMatch(extension::equals);
    }

    /**
     * 移除 Jsoup 清洗后残留的空段落、空列表项和空容器节点。
     * Jsoup Cleaner 不会主动删除无内容节点，需手动遍历 body 清理，
     * 避免导入后的 HTML 在富文本编辑器中产生多余空行。
     * 对 li 元素排除嵌套 ul/ol 的文字，只判断自身内容是否为空。
     */
    private void removeEmptyNodes(Document document) {
        boolean changed = true;
        while (changed) {
            changed = false;

            // 删除空 p / div / span / blockquote
            for (Element element : document.body().select("p, div, span, blockquote")) {
                String text = element.text().replaceAll("[\\s\\u00a0\\u3000\\u200B-\\u200F\\uFEFF]", "");
                int mediaCount = element.select("img, table, pre, code, iframe").size();
                boolean isNumberOnly = text.matches("\\d+[.:：]?");
                if ((text.isEmpty() || isNumberOnly) && mediaCount == 0) {
                    element.remove();
                    changed = true;
                }
            }

            // 删除空 li：计算 li 自身文字 = 全部文字 - 嵌套列表文字
            for (Element li : document.body().select("li")) {
                String allText = li.text().replaceAll("[\\s\\u00a0\\u3000\\u200B-\\u200F\\uFEFF]", "");
                String nestedListText = li.select("ul, ol").stream()
                    .map(el -> el.text())
                    .collect(java.util.stream.Collectors.joining())
                    .replaceAll("[\\s\\u00a0\\u3000\\u200B-\\u200F\\uFEFF]", "");
                String ownContent = allText.replace(nestedListText, "");
                int mediaCount = li.select("img, table, pre, code, iframe").size();
                boolean hasNonEmptyNestedLists = !li.select("ul > li, ol > li").isEmpty();
                boolean isNumberOnly = ownContent.matches("\\d+[.:：]?");

                if ((ownContent.isEmpty() || isNumberOnly) && mediaCount == 0 && !hasNonEmptyNestedLists) {
                    li.remove();
                    changed = true;
                }
            }

            // 删除所有 li 都被清空的 ul/ol
            for (Element list : document.body().select("ul, ol")) {
                if (list.select("li").isEmpty()) {
                    list.remove();
                    changed = true;
                }
            }
        }
    }

    /**
     * 将换行分隔的纯文本内容转换为带 <p> 标签的标准 HTML 段落结构。
     * 预处理输入时，将连续两个及以上换行符之间的内容包裹成 <p> 段落，单个换行替换为 <br>。<br>
     * 如果输入已经是标准 HTML（包含任意块级标签），则原样返回，避免重复包裹。
     */
    private String ensureParagraphStructure(String html) {
        if (html == null || html.isBlank()) {
            return html;
        }

        // 如果已经包含块级 HTML 标签，说明已经是标准 HTML，不做预处理
        if (containsBlockTag(html)) {
            return html;
        }

        // 按连续换行分段（支持 \r\n、\n、\r）
        String[] paragraphs = html.split("\\R{2,}");
        List<String> wrapped = new ArrayList<>();
        for (String para : paragraphs) {
            String trimmed = para.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            // 段落内的单个换行替换为 <br>
            String inline = trimmed.replaceAll("\\R", "<br>");
            wrapped.add("<p>" + inline + "</p>");
        }

        return String.join("\n", wrapped);
    }

    /**
     * 快速检测字符串中是否包含块级 HTML 标签。
     * 用于判断输入是否已经是标准 HTML，避免重复包裹。
     */
    private boolean containsBlockTag(String html) {
        String lower = html.toLowerCase();
        return lower.contains("<p") || lower.contains("<div") || lower.contains("<h1") || lower.contains("<h2")
            || lower.contains("<h3") || lower.contains("<h4") || lower.contains("<h5") || lower.contains("<h6")
            || lower.contains("<blockquote") || lower.contains("<pre") || lower.contains("<ul")
            || lower.contains("<ol") || lower.contains("<table") || lower.contains("<hr");
    }

    /**
     * 统一填补字符串默认值。
     * 避免导入链路中多处重复判空，让 HTML 清洗和资源迁移逻辑更聚焦业务流程本身。
     */
    private String defaultString(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }

    /**
     * 承接资源迁移后的中间结果。
     * 统一保留原地址、目标地址与迁移状态，供后台预处理界面和保存链路复用。
     */
    private record AssetMigrationResult(String sourceUrl, String targetUrl, String status, String message) {
    }

    /**
     * 承接单个下载资源的二进制内容。
     * 将远程抓取结果统一标准化，便于后续执行白名单校验和对象存储上传。
     */
    private record DownloadedAsset(byte[] bytes, String contentType, String fileName) {

        /**
         * 转换为可重复上传的输入流。
         * 基于内存字节数组生成输入流，便于对象存储客户端直接消费导入资源内容。
         */
        private ByteArrayInputStream inputStream() {
            return new ByteArrayInputStream(bytes);
        }

        /**
         * 返回资源字节大小。
         * 用于统一执行对象存储上传时的长度声明和导入大小限制校验。
         */
        private long size() {
            return bytes.length;
        }
    }
}
