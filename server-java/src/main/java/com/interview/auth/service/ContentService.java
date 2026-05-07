package com.interview.auth.service;

import com.interview.auth.domain.dto.response.AiHotspotResponse;
import com.interview.auth.domain.dto.response.TechArticleResponse;
import com.interview.auth.domain.dto.response.WorldNewsIssueResponse;
import java.util.List;
import java.util.Map;

/**
 * 对外聚合技术文章、看天下和 AI 热点三类内容服务。
 * Service 层负责做字段转换和数组拆分，让 Controller 只关注 HTTP 返回结构。
 */
public interface ContentService {

    /**
     * 获取技术文章模块列表数据。
     * 返回结果兼容现有前端字段命名，便于页面无痛从静态数据切换到后端接口。
     */
    List<TechArticleResponse> listTechArticles();

    /**
     * 获取看天下期刊列表数据。
     * 返回结果按频道页现有结构组织，前端直接遍历即可渲染。
     */
    List<WorldNewsIssueResponse> listWorldNewsIssues();

    /**
     * 获取 AI 热点列表数据。
     * 保留推荐标记、发布时间和标签数组，方便前端做推荐 / 最新两种切换。
     */
    List<AiHotspotResponse> listAiHotspots();

    /**
     * 为指定技术文章增加阅读量。
     * 使用原子 UPDATE 自增 read_count，返回最新阅读数。
     *
     * @param articleKey 文章业务主键
     * @return 包含最新阅读数的 Map（key: "readCount"）
     */
    Map<String, Object> incrementArticleReadCount(String articleKey);

    /**
     * 新增一条技术文章评论，同时自增文章的 comment_count。
     * 返回新增评论的完整数据。
     *
     * @param articleKey  文章业务主键
     * @param nickname    评论者昵称
     * @param content     评论内容
     * @param avatarText  头像文字
     * @param avatarAccent 头像背景渐变
     * @param parentId    父评论ID（null表示顶级评论）
     * @return 包含评论数据的 Map
     */
    Map<String, Object> addArticleComment(String articleKey, String nickname, String content, String avatarText, String avatarAccent, Long parentId);

    /**
     * 获取指定技术文章的评论列表。
     *
     * @param articleKey 文章业务主键
     * @return 评论列表
     */
    List<Map<String, Object>> listArticleComments(String articleKey);
}
