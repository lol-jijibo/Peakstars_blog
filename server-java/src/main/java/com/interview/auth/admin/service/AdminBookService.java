package com.interview.auth.admin.service;

import com.interview.auth.admin.dto.request.AdminBookImportBatchRequest;
import com.interview.auth.admin.dto.request.AdminBookCategoryUpdateRequest;
import com.interview.auth.admin.dto.request.AdminBookImportChapterBatchUpdateRequest;
import com.interview.auth.admin.dto.request.AdminBookImportChapterUpdateRequest;
import com.interview.auth.admin.dto.request.AdminBookImportExternalRequest;
import com.interview.auth.admin.dto.request.AdminBookImportMetadataUpdateRequest;
import com.interview.auth.admin.dto.response.AdminBookImportBatchResponse;
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
     * 更新导入任务的书籍元数据。
     * 保存发布前人工校正的书名、作者和译者，覆盖自动解析出的任务字段。
     */
    AdminBookImportJobResponse updateImportJobMetadata(String jobKey, AdminBookImportMetadataUpdateRequest request);

    /**
     * 更新导入任务下某一章的暂存内容。
     */
    AdminBookImportChapterResponse updateImportJobChapter(String jobKey, String tempChapterKey, AdminBookImportChapterUpdateRequest request);

    /**
     * 批量更新导入任务下多个章节的暂存内容。
     * 前端一次提交多个章节草稿，服务层逐章写入并返回最新章节列表。
     */
    List<AdminBookImportChapterResponse> updateImportJobChapters(String jobKey, AdminBookImportChapterBatchUpdateRequest request);

    /**
     * 将导入任务中的书籍与章节正式发布到用户端。
     */
    BookResponse publishImportJob(String jobKey);

    /**
     * 批量通过书籍导入任务审核。
     * 校验任务可审核后统一标记为已通过，方便后续批量发布。
     */
    AdminBookImportBatchResponse approveImportJobs(AdminBookImportBatchRequest request);

    /**
     * 批量拒绝书籍导入任务审核。
     * 校验任务可拒绝后统一写入拒绝状态和原因，避免进入发布流程。
     */
    AdminBookImportBatchResponse rejectImportJobs(AdminBookImportBatchRequest request);

    /**
     * 批量发布书籍导入任务。
     * 逐个复用单本发布流程并汇总结果，单本失败不会阻断其他任务上线。
     */
    AdminBookImportBatchResponse publishImportJobs(AdminBookImportBatchRequest request);

    /**
     * 查询后台书籍列表。
     */
    List<BookResponse> listBooks();

    /**
     * 更新已发布书籍的分类标签。
     * 根据书籍主键同步正式书籍与导入任务分类，保证后台总览和审核页口径一致。
     */
    BookResponse updateBookCategory(String bookKey, AdminBookCategoryUpdateRequest request);

    /**
     * 将已发布书籍移入已删除列表。
     * 同步下线正式书籍并保留导入任务与资源文件，便于后续恢复原始状态。
     */
    void softDeleteBook(String bookKey);

    /**
     * 彻底删除已发布书籍及其关联导入记录。
     * 清理数据库记录与对象存储资源，确保书籍内容不再保留任何可恢复缓存。
     */
    void hardDeleteBook(String bookKey);

    /**
     * 删除指定导入任务及其关联数据。
     * 清理暂存章节和导入主记录；若任务已发布，同步删除正式书籍和章节。
     */
    void deleteImportJob(String jobKey);

    /**
     * 彻底删除指定导入任务及其关联资源。
     * 清理数据库中的导入记录、正式书籍与章节，并同步回收对象存储中的封面、正文图片和源文件。
     */
    void hardDeleteImportJob(String jobKey);

    /**
     * 恢复已删除的书籍导入任务。
     * 将任务状态还原为删除前状态，并按需恢复正式书籍的展示状态。
     */
    AdminBookImportJobResponse restoreImportJob(String jobKey);

    /**
     * 批量删除导入任务。
     * 逐个删除导入记录及关联数据，汇总成功与失败结果。
     */
    AdminBookImportBatchResponse batchDeleteImportJobs(AdminBookImportBatchRequest request);

    /**
     * 按分类删除导入任务。
     * 清理指定分类下所有导入记录及其暂存章节，已发布的正式书籍不受影响。
     */
    int deleteImportJobsByCategory(String category);

    /**
     * 重新从源文件中解析封面并更新导入任务。
     * 用于已导入但封面未正确解析的任务，无需重新上传文件即可恢复封面图片。
     *
     * @param jobKey 导入任务主键
     * @return 更新后的导入任务响应，coverUrl 已填充
     */
    AdminBookImportJobResponse repairImportJobCover(String jobKey) throws Exception;
}
