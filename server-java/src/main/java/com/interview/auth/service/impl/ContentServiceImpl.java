package com.interview.auth.service.impl;

import com.interview.auth.common.BusinessException;
import com.interview.auth.common.TechArticleReadTimeCalculator;
import com.interview.auth.domain.dto.response.PageResult;
import com.interview.auth.domain.dto.response.TechArticleAuthorResponse;
import com.interview.auth.domain.dto.response.TechArticleResponse;
import com.interview.auth.domain.dto.response.WorldNewsIssueResponse;
import com.interview.auth.domain.dto.response.WorldNewsSuggestionResponse;
import com.interview.auth.domain.entity.TechArticle;
import com.interview.auth.domain.entity.WorldNewsIssue;
import com.interview.auth.infrastructure.mapper.ContentMapper;
import com.interview.auth.service.ContentService;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 把内容实体转换成前端可直接消费的频道数据结构。
 * 这里集中处理时间格式、分页结果和榜单排序映射，保证多个页面共用同一输出口径。
 */
@Service
@RequiredArgsConstructor
public class ContentServiceImpl implements ContentService {

    private static final DateTimeFormatter ARTICLE_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter WORLD_NEWS_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final DateTimeFormatter ARTICLE_HISTORY_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final ContentMapper contentMapper;

    /**
     * 查询技术文章列表并转换成前端文章流结构。
     * 后端补齐作者对象、精选标记和亮点数组，让文章页与导航预览复用同一数据源。
     */
    @Override
    public List<TechArticleResponse> listTechArticles(Long currentUserId) {
        List<TechArticle> articles = currentUserId == null
            ? contentMapper.findPublishedTechArticles()
            : contentMapper.findPublishedTechArticlesByUser(currentUserId);
        return articles
            .stream()
            .map(this::toTechArticleResponse)
            .toList();
    }

    /**
     * 查询看天下期刊列表并转换成频道首页结构。
     * 首页列表直接复用同一个响应对象，前端无需再做字段重命名。
     */
    @Override
    public List<WorldNewsIssueResponse> listWorldNewsIssues() {
        return contentMapper.findPublishedWorldNewsIssues()
            .stream()
            .map(this::toWorldNewsIssueResponse)
            .toList();
    }

    /**
     * 按关键词检索看天下期刊并返回分页结构。
     * 先统一整理查询关键字，再读取总数和分页列表并转换成前端对象。
     */
    @Override
    public PageResult<WorldNewsIssueResponse> searchWorldNewsIssues(String keyword, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.max(size, 1);
        String normalizedKeyword = wrapKeyword(keyword);
        long total = contentMapper.countWorldNewsIssuesByKeyword(normalizedKeyword);
        List<WorldNewsIssueResponse> list = contentMapper.searchWorldNewsIssues(normalizedKeyword, safePage * safeSize, safeSize)
            .stream()
            .map(this::toWorldNewsIssueResponse)
            .toList();
        return new PageResult<>(list, total, safePage, safeSize);
    }

    /**
     * 查询看天下搜索联想词。
     * 关键词为空时直接返回空列表，非空时把候选文案转换成统一下拉项结构。
     */
    @Override
    public List<WorldNewsSuggestionResponse> suggestWorldNewsIssues(String keyword, int size) {
        if (keyword == null || keyword.isBlank()) {
            return List.of();
        }
        int safeSize = Math.max(size, 1);
        return contentMapper.suggestWorldNewsKeywords(wrapKeyword(keyword), safeSize)
            .stream()
            .map(this::toWorldNewsSuggestionResponse)
            .toList();
    }

    /**
     * 查询看天下榜单列表。
     * 通过榜单类型映射不同排序规则，再统一转换成频道卡片所需字段。
     */
    @Override
    public List<WorldNewsIssueResponse> listWorldNewsRanking(String type, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.max(size, 1);
        return contentMapper.findWorldNewsRanking(normalizeRankingType(type), safePage * safeSize, safeSize)
            .stream()
            .map(this::toWorldNewsIssueResponse)
            .toList();
    }

    /**
     * 查询看天下热门推荐列表。
     * 热门区采用更轻量的数量限制查询，避免首页重复拉取完整频道数据。
     */
    @Override
    public List<WorldNewsIssueResponse> listPopularWorldNews(int size) {
        int safeSize = Math.max(size, 1);
        return contentMapper.findPopularWorldNewsIssues(safeSize)
            .stream()
            .map(this::toWorldNewsIssueResponse)
            .toList();
    }

    /**
     * 查询单条看天下期刊详情。
     * 详情接口返回完整正文字段，供频道页浮层按需加载当前期刊内容。
     */
    @Override
    public WorldNewsIssueResponse getWorldNewsIssueDetail(String issueKey) {
        WorldNewsIssue issue = contentMapper.findWorldNewsIssueByKey(issueKey);
        return issue == null ? null : toWorldNewsIssueResponse(issue);
    }

