package com.interview.auth.domain.entity;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/**
 * 对应数据库 tech_article 表，封装技术文章展示与阅读状态的核心字段。
 * 配合 MyBatis 完成 ORM 映射，由 Service 层补齐当前用户阅读记录后输出给前端。
 */
@Getter
@Setter
public class TechArticle {

    private Long id;
    private String articleKey;
    private String category;
    private String categoryLabel;
    private String title;
    private String summary;
    private String essence;
    private String highlightList;
    private String authorName;
    private String authorRole;
    private String authorInitials;
    private String authorAccent;
    private String coverUrl;
    private String contentHtml;
    private LocalDateTime publishedAt;
    private Integer readCount;
    private Integer likeCount;
    private Integer collectCount;
    private Integer commentCount;
    private String readTime;
    private Integer isVip;
    private Integer isCollected;
    private Integer isLiked;
    private Integer inHistory;
    private Integer featured;
    private Integer status;
    private Integer sortOrder;
    private LocalDateTime lastReadAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
