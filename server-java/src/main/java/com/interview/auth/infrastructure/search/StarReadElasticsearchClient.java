package com.interview.auth.infrastructure.search;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.interview.auth.config.StarReadSearchProperties;
import com.interview.auth.domain.dto.response.StarReadBookResponse;
import com.interview.auth.domain.dto.response.StarReadSearchResponse;
import com.interview.auth.domain.dto.response.StarReadSearchSuggestionResponse;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 封装 star_read 对 Elasticsearch 的轻量调用能力。
 * 通过 HTTP 直连搜索服务，未启用时返回空结果交由业务层执行本地兜底。
 */
@Component
@RequiredArgsConstructor
public class StarReadElasticsearchClient {

    private final StarReadSearchProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newBuilder().build();

    /**
     * 判断当前环境是否已启用 Elasticsearch 搜索能力。
     * 仅在开关打开且索引与地址齐备时才发起远程检索请求。
     */
    public boolean isEnabled() {
        return properties.isEnabled()
            && hasText(properties.getEndpoint())
            && hasText(properties.getIndex());
    }

    /**
     * 使用 Elasticsearch 查询书籍结果列表并回填统一响应结构。
     * 根据关键词构造 multi_match 请求，解析命中的文档并保留分页信息。
     */
    public Optional<StarReadSearchResponse> search(String keyword, int page, int size) {
        if (!isEnabled() || !hasText(keyword)) {
            return Optional.empty();
        }

        try {
            HttpRequest request = buildRequest(buildSearchUri(), buildSearchBody(keyword, page, size));
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return Optional.empty();
            }
            return Optional.of(parseSearchResponse(keyword, page, size, response.body()));
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return Optional.empty();
        } catch (Exception ex) {
            return Optional.empty();
        }
    }

    /**
     * 使用 Elasticsearch 生成搜索框下拉建议词列表。
     * 复用标题和作者字段做前缀检索，再去重后返回有限条建议结果。
     */
    public List<StarReadSearchSuggestionResponse> suggest(String keyword, int size) {
        if (!isEnabled() || !hasText(keyword)) {
            return List.of();
        }

        try {
            HttpRequest request = buildRequest(buildSearchUri(), buildSuggestBody(keyword, size));
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return List.of();
            }
            return parseSuggestResponse(response.body(), size);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return List.of();
        } catch (Exception ex) {
            return List.of();
        }
    }

    /**
     * 构造 Elasticsearch 搜索接口地址。
     * 统一裁掉尾部斜杠后拼接索引路径，避免不同配置格式导致请求失败。
     */
    private URI buildSearchUri() {
        String endpoint = properties.getEndpoint().trim();
        if (endpoint.endsWith("/")) {
            endpoint = endpoint.substring(0, endpoint.length() - 1);
        }
        return URI.create(endpoint + "/" + properties.getIndex().trim() + "/_search");
    }

    /**
     * 创建带鉴权头的 HTTP 请求对象。
     * 按顺序注入 API Key 或 Basic Auth，保证不同部署方式都能复用同一客户端。
     */
    private HttpRequest buildRequest(URI uri, String body) {
        HttpRequest.Builder builder = HttpRequest.newBuilder(uri)
            .timeout(Duration.ofSeconds(Math.max(properties.getTimeoutSeconds(), 1)))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8));

        if (hasText(properties.getApiKey())) {
            builder.header("Authorization", "ApiKey " + properties.getApiKey().trim());
        } else if (hasText(properties.getUsername()) && hasText(properties.getPassword())) {
            String token = properties.getUsername().trim() + ":" + properties.getPassword().trim();
            String encoded = Base64.getEncoder().encodeToString(token.getBytes(StandardCharsets.UTF_8));
            builder.header("Authorization", "Basic " + encoded);
        }

        return builder.build();
    }

    /**
     * 生成书籍搜索的 Elasticsearch 请求体。
     * 通过 multi_match 同时覆盖标题、摘要、作者和正文等字段，提高兼容性。
     */
    private String buildSearchBody(String keyword, int page, int size) throws IOException {
        int safePage = Math.max(page, 0);
        int safeSize = Math.max(size, 1);
        int from = safePage * safeSize;

        JsonNode root = objectMapper.createObjectNode()
            .deepCopy();
        ((com.fasterxml.jackson.databind.node.ObjectNode) root).put("from", from);
        ((com.fasterxml.jackson.databind.node.ObjectNode) root).put("size", safeSize);
        ((com.fasterxml.jackson.databind.node.ObjectNode) root).set("_source", objectMapper.valueToTree(List.of(
            "article_key", "articleKey", "title", "summary", "essence", "author_name", "authorName",
            "cover_url", "coverUrl", "category", "published_at", "publishedAt", "read_count", "readCount",
            "like_count", "likeCount", "highlight_list", "highlightList"
        )));

        com.fasterxml.jackson.databind.node.ObjectNode multiMatch = objectMapper.createObjectNode();
        multiMatch.put("query", keyword.trim());
        multiMatch.put("type", "best_fields");
        multiMatch.set("fields", objectMapper.valueToTree(List.of(
            "title^5", "summary^3", "essence^3", "author_name^2", "authorName^2",
            "highlight_list^2", "highlightList^2", "category^2", "content_html", "contentHtml"
        )));

        com.fasterxml.jackson.databind.node.ObjectNode bool = objectMapper.createObjectNode();
        bool.set("must", objectMapper.valueToTree(List.of(objectMapper.createObjectNode().set("multi_match", multiMatch))));
        ((com.fasterxml.jackson.databind.node.ObjectNode) root).set("query", objectMapper.createObjectNode().set("bool", bool));
        ((com.fasterxml.jackson.databind.node.ObjectNode) root).set("sort", objectMapper.valueToTree(List.of(
            objectMapper.createObjectNode().put("_score", "desc"),
            objectMapper.createObjectNode().put("readCount", "desc"),
            objectMapper.createObjectNode().put("publishedAt", "desc")
        )));

        return objectMapper.writeValueAsString(root);
    }

    /**
     * 生成搜索建议的 Elasticsearch 请求体。
     * 使用短列表前缀匹配标题和作者字段，减少下拉建议的噪声内容。
     */
    private String buildSuggestBody(String keyword, int size) throws IOException {
        int safeSize = Math.max(size, 1);

        com.fasterxml.jackson.databind.node.ObjectNode root = objectMapper.createObjectNode();
        root.put("from", 0);
        root.put("size", safeSize);
        root.set("_source", objectMapper.valueToTree(List.of("title", "author_name", "authorName")));

        com.fasterxml.jackson.databind.node.ObjectNode titleMatch = objectMapper.createObjectNode();
        titleMatch.put("title", keyword.trim());
        com.fasterxml.jackson.databind.node.ObjectNode authorMatch = objectMapper.createObjectNode();
        authorMatch.put("author_name", keyword.trim());
        com.fasterxml.jackson.databind.node.ObjectNode authorCamelMatch = objectMapper.createObjectNode();
        authorCamelMatch.put("authorName", keyword.trim());

        com.fasterxml.jackson.databind.node.ObjectNode bool = objectMapper.createObjectNode();
        bool.set("should", objectMapper.valueToTree(List.of(
            objectMapper.createObjectNode().set("match_phrase_prefix", titleMatch),
            objectMapper.createObjectNode().set("match_phrase_prefix", authorMatch),
            objectMapper.createObjectNode().set("match_phrase_prefix", authorCamelMatch)
        )));
        bool.put("minimum_should_match", 1);
        root.set("query", objectMapper.createObjectNode().set("bool", bool));
        return objectMapper.writeValueAsString(root);
    }

    /**
     * 解析 Elasticsearch 返回的搜索结果。
     * 把命中文档映射成页面统一书籍结构，并回填总数和引擎标识。
     */
    private StarReadSearchResponse parseSearchResponse(String keyword, int page, int size, String body) throws IOException {
        JsonNode root = objectMapper.readTree(body);
        JsonNode hitsNode = root.path("hits");
        JsonNode hitList = hitsNode.path("hits");

        List<StarReadBookResponse> list = new ArrayList<>();
        if (hitList.isArray()) {
            for (JsonNode hitNode : hitList) {
                StarReadBookResponse response = toBookResponse(hitNode);
                if (response != null) {
                    list.add(response);
                }
            }
        }

        StarReadSearchResponse response = new StarReadSearchResponse();
        response.setQuery(keyword.trim());
        response.setEngine("elasticsearch");
        response.setList(list);
        response.setTotal(resolveTotal(hitsNode.path("total")));
        response.setPage(Math.max(page, 0));
        response.setPageSize(Math.max(size, 1));
        return response;
    }

    /**
     * 解析 Elasticsearch 返回的建议结果。
     * 依次提取标题和作者字段并去重，保持搜索框建议词简短可点击。
     */
    private List<StarReadSearchSuggestionResponse> parseSuggestResponse(String body, int size) throws IOException {
        JsonNode root = objectMapper.readTree(body);
        JsonNode hitList = root.path("hits").path("hits");
        Set<String> texts = new LinkedHashSet<>();

        if (hitList.isArray()) {
            for (JsonNode hitNode : hitList) {
                JsonNode sourceNode = hitNode.path("_source");
                collectSuggestion(texts, readText(sourceNode, "title"));
                collectSuggestion(texts, readText(sourceNode, "author_name"));
                collectSuggestion(texts, readText(sourceNode, "authorName"));
            }
        }

        List<StarReadSearchSuggestionResponse> suggestions = new ArrayList<>();
        for (String text : texts) {
            if (suggestions.size() >= Math.max(size, 1)) {
                break;
            }
            StarReadSearchSuggestionResponse suggestion = new StarReadSearchSuggestionResponse();
            suggestion.setText(text);
            suggestion.setSource("elasticsearch");
            suggestions.add(suggestion);
        }
        return suggestions;
    }

    /**
     * 把单条 Elasticsearch 命中文档转换为页面书籍结构。
     * 优先读取索引中的原始字段，缺失字段则使用默认值保持前端渲染稳定。
     */
    private StarReadBookResponse toBookResponse(JsonNode hitNode) {
        JsonNode sourceNode = hitNode.path("_source");
        if (sourceNode.isMissingNode() || sourceNode.isNull()) {
            return null;
        }

        StarReadBookResponse response = new StarReadBookResponse();
        String category = readText(sourceNode, "category");
        response.setId(readText(sourceNode, "article_key", "articleKey", "id", "_id"));
        response.setTitle(readText(sourceNode, "title"));
        response.setAuthorName(defaultText(readText(sourceNode, "author_name", "authorName"), "star_read"));
        response.setCoverUrl(defaultText(readText(sourceNode, "cover_url", "coverUrl"), "/peakstars-blog-icon.jpg"));
        response.setCategory(category);
        response.setCategoryLabel(resolveCategoryLabel(category));
        response.setSummary(defaultText(readText(sourceNode, "summary", "essence"), "适合放进书架反复翻看的内容。"));
        response.setBadgeText(resolveBadgeText(category));
        response.setReadCount(readInteger(sourceNode, "read_count", "readCount"));
        response.setLikeCount(readInteger(sourceNode, "like_count", "likeCount"));
        response.setRecommendationScore(buildRecommendationScore(response.getReadCount(), response.getLikeCount(), true));
        response.setPublishedAt(normalizeDate(readText(sourceNode, "published_at", "publishedAt")));
        return response;
    }

    /**
     * 提取 Elasticsearch 的总命中数值。
     * 同时兼容数字与对象结构，避免不同版本返回格式差异影响分页。
     */
    private long resolveTotal(JsonNode totalNode) {
        if (totalNode == null || totalNode.isMissingNode() || totalNode.isNull()) {
            return 0L;
        }
        if (totalNode.isNumber()) {
            return totalNode.asLong();
        }
        return totalNode.path("value").asLong(0L);
    }

    /**
     * 从多个候选字段中读取首个非空文本值。
     * 用于兼容下划线和驼峰两套索引字段命名方式。
     */
    private String readText(JsonNode node, String... fieldNames) {
        for (String fieldName : fieldNames) {
            if ("_id".equals(fieldName)) {
                continue;
            }
            JsonNode valueNode = node.path(fieldName);
            if (!valueNode.isMissingNode() && !valueNode.isNull()) {
                String value = valueNode.asText("");
                if (hasText(value)) {
                    return value.trim();
                }
            }
        }
        return "";
    }

    /**
     * 从多个候选字段中读取整数值。
     * 缺失数字时返回零值，保证推荐分和列表渲染可以继续执行。
     */
    private Integer readInteger(JsonNode node, String... fieldNames) {
        for (String fieldName : fieldNames) {
            JsonNode valueNode = node.path(fieldName);
            if (!valueNode.isMissingNode() && !valueNode.isNull()) {
                if (valueNode.isInt() || valueNode.isLong() || valueNode.isNumber()) {
                    return valueNode.asInt();
                }
                String text = valueNode.asText("");
                if (hasText(text)) {
                    try {
                        return Integer.parseInt(text.trim());
                    } catch (NumberFormatException ignored) {
                        return 0;
                    }
                }
            }
        }
        return 0;
    }

    /**
     * 统一归一化发布时间文本。
     * 带时分秒的时间仅保留日期部分，便于首页榜单展示一致。
     */
    private String normalizeDate(String rawText) {
        if (!hasText(rawText)) {
            return "";
        }
        String text = rawText.trim();
        return text.length() >= 10 ? text.substring(0, 10) : text;
    }

    /**
     * 计算搜索结果卡片展示的推荐值。
     * 结合阅读量与点赞量生成稳定区间分数，避免远端索引缺少该字段时出现空白。
     */
    private Double buildRecommendationScore(Integer readCount, Integer likeCount, boolean boost) {
        int read = readCount == null ? 0 : readCount;
        int like = likeCount == null ? 0 : likeCount;
        double score = 78.0 + Math.min(18.0, read / 220.0) + Math.min(4.0, like / 80.0);
        if (boost) {
            score += 1.2;
        }
        return Math.min(99.0, Math.round(score * 10.0) / 10.0);
    }

    /**
     * 根据分类编码返回书架模块的中文标签。
     * 前端统一读取分类文案，避免页面内重复维护映射表。
     */
    private String resolveCategoryLabel(String category) {
        return switch (defaultText(category, "").trim().toLowerCase()) {
            case "frontend" -> "前端书架";
            case "backend" -> "后端书架";
            case "project" -> "项目业务解析";
            case "vip" -> "专题长读";
            default -> "精选阅读";
        };
    }

    /**
     * 根据分类编码返回榜单条目右侧的轻量徽标文案。
     * 统一把不同内容归入少量标签，保持榜单列表干净克制。
     */
    private String resolveBadgeText(String category) {
        return switch (defaultText(category, "").trim().toLowerCase()) {
            case "frontend" -> "值得一读";
            case "backend" -> "深读推荐";
            case "project" -> "项目拆解";
            case "vip" -> "好评加藏";
            default -> "star_read";
        };
    }

    /**
     * 收集并去重一条建议文本。
     * 过滤空值和重复项，保证搜索框下拉内容可控。
     */
    private void collectSuggestion(Set<String> texts, String text) {
        if (hasText(text)) {
            texts.add(text.trim());
        }
    }

    /**
     * 判断文本是否包含有效内容。
     * 统一收敛空串和空白字符串判断，减少远程解析分支重复代码。
     */
    private boolean hasText(String text) {
        return text != null && !text.trim().isEmpty();
    }

    /**
     * 在远程字段缺失时回填兜底文本。
     * 让前端拿到的字段始终完整，避免首页卡片出现空白区域。
     */
    private String defaultText(String text, String defaultValue) {
        return hasText(text) ? text.trim() : defaultValue;
    }
}
