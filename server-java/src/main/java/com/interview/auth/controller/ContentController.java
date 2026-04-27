package com.interview.auth.controller;

import com.interview.auth.common.ApiResponse;
import com.interview.auth.domain.dto.response.AiHotspotResponse;
import com.interview.auth.domain.dto.response.TechArticleResponse;
import com.interview.auth.domain.dto.response.WorldNewsIssueResponse;
import com.interview.auth.service.ContentService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 业务目的：对外暴露技术文章、看天下和 AI 热点三类内容接口。
 * 业务逻辑：Controller 只负责维持统一的 code / message / data 响应结构，具体查询与转换交给 Service。
 */
@RestController
@RequestMapping("/api/content")
@RequiredArgsConstructor
public class ContentController {

    private final ContentService contentService;

    /**
     * 业务目的：提供技术文章模块列表接口，服务文章频道页和顶部导航预览。
     * 业务逻辑：返回结果已经过后端字段转换，前端可以直接按现有结构渲染。
     */
    @GetMapping("/tech-articles")
    public ApiResponse<List<TechArticleResponse>> listTechArticles() {
        return ApiResponse.success(contentService.listTechArticles());
    }

    /**
     * 业务目的：提供看天下期刊列表接口，服务看天下频道页。
     * 业务逻辑：保持现有页面字段命名不变，降低前端切换后端数据源的改造量。
     */
    @GetMapping("/world-news")
    public ApiResponse<List<WorldNewsIssueResponse>> listWorldNewsIssues() {
        return ApiResponse.success(contentService.listWorldNewsIssues());
    }

    /**
     * 业务目的：提供 AI 热点列表接口，服务热点流与最新流切换。
     * 业务逻辑：推荐标记、发布时间和标签数组都在后端完成组织，前端只负责展示与筛选。
     */
    @GetMapping("/ai-hotspots")
    public ApiResponse<List<AiHotspotResponse>> listAiHotspots() {
        return ApiResponse.success(contentService.listAiHotspots());
    }
}
