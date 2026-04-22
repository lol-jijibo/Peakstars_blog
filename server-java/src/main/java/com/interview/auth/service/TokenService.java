package com.interview.auth.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.interview.auth.common.BusinessException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Token 服务。
 * 作用：签发和校验登录 token，供前端在前后端分离模式下保存真实登录态。
 */
@Component
public class TokenService {

    /**
     * JSON 序列化工具。
     * 作用：把 token payload 在 Map 和 JSON 字符串之间转换。
     */
    private final ObjectMapper objectMapper;

    /**
     * token 签名密钥。
     * 作用：参与 HMAC 签名，防止 token 被篡改。
     */
    private final String tokenSecret;

    /**
     * token 过期小时数。
     * 作用：控制登录态最长有效时长。
     */
    private final Integer tokenExpireHours;

    public TokenService(
        ObjectMapper objectMapper,
        @Value("${app.auth.token-secret}") String tokenSecret,
        @Value("${app.auth.token-expire-hours}") Integer tokenExpireHours
    ) {
        this.objectMapper = objectMapper;
        this.tokenSecret = tokenSecret;
        this.tokenExpireHours = tokenExpireHours;
    }

    /**
     * 生成登录 token。
     * 说明：这里实现的是轻量自定义 token 格式，结构为 {@code payload.signature}。
     *
     * @param userId 用户主键 ID
     * @param email 用户邮箱
     * @param username 用户名
     * @return 可直接返回给前端保存的 token 字符串
     */
    public String generateToken(Long userId, String email, String username) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("userId", userId);
            payload.put("email", email);
            payload.put("username", username);
            payload.put("exp", Instant.now().plusSeconds(tokenExpireHours.longValue() * 3600L).toEpochMilli());

            String payloadJson = objectMapper.writeValueAsString(payload);
            String encodedPayload = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(payloadJson.getBytes(StandardCharsets.UTF_8));
            String signature = sign(encodedPayload);
            return encodedPayload + "." + signature;
        } catch (Exception exception) {
            throw new IllegalStateException("生成 token 失败", exception);
        }
    }

    /**
     * 解析并校验 Bearer token。
     * 校验内容：token 结构、签名、过期时间。
     *
     * @param bearerToken 请求头中的 Authorization 值
     * @return 解析后的 token 载荷数据
     */
    public Map<String, Object> parseToken(String bearerToken) {
        try {
            String token = bearerToken == null ? "" : bearerToken.replace("Bearer ", "").trim();
            if (!token.contains(".")) {
                throw new BusinessException(401, "缺少有效登录凭证");
            }

            String[] parts = token.split("\\.", 2);
            String encodedPayload = parts[0];
            String signature = parts[1];
            if (!signature.equals(sign(encodedPayload))) {
                throw new BusinessException(401, "登录凭证校验失败");
            }

            String json = new String(Base64.getUrlDecoder().decode(encodedPayload), StandardCharsets.UTF_8);
            Map<String, Object> payload = objectMapper.readValue(json, new TypeReference<>() {});
            long expireAt = Long.parseLong(String.valueOf(payload.get("exp")));
            if (expireAt < Instant.now().toEpochMilli()) {
                throw new BusinessException(401, "登录状态已过期，请重新登录");
            }
            return payload;
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new BusinessException(401, "登录凭证解析失败");
        }
    }

    /**
     * 对 token 载荷做 HMAC-SHA256 签名。
     *
     * @param content 已编码的 payload 内容
     * @return Base64 URL Safe 的签名结果
     */
    private String sign(String content) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(tokenSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] signBytes = mac.doFinal(content.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(signBytes);
        } catch (Exception exception) {
            throw new IllegalStateException("token 签名失败", exception);
        }
    }
}
