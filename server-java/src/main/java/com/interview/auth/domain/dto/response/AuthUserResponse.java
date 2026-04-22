package com.interview.auth.domain.dto.response;

import lombok.Getter;
import lombok.Setter;

/**
 * 返回给前端的用户信息。
 * 作用：对外只暴露登录态需要的安全字段，不返回密码哈希等敏感信息。
 */
@Getter
@Setter
public class AuthUserResponse {

    /**
     * 用户主键 ID。
     * 作用：供前端识别当前用户及后续扩展个人中心使用。
     */
    private Long id;

    /**
     * 用户名。
     * 作用：登录成功提示、页面头部昵称展示等场景会使用。
     */
    private String username;

    /**
     * 注册邮箱。
     * 作用：前端个人信息展示和账号识别使用。
     */
    private String email;

    /**
     * 注册时间。
     * 说明：这里已经被格式化为字符串，方便前端直接展示。
     */
    private String joinedAt;

    /**
     * 获取用户 ID。
     *
     * @return 用户主键 ID
     */
    public Long getId() {
        return id;
    }

    /**
     * 设置用户 ID。
     *
     * @param id 用户主键 ID
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * 获取用户名。
     *
     * @return 用户名
     */
    public String getUsername() {
        return username;
    }

    /**
     * 设置用户名。
     *
     * @param username 用户名
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * 获取邮箱。
     *
     * @return 注册邮箱
     */
    public String getEmail() {
        return email;
    }

    /**
     * 设置邮箱。
     *
     * @param email 注册邮箱
     */
    public void setEmail(String email) {
        this.email = email;
    }

    /**
     * 获取加入时间。
     *
     * @return 格式化后的注册时间
     */
    public String getJoinedAt() {
        return joinedAt;
    }

    /**
     * 设置加入时间。
     *
     * @param joinedAt 格式化后的注册时间
     */
    public void setJoinedAt(String joinedAt) {
        this.joinedAt = joinedAt;
    }
}
