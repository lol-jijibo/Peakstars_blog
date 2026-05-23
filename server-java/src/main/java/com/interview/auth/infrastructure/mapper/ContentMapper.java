package com.interview.auth.infrastructure.mapper;

import com.interview.auth.domain.entity.TechArticle;
import com.interview.auth.domain.entity.WorldNewsIssue;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 定义内容模块的对外契约，统一技术文章与资讯内容的数据访问入口。
 * 实现类按此接口编码，并在 XML 中维护用户阅读记录与内容主表的关联查询。
 */
@Mapper
public interface ContentMapper {

    /**
     * 读取已发布技术文章列表。
     * 列表结果只包含文章公共字段，适合未携带用户身份的通用场景复用。
     */
    List<TechArticle> findPublishedTechArticles();

    /**
     * 读取已发布技术文章列表并合并当前用户的最近阅读时间。
     * 通过左连接用户阅读记录表补齐 inHistory 与 lastReadAt，供浏览记录页直接展示。
     */
    List<TechArticle> findPublishedTechArticlesByUser(@Param("userId") Long userId);

    /**
     * 读取已发布看天下期刊列表。
     * 首页默认按发布时间倒序展示，保证最新一期优先出现在前面。
     */
    List<WorldNewsIssue> findPublishedWorldNewsIssues();

    /**
     * 按关键词分页检索看天下期刊。
     * 同时匹配标题、封面文案和正文摘要，支撑频道页统一搜索入口。
     */
    List<WorldNewsIssue> searchWorldNewsIssues(
        @Param("keyword") String keyword,
        @Param("offset") int offset,
        @Param("size") int size
    );

    /**
     * 统计关键词检索命中的看天下期刊总数。
     * 总数字段与分页列表复用同一套匹配条件，避免分页器和结果列表口径不一致。
     */
    long countWorldNewsIssuesByKeyword(@Param("keyword") String keyword);

    /**
     * 读取看天下搜索框联想词。
     * 从标题和封面文案里提取候选内容，供搜索框下拉建议直接展示。
     */
    List<String> suggestWorldNewsKeywords(@Param("keyword") String keyword, @Param("size") int size);

    /**
     * 按榜单类型分页读取看天下期刊。
     * 排序表达式放在 XML 中按类型切换，保证各榜单口径长期稳定。
     */
    List<WorldNewsIssue> findWorldNewsRanking(
        @Param("type") String type,
        @Param("offset") int offset,
        @Param("size") int size
    );

    /**
     * 读取看天下热门推荐列表。
     * 热门区域优先使用今日阅读和推荐值排序，适合首页卡片区直接复用。
     */
    List<WorldNewsIssue> findPopularWorldNewsIssues(@Param("size") int size);

    /**
     * 根据期刊业务键读取单条看天下详情。
     * 返回完整正文和封面字段，供频道页详情浮层按需展示。
     */
    WorldNewsIssue findWorldNewsIssueByKey(@Param("issueKey") String issueKey);

    /**
     * 原子增加技术文章的阅读数。
     * 详情页打开时直接更新文章主表，避免并发场景下出现覆盖写入。
     */
    int incrementReadCount(@Param("articleKey") String articleKey);

    /**
     * 根据文章主键读取最新阅读数。
     * 阅读数更新后立即回查结果，方便前端无刷新同步数字。
     */
    Integer findReadCount(@Param("articleKey") String articleKey);

    /**
     * 查询指定用户是否已点赞技术文章。
     * 通过用户和文章联合唯一键读取记录数量，供详情页按钮状态回显。
     */
    int countArticleLikeByUser(@Param("articleKey") String articleKey, @Param("userId") Long userId);

    /**
     * 写入当前用户对技术文章的点赞记录。
     * 使用唯一键约束避免重复点赞，返回影响行数决定是否递增文章点赞数。
     */
    int insertArticleLike(@Param("articleKey") String articleKey, @Param("userId") Long userId);

    /**
     * 删除当前用户对技术文章的点赞记录。
     * 取消点赞时只删除用户维度关系，返回影响行数决定是否递减文章点赞数。
     */
    int deleteArticleLike(@Param("articleKey") String articleKey, @Param("userId") Long userId);

    /**
     * 原子递增技术文章点赞数。
     * 用户首次点赞成功后回写文章主表计数，保持列表和详情页展示一致。
     */
    int incrementLikeCount(@Param("articleKey") String articleKey);

    /**
     * 原子递减技术文章点赞数。
     * 用户取消点赞成功后回写文章主表计数，并限制结果不小于零。
     */
    int decrementLikeCount(@Param("articleKey") String articleKey);

    /**
     * 查询技术文章当前点赞数。
     * 点赞或取消点赞后立即回查主表数值，供前端同步更新按钮和统计区。
     */
    Integer findLikeCount(@Param("articleKey") String articleKey);

    /**
     * 写入当前用户的技术文章阅读记录。
     * 首次阅读时插入新记录，重复阅读时刷新最后阅读时间并累计阅读次数。
     */
    int upsertArticleReadHistory(@Param("userId") Long userId, @Param("articleKey") String articleKey);

    /**
     * 新增一条技术文章评论。
     * 评论内容和头像展示信息一起落库，前端可以直接回填最新评论。
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
     * 原子增加技术文章的评论数。
     * 新评论写入后同步回写统计值，避免后台和详情页展示不一致。
     */
    int incrementCommentCount(@Param("articleKey") String articleKey);

    /**
     * 查询指定文章的评论列表。
     * 结果按创建时间正序返回，方便前端直接组装楼层和回复关系。
     */
    List<Map<String, Object>> findArticleComments(@Param("articleKey") String articleKey);

    /**
     * 软删除一条评论记录。
     * 只修改状态字段保留操作痕迹，后续仍可用于审计和统计回收。
     */
    int softDeleteComment(@Param("commentId") Long commentId);

    /**
     * 原子递减技术文章的评论数。
     * 删除评论后同步下调统计值，并限制结果不小于零。
     */
    int decrementCommentCount(@Param("articleKey") String articleKey);

    /**
     * 根据评论 ID 查询所属文章主键。
     * 删除评论前先定位文章，用来同步回收文章上的评论统计值。
     */
    String findArticleKeyByCommentId(@Param("commentId") Long commentId);

    /**
     * 把指定文章标记为浏览历史。
     * 只在首次阅读时更新状态，减少重复写库造成的无意义变更。
     */
    int markArticleInHistory(@Param("articleKey") String articleKey);
}
