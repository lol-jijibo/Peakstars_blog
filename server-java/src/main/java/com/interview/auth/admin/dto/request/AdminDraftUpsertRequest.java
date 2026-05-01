package com.interview.auth.admin.dto.request;

import java.util.Map;
import jakarta.validation.constraints.NotBlank;

/**
 * 草稿保存请求体。
 * data 为前端表单所有字段的完整 Map 结构。
 */
public class AdminDraftUpsertRequest {

    /** 草稿唯一标识，前端生成，为空时后端创建 */
    private String draftKey;

    /** 内容类型：tech / world / ai / interview */
    @NotBlank(message = "contentType 不能为空")
    private String contentType;

    /** 草稿标题，方便列表展示 */
    private String title;

    /** 正文富文本 HTML */
    private String contentHtml;

    /** 表单所有字段的完整 Map */
    private Map<String, Object> data;

    public String getDraftKey() { return draftKey; }
    public void setDraftKey(String draftKey) { this.draftKey = draftKey; }

    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getContentHtml() { return contentHtml; }
    public void setContentHtml(String contentHtml) { this.contentHtml = contentHtml; }

    public Map<String, Object> getData() { return data; }
    public void setData(Map<String, Object> data) { this.data = data; }
}