    /**
     * 把技术文章实体转换成前端文章对象。
     * 使用 articleKey 作为稳定 id，保证现有路由和列表 key 行为不变。
     */
    private TechArticleResponse toTechArticleResponse(TechArticle article) {
        TechArticleResponse response = new TechArticleResponse();
        response.setId(article.getArticleKey());
        response.setCategory(article.getCategory());
        response.setCategoryLabel(resolveTechCategoryLabel(article));
        response.setTitle(article.getTitle());
        response.setSummary(article.getSummary());
        response.setEssence(article.getEssence());
        response.setHighlights(splitPipeValues(article.getHighlightList()));
        response.setAuthor(toAuthorResponse(article));
        response.setCoverUrl(article.getCoverUrl());
        response.setContentHtml(article.getContentHtml());
        response.setPublishedAt(article.getPublishedAt() == null ? null : article.getPublishedAt().format(ARTICLE_DATE_FORMATTER));
        response.setReadCount(article.getReadCount());
        response.setLikeCount(article.getLikeCount());
        response.setCollectCount(article.getCollectCount());
        response.setCommentCount(article.getCommentCount());
        response.setReadTime(TechArticleReadTimeCalculator.estimateReadTime(article.getContentHtml(), article.getReadTime()));
        response.setLastReadAt(article.getLastReadAt() == null ? null : article.getLastReadAt().format(ARTICLE_HISTORY_TIME_FORMATTER));
        response.setIsVip(toBoolean(article.getIsVip()));
        response.setIsCollected(toBoolean(article.getIsCollected()));
        response.setIsLiked(toBoolean(article.getIsLiked()));
        response.setInHistory(toBoolean(article.getInHistory()));
        response.setFeatured(toBoolean(article.getFeatured()));
        return response;
    }

    /**
     * 把作者原始字段组装成前端复用的 author 结构。
     * 页面无需理解数据库拆分方式，直接读取嵌套对象即可完成渲染。
     */
    private TechArticleAuthorResponse toAuthorResponse(TechArticle article) {
        TechArticleAuthorResponse author = new TechArticleAuthorResponse();
        author.setName(article.getAuthorName());
        author.setRole(article.getAuthorRole());
        author.setInitials(article.getAuthorInitials());
        author.setAccent(article.getAuthorAccent());
        return author;
    }

    /**
     * 解析技术文章分类中文标签。
     * 优先使用数据库保存的展示字段，缺失时按历史分类编码兜底生成页面文案。
     */
    private String resolveTechCategoryLabel(TechArticle article) {
        if (article.getCategoryLabel() != null && !article.getCategoryLabel().isBlank()) {
            return article.getCategoryLabel();
        }
        return switch (String.valueOf(article.getCategory()).trim().toLowerCase()) {
            case "frontend" -> "前端工程";
            case "backend" -> "后端架构";
            case "project" -> "项目业务解析";
            default -> "技术文章";
        };
    }

    /**
     * 把看天下实体转换成频道卡片和详情共用结构。
     * 同时补齐发布时间和正文内容，保证搜索页、榜单区和详情浮层共用同一 DTO。
     */
    private WorldNewsIssueResponse toWorldNewsIssueResponse(WorldNewsIssue issue) {
        WorldNewsIssueResponse response = new WorldNewsIssueResponse();
        response.setId(issue.getIssueKey());
        response.setTitle(issue.getTitle());
        response.setIssueLabel(issue.getIssueLabel());
        response.setCategory(issue.getCategory());
        response.setTodayReads(issue.getTodayReads());
        response.setRecommendation(issue.getRecommendation());
        response.setDescription(issue.getDescription());
        response.setCoverAccent(issue.getCoverAccent());
        response.setCoverKicker(issue.getCoverKicker());
        response.setCoverHeadline(issue.getCoverHeadline());
        response.setCoverSummary(issue.getCoverSummary());
        response.setCoverFooter(issue.getCoverFooter());
        response.setPublishedAt(issue.getPublishedAt() == null ? null : issue.getPublishedAt().format(WORLD_NEWS_DATE_FORMATTER));
        response.setContentHtml(issue.getContentHtml());
        return response;
    }

    /**
     * 把联想词文案转换成搜索下拉项结构。
     * 当前统一标记为专题线索，后续扩展更多来源时前端协议也无需变化。
     */
    private WorldNewsSuggestionResponse toWorldNewsSuggestionResponse(String text) {
        WorldNewsSuggestionResponse response = new WorldNewsSuggestionResponse();
        response.setText(text);
        response.setType("专题线索");
        return response;
    }

