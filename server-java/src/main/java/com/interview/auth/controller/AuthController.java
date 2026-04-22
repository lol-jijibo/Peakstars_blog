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

    /**
     * 认证业务服务。
     * Controller 只负责接收请求、参数校验和组织响应，具体业务逻辑交给 Service 层处理。
     */
    private final AuthService authService;

    /**
     * 发送注册验证码。
     * 业务目标：
     * 1. 接收前端提交的注册邮箱。
     * 2. 识别当前请求来源 IP，用于验证码发送限流和风控。
     * 3. 调用认证服务发送验证码，并返回冷却时间与过期时间等信息。
     */
    @PostMapping("/register/email-code/send")
    public ApiResponse<VerificationCodeSendResponse> sendRegisterEmailCode(
        @Valid @RequestBody SendRegisterCodeRequest request,
        HttpServletRequest httpServletRequest // 用于提取客户端真实 IP
    ) {
        // 提取客户端真实 IP，优先取反向代理转发头，便于做 IP 维度的限流与安全校验。
        VerificationCodeSendResponse response = authService.sendRegisterEmailCode(
            request,
            extractClientIp(httpServletRequest)
        );

        // 统一包装成功响应，返回给前端用于展示发送结果和倒计时信息。
        return ApiResponse.success("Verification code sent. Please check your QQ mailbox.", response);
    }

    /**
     * 用户注册。
     * 业务目标：
     * 1. 校验注册表单参数是否合法。
     * 2. 调用服务层完成验证码校验、用户创建、密码加密等核心注册流程。
     * 3. 返回脱敏后的用户信息，供前端在注册成功后展示。
     */
    @PostMapping("/register")
    public ApiResponse<Map<String, AuthUserResponse>> register(@Valid @RequestBody RegisterRequest request) {
        // Service 层内部会完成邮箱验证码校验、唯一性校验、密码处理和用户落库。
        AuthUserResponse user = authService.register(request);

        // 统一返回 user 对象，便于前端沿用固定的数据结构读取注册结果。
        return ApiResponse.success("Registration completed. Please sign in.", Map.of("user", user));
    }

    /**
     * 用户登录。
     * 业务目标：
     * 1. 校验账号和密码。
     * 2. 调用服务层完成身份认证。
     * 3. 返回登录态所需的 token 和当前用户信息。
     */
    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        // Service 层会负责账号识别、密码比对以及 token 生成。
        LoginResponse response = authService.login(request);

        // 登录成功后将用户信息和 token 一并返回，前端可直接完成登录态持久化。
        return ApiResponse.success("Welcome back, " + response.getUser().getUsername(), response);
    }

    /**
     * 获取当前登录用户信息。
     * 业务目标：
     * 1. 从 Authorization 请求头中读取 token。
     * 2. 调用服务层解析并校验 token。
     * 3. 返回当前登录用户的基础资料。
     */
    @GetMapping("/me")
    public ApiResponse<Map<String, AuthUserResponse>> me(@RequestHeader("Authorization") String authorization) {
        // Service 层会负责 Bearer Token 解析、有效性校验以及用户身份恢复。
        AuthUserResponse user = authService.getCurrentUser(authorization);

        // 保持与其他认证接口一致的响应结构，降低前端处理复杂度。
        return ApiResponse.success(Map.of("user", user));
    }

    /**
     * 忘记密码入口。
     * 业务目标：
     * 1. 接收用户提交的邮箱信息。
     * 2. 调用服务层触发找回密码相关流程。
     * 3. 返回处理结果消息，供前端提示用户后续操作。
     */
    @PostMapping("/forgot-password")
    public ApiResponse<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        // 具体的邮箱校验、重置流程触发等逻辑由 Service 层统一处理。
        String message = authService.forgotPassword(request);

        // 该接口当前只需要返回提示信息，因此 data 为空。
        return ApiResponse.success(message, null);
    }

    /**
     * 提取客户端真实 IP。
     * 处理思路：
     * 1. 优先读取 X-Forwarded-For，适配 Nginx / 网关 / 负载均衡转发场景。
     * 2. 再读取 X-Real-IP，兼容部分代理服务器配置。
     * 3. 最后回退到 Servlet 容器识别到的远端地址。
     */
    private String extractClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            // X-Forwarded-For 可能是多个 IP，首个通常是原始客户端地址。
            return forwardedFor.split(",")[0].trim();
        }

        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            // 如果代理层单独透传了真实 IP，则直接使用。
            return realIp.trim();
        }

        // 当前面都没有时，使用容器直接感知到的请求来源地址。
        return request.getRemoteAddr();
    }
}
