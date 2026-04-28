package com.interview.auth.admin.dto.response;

import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

/**
 * 目的: 承接后台评论管理模块所需的统一评论运营记录。
 * 逻辑: 基于现有内容表里的评论量、浏览量和发布时间做聚合映射，让前端无需真实评论明细表也能完成评论管理看板与优先级巡检。
 */
@Getter
@Setter
public class AdminCommentRecordResponse {

    private String contentType;
    private String contentKey;
    private String moduleLabel;
    private String contentTitle;
    private String authorName;
    private String publishedAt;
    private Integer viewCount;
    private Integer commentCount;
    private Integer pendingCount;
    private BigDecimal engagementRate;
    private String priority;
    private String priorityLabel;
    private String status;
    private String statusLabel;
    private String actionHint;
}
