package com.interview.auth.admin.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/**
 * 外部资源导入请求参数。
 * 支持通过 URL 从外部 API 或在线资源导入书籍内容，自动拉取并解析。
 */
@Getter
@Setter
public class AdminBookImportExternalRequest {

    @NotBlank(message = "资源地址不能为空")
    private String sourceUrl;

    private String title;

    private String author;

    private String publisher;

    private String category;

    private String summary;

    private String importType;
}
