package com.interview.auth.admin.dto.response;

import java.util.Map;

/**
 * 后台草稿响应 DTO，返回给前端渲染草稿列表。
 */
public class AdminDraftResponse {

    private String draftKey;
    private String contentType;
    private String title;
    private String savedAt;
    /** 前端表单所有字段的完整 Map */
    private Map<String, Object> formData;

    public String getDraftKey() { return draftKey; }
    public void setDraftKey(String draftKey) { this.draftKey = draftKey; }

    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getSavedAt() { return savedAt; }
    public void setSavedAt(String savedAt) { this.savedAt = savedAt; }

    public Map<String, Object> getFormData() { return formData; }
    public void setFormData(Map<String, Object> formData) { this.formData = formData; }
}
