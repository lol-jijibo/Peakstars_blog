package com.interview.auth.service;

import com.interview.auth.domain.dto.response.PageResult;
import com.interview.auth.domain.dto.response.TechArticleResponse;
import com.interview.auth.domain.dto.response.WorldNewsIssueResponse;
import com.interview.auth.domain.dto.response.WorldNewsSuggestionResponse;
import java.util.List;
import java.util.Map;

/**
 * 定义内容模块的对外契约，统一技术文章与资讯内容的查询入口。
 * 实现类按此接口编码，并按当前登录用户补齐最近阅读时间等个性化字段。
 */
public interface ContentService {

    /**
     * 返回技术文章列表，并补齐当前用户的最近阅读状态与阅读时间。
     * 未登录时只返回公共展示字段，已登录时额外合并用户维度的浏览记录。
     */
    List<TechArticleResponse> listTechArticles(Long currentUserId);

    /**
     * 获取看天下期刊首页列表数据。
     * 频道首页继续沿用统一字段结构，前端无需额外转换即可直接渲染。
     */
    List<WorldNewsIssueResponse> listWorldNewsIssues();

    /**
     * 按关键词分页检索看天下期刊。
     * 统一返回列表、总数和分页信息，便于频道搜索结果直接展示。
     */
    PageResult<WorldNewsIssueResponse> searchWorldNewsIssues(String keyword, int page, int size);

    /**
     * 获取看天下搜索联想词。
     * 后端聚合标题与封面文案候选，减少前端自行拼装候选结果的复杂度。
     */
    List<WorldNewsSuggestionResponse> suggestWorldNewsIssues(String keyword, int size);

    /**
     * 获取看天下榜单数据。
     * 按榜单类型切换不同排序规则，支撑频道页多个榜单模块复用。
     */
    List<WorldNewsIssueResponse> listWorldNewsRanking(String type, int page, int size);

    /**
     * 获取看天下热门推荐数据。
     * 热门区域复用统一期刊结构，前端只需裁剪成轻量卡片样式。
     */
    List<WorldNewsIssueResponse> listPopularWorldNews(int size);

    /**
     * 获取单条看天下期刊详情。
     * 返回完整正文和封面信息，供频道详情层按需展示。
     */
    WorldNewsIssueResponse getWorldNewsIssueDetail(String issueKey);

    /**
     * 为指定技术文章增加阅读量，并刷新当前用户的最近阅读时间。
     * 先原子更新文章阅读数，再按用户维度写入最后阅读时间供历史列表展示。
     */
    Map<String, Object> incrementArticleReadCount(String articleKey, Long currentUserId);

    /**
     * 切换当前用户对技术文章的点赞状态。
     * 已点赞时取消并扣减计数，未点赞时写入记录并递增计数。
     */
    Map<String, Object> toggleArticleLike(String articleKey, Long currentUserId);

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
