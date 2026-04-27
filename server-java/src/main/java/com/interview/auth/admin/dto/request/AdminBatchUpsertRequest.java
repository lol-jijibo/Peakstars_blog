package com.interview.auth.admin.dto.request;

import jakarta.validation.Valid;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

/**
 * 业务目的：承接后台 Excel 批量导入后的统一保存请求。
 * 业务逻辑：前端会先把 XLSX 转成标准记录数组，再由后端逐条做幂等写入和编辑日志记录。
 */
@Getter
@Setter
public class AdminBatchUpsertRequest {

    @Valid
    private List<AdminContentUpsertRequest> records = new ArrayList<>();
}
