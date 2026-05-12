package com.interview.auth.service;

import com.interview.auth.domain.dto.response.StarReadHomeResponse;
import com.interview.auth.domain.dto.response.StarReadSearchResponse;
import com.interview.auth.domain.dto.response.StarReadSearchSuggestionResponse;
import java.util.List;

/**
 * 定义 star_read 首页与搜索模块的对外契约。
 * 实现类按此接口组装阅读榜单与搜索结果，便于后续替换真实搜索实现。
 */
public interface StarReadService {

    /**
     * 组装 star_read 首页所需的阅读卡片、榜单和分类数据。
     * 基于现有技术文章列表计算展示模块，保持前后端字段稳定。
     */
    StarReadHomeResponse getHomeData();

    /**
     * 按关键词查询 star_read 阅读内容列表。
     * 优先尝试 Elasticsearch 检索，未启用或失败时降级为数据库结果。
     */
    StarReadSearchResponse searchBooks(String keyword, int page, int size);

    /**
     * 获取 star_read 搜索框的即时建议词。
     * 先读取搜索引擎建议，再回退到本地文章标题与标签匹配结果。
     */
    List<StarReadSearchSuggestionResponse> suggestBooks(String keyword, int size);
}
