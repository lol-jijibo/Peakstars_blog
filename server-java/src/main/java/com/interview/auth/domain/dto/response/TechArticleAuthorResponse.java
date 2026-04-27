package com.interview.auth.domain.dto.response;

import lombok.Getter;
import lombok.Setter;

/**
 * 业务目的：向前端返回技术文章作者展示对象，保持现有页面对 author 嵌套结构的依赖不变。
 * 业务逻辑：作者名、角色、头像缩写和渐变色在后端组装完成，前端无需再拼接展示字段。
 */
@Getter
@Setter
public class TechArticleAuthorResponse {

    private String name;
    private String role;
    private String initials;
    private String accent;
}
