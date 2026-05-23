package com.interview.auth.controller;

import com.interview.auth.common.ApiResponse;
import com.interview.auth.domain.dto.response.PageResult;
import com.interview.auth.domain.dto.response.TechArticleResponse;
import com.interview.auth.domain.dto.response.WorldNewsIssueResponse;
import com.interview.auth.domain.dto.response.WorldNewsSuggestionResponse;
import com.interview.auth.service.ContentService;
import com.interview.auth.service.TokenService;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 对外暴露技术文章和看天下两类内容接口。
 * Controller 负责识别当前登录用户，并把用户身份传给 Service 组装个性化阅读信息。
 */
@RestController
@RequestMapping("/api/content")
@RequiredArgsConstructor
public class ContentController {

    private final ContentService contentService;
    private final TokenService tokenService;

    /**
     * 提供技术文章模块列表接口。
     * 根据当前登录用户合并最近阅读时间，让浏览记录页直接展示真实阅读时刻。
     */
    @GetMapping("/tech-articles")
    public ApiResponse<List<TechArticleResponse>> listTechArticles(
        @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        return ApiResponse.success(contentService.listTechArticles(resolveCurrentUserId(authorization)));
    }

    /**
     * 提供看天下首页期刊列表接口。
     * 首页初始渲染继续沿用原有字段命名，减少前端切换成本。
     */
    @GetMapping("/world-news")
    public ApiResponse<List<WorldNewsIssueResponse>> listWorldNewsIssues() {
        return ApiResponse.success(contentService.listWorldNewsIssues());
    }

    /**
     * 提供看天下搜索结果接口。
     * 根据关键词返回分页列表，供频道页搜索模式直接展示。
     */
    @GetMapping("/world-news/search")
    public ApiResponse<PageResult<WorldNewsIssueResponse>> searchWorldNewsIssues(
        @RequestParam(defaultValue = "") String q,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.success(contentService.searchWorldNewsIssues(q, page, size));
    }

    /**
     * 提供看天下搜索联想接口。
     * 返回精简联想结构，供搜索框下拉建议实时展示。
     */
    @GetMapping("/world-news/suggest")
    public ApiResponse<List<WorldNewsSuggestionResponse>> suggestWorldNewsIssues(
        @RequestParam(defaultValue = "") String q,
        @RequestParam(defaultValue = "8") int size
    ) {
        return ApiResponse.success(contentService.suggestWorldNewsIssues(q, size));
    }

    /**
     * 提供看天下榜单接口。
     * 按榜单类型切换排序方式，复用到频道页多个榜单卡片区域。
     */
    @GetMapping("/world-news/ranking")
    public ApiResponse<List<WorldNewsIssueResponse>> listWorldNewsRanking(
        @RequestParam(defaultValue = "rising") String type,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "6") int size
    ) {
        return ApiResponse.success(contentService.listWorldNewsRanking(type, page, size));
    }

    /**
     * 提供看天下热门推荐接口。
     * 热门区域只拿轻量列表数据，减少首页重复请求和前端筛选成本。
     */
    @GetMapping("/world-news/popular")
    public ApiResponse<List<WorldNewsIssueResponse>> listPopularWorldNews(@RequestParam(defaultValue = "4") int size) {
        return ApiResponse.success(contentService.listPopularWorldNews(size));
    }

    /**
     * 提供看天下单期详情接口。
     * 返回完整正文和封面文案，供频道页详情浮层按需加载。
     */
    @GetMapping("/world-news/{issueKey}")
    public ApiResponse<WorldNewsIssueResponse> getWorldNewsIssueDetail(@PathVariable String issueKey) {
        return ApiResponse.success(contentService.getWorldNewsIssueDetail(issueKey));
    }


    /**
     * 为指定技术文章增加阅读量。
     * 用户打开详情页时同时刷新个人最近阅读时间，返回最新阅读数用于页面同步。
     */
    @PostMapping("/tech-articles/{articleKey}/read")
    public ApiResponse<Map<String, Object>> incrementReadCount(
        @PathVariable String articleKey,
        @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        return ApiResponse.success(contentService.incrementArticleReadCount(articleKey, resolveCurrentUserId(authorization)));
    }

    /**
     * 切换指定技术文章的点赞状态。
     * 根据当前登录用户新增或取消点赞记录，并返回最新点赞状态和计数。
     */
    @PostMapping("/tech-articles/{articleKey}/like")
    public ApiResponse<Map<String, Object>> toggleArticleLike(
        @PathVariable String articleKey,
        @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        return ApiResponse.success(contentService.toggleArticleLike(articleKey, resolveCurrentUserId(authorization)));
    }

    /**
     * 获取指定技术文章的评论列表。
     * 评论结果按时间正序返回，方便前端直接构建楼层关系。
     */
    @GetMapping("/tech-articles/{articleKey}/comments")
    public ApiResponse<List<Map<String, Object>>> listArticleComments(@PathVariable String articleKey) {
        return ApiResponse.success(contentService.listArticleComments(articleKey));
    }

    /**
     * 为指定技术文章新增评论。
     * 新评论写入成功后同步回写文章统计值，并把最新评论返回给前端。
     */
    @PostMapping("/tech-articles/{articleKey}/comments")
    public ApiResponse<Map<String, Object>> addArticleComment(@PathVariable String articleKey, @RequestBody Map<String, Object> body) {
        String nickname = (String) body.getOrDefault("nickname", "匿名用户");
        String content = (String) body.get("content");
        String avatarText = (String) body.getOrDefault("avatarText", "匿");
        String avatarAccent = (String) body.getOrDefault("avatarAccent", "linear-gradient(135deg, #667eea 0%, #764ba2 100%)");
        Long parentId = body.get("parentId") != null ? Long.valueOf(body.get("parentId").toString()) : null;
        return ApiResponse.success(contentService.addArticleComment(articleKey, nickname, content, avatarText, avatarAccent, parentId));
    }

    /**
     * 删除指定评论并回收文章上的评论计数。
     * 需要登录后才能执行，删除动作只改状态字段。
     */
    @DeleteMapping("/comments/{commentId}")
    public ApiResponse<Map<String, Object>> deleteArticleComment(
        @PathVariable Long commentId,
        @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        Long currentUserId = resolveCurrentUserId(authorization);
        if (currentUserId == null) {
            return ApiResponse.fail(401, "请先登录后再删除评论");
        }
        boolean deleted = contentService.deleteArticleComment(commentId);
        Map<String, Object> result = new HashMap<>();
        result.put("deleted", deleted);
        return ApiResponse.success(result);
    }

    /**
     * 解析当前请求里的用户身份。
     * 读取 Bearer Token 中的 userId，未登录时返回空值供内容接口走公共分支。
     */
    private Long resolveCurrentUserId(String authorization) {
        if (authorization == null || authorization.isBlank()) {
            return null;
        }
        Map<String, Object> payload = tokenService.parseToken(authorization);
        return Long.valueOf(String.valueOf(payload.get("userId")));
    }
}
