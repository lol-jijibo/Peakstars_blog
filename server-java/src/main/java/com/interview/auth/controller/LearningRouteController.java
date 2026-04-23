package com.interview.auth.controller;

import com.interview.auth.common.ApiResponse;
import com.interview.auth.domain.dto.response.LearningRouteResponse;
import com.interview.auth.service.LearningRouteService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/learning-routes")
@RequiredArgsConstructor
public class LearningRouteController {

    private final LearningRouteService learningRouteService;

    /**
     * 业务说明：为首页学习路线模块提供已发布的 Java / 全栈路线卡片数据。
     * Controller 只负责组织 HTTP 响应，具体查询、转换和排序逻辑交给 Service 层处理。
     */
    @GetMapping
    public ApiResponse<List<LearningRouteResponse>> listHomeRoutes() {
        return ApiResponse.success(learningRouteService.listHomeRoutes());
    }

    /**
     * 业务说明：根据 slug 查询学习路线详情。
     * 详情页会展示路线信息、阶段目录、统计数据和文章正文。
     */
    @GetMapping("/{slug}")
    public ApiResponse<LearningRouteResponse> getRouteDetail(@PathVariable String slug) {
        return ApiResponse.success(learningRouteService.getRouteDetail(slug));
    }
}
