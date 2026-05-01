package com.interview.auth.domain.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/**
 * 承接面经主表的持久化字段，供 MyBatis 直接映射 interview 表记录。
 * 实体保留数据库原始字段，公司、分类、标签等关联信息由 Service 层通过 JOIN 查询组装。
 */
@Getter
@Setter
public class Interview {

    private Long id;
    private Long companyId;
    private Long categoryId;
    private String title;
    private String author;
    private String summary;
    private String content;
    private Integer views;
    private Integer likes;
    private Integer collects;
    private LocalDate publishDate;
    private Integer status;
    private String difficulty;
    private String tagList;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
