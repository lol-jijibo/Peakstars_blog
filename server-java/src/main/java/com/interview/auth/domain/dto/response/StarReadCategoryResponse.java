package com.interview.auth.domain.dto.response;

import lombok.Getter;
import lombok.Setter;

/**
 * 承接 star_read 分类区卡片所需的名称、数量与封面字段。
 * 服务层按现有文章数据聚合标签与分类，保持页面结构简单但内容可联动。
 */
@Getter
@Setter
public class StarReadCategoryResponse {

    private String key;
    private String name;
    private Integer bookCount;
    private String coverUrl;
}
