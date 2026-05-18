package com.interview.auth.domain.dto.response;

import java.math.BigDecimal;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

/**
 * 承接用户端书籍详情页与后台书籍概览的展示字段。
 * 服务层统一补齐标签、评分和目录摘要，前端可直接渲染书籍信息面板。
 */
@Getter
@Setter
public class BookResponse {

    private String id;
    private String title;
    private String author;
    private String translator;
    private String publisher;
    private String category;
    private String summary;
    private String coverUrl;
    private List<String> tags;
    private Integer wordCount;
    private Integer chapterCount;
    private Integer readCount;
    private BigDecimal rating;
    private String publishedAt;
}
