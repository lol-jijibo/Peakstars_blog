package com.interview.auth.domain.dto.response;

import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

/**
 * 业务目的：承接看天下期刊页的前端展示结构，确保频道页继续沿用现有字段命名。
 * 业务逻辑：后端直接输出封面与统计字段，前端只负责遍历渲染，不再依赖本地静态数据。
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
}
