package com.interview.auth.common;

/**
 * Redis Key 常量类
 * 统一管理所有 Redis key 的前缀，避免硬编码和重复定义
 */
public final class RedisKeyConstants {

    // 防止实例化
    private RedisKeyConstants() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    /**
     * 注册验证码相关 Key 前缀
     */
    public static final class Register {

        private Register() {
            throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
        }

        /**
         * 注册验证码存储
         * 格式：auth:register:email-code:{email}
         * 示例：auth:register:email-code:1843599766@qq.com
         * 值：6位数字验证码
         * 过期时间：5分钟
         */
        public static final String CODE_PREFIX = "auth:register:email-code:";

        /**
         * 验证尝试次数记录
         * 格式：auth:register:verify-attempts:{email}
         * 示例：auth:register:verify-attempts:1843599766@qq.com
         * 值：尝试次数（数字）
         * 过期时间：5分钟（与验证码同步）
         */
        public static final String ATTEMPT_PREFIX = "auth:register:verify-attempts:";

        /**
         * 发送冷却时间标记
         * 格式：auth:register:send-cooldown:{email}
         * 示例：auth:register:send-cooldown:1843599766@qq.com
         * 值：1（标记位）
         * 过期时间：60秒
         */
        public static final String COOLDOWN_PREFIX = "auth:register:send-cooldown:";

        /**
         * 每日邮箱发送次数统计
         * 格式：auth:register:send-count:email:{email}:{date}
         * 示例：auth:register:send-count:email:1843599766@qq.com:20260421
         * 值：发送次数（数字）
         * 过期时间：到次日凌晨1点
         */
        public static final String DAILY_EMAIL_PREFIX = "auth:register:send-count:email:";

        /**
         * 每日IP发送次数统计
         * 格式：auth:register:send-count:ip:{ip}:{date}
         * 示例：auth:register:send-count:ip:192.168.1.1:20260421
         * 值：发送次数（数字）
         * 过期时间：到次日凌晨1点
         */
        public static final String DAILY_IP_PREFIX = "auth:register:send-count:ip:";
    }

    /**
     * 登录相关 Key 前缀（预留扩展）
     */
    public static final class Login {

        private Login() {
            throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
        }

        // 可以在这里添加登录相关的 Redis key 前缀
        // 例如：登录失败次数、Token 黑名单等
    }

    /**
     * 会话相关 Key 前缀（预留扩展）
     */
    public static final class Session {

        private Session() {
            throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
        }

        // 可以在这里添加会话相关的 Redis key 前缀
        // 例如：用户在线状态、会话信息等
    }
}
