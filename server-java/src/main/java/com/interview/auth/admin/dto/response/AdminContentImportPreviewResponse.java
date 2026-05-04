package com.interview.auth.admin.dto.response;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

/**
 * 承接后台内容导入预处理结果。
 * 统一返回清洗后的标准正文、迁移资源清单与风险提示，支撑管理端导入前预览确认。
 */
@Getter
@Setter
public class AdminContentImportPreviewResponse {

    private String sourceType;
    private String normalizedHtml;
    private String plainText;
    private String migratedCoverUrl;
    private List<AdminImportedAssetResponse> assets = new ArrayList<>();
    private List<String> warnings = new ArrayList<>();
}
