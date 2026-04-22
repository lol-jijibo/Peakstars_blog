package com.interview.auth.service.impl;

import com.interview.auth.common.BusinessException;
import com.interview.auth.common.RedisKeyConstants;
import com.interview.auth.config.AuthVerificationProperties;
import com.interview.auth.domain.dto.response.VerificationCodeSendResponse;
import com.interview.auth.service.EmailVerificationService;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * 邮箱验证服务实现类
 * 负责发送和验证注册验证码，包含频率限制、防刷机制
 */
@Service
@RequiredArgsConstructor
public class EmailVerificationServiceImpl implements EmailVerificationService {

    // 安全随机数生成器，用于生成验证码
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    // 日期格式化器，用于生成每日计数的key
    private static final DateTimeFormatter DAY_FORMATTER = DateTimeFormatter.BASIC_ISO_DATE;

    // Spring邮件发送器
    private final JavaMailSender javaMailSender;
    // Redis操作模板
    private final StringRedisTemplate stringRedisTemplate;
    // 验证码配置属性
    private final AuthVerificationProperties verificationProperties;
    
    @Override
    public VerificationCodeSendResponse sendRegisterCode(String email, String clientIp) {
        String normalizedEmail = normalizeEmail(email);
        String normalizedIp = normalizeClientIp(clientIp);

        ensureQqEmail(normalizedEmail);
        ensureNotCoolingDown(normalizedEmail);
        ensureDailyLimit(
            buildDailyEmailCountKey(normalizedEmail),
            verificationProperties.getRegisterMaxSendPerEmailPerDay(),
            "Daily send limit reached for this email"
        );
        ensureDailyLimit(
            buildDailyIpCountKey(normalizedIp),
            verificationProperties.getRegisterMaxSendPerIpPerDay(),
            "Too many requests from the current network"
        );

        String code = generateCode(verificationProperties.getRegisterCodeLength());
        Duration codeTtl = Duration.ofSeconds(verificationProperties.getRegisterCodeExpireSeconds());

        try {
            stringRedisTemplate.opsForValue().set(buildCodeKey(normalizedEmail), code, codeTtl);
            stringRedisTemplate.delete(buildAttemptKey(normalizedEmail));
            sendRegisterCodeEmail(normalizedEmail, code);
            stringRedisTemplate.opsForValue().set(
                buildCooldownKey(normalizedEmail),
                "1",
                Duration.ofSeconds(verificationProperties.getRegisterSendCooldownSeconds())
            );
            incrementDailyCount(buildDailyEmailCountKey(normalizedEmail));
            incrementDailyCount(buildDailyIpCountKey(normalizedIp));
        } catch (MailException exception) {
            cleanupRegisterCode(normalizedEmail);
            throw new BusinessException(500, "Failed to send verification code");
        } catch (RuntimeException exception) {
            cleanupRegisterCode(normalizedEmail);
            throw new BusinessException(500, "Verification code service is unavailable");
        }

        VerificationCodeSendResponse response = new VerificationCodeSendResponse();
        response.setExpiresInSeconds(verificationProperties.getRegisterCodeExpireSeconds());
        response.setCooldownSeconds(verificationProperties.getRegisterSendCooldownSeconds());
        return response;
    }

    @Override
    public void verifyRegisterCode(String email, String code) {
        String normalizedEmail = normalizeEmail(email);
        String inputCode = code == null ? "" : code.trim();
        String codeKey = buildCodeKey(normalizedEmail);
        String attemptKey = buildAttemptKey(normalizedEmail);

        String cachedCode = stringRedisTemplate.opsForValue().get(codeKey);
        if (cachedCode == null || cachedCode.isBlank()) {
            throw new BusinessException(400, "Verification code expired. Please request a new one.");
        }

        Long currentAttempts = stringRedisTemplate.opsForValue().increment(attemptKey);
        if (currentAttempts != null && currentAttempts == 1L) {
            stringRedisTemplate.expire(
                attemptKey,
                Duration.ofSeconds(verificationProperties.getRegisterCodeExpireSeconds())
            );
        }

        if (currentAttempts != null && currentAttempts > verificationProperties.getRegisterMaxVerifyAttempts()) {
            cleanupRegisterCode(normalizedEmail);
            throw new BusinessException(400, "Too many invalid attempts. Please request a new code.");
        }

        if (!isEqual(cachedCode, inputCode)) {
            long remainingAttempts = verificationProperties.getRegisterMaxVerifyAttempts() - (currentAttempts == null ? 0L : currentAttempts);
            if (remainingAttempts <= 0) {
                cleanupRegisterCode(normalizedEmail);
                throw new BusinessException(400, "Too many invalid attempts. Please request a new code.");
            }
            throw new BusinessException(400, "Invalid verification code. Remaining attempts: " + remainingAttempts);
        }

        stringRedisTemplate.delete(codeKey);
        stringRedisTemplate.delete(attemptKey);
    }

