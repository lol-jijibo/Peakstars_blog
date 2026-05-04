package com.interview.auth.admin.service;

import com.interview.auth.admin.dto.request.AdminContentImportPreviewRequest;
import com.interview.auth.admin.dto.response.AdminContentImportPreviewResponse;

/**
 * 统一抽象后台外部内容导入能力。
 * 将 HTML 清洗、图片附件迁移和导入预览聚合到同一服务中，保证正式保存与导入预处理口径一致。
 */
public interface AdminContentImportService {

    /**
     * 预处理外部内容并返回预览结果。
     * 用于后台在正式保存前先完成白名单清洗、资源迁移与风险提示展示。
     *
     * @param contentType 内容类型
     * @param request 导入预处理请求
     * @return 预处理结果
     */
    AdminContentImportPreviewResponse preview(String contentType, AdminContentImportPreviewRequest request);

    /**
     * 标准化正文 HTML。
     * 供后台保存链路复用同一套企业级内容导入标准，避免直接保存未经清洗的外部富文本。
     *
     * @param contentType 内容类型
     * @param html 原始正文 HTML
     * @param sourceType 来源类型
     * @param sourceUrl 来源地址
     * @param migrateAssets 是否迁移资源
     * @return 标准化结果
     */
    AdminContentImportPreviewResponse normalizeHtml(
        String contentType,
        String html,
        String sourceType,
        String sourceUrl,
        boolean migrateAssets
    );

    /**
     * 标准化封面链接。
     * 统一将外部封面图迁移到自有对象存储，保证后台保存后前台展示不依赖第三方图床。
     *
     * @param contentType 内容类型
     * @param coverUrl 原始封面地址
     * @return 标准化后的封面地址
     */
    String normalizeCoverUrl(String contentType, String coverUrl);
}
