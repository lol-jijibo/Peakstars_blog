package com.interview.auth.admin.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/**
 * 承接后台对导入章节暂存内容的修改请求。
 * 允许运营在发布前调整标题、副标题、正文和排序，保证正式入库内容可控。
 */
@Getter
@Setter
public class AdminBookImportChapterUpdateRequest {

    @NotBlank(message = "title 不能为空")
    private String title;

    private String subtitle;

    @NotBlank(message = "contentHtml 不能为空")
    private String contentHtml;

    private Integer sortOrder;
}
