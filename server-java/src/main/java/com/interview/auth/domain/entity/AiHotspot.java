package com.interview.auth.domain.entity;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/**
 * 业务目的：承接 AI 热点表的持久化字段，供热点流与推荐榜统一读取。
 * 业务逻辑：实体保留赛道、推荐标记、标签串和热度等原始字段，后续由 Service 层转换为前端数组结构。
 */
@Getter
@Setter
public class AiHotspot {

    private Long id;
    private String hotspotKey;
    private String track;
    private String hotspotType;
    private String title;
    private String summary;
    private String authorName;
    private LocalDateTime publishedAt;
    private String coverUrl;
    private String tagList;
    private Integer viewCount;
    private Integer commentCount;
    private Integer likeCount;
    private Integer heat;
    private Integer isRecommended;
    private Integer isToday;
    private Integer status;
    private Integer sortOrder;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
