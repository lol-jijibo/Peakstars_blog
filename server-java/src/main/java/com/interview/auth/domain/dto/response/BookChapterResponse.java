package com.interview.auth.domain.dto.response;

import lombok.Getter;
import lombok.Setter;

/**
 * 承接用户端书籍目录与章节正文的核心展示字段。
 * 服务层统一补齐免费标记与正文内容，阅读页可按目录和正文模式直接复用。
 */
@Getter
@Setter
public class BookChapterResponse {

    private String id;
    private Integer chapterNo;
    private String title;
    private String subtitle;
    private String contentHtml;
    private Integer wordCount;
    private Boolean free;
}
