package com.interview.auth.service;

import com.interview.auth.domain.dto.response.CategoryResponse;
import com.interview.auth.domain.dto.response.InterviewDetailResponse;
import com.interview.auth.domain.dto.response.InterviewListResponse;
import com.interview.auth.domain.dto.response.PageResult;
import java.util.List;
import java.util.Map;

/**
 * 对外聚合面经模块的查询与操作服务，包括面经列表/详情/点赞/收藏和分类查询。
 * Service 层负责字段转换和数组拆分，让 Controller 只关注 HTTP 请求参收和统一响应封装。
 */
public interface InterviewService {

    /**
     * 按分类、关键词和分页参数查询面经列表。
     * 后端完成标签字符串拆分和日期格式化，返回统一的分页结构。
     *
     * @param category 分类编码（all 表示全部）
     * @param keyword  搜索关键词
     * @param page     页码（从 1 开始）
     * @param pageSize 每页条数
     * @return 分页后的面经列表
     */
    PageResult<InterviewListResponse> listInterviews(String category, String tag, String keyword, int page, int pageSize);

    /**
     * 根据面经 ID 获取完整详情。
     * 包含正文内容和公司描述，同时异步增加浏览量。
     *
     * @param id 面经 ID
     * @return 面经详情，不存在时抛出 BusinessException
     */
    InterviewDetailResponse getInterviewDetail(Long id);

    /**
     * 为指定面经执行点赞操作。
     * 使用原子 UPDATE 自增点赞数，返回最新的点赞数。
     *
     * @param id 面经 ID
     * @return 包含最新点赞数的 Map（key: "likes"）
     */
    Map<String, Object> likeInterview(Long id);

    /**
     * 为指定面经执行收藏操作。
     * 使用原子 UPDATE 自增收藏数，返回最新的收藏数。
     *
     * @param id 面经 ID
     * @return 包含最新收藏数的 Map（key: "collects"）
     */
    Map<String, Object> collectInterview(Long id);

    /**
     * 获取面经分类列表，供前端导航和筛选使用。
     * 按 sort_order 升序排列返回。
     *
     * @return 分类列表
     */
    List<CategoryResponse> listCategories();

    /**
     * 根据分类代码查询该分类下所有面经使用过的标签列表。
     * 用于后台管理新增面经时，根据选择的分类动态加载可选标签。
     *
     * @param categoryCode 分类代码，如 frontend / java / agent
     * @return 标签名称列表（去重）
     */
    List<String> listTagsByCategory(String categoryCode);
}
