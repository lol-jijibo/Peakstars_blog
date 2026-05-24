package com.interview.auth.admin.controller;

import com.interview.auth.admin.dto.request.AdminBookCategoryUpdateRequest;
import com.interview.auth.admin.dto.request.AdminBookImportBatchRequest;
import com.interview.auth.admin.dto.request.AdminBookImportChapterBatchUpdateRequest;
import com.interview.auth.admin.dto.request.AdminBookImportChapterUpdateRequest;
import com.interview.auth.admin.dto.request.AdminBookImportExternalRequest;
import com.interview.auth.admin.dto.request.AdminBookImportMetadataUpdateRequest;
import com.interview.auth.admin.dto.response.AdminBookImportBatchResponse;
import com.interview.auth.admin.dto.response.AdminBookImportChapterResponse;
import com.interview.auth.admin.dto.response.AdminBookImportJobResponse;
import com.interview.auth.admin.service.AdminBookService;
import com.interview.auth.common.ApiResponse;
import com.interview.auth.domain.dto.response.BookResponse;
import jakarta.validation.Valid;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;
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
 * 统一暴露后台书籍导入、审核与发布接口。
 * 支持 ZIP 包导入、单文件导入（EPUB/PDF/TXT/MD/DOCX/HTML）、外部资源导入。
 */
@RestController
@RequestMapping("/api/admin/books")
@RequiredArgsConstructor
public class AdminBookController {

    private static final long MAX_UPLOAD_SIZE = 200 * 1024 * 1024; // 200MB

    private static final Set<String> ALLOWED_ZIP_EXTENSIONS = Set.of("zip");
    private static final Set<String> ALLOWED_SINGLE_FILE_EXTENSIONS = Set.of(
        "epub", "pdf", "txt", "md", "markdown", "docx", "html", "htm"
    );

    private final AdminBookService adminBookService;

    /**
     * 创建 ZIP 书稿导入任务。
     */
    @PostMapping("/import-jobs")
    public ApiResponse<AdminBookImportJobResponse> createImportJob(@RequestParam("file") MultipartFile file) throws Exception {
        String error = validateBookFile(file, ALLOWED_ZIP_EXTENSIONS);
        if (error != null) {
            return ApiResponse.fail(400, error);
        }
        return ApiResponse.success(adminBookService.createImportJob(
            file.getOriginalFilename(),
            file.getInputStream(),
            file.getSize(),
            file.getContentType()
        ));
    }

    /**
     * 创建单文件导入任务（支持 EPUB/PDF/TXT/MD/DOCX/HTML）。
     */
    @PostMapping("/import-jobs/file")
    public ApiResponse<AdminBookImportJobResponse> createImportJobFromFile(@RequestParam("file") MultipartFile file) throws Exception {
        String error = validateBookFile(file, ALLOWED_SINGLE_FILE_EXTENSIONS);
        if (error != null) {
            return ApiResponse.fail(400, error);
        }
        return ApiResponse.success(adminBookService.createImportJobFromFile(
            file.getOriginalFilename(),
            file.getInputStream(),
            file.getSize(),
            file.getContentType()
        ));
    }

    /**
     * 创建外部资源导入任务。
     */
    @PostMapping("/import-jobs/external")
    public ApiResponse<AdminBookImportJobResponse> createImportJobFromExternal(
        @Valid @RequestBody AdminBookImportExternalRequest request
    ) throws Exception {
        return ApiResponse.success(adminBookService.createImportJobFromExternal(request));
    }

    /**
     * 查询导入任务状态。
     */
    @GetMapping("/import-jobs/{jobKey}")
    public ApiResponse<AdminBookImportJobResponse> getImportJob(@PathVariable String jobKey) {
        return ApiResponse.success(adminBookService.getImportJob(jobKey));
    }

    /**
     * 查询最近导入任务列表。
     */
    @GetMapping("/import-jobs")
    public ApiResponse<List<AdminBookImportJobResponse>> listRecentImportJobs(
        @RequestParam(defaultValue = "20") int limit
    ) {
        return ApiResponse.success(adminBookService.listRecentImportJobs(limit));
    }

    /**
     * 查询导入任务下的章节暂存列表。
     */
    @GetMapping("/import-jobs/{jobKey}/chapters")
    public ApiResponse<List<AdminBookImportChapterResponse>> listImportJobChapters(@PathVariable String jobKey) {
        return ApiResponse.success(adminBookService.listImportJobChapters(jobKey));
    }

    /**
     * 更新导入任务的书籍元数据。
     * 发布前保存书名、作者和译者修正结果，后续发布流程直接复用任务字段。
     */
    @PutMapping("/import-jobs/{jobKey}/metadata")
    public ApiResponse<AdminBookImportJobResponse> updateImportJobMetadata(
        @PathVariable String jobKey,
        @Valid @RequestBody AdminBookImportMetadataUpdateRequest request
    ) {
        return ApiResponse.success(adminBookService.updateImportJobMetadata(jobKey, request));
    }

