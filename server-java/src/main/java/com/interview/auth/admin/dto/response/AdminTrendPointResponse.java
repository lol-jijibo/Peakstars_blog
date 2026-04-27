package com.interview.auth.admin.dto.response;

import lombok.Getter;
import lombok.Setter;

/**
 * 业务目的：承接后台实时趋势图的数据点，供 ECharts 折线图直接消费。
 * 业务逻辑：每个点保存统一时间标签和关键指标，前端无需再拼接坐标轴结构。
 */
@Getter
@Setter
public class AdminTrendPointResponse {

    private String timeLabel;
    private Integer onlineUsers;
    private Integer totalViews;
    private Integer totalComments;
    private Integer editsToday;
}
