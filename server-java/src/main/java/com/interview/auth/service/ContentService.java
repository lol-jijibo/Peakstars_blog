package com.interview.auth.service;

import com.interview.auth.domain.dto.response.AiHotspotResponse;
import com.interview.auth.domain.dto.response.TechArticleResponse;
import com.interview.auth.domain.dto.response.WorldNewsIssueResponse;
import java.util.List;

/**
 * 业务目的：对外聚合技术文章、看天下和 AI 热点三类内容服务。
 * 业务逻辑：Service 层负责做字段转换和数组拆分，让 Controller 只关注 HTTP 返回结构。
 */
public interface ContentService {

    /**
     * 业务目的：获取技术文章模块列表数据。
     * 业务逻辑：返回结果兼容现有前端字段命名，便于页面无痛从静态数据切换到后端接口。
     */
    List<TechArticleResponse> listTechArticles();

    /**
     * 业务目的：获取看天下期刊列表数据。
     * 业务逻辑：返回结果按频道页现有结构组织，前端直接遍历即可渲染。
     */
    List<WorldNewsIssueResponse> listWorldNewsIssues();

    /**
     * 业务目的：获取 AI 热点列表数据。
     * 业务逻辑：保留推荐标记、发布时间和标签数组，方便前端做推荐 / 最新两种切换。
     */
    List<AiHotspotResponse> listAiHotspots();
}
