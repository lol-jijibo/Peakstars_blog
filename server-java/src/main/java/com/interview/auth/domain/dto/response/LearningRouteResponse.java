package com.interview.auth.domain.dto.response;

import java.util.List;
import lombok.Getter;
import lombok.Setter;

/**
 * Response object for learning route cards on the home page.
 */
@Getter
@Setter
public class LearningRouteResponse {

    private Long id;
    private String slug;
    private String routeType;
    private String title;
    private String summary;
    private String coverUrl;
    private String content;
    private List<String> stages;
    private String author;
    private String publishedAt;
    private Integer estimatedMinutes;
    private Integer viewCount;
    private Integer commentCount;
    private Integer likeCount;
    private Integer stageCount;
    private String difficulty;
    private List<String> tags;
    private Boolean featured;
}
