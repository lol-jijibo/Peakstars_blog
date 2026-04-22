package com.interview.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "app.auth.verification")
public class AuthVerificationProperties {
    // 注册验证码过期时间（秒）
    @Min(60)
    @Max(1800)
    private int registerCodeExpireSeconds = 300;
    // 注册验证码发送冷却时间
    @Min(30)
    @Max(600)
    private int registerSendCooldownSeconds = 60;
    // 注册验证码最大验证次数
    @Min(1)
    @Max(10)
    private int registerMaxVerifyAttempts = 5;
    // 单个邮箱每日最大发送验证码次数
    @Min(1)
    @Max(100)
    private int registerMaxSendPerEmailPerDay = 10;
    // 单个IP每日最大发送验证码次数
    @Min(1)
    @Max(500)
    private int registerMaxSendPerIpPerDay = 30;
    // 注册验证码长度
    @Min(4)
    @Max(8)
    private int registerCodeLength = 6;
    // 发件人邮箱
    @NotBlank
    private String mailFrom = "";
    // 注册验证码邮件主题
    @NotBlank
    private String registerMailSubject = "PeakStars_blog 注册验证码";
}
