package com.interview.auth.service.impl;

import com.interview.auth.domain.dto.response.AiHotspotResponse;
import com.interview.auth.domain.dto.response.TechArticleAuthorResponse;
import com.interview.auth.domain.dto.response.TechArticleResponse;
import com.interview.auth.domain.dto.response.WorldNewsIssueResponse;
import com.interview.auth.domain.entity.AiHotspot;
import com.interview.auth.domain.entity.TechArticle;
import com.interview.auth.domain.entity.WorldNewsIssue;
import com.interview.auth.infrastructure.mapper.ContentMapper;
import com.interview.auth.service.ContentService;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 业务目的：把三类内容从 MySQL 实体转换成前端可直接消费的频道数据结构。
 * 业务逻辑：Service 层集中处理时间格式、布尔字段和字符串数组拆分，保证多个页面共享统一输出口径。
 */
@Service
@RequiredArgsConstructor
public class ContentServiceImpl implements ContentService {

    private static final DateTimeFormatter ARTICLE_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter HOTSPOT_DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final ContentMapper contentMapper;

    /**
     * 业务目的：查询技术文章列表并转成前端文章流结构。
     * 业务逻辑：后端补齐作者对象、精选标记和亮点数组，让文章页与头部导航共用一份数据源。
     */
    @Override
    public List<TechArticleResponse> listTechArticles() {
        return contentMapper.findPublishedTechArticles()
            .stream()
            .map(this::toTechArticleResponse)
            .toList();
    }

    /**
     * 业务目的：查询看天下期刊列表并转成前端展示结构。
     * 业务逻辑：这里直接透传频道页所需字段，避免页面再做字段重命名。
     */
    @Override
    public List<WorldNewsIssueResponse> listWorldNewsIssues() {
        return contentMapper.findPublishedWorldNewsIssues()
            .stream()
            .map(this::toWorldNewsIssueResponse)
            .toList();
    }

    /**
     * 业务目的：查询 AI 热点列表并转成前端热点流结构。
     * 业务逻辑：在后端完成标签拆分与时间格式化，前端只做推荐 / 最新的界面切换。
     */
    @Override
    public List<AiHotspotResponse> listAiHotspots() {
        return contentMapper.findPublishedAiHotspots()
            .stream()
            .map(this::toAiHotspotResponse)
            .toList();
    }

    /**
     * 业务目的：把技术文章实体转换成前端文章对象。
     * 业务逻辑：使用 article_key 作为前端 id，保持现有页面 key 和路由行为稳定。
     */
    private TechArticleResponse toTechArticleResponse(TechArticle article) {
        TechArticleResponse response = new TechArticleResponse();
        response.setId(article.getArticleKey());
        response.setCategory(article.getCategory());
        response.setTitle(article.getTitle());
        response.setSummary(article.getSummary());
        response.setEssence(article.getEssence());
        response.setHighlights(splitPipeValues(article.getHighlightList()));
        response.setAuthor(toAuthorResponse(article));
        response.setCoverUrl(article.getCoverUrl());
        response.setPublishedAt(article.getPublishedAt() == null ? null : article.getPublishedAt().format(ARTICLE_DATE_FORMATTER));
        response.setReadCount(article.getReadCount());
        response.setLikeCount(article.getLikeCount());
        response.setCollectCount(article.getCollectCount());
        response.setCommentCount(article.getCommentCount());
        response.setReadTime(article.getReadTime());
        response.setIsVip(toBoolean(article.getIsVip()));
        response.setIsCollected(toBoolean(article.getIsCollected()));
        response.setIsLiked(toBoolean(article.getIsLiked()));
        response.setInHistory(toBoolean(article.getInHistory()));
        response.setFeatured(toBoolean(article.getFeatured()));
        return response;
    }

    /**
     * 业务目的：把作者原始字段组装成前端沿用的 author 嵌套结构。
     * 业务逻辑：页面层无需理解数据库字段拆分方式，直接读取统一 author 对象即可。
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
     * 业务目的：把看天下实体转换成前端期刊卡片结构。
     * 业务逻辑：频道页当前不需要额外派生字段，因此保持一一映射，降低维护复杂度。
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
        return response;
    }

    /**
     * 业务目的：把 AI 热点实体转换成前端热点流结构。
     * 业务逻辑：后端统一处理标签数组与推荐标记，保证推荐流和最新流共用一套接口数据。
     */
    private AiHotspotResponse toAiHotspotResponse(AiHotspot hotspot) {
        AiHotspotResponse response = new AiHotspotResponse();
        response.setId(hotspot.getHotspotKey());
        response.setTrack(hotspot.getTrack());
        response.setHotspotType(hotspot.getHotspotType());
        response.setTitle(hotspot.getTitle());
        response.setSummary(hotspot.getSummary());
        response.setAuthorName(hotspot.getAuthorName());
        response.setPublishedAt(hotspot.getPublishedAt() == null ? null : hotspot.getPublishedAt().format(HOTSPOT_DATE_TIME_FORMATTER));
        response.setCoverUrl(hotspot.getCoverUrl());
        response.setTags(splitPipeValues(hotspot.getTagList()));
        response.setViewCount(hotspot.getViewCount());
        response.setCommentCount(hotspot.getCommentCount());
        response.setLikeCount(hotspot.getLikeCount());
        response.setHeat(hotspot.getHeat());
        response.setIsRecommended(toBoolean(hotspot.getIsRecommended()));
        response.setIsToday(toBoolean(hotspot.getIsToday()));
        return response;
    }

    /**
     * 业务目的：把数据库里的竖线分隔字段转换成前端数组。
     * 业务逻辑：统一在 Service 层处理，避免多个页面重复解析持久化格式。
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
     * 业务目的：把数据库中的 0/1 状态字段转换成前端布尔值。
     * 业务逻辑：统一兜底 null 场景，保证前端总是拿到明确的 true / false。
     */
    private Boolean toBoolean(Integer flag) {
        return flag != null && flag == 1;
    }
}
