package com.interview.auth.admin.mapper;

import com.interview.auth.admin.entity.BookImportChapterStage;
import com.interview.auth.admin.entity.BookImportJob;
import com.interview.auth.admin.entity.BookSourceFile;
import com.interview.auth.domain.entity.Book;
import com.interview.auth.domain.entity.BookChapter;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 承接后台书籍导入模块的查询、暂存与发布 SQL 映射。
 * 统一复用 MyBatis 管理导入任务、原始文件、暂存章节和正式书籍数据，避免在 Service 层拼装原始 SQL。
 */
@Mapper
public interface AdminBookMapper {

    int saveSourceFile(BookSourceFile sourceFile);

    /**
     * 查询导入任务关联的原始源文件记录。
     * 通过 sourceFileKey 回查存档文件地址，供旧任务补解析封面时复用。
     */
    BookSourceFile findSourceFileByKey(@Param("fileKey") String fileKey);

    /**
     * 删除指定源文件归档记录。
     * 在彻底清理导入任务后移除原始导入包索引，避免后台残留无效源文件元数据。
     */
    int deleteSourceFileByKey(@Param("fileKey") String fileKey);

    int saveImportJob(BookImportJob importJob);

    BookImportJob findImportJobByKey(@Param("jobKey") String jobKey);

    int updateImportJob(BookImportJob importJob);

    int deleteImportStageByJobKey(@Param("jobKey") String jobKey);

    int batchInsertImportStages(@Param("stages") List<BookImportChapterStage> stages);

    List<BookImportChapterStage> findImportStagesByJobKey(@Param("jobKey") String jobKey);

    BookImportChapterStage findImportStageByKey(@Param("jobKey") String jobKey, @Param("tempChapterKey") String tempChapterKey);

    int updateImportStage(BookImportChapterStage stage);

    int saveBook(Book book);

    /**
     * 更新已发布书籍的主信息与统计字段。
     * 保存导入审核页的修改时同步正式书籍表，保留阅读量和发布时间等运营数据。
     */
    int updatePublishedBookFromImportJob(Book book);

    /**
     * 查询指定业务主键对应的正式书籍。
     * 用于总览页编辑标签和删除前校验书籍是否存在。
     */
    Book findBookByKey(@Param("bookKey") String bookKey);

    /**
     * 更新正式书籍的分类标签字段。
     * 同步维护标签列表字段，保证总览筛选和用户端展示一致。
     */
    int updateBookCategory(@Param("bookKey") String bookKey, @Param("category") String category);

    /**
     * 更新正式书籍的上下线状态。
     * 通过书籍主键切换 status 字段，配合导入任务删除与恢复流程同步前台展示。
     */
    int updateBookStatusByKey(@Param("bookKey") String bookKey, @Param("status") int status);

    /**
     * 更新已发布书籍关联导入任务的分类字段。
     * 让审核页再次打开时能看到总览页保存后的最新标签。
     */
    int updateImportJobCategoryByBookKey(@Param("bookKey") String bookKey, @Param("category") String category);

    /**
     * 删除指定书籍关联导入任务下的章节暂存。
     * 通过导入任务关联书籍主键定位暂存章节，避免残留审核数据。
     */
    int deleteImportStageByBookKey(@Param("bookKey") String bookKey);

    /**
     * 删除指定书籍关联的导入任务记录。
     * 在正式书籍删除时同步清理导入历史，避免总览再次聚合出旧数据。
     */
    int deleteImportJobByBookKey(@Param("bookKey") String bookKey);

    /**
     * 删除指定业务主键对应的正式书籍。
     * 在章节和导入数据清理完成后移除主记录，完成彻底删除。
     */
    int deleteBookByKey(@Param("bookKey") String bookKey);

    int deleteBookChaptersByBookKey(@Param("bookKey") String bookKey);

    /**
     * 查询指定书籍下的正式章节记录。
     * 供彻底删除书籍前扫描正文资源链接，保证对象存储文件可被同步回收。
     */
    List<BookChapter> findBookChaptersByBookKey(@Param("bookKey") String bookKey);

    int batchInsertBookChapters(@Param("chapters") List<BookChapter> chapters);

    List<Book> findAllBooks();

    List<BookImportJob> listImportJobs(@Param("limit") int limit);

    /**
     * 查询指定正式书籍关联的导入任务。
     * 用于从总览删除书籍时同步把导入记录移入已删除列表。
     */
    List<BookImportJob> findImportJobsByBookKey(@Param("bookKey") String bookKey);

    /**
     * 删除单条导入任务主记录。
     * 供彻底删除指定任务时使用，避免按 bookKey 误删同书籍下其他导入历史。
     */
    int deleteImportJobByKey(@Param("jobKey") String jobKey);

    /**
     * 删除指定业务主键对应的导入任务。
     * 在清理暂存章节后移除主记录，避免导入记录残留。
     */
    /**
     * 将导入任务标记为已删除并保存原始状态。
     * 利用 message 暂存删除前状态，便于后续恢复到原展示位置。
     */
    int updateImportJobDeletedByKey(@Param("jobKey") String jobKey, @Param("message") String message);

    /**
     * 将已删除导入任务恢复到删除前状态。
     * 优先从 message 回填 status，缺失时回退到待审核状态。
     */
    int restoreImportJobStatusByKey(@Param("jobKey") String jobKey, @Param("message") String message);

    /**
     * 按分类查询导入任务列表。
     * 用于分类删除前定位待清理的导入记录。
     */
    List<BookImportJob> findImportJobsByCategory(@Param("category") String category);

    /**
     * 按分类删除导入任务。
     * 清理指定分类下所有导入记录，不删除已发布的正式书籍。
     */
    int deleteImportJobsByCategory(@Param("category") String category);

    /**
     * 按分类删除导入暂存章节。
     * 通过 JOIN 定位分类对应任务下的暂存章节，与分类删除任务同步清理。
     */
    int deleteImportStagesByCategory(@Param("category") String category);
}
