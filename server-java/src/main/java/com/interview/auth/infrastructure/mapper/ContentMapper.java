package com.interview.auth.infrastructure.mapper;

import com.interview.auth.domain.entity.AiHotspot;
import com.interview.auth.domain.entity.TechArticle;
import com.interview.auth.domain.entity.WorldNewsIssue;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

/**
 * 业务目的：统一承接内容模块的数据库查询，避免为三个频道拆成多套相似的数据访问代码。
 * 业务逻辑：查询层只返回已发布内容，排序规则固定在 XML，保证前端不同页面拿到一致的数据顺序。
 */
@Mapper
public interface ContentMapper {

    /**
     * 业务目的：读取已发布技术文章列表，服务文章页、导航下拉和 AI 频道榜单。
     * 业务逻辑：排序优先级由 XML 维护，确保精选内容始终优先展示。
     */
    List<TechArticle> findPublishedTechArticles();

    /**
     * 业务目的：读取已发布看天下期刊列表，服务频道首页展示。
     * 业务逻辑：按发布时间倒序返回，让最新期刊默认出现在最前面。
     */
    List<WorldNewsIssue> findPublishedWorldNewsIssues();

    /**
     * 业务目的：读取已发布 AI 热点列表，服务推荐流与最新流切换。
     * 业务逻辑：基础列表按业务排序返回，前端可继续按热度或发布时间切换视图。
     */
    List<AiHotspot> findPublishedAiHotspots();
}
