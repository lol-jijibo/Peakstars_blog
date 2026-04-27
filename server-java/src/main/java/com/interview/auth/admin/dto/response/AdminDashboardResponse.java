package com.interview.auth.admin.dto.response;

import java.util.List;
import lombok.Getter;
import lombok.Setter;

/**
 * 业务目的：聚合后台管理页首页所需的全部仪表盘数据。
 * 业务逻辑：后端一次性返回核心指标、趋势点、模块统计和最近编辑，减少管理台首屏请求数量。
 */
@Getter
@Setter
public class AdminDashboardResponse {

    private AdminSummaryResponse summary;
    private List<AdminTrendPointResponse> trendPoints;
    private List<AdminModuleStatResponse> moduleStats;
    private List<AdminRecentEditResponse> recentEdits;
    private List<AdminContentRecordResponse> hotContents;
}
