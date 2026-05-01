package com.interview.auth.domain.dto.response;

import java.util.List;
import lombok.Getter;
import lombok.Setter;

/**
 * 承载面经列表项的前端展示结构，服务面经列表页的卡片渲染。
 * 后端通过 JOIN 查询组装公司名称、分类信息和标签数组，前端直接按字段展示，无需二次转换。
 */
@Getter
@Setter
public class InterviewListResponse {

    private Long id;
    private String title;
    private String author;
    private String summary;
    private Integer views;
    private Integer likes;
    private Integer collects;
    private String date;
    private String category;
    private String categoryName;
    private String companyName;
    private String avatar;
    private String avatarColor;
    private List<String> tags;
}
