package com.interview.auth.domain.dto.response;

import java.util.List;
import lombok.Getter;
import lombok.Setter;

/**
 * 承接技术文章列表与详情页响应字段，统一输出前端直接可用的数据结构。
 * 后端在这里补齐作者对象、阅读状态和最近阅读时间，减少页面层转换成本。
 */
@Getter
@Setter
public class TechArticleResponse {

    private String id;
    private String category;
    private String categoryLabel;
    private String title;
    private String summary;
    private String essence;
    private List<String> highlights;
    private TechArticleAuthorResponse author;
    private String coverUrl;
    private String contentHtml;
    private String publishedAt;
    private Integer readCount;
    private Integer likeCount;
    private Integer collectCount;
    private Integer commentCount;
    private String readTime;
    private String lastReadAt;
    private Boolean isVip;
    private Boolean isCollected;
    private Boolean isLiked;
    private Boolean inHistory;
    private Boolean featured;
}
