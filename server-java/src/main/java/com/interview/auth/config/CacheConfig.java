package com.interview.auth.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 启用 Spring 方法级缓存，基于 ConcurrentHashMap 实现进程内轻量缓存。
 * 适用于 StarRead 首页、管理员仪表盘等读多写少的场景。
 */
@Configuration
@EnableCaching
public class CacheConfig {

    public static final String CACHE_STAR_READ = "starReadArticles";
    public static final String CACHE_ADMIN_SUMMARY = "adminSummary";
    public static final String CACHE_ADMIN_HOT = "adminHotContents";

    @Bean
    public CacheManager cacheManager() {
        return new ConcurrentMapCacheManager(
            CACHE_STAR_READ,
            CACHE_ADMIN_SUMMARY,
            CACHE_ADMIN_HOT
        );
    }
}
