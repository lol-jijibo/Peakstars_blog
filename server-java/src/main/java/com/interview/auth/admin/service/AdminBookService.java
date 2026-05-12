package com.interview.auth.admin.service;

import com.interview.auth.admin.dto.request.AdminBookImportChapterUpdateRequest;
import com.interview.auth.admin.dto.request.AdminBookImportExternalRequest;
import com.interview.auth.admin.dto.response.AdminBookImportChapterResponse;
import com.interview.auth.admin.dto.response.AdminBookImportJobResponse;
import com.interview.auth.domain.dto.response.BookResponse;
import java.io.InputStream;
import java.util.List;

/**
 * 定义后台书籍导入与发布模块的对外契约，屏蔽底层实现细节。
 * 实现类按此接口完成导入任务管理、章节审核与正式发布，便于替换和单元测试。
 */
public interface AdminBookService {

    /**
     * 创建书籍 ZIP 导入任务并立即完成解析预处理。
     * 接收上传包后落源文件、解压章节并写入暂存表，返回后台轮询所需的任务结果。
     */
    AdminBookImportJobResponse createImportJob(String fileName, InputStream inputStream, long size, String contentType) throws Exception;

    /**
     * 创建单文件导入任务（支持 epub/pdf/txt/md/docx/html）。
     * 自动检测文件格式并选择对应的解析器，将内容拆分为章节暂存。
     */
    AdminBookImportJobResponse createImportJobFromFile(String fileName, InputStream inputStream, long size, String contentType) throws Exception;

    /**
     * 创建外部资源导入任务。
     * 通过 URL 拉取外部内容，解析后写入章节暂存表。
     */
    AdminBookImportJobResponse createImportJobFromExternal(AdminBookImportExternalRequest request) throws Exception;

    /**
     * 查询指定书籍导入任务的当前状态。
     */
    AdminBookImportJobResponse getImportJob(String jobKey);

    /**
     * 查询最近的导入任务列表。
     */
    List<AdminBookImportJobResponse> listRecentImportJobs(int limit);

    /**
     * 查询指定导入任务下的章节暂存列表。
     */
    List<AdminBookImportChapterResponse> listImportJobChapters(String jobKey);

    /**
     * 更新导入任务下某一章的暂存内容。
     */
    AdminBookImportChapterResponse updateImportJobChapter(String jobKey, String tempChapterKey, AdminBookImportChapterUpdateRequest request);

    /**
     * 将导入任务中的书籍与章节正式发布到用户端。
     */
    BookResponse publishImportJob(String jobKey);

    /**
     * 查询后台书籍列表。
     */
    List<BookResponse> listBooks();
}
