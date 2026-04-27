package com.interview.auth.domain.entity;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/**
 * 业务目的：承接技术文章表的持久化字段，供 MyBatis 直接映射数据库记录。
 * 业务逻辑：实体保留作者、亮点和展示状态等原始字段，具体数组拆分与前端结构转换交给 Service 层完成。
 */
@Getter
@Setter
public class TechArticle {

    private Long id;
    private String articleKey;
    private String category;
    private String title;
    private String summary;
    private String essence;
    private String highlightList;
    private String authorName;
    private String authorRole;
    private String authorInitials;
    private String authorAccent;
    private String coverUrl;
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
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
