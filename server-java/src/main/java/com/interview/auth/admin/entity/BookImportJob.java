package com.interview.auth.admin.entity;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/**
 * 对应数据库 book_import_job 表，封装书籍导入任务的主状态字段。
 * 配合 MyBatis 记录导入进度、解析结果与发布状态，支撑后台导入面板轮询展示。
 */
@Getter
@Setter
public class BookImportJob {

    private Long id;
    private String jobKey;
    private String bookKey;
    private String title;
    private String author;
    private String publisher;
    private String summary;
    private String category;
    private String coverUrl;
    private String sourceFileKey;
    private String importType;
    private String sourceUrl;
    private String originalFormat;
    private Long fileSize;
    private String status;
    private Integer progress;
    private Integer totalChapters;
    private Integer successChapters;
    private Integer failChapters;
    private String message;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
