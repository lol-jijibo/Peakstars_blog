package com.interview.auth.controller;

import com.interview.auth.common.ApiResponse;
import com.interview.auth.domain.dto.response.StarReadHomeResponse;
import com.interview.auth.domain.dto.response.StarReadSearchResponse;
import com.interview.auth.domain.dto.response.StarReadSearchSuggestionResponse;
import com.interview.auth.service.StarReadService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 对外暴露 star_read 首页与搜索模块的聚合接口。
 * 控制器只负责参数接收与统一返回结构，页面所需数据全部交由服务层组装。
 */
@RestController
@RequestMapping("/api/star-read")
@RequiredArgsConstructor
public class StarReadController {

    private final StarReadService starReadService;

    /**
     * 返回 star_read 首页需要的品牌区、榜单区和分类区数据。
     * 页面首屏一次请求即可完成主要模块渲染，减少前端拼装成本。
     */
    @GetMapping("/home")
    public ApiResponse<StarReadHomeResponse> getHomeData() {
        return ApiResponse.success(starReadService.getHomeData());
    }

    /**
     * 返回 star_read 搜索结果列表与分页信息。
     * 服务层优先走 Elasticsearch 检索并在异常时自动降级为本地搜索结果。
     */
    @GetMapping("/search")
    public ApiResponse<StarReadSearchResponse> searchBooks(
        @RequestParam(defaultValue = "") String q,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "12") int size
    ) {
        return ApiResponse.success(starReadService.searchBooks(q, page, size));
    }

    /**
     * 返回 star_read 搜索框的下拉建议词列表。
     * 建议词数量由服务层限制，前端可直接用于输入框联想展示。
     */
    @GetMapping("/suggest")
    public ApiResponse<List<StarReadSearchSuggestionResponse>> suggestBooks(
        @RequestParam(defaultValue = "") String q,
        @RequestParam(defaultValue = "8") int size
    ) {
        return ApiResponse.success(starReadService.suggestBooks(q, size));
    }
}
