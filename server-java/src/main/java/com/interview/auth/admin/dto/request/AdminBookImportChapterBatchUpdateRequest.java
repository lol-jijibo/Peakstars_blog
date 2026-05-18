package com.interview.auth.admin.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

/**
 * 承接后台导入章节的批量保存请求。
 * 前端汇总多个章节草稿后一次提交，服务层逐章更新暂存内容。
 */
@Getter
@Setter
public class AdminBookImportChapterBatchUpdateRequest {

    @Valid
    @NotEmpty(message = "chapters 不能为空")
    private List<AdminBookImportChapterBatchUpdateItem> chapters = new ArrayList<>();
}
