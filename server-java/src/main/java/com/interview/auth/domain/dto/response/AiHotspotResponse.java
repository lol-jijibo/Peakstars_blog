package com.interview.auth.domain.dto.response;

import java.util.List;
import lombok.Getter;
import lombok.Setter;

/**
 * 业务目的：承接 AI 热点流的前端展示结构，支撑推荐流、最新流和标签渲染。
 * 业务逻辑：后端提前把标签串转换为数组，同时保留推荐与今日热点标记，方便前端直接筛选。
 */
@Getter
@Setter
public class AiHotspotResponse {

    private String id;
    private String track;
    private String hotspotType;
    private String title;
    private String summary;
    private String authorName;
    private String publishedAt;
    private String coverUrl;
    private List<String> tags;
    private Integer viewCount;
    private Integer commentCount;
    private Integer likeCount;
    private Integer heat;
    private Boolean isRecommended;
    private Boolean isToday;
}
