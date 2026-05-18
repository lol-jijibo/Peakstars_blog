package com.interview.auth.admin.dto.response;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

/**
 * 承接后台书籍导入任务批量处理后的结果摘要。
 * 服务层汇总成功数量、失败主键和最新任务状态，前端据此刷新批量操作反馈。
 */
@Getter
@Setter
public class AdminBookImportBatchResponse {

    private Integer successCount = 0;
    private Integer failedCount = 0;
    private List<String> failedKeys = new ArrayList<>();
    private List<AdminBookImportJobResponse> jobs = new ArrayList<>();
}
