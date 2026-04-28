package com.interview.auth.domain.dto.response;

import java.util.List;
import lombok.Getter;
import lombok.Setter;

/**
 * 业务目的：承接技术文章模块的前端展示结构，服务文章列表页与导航下拉预览。
 * 业务逻辑：后端直接返回前端可渲染的布尔状态、亮点数组和作者对象，减少页面层转换成本。
 */
@Getter
@Setter
public class TechArticleResponse {

    private String id;
    private String category;
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
    private Boolean isVip;
    private Boolean isCollected;
    private Boolean isLiked;
    private Boolean inHistory;
    private Boolean featured;
}
