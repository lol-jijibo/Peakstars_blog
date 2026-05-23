package com.interview.auth.config;

import com.interview.auth.common.BusinessException;
import com.interview.auth.service.TokenService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 管理员接口鉴权拦截器。
 * 对所有 /api/admin/** 请求校验 Bearer Token，未登录或 token 无效直接返回 401。
 */
@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {

    private final TokenService tokenService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String authorization = request.getHeader("Authorization");
        if (authorization == null || authorization.isBlank()) {
            throw new BusinessException(401, "请先登录后再访问管理后台");
        }

        Map<String, Object> payload = tokenService.parseToken(authorization);
        request.setAttribute("currentUserId", payload.get("userId"));
        return true;
    }
}
