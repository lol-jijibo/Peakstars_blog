package com.interview.auth.admin.service.impl;

import com.interview.auth.admin.dto.request.AdminBatchUpsertRequest;
import com.interview.auth.admin.dto.request.AdminContentUpsertRequest;
import com.interview.auth.admin.dto.response.AdminContentRecordResponse;
import com.interview.auth.admin.dto.response.AdminDashboardResponse;
import com.interview.auth.admin.dto.response.AdminModuleStatResponse;
import com.interview.auth.admin.dto.response.AdminRecentEditResponse;
import com.interview.auth.admin.dto.response.AdminSummaryResponse;
import com.interview.auth.admin.dto.response.AdminTrendPointResponse;
import com.interview.auth.admin.entity.ContentEditLog;
import com.interview.auth.admin.mapper.AdminMapper;
import com.interview.auth.admin.service.AdminService;
import com.interview.auth.common.BusinessException;
import com.interview.auth.domain.entity.AiHotspot;
import com.interview.auth.domain.entity.TechArticle;
import com.interview.auth.domain.entity.WorldNewsIssue;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 业务目的：集中承接后台管理台的数据聚合、内容 CRUD 和实时心跳统计。
 * 业务逻辑：后端统一负责模块差异映射、仪表盘快照生成和编辑日志记录，前端只消费标准化结构。
 */
