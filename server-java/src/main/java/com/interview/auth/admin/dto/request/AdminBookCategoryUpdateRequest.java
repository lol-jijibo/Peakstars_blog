package com.interview.auth.admin.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/**
 * 承接后台书籍总览页的分类标签修改请求。
 * 校验分类标签非空后交由 Service 层同步正式书籍与导入任务。
 */
@Getter
@Setter
public class AdminBookCategoryUpdateRequest {

    @NotBlank(message = "category 不能为空")
    private String category;
}
