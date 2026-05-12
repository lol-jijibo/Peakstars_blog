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

    int saveImportJob(BookImportJob importJob);

    BookImportJob findImportJobByKey(@Param("jobKey") String jobKey);

    int updateImportJob(BookImportJob importJob);

    int deleteImportStageByJobKey(@Param("jobKey") String jobKey);

    int batchInsertImportStages(@Param("stages") List<BookImportChapterStage> stages);

    List<BookImportChapterStage> findImportStagesByJobKey(@Param("jobKey") String jobKey);

    BookImportChapterStage findImportStageByKey(@Param("jobKey") String jobKey, @Param("tempChapterKey") String tempChapterKey);

    int updateImportStage(BookImportChapterStage stage);

    int saveBook(Book book);

    int deleteBookChaptersByBookKey(@Param("bookKey") String bookKey);

    int batchInsertBookChapters(@Param("chapters") List<BookChapter> chapters);

    List<Book> findAllBooks();

    List<BookImportJob> listImportJobs(@Param("limit") int limit);
}
