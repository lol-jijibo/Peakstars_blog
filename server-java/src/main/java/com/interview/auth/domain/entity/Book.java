package com.interview.auth.domain.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/**
 * 对应数据库 book 表，封装书籍主记录的核心字段。
 * 配合 MyBatis 完成书籍主信息映射，由 Service 层组装后输出给后台与用户端。
 */
@Getter
@Setter
public class Book {

    private Long id;
    private String bookKey;
    private String title;
    private String author;
    private String publisher;
    private String category;
    private String summary;
    private String coverUrl;
    private String tagList;
    private Integer wordCount;
    private Integer chapterCount;
    private Integer readCount;
    private BigDecimal rating;
    private Integer status;
    private Integer sortOrder;
    private LocalDateTime publishedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
