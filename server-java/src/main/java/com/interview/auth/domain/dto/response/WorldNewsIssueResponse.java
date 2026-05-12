package com.interview.auth.domain.dto.response;

import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

/**
 * 承接看天下频道页、榜单区和详情浮层共用的期刊展示字段。
 * 后端把数据库原始列统一收口到这里，前端可以直接复用同一份结构渲染不同区域。
 */
@Getter
@Setter
public class WorldNewsIssueResponse {

    private String id;
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
    private String publishedAt;
    private String contentHtml;
}
