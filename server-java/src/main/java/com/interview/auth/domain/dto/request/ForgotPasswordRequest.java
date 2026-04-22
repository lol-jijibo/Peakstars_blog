package com.interview.auth.domain.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/**
 * 忘记密码请求参数。
 * 作用：承接登录页弹层里的邮箱输入。
 */
@Getter
@Setter
public class ForgotPasswordRequest {

    /**
     * 用户注册邮箱。
     * 说明：后续邮件找回密码也会基于这个字段进行投递。
     */
    @Email(message = "邮箱格式不正确")
    @NotBlank(message = "邮箱不能为空")
    private String email;

    /**
     * 获取邮箱。
     *
     * @return 用户输入的邮箱地址
     */
    public String getEmail() {
        return email;
    }

    /**
     * 设置邮箱。
     *
     * @param email 用户输入的邮箱地址
     */
    public void setEmail(String email) {
        this.email = email;
    }
}
