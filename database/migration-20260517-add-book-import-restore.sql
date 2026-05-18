-- 为书籍导入任务增加删除恢复状态字段
SET @schema_name := DATABASE();

SET @column_exists := (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @schema_name
    AND TABLE_NAME = 'book_import_job'
    AND COLUMN_NAME = 'previous_status'
);
SET @ddl := IF(
  @column_exists = 0,
  'ALTER TABLE `book_import_job` ADD COLUMN `previous_status` VARCHAR(32) NOT NULL DEFAULT '''' COMMENT ''删除前任务状态'' AFTER `job_key`',
  'SELECT ''book_import_job.previous_status exists'' AS message'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
