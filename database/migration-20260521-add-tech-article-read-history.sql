USE interview_db;

CREATE TABLE IF NOT EXISTS `tech_article_read_history` (
  `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '阅读记录主键ID',
  `user_id`       BIGINT UNSIGNED NOT NULL                COMMENT '关联 auth_user 主键',
  `article_key`   VARCHAR(64)     NOT NULL                COMMENT '关联技术文章业务主键',
  `read_count`    INT UNSIGNED    NOT NULL DEFAULT 1      COMMENT '当前用户阅读该文章的累计次数',
  `last_read_at`  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '最近一次阅读时间',
  `created_at`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tech_article_user_history` (`user_id`, `article_key`),
  KEY `idx_tech_article_history_article` (`article_key`),
  KEY `idx_tech_article_history_last_read` (`last_read_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='技术文章用户阅读记录表';
