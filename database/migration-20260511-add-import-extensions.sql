-- 扩展导入任务表：支持多格式导入和外部资源导入
SET @schema_name := DATABASE();

SET @column_exists := (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @schema_name
    AND TABLE_NAME = 'book_import_job'
    AND COLUMN_NAME = 'source_url'
);
SET @ddl := IF(
  @column_exists = 0,
  'ALTER TABLE `book_import_job` ADD COLUMN `source_url` VARCHAR(1024) NOT NULL DEFAULT '''' COMMENT ''外部资源URL'' AFTER `import_type`',
  'SELECT ''source_url exists'' AS message'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @column_exists := (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @schema_name
    AND TABLE_NAME = 'book_import_job'
    AND COLUMN_NAME = 'original_format'
);
SET @ddl := IF(
  @column_exists = 0,
  'ALTER TABLE `book_import_job` ADD COLUMN `original_format` VARCHAR(32) NOT NULL DEFAULT '''' COMMENT ''原始文件格式(epub/pdf/txt/md/docx/html/zip)'' AFTER `source_url`',
  'SELECT ''original_format exists'' AS message'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @column_exists := (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @schema_name
    AND TABLE_NAME = 'book_import_job'
    AND COLUMN_NAME = 'file_size'
);
SET @ddl := IF(
  @column_exists = 0,
  'ALTER TABLE `book_import_job` ADD COLUMN `file_size` BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT ''上传文件字节数'' AFTER `original_format`',
  'SELECT ''file_size exists'' AS message'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 添加按状态和时间查询导入任务的索引
SET @index_exists := (
  SELECT COUNT(*)
  FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = @schema_name
    AND TABLE_NAME = 'book_import_job'
    AND INDEX_NAME = 'idx_book_import_job_type_status'
);
SET @ddl := IF(
  @index_exists = 0,
  'CREATE INDEX `idx_book_import_job_type_status` ON `book_import_job` (`import_type`, `status`, `updated_at` DESC)',
  'SELECT ''idx_book_import_job_type_status exists'' AS message'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
