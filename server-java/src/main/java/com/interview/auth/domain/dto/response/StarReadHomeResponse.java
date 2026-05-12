package com.interview.auth.domain.dto.response;

import java.util.List;
import lombok.Getter;
import lombok.Setter;

/**
 * 承接 star_read 首页的品牌、热搜、榜单与分类聚合数据。
 * 控制器直接返回该结构，前端只需按模块顺序渲染即可完成首页拼装。
 */
@Getter
@Setter
public class StarReadHomeResponse {

    private String brandName;
    private String searchPlaceholder;
    private List<String> hotKeywords;
    private List<StarReadBookResponse> everyoneReads;
    private List<StarReadRankingResponse> rankings;
    private List<StarReadCategoryResponse> categories;
}
