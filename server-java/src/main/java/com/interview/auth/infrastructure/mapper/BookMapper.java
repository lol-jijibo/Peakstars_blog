package com.interview.auth.infrastructure.mapper;

import com.interview.auth.domain.entity.Book;
import com.interview.auth.domain.entity.BookChapter;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 承接书籍模块对用户端的读取 SQL 映射。
 * 统一复用 MyBatis 映射管理书籍主表和章节表，避免在 Service 层拼接原始 SQL。
 */
@Mapper
public interface BookMapper {

    List<Book> findPublishedBooks();

    Book findPublishedBookByKey(@Param("bookKey") String bookKey);

    List<BookChapter> findPublishedChaptersByBookKey(@Param("bookKey") String bookKey);

    List<BookChapter> findPublishedChaptersLightByBookKey(@Param("bookKey") String bookKey);

    BookChapter findPublishedChapterByKey(@Param("bookKey") String bookKey, @Param("chapterKey") String chapterKey);
}
