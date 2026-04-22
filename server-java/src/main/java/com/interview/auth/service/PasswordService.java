package com.interview.auth.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.HexFormat;
import org.springframework.stereotype.Component;

/**
 * 密码服务。
 * 作用：负责密码加密和密码校验，避免控制器或业务层直接操作明文密码。
 */
@Component
public class PasswordService {

    /**
     * 安全随机数生成器。
     * 作用：为每个密码生成独立盐值，降低相同密码得到相同哈希的风险。
     */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    /**
     * 对明文密码进行加盐哈希。
     *
     * @param rawPassword 用户输入的明文密码
     * @return 形如 {@code salt:hash} 的持久化结果
     */
    public String encode(String rawPassword) {
        String salt = generateSalt();
        String hash = sha256(salt + rawPassword);
        return salt + ":" + hash;
    }

    /**
     * 校验明文密码与已存储密码是否匹配。
     *
     * @param rawPassword 登录时输入的明文密码
     * @param encodedPassword 数据库存储的加盐哈希结果
     * @return 匹配返回 true，否则返回 false
     */
    public boolean matches(String rawPassword, String encodedPassword) {
        if (encodedPassword == null || !encodedPassword.contains(":")) {
            return false;
        }
        String[] parts = encodedPassword.split(":", 2);
        String salt = parts[0];
        String expectedHash = parts[1];
        return expectedHash.equals(sha256(salt + rawPassword));
    }

    /**
     * 生成盐值。
     *
     * @return 16 字节随机数对应的十六进制字符串
     */
    private String generateSalt() {
        byte[] bytes = new byte[16];
        SECURE_RANDOM.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    /**
     * 计算 SHA-256 哈希。
     *
     * @param content 待哈希内容
     * @return 十六进制哈希字符串
     */
    private String sha256(String content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(content.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (Exception exception) {
            throw new IllegalStateException("密码加密失败", exception);
        }
    }
}
