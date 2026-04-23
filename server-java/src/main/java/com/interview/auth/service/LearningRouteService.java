package com.interview.auth.service;

import com.interview.auth.domain.dto.response.LearningRouteResponse;
import java.util.List;

public interface LearningRouteService {

    /**
     * 业务说明：查询首页学习路线卡片列表。
     * 返回结果已经整理成前端可直接渲染的结构，包含封面、标题、发布时间、阅读/评论/点赞等展示字段。
     */
    List<LearningRouteResponse> listHomeRoutes();

    /**
     * 业务说明：根据路线 slug 查询学习路线详情。
     * 返回结果包含基础信息、阶段目录、统计数据和文章正文。
     */
    LearningRouteResponse getRouteDetail(String slug);
}
