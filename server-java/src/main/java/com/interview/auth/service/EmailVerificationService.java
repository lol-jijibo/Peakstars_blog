package com.interview.auth.service;

import com.interview.auth.domain.dto.response.VerificationCodeSendResponse;

public interface EmailVerificationService {
    // 发送注册验证码
    VerificationCodeSendResponse sendRegisterCode(String email, String clientIp);
    // 验证注册验证码
    void verifyRegisterCode(String email, String code);
}
