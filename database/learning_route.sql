-- ============================================================
-- Peakstars Blog - learning route module
-- Run after database/schema.sql.
-- ============================================================

USE interview_db;

CREATE TABLE IF NOT EXISTS `learning_route` (
  `id`                BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '学习路线ID',
  `slug`              VARCHAR(64)     NOT NULL                COMMENT '前端路由或详情标识',
  `route_type`        VARCHAR(32)     NOT NULL                COMMENT '路线类型：java / fullstack',
  `title`             VARCHAR(128)    NOT NULL                COMMENT '学习路线标题',
  `summary`           VARCHAR(512)    NOT NULL DEFAULT ''     COMMENT '学习路线摘要',
  `cover_url`         VARCHAR(256)    NOT NULL DEFAULT ''     COMMENT '封面图片地址',
  `content`           MEDIUMTEXT      NOT NULL                COMMENT '详情页文章正文',
  `stage_list`        VARCHAR(1024)   NOT NULL DEFAULT ''     COMMENT '竖线分隔的阶段目录',
  `author`            VARCHAR(64)     NOT NULL DEFAULT 'Peakstars' COMMENT '作者',
  `published_at`      DATETIME        NOT NULL                COMMENT '发布时间',
  `estimated_minutes` INT UNSIGNED    NOT NULL DEFAULT 0      COMMENT '预计阅读分钟数',
  `view_count`        INT UNSIGNED    NOT NULL DEFAULT 0      COMMENT '阅读人数',
  `comment_count`     INT UNSIGNED    NOT NULL DEFAULT 0      COMMENT '评论人数',
  `like_count`        INT UNSIGNED    NOT NULL DEFAULT 0      COMMENT '点赞人数',
  `stage_count`       INT UNSIGNED    NOT NULL DEFAULT 0      COMMENT '路线阶段数',
  `difficulty`        VARCHAR(32)     NOT NULL DEFAULT ''     COMMENT '难度说明',
  `tags`              VARCHAR(256)    NOT NULL DEFAULT ''     COMMENT '逗号分隔标签',
  `featured`          TINYINT         NOT NULL DEFAULT 0      COMMENT '是否首页重点展示：1=是 0=否',
  `status`            TINYINT         NOT NULL DEFAULT 1      COMMENT '状态：1=发布 0=草稿 -1=删除',
  `sort_order`        INT             NOT NULL DEFAULT 0      COMMENT '排序权重，越小越靠前',
  `created_at`        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_learning_route_slug` (`slug`),
  KEY `idx_learning_route_status_sort` (`status`, `featured`, `sort_order`),
  KEY `idx_learning_route_type` (`route_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='学习路线首页模块表';

-- If an older version of this table already exists, add the detail columns safely.
SET @learning_route_content_exists = (
  SELECT COUNT(*)
  FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'learning_route'
    AND COLUMN_NAME = 'content'
);
SET @learning_route_content_sql = IF(
  @learning_route_content_exists = 0,
  'ALTER TABLE `learning_route` ADD COLUMN `content` MEDIUMTEXT NULL COMMENT ''详情页文章正文'' AFTER `cover_url`',
  'SELECT 1'
);
PREPARE learning_route_content_stmt FROM @learning_route_content_sql;
EXECUTE learning_route_content_stmt;
DEALLOCATE PREPARE learning_route_content_stmt;

SET @learning_route_stage_exists = (
  SELECT COUNT(*)
  FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'learning_route'
    AND COLUMN_NAME = 'stage_list'
);
SET @learning_route_stage_sql = IF(
  @learning_route_stage_exists = 0,
  'ALTER TABLE `learning_route` ADD COLUMN `stage_list` VARCHAR(1024) NOT NULL DEFAULT '''' COMMENT ''竖线分隔的阶段目录'' AFTER `content`',
  'SELECT 1'
);
PREPARE learning_route_stage_stmt FROM @learning_route_stage_sql;
EXECUTE learning_route_stage_stmt;
DEALLOCATE PREPARE learning_route_stage_stmt;

INSERT INTO `learning_route` (
  `slug`,
  `route_type`,
  `title`,
  `summary`,
  `cover_url`,
  `content`,
  `stage_list`,
  `author`,
  `published_at`,
  `estimated_minutes`,
  `view_count`,
  `comment_count`,
  `like_count`,
  `stage_count`,
  `difficulty`,
  `tags`,
  `featured`,
  `status`,
  `sort_order`
) VALUES
(
  'java-backend-roadmap',
  'java',
  'Java 后端工程师成长路线',
  '从 Java 基础、集合并发、JVM 到 Spring Boot、MyBatis、Redis、MySQL 与微服务实战，按阶段建立可落地的后端能力树。',
  '/【哲风壁纸】xiaomiyu7-小米suv.png',
  '这条路线适合希望系统进入 Java 后端方向的学习者。前期重点是 Java 语法、面向对象、集合、异常、IO 与并发基础；中期进入 JVM、MySQL、Redis、Spring Boot、MyBatis 和接口设计；后期通过权限系统、内容系统、缓存优化、异步任务、日志监控和部署发布，把知识串成真实工程能力。学习时不要只追求“看过”，更要把每个阶段沉淀成一个可运行的小项目。',
  'Java 基础与面向对象|集合、泛型、异常与 IO|多线程、线程池与 JVM 基础|MySQL 建模与 SQL 优化|Spring Boot + MyBatis 接口开发|Redis 缓存与登录鉴权|微服务、部署、日志与监控',
  'Peakstars',
  '2026-04-23 10:00:00',
  12,
  2846,
  36,
  528,
  7,
  '进阶路线',
  'Java,Spring Boot,MyBatis,MySQL,Redis',
  1,
  1,
  1
),
(
  'fullstack-roadmap',
  'fullstack',
  '全栈开发者进阶路线',
  '以 Vue3 前端体验、Java API 服务、MySQL 数据建模、部署监控和 AI 工具链为主线，串起从页面到系统的完整交付能力。',
  '/【哲风壁纸】夏日-晴天-氛围感.png',
  '全栈路线的核心不是“什么都会一点”，而是理解一次完整交付如何发生：从页面设计、组件拆分、状态管理，到 Java 后端 API、MySQL 数据结构、权限认证、部署上线和监控复盘。建议每学一个阶段都围绕同一个产品原型迭代，让前端、后端、数据库和工程化自然连接起来。',
  'HTML/CSS/JavaScript 体验基础|Vue3 组件、路由与状态管理|Java REST API 与分层架构|MySQL 表设计与 MyBatis 持久化|登录鉴权、文件上传与内容管理|前后端联调、环境配置与部署|性能优化、监控日志与故障复盘|AI Coding 辅助开发工作流',
  'Peakstars',
  '2026-04-23 11:20:00',
  15,
  1938,
  24,
  416,
  8,
  '系统路线',
  'Vue3,Java API,全栈,部署,AI Coding',
  1,
  1,
  2
)
ON DUPLICATE KEY UPDATE
  `title` = VALUES(`title`),
  `summary` = VALUES(`summary`),
  `cover_url` = VALUES(`cover_url`),
  `content` = VALUES(`content`),
  `stage_list` = VALUES(`stage_list`),
  `published_at` = VALUES(`published_at`),
  `estimated_minutes` = VALUES(`estimated_minutes`),
  `view_count` = VALUES(`view_count`),
  `comment_count` = VALUES(`comment_count`),
  `like_count` = VALUES(`like_count`),
  `stage_count` = VALUES(`stage_count`),
  `difficulty` = VALUES(`difficulty`),
  `tags` = VALUES(`tags`),
  `featured` = VALUES(`featured`),
  `status` = VALUES(`status`),
  `sort_order` = VALUES(`sort_order`);
