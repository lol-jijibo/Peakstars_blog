package com.interview.auth.admin.controller;

import com.interview.auth.admin.dto.request.AdminBatchUpsertRequest;
import com.interview.auth.admin.dto.request.AdminContentImportPreviewRequest;
import com.interview.auth.admin.dto.request.AdminContentUpsertRequest;
import com.interview.auth.admin.dto.request.AdminDraftUpsertRequest;
import com.interview.auth.admin.dto.request.AdminHeartbeatRequest;
import com.interview.auth.admin.dto.response.AdminContentImportPreviewResponse;
import com.interview.auth.admin.dto.response.AdminContentRecordResponse;
import com.interview.auth.admin.dto.response.AdminDashboardResponse;
import com.interview.auth.admin.dto.response.AdminDraftResponse;
import com.interview.auth.admin.service.AdminService;
import com.interview.auth.common.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 对外暴露后台管理页所需的心跳、仪表盘、内容 CRUD、批量导入与导入预处理接口。
 * Controller 只负责接参和包装统一响应结构，具体数据处理交给 AdminService。
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    /**
     * 记录后台当前访问者的心跳，驱动在线人数实时统计。
     * 前端会定时发送 clientId，后端据此维护短周期在线会话。
     *
     * @param request 心跳请求
     * @return 统一响应
     */
    @PostMapping("/heartbeat")
    public ApiResponse<Void> heartbeat(@Valid @RequestBody AdminHeartbeatRequest request) {
        adminService.heartbeat(request.getClientId());
        return ApiResponse.success("Heartbeat updated", null);
    }

    /**
     * 获取后台首页仪表盘聚合数据。
     * 统一返回指标卡片、趋势图、模块统计和最近编辑，减少前端首屏请求数量。
     *
     * @return 仪表盘聚合数据
     */
    @GetMapping("/dashboard")
    public ApiResponse<AdminDashboardResponse> getDashboard() {
        return ApiResponse.success(adminService.getDashboard());
    }

    /**
     * 按模块读取后台内容管理列表。
     * type 决定读取技术文章、看天下、AI 热点或面经，返回结果统一映射成后台记录结构。
     *
     * @param type 内容类型
     * @return 内容列表
     */
    @GetMapping("/content")
    public ApiResponse<List<AdminContentRecordResponse>> listContent(@RequestParam String type) {
        return ApiResponse.success(adminService.listContent(type));
    }

    /**
     * 新增指定模块的一条内容记录。
     * 后端会按模块规则生成业务主键并落库，同时记录一条编辑日志。
     *
     * @param type 内容类型
     * @param request 内容保存请求
     * @return 保存后的统一记录
     */
    @PostMapping("/content")
    public ApiResponse<AdminContentRecordResponse> createContent(
        @RequestParam String type,
        @Valid @RequestBody AdminContentUpsertRequest request
    ) {
        return ApiResponse.success(adminService.saveContent(type, null, request));
    }

    /**
     * 更新指定模块的一条内容记录。
     * 通过路径上的业务主键定位记录，再按请求体内容执行幂等更新。
     *
     * @param type 内容类型
     * @param contentKey 内容主键
     * @param request 内容保存请求
     * @return 保存后的统一记录
     */
    @PutMapping("/content/{contentKey}")
    public ApiResponse<AdminContentRecordResponse> updateContent(
        @RequestParam String type,
        @PathVariable String contentKey,
        @Valid @RequestBody AdminContentUpsertRequest request
    ) {
        return ApiResponse.success(adminService.saveContent(type, contentKey, request));
    }

    /**
     * 处理管理台的 Excel 批量导入保存。
     * 前端先把 XLSX 转成标准记录数组，再统一提交到该接口执行批量写入。
     *
     * @param type 内容类型
     * @param request 批量保存请求
     * @return 保存后的统一记录集合
     */
    @PostMapping("/content/batch")
    public ApiResponse<List<AdminContentRecordResponse>> batchSaveContent(
        @RequestParam String type,
        @Valid @RequestBody AdminBatchUpsertRequest request
    ) {
        return ApiResponse.success(adminService.batchSaveContent(type, request));
    }

    /**
     * 对外部编辑器内容执行导入预处理。
     * 在正式发布前先统一完成 HTML 白名单清洗与 MinIO 资源迁移，返回可直接回填到富文本编辑器的标准正文。
     *
     * @param type 内容类型
     * @param request 导入预处理请求
     * @return 导入预处理结果
     */
    @PostMapping("/content/import-preview")
    public ApiResponse<AdminContentImportPreviewResponse> previewImportedContent(
        @RequestParam String type,
        @Valid @RequestBody AdminContentImportPreviewRequest request
    ) {
        return ApiResponse.success(adminService.previewImportedContent(type, request));
    }

    /**
     * 下线指定模块的一条内容记录。
     * 删除采用软删除方案，只更新 status 并同步写入编辑日志。
     *
     * @param type 内容类型
     * @param contentKey 内容主键
     * @return 统一响应
     */
    @DeleteMapping("/content/{contentKey}")
    public ApiResponse<Void> deleteContent(@RequestParam String type, @PathVariable String contentKey) {
        adminService.deleteContent(type, contentKey);
        return ApiResponse.success("Content removed", null);
    }

    /**
     * 按模块列出全部草稿，返回给前端“待编辑”表格展示。
     * 按更新时间倒序排列，最新的草稿排在最前面。
     *
     * @param type 内容类型
     * @return 草稿列表
     */
    @GetMapping("/draft")
    public ApiResponse<List<AdminDraftResponse>> listDrafts(@RequestParam String type) {
        return ApiResponse.success(adminService.listDrafts(type));
    }

    /**
     * 创建或更新草稿，按 draftKey 幂等写入。
     * 前端在新增或编辑过程中点击“保存草稿”或触发自动保存时调用此接口。
     *
     * @param request 草稿保存请求
     * @return 草稿结果
     */
    @PostMapping("/draft")
    public ApiResponse<AdminDraftResponse> saveDraft(@Valid @RequestBody AdminDraftUpsertRequest request) {
        return ApiResponse.success(adminService.saveDraft(request));
    }

    /**
     * 删除指定草稿。
     * 草稿发布成功或用户手动删除时调用，直接从 content_draft 表物理删除。
     *
     * @param draftKey 草稿主键
     * @return 统一响应
     */
    @DeleteMapping("/draft/{draftKey}")
    public ApiResponse<Void> deleteDraft(@PathVariable String draftKey) {
        adminService.deleteDraft(draftKey);
        return ApiResponse.success("Draft removed", null);
    }

    /**
     * 上传封面图片。
     * 接收前端通过文件选择器提交的图片，上传到对象存储后返回可访问的 URL 地址。
     * 支持 jpg/jpeg/png/gif/webp/bmp/svg 等主流图片格式，单文件最大 10MB。
     *
     * @param file 图片文件
     * @return 上传结果，包含可访问的 URL 地址
     */
    @PostMapping("/upload/cover")
    public ApiResponse<Map<String, String>> uploadCoverImage(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ApiResponse.fail(400, "上传文件不能为空");
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            return ApiResponse.fail(400, "仅支持上传图片文件");
        }

        long maxSize = 10 * 1024 * 1024;
        if (file.getSize() > maxSize) {
            return ApiResponse.fail(400, "图片大小不能超过 10MB");
        }

        try {
            String url = adminService.uploadCoverImage(
                file.getOriginalFilename(),
                file.getInputStream(),
                file.getSize(),
                contentType
            );
            return ApiResponse.success(Map.of("url", url));
        } catch (Exception e) {
            return ApiResponse.fail(500, "图片上传失败：" + e.getMessage());
        }
    }
}
