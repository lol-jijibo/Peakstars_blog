package com.interview.auth.service;

import com.interview.auth.common.BusinessException;
import com.interview.auth.common.RedisKeyConstants;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * 登录频率限制服务。
 * 基于 Redis 实现登录失败次数计数与锁定，防止暴力破解。
 * 同一账号+IP 在 15 分钟内失败 5 次后锁定 15 分钟。
 * Redis 不可用时自动降级，仅记录警告不阻断登录流程。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LoginRateLimitService {

    private static final int MAX_FAILURES = 5;
    private static final Duration LOCK_DURATION = Duration.ofMinutes(15);

    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 检查登录是否被锁定。
     * Redis 不可用时降级跳过限流检查，避免因缓存故障导致全员无法登录。
     *
     * @param account 登录账号（邮箱或用户名）
     * @param clientIp 客户端 IP
     * @throws BusinessException 如果当前账号+IP 已被锁定
     */
    public void checkNotLocked(String account, String clientIp) {
        try {
            String lockKey = buildLockKey(account, clientIp);
            if (Boolean.TRUE.equals(stringRedisTemplate.hasKey(lockKey))) {
                long remainingSeconds = getRemainingSeconds(lockKey);
                throw new BusinessException(429,
                    "操作过于频繁，请在 " + formatRemaining(remainingSeconds) + " 后重试");
            }
        } catch (DataAccessException e) {
            log.warn("Redis 不可用，跳过登录限流检查: {}", e.getMessage());
        }
    }

    /**
     * 登录成功后清除该账号+IP 的失败计数和锁定状态，避免正常用户在锁定窗口外的下一次登录被误拦。
     */
    public void onLoginSuccess(String account, String clientIp) {
        try {
            stringRedisTemplate.delete(buildFailCountKey(account, clientIp));
            stringRedisTemplate.delete(buildLockKey(account, clientIp));
        } catch (DataAccessException e) {
            log.warn("Redis 不可用，跳过登录成功计数清除: {}", e.getMessage());
        }
    }

    /**
     * 登录失败后递增失败计数，达到阈值时锁定。
     */
    public void onLoginFailure(String account, String clientIp) {
        try {
            String failKey = buildFailCountKey(account, clientIp);
            Long count = stringRedisTemplate.opsForValue().increment(failKey);
            if (count != null && count == 1L) {
                stringRedisTemplate.expire(failKey, LOCK_DURATION);
            }
            if (count != null && count >= MAX_FAILURES) {
                stringRedisTemplate.opsForValue().set(buildLockKey(account, clientIp), "1", LOCK_DURATION);
            }
        } catch (DataAccessException e) {
            log.warn("Redis 不可用，跳过登录失败计数: {}", e.getMessage());
        }
    }

    private String buildFailCountKey(String account, String clientIp) {
        return RedisKeyConstants.Login.FAIL_COUNT_PREFIX + account + ":" + clientIp;
    }

    private String buildLockKey(String account, String clientIp) {
        return RedisKeyConstants.Login.LOCKED_PREFIX + account + ":" + clientIp;
    }

    private long getRemainingSeconds(String key) {
        Long ttl = stringRedisTemplate.getExpire(key);
        return ttl == null || ttl < 0 ? LOCK_DURATION.toSeconds() : ttl;
    }

    private String formatRemaining(long seconds) {
        if (seconds >= 60) {
            return (seconds / 60) + " 分钟";
        }
        return seconds + " 秒";
    }
}
