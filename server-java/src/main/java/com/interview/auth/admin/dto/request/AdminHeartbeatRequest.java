package com.interview.auth.admin.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/**
 * 业务目的：接收后台管理页的在线心跳请求，用于统计当前在线人数。
 * 业务逻辑：前端会定时携带稳定 clientId 上报，后端据此维护短周期在线会话。
 */
@Getter
@Setter
public class AdminHeartbeatRequest {

    @NotBlank(message = "clientId 不能为空")
    private String clientId;
}
