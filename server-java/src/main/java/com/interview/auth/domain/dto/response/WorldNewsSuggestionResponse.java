package com.interview.auth.domain.dto.response;

import lombok.Getter;
import lombok.Setter;

/**
 * 承接看天下搜索框联想词的展示字段。
 * 后端统一输出联想文案和类型标签，前端只负责回填和渲染下拉列表。
 */
@Getter
@Setter
public class WorldNewsSuggestionResponse {

    private String text;
    private String type;
}
