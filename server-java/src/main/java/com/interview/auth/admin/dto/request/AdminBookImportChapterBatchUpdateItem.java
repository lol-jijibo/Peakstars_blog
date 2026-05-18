package com.interview.auth.admin.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/**
 * 承接单个导入章节的批量保存字段。
 * 通过章节临时主键定位暂存记录，再复用标题、正文和排序字段完成更新。
 */
@Getter
@Setter
public class AdminBookImportChapterBatchUpdateItem extends AdminBookImportChapterUpdateRequest {

    @NotBlank(message = "tempChapterKey 不能为空")
    private String tempChapterKey;
}
