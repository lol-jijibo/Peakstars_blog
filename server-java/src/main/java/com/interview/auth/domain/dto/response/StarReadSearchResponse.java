package com.interview.auth.domain.dto.response;

import java.util.List;
import lombok.Getter;
import lombok.Setter;

/**
 * 承接 star_read 搜索结果页所需的查询词、引擎标识与分页结果。
 * 无论命中 Elasticsearch 还是数据库兜底，前端都按同一结构接收结果。
 */
@Getter
@Setter
public class StarReadSearchResponse {

    private String query;
    private String engine;
    private List<StarReadBookResponse> list;
    private long total;
    private int page;
    private int pageSize;
}
