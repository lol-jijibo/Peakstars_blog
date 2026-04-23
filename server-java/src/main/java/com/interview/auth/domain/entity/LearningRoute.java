package com.interview.auth.domain.entity;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/**
 * Learning route persistence entity.
 * Maps the learning_route table and carries the card data shown on the home page.
 */
@Getter
@Setter
public class LearningRoute {

    private Long id;
    private String slug;
    private String routeType;
    private String title;
    private String summary;
    private String coverUrl;
    private String content;
    private String stageList;
    private String author;
    private LocalDateTime publishedAt;
    private Integer estimatedMinutes;
    private Integer viewCount;
    private Integer commentCount;
    private Integer likeCount;
    private Integer stageCount;
    private String difficulty;
    private String tags;
    private Integer featured;
    private Integer status;
    private Integer sortOrder;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