    /**
     * 为指定技术文章增加阅读量。
     * 更新阅读数后顺带标记浏览历史，再把最新阅读数返回给前端刷新显示。
     */
    @Override
    public Map<String, Object> incrementArticleReadCount(String articleKey, Long currentUserId) {
        contentMapper.incrementReadCount(articleKey);
        contentMapper.markArticleInHistory(articleKey);
        if (currentUserId != null) {
            contentMapper.upsertArticleReadHistory(currentUserId, articleKey);
        }
        Integer readCount = contentMapper.findReadCount(articleKey);
        Map<String, Object> result = new HashMap<>();
        result.put("readCount", readCount != null ? readCount : 0);
        return result;
    }

    /**
     * 切换当前用户对技术文章的点赞状态。
     * 先检查用户维度点赞记录，再按新增或删除结果同步更新文章点赞计数。
     */
    @Override
    public Map<String, Object> toggleArticleLike(String articleKey, Long currentUserId) {
        if (currentUserId == null) {
            throw new BusinessException(401, "请先登录后再点赞");
        }

        boolean liked;
        if (contentMapper.countArticleLikeByUser(articleKey, currentUserId) > 0) {
            int deleted = contentMapper.deleteArticleLike(articleKey, currentUserId);
            if (deleted > 0) {
                contentMapper.decrementLikeCount(articleKey);
            }
            liked = false;
        } else {
            int inserted = contentMapper.insertArticleLike(articleKey, currentUserId);
            if (inserted > 0) {
                contentMapper.incrementLikeCount(articleKey);
            }
            liked = true;
        }

        Integer likeCount = contentMapper.findLikeCount(articleKey);
        Map<String, Object> result = new HashMap<>();
        result.put("liked", liked);
        result.put("likeCount", likeCount != null ? likeCount : 0);
        return result;
    }

    /**
     * 新增一条技术文章评论并同步更新评论数。
     * 评论入库后重新查询列表末尾匹配项，作为当前新增评论返回给前端。
     */
    @Override
    public Map<String, Object> addArticleComment(String articleKey, String nickname, String content, String avatarText, String avatarAccent, Long parentId) {
        contentMapper.insertArticleComment(articleKey, nickname, content, avatarText, avatarAccent, parentId);
        contentMapper.incrementCommentCount(articleKey);

        List<Map<String, Object>> comments = contentMapper.findArticleComments(articleKey);
        Map<String, Object> newComment = comments.stream()
            .filter(c -> nickname.equals(c.get("nickname")) && content.equals(c.get("content")))
            .reduce((first, second) -> second)
            .orElse(Collections.emptyMap());

        if (!newComment.isEmpty() && !newComment.containsKey("parentId")) {
            newComment.put("parentId", null);
        }

        return newComment;
    }

    /**
     * 获取指定技术文章的评论列表。
     * 遍历补齐空父评论键，避免前端在构建树形回复时出现字段缺失。
     */
    @Override
    public List<Map<String, Object>> listArticleComments(String articleKey) {
        List<Map<String, Object>> comments = contentMapper.findArticleComments(articleKey);
        comments.forEach(comment -> {
            if (!comment.containsKey("parentId")) {
                comment.put("parentId", null);
            }
        });
        return comments;
    }

    /**
     * 删除指定评论并同步回收评论数。
     * 只有先查到所属文章且软删除成功时，才会继续下调文章上的统计值。
     */
    @Override
    public boolean deleteArticleComment(Long commentId) {
        String articleKey = contentMapper.findArticleKeyByCommentId(commentId);
        if (articleKey == null) {
            return false;
        }
        int rows = contentMapper.softDeleteComment(commentId);
        if (rows > 0) {
            contentMapper.decrementCommentCount(articleKey);
            return true;
        }
        return false;
    }

    /**
     * 把数据库里的竖线分隔字段转换成数组。
     * 统一在 Service 层解析持久化格式，避免多个页面重复拆分字符串。
     */
    private List<String> splitPipeValues(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return List.of();
        }
        return Arrays.stream(rawValue.split("\\|"))
            .map(String::trim)
            .filter(value -> !value.isEmpty())
            .toList();
    }

    /**
     * 把数据库里的 0 和 1 状态字段转换成布尔值。
     * 这里顺带兜底空值场景，保证前端拿到的永远是明确的 true 或 false。
     */
    private Boolean toBoolean(Integer flag) {
        return flag != null && flag == 1;
    }

    /**
     * 把搜索词整理成 SQL LIKE 可用的模糊匹配格式。
     * 去掉首尾空白后统一包上百分号，让搜索和联想复用同一套关键词规则。
     */
    private String wrapKeyword(String keyword) {
        String normalizedKeyword = keyword == null ? "" : keyword.trim();
        return "%" + normalizedKeyword + "%";
    }

    /**
     * 归一化前端传入的榜单类型。
     * 非法类型统一回退到 rising，避免 SQL 分支拿到未知值后出现空排序。
     */
    private String normalizeRankingType(String type) {
        if (type == null || type.isBlank()) {
            return "rising";
        }
        return switch (type) {
            case "rising", "newbook", "overall", "masterpiece" -> type;
            default -> "rising";
        };
    }
}