@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private static final String TYPE_TECH = "tech";
    private static final String TYPE_WORLD = "world";
    private static final String TYPE_AI = "ai";
    private static final String DEFAULT_OPERATOR = "admin";
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final DateTimeFormatter DATE_ONLY_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_LABEL_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final Duration ONLINE_EXPIRE_WINDOW = Duration.ofSeconds(75);
    private static final Duration SNAPSHOT_MIN_INTERVAL = Duration.ofSeconds(20);
    private static final int MAX_TREND_POINTS = 12;

    private final AdminMapper adminMapper;

    /**
     * 业务目的：维护后台当前在线会话集合，给管理台实时在线人数提供数据来源。
     * 业务逻辑：每个 clientId 的最后心跳时间只保存在内存中，足以满足单实例管理后台实时展示需求。
     */
    private final Map<String, LocalDateTime> onlineClientMap = new ConcurrentHashMap<>();

    /**
     * 业务目的：维护后台仪表盘最近一段时间的趋势快照，供 ECharts 折线图直接使用。
     * 业务逻辑：每次心跳或仪表盘刷新都会按节流规则写入快照，避免前端自己拼接趋势历史。
     */
    private final Deque<DashboardSnapshot> dashboardSnapshots = new ArrayDeque<>();

    /**
     * 业务目的：记录后台心跳，驱动在线人数实时变化。
     * 业务逻辑：同一个 clientId 反复心跳只刷新过期时间，不会重复累计在线人数。
     */
    @Override
    public void heartbeat(String clientId) {
        trimExpiredClients();
        onlineClientMap.put(clientId, LocalDateTime.now());
        appendSnapshotIfNeeded(buildCurrentSummary());
    }

    /**
     * 业务目的：聚合后台仪表盘所需的全部数据。
     * 业务逻辑：查询数据库当前总量后补齐趋势快照、模块分布和最近编辑，保证管理台首屏只打一条接口。
     */
    @Override
    public AdminDashboardResponse getDashboard() {
        AdminSummaryResponse summary = buildCurrentSummary();
        appendSnapshotIfNeeded(summary);

        AdminDashboardResponse response = new AdminDashboardResponse();
        response.setSummary(summary);
        response.setTrendPoints(buildTrendPoints());
        response.setModuleStats(buildModuleStats());
        response.setRecentEdits(buildRecentEdits());
        response.setHotContents(buildHotContents());
        return response;
    }

    /**
     * 业务目的：按模块返回后台管理列表数据。
     * 业务逻辑：不同表的原始结构会在这里映射成统一记录格式，便于前端共享一个表格和编辑抽屉。
     */
    @Override
    public List<AdminContentRecordResponse> listContent(String type) {
        return switch (normalizeType(type)) {
            case TYPE_TECH -> adminMapper.findAllTechArticles().stream().map(this::toTechAdminRecord).toList();
            case TYPE_WORLD -> adminMapper.findAllWorldNewsIssues().stream().map(this::toWorldAdminRecord).toList();
            case TYPE_AI -> adminMapper.findAllAiHotspots().stream().map(this::toAiAdminRecord).toList();
            default -> throw new BusinessException(400, "Unsupported content type");
        };
    }

    /**
     * 业务目的：处理后台单条内容的新增和编辑保存。
     * 业务逻辑：先按模块补齐默认值和业务主键，再执行幂等写入并记录最近编辑日志。
     */
    @Override
    @Transactional
    public AdminContentRecordResponse saveContent(String type, String contentKey, AdminContentUpsertRequest request) {
        String normalizedType = normalizeType(type);
        String resolvedKey = resolveContentKey(normalizedType, contentKey, request.getId(), request.getTitle());

        AdminContentRecordResponse saved = switch (normalizedType) {
            case TYPE_TECH -> saveTechArticle(resolvedKey, request);
            case TYPE_WORLD -> saveWorldNewsIssue(resolvedKey, request);
            case TYPE_AI -> saveAiHotspot(resolvedKey, request);
            default -> throw new BusinessException(400, "Unsupported content type");
        };

        appendEditLog(normalizedType, resolvedKey, "update", request.getTitle());
        appendSnapshotIfNeeded(buildCurrentSummary());
        return saved;
    }

    /**
     * 业务目的：接收 Excel 批量导入后的多条记录写入请求。
     * 业务逻辑：逐条复用单条写入逻辑，并在完成后补一条批量导入日志，便于后台回看导入动作。
     */
    @Override
    @Transactional
    public List<AdminContentRecordResponse> batchSaveContent(String type, AdminBatchUpsertRequest request) {
        String normalizedType = normalizeType(type);
        List<AdminContentRecordResponse> results = new ArrayList<>();

        for (AdminContentUpsertRequest record : request.getRecords()) {
            results.add(saveContent(normalizedType, record.getId(), record));
        }

        appendEditLog(normalizedType, "batch-import", "batch-import", "批量导入 " + results.size() + " 条内容");
        appendSnapshotIfNeeded(buildCurrentSummary());
        return results;
    }

    /**
     * 业务目的：软删除指定内容记录，让前台列表立即不再展示该内容。
     * 业务逻辑：删除动作只更新 status，并同步记录一条编辑日志，便于管理台展示最近变更。
     */
    @Override
    @Transactional
    public void deleteContent(String type, String contentKey) {
        String normalizedType = normalizeType(type);
        int affectedRows = switch (normalizedType) {
            case TYPE_TECH -> adminMapper.disableTechArticle(contentKey);
            case TYPE_WORLD -> adminMapper.disableWorldNewsIssue(contentKey);
            case TYPE_AI -> adminMapper.disableAiHotspot(contentKey);
            default -> 0;
        };

        if (affectedRows == 0) {
            throw new BusinessException(404, "Content not found");
        }

        appendEditLog(normalizedType, contentKey, "delete", contentKey);
        appendSnapshotIfNeeded(buildCurrentSummary());
    }

    /**
     * 业务目的：构建当前仪表盘汇总指标。
     * 业务逻辑：把三类内容的浏览、评论和数量汇总后，再叠加在线人数和最近更新时间，形成顶部指标卡片。
     */
    private AdminSummaryResponse buildCurrentSummary() {
        trimExpiredClients();

        int techCount = safeInt(adminMapper.countPublishedTechArticles());
        int worldCount = safeInt(adminMapper.countPublishedWorldNewsIssues());
        int aiCount = safeInt(adminMapper.countPublishedAiHotspots());
        int techViews = safeInt(adminMapper.sumTechArticleViews());
        int worldViews = safeInt(adminMapper.sumWorldNewsReads());
        int aiViews = safeInt(adminMapper.sumAiHotspotViews());
        int totalComments = safeInt(adminMapper.sumTechArticleComments()) + safeInt(adminMapper.sumAiHotspotComments());

        AdminSummaryResponse summary = new AdminSummaryResponse();
        summary.setOnlineUsers(onlineClientMap.size());
        summary.setTotalViews(techViews + worldViews + aiViews);
        summary.setTotalComments(totalComments);
        summary.setEditsToday(safeInt(adminMapper.countTodayEdits()));
        summary.setTotalContents(techCount + worldCount + aiCount);
        summary.setLastUpdatedAt(LocalDateTime.now().format(DATE_TIME_FORMATTER));
        return summary;
    }

    /**
     * 业务目的：构建后台趋势图所需的时间序列点。
     * 业务逻辑：趋势点来源于后端维护的快照队列，前端无需自行做轮询历史累积。
     */
    private List<AdminTrendPointResponse> buildTrendPoints() {
        List<AdminTrendPointResponse> trendPoints = new ArrayList<>();
        for (DashboardSnapshot snapshot : dashboardSnapshots) {
            AdminTrendPointResponse point = new AdminTrendPointResponse();
            point.setTimeLabel(snapshot.createdAt().format(TIME_LABEL_FORMATTER));
            point.setOnlineUsers(snapshot.onlineUsers());
            point.setTotalViews(snapshot.totalViews());
            point.setTotalComments(snapshot.totalComments());
            point.setEditsToday(snapshot.editsToday());
            trendPoints.add(point);
        }
        return trendPoints;
    }

    /**
     * 业务目的：构建模块维度统计，供 G2 做高级对比图表。
     * 业务逻辑：每个模块统一输出内容量、浏览量和评论量，方便前端绘制多指标对比。
     */
    private List<AdminModuleStatResponse> buildModuleStats() {
        return List.of(
            createModuleStat(TYPE_TECH, "技术文章", safeInt(adminMapper.countPublishedTechArticles()), safeInt(adminMapper.sumTechArticleViews()), safeInt(adminMapper.sumTechArticleComments())),
            createModuleStat(TYPE_WORLD, "看天下", safeInt(adminMapper.countPublishedWorldNewsIssues()), safeInt(adminMapper.sumWorldNewsReads()), 0),
            createModuleStat(TYPE_AI, "AI 热点", safeInt(adminMapper.countPublishedAiHotspots()), safeInt(adminMapper.sumAiHotspotViews()), safeInt(adminMapper.sumAiHotspotComments()))
        );
    }

    /**
     * 业务目的：构建后台最近编辑时间线。
     * 业务逻辑：直接读取最近日志并格式化时间，前端可以把结果作为动态列表展示。
     */
    private List<AdminRecentEditResponse> buildRecentEdits() {
        return adminMapper.findRecentEditLogs(8).stream().map(log -> {
            AdminRecentEditResponse response = new AdminRecentEditResponse();
            response.setId(log.getId());
            response.setContentType(log.getContentType());
            response.setContentKey(log.getContentKey());
            response.setActionType(log.getActionType());
            response.setOperatorName(log.getOperatorName());
            response.setContentTitle(log.getContentTitle());
            response.setCreatedAt(log.getCreatedAt() == null ? null : log.getCreatedAt().format(DATE_TIME_FORMATTER));
            return response;
        }).toList();
    }

    /**
     * 业务目的：构建后台热门内容列表，给管理台展示当前最值得关注的内容。
     * 业务逻辑：把三类内容统一折算成浏览量后排序，便于管理员快速定位高热内容。
     */
    private List<AdminContentRecordResponse> buildHotContents() {
        List<AdminContentRecordResponse> records = new ArrayList<>();
        records.addAll(adminMapper.findAllTechArticles().stream().map(this::toTechAdminRecord).toList());
        records.addAll(adminMapper.findAllWorldNewsIssues().stream().map(this::toWorldAdminRecord).toList());
        records.addAll(adminMapper.findAllAiHotspots().stream().map(this::toAiAdminRecord).toList());

        return records.stream()
            .sorted(Comparator.comparing((AdminContentRecordResponse item) -> safeInt(item.getViewCount())).reversed())
            .limit(6)
            .toList();
    }

    /**
     * 业务目的：按模块创建统一的统计对象。
     * 业务逻辑：保证不同内容模块都能按同一结构输出给图表层使用。
     */
    private AdminModuleStatResponse createModuleStat(String moduleKey, String moduleLabel, int contentCount, int viewCount, int commentCount) {
        AdminModuleStatResponse response = new AdminModuleStatResponse();
        response.setModuleKey(moduleKey);
        response.setModuleLabel(moduleLabel);
        response.setContentCount(contentCount);
        response.setViewCount(viewCount);
        response.setCommentCount(commentCount);
        return response;
    }

    /**
     * 业务目的：写入技术文章内容。
     * 业务逻辑：把后台统一请求映射成技术文章实体，并执行幂等保存。
     */
    private AdminContentRecordResponse saveTechArticle(String articleKey, AdminContentUpsertRequest request) {
        TechArticle article = new TechArticle();
        article.setArticleKey(articleKey);
        article.setCategory(defaultString(request.getCategory(), "frontend"));
        article.setTitle(requireTitle(request.getTitle()));
        article.setSummary(defaultString(request.getSummary(), ""));
        article.setEssence(defaultString(request.getEssence(), request.getSummary()));
        article.setHighlightList(joinPipeValues(request.getHighlights()));
        article.setAuthorName(defaultString(request.getAuthorName(), "后台编辑"));
        article.setAuthorRole(defaultString(request.getAuthorRole(), "内容运营"));
        article.setAuthorInitials(defaultString(request.getAuthorInitials(), buildInitials(article.getAuthorName())));
        article.setAuthorAccent(defaultString(request.getAuthorAccent(), "linear-gradient(135deg, #2563eb, #60a5fa)"));
        article.setCoverUrl(defaultString(request.getCoverUrl(), "/peakstars-blog-icon.jpg"));
        article.setContentHtml(defaultString(request.getContentHtml(), wrapParagraph(request.getSummary())));
        article.setPublishedAt(parseDateTime(defaultString(request.getPublishedAt(), LocalDateTime.now().format(DATE_TIME_FORMATTER))));
        article.setReadCount(safeInt(request.getViewCount()));
        article.setLikeCount(safeInt(request.getLikeCount()));
        article.setCollectCount(safeInt(request.getCollectCount()));
        article.setCommentCount(safeInt(request.getCommentCount()));
        article.setReadTime(defaultString(request.getReadTime(), "6 min"));
        article.setIsVip(toFlag(request.getVip()));
        article.setIsCollected(toFlag(request.getCollected()));
        article.setIsLiked(toFlag(request.getLiked()));
        article.setInHistory(toFlag(request.getHistory()));
        article.setFeatured(toFlag(request.getFeatured()));
        article.setStatus(1);
        article.setSortOrder(0);
        adminMapper.saveTechArticle(article);
        return toTechAdminRecord(article);
    }

    /**
     * 业务目的：写入看天下期刊内容。
     * 业务逻辑：把后台统一请求映射成期刊实体，并保留期刊封面和推荐值等展示字段。
     */
    private AdminContentRecordResponse saveWorldNewsIssue(String issueKey, AdminContentUpsertRequest request) {
        WorldNewsIssue issue = new WorldNewsIssue();
        issue.setIssueKey(issueKey);
        issue.setTitle(requireTitle(request.getTitle()));
        issue.setIssueLabel(defaultString(request.getIssueLabel(), DATE_ONLY_FORMATTER.format(LocalDateTime.now()).replace("-", ".")));
        issue.setCategory(defaultString(request.getCategory(), "看天下"));
        issue.setTodayReads(safeInt(request.getViewCount()));
        issue.setRecommendation(defaultBigDecimal(request.getRecommendation()));
        issue.setDescription(defaultString(request.getSummary(), ""));
        issue.setCoverAccent(defaultString(request.getCoverAccent(), "#d33b2d"));
        issue.setCoverKicker(defaultString(request.getCoverKicker(), "期刊聚焦"));
        issue.setCoverHeadline(defaultString(request.getCoverHeadline(), requireTitle(request.getTitle())));
        issue.setCoverSummary(defaultString(request.getCoverSummary(), request.getSummary()));
        issue.setCoverFooter(defaultString(request.getCoverFooter(), "专题 / 深读 / 评论"));
        issue.setContentHtml(defaultString(request.getContentHtml(), wrapParagraph(request.getSummary())));
        issue.setPublishedAt(parseDateTime(defaultString(request.getPublishedAt(), LocalDateTime.now().format(DATE_TIME_FORMATTER))));
        issue.setStatus(1);
        issue.setSortOrder(0);
        adminMapper.saveWorldNewsIssue(issue);
        return toWorldAdminRecord(issue);
    }

    /**
     * 业务目的：写入 AI 热点内容。
     * 业务逻辑：把后台统一请求映射成热点实体，并保留赛道、热度、推荐态和标签配置。
     */
    private AdminContentRecordResponse saveAiHotspot(String hotspotKey, AdminContentUpsertRequest request) {
        AiHotspot hotspot = new AiHotspot();
        hotspot.setHotspotKey(hotspotKey);
        hotspot.setTrack(defaultString(request.getTrack(), "agent"));
        hotspot.setHotspotType(defaultString(request.getHotspotType(), hotspot.getTrack()));
        hotspot.setTitle(requireTitle(request.getTitle()));
        hotspot.setSummary(defaultString(request.getSummary(), ""));
        hotspot.setAuthorName(defaultString(request.getAuthorName(), "后台编辑"));
        hotspot.setPublishedAt(parseDateTime(defaultString(request.getPublishedAt(), LocalDateTime.now().format(DATE_TIME_FORMATTER))));
        hotspot.setCoverUrl(defaultString(request.getCoverUrl(), "/peakstars-blog-icon.jpg"));
        hotspot.setContentHtml(defaultString(request.getContentHtml(), wrapParagraph(request.getSummary())));
        hotspot.setTagList(joinPipeValues(request.getTags()));
        hotspot.setViewCount(safeInt(request.getViewCount()));
        hotspot.setCommentCount(safeInt(request.getCommentCount()));
        hotspot.setLikeCount(safeInt(request.getLikeCount()));
        hotspot.setHeat(safeInt(request.getHeat()));
        hotspot.setIsRecommended(toFlag(request.getRecommended()));
        hotspot.setIsToday(toFlag(request.getToday()));
        hotspot.setStatus(1);
        hotspot.setSortOrder(0);
        adminMapper.saveAiHotspot(hotspot);
        return toAiAdminRecord(hotspot);
    }

    /**
     * 业务目的：把技术文章实体转换成后台统一记录。
     * 业务逻辑：统一输出后台表格与编辑抽屉所需字段，前端不再依赖多个模块的不同对象结构。
     */
    private AdminContentRecordResponse toTechAdminRecord(TechArticle article) {
        AdminContentRecordResponse response = new AdminContentRecordResponse();
        response.setType(TYPE_TECH);
        response.setId(article.getArticleKey());
        response.setTitle(article.getTitle());
        response.setCategory(article.getCategory());
        response.setSummary(article.getSummary());
        response.setEssence(article.getEssence());
        response.setHighlights(splitPipeValues(article.getHighlightList()));
        response.setAuthorName(article.getAuthorName());
        response.setAuthorRole(article.getAuthorRole());
        response.setAuthorInitials(article.getAuthorInitials());
        response.setAuthorAccent(article.getAuthorAccent());
        response.setCoverUrl(article.getCoverUrl());
        response.setContentHtml(article.getContentHtml());
        response.setPublishedAt(formatDateTime(article.getPublishedAt()));
        response.setViewCount(safeInt(article.getReadCount()));
        response.setCommentCount(safeInt(article.getCommentCount()));
        response.setLikeCount(safeInt(article.getLikeCount()));
        response.setCollectCount(safeInt(article.getCollectCount()));
        response.setReadTime(article.getReadTime());
        response.setFeatured(isTrue(article.getFeatured()));
        response.setVip(isTrue(article.getIsVip()));
        response.setCollected(isTrue(article.getIsCollected()));
        response.setLiked(isTrue(article.getIsLiked()));
        response.setHistory(isTrue(article.getInHistory()));
        return response;
    }

    /**
     * 业务目的：把看天下实体转换成后台统一记录。
     * 业务逻辑：沿用统一记录结构输出期刊封面和推荐值字段，方便后台在同一页面管理。
     */
    private AdminContentRecordResponse toWorldAdminRecord(WorldNewsIssue issue) {
        AdminContentRecordResponse response = new AdminContentRecordResponse();
        response.setType(TYPE_WORLD);
        response.setId(issue.getIssueKey());
        response.setTitle(issue.getTitle());
        response.setCategory(issue.getCategory());
        response.setSummary(issue.getDescription());
        response.setCoverUrl("");
        response.setContentHtml(issue.getContentHtml());
        response.setPublishedAt(formatDateTime(issue.getPublishedAt()));
        response.setViewCount(safeInt(issue.getTodayReads()));
        response.setIssueLabel(issue.getIssueLabel());
        response.setRecommendation(issue.getRecommendation());
        response.setCoverAccent(issue.getCoverAccent());
        response.setCoverKicker(issue.getCoverKicker());
        response.setCoverHeadline(issue.getCoverHeadline());
        response.setCoverSummary(issue.getCoverSummary());
        response.setCoverFooter(issue.getCoverFooter());
        return response;
    }

    /**
     * 业务目的：把 AI 热点实体转换成后台统一记录。
     * 业务逻辑：保留热度、推荐态和标签数组，方便后台做热点分组和编辑管理。
     */
    private AdminContentRecordResponse toAiAdminRecord(AiHotspot hotspot) {
        AdminContentRecordResponse response = new AdminContentRecordResponse();
        response.setType(TYPE_AI);
        response.setId(hotspot.getHotspotKey());
        response.setTitle(hotspot.getTitle());
        response.setSummary(hotspot.getSummary());
        response.setAuthorName(hotspot.getAuthorName());
        response.setCoverUrl(hotspot.getCoverUrl());
        response.setContentHtml(hotspot.getContentHtml());
        response.setPublishedAt(formatDateTime(hotspot.getPublishedAt()));
        response.setViewCount(safeInt(hotspot.getViewCount()));
        response.setCommentCount(safeInt(hotspot.getCommentCount()));
        response.setLikeCount(safeInt(hotspot.getLikeCount()));
        response.setTrack(hotspot.getTrack());
        response.setHotspotType(hotspot.getHotspotType());
        response.setHeat(safeInt(hotspot.getHeat()));
        response.setRecommended(isTrue(hotspot.getIsRecommended()));
        response.setToday(isTrue(hotspot.getIsToday()));
        response.setTags(splitPipeValues(hotspot.getTagList()));
        return response;
    }

    /**
     * 业务目的：写入后台编辑日志，支撑仪表盘最近编辑动态与今日编辑统计。
     * 业务逻辑：每次保存、删除和批量导入都记录一条操作轨迹，便于后台管理员回看最近变化。
     */
    private void appendEditLog(String contentType, String contentKey, String actionType, String contentTitle) {
        ContentEditLog log = new ContentEditLog();
        log.setContentType(contentType);
        log.setContentKey(contentKey);
        log.setActionType(actionType);
        log.setOperatorName(DEFAULT_OPERATOR);
        log.setContentTitle(defaultString(contentTitle, contentKey));
        adminMapper.insertContentEditLog(log);
    }

    /**
     * 业务目的：清理后台已过期的在线会话。
     * 业务逻辑：超过过期窗口未继续上报心跳的 clientId 会被移除，保证在线人数接近实时状态。
     */
    private void trimExpiredClients() {
        LocalDateTime now = LocalDateTime.now();
        onlineClientMap.entrySet().removeIf(entry -> Duration.between(entry.getValue(), now).compareTo(ONLINE_EXPIRE_WINDOW) > 0);
    }

    /**
     * 业务目的：把当前仪表盘汇总值按节流规则写入趋势快照队列。
     * 业务逻辑：只有间隔达到阈值时才记录新点，避免后台频繁轮询导致趋势图点位爆炸。
     */
    private synchronized void appendSnapshotIfNeeded(AdminSummaryResponse summary) {
        LocalDateTime now = LocalDateTime.now();
        DashboardSnapshot lastSnapshot = dashboardSnapshots.peekLast();
        if (lastSnapshot != null && Duration.between(lastSnapshot.createdAt(), now).compareTo(SNAPSHOT_MIN_INTERVAL) < 0) {
            return;
        }

        dashboardSnapshots.addLast(new DashboardSnapshot(
            now,
            safeInt(summary.getOnlineUsers()),
            safeInt(summary.getTotalViews()),
            safeInt(summary.getTotalComments()),
            safeInt(summary.getEditsToday())
        ));

        while (dashboardSnapshots.size() > MAX_TREND_POINTS) {
            dashboardSnapshots.removeFirst();
        }
    }

    /**
     * 业务目的：规范后台内容类型，避免前后端传值不一致导致分支逻辑失效。
     * 业务逻辑：统一转成小写后匹配固定模块 key，不支持的类型直接抛业务异常。
     */
    private String normalizeType(String type) {
        String normalized = defaultString(type, "").trim().toLowerCase();
        if (List.of(TYPE_TECH, TYPE_WORLD, TYPE_AI).contains(normalized)) {
            return normalized;
        }
        throw new BusinessException(400, "Unsupported content type");
    }

    /**
     * 业务目的：为新增内容生成稳定业务主键，同时允许编辑场景沿用原始主键。
     * 业务逻辑：优先复用路径参数和请求体内主键，缺失时再按模块前缀和标题生成可读 key。
     */
    private String resolveContentKey(String type, String contentKey, String requestId, String title) {
        if (contentKey != null && !contentKey.isBlank()) {
            return contentKey.trim();
        }
        if (requestId != null && !requestId.isBlank()) {
            return requestId.trim();
        }

        String slugBase = defaultString(title, type).trim().toLowerCase()
            .replaceAll("[^a-z0-9\\u4e00-\\u9fa5]+", "-")
            .replaceAll("(^-|-$)", "");
        if (slugBase.isBlank()) {
            slugBase = UUID.randomUUID().toString().substring(0, 8);
        }

        return switch (type) {
            case TYPE_TECH -> "tech-" + slugBase;
            case TYPE_WORLD -> "world-" + slugBase;
            case TYPE_AI -> "ai-" + slugBase;
            default -> slugBase;
        };
    }

    /**
     * 业务目的：把后台输入的发布时间文案解析成数据库时间。
     * 业务逻辑：兼容 yyyy-MM-dd 和 yyyy-MM-dd HH:mm 两种常见输入格式，减少表单录入限制。
     */
    private LocalDateTime parseDateTime(String value) {
        if (value == null || value.isBlank()) {
            return LocalDateTime.now();
        }

        String trimmed = value.trim();
        if (trimmed.length() == 10) {
            return LocalDateTime.parse(trimmed + " 00:00", DATE_TIME_FORMATTER);
        }
        return LocalDateTime.parse(trimmed.replace("T", " "), DATE_TIME_FORMATTER);
    }

    /**
     * 业务目的：统一格式化后台返回时间，方便前端直接显示。
     * 业务逻辑：避免页面在多个表格和抽屉里重复处理日期格式。
     */
    private String formatDateTime(LocalDateTime value) {
        return value == null ? null : value.format(DATE_TIME_FORMATTER);
    }

    /**
     * 业务目的：把列表字段统一存成竖线拼接字符串，适配数据库持久化格式。
     * 业务逻辑：忽略空值后再拼接，避免前端导入和手动编辑把空标签落进数据库。
     */
    private String joinPipeValues(List<String> values) {
        if (values == null || values.isEmpty()) {
            return "";
        }
        return values.stream()
            .filter(Objects::nonNull)
            .map(String::trim)
            .filter(item -> !item.isEmpty())
            .reduce((left, right) -> left + "|" + right)
            .orElse("");
    }

    /**
     * 业务目的：把数据库中竖线分隔字段还原成数组，方便后台表单和标签组件直接使用。
     * 业务逻辑：统一在后端拆分，前端无须关心持久化格式细节。
     */
    private List<String> splitPipeValues(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return List.of();
        }
        return Arrays.stream(rawValue.split("\\|"))
            .map(String::trim)
            .filter(item -> !item.isEmpty())
            .toList();
    }

    /**
     * 业务目的：把可能为空的字符串字段统一补成默认值。
     * 业务逻辑：保障 MySQL 非空列在新增和批量导入场景下始终拿到有效内容。
     */
    private String defaultString(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }

    /**
     * 业务目的：要求后台保存操作必须携带标题。
     * 业务逻辑：标题是管理台表格、日志和主键生成的核心字段，缺失时直接阻断保存。
     */
    private String requireTitle(String title) {
        if (title == null || title.isBlank()) {
            throw new BusinessException(400, "标题不能为空");
        }
        return title.trim();
    }

    /**
     * 业务目的：把布尔值转成数据库 0/1 标志位。
     * 业务逻辑：统一处理 null 场景，避免空值落库破坏后续前端布尔判断。
     */
    private Integer toFlag(Boolean value) {
        return Boolean.TRUE.equals(value) ? 1 : 0;
    }

    /**
     * 业务目的：把数据库或请求里的可空整型统一转成非空数值。
     * 业务逻辑：管理台统计和图表不接受 null，因此这里统一做兜底。
     */
    private int safeInt(Integer value) {
        return value == null ? 0 : value;
    }

    /**
     * 业务目的：把数据库 0/1 标志位统一转成前端布尔值。
     * 业务逻辑：避免前端对数值状态做额外判断，直接消费 true / false。
     */
    private Boolean isTrue(Integer flag) {
        return flag != null && flag == 1;
    }

    /**
     * 业务目的：把推荐值空值统一转成 0，保证看天下期刊表单和图表输出稳定。
     * 业务逻辑：防止空推荐值在后台表格和 Excel 导入导出场景下造成格式异常。
     */
    private BigDecimal defaultBigDecimal(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    /**
     * 业务目的：给后台新增作者自动补一个简短头像缩写。
     * 业务逻辑：没有手填 initials 时，优先从名称前两个字符中生成，保证表格和头像卡片不空。
     */
    private String buildInitials(String source) {
        if (source == null || source.isBlank()) {
            return "AD";
        }
        String normalized = source.trim();
        return normalized.length() <= 2 ? normalized.toUpperCase() : normalized.substring(0, 2).toUpperCase();
    }

    /**
     * 业务目的：在内容尚未补齐正文时，为富文本字段生成最小可展示 HTML。
     * 业务逻辑：确保 AiEditor 每次打开都有合法初始内容，避免空字符串导致编辑体验发散。
     */
    private String wrapParagraph(String text) {
        String safeText = defaultString(text, "请在此输入内容");
        return "<p>" + safeText + "</p>";
    }

    /**
     * 业务目的：封装后台趋势快照的内存结构，供仪表盘折线图读取。
     * 业务逻辑：每个快照只保留当前时刻关键指标，避免把整份仪表盘对象长期留在内存中。
     */
    private record DashboardSnapshot(
        LocalDateTime createdAt,
        Integer onlineUsers,
        Integer totalViews,
        Integer totalComments,
        Integer editsToday
    ) {
    }
}
