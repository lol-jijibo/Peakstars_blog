package com.interview.auth.service;

import com.interview.auth.common.BusinessException;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * 评论 IP 级别频率限制服务。
 * 基于 Redis 实现同一 IP 在时间窗口内的评论次数计数，防止刷评滥用。
 * Redis 不可用时自动降级，仅记录警告不阻断评论流程。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CommentRateLimitService {

    private static final String KEY_PREFIX = "comment:rate-limit:ip:";
    private static final int MAX_COMMENTS_PER_WINDOW = 10;
    private static final Duration WINDOW_DURATION = Duration.ofMinutes(1);

    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 检查指定 IP 是否超出评论频率限制。
     *
     * @param clientIp 客户端 IP
     * @throws BusinessException 如果超出频率限制
     */
    public void checkRateLimit(String clientIp) {
        try {
            String key = KEY_PREFIX + clientIp;
            Long count = stringRedisTemplate.opsForValue().increment(key);
            if (count != null && count == 1L) {
                stringRedisTemplate.expire(key, WINDOW_DURATION);
            }
            if (count != null && count > MAX_COMMENTS_PER_WINDOW) {
                throw new BusinessException(429, "评论过于频繁，请稍后再试");
            }
        } catch (DataAccessException e) {
            log.warn("Redis 不可用，跳过评论频率限制: {}", e.getMessage());
        }
    }
}
