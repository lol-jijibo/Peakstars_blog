package com.interview.auth.domain.dto.response;

import lombok.Getter;
import lombok.Setter;

/**
 * 承接 star_read 搜索框下拉建议所需的文本与来源标识。
 * 建议词统一由后端生成，前端只负责展示和回填关键词。
 */
@Getter
@Setter
public class StarReadSearchSuggestionResponse {

    private String text;
    private String source;
}
