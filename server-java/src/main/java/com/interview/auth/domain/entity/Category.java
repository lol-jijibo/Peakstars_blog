package com.interview.auth.domain.entity;

import lombok.Getter;
import lombok.Setter;

/**
 * 业务目的：承接面经分类表的持久化字段，用于面经列表的分类筛选和分类导航展示。
 * 业务逻辑：保存分类编码和排序权重，前端的分类下拉筛选依赖 c.code 做参数传递。
 */
@Getter
@Setter
public class Category {

    private Long id;
    private String code;
    private String name;
    private Integer sortOrder;
}
