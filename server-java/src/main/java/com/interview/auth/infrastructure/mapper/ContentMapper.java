package com.interview.auth.infrastructure.mapper;

import com.interview.auth.domain.entity.AiHotspot;
import com.interview.auth.domain.entity.TechArticle;
import com.interview.auth.domain.entity.WorldNewsIssue;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 统一承接内容模块的数据库查询，避免为三个频道拆成多套相似的数据访问代码。
 * 查询层只返回已发布内容，排序规则固定在 XML，保证前端不同页面拿到一致的数据顺序。
 */
@Mapper
public interface ContentMapper {

    /**
     * 读取已发布技术文章列表，服务文章页、导航下拉和 AI 频道榜单。
     * 排序优先级由 XML 维护，确保精选内容始终优先展示。
     */
    List<TechArticle> findPublishedTechArticles();

    /**
     * 读取已发布看天下期刊列表，服务频道首页展示。
     * 按发布时间倒序返回，让最新期刊默认出现在最前面。
     */
    List<WorldNewsIssue> findPublishedWorldNewsIssues();

    /**
     * 读取已发布 AI 热点列表，服务推荐流与最新流切换。
     * 基础列表按业务排序返回，前端可继续按热度或发布时间切换视图。
     */
    List<AiHotspot> findPublishedAiHotspots();

    /**
     * 原子自增技术文章的阅读数，在详情页请求时触发。
     * 使用原子 UPDATE 保证并发安全，不阻塞主查询返回。
     *
     * @param articleKey 文章业务主键
     * @return 影响行数（0 表示文章不存在）
     */
    int incrementReadCount(@Param("articleKey") String articleKey);

    /**
     * 根据 article_key 查询技术文章的阅读数，供阅读量递增后返回最新值。
     *
     * @param articleKey 文章业务主键
     * @return 最新阅读数
     */
    Integer findReadCount(@Param("articleKey") String articleKey);

    /**
     * 新增一条技术文章评论。
     *
     * @param articleKey 文章业务主键
     * @param nickname   评论者昵称
     * @param content    评论内容
     * @param avatarText 头像文字
     * @param avatarAccent 头像背景渐变
     * @param parentId   父评论ID（null表示顶级评论）
     * @return 影响行数
     */
    int insertArticleComment(
        @Param("articleKey") String articleKey,
        @Param("nickname") String nickname,
        @Param("content") String content,
        @Param("avatarText") String avatarText,
        @Param("avatarAccent") String avatarAccent,
        @Param("parentId") Long parentId
    );

    /**
     * 原子自增技术文章的评论数。
     *
     * @param articleKey 文章业务主键
     * @return 影响行数
     */
    int incrementCommentCount(@Param("articleKey") String articleKey);

    /**
     * 查询指定文章的评论列表，按创建时间正序排列。
     *
     * @param articleKey 文章业务主键
     * @return 评论列表
     */
    List<Map<String, Object>> findArticleComments(@Param("articleKey") String articleKey);

    /**
     * 软删除评论（将 status 置为 -1），只有评论存在且状态正常时才执行。
     *
     * @param commentId 评论主键ID
     * @return 影响行数
     */
    int softDeleteComment(@Param("commentId") Long commentId);

    /**
     * 原子递减技术文章的评论数。
     *
     * @param articleKey 文章业务主键
     * @return 影响行数
     */
    int decrementCommentCount(@Param("articleKey") String articleKey);

    /**
     * 根据评论ID查询评论所属的文章业务主键。
     *
     * @param commentId 评论主键ID
     * @return 文章业务主键
     */
    String findArticleKeyByCommentId(@Param("commentId") Long commentId);

    /**
     * 将指定文章标记为浏览历史（幂等操作）。
     * 仅在 in_history = 0 时更新为 1，避免重复标记。
     *
     * @param articleKey 文章业务主键
     * @return 影响行数（0 表示已标记或文章不存在）
     */
    int markArticleInHistory(@Param("articleKey") String articleKey);
}
