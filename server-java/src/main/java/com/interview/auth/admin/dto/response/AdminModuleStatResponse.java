package com.interview.auth.admin.dto.response;

import lombok.Getter;
import lombok.Setter;

/**
 * 业务目的：承接模块维度的内容量与热度统计，供 G2 高级图表绘制。
 * 业务逻辑：把不同内容模块统一抽象成同一统计结构，方便前端做对比视图。
 */
@Getter
@Setter
public class AdminModuleStatResponse {

    private String moduleKey;
    private String moduleLabel;
    private Integer contentCount;
    private Integer viewCount;
    private Integer commentCount;
}
