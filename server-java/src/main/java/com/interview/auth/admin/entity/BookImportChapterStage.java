package com.interview.auth.admin.entity;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/**
 * 对应数据库 book_import_chapter_stage 表，封装书籍导入章节暂存字段。
 * 配合 MyBatis 保存预处理后的章节正文，供后台审核后再正式发布到章节表。
 */
@Getter
@Setter
public class BookImportChapterStage {

    private Long id;
    private String jobKey;
    private String tempChapterKey;
    private String sourcePath;
    private Integer chapterNo;
    private String title;
    private String subtitle;
    private String contentHtml;
    private String plainText;
    private Integer wordCount;
    private String warningJson;
    private String reviewStatus;
    private Integer sortOrder;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
