package com.interview.auth.controller;

import com.interview.auth.common.ApiResponse;
import com.interview.auth.domain.dto.response.CategoryResponse;
import com.interview.auth.service.InterviewService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 业务目的：对外暴露面经分类接口，供前端分类导航和筛选下拉使用。
 * 业务逻辑：该接口独立于 /api/interviews 前缀，直接挂载在 /api/categories 路径下，
 *          与原 Node.js Express 后端的 /api/categories 路径完全对齐，前端无需修改请求路径。
 */
@RestController
@RequiredArgsConstructor
public class CategoryController {

    private final InterviewService interviewService;

    /**
     * 业务目的：获取面经分类列表，供前端导航和列表筛选使用。
     * 业务逻辑：数据按 sort_order 升序排列，返回 id / code / name 三个字段与现有前端对齐。
     *
     * @return 分类列表
     */
    @GetMapping("/api/categories")
    public ApiResponse<List<CategoryResponse>> listCategories() {
        return ApiResponse.success(interviewService.listCategories());
    }
}
