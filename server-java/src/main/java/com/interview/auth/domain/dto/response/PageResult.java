package com.interview.auth.domain.dto.response;

import java.util.List;

/**
 * 统一承载分页查询的通用返回结构，供面经列表等需要分页的接口复用。
 * 前端消费方直接读取 list / total / page / pageSize 四个字段渲染分页器。
 *
 * @param <T> 列表条目类型
 */
public record PageResult<T>(List<T> list, long total, int page, int pageSize) {
}