    /**
     * 更新导入章节暂存内容。
     */
    @PutMapping("/import-jobs/{jobKey}/chapters/{tempChapterKey}")
    public ApiResponse<AdminBookImportChapterResponse> updateImportJobChapter(
        @PathVariable String jobKey,
        @PathVariable String tempChapterKey,
        @Valid @RequestBody AdminBookImportChapterUpdateRequest request
    ) {
        return ApiResponse.success(adminBookService.updateImportJobChapter(jobKey, tempChapterKey, request));
    }

    /**
     * 批量更新导入章节暂存内容。
     * 前端汇总多个章节草稿后一次提交，减少逐章保存的重复请求。
     */
    @PutMapping("/import-jobs/{jobKey}/chapters")
    public ApiResponse<List<AdminBookImportChapterResponse>> updateImportJobChapters(
        @PathVariable String jobKey,
        @Valid @RequestBody AdminBookImportChapterBatchUpdateRequest request
    ) {
        return ApiResponse.success(adminBookService.updateImportJobChapters(jobKey, request));
    }

    /**
     * 发布导入任务中的整本书。
     */
    @PostMapping("/import-jobs/{jobKey}/publish")
    public ApiResponse<BookResponse> publishImportJob(@PathVariable String jobKey) {
        return ApiResponse.success(adminBookService.publishImportJob(jobKey));
    }

    /**
     * 批量通过导入任务审核。
     * 接收任务主键集合后统一标记通过，前端可继续选择批量发布。
     */
    @PostMapping("/import-jobs/batch/approve")
    public ApiResponse<AdminBookImportBatchResponse> approveImportJobs(
        @Valid @RequestBody AdminBookImportBatchRequest request
    ) {
        return ApiResponse.success(adminBookService.approveImportJobs(request));
    }

    /**
     * 批量拒绝导入任务审核。
     * 接收任务主键集合和原因后统一标记拒绝，防止误进入发布队列。
     */
    @PostMapping("/import-jobs/batch/reject")
    public ApiResponse<AdminBookImportBatchResponse> rejectImportJobs(
        @Valid @RequestBody AdminBookImportBatchRequest request
    ) {
        return ApiResponse.success(adminBookService.rejectImportJobs(request));
    }

    /**
     * 批量发布导入任务中的书籍。
     * 按任务逐本复用发布流程并汇总结果，单本异常不会影响其他任务。
     */
    @PostMapping("/import-jobs/batch/publish")
    public ApiResponse<AdminBookImportBatchResponse> publishImportJobs(
        @Valid @RequestBody AdminBookImportBatchRequest request
    ) {
        return ApiResponse.success(adminBookService.publishImportJobs(request));
    }

    /**
     * 查询书籍列表。
     */
    @GetMapping
    public ApiResponse<List<BookResponse>> listBooks() {
        return ApiResponse.success(adminBookService.listBooks());
    }

    /**
     * 更新书籍分类标签。
     * 根据书籍业务主键修改分类，并同步导入任务里的分类字段。
     */
    @PutMapping("/{bookKey}/category")
    public ApiResponse<BookResponse> updateBookCategory(
        @PathVariable String bookKey,
        @Valid @RequestBody AdminBookCategoryUpdateRequest request
    ) {
        return ApiResponse.success(adminBookService.updateBookCategory(bookKey, request));
    }

    /**
     * 将书籍移入已删除列表。
     * 下线前台正式书籍并保留导入任务与资源文件，便于后续恢复原始状态。
     */
    @DeleteMapping("/{bookKey}")
    public ApiResponse<Void> softDeleteBook(@PathVariable String bookKey) {
        adminBookService.softDeleteBook(bookKey);
        return ApiResponse.success(null);
    }

    /**
     * 彻底删除书籍及其关联导入记录。
     * 同步清理数据库记录与对象存储资源，确保后台不再保留任何可恢复缓存。
     */
    @DeleteMapping("/{bookKey}/hard")
    public ApiResponse<Void> hardDeleteBook(@PathVariable String bookKey) {
        adminBookService.hardDeleteBook(bookKey);
        return ApiResponse.success(null);
    }

    /**
     * 删除指定导入任务及其关联数据。
     * 清理暂存章节和导入主记录；若任务已发布，同步删除正式书籍和章节。
     */
    @DeleteMapping("/import-jobs/{jobKey}")
    public ApiResponse<Void> deleteImportJob(@PathVariable String jobKey) {
        adminBookService.deleteImportJob(jobKey);
        return ApiResponse.success(null);
    }

