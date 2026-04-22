package com.interview.auth.service;

import com.interview.auth.domain.dto.request.ForgotPasswordRequest;
import com.interview.auth.domain.dto.request.LoginRequest;
import com.interview.auth.domain.dto.request.RegisterRequest;
import com.interview.auth.domain.dto.request.SendRegisterCodeRequest;
import com.interview.auth.domain.dto.response.AuthUserResponse;
import com.interview.auth.domain.dto.response.LoginResponse;
import com.interview.auth.domain.dto.response.VerificationCodeSendResponse;

public interface AuthService {

    VerificationCodeSendResponse sendRegisterEmailCode(SendRegisterCodeRequest request, String clientIp);

    AuthUserResponse register(RegisterRequest request);

    LoginResponse login(LoginRequest request);

    AuthUserResponse getCurrentUser(String bearerToken);

    String forgotPassword(ForgotPasswordRequest request);
}
