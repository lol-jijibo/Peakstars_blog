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
 * 业务目的：统一暴露后台管理页所需的心跳、仪表盘、内容管理、草稿管理与上传接口。
 * 业务逻辑：控制器只负责接参与响应封装，具体业务处理统一下沉到 AdminService。
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    /**
     * 业务目的：记录后台访问者心跳，驱动仪表盘在线人数实时变化。
     * 业务逻辑：前端周期上报 clientId，后端按会话维度刷新在线状态。
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
     * 业务目的：获取后台首页仪表盘聚合数据。
     * 业务逻辑：统一返回统计卡片、趋势图、模块分布与最近编辑记录，减少首屏请求次数。
     *
     * @return 仪表盘聚合数据
     */
    @GetMapping("/dashboard")
    public ApiResponse<AdminDashboardResponse> getDashboard() {
        return ApiResponse.success(adminService.getDashboard());
    }

    /**
     * 业务目的：按模块读取后台内容列表。
     * 业务逻辑：根据内容类型分发到不同业务表，再统一映射成后台列表结构。
     *
     * @param type 内容类型
     * @return 内容列表
     */
    @GetMapping("/content")
    public ApiResponse<List<AdminContentRecordResponse>> listContent(@RequestParam String type) {
        return ApiResponse.success(adminService.listContent(type));
    }

    /**
     * 业务目的：新增指定模块的一条内容记录。
     * 业务逻辑：后端按模块规则生成业务主键并完成持久化。
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
     * 业务目的：更新指定模块的一条内容记录。
     * 业务逻辑：通过路径主键定位记录，再按请求体内容执行幂等更新。
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
     * 业务目的：处理后台批量导入后的多条内容保存。
     * 业务逻辑：前端先标准化记录数组，后端统一执行批量写入。
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
     * 业务目的：在正式发布前预处理外部导入的正文内容。
     * 业务逻辑：统一完成 HTML 清洗与资源迁移，返回可直接回填到富文本编辑器的结果。
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
     * 业务目的：下线指定模块的一条内容记录。
     * 业务逻辑：删除采用软删方案，只更新状态并记录编辑日志。
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
     * 业务目的：按模块列出全部草稿。
     * 业务逻辑：统一返回后台待编辑面板所需的草稿列表数据。
     *
     * @param type 内容类型
     * @return 草稿列表
     */
    @GetMapping("/draft")
    public ApiResponse<List<AdminDraftResponse>> listDrafts(@RequestParam String type) {
        return ApiResponse.success(adminService.listDrafts(type));
    }

    /**
     * 业务目的：创建或更新后台草稿。
     * 业务逻辑：统一承接手动保存与自动保存场景，按 draftKey 幂等写入。
     *
     * @param request 草稿保存请求
     * @return 草稿结果
     */
    @PostMapping("/draft")
    public ApiResponse<AdminDraftResponse> saveDraft(@Valid @RequestBody AdminDraftUpsertRequest request) {
        return ApiResponse.success(adminService.saveDraft(request));
    }

    /**
     * 业务目的：删除指定草稿。
     * 业务逻辑：草稿发布成功或用户主动丢弃时，直接清理草稿记录。
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
     * 业务目的：上传后台文章封面图，并统一落到对象存储。
     * 业务逻辑：复用统一图片校验规则，通过服务层写入 MinIO 后返回可访问地址。
     *
     * @param file 图片文件
     * @return 上传结果，包含封面图访问地址
     */
    @PostMapping("/upload/cover")
    public ApiResponse<Map<String, String>> uploadCoverImage(@RequestParam("file") MultipartFile file) {
        String validationMessage = validateImageFile(file);
        if (validationMessage != null) {
            return ApiResponse.fail(400, validationMessage);
        }

        try {
            String url = adminService.uploadCoverImage(
                file.getOriginalFilename(),
                file.getInputStream(),
                file.getSize(),
                file.getContentType()
            );
            return ApiResponse.success(Map.of("url", url));
        } catch (Exception e) {
            return ApiResponse.fail(500, "封面图片上传失败：" + e.getMessage());
        }
    }

    /**
     * 业务目的：为后台富文本正文图片提供独立上传入口，确保文章插图统一落到 MinIO。
     * 业务逻辑：沿用统一图片校验规则，上传成功后返回编辑器可直接回填的正文图片地址。
     *
     * @param file 正文图片文件
     * @return 上传结果，包含正文图片访问地址
     */
    @PostMapping("/upload/rich-text-image")
    public ApiResponse<Map<String, String>> uploadRichTextImage(@RequestParam("file") MultipartFile file) {
        String validationMessage = validateImageFile(file);
        if (validationMessage != null) {
            return ApiResponse.fail(400, validationMessage);
        }

        try {
            String url = adminService.uploadRichTextImage(
                file.getOriginalFilename(),
                file.getInputStream(),
                file.getSize(),
                file.getContentType()
            );
            return ApiResponse.success(Map.of("url", url));
        } catch (Exception e) {
            return ApiResponse.fail(500, "正文图片上传失败：" + e.getMessage());
        }
    }

    /**
     * 业务目的：统一后台图片上传的校验口径，避免封面图与正文图规则分散。
     * 业务逻辑：集中校验空文件、图片类型与大小限制，校验失败时直接返回提示文案。
     *
     * @param file 上传文件
     * @return 校验失败文案，校验通过时返回 null
     */
    private String validateImageFile(MultipartFile file) {
        if (file.isEmpty()) {
            return "上传文件不能为空";
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            return "仅支持上传图片文件";
        }

        long maxSize = 10 * 1024 * 1024;
        if (file.getSize() > maxSize) {
            return "图片大小不能超过 10MB";
        }

        return null;
    }
}
