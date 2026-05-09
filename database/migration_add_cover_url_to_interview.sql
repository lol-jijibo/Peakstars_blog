-- 为 interview 表添加 cover_url 字段，支持面经封面图
-- 执行日期: 2026-05-09

ALTER TABLE `interview` 
ADD COLUMN `cover_url` VARCHAR(512) NOT NULL DEFAULT '' COMMENT '封面图URL' 
AFTER `content`;

-- 验证字段是否添加成功
-- SELECT COLUMN_NAME, DATA_TYPE, IS_NULLABLE, COLUMN_DEFAULT 
-- FROM INFORMATION_SCHEMA.COLUMNS 
-- WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'interview' AND COLUMN_NAME = 'cover_url';
