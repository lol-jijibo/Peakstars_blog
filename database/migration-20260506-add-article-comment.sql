-- ------------------------------------------------------------
-- 技术文章评论表
-- 业务目的：支撑技术文章详情页底部的评论功能，支持多级回复
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `article_comment` (
  `id`             BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT COMMENT '评论主键ID',
  `article_key`    VARCHAR(64)      NOT NULL                COMMENT '关联文章业务主键',
  `nickname`       VARCHAR(64)      NOT NULL DEFAULT '匿名用户' COMMENT '评论者昵称',
  `content`        TEXT             NOT NULL                COMMENT '评论内容',
  `avatar_text`    VARCHAR(16)      NOT NULL DEFAULT '匿'   COMMENT '头像显示文字缩写',
  `avatar_accent`  VARCHAR(255)     NOT NULL DEFAULT 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)' COMMENT '头像背景渐变',
  `parent_id`      BIGINT UNSIGNED  NULL     DEFAULT NULL   COMMENT '父评论ID（NULL=顶级评论）',
  `like_count`     INT UNSIGNED     NOT NULL DEFAULT 0      COMMENT '点赞数',
  `status`         TINYINT          NOT NULL DEFAULT 1      COMMENT '状态：1=正常 0=隐藏 -1=删除',
  `created_at`     DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`     DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_article_comment_key` (`article_key`),
  KEY `idx_article_comment_parent` (`parent_id`),
  KEY `idx_article_comment_created` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='技术文章评论表';
