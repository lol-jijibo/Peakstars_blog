package com.interview.auth.admin.dto.response;

import lombok.Getter;
import lombok.Setter;

/**
 * 承接后台书籍导入任务面板的任务状态字段。
 * 服务层统一输出导入进度、章节数量和错误说明，前端可直接轮询渲染任务卡片。
 */
@Getter
@Setter
public class AdminBookImportJobResponse {

    private String jobKey;
    private String bookKey;
    private String title;
    private String author;
    private String publisher;
    private String summary;
    private String category;
    private String coverUrl;
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
    private String createdAt;
    private String updatedAt;
}
