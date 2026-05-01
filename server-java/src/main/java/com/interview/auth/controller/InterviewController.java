package com.interview.auth.controller;

import com.interview.auth.common.ApiResponse;
import com.interview.auth.domain.dto.response.InterviewDetailResponse;
import com.interview.auth.domain.dto.response.InterviewListResponse;
import com.interview.auth.domain.dto.response.PageResult;
import com.interview.auth.service.InterviewService;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 业务目的：对外暴露面经模块的全部接口，包括面经列表/详情/点赞/收藏和分类查询。
 * 业务逻辑：Controller 只负责 HTTP 层参数接收和统一响应包装，具体查询与转换交给 InterviewService。
 *          面经模块以前由 Node.js Express 服务承载，迁移到 Java 后端后接口路径和返回结构保持完全一致，
 *          前端只需修改 BASE_URL 即可无痛切换。
 */
@RestController
@RequestMapping("/api/interviews")
@RequiredArgsConstructor
public class InterviewController {

    private final InterviewService interviewService;

    /**
     * 业务目的：提供面经列表接口，支持分类筛选、关键词搜索和分页。
     * 业务逻辑：category 传 all 或不传表示不过滤，keyword 可空，page 和 pageSize 有默认值。
     *          返回统一的分页结构 { list, total, page, pageSize }，兼容现有前端 getInterviews 消费逻辑。
     *
     * @param category 分类编码（all / frontend / java）
     * @param keyword  搜索关键词
     * @param page     页码（默认 1）
     * @param pageSize 每页条数（默认 20）
     * @return 分页面经列表
     */
    @GetMapping
    public ApiResponse<PageResult<InterviewListResponse>> listInterviews(
        @RequestParam(defaultValue = "all") String category,
        @RequestParam(defaultValue = "") String keyword,
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "20") int pageSize
    ) {
        return ApiResponse.success(interviewService.listInterviews(category, keyword, page, pageSize));
    }

    /**
     * 业务目的：提供面经详情接口，包含正文内容和公司描述。
     * 业务逻辑：路径参数 id 为面经主键，不存在时返回 404 错误响应。
     *          调用后台时会自动异步增加该面经的浏览量。
     *
     * @param id 面经 ID
     * @return 面经详情
     */
    @GetMapping("/{id}")
    public ApiResponse<InterviewDetailResponse> getInterviewDetail(@PathVariable Long id) {
        return ApiResponse.success(interviewService.getInterviewDetail(id));
    }

    /**
     * 业务目的：提供面经点赞接口，自增点赞数并返回最新值。
     * 业务逻辑：使用原子递增保证并发安全，返回 { likes: number } 与现有前端 likeInterview 消费逻辑一致。
     *
     * @param id 面经 ID
     * @return 最新点赞数
     */
    @PostMapping("/{id}/like")
    public ApiResponse<Map<String, Object>> likeInterview(@PathVariable Long id) {
        return ApiResponse.success(interviewService.likeInterview(id));
    }

    /**
     * 业务目的：提供面经收藏接口，自增收藏数并返回最新值。
     * 业务逻辑：使用原子递增保证并发安全，返回 { collects: number } 与现有前端 collectInterview 消费逻辑一致。
     *
     * @param id 面经 ID
     * @return 最新收藏数
     */
    @PostMapping("/{id}/collect")
    public ApiResponse<Map<String, Object>> collectInterview(@PathVariable Long id) {
        return ApiResponse.success(interviewService.collectInterview(id));
    }

}
