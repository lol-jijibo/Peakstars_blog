package com.interview.auth.config;

import com.interview.auth.common.BusinessException;
import com.interview.auth.domain.entity.AuthUser;
import com.interview.auth.infrastructure.mapper.AuthUserMapper;
import com.interview.auth.service.TokenService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 集中拦截管理后台接口请求，确保只有管理员身份可以访问。
 * 解析登录凭证后查询数据库角色，未通过校验时返回隐晦的不可用提示。
 */
@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {

    private static final String ADMIN_ROLE = "admin";
    private static final String ACCESS_UNAVAILABLE_MESSAGE = "当前页面暂时不可用";

    private final TokenService tokenService;
    private final AuthUserMapper authUserMapper;

    /**
     * 校验管理后台请求的登录态与管理员身份。
     * 从 Bearer Token 解析用户编号，再以数据库中的有效角色作为最终准入依据。
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String authorization = request.getHeader("Authorization");
        if (authorization == null || authorization.isBlank()) {
            throw new BusinessException(401, ACCESS_UNAVAILABLE_MESSAGE);
        }

        Map<String, Object> payload = tokenService.parseToken(authorization);
        Long userId = resolveUserId(payload);
        AuthUser authUser = authUserMapper.findById(userId);
        if (!isActiveAdmin(authUser)) {
            throw new BusinessException(403, ACCESS_UNAVAILABLE_MESSAGE);
        }

        request.setAttribute("currentUserId", userId);
        return true;
    }

    /**
     * 从 token 载荷中提取用户编号。
     * 编号缺失或格式异常时统一按不可访问处理，避免暴露内部校验细节。
     */
    private Long resolveUserId(Map<String, Object> payload) {
        try {
            return Long.valueOf(String.valueOf(payload.get("userId")));
        } catch (Exception exception) {
            throw new BusinessException(401, ACCESS_UNAVAILABLE_MESSAGE);
        }
    }

    /**
     * 判断用户是否仍是可用管理员。
     * 以数据库实时状态为准，防止前端缓存或旧 token 绕过角色变更。
     */
    private boolean isActiveAdmin(AuthUser authUser) {
        return authUser != null
            && Integer.valueOf(1).equals(authUser.getStatus())
            && ADMIN_ROLE.equals(authUser.getRole());
    }
}
