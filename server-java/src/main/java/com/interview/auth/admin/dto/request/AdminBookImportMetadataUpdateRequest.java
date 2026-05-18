package com.interview.auth.admin.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/**
 * 承接后台对导入书籍元数据的修改请求。
 * 允许运营在发布前校正书名、作者、译者和封面，保证正式入库信息准确。
 */
@Getter
@Setter
public class AdminBookImportMetadataUpdateRequest {

    @NotBlank(message = "title 不能为空")
    private String title;

    @NotBlank(message = "author 不能为空")
    private String author;

    private String translator;

    private String category;

    private String coverUrl;
}
