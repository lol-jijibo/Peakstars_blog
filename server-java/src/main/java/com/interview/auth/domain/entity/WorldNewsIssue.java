package com.interview.auth.domain.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/**
 * 业务目的：承接看天下期刊表的持久化字段，供后端统一查询频道页数据。
 * 业务逻辑：实体只负责保留数据库原始列，排序与裁剪由 Mapper / Service 按频道展示要求处理。
 */
@Getter
@Setter
public class WorldNewsIssue {

    private Long id;
    private String issueKey;
    private String title;
    private String issueLabel;
    private String category;
    private Integer todayReads;
    private BigDecimal recommendation;
    private String description;
    private String coverAccent;
    private String coverKicker;
    private String coverHeadline;
    private String coverSummary;
    private String coverFooter;
    private String contentHtml;
    private LocalDateTime publishedAt;
    private Integer status;
    private Integer sortOrder;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
