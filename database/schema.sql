-- ============================================================
-- 面经宝典 - 数据库表结构
-- 数据库版本：MySQL 5.7+ / 8.0+
-- 字符集：utf8mb4（支持 emoji）
-- ============================================================

CREATE DATABASE IF NOT EXISTS interview_db
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

USE interview_db;

-- ------------------------------------------------------------
-- 1. 企业表
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `company` (
  `id`           INT UNSIGNED    NOT NULL AUTO_INCREMENT COMMENT '企业ID',
  `name`         VARCHAR(64)     NOT NULL                COMMENT '企业名称，如 百度',
  `short_name`   VARCHAR(16)     NOT NULL                COMMENT '企业简称，如 BD',
  `avatar_text`  VARCHAR(8)      NOT NULL DEFAULT ''     COMMENT '头像显示文字，如 百',
  `avatar_color` VARCHAR(16)     NOT NULL DEFAULT '#999' COMMENT '头像背景色（HEX）',
  `logo_url`     VARCHAR(256)    NOT NULL DEFAULT ''     COMMENT '企业 Logo URL（可选）',
  `description`  VARCHAR(512)    NOT NULL DEFAULT ''     COMMENT '企业简介',
  `created_at`   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='企业信息表';

-- ------------------------------------------------------------
-- 2. 分类表
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `category` (
  `id`         INT UNSIGNED   NOT NULL AUTO_INCREMENT COMMENT '分类ID',
  `code`       VARCHAR(32)    NOT NULL                COMMENT '分类代码，如 frontend / java',
  `name`       VARCHAR(32)    NOT NULL                COMMENT '分类名称，如 前端 / Java后端',
  `sort_order` TINYINT UNSIGNED NOT NULL DEFAULT 0    COMMENT '排序权重（越小越靠前）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='面经分类表';

-- ------------------------------------------------------------
-- 3. 标签表
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `tag` (
  `id`   INT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '标签ID',
  `name` VARCHAR(32)  NOT NULL                COMMENT '标签名称',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='标签表';

-- ------------------------------------------------------------
-- 4. 面经主表
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `interview` (
  `id`          INT UNSIGNED    NOT NULL AUTO_INCREMENT COMMENT '面经ID',
  `company_id`  INT UNSIGNED    NOT NULL                COMMENT '关联企业ID',
  `category_id` INT UNSIGNED    NOT NULL                COMMENT '关联分类ID',
  `title`       VARCHAR(128)    NOT NULL                COMMENT '面经标题',
  `author`      VARCHAR(64)     NOT NULL DEFAULT ''     COMMENT '作者昵称',
  `summary`     VARCHAR(512)    NOT NULL DEFAULT ''     COMMENT '摘要（列表页展示）',
  `content`     MEDIUMTEXT      NOT NULL                COMMENT '正文内容（Markdown格式）',
  `views`       INT UNSIGNED    NOT NULL DEFAULT 0      COMMENT '浏览量',
  `likes`       INT UNSIGNED    NOT NULL DEFAULT 0      COMMENT '点赞数',
  `collects`    INT UNSIGNED    NOT NULL DEFAULT 0      COMMENT '收藏数',
  `publish_date` DATE           NOT NULL                COMMENT '发布日期',
  `status`      TINYINT         NOT NULL DEFAULT 1      COMMENT '状态：1=发布 0=草稿 -1=删除',
  `created_at`  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_company` (`company_id`),
  KEY `idx_category` (`category_id`),
  KEY `idx_publish_date` (`publish_date`),
  KEY `idx_likes` (`likes`),
  KEY `idx_views` (`views`),
  CONSTRAINT `fk_interview_company`  FOREIGN KEY (`company_id`)  REFERENCES `company`(`id`),
  CONSTRAINT `fk_interview_category` FOREIGN KEY (`category_id`) REFERENCES `category`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='面经主表';

-- ------------------------------------------------------------
-- 5. 面经-标签 多对多关联表
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `interview_tag` (
  `interview_id` INT UNSIGNED NOT NULL COMMENT '面经ID',
  `tag_id`       INT UNSIGNED NOT NULL COMMENT '标签ID',
  PRIMARY KEY (`interview_id`, `tag_id`),
  KEY `idx_tag` (`tag_id`),
  CONSTRAINT `fk_it_interview` FOREIGN KEY (`interview_id`) REFERENCES `interview`(`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_it_tag`       FOREIGN KEY (`tag_id`)       REFERENCES `tag`(`id`)       ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='面经标签关联表';

-- ------------------------------------------------------------
-- 6. 用户行为表（点赞/收藏，可选扩展）
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `user_action` (
  `id`           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '行为ID',
  `user_id`      INT UNSIGNED    NOT NULL DEFAULT 0      COMMENT '用户ID（0=游客）',
  `interview_id` INT UNSIGNED    NOT NULL                COMMENT '面经ID',
  `action_type`  TINYINT         NOT NULL                COMMENT '行为类型：1=点赞 2=收藏',
  `created_at`   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_interview_action` (`user_id`, `interview_id`, `action_type`),
  KEY `idx_interview` (`interview_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户行为表';
