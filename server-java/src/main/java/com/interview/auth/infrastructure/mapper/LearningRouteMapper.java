package com.interview.auth.infrastructure.mapper;

import com.interview.auth.domain.entity.LearningRoute;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * Data access layer for learning route cards.
 */
@Mapper
public interface LearningRouteMapper {

    /**
     * 业务说明：按首页展示顺序查询已发布的学习路线。
     * 排序规则在 XML 中维护，便于后续按 featured、sortOrder、发布时间继续扩展。
     */
    List<LearningRoute> findPublishedRoutes();

    /**
     * 业务说明：按 slug 查询单条已发布学习路线详情。
     * 详情页依赖该查询读取正文、阶段目录和统计数据。
     */
    LearningRoute findPublishedBySlug(@Param("slug") String slug);
}
