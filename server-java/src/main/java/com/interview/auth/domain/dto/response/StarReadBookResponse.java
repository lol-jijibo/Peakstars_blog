package com.interview.auth.domain.dto.response;

import lombok.Getter;
import lombok.Setter;

/**
 * 承接 star_read 页面中的书籍卡片与榜单条目字段。
 * 后端统一补齐作者、推荐值和封面信息，前端可直接渲染极简阅读布局。
 */
@Getter
@Setter
public class StarReadBookResponse {

    private String id;
    private String title;
    private String authorName;
    private String coverUrl;
    private String category;
    private String categoryLabel;
    private String summary;
    private String badgeText;
    private Integer readCount;
    private Integer likeCount;
    private Double recommendationScore;
    private String publishedAt;
}
