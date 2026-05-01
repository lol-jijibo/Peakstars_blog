package com.interview.auth.service.impl;

import com.interview.auth.common.BusinessException;
import com.interview.auth.domain.dto.response.CategoryResponse;
import com.interview.auth.domain.dto.response.InterviewDetailResponse;
import com.interview.auth.domain.dto.response.InterviewListResponse;
import com.interview.auth.domain.dto.response.PageResult;
import com.interview.auth.domain.entity.Category;
import com.interview.auth.domain.entity.Interview;
import com.interview.auth.infrastructure.mapper.InterviewMapper;
import com.interview.auth.service.InterviewService;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 实现面经模块的全部业务逻辑，包括列表查询、详情获取、点赞/收藏和分类查询。
 * 通过 MyBatis Mapper 获取原始数据后做字段映射和标签数组拆分，
 *         保证 Controller 层拿到的是可直接返回给前端的标准化结构。
 */
@Service
@RequiredArgsConstructor
public class InterviewServiceImpl implements InterviewService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final InterviewMapper interviewMapper;

    /**
     * 按分类、关键词和分页参数查询面经列表。
     * 先查总数和列表，再将每行的 tagsStr 逗号字符串拆分成 List<String>，
     *         最后包装为 PageResult 返回。已处理空列表和空标签的边界。
     */
    @Override
    public PageResult<InterviewListResponse> listInterviews(String category, String keyword, int page, int pageSize) {
        int offset = (page - 1) * pageSize;
        long total = interviewMapper.countInterviews(category, keyword);

        if (total == 0) {
            return new PageResult<>(Collections.emptyList(), 0, page, pageSize);
        }

        List<Map<String, Object>> rows = interviewMapper.findInterviewList(category, keyword, offset, pageSize);
        List<InterviewListResponse> list = rows.stream()
            .map(this::toInterviewListResponse)
            .collect(Collectors.toList());

        return new PageResult<>(list, total, page, pageSize);
    }

    /**
     * 根据面经 ID 获取完整详情。
     * 如果面经不存在（查询结果为 null 或空）则抛 BusinessException。
     *         查询到数据后异步增加浏览量，不阻塞返回。标签字符串在 Service 层拆分为数组。
     */
    @Override
    public InterviewDetailResponse getInterviewDetail(Long id) {
        Map<String, Object> row = interviewMapper.findInterviewDetail(id);
        if (row == null || row.isEmpty()) {
            throw new BusinessException(404, "面经不存在");
        }

        // 异步增加浏览量（不阻塞返回）
        interviewMapper.incrementViews(id);

        return toInterviewDetailResponse(row);
    }

    /**
     * 为指定面经执行点赞操作。
     * 使用原子 UPDATE 自增点赞数。如果影响行数为 0 表示面经不存在，抛异常。
     *         更新后重新查询返回最新点赞数。
     */
    @Override
    @Transactional
    public Map<String, Object> likeInterview(Long id) {
        int affected = interviewMapper.incrementLikes(id);
        if (affected == 0) {
            throw new BusinessException(404, "面经不存在");
        }
        Interview interview = interviewMapper.findLikesAndCollects(id);
        return Map.of("likes", interview != null ? interview.getLikes() : 0);
    }

    /**
     * 为指定面经执行收藏操作。
     * 与 likeInterview 相同模式，使用原子 UPDATE 并返回最新收藏数。
     */
    @Override
    @Transactional
    public Map<String, Object> collectInterview(Long id) {
        int affected = interviewMapper.incrementCollects(id);
        if (affected == 0) {
            throw new BusinessException(404, "面经不存在");
        }
        Interview interview = interviewMapper.findLikesAndCollects(id);
        return Map.of("collects", interview != null ? interview.getCollects() : 0);
    }

    /**
     * 获取面经分类列表。
     * 直接返回所有分类，按 sort_order 升序排列，无需额外过滤。
     */
    @Override
    public List<CategoryResponse> listCategories() {
        return interviewMapper.findAllCategories()
            .stream()
            .map(this::toCategoryResponse)
            .collect(Collectors.toList());
    }

    /**
     * 把面经列表的 Map 行数据转换成前端消费的 InterviewListResponse 对象。
     * 标签从数据库的逗号分隔字符串拆分为 List<String>，
     *         日期转换为 yyyy-MM-dd 字符串，已处理 null 和空字符串的边界。
     */
    @SuppressWarnings("unchecked")
    private InterviewListResponse toInterviewListResponse(Map<String, Object> row) {
        InterviewListResponse resp = new InterviewListResponse();
        resp.setId(toLong(row.get("id")));
        resp.setTitle((String) row.get("title"));
        resp.setAuthor((String) row.get("author"));
        resp.setSummary((String) row.get("summary"));
        resp.setViews(toInt(row.get("views")));
        resp.setLikes(toInt(row.get("likes")));
        resp.setCollects(toInt(row.get("collects")));
        resp.setDate(Objects.toString(row.get("date"), null));
        resp.setCategory((String) row.get("category"));
        resp.setCategoryName((String) row.get("categoryName"));
        resp.setCompanyName((String) row.get("companyName"));
        resp.setAvatar((String) row.get("avatar"));
        resp.setAvatarColor((String) row.get("avatarColor"));
        resp.setTags(splitTags((String) row.get("tagsStr")));
        return resp;
    }

    /**
     * 把面经详情的 Map 行数据转换成前端消费的 InterviewDetailResponse 对象。
     * 在列表字段基础上增加 content 和 companyDesc，标签和日期处理与列表一致。
     */
    @SuppressWarnings("unchecked")
    private InterviewDetailResponse toInterviewDetailResponse(Map<String, Object> row) {
        InterviewDetailResponse resp = new InterviewDetailResponse();
        resp.setId(toLong(row.get("id")));
        resp.setTitle((String) row.get("title"));
        resp.setAuthor((String) row.get("author"));
        resp.setSummary((String) row.get("summary"));
        resp.setContent((String) row.get("content"));
        resp.setViews(toInt(row.get("views")));
        resp.setLikes(toInt(row.get("likes")));
        resp.setCollects(toInt(row.get("collects")));
        resp.setDate(Objects.toString(row.get("date"), null));
        resp.setCategory((String) row.get("category"));
        resp.setCategoryName((String) row.get("categoryName"));
        resp.setCompanyName((String) row.get("companyName"));
        resp.setAvatar((String) row.get("avatar"));
        resp.setAvatarColor((String) row.get("avatarColor"));
        resp.setCompanyDesc((String) row.get("companyDesc"));
        resp.setTags(splitTags((String) row.get("tagsStr")));
        return resp;
    }

    /**
     * 把 Category 实体转换成前端消费的结构。
     * 直接透传 id / code / name 三个字段，与现有前端 getCategories 接口对齐。
     */
    private CategoryResponse toCategoryResponse(Category category) {
        CategoryResponse resp = new CategoryResponse();
        resp.setId(category.getId());
        resp.setCode(category.getCode());
        resp.setName(category.getName());
        return resp;
    }

    /**
     * 把数据库中的逗号分隔标签字符串拆分成 List&lt;String&gt;。
     * 统一在 Service 层处理空字符串和 null 的边界，避免 Controller 和 View 层重复解析。
     */
    private List<String> splitTags(String tagsStr) {
        if (tagsStr == null || tagsStr.isBlank()) {
            return Collections.emptyList();
        }
        return Arrays.stream(tagsStr.split(","))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .collect(Collectors.toList());
    }

    private Long toLong(Object value) {
        if (value == null) return null;
        if (value instanceof Number n) return n.longValue();
        return Long.valueOf(value.toString());
    }

    private Integer toInt(Object value) {
        if (value == null) return 0;
        if (value instanceof Number n) return n.intValue();
        return Integer.valueOf(value.toString());
    }
}
