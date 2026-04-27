package com.interview.auth.admin.dto.response;

import java.math.BigDecimal;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

/**
 * 业务目的：承接后台内容管理表格和编辑抽屉的统一记录结构。
 * 业务逻辑：用一份超集字段覆盖技术文章、看天下和 AI 热点三类内容，前端按当前模块裁剪展示字段。
 */
@Getter
@Setter
public class AdminContentRecordResponse {

    private String type;
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
