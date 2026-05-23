-- 技术文章分类展示字段
-- 业务目的：保存技术文章分类中文文案，支持“项目业务解析”等后台新增分类稳定回显。
-- 业务逻辑：老库缺列时追加 category_label，并按已有 category 编码补齐默认中文标签。

SET @tech_article_category_label_exists = (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'tech_article'
    AND COLUMN_NAME = 'category_label'
);

SET @tech_article_category_label_sql = IF(
  @tech_article_category_label_exists = 0,
  'ALTER TABLE `tech_article` ADD COLUMN `category_label` VARCHAR(64) NOT NULL DEFAULT '''' COMMENT ''文章分类展示文案，如 项目业务解析'' AFTER `category`',
  'SELECT 1'
);

PREPARE tech_article_category_label_stmt FROM @tech_article_category_label_sql;
EXECUTE tech_article_category_label_stmt;
DEALLOCATE PREPARE tech_article_category_label_stmt;

UPDATE `tech_article`
SET `category_label` = CASE `category`
  WHEN 'frontend' THEN '前端工程'
  WHEN 'backend' THEN '后端架构'
  WHEN 'project' THEN '项目业务解析'
  ELSE `category`
END
WHERE `category_label` = '';
