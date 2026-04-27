package com.interview.auth.admin.dto.response;

import lombok.Getter;
import lombok.Setter;

/**
 * 业务目的：承接后台仪表盘顶部核心指标卡片数据。
 * 业务逻辑：将在线人数、总浏览、评论量和今日编辑量集中返回，前端可直接做卡片展示。
 */
@Getter
@Setter
public class AdminSummaryResponse {

    private Integer onlineUsers;
    private Integer totalViews;
    private Integer totalComments;
    private Integer editsToday;
    private Integer totalContents;
    private String lastUpdatedAt;
}
