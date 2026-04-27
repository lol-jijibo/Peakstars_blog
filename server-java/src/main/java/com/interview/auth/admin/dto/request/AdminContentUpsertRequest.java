package com.interview.auth.admin.dto.request;

import java.math.BigDecimal;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

/**
 * 业务目的：承接后台管理页对三类内容的新增、编辑和批量导入请求。
 * 业务逻辑：请求体统一保留三类内容的公共字段和扩展字段，Service 层再按模块做差异化映射。
 */
@Getter
@Setter
public class AdminContentUpsertRequest {

    private String id;
    private String title;
    private String category;
    private String summary;
    private String essence;
    private List<String> highlights;
    private String authorName;
    private String authorRole;
    private String authorInitials;
    private String authorAccent;
    private String coverUrl;
    private String contentHtml;
    private String publishedAt;
    private Integer viewCount;
    private Integer commentCount;
    private Integer likeCount;
    private Integer collectCount;
    private String readTime;
    private Boolean featured;
    private Boolean vip;
    private Boolean collected;
    private Boolean liked;
    private Boolean history;
    private String issueLabel;
    private BigDecimal recommendation;
    private String coverAccent;
    private String coverKicker;
    private String coverHeadline;
    private String coverSummary;
    private String coverFooter;
    private String track;
    private String hotspotType;
    private Integer heat;
    private Boolean recommended;
    private Boolean today;
    private List<String> tags;
}
