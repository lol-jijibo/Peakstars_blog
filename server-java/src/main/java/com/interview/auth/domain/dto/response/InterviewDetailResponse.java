package com.interview.auth.domain.dto.response;

import java.util.List;
import lombok.Getter;
import lombok.Setter;

/**
 * 业务目的：承载面经详情页的前端展示结构，包含正文内容和公司描述。
 * 业务逻辑：继承列表项字段的基础上增加 content 和 companyDesc，确保详情页与列表页共用一致的展示字段定义。
 */
@Getter
@Setter
public class InterviewDetailResponse {

    private Long id;
    private String title;
    private String author;
    private String summary;
    private String content;
    private Integer views;
    private Integer likes;
    private Integer collects;
    private String date;
    private String category;
    private String categoryName;
    private String companyName;
    private String avatar;
    private String avatarColor;
    private String companyDesc;
    private List<String> tags;
}
