package com.interview.auth.domain.dto.response;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VerificationCodeSendResponse {
    // 验证码有效期（秒）
    private long expiresInSeconds;
    // 验证码发送冷却时间（秒）
    private long cooldownSeconds;
}
