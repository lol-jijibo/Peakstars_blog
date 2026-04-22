package com.interview.auth.domain.dto.response;

import lombok.Data;

/**
 * 登录成功响应体。
 * 作用：前端拿到 token 和 user 后写入本地，供路由守卫和页面展示使用。
 */
@Data
public class LoginResponse {

    /**
     * 登录凭证。
     * 说明：由后端签发，前端后续请求可通过 Authorization 头携带。
     */
    private String token;

    /**
     * 当前登录用户。
     * 说明：已经过脱敏处理，只保留前端需要展示的安全字段。
     */
    private AuthUserResponse user;

    /**
     * 获取 token。
     *
     * @return 当前登录凭证
     */
    public String getToken() {
        return token;
    }

    /**
     * 设置 token。
     *
     * @param token 当前登录凭证
     */
    public void setToken(String token) {
        this.token = token;
    }

    /**
     * 获取用户信息。
     *
     * @return 当前登录用户
     */
    public AuthUserResponse getUser() {
        return user;
    }

    /**
     * 设置用户信息。
     *
     * @param user 当前登录用户
     */
    public void setUser(AuthUserResponse user) {
        this.user = user;
    }
}
