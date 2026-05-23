package com.interview.auth.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.HexFormat;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 密码服务。
 * 使用 BCrypt 进行密码哈希，同时兼容旧版 SHA-256 哈希的自动迁移。
 */
@Component
public class PasswordService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final BCryptPasswordEncoder BCRYPT = new BCryptPasswordEncoder();

    /**
     * 对明文密码进行 BCrypt 哈希。
     */
    public String encode(String rawPassword) {
        return BCRYPT.encode(rawPassword);
    }

    /**
     * 校验明文密码与已存储密码是否匹配。
     * 兼容旧版 SHA-256 哈希格式（salt:hash），并使用常量时间比较防止时序攻击。
     */
    public boolean matches(String rawPassword, String encodedPassword) {
        if (encodedPassword == null) {
            return false;
        }

        if (isLegacyHash(encodedPassword)) {
            return matchesLegacy(rawPassword, encodedPassword);
        }

        return BCRYPT.matches(rawPassword, encodedPassword);
    }

    /**
     * 判断存储的密码是否为旧版 SHA-256 格式（salt:hash）。
     * 旧版密码在登录成功后应调用 {@link #encode(String)} 升级为 BCrypt。
     */
    public boolean isLegacyHash(String encodedPassword) {
        return encodedPassword != null && encodedPassword.contains(":") && !encodedPassword.startsWith("$");
    }

    private boolean matchesLegacy(String rawPassword, String encodedPassword) {
        String[] parts = encodedPassword.split(":", 2);
        if (parts.length != 2) {
            return false;
        }
        String expectedHash = sha256(parts[0] + rawPassword);
        return MessageDigest.isEqual(
            expectedHash.getBytes(StandardCharsets.UTF_8),
            parts[1].getBytes(StandardCharsets.UTF_8)
        );
    }

    private String sha256(String content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(content.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (Exception exception) {
            throw new IllegalStateException("密码加密失败", exception);
        }
    }

    /**
     * 生成旧版 SHA-256 格式的盐值（仅用于兼容校验，新密码不再使用）。
     */
    public String generateSalt() {
        byte[] bytes = new byte[16];
        SECURE_RANDOM.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }
}
