package com.interview.auth.admin.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

/**
 * 承接后台书籍导入任务的批量审核请求。
 * 前端提交任务主键集合后，服务层统一执行通过、拒绝或发布动作。
 */
@Getter
@Setter
public class AdminBookImportBatchRequest {

    @NotEmpty(message = "jobKeys 不能为空")
    private List<@NotBlank(message = "jobKey 不能为空") String> jobKeys = new ArrayList<>();

    private String reason;
}
