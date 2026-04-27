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

-- ------------------------------------------------------------
-- 7. 技术文章表
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `tech_article` (
  `id`                BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '技术文章主键ID',
  `article_key`       VARCHAR(64)     NOT NULL                COMMENT '技术文章业务主键，前端使用该字段做稳定路由与渲染 key',
  `category`          VARCHAR(32)     NOT NULL                COMMENT '文章分类，如 frontend / backend',
  `title`             VARCHAR(255)    NOT NULL                COMMENT '文章标题',
  `summary`           VARCHAR(1000)   NOT NULL DEFAULT ''     COMMENT '文章摘要，用于列表卡片文案',
  `essence`           VARCHAR(1000)   NOT NULL DEFAULT ''     COMMENT '文章精华摘要，用于导航下拉预览',
  `highlight_list`    VARCHAR(1000)   NOT NULL DEFAULT ''     COMMENT '文章亮点列表，使用竖线分隔，便于后端转换为数组',
  `author_name`       VARCHAR(64)     NOT NULL                COMMENT '作者名称',
  `author_role`       VARCHAR(128)    NOT NULL DEFAULT ''     COMMENT '作者身份说明',
  `author_initials`   VARCHAR(16)     NOT NULL DEFAULT ''     COMMENT '作者头像文字缩写',
  `author_accent`     VARCHAR(255)    NOT NULL DEFAULT ''     COMMENT '作者头像渐变背景',
  `cover_url`         VARCHAR(255)    NOT NULL DEFAULT ''     COMMENT '文章封面地址',
  `published_at`      DATETIME        NOT NULL                COMMENT '发布时间，用于排序和前端展示',
  `read_count`        INT UNSIGNED    NOT NULL DEFAULT 0      COMMENT '阅读数',
  `like_count`        INT UNSIGNED    NOT NULL DEFAULT 0      COMMENT '点赞数',
  `collect_count`     INT UNSIGNED    NOT NULL DEFAULT 0      COMMENT '收藏数',
  `comment_count`     INT UNSIGNED    NOT NULL DEFAULT 0      COMMENT '评论数',
  `read_time`         VARCHAR(32)     NOT NULL DEFAULT ''     COMMENT '阅读时长文案，如 8 min',
  `is_vip`            TINYINT(1)      NOT NULL DEFAULT 0      COMMENT '是否为 VIP 文章',
  `is_collected`      TINYINT(1)      NOT NULL DEFAULT 0      COMMENT '是否加入收藏夹，用于前端演示态',
  `is_liked`          TINYINT(1)      NOT NULL DEFAULT 0      COMMENT '是否已点赞，用于前端演示态',
  `in_history`        TINYINT(1)      NOT NULL DEFAULT 0      COMMENT '是否出现在最近浏览历史',
  `featured`          TINYINT(1)      NOT NULL DEFAULT 0      COMMENT '是否为精选内容，用于头部和置顶展示',
  `status`            TINYINT         NOT NULL DEFAULT 1      COMMENT '状态：1=发布 0=下线',
  `sort_order`        INT             NOT NULL DEFAULT 0      COMMENT '业务排序权重，值越小越靠前',
  `created_at`        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tech_article_key` (`article_key`),
  KEY `idx_tech_article_category` (`category`),
  KEY `idx_tech_article_publish` (`published_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='技术文章内容表';

-- ------------------------------------------------------------
-- 8. 看天下期刊表
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `world_news_issue` (
  `id`                BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '看天下期刊主键ID',
  `issue_key`         VARCHAR(64)     NOT NULL                COMMENT '期刊业务主键，前端列表使用该字段做稳定 key',
  `title`             VARCHAR(255)    NOT NULL                COMMENT '期刊标题',
  `issue_label`       VARCHAR(32)     NOT NULL                COMMENT '期号标签，如 2026.08',
  `category`          VARCHAR(32)     NOT NULL DEFAULT '看天下' COMMENT '栏目分类文案',
  `today_reads`       INT UNSIGNED    NOT NULL DEFAULT 0      COMMENT '今日阅读人数',
  `recommendation`    DECIMAL(5,1)    NOT NULL DEFAULT 0.0    COMMENT '推荐值百分比',
  `description`       VARCHAR(1000)   NOT NULL DEFAULT ''     COMMENT '期刊说明文案',
  `cover_accent`      VARCHAR(32)     NOT NULL DEFAULT ''     COMMENT '封面主题色',
  `cover_kicker`      VARCHAR(64)     NOT NULL DEFAULT ''     COMMENT '封面导语',
  `cover_headline`    VARCHAR(255)    NOT NULL DEFAULT ''     COMMENT '封面主标题',
  `cover_summary`     VARCHAR(1000)   NOT NULL DEFAULT ''     COMMENT '封面摘要',
  `cover_footer`      VARCHAR(255)    NOT NULL DEFAULT ''     COMMENT '封面底部说明',
  `published_at`      DATETIME        NOT NULL                COMMENT '期刊发布时间，用于排序',
  `status`            TINYINT         NOT NULL DEFAULT 1      COMMENT '状态：1=发布 0=下线',
  `sort_order`        INT             NOT NULL DEFAULT 0      COMMENT '业务排序权重，值越小越靠前',
  `created_at`        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_world_news_issue_key` (`issue_key`),
  KEY `idx_world_news_publish` (`published_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='看天下期刊内容表';

-- ------------------------------------------------------------
-- 9. AI 热点表
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `ai_hotspot` (
  `id`                BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'AI 热点主键ID',
  `hotspot_key`       VARCHAR(64)     NOT NULL                COMMENT '热点业务主键，前端列表使用该字段做稳定 key',
  `track`             VARCHAR(32)     NOT NULL                COMMENT '热点赛道，如 agent / multimodal / infra',
  `hotspot_type`      VARCHAR(32)     NOT NULL                COMMENT '热点类型，当前与赛道保持一致，预留后续细分类',
  `title`             VARCHAR(255)    NOT NULL                COMMENT '热点标题',
  `summary`           VARCHAR(1000)   NOT NULL DEFAULT ''     COMMENT '热点摘要',
  `author_name`       VARCHAR(64)     NOT NULL DEFAULT ''     COMMENT '发布作者名称',
  `published_at`      DATETIME        NOT NULL                COMMENT '发布时间，用于最新排序',
  `cover_url`         VARCHAR(255)    NOT NULL DEFAULT ''     COMMENT '热点封面地址',
  `tag_list`          VARCHAR(1000)   NOT NULL DEFAULT ''     COMMENT '热点标签列表，使用竖线分隔，便于后端转换为数组',
  `view_count`        INT UNSIGNED    NOT NULL DEFAULT 0      COMMENT '浏览数',
  `comment_count`     INT UNSIGNED    NOT NULL DEFAULT 0      COMMENT '评论数',
  `like_count`        INT UNSIGNED    NOT NULL DEFAULT 0      COMMENT '点赞数',
  `heat`              INT UNSIGNED    NOT NULL DEFAULT 0      COMMENT '热度分，用于推荐排序',
  `is_recommended`    TINYINT(1)      NOT NULL DEFAULT 0      COMMENT '是否进入推荐列表',
  `is_today`          TINYINT(1)      NOT NULL DEFAULT 0      COMMENT '是否属于今日热点',
  `status`            TINYINT         NOT NULL DEFAULT 1      COMMENT '状态：1=发布 0=下线',
  `sort_order`        INT             NOT NULL DEFAULT 0      COMMENT '业务排序权重，值越小越靠前',
  `created_at`        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ai_hotspot_key` (`hotspot_key`),
  KEY `idx_ai_hotspot_track` (`track`),
  KEY `idx_ai_hotspot_publish` (`published_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI 热点内容表';
