package com.interview.auth.admin.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/**
 * 承接后台外部内容导入预处理请求。
 * 统一接收外部编辑器粘贴过来的正文 HTML 与来源信息，供服务端执行资源迁移和白名单清洗。
 */
@Getter
@Setter
public class AdminContentImportPreviewRequest {

    @NotBlank(message = "contentHtml 不能为空")
    private String contentHtml;

    private String sourceType;

    private String sourceUrl;

    private Boolean migrateAssets;
}