    /**
     * 发送注册验证码邮件
     * 业务逻辑：构造邮件对象并通过 JavaMailSender 发送
     *
     * @param email 收件人邮箱
     * @param code 验证码
     * @throws MailException 邮件发送失败时抛出
     */
    private void sendRegisterCodeEmail(String email, String code) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(verificationProperties.getMailFrom());
        message.setTo(email);
        message.setSubject(verificationProperties.getRegisterMailSubject());
        message.setText(buildRegisterMailContent(code));
        javaMailSender.send(message);
    }

    /**
     * 构建注册验证码邮件内容
     * 业务逻辑：生成包含欢迎语、验证码、有效期和安全提示的邮件正文
     *
     * @param code 验证码
     * @return 邮件正文内容
     */
    private String buildRegisterMailContent(String code) {
        return """
            欢迎进入 PeakStars 的博客！

            您正在注册 PeakStars_blog 账户。
            【验证码】%s
            【有效期】%d 分钟

            安全提示：
            1. 此验证码仅可使用一次
            2. 请勿将验证码透露给他人
            3. 如非本人操作，请忽略此邮件
            ------------------------
            PeakStars 技术团队
            """.formatted(code, verificationProperties.getRegisterCodeExpireSeconds() / 60);
    }

    /**
     * 确保邮箱是QQ邮箱
     * 业务逻辑：检查邮箱后缀是否为 @qq.com，不是则抛出异常
     *
     * @param email 邮箱地址
     * @throws BusinessException 如果不是QQ邮箱则抛出异常
     */
    private void ensureQqEmail(String email) {
        if (!email.endsWith("@qq.com")) {
            throw new BusinessException(400, "Only QQ mailboxes are supported");
        }
    }

    /**
     * 确保不在冷却时间内
     * 业务逻辑：检查 Redis 中是否存在冷却标记，存在则说明60秒内已发送过，抛出异常
     *
     * @param email 邮箱地址
     * @throws BusinessException 如果在冷却时间内则抛出异常，提示剩余秒数
     */
    private void ensureNotCoolingDown(String email) {
        String cooldownKey = buildCooldownKey(email);
        if (Boolean.TRUE.equals(stringRedisTemplate.hasKey(cooldownKey))) {
            long retryAfterSeconds = getRemainingSeconds(cooldownKey);
            throw new BusinessException(429, "Verification code already sent. Retry after " + retryAfterSeconds + " seconds.");
        }
    }

    /**
     * 确保未超过每日发送限制
     * 业务逻辑：从 Redis 读取今日发送次数，如果达到或超过限制则抛出异常
     *
     * @param key Redis计数key
     * @param limit 限制次数
     * @param message 超限时的错误提示
     * @throws BusinessException 如果超过限制则抛出异常
     */
    private void ensureDailyLimit(String key, int limit, String message) {
        String count = stringRedisTemplate.opsForValue().get(key);
        if (count != null && Long.parseLong(count) >= limit) {
            throw new BusinessException(429, message);
        }
    }

    /**
     * 增加每日发送计数
     * 业务逻辑：Redis 计数器自增，如果是当天第一次发送，设置过期时间到次日凌晨1点
     *
     * @param key Redis计数key
     */
    private void incrementDailyCount(String key) {
        Long count = stringRedisTemplate.opsForValue().increment(key);
        if (count != null && count == 1L) {
            Duration ttl = Duration.between(LocalDateTime.now(), LocalDate.now().plusDays(1).atTime(LocalTime.MIN)).plusHours(1);
            stringRedisTemplate.expire(key, ttl);
        }
    }

    /**
     * 获取Redis key的剩余有效时间（秒）
     * 业务逻辑：查询 Redis key 的 TTL，如果获取失败则返回默认冷却时间
     *
     * @param key Redis key
     * @return 剩余秒数，如果获取失败则返回默认冷却时间
     */
    private long getRemainingSeconds(String key) {
        Long ttl = stringRedisTemplate.getExpire(key, TimeUnit.SECONDS);
        return ttl == null || ttl < 0 ? verificationProperties.getRegisterSendCooldownSeconds() : ttl;
    }

    /**
     * 清理注册验证码相关数据
     * 业务逻辑：删除 Redis 中的验证码和尝试次数记录，用于验证失败或异常时的清理
     *
     * @param email 邮箱地址
     */
    private void cleanupRegisterCode(String email) {
        stringRedisTemplate.delete(buildCodeKey(email));
        stringRedisTemplate.delete(buildAttemptKey(email));
    }

    /**
     * 安全比对两个验证码是否相等
     * 业务逻辑：使用 MessageDigest.isEqual 进行常量时间比较，防止时序攻击
     *
     * @param cachedCode 缓存的验证码
     * @param inputCode 用户输入的验证码
     * @return 是否相等
     */
    private boolean isEqual(String cachedCode, String inputCode) {
        return MessageDigest.isEqual(
            cachedCode.getBytes(StandardCharsets.UTF_8),
            inputCode.getBytes(StandardCharsets.UTF_8)
        );
    }

    /**
     * 规范化邮箱地址
     * 业务逻辑：去除空格并转为小写，确保邮箱格式统一
     *
     * @param email 原始邮箱
     * @return 规范化后的邮箱
     */
    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }

    /**
     * 规范化客户端IP地址
     * 业务逻辑：去除空格，空值返回"unknown"，用于防刷统计
     *
     * @param clientIp 原始IP
     * @return 规范化后的IP
     */
    private String normalizeClientIp(String clientIp) {
        return (clientIp == null || clientIp.isBlank()) ? "unknown" : clientIp.trim();
    }

    /**
     * 生成指定长度的数字验证码
     * 业务逻辑：使用安全随机数生成器生成指定位数的纯数字验证码
     *
     * @param codeLength 验证码长度
     * @return 数字验证码字符串
     */
    private String generateCode(int codeLength) {
        int max = (int) Math.pow(10, codeLength);
        int min = (int) Math.pow(10, codeLength - 1);
        return String.valueOf(SECURE_RANDOM.nextInt(max - min) + min);
    }

    /**
     * 构建验证码存储的Redis key
     * 业务逻辑：拼接前缀和邮箱地址，生成唯一的验证码存储key
     *
     * @param email 邮箱地址
     * @return Redis key
     */
    private String buildCodeKey(String email) {
        return RedisKeyConstants.Register.CODE_PREFIX + email;
    }

    /**
     * 构建验证尝试次数的Redis key
     * 业务逻辑：拼接前缀和邮箱地址，用于记录验证失败次数
     *
     * @param email 邮箱地址
     * @return Redis key
     */
    private String buildAttemptKey(String email) {
        return RedisKeyConstants.Register.ATTEMPT_PREFIX + email;
    }

    /**
     * 构建发送冷却时间的Redis key
     * 业务逻辑：拼接前缀和邮箱地址，用于标记60秒冷却期
     *
     * @param email 邮箱地址
     * @return Redis key
     */
    private String buildCooldownKey(String email) {
        return RedisKeyConstants.Register.COOLDOWN_PREFIX + email;
    }

    /**
     * 构建每日邮箱发送次数的Redis key
     * 业务逻辑：拼接前缀、邮箱地址和日期，用于统计单个邮箱每天的发送次数
     *
     * @param email 邮箱地址
     * @return Redis key，包含日期后缀，每天自动重置
     */
    private String buildDailyEmailCountKey(String email) {
        return RedisKeyConstants.Register.DAILY_EMAIL_PREFIX + email + ":" + LocalDate.now().format(DAY_FORMATTER);
    }

    /**
     * 构建每日IP发送次数的Redis key
     * 业务逻辑：拼接前缀、IP地址和日期，用于统计单个IP每天的发送次数，防止恶意攻击
     *
     * @param clientIp 客户端IP地址
     * @return Redis key，包含日期后缀，每天自动重置
     */
    private String buildDailyIpCountKey(String clientIp) {
        return RedisKeyConstants.Register.DAILY_IP_PREFIX + clientIp + ":" + LocalDate.now().format(DAY_FORMATTER);
    }
}
