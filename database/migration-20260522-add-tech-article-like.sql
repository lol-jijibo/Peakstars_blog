-- ------------------------------------------------------------
-- 技术文章用户点赞记录
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `tech_article_like` (
  `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '点赞记录主键ID',
  `user_id`     BIGINT UNSIGNED NOT NULL                COMMENT '关联 auth_user 主键',
  `article_key` VARCHAR(64)     NOT NULL                COMMENT '关联 tech_article.article_key',
  `created_at`  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '点赞时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tech_article_like_user_article` (`user_id`, `article_key`),
  KEY `idx_tech_article_like_article` (`article_key`),
  KEY `idx_tech_article_like_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='技术文章用户点赞记录表';

