package com.interview.auth.domain.entity;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/**
 * 对应数据库 book_chapter 表，封装书籍章节正文与目录字段。
 * 配合 MyBatis 完成章节映射，由 Service 层按目录顺序组装后输出给阅读页。
 */
@Getter
@Setter
public class BookChapter {

    private Long id;
    private String chapterKey;
    private String bookKey;
    private Integer chapterNo;
    private String title;
    private String subtitle;
    private String contentHtml;
    private Integer wordCount;
    private Integer isFree;
    private Integer status;
    private Integer sortOrder;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
