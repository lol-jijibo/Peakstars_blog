package com.interview.auth.domain.entity;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/**
 * 业务目的：承接企业信息表的持久化字段，用于面经列表和详情中的公司信息展示。
 * 业务逻辑：保存企业名称、头像文字和背景色等展示字段，通过 interview.company_id 关联查询使用。
 */
@Getter
@Setter
public class Company {

    private Long id;
    private String name;
    private String shortName;
    private String avatarText;
    private String avatarColor;
    private String logoUrl;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
