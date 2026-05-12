package com.interview.auth.service;

import com.interview.auth.domain.dto.response.AiHotspotResponse;
import com.interview.auth.domain.dto.response.PageResult;
import com.interview.auth.domain.dto.response.TechArticleResponse;
import com.interview.auth.domain.dto.response.WorldNewsIssueResponse;
import com.interview.auth.domain.dto.response.WorldNewsSuggestionResponse;
import java.util.List;
import java.util.Map;

/**
 * 对外聚合技术文章、看天下和 AI 热点三类内容服务。
 * Service 层统一完成字段转换、分页装配和榜单整理，Controller 只负责暴露接口。
 */
public interface ContentService {

    /**
     * 获取技术文章模块列表数据。
     * 返回结构兼容现有前端字段命名，页面可以直接替换静态数据源。
     */
    List<TechArticleResponse> listTechArticles();

    /**
     * 获取看天下期刊首页列表数据。
     * 频道首页继续沿用同一套字段结构，前端无需再做字段重命名。
     */
    List<WorldNewsIssueResponse> listWorldNewsIssues();

    /**
     * 按关键词分页检索看天下期刊。
     * 统一返回列表、总数和页码信息，方便频道页搜索模式直接接入。
     */
    PageResult<WorldNewsIssueResponse> searchWorldNewsIssues(String keyword, int page, int size);

    /**
     * 获取看天下搜索联想词。
     * 后端合并标题与封面文案候选，减少前端自行拼接文案规则。
     */
    List<WorldNewsSuggestionResponse> suggestWorldNewsIssues(String keyword, int size);

    /**
     * 获取看天下榜单数据。
     * 按榜单类型切换不同排序规则，支撑频道页四块榜单卡片展示。
     */
    List<WorldNewsIssueResponse> listWorldNewsRanking(String type, int page, int size);

    /**
     * 获取看天下热门推荐数据。
     * 热门区复用统一期刊结构，前端只需裁剪成更轻量的卡片样式。
     */
    List<WorldNewsIssueResponse> listPopularWorldNews(int size);

    /**
     * 获取单条看天下期刊详情。
     * 返回完整正文和封面信息，供频道页详情浮层直接展示。
     */
    WorldNewsIssueResponse getWorldNewsIssueDetail(String issueKey);

    /**
     * 获取 AI 热点列表数据。
     * 推荐标记、发布时间和标签数组都在后端统一整理后返回给前端。
     */
    List<AiHotspotResponse> listAiHotspots();

    /**
     * 为指定技术文章增加阅读量。
     * 使用原子更新后再回查最新值，保证详情页数字刷新稳定。
     */
    Map<String, Object> incrementArticleReadCount(String articleKey);

    /**
     * 新增一条技术文章评论并回写统计数。
     * 返回最新落库的评论结构，便于前端无刷新插入评论列表。
     */
    Map<String, Object> addArticleComment(String articleKey, String nickname, String content, String avatarText, String avatarAccent, Long parentId);

    /**
     * 获取指定技术文章的评论列表。
     * 统一补齐父评论字段，方便前端直接组装嵌套回复结构。
     */
    List<Map<String, Object>> listArticleComments(String articleKey);

    /**
     * 删除指定评论并同步回收评论计数。
     * 先定位评论所属文章，再执行软删除和文章统计回写。
     */
    boolean deleteArticleComment(Long commentId);
}