    /**
     * 彻底删除指定导入任务及其关联资源。
     * 同步清理数据库记录与对象存储资源，删除后不再保留恢复入口与缓存文件。
     */
    @DeleteMapping("/import-jobs/{jobKey}/hard")
    public ApiResponse<Void> hardDeleteImportJob(@PathVariable String jobKey) {
        adminBookService.hardDeleteImportJob(jobKey);
        return ApiResponse.success(null);
    }

    /**
     * 恢复已删除的导入任务和关联书籍。
     * 后台从已删除列表触发后还原任务状态，并让已发布书籍重新进入展示列表。
     */
    @PostMapping("/import-jobs/{jobKey}/restore")
    public ApiResponse<AdminBookImportJobResponse> restoreImportJob(@PathVariable String jobKey) {
        return ApiResponse.success(adminBookService.restoreImportJob(jobKey));
    }

    /**
     * 批量删除导入任务。
     * 接收任务主键集合后逐个删除，单条失败不影响其他任务。
     */
    @PostMapping("/import-jobs/batch/delete")
    public ApiResponse<AdminBookImportBatchResponse> batchDeleteImportJobs(
        @Valid @RequestBody AdminBookImportBatchRequest request
    ) {
        return ApiResponse.success(adminBookService.batchDeleteImportJobs(request));
    }

    /**
     * 按分类删除导入任务。
     * 清理指定分类下所有导入记录及其暂存章节，已发布的正式书籍不受影响。
     */
    @DeleteMapping("/import-jobs/category/{category}")
    public ApiResponse<Integer> deleteImportJobsByCategory(@PathVariable String category) {
        int count = adminBookService.deleteImportJobsByCategory(category);
        return ApiResponse.success(count);
    }

    /**
     * 重新从源文件中解析封面并更新导入任务。
     * 已导入但封面未正确解析或封面图丢失的任务可调用此接口，
     * 无需重新上传 ZIP 即可从已有源文件中恢复封面图片。
     */
    @PostMapping("/import-jobs/{jobKey}/repair-cover")
    public ApiResponse<AdminBookImportJobResponse> repairImportJobCover(@PathVariable String jobKey) throws Exception {
        return ApiResponse.success(adminBookService.repairImportJobCover(jobKey));
    }

    /**
     * 统一校验上传书籍文件：文件名扩展名、Content-Type、文件魔数、大小限制。
     * 防止将可执行文件或恶意脚本伪装成书籍格式上传。
     */
    private String validateBookFile(MultipartFile file, Set<String> allowedExtensions) {
        if (file.isEmpty()) {
            return "上传文件不能为空";
        }

        String originalName = file.getOriginalFilename();
        if (originalName == null || !hasAllowedBookExtension(originalName, allowedExtensions)) {
            return "仅支持上传 " + String.join(" / ", allowedExtensions) + " 格式文件";
        }

        if (file.getSize() > MAX_UPLOAD_SIZE) {
            return "文件大小超过 200MB 限制";
        }

        if (!hasValidBookMagicBytes(file)) {
            return "文件内容与扩展名不匹配，请确认文件格式正确";
        }

        return null;
    }

    private boolean hasAllowedBookExtension(String filename, Set<String> allowedExtensions) {
        int dot = filename.lastIndexOf('.');
        if (dot < 0) {
            return false;
        }
        return allowedExtensions.contains(filename.substring(dot + 1).toLowerCase(Locale.ROOT));
    }

    /**
     * 读取文件头部魔数校验真实格式。
     * EPUB/DOCX 是 ZIP 压缩包，与 ZIP 共用魔数 PK\x03\x04。
     * TXT/MD/HTML 为纯文本格式，无固定魔数，跳过校验。
     */
    private boolean hasValidBookMagicBytes(MultipartFile file) {
        String originalName = file.getOriginalFilename();
        if (originalName == null) {
            return false;
        }
        String ext = originalName.substring(originalName.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);

        // 纯文本格式无固定魔数，跳过校验
        if (Set.of("txt", "md", "markdown", "html", "htm").contains(ext)) {
            return true;
        }

        try (InputStream in = file.getInputStream()) {
            byte[] header = new byte[4];
            int read = in.read(header);
            if (read < 4) {
                return false;
            }

            // PDF: 25 50 44 46 (%PDF)
            if (ext.equals("pdf")) {
                return match(header, 0, 0x25, 0x50, 0x44, 0x46);
            }

            // ZIP / EPUB / DOCX: 50 4B 03 04 (PK..)
            if (ext.equals("zip") || ext.equals("epub") || ext.equals("docx")) {
                return match(header, 0, 0x50, 0x4B, 0x03, 0x04);
            }

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
