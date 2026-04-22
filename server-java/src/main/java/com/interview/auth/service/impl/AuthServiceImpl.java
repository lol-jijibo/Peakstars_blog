package com.interview.auth.service.impl;

import com.interview.auth.common.BusinessException;
import com.interview.auth.domain.dto.request.ForgotPasswordRequest;
import com.interview.auth.domain.dto.request.LoginRequest;
import com.interview.auth.domain.dto.request.RegisterRequest;
import com.interview.auth.domain.dto.request.SendRegisterCodeRequest;
import com.interview.auth.domain.dto.response.AuthUserResponse;
import com.interview.auth.domain.dto.response.LoginResponse;
import com.interview.auth.domain.dto.response.VerificationCodeSendResponse;
import com.interview.auth.domain.entity.AuthUser;
import com.interview.auth.infrastructure.mapper.AuthUserMapper;
import com.interview.auth.service.AuthService;
import com.interview.auth.service.EmailVerificationService;
import com.interview.auth.service.PasswordService;
import com.interview.auth.service.TokenService;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final AuthUserMapper authUserMapper;
    private final PasswordService passwordService;
    private final TokenService tokenService;
    private final EmailVerificationService emailVerificationService;

    @Override
    public VerificationCodeSendResponse sendRegisterEmailCode(SendRegisterCodeRequest request, String clientIp) {
        String email = normalizeEmail(request.getEmail());
        ensureEmailAvailable(email);
        return emailVerificationService.sendRegisterCode(email, clientIp);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AuthUserResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.getEmail());
        String username = buildUsername(request.getUsername(), email);

        ensureEmailAvailable(email);
        emailVerificationService.verifyRegisterCode(email, request.getEmailCode());

        if (authUserMapper.findByUsername(username) != null) {
            throw new BusinessException(400, "Username already exists");
        }

        if (!request.getPassword().equals(request.getPasswordConfirm())) {
            throw new BusinessException(400, "Passwords do not match");
        }

        AuthUser authUser = new AuthUser();
        authUser.setUsername(username);
        authUser.setEmail(email);
        authUser.setPasswordHash(passwordService.encode(request.getPassword()));
        authUser.setStatus(1);
        authUserMapper.insert(authUser);

        return toResponse(authUserMapper.findById(authUser.getId()));
    }

    @Override
    public LoginResponse login(LoginRequest request) {
        String account = request.getAccount().trim().toLowerCase();
        AuthUser authUser = authUserMapper.findByEmailOrUsername(account);

        if (authUser == null || authUser.getStatus() == null || authUser.getStatus() != 1) {
            throw new BusinessException(400, "Account does not exist");
        }

        if (!passwordService.matches(request.getPassword(), authUser.getPasswordHash())) {
            throw new BusinessException(400, "Incorrect password");
        }

        LoginResponse loginResponse = new LoginResponse();
        loginResponse.setToken(tokenService.generateToken(authUser.getId(), authUser.getEmail(), authUser.getUsername()));
        loginResponse.setUser(toResponse(authUser));
        return loginResponse;
    }

    @Override
    public AuthUserResponse getCurrentUser(String bearerToken) {
        Map<String, Object> payload = tokenService.parseToken(bearerToken);
        Long userId = Long.valueOf(String.valueOf(payload.get("userId")));
        AuthUser authUser = authUserMapper.findById(userId);
        if (authUser == null || authUser.getStatus() == null || authUser.getStatus() != 1) {
            throw new BusinessException(401, "Current user is unavailable");
        }
        return toResponse(authUser);
    }

    @Override
    public String forgotPassword(ForgotPasswordRequest request) {
        return "Password reset link sent to " + normalizeEmail(request.getEmail());
    }

    private AuthUserResponse toResponse(AuthUser authUser) {
        AuthUserResponse response = new AuthUserResponse();
        response.setId(authUser.getId());
        response.setUsername(authUser.getUsername());
        response.setEmail(authUser.getEmail());
        response.setJoinedAt(
            authUser.getCreatedAt() == null
                ? null
                : authUser.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        );
        return response;
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }

    private String buildUsername(String username, String email) {
        if (username != null && !username.trim().isEmpty()) {
            return username.trim();
        }
        int atIndex = email.indexOf("@");
        return atIndex > 0 ? email.substring(0, atIndex) : "user";
    }

    private void ensureEmailAvailable(String email) {
        if (authUserMapper.findByEmail(email) != null) {
            throw new BusinessException(400, "This email has already been registered");
        }
    }
}
