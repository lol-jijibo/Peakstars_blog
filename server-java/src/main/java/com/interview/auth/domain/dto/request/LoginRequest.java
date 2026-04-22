package com.interview.auth.domain.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/**
 * 登录请求参数。
 * 作用：承载前端登录表单提交的账号、密码和记住我标记。
 */
@Getter
@Setter
public class LoginRequest {

    /**
     * 登录账号。
     * 说明：当前同时支持邮箱和用户名两种输入方式。
     */
    @NotBlank(message = "账号不能为空")
    private String account;

    /**
     * 登录密码。
     * 说明：这里接收明文，进入业务层后只做匹配，不会持久化明文。
     */
    @NotBlank(message = "密码不能为空")
    private String password;

    /**
     * 记住我标记。
     * 说明：后端当前不参与 token 时长计算，只保留给前端存储 rememberedAccount 使用。
     */
    private Boolean remember;

    /**
     * 获取登录账号。
     *
     * @return 邮箱或用户名
     */
    public String getAccount() {
        return account;
    }

    /**
     * 设置登录账号。
     *
     * @param account 邮箱或用户名
     */
    public void setAccount(String account) {
        this.account = account;
    }

    /**
     * 获取登录密码。
     *
     * @return 当前输入的明文密码
     */
    public String getPassword() {
        return password;
    }

    /**
     * 设置登录密码。
     *
     * @param password 当前输入的明文密码
     */
    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * 获取记住我标记。
     *
     * @return 是否勾选记住我
     */
    public Boolean getRemember() {
        return remember;
    }

    /**
     * 设置记住我标记。
     *
     * @param remember 是否勾选记住我
     */
    public void setRemember(Boolean remember) {
        this.remember = remember;
    }
}
