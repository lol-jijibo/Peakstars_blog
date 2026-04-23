package com.interview.auth.service.impl;

import com.interview.auth.common.BusinessException;
import com.interview.auth.domain.dto.response.LearningRouteResponse;
import com.interview.auth.domain.entity.LearningRoute;
import com.interview.auth.infrastructure.mapper.LearningRouteMapper;
import com.interview.auth.service.LearningRouteService;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LearningRouteServiceImpl implements LearningRouteService {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final LearningRouteMapper learningRouteMapper;

    /**
     * 业务说明：从 MySQL 查询已发布的学习路线，并转换成首页卡片 DTO。
     * Service 层会隐藏 status、sortOrder 等持久化字段，同时格式化发布时间和标签，降低前端处理复杂度。
     */
    @Override
    public List<LearningRouteResponse> listHomeRoutes() {
        return learningRouteMapper.findPublishedRoutes()
            .stream()
            .map(this::toResponse)
            .toList();
    }

    /**
     * 业务说明：按 slug 获取学习路线详情。
     * 如果路线不存在或未发布，抛出业务异常，让前端得到明确的错误提示。
     */
    @Override
    public LearningRouteResponse getRouteDetail(String slug) {
        LearningRoute route = learningRouteMapper.findPublishedBySlug(slug);
        if (route == null) {
            throw new BusinessException(404, "Learning route not found");
        }
        return toResponse(route);
    }

    /**
     * 业务说明：将数据库实体转换成前端响应对象。
     * 这里集中处理字段脱敏、时间格式化和 featured 布尔值转换，保证 Controller 返回结构稳定。
     */
    private LearningRouteResponse toResponse(LearningRoute route) {
        LearningRouteResponse response = new LearningRouteResponse();
        response.setId(route.getId());
        response.setSlug(route.getSlug());
        response.setRouteType(route.getRouteType());
        response.setTitle(route.getTitle());
        response.setSummary(route.getSummary());
        response.setCoverUrl(route.getCoverUrl());
        response.setContent(route.getContent());
        response.setStages(splitStages(route.getStageList()));
        response.setAuthor(route.getAuthor());
        response.setPublishedAt(route.getPublishedAt() == null ? null : route.getPublishedAt().format(DATE_TIME_FORMATTER));
        response.setEstimatedMinutes(route.getEstimatedMinutes());
        response.setViewCount(route.getViewCount());
        response.setCommentCount(route.getCommentCount());
        response.setLikeCount(route.getLikeCount());
        response.setStageCount(route.getStageCount());
        response.setDifficulty(route.getDifficulty());
        response.setTags(splitTags(route.getTags()));
        response.setFeatured(route.getFeatured() != null && route.getFeatured() == 1);
        return response;
    }

    /**
     * 业务说明：把 MySQL 中逗号分隔的标签字符串拆分为标签数组。
     * 前端可以直接循环渲染标签徽章，不需要关心数据库存储格式。
     */
    private List<String> splitTags(String tags) {
        if (tags == null || tags.isBlank()) {
            return List.of();
        }
        return Arrays.stream(tags.split(","))
            .map(String::trim)
            .filter(tag -> !tag.isEmpty())
            .toList();
    }

    /**
     * 业务说明：把 MySQL 中竖线分隔的阶段目录拆分为数组。
     * 详情页可以直接渲染路线阶段，不需要解析原始字符串。
     */
    private List<String> splitStages(String stageList) {
        if (stageList == null || stageList.isBlank()) {
            return List.of();
        }
        return Arrays.stream(stageList.split("\\|"))
            .map(String::trim)
            .filter(stage -> !stage.isEmpty())
            .toList();
    }
}
