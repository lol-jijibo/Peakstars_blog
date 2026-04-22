package com.interview.auth.controller;

import com.interview.auth.common.ApiResponse;
import com.interview.auth.domain.dto.request.ForgotPasswordRequest;
import com.interview.auth.domain.dto.request.LoginRequest;
import com.interview.auth.domain.dto.request.RegisterRequest;
import com.interview.auth.domain.dto.request.SendRegisterCodeRequest;
import com.interview.auth.domain.dto.response.AuthUserResponse;
import com.interview.auth.domain.dto.response.LoginResponse;
import com.interview.auth.domain.dto.response.VerificationCodeSendResponse;
import com.interview.auth.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register/email-code/send")
    public ApiResponse<VerificationCodeSendResponse> sendRegisterEmailCode(
        @Valid @RequestBody SendRegisterCodeRequest request,
        HttpServletRequest httpServletRequest
    ) {
        VerificationCodeSendResponse response = authService.sendRegisterEmailCode(
            request,
            extractClientIp(httpServletRequest)
        );
        return ApiResponse.success("Verification code sent. Please check your QQ mailbox.", response);
    }

    @PostMapping("/register")
    public ApiResponse<Map<String, AuthUserResponse>> register(@Valid @RequestBody RegisterRequest request) {
        AuthUserResponse user = authService.register(request);
        return ApiResponse.success("Registration completed. Please sign in.", Map.of("user", user));
    }

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return ApiResponse.success("Welcome back, " + response.getUser().getUsername(), response);
    }

    @GetMapping("/me")
    public ApiResponse<Map<String, AuthUserResponse>> me(@RequestHeader("Authorization") String authorization) {
        AuthUserResponse user = authService.getCurrentUser(authorization);
        return ApiResponse.success(Map.of("user", user));
    }

    @PostMapping("/forgot-password")
    public ApiResponse<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        String message = authService.forgotPassword(request);
        return ApiResponse.success(message, null);
    }

    private String extractClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }

        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }

        return request.getRemoteAddr();
    }
}
