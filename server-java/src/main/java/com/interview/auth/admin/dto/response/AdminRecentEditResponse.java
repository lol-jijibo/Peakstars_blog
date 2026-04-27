package com.interview.auth.admin.dto.response;

import lombok.Getter;
import lombok.Setter;

/**
 * 业务目的：承接后台最近编辑动态，方便管理台展示操作时间线。
 * 业务逻辑：日志已经带上操作类型、内容标题和时间，前端只需要顺序渲染即可。
 */
@Getter
@Setter
public class AdminRecentEditResponse {

    private Long id;
    private String contentType;
    private String contentKey;
    private String actionType;
    private String operatorName;
    private String contentTitle;
    private String createdAt;
}
