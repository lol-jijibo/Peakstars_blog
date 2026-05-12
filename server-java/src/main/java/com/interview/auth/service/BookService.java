package com.interview.auth.service;

import com.interview.auth.domain.dto.response.BookChapterResponse;
import com.interview.auth.domain.dto.response.BookResponse;
import java.util.List;

/**
 * 定义书籍模块对用户端的读取契约，屏蔽底层导入与存储实现细节。
 * 实现类按此接口组装书籍列表、详情与章节正文，便于前端阅读页和后续搜索扩展。
 */
public interface BookService {

    /**
     * 查询用户端可见的书籍列表。
     * 按发布时间与排序值输出已发布书籍，供书架页和推荐区直接展示。
     */
    List<BookResponse> listPublishedBooks();

    /**
     * 查询指定书籍的详情信息。
     * 根据书籍业务主键读取主记录并补齐展示字段，供阅读页左侧信息面板使用。
     */
    BookResponse getBookDetail(String bookKey);

    /**
     * 查询指定书籍的已发布章节目录。
     * 只返回目录级字段和免费标记，供阅读页目录面板和章节跳转使用。
     */
    List<BookChapterResponse> listBookChapters(String bookKey);

    /**
     * 查询指定书籍下某一章的已发布正文。
     * 根据书籍和章节双主键读取正文内容，供用户端阅读面板按需加载章节。
     */
    BookChapterResponse getBookChapterDetail(String bookKey, String chapterKey);
}
