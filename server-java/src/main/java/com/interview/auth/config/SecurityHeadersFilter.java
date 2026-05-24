package com.interview.auth.config;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.stereotype.Component;

/**
 * 为所有 HTTP 响应添加基础安全头，防御常见 Web 攻击。
 * 包括点击劫持防护、MIME 类型嗅探防护和 XSS 浏览器防护。
 */
@Component
public class SecurityHeadersFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
        throws IOException, ServletException {

        HttpServletResponse httpResponse = (HttpServletResponse) response;

        // 禁止浏览器 MIME 类型嗅探
        httpResponse.setHeader("X-Content-Type-Options", "nosniff");
        // 禁止页面被嵌入 iframe（防点击劫持）
        httpResponse.setHeader("X-Frame-Options", "DENY");
        // 启用浏览器内置 XSS 过滤器
        httpResponse.setHeader("X-XSS-Protection", "1; mode=block");
        // 限制 referrer 信息只在同源请求中发送
        httpResponse.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");

        chain.doFilter(request, response);
    }

    @Override
    public void init(FilterConfig filterConfig) {
    }

    @Override
    public void destroy() {
    }
}
