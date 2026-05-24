package com.interview.auth.config;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 启动时校验 token 签名密钥强度，防止生产环境使用弱密钥。
 * 仅在检测到已知的弱密钥或密钥长度不足时发出告警。
 */
@Slf4j
@Component
public class TokenSecretStartupChecker {

    private static final int MIN_SECRET_BYTES = 32; // HMAC-SHA256 推荐最低 32 字节密钥
    private static final String KNOWN_DEV_SECRET = "dev-secret-key-for-local-development-only";

    @Value("${app.auth.token-secret}")
    private String tokenSecret;

    @EventListener(ApplicationReadyEvent.class)
    public void checkTokenSecret() {
        if (tokenSecret == null || tokenSecret.isBlank()) {
            log.error("============================================================");
            log.error("!!! AUTH_TOKEN_SECRET 未设置，服务将无法正常签发 token !!!");
            log.error("请设置环境变量 AUTH_TOKEN_SECRET 为至少 {} 字符的强随机值", MIN_SECRET_BYTES);
            log.error("============================================================");
            return;
        }

        if (KNOWN_DEV_SECRET.equals(tokenSecret)) {
            log.warn("============================================================");
            log.warn("!!! 检测到正在使用开发环境默认 token 密钥，部署到生产环境前请替换 !!!");
            log.warn("设置环境变量 AUTH_TOKEN_SECRET 为至少 {} 字符的强随机值", MIN_SECRET_BYTES);
            log.warn("============================================================");
            return;
        }

        int byteLength = tokenSecret.getBytes(StandardCharsets.UTF_8).length;
        if (byteLength < MIN_SECRET_BYTES) {
            log.warn("============================================================");
            log.warn("!!! token 密钥长度不足（当前 {} 字节，建议至少 {} 字节）!!!", byteLength, MIN_SECRET_BYTES);
            log.warn("请生成更强的随机密钥替换当前 AUTH_TOKEN_SECRET");
            log.warn("============================================================");
        }

        log.info("token 签名密钥校验通过（密钥长度: {} 字节）", byteLength);
    }
}
