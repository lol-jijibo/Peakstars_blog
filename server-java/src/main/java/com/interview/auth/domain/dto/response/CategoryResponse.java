package com.interview.auth.domain.dto.response;

import lombok.Getter;
import lombok.Setter;

/**
 * 承载面经分类的对外输出结构，供前端分类导航和筛选下拉使用。
 * 直接透传数据库的 id / code / name，与现有前端 getCategories 接口的消费方对齐。
 */
@Getter
@Setter
public class CategoryResponse {

    private Long id;
    private String code;
    private String name;
}
