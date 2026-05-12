package com.interview.auth.service.impl;

import com.interview.auth.domain.dto.response.StarReadBookResponse;
import com.interview.auth.domain.dto.response.StarReadCategoryResponse;
import com.interview.auth.domain.dto.response.StarReadHomeResponse;
import com.interview.auth.domain.dto.response.StarReadRankingResponse;
import com.interview.auth.domain.dto.response.StarReadSearchResponse;
import com.interview.auth.domain.dto.response.StarReadSearchSuggestionResponse;
import com.interview.auth.domain.entity.TechArticle;
import com.interview.auth.infrastructure.mapper.ContentMapper;
import com.interview.auth.infrastructure.search.StarReadElasticsearchClient;
import com.interview.auth.service.StarReadService;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 组装 star_read 首页与搜索页所需的极简阅读数据结构。
 * 复用现有技术文章数据生成榜单和分类，并在搜索阶段接入 Elasticsearch 兜底策略。
 */
@Service
@RequiredArgsConstructor
public class StarReadServiceImpl implements StarReadService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final ContentMapper contentMapper;
    private final StarReadElasticsearchClient elasticsearchClient;

    /**
     * 组装 star_read 首页所需的阅读卡片、榜单和分类数据。
     * 从现有技术文章中计算热门内容、榜单排序和标签聚合，保持页面结构简洁统一。
     */
    @Override
    public StarReadHomeResponse getHomeData() {
        List<TechArticle> articles = loadPublishedArticles();
        List<StarReadBookResponse> books = articles.stream()
            .map(article -> toBookResponse(article, null, false))
            .toList();

        StarReadHomeResponse response = new StarReadHomeResponse();
        response.setBrandName("star_read");
        response.setSearchPlaceholder("搜索书名、作者、专题");
        response.setHotKeywords(buildHotKeywords(articles));
        response.setEveryoneReads(buildEveryoneReads(articles));
        response.setRankings(buildRankings(articles));
        response.setCategories(buildCategories(articles, books));
        return response;
    }

    /**
     * 按关键词查询 star_read 阅读内容列表。
     * 优先尝试 Elasticsearch 检索，未启用或失败时降级为本地评分搜索结果。
     */
    @Override
    public StarReadSearchResponse searchBooks(String keyword, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.max(size, 1);
        String normalizedKeyword = normalizeKeyword(keyword);

        if (!normalizedKeyword.isEmpty()) {
            var elasticsearchResult = elasticsearchClient.search(normalizedKeyword, safePage, safeSize);
            if (elasticsearchResult.isPresent()) {
                return elasticsearchResult.get();
            }
        }

        List<TechArticle> articles = loadPublishedArticles();
        List<TechArticle> matchedArticles = articles.stream()
            .filter(article -> matchesKeyword(article, normalizedKeyword))
            .sorted(buildLocalSearchComparator(normalizedKeyword))
            .toList();

        int fromIndex = Math.min(safePage * safeSize, matchedArticles.size());
        int toIndex = Math.min(fromIndex + safeSize, matchedArticles.size());
        List<StarReadBookResponse> list = matchedArticles.subList(fromIndex, toIndex)
            .stream()
            .map(article -> toBookResponse(article, normalizedKeyword, true))
            .toList();

        StarReadSearchResponse response = new StarReadSearchResponse();
        response.setQuery(normalizedKeyword);
        response.setEngine("mysql-fallback");
        response.setList(list);
        response.setTotal(matchedArticles.size());
        response.setPage(safePage);
        response.setPageSize(safeSize);
        return response;
    }

    /**
     * 获取 star_read 搜索框的即时建议词。
     * 先读取搜索服务建议，失败后再从标题、作者和亮点字段本地筛选候选文本。
     */
    @Override
    public List<StarReadSearchSuggestionResponse> suggestBooks(String keyword, int size) {
        int safeSize = Math.max(size, 1);
        String normalizedKeyword = normalizeKeyword(keyword);

        if (normalizedKeyword.isEmpty()) {
            return buildFallbackSuggestionsFromKeywords(buildHotKeywords(loadPublishedArticles()), safeSize, "hot");
        }

        List<StarReadSearchSuggestionResponse> elasticsearchSuggestions = elasticsearchClient.suggest(normalizedKeyword, safeSize);
        if (!elasticsearchSuggestions.isEmpty()) {
            return elasticsearchSuggestions;
        }

        Set<String> texts = new LinkedHashSet<>();
        for (TechArticle article : loadPublishedArticles()) {
            collectMatchedText(texts, article.getTitle(), normalizedKeyword);
            collectMatchedText(texts, article.getAuthorName(), normalizedKeyword);
            collectMatchedText(texts, resolveCategoryLabel(article.getCategory()), normalizedKeyword);
            splitPipeValues(article.getHighlightList()).forEach(value -> collectMatchedText(texts, value, normalizedKeyword));
            if (texts.size() >= safeSize) {
                break;
            }
        }

        return buildFallbackSuggestionsFromKeywords(texts, safeSize, "mysql");
    }

    /**
     * 读取当前已发布的技术文章列表。
     * 复用现有内容查询口径，避免 star_read 与文章模块出现数据源分叉。
     */
    private List<TechArticle> loadPublishedArticles() {
        return contentMapper.findPublishedTechArticles();
    }

    /**
     * 生成首页顶部的热搜关键词列表。
     * 先抽取高热文章标题，再补充亮点标签，确保搜索快捷词数量稳定。
     */
    private List<String> buildHotKeywords(List<TechArticle> articles) {
        Set<String> keywords = new LinkedHashSet<>();
        for (TechArticle article : sortArticles(articles, Comparator
            .comparing(this::featuredWeight, Comparator.reverseOrder())
            .thenComparing(this::safeReadCount, Comparator.reverseOrder())
            .thenComparing(this::safePublishedAt, Comparator.reverseOrder()))) {
            collectKeyword(keywords, article.getTitle());
            splitPipeValues(article.getHighlightList()).forEach(value -> collectKeyword(keywords, value));
            if (keywords.size() >= 5) {
                break;
            }
        }
        return new ArrayList<>(keywords);
    }

    /**
     * 生成首页“大家都在看”的展示卡片。
     * 以精选优先并结合阅读量排序，保证首页首屏呈现最稳定的内容入口。
     */
    private List<StarReadBookResponse> buildEveryoneReads(List<TechArticle> articles) {
        return sortArticles(articles, Comparator
            .comparing(this::featuredWeight, Comparator.reverseOrder())
            .thenComparing(this::safeReadCount, Comparator.reverseOrder())
            .thenComparing(this::safeLikeCount, Comparator.reverseOrder())
            .thenComparing(this::safePublishedAt, Comparator.reverseOrder()))
            .stream()
            .limit(4)
            .map(article -> toBookResponse(article, null, true))
            .toList();
    }

    /**
     * 生成首页四块榜单卡片。
     * 对同一批文章应用不同排序规则，再映射为统一榜单结构供前端复用。
     */
    private List<StarReadRankingResponse> buildRankings(List<TechArticle> articles) {
        return List.of(
            buildRanking(
                "rising",
                "TOP 50 / 飙升榜",
                "star_read 近期热度攀升最快的内容",
                "#ef5b78",
                sortArticles(articles, Comparator
                    .comparing(this::featuredWeight, Comparator.reverseOrder())
                    .thenComparing(this::safeReadCount, Comparator.reverseOrder())
                    .thenComparing(this::safePublishedAt, Comparator.reverseOrder()))
            ),
            buildRanking(
                "newbook",
                "TOP 50 / 新书榜",
                "最近更新后最值得点开的内容",
                "#f29d38",
                sortArticles(articles, Comparator
                    .comparing(this::safePublishedAt, Comparator.reverseOrder())
                    .thenComparing(this::featuredWeight, Comparator.reverseOrder())
                    .thenComparing(this::safeReadCount, Comparator.reverseOrder()))
            ),
            buildRanking(
                "overall",
                "TOP 200 / 总榜",
                "star_read 用户最常加入书架的精选内容",
                "#39a0ff",
                sortArticles(articles, Comparator
                    .comparingInt(this::overallWeight).reversed()
                    .thenComparing(this::safePublishedAt, Comparator.reverseOrder()))
            ),
            buildRanking(
                "masterpiece",
                "神作榜",
                "高分读者更愿意反复翻看的内容",
                "#d7a031",
                sortArticles(articles, Comparator
                    .comparingInt(this::masterpieceWeight).reversed()
                    .thenComparing(this::safePublishedAt, Comparator.reverseOrder()))
            )
        );
    }

    /**
     * 生成首页分类区卡片列表。
     * 结合分类与亮点标签聚合出更多入口，尽量还原阅读站点常见的分类网格结构。
     */
    private List<StarReadCategoryResponse> buildCategories(List<TechArticle> articles, List<StarReadBookResponse> books) {
        Map<String, StarReadCategoryResponse> categoryMap = new LinkedHashMap<>();

        for (TechArticle article : sortArticles(articles, Comparator
            .comparing(this::safeReadCount, Comparator.reverseOrder())
            .thenComparing(this::safePublishedAt, Comparator.reverseOrder()))) {
            List<String> keys = new ArrayList<>();
            keys.add(resolveCategoryLabel(article.getCategory()));
            keys.addAll(splitPipeValues(article.getHighlightList()));

            for (String key : keys) {
                if (!hasText(key)) {
                    continue;
                }
                String normalizedKey = key.trim();
                StarReadCategoryResponse category = categoryMap.computeIfAbsent(normalizedKey, ignored -> {
                    StarReadCategoryResponse response = new StarReadCategoryResponse();
                    response.setKey(slugify(normalizedKey));
                    response.setName(normalizedKey);
                    response.setBookCount(0);
                    response.setCoverUrl(resolveCoverUrl(article.getCoverUrl()));
                    return response;
                });
                category.setBookCount(category.getBookCount() + 1);
                if (!hasText(category.getCoverUrl())) {
                    category.setCoverUrl(resolveCoverUrl(article.getCoverUrl()));
                }
            }
        }

        if (categoryMap.size() < 8) {
            for (StarReadBookResponse book : books) {
                if (categoryMap.size() >= 8) {
                    break;
                }
                String fallbackKey = defaultText(book.getTitle(), "精选内容");
                if (!categoryMap.containsKey(fallbackKey)) {
                    StarReadCategoryResponse category = new StarReadCategoryResponse();
                    category.setKey(slugify(fallbackKey));
                    category.setName(fallbackKey);
                    category.setBookCount(1);
                    category.setCoverUrl(resolveCoverUrl(book.getCoverUrl()));
                    categoryMap.put(fallbackKey, category);
                }
            }
        }

        return categoryMap.values().stream().limit(12).toList();
    }

    /**
     * 组装单个榜单卡片的结构化结果。
     * 截取前六条内容并写入榜单元信息，便于前端直接双列渲染。
     */
    private StarReadRankingResponse buildRanking(String key, String title, String subtitle, String accentColor, List<TechArticle> articles) {
        StarReadRankingResponse response = new StarReadRankingResponse();
        response.setKey(key);
        response.setTitle(title);
        response.setSubtitle(subtitle);
        response.setAccentColor(accentColor);
        response.setBooks(articles.stream().limit(6).map(article -> toBookResponse(article, null, true)).toList());
        return response;
    }

    /**
     * 把文章实体转换为 star_read 页面可直接消费的书籍结构。
     * 同时补齐封面、徽标、推荐值和分类文案，减少前端加工逻辑。
     */
    private StarReadBookResponse toBookResponse(TechArticle article, String keyword, boolean boostScore) {
        StarReadBookResponse response = new StarReadBookResponse();
        response.setId(article.getArticleKey());
        response.setTitle(defaultText(article.getTitle(), "未命名内容"));
        response.setAuthorName(defaultText(article.getAuthorName(), "star_read"));
        response.setCoverUrl(resolveCoverUrl(article.getCoverUrl()));
        response.setCategory(article.getCategory());
        response.setCategoryLabel(resolveCategoryLabel(article.getCategory()));
        response.setSummary(resolveSummary(article));
        response.setBadgeText(resolveBadgeText(article.getCategory()));
        response.setReadCount(safeReadCount(article));
        response.setLikeCount(safeLikeCount(article));
        response.setRecommendationScore(buildRecommendationScore(article, keyword, boostScore));
        response.setPublishedAt(formatDate(article.getPublishedAt()));
        return response;
    }

    /**
     * 生成本地搜索排序器。
     * 先按关键词命中权重排序，再按精选状态与热度做二次稳定排序。
     */
    private Comparator<TechArticle> buildLocalSearchComparator(String keyword) {
        return Comparator
            .comparingInt((TechArticle article) -> searchWeight(article, keyword))
            .reversed()
            .thenComparing(this::featuredWeight, Comparator.reverseOrder())
            .thenComparing(this::safeReadCount, Comparator.reverseOrder())
            .thenComparing(this::safePublishedAt, Comparator.reverseOrder());
    }

    /**
     * 判断文章是否命中当前搜索词。
     * 同时匹配标题、摘要、作者和亮点字段，提高本地兜底搜索的可用性。
     */
    private boolean matchesKeyword(TechArticle article, String keyword) {
        if (keyword.isEmpty()) {
            return true;
        }

        String normalizedKeyword = keyword.toLowerCase(Locale.ROOT);
        return containsText(article.getTitle(), normalizedKeyword)
            || containsText(article.getSummary(), normalizedKeyword)
            || containsText(article.getEssence(), normalizedKeyword)
            || containsText(article.getAuthorName(), normalizedKeyword)
            || containsText(resolveCategoryLabel(article.getCategory()), normalizedKeyword)
            || splitPipeValues(article.getHighlightList()).stream().anyMatch(value -> containsText(value, normalizedKeyword));
    }

    /**
     * 计算文章在本地搜索中的相关度权重。
     * 标题命中权重最高，其余字段按阅读站点常见优先级逐级递减。
     */
    private int searchWeight(TechArticle article, String keyword) {
        if (keyword.isEmpty()) {
            return 0;
        }

        String normalizedKeyword = keyword.toLowerCase(Locale.ROOT);
        int weight = 0;
        if (containsText(article.getTitle(), normalizedKeyword)) {
            weight += 12;
        }
        if (containsText(article.getAuthorName(), normalizedKeyword)) {
            weight += 8;
        }
        if (containsText(article.getSummary(), normalizedKeyword)) {
            weight += 6;
        }
        if (containsText(article.getEssence(), normalizedKeyword)) {
            weight += 5;
        }
        if (containsText(resolveCategoryLabel(article.getCategory()), normalizedKeyword)) {
            weight += 4;
        }
        for (String highlight : splitPipeValues(article.getHighlightList())) {
            if (containsText(highlight, normalizedKeyword)) {
                weight += 3;
            }
        }
        return weight;
    }

    /**
     * 计算首页总榜的综合权重值。
     * 结合阅读、点赞、收藏和评论热度，模拟阅读站点的总榜排序口径。
     */
    private int overallWeight(TechArticle article) {
        return safeReadCount(article)
            + safeLikeCount(article) * 6
            + safeCount(article.getCollectCount()) * 8
            + safeCount(article.getCommentCount()) * 5
            + featuredWeight(article) * 180;
    }

    /**
     * 计算首页神作榜的综合权重值。
     * 更强调精选状态、收藏和评论反馈，让高质量内容稳定进入高位。
     */
    private int masterpieceWeight(TechArticle article) {
        return featuredWeight(article) * 300
            + safeCount(article.getCollectCount()) * 10
            + safeCount(article.getCommentCount()) * 8
            + safeLikeCount(article) * 4
            + safeReadCount(article) / 2;
    }

    /**
     * 计算卡片显示的推荐值。
     * 在基础热度分上叠加搜索命中和精选加成，保证同一页面的分值有层次。
     */
    private double buildRecommendationScore(TechArticle article, String keyword, boolean boostScore) {
        double score = 77.0
            + Math.min(16.0, safeReadCount(article) / 220.0)
            + Math.min(4.0, safeLikeCount(article) / 70.0)
            + Math.min(3.0, safeCount(article.getCollectCount()) / 45.0);

        if (featuredWeight(article) == 1) {
            score += 1.8;
        }
        if (boostScore) {
            score += 0.8;
        }
        if (hasText(keyword) && matchesKeyword(article, keyword)) {
            score += Math.min(2.4, searchWeight(article, keyword) / 6.0);
        }

        return Math.min(99.0, Math.round(score * 10.0) / 10.0);
    }

    /**
     * 统一收敛文章摘要文案。
     * 优先使用精华摘要，缺失时回退到普通摘要，保证首页卡片总有简短说明。
     */
    private String resolveSummary(TechArticle article) {
        if (hasText(article.getEssence())) {
            return article.getEssence().trim();
        }
        if (hasText(article.getSummary())) {
            return article.getSummary().trim();
        }
        return "适合放进书架慢慢翻看的精选内容。";
    }

    /**
     * 根据内容分类返回首页卡片的中文文案。
     * 统一把现有技术内容转换成阅读站风格的书架标签。
     */
    private String resolveCategoryLabel(String category) {
        return switch (defaultText(category, "").trim().toLowerCase(Locale.ROOT)) {
            case "frontend" -> "前端书架";
            case "backend" -> "后端书架";
            case "vip" -> "专题长读";
            default -> "精选阅读";
        };
    }

    /**
     * 根据内容分类返回榜单条目的轻量徽标文案。
     * 前端只展示少量辅助词，保持榜单信息密度干净克制。
     */
    private String resolveBadgeText(String category) {
        return switch (defaultText(category, "").trim().toLowerCase(Locale.ROOT)) {
            case "frontend" -> "值得一读";
            case "backend" -> "深读推荐";
            case "vip" -> "高分加藏";
            default -> "star_read";
        };
    }

    /**
     * 把技术文章列表按给定比较器重新排序。
     * 复制后再排序，避免对原始查询结果产生副作用。
     */
    private List<TechArticle> sortArticles(List<TechArticle> articles, Comparator<TechArticle> comparator) {
        List<TechArticle> copiedArticles = new ArrayList<>(articles);
        copiedArticles.sort(comparator);
        return copiedArticles;
    }

    /**
     * 构造建议词响应列表。
     * 把去重后的纯文本候选映射为统一的下拉建议结构。
     */
    private List<StarReadSearchSuggestionResponse> buildFallbackSuggestionsFromKeywords(Collection<String> keywords, int size, String source) {
        List<StarReadSearchSuggestionResponse> responses = new ArrayList<>();
        for (String keyword : keywords) {
            if (!hasText(keyword) || responses.size() >= size) {
                continue;
            }
            StarReadSearchSuggestionResponse response = new StarReadSearchSuggestionResponse();
            response.setText(keyword.trim());
            response.setSource(source);
            responses.add(response);
        }
        return responses;
    }

    /**
     * 把匹配到的文本收集到建议集合中。
     * 仅在包含当前关键词时写入结果，避免建议列表出现无关内容。
     */
    private void collectMatchedText(Set<String> texts, String value, String keyword) {
        if (hasText(value) && containsText(value, keyword)) {
            texts.add(value.trim());
        }
    }

    /**
     * 提取可用于顶部热搜词的短文本。
     * 过长标题会被裁剪成轻量关键词，保证顶部快捷词更接近截图的简洁效果。
     */
    private void collectKeyword(Set<String> keywords, String rawText) {
        if (!hasText(rawText) || keywords.size() >= 5) {
            return;
        }
        String normalizedText = rawText.trim();
        if (normalizedText.length() > 12) {
            normalizedText = normalizedText.substring(0, 12);
        }
        keywords.add(normalizedText);
    }

    /**
     * 将竖线分隔的亮点字段拆分为数组。
     * 统一在服务层处理持久化格式，避免不同首页模块各自解析同一字段。
     */
    private List<String> splitPipeValues(String rawValue) {
        if (!hasText(rawValue)) {
            return List.of();
        }
        return List.of(rawValue.split("\\|")).stream()
            .map(String::trim)
            .filter(this::hasText)
            .toList();
    }

    /**
     * 把字符串整理成 URL 友好的 key。
     * 首页分类卡片只需要稳定标识，不要求与数据库真实字段完全一致。
     */
    private String slugify(String rawText) {
        String text = defaultText(rawText, "star-read").trim().toLowerCase(Locale.ROOT);
        StringBuilder builder = new StringBuilder();
        for (char currentChar : text.toCharArray()) {
            if ((currentChar >= 'a' && currentChar <= 'z') || (currentChar >= '0' && currentChar <= '9')) {
                builder.append(currentChar);
            } else if (currentChar == ' ' || currentChar == '-' || currentChar == '_') {
                builder.append('-');
            } else {
                builder.append(Integer.toHexString(currentChar));
            }
        }
        return builder.toString().replaceAll("-{2,}", "-");
    }

    /**
     * 统一归一化搜索词。
     * 去除前后空格后继续搜索，避免输入框携带空白字符影响结果判断。
     */
    private String normalizeKeyword(String keyword) {
        return keyword == null ? "" : keyword.trim();
    }

    /**
     * 统一判断文本是否包含有效内容。
     * 把空串和空白串识别逻辑集中起来，减少首页组装过程的分支重复。
     */
    private boolean hasText(String text) {
        return text != null && !text.trim().isEmpty();
    }

    /**
     * 判断文本中是否包含目标关键词。
     * 全部统一转为小写比较，兼容搜索兜底里的大小写输入差异。
     */
    private boolean containsText(String value, String keyword) {
        return hasText(value) && hasText(keyword) && value.toLowerCase(Locale.ROOT).contains(keyword.toLowerCase(Locale.ROOT));
    }

    /**
     * 在文本缺失时回填默认值。
     * 让前端拿到的品牌、标题和分类字段保持可渲染状态。
     */
    private String defaultText(String text, String defaultValue) {
        return hasText(text) ? text.trim() : defaultValue;
    }

    /**
     * 统一处理封面地址兜底逻辑。
     * 内容缺少封面时返回站点默认图，避免首页网格出现空白占位。
     */
    private String resolveCoverUrl(String coverUrl) {
        return hasText(coverUrl) ? coverUrl.trim() : "/peakstars-blog-icon.jpg";
    }

    /**
     * 格式化发布日期文本。
     * 首页仅展示日期维度，带时分秒的字段会统一裁剪为日粒度文案。
     */
    private String formatDate(LocalDateTime publishedAt) {
        return publishedAt == null ? "" : publishedAt.format(DATE_FORMATTER);
    }

    /**
     * 读取文章精选状态对应的权重值。
     * 统一把布尔状态转成数字，便于排序器重复复用。
     */
    private Integer featuredWeight(TechArticle article) {
        return article != null && article.getFeatured() != null && article.getFeatured() == 1 ? 1 : 0;
    }

    /**
     * 读取文章发布时间兜底值。
     * 空时间会替换为最早时间，确保各种榜单排序比较稳定。
     */
    private LocalDateTime safePublishedAt(TechArticle article) {
        return article != null && article.getPublishedAt() != null ? article.getPublishedAt() : LocalDateTime.MIN;
    }

    /**
     * 读取文章阅读量兜底值。
     * 统一把空数字视为零，避免排序和推荐值计算出现空指针。
     */
    private Integer safeReadCount(TechArticle article) {
        return article == null ? 0 : safeCount(article.getReadCount());
    }

    /**
     * 读取文章点赞量兜底值。
     * 统一把空数字视为零，供搜索和榜单权重计算复用。
     */
    private Integer safeLikeCount(TechArticle article) {
        return article == null ? 0 : safeCount(article.getLikeCount());
    }

    /**
     * 把可空整数转成稳定的零值。
     * 所有涉及热度分计算的数字字段都通过这里收敛空值。
     */
    private Integer safeCount(Integer value) {
        return value == null ? 0 : value;
    }
}
