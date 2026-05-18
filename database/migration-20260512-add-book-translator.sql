-- 扩展书籍与导入任务表：支持译者识别、审核展示和发布保存
SET @schema_name := DATABASE();

SET @column_exists := (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @schema_name
    AND TABLE_NAME = 'book'
    AND COLUMN_NAME = 'translator'
);
SET @ddl := IF(
  @column_exists = 0,
  'ALTER TABLE `book` ADD COLUMN `translator` VARCHAR(128) NOT NULL DEFAULT '''' COMMENT ''译者名称'' AFTER `author`',
  'SELECT ''book.translator exists'' AS message'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @column_exists := (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @schema_name
    AND TABLE_NAME = 'book_import_job'
    AND COLUMN_NAME = 'translator'
);
SET @ddl := IF(
  @column_exists = 0,
  'ALTER TABLE `book_import_job` ADD COLUMN `translator` VARCHAR(128) NOT NULL DEFAULT '''' COMMENT ''解析出的译者名称'' AFTER `author`',
  'SELECT ''book_import_job.translator exists'' AS message'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
