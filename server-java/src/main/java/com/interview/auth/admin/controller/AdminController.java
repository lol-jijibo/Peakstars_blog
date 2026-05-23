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
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
 * 统一暴露后台管理页所需的心跳、仪表盘、内容管理、草稿管理与上传接口。
 * 控制器只负责接参与响应封装，具体处理统一下沉到 AdminService。
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    /**
     * 记录后台访问者心跳，驱动仪表盘在线人数实时变化。
     * 前端周期上报 clientId，后端按会话维度刷新在线状态。
     */
    @PostMapping("/heartbeat")
    public ApiResponse<Void> heartbeat(@Valid @RequestBody AdminHeartbeatRequest request) {
        adminService.heartbeat(request.getClientId());
        return ApiResponse.success("Heartbeat updated", null);
    }

    /**
     * 获取后台首页仪表盘聚合数据。
     * 统一返回统计卡片、趋势图、模块分布与最近编辑记录，减少首屏请求次数。
     */
    @GetMapping("/dashboard")
    public ApiResponse<AdminDashboardResponse> getDashboard() {
        return ApiResponse.success(adminService.getDashboard());
    }

    /**
     * 按模块读取后台内容列表。
     * 根据内容类型分发到不同业务表，再统一映射成后台列表结构。
     */
    @GetMapping("/content")
    public ApiResponse<List<AdminContentRecordResponse>> listContent(@RequestParam String type) {
        return ApiResponse.success(adminService.listContent(type));
    }

    /**
     * 新增指定模块的一条内容记录。
     * 后端按模块规则生成业务主键并完成持久化。
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
     * 通过路径主键定位记录，再按请求体内容执行幂等更新。
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
     * 处理后台批量导入后的多条内容保存。
     * 前端先标准化记录数组，后端统一执行批量写入。
     */
    @PostMapping("/content/batch")
    public ApiResponse<List<AdminContentRecordResponse>> batchSaveContent(
        @RequestParam String type,
        @Valid @RequestBody AdminBatchUpsertRequest request
    ) {
        return ApiResponse.success(adminService.batchSaveContent(type, request));
    }

    /**
     * 在正式发布前预处理外部导入的正文内容。
     * 统一完成 HTML 清洗与资源迁移，返回可直接回填到富文本编辑器的结果。
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
     * 删除采用软删方案，只更新状态并记录编辑日志。
     */
    @DeleteMapping("/content/{contentKey}")
    public ApiResponse<Void> deleteContent(@RequestParam String type, @PathVariable String contentKey) {
        adminService.deleteContent(type, contentKey);
        return ApiResponse.success("Content removed", null);
    }

    /**
     * 按模块列出全部草稿。
     * 统一返回后台待编辑面板所需的草稿列表数据。
     */
    @GetMapping("/draft")
    public ApiResponse<List<AdminDraftResponse>> listDrafts(@RequestParam String type) {
        return ApiResponse.success(adminService.listDrafts(type));
    }

    /**
     * 创建或更新后台草稿。
     * 统一承接手动保存与自动保存场景，按 draftKey 幂等写入。
     */
    @PostMapping("/draft")
    public ApiResponse<AdminDraftResponse> saveDraft(@Valid @RequestBody AdminDraftUpsertRequest request) {
        return ApiResponse.success(adminService.saveDraft(request));
    }

    /**
     * 删除指定草稿。
     * 草稿发布成功或用户主动丢弃时，直接清理草稿记录。
     */
    @DeleteMapping("/draft/{draftKey}")
    public ApiResponse<Void> deleteDraft(@PathVariable String draftKey) {
        adminService.deleteDraft(draftKey);
        return ApiResponse.success("Draft removed", null);
    }

    /**
     * 上传后台文章封面图，并统一落到阿里云 OSS。
     * 复用统一图片校验规则，通过服务层写入对象存储后返回可访问地址。
     */
    @PostMapping("/upload/cover")
    public ApiResponse<Map<String, String>> uploadCoverImage(
        @RequestParam("file") MultipartFile file,
        @RequestParam(value = "moduleType", required = false) String moduleType
    ) {
        String validationMessage = validateImageFile(file);
        if (validationMessage != null) {
            return ApiResponse.fail(400, validationMessage);
        }

        try {
            String url = adminService.uploadCoverImage(
                moduleType,
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
     * 为后台富文本正文图片提供独立上传入口，确保文章插图统一落到阿里云 OSS。
     * 沿用统一图片校验规则，上传成功后返回编辑器可直接回填的正文图片地址。
     */
    @PostMapping("/upload/rich-text-image")
    public ApiResponse<Map<String, String>> uploadRichTextImage(
        @RequestParam("file") MultipartFile file,
        @RequestParam(value = "moduleType", required = false) String moduleType
    ) {
        String validationMessage = validateImageFile(file);
        if (validationMessage != null) {
            return ApiResponse.fail(400, validationMessage);
        }

        try {
            String url = adminService.uploadRichTextImage(
                moduleType,
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

    private static final Set<String> ALLOWED_IMAGE_EXTENSIONS = Set.of(
        "png", "jpg", "jpeg", "gif", "webp", "bmp", "svg"
    );

    /**
     * 统一后台图片上传校验：空文件、文件扩展名、Content-Type、文件魔数、大小限制。
     * 魔数校验防止将非图片文件伪装成图片上传。
     */
    private String validateImageFile(MultipartFile file) {
        if (file.isEmpty()) {
            return "上传文件不能为空";
        }

        String originalName = file.getOriginalFilename();
        if (originalName == null || !hasAllowedImageExtension(originalName)) {
            return "仅支持上传 png / jpg / jpeg / gif / webp / bmp / svg 格式图片";
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            return "仅支持上传图片文件";
        }

        if (!hasValidImageMagicBytes(file)) {
            return "文件内容与图片格式不匹配";
        }

        long maxSize = 10 * 1024 * 1024;
        if (file.getSize() > maxSize) {
            return "图片大小不能超过 10MB";
        }

        return null;
    }

    private boolean hasAllowedImageExtension(String filename) {
        int dot = filename.lastIndexOf('.');
        if (dot < 0) {
            return false;
        }
        return ALLOWED_IMAGE_EXTENSIONS.contains(filename.substring(dot + 1).toLowerCase());
    }

    /**
     * 读取文件头部魔数判断是否为真实图片。
     * 允许 SVG 直接通过，因为它是 XML 文本格式没有固定魔数。
     */
    private boolean hasValidImageMagicBytes(MultipartFile file) {
        String originalName = file.getOriginalFilename();
        if (originalName != null && originalName.toLowerCase().endsWith(".svg")) {
            return true;
        }

        try (InputStream in = file.getInputStream()) {
            byte[] header = new byte[12];
            int read = in.read(header);
            if (read < 4) {
                return false;
            }

            // PNG: 89 50 4E 47
            if (match(header, 0, 0x89, 0x50, 0x4E, 0x47)) return true;
            // JPEG: FF D8 FF
            if (match(header, 0, 0xFF, 0xD8, 0xFF)) return true;
            // GIF: 47 49 46 38
            if (match(header, 0, 0x47, 0x49, 0x46, 0x38)) return true;
            // WebP: 52 49 46 46 ... 57 45 42 50
            if (read >= 12 && match(header, 0, 0x52, 0x49, 0x46, 0x46) && match(header, 8, 0x57, 0x45, 0x42, 0x50)) return true;
            // BMP: 42 4D
            if (match(header, 0, 0x42, 0x4D)) return true;

            return false;
        } catch (IOException e) {
            return false;
        }
    }

    private boolean match(byte[] header, int offset, int... expected) {
        for (int i = 0; i < expected.length; i++) {
            if ((header[offset + i] & 0xFF) != expected[i]) {
                return false;
            }
        }
        return true;
    }
}
