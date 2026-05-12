package com.interview.auth.admin.controller;

import com.interview.auth.admin.dto.request.AdminBookImportChapterUpdateRequest;
import com.interview.auth.admin.dto.request.AdminBookImportExternalRequest;
import com.interview.auth.admin.dto.response.AdminBookImportChapterResponse;
import com.interview.auth.admin.dto.response.AdminBookImportJobResponse;
import com.interview.auth.admin.service.AdminBookService;
import com.interview.auth.common.ApiResponse;
import com.interview.auth.domain.dto.response.BookResponse;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
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

    private final AdminBookService adminBookService;

    /**
     * 创建 ZIP 书稿导入任务。
     */
    @PostMapping("/import-jobs")
    public ApiResponse<AdminBookImportJobResponse> createImportJob(@RequestParam("file") MultipartFile file) throws Exception {
        if (file.isEmpty()) {
            return ApiResponse.fail(400, "上传文件不能为空");
        }
        if (file.getSize() > MAX_UPLOAD_SIZE) {
            return ApiResponse.fail(400, "文件大小超过 200MB 限制");
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
        if (file.isEmpty()) {
            return ApiResponse.fail(400, "上传文件不能为空");
        }
        if (file.getSize() > MAX_UPLOAD_SIZE) {
            return ApiResponse.fail(400, "文件大小超过 200MB 限制");
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
     * 发布导入任务中的整本书。
     */
    @PostMapping("/import-jobs/{jobKey}/publish")
    public ApiResponse<BookResponse> publishImportJob(@PathVariable String jobKey) {
        return ApiResponse.success(adminBookService.publishImportJob(jobKey));
    }

    /**
     * 查询书籍列表。
     */
    @GetMapping
    public ApiResponse<List<BookResponse>> listBooks() {
        return ApiResponse.success(adminBookService.listBooks());
    }
}
