package com.interview.auth.service.impl;

import com.interview.auth.domain.dto.response.BookChapterResponse;
import com.interview.auth.domain.dto.response.BookResponse;
import com.interview.auth.domain.entity.Book;
import com.interview.auth.domain.entity.BookChapter;
import com.interview.auth.infrastructure.mapper.BookMapper;
import com.interview.auth.service.BookService;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 集中承接用户端书籍列表、详情与章节读取逻辑。
 * 统一完成书籍主表和章节表的字段转换，保证阅读页与书架页直接消费稳定响应结构。
 */
@Service
@RequiredArgsConstructor
public class BookServiceImpl implements BookService {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final BookMapper bookMapper;

    /**
     * 查询全部已发布书籍。
     * 按发布时间与排序值输出书籍概览，供用户端书架页和推荐区直接复用。
     */
    @Override
    public List<BookResponse> listPublishedBooks() {
        return bookMapper.findPublishedBooks().stream().map(this::toBookResponse).toList();
    }

    /**
     * 查询指定书籍详情。
     * 根据书籍业务主键读取已发布主记录并转换成用户端书籍详情结构。
     */
    @Override
    public BookResponse getBookDetail(String bookKey) {
        Book book = bookMapper.findPublishedBookByKey(bookKey);
        return book == null ? null : toBookResponse(book);
    }

    /**
     * 查询指定书籍的已发布章节目录。
     * 只返回目录和免费标记所需字段，供阅读页目录面板按需加载展示。
     */
    @Override
    public List<BookChapterResponse> listBookChapters(String bookKey) {
        return bookMapper.findPublishedChaptersByBookKey(bookKey).stream().map(this::toBookChapterResponse).toList();
    }

    /**
     * 查询指定书籍下某一章正文。
     * 使用书籍主键和章节主键双重定位章节，保证阅读页按目录跳转时拿到精确内容。
     */
    @Override
    public BookChapterResponse getBookChapterDetail(String bookKey, String chapterKey) {
        BookChapter chapter = bookMapper.findPublishedChapterByKey(bookKey, chapterKey);
        return chapter == null ? null : toBookChapterResponse(chapter);
    }

    /**
     * 将书籍实体转换为用户端详情对象。
     * 统一拆分标签列表和格式化发布时间，避免前端重复理解数据库存储格式。
     */
    private BookResponse toBookResponse(Book book) {
        BookResponse response = new BookResponse();
        response.setId(book.getBookKey());
        response.setTitle(book.getTitle());
        response.setAuthor(book.getAuthor());
        response.setPublisher(book.getPublisher());
        response.setCategory(book.getCategory());
        response.setSummary(book.getSummary());
        response.setCoverUrl(book.getCoverUrl());
        response.setTags(splitPipeValues(book.getTagList()));
        response.setWordCount(defaultInt(book.getWordCount()));
        response.setChapterCount(defaultInt(book.getChapterCount()));
        response.setReadCount(defaultInt(book.getReadCount()));
        response.setRating(book.getRating());
        response.setPublishedAt(book.getPublishedAt() == null ? null : book.getPublishedAt().format(DATE_TIME_FORMATTER));
        return response;
    }

    /**
     * 将章节实体转换为阅读页可直接消费的目录与正文对象。
     * 统一把数据库 0/1 免费标记转换成布尔值，减少前端目录模板分支判断。
     */
    private BookChapterResponse toBookChapterResponse(BookChapter chapter) {
        BookChapterResponse response = new BookChapterResponse();
        response.setId(chapter.getChapterKey());
        response.setChapterNo(defaultInt(chapter.getChapterNo()));
        response.setTitle(chapter.getTitle());
        response.setSubtitle(chapter.getSubtitle());
        response.setContentHtml(chapter.getContentHtml());
        response.setWordCount(defaultInt(chapter.getWordCount()));
        response.setFree(chapter.getIsFree() != null && chapter.getIsFree() == 1);
        return response;
    }

    /**
     * 把竖线分隔标签字段还原为数组。
     * 在后端统一拆分持久化格式，保证前端标签组件只处理标准数组结构。
     */
    private List<String> splitPipeValues(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return List.of();
        }
        return Arrays.stream(rawValue.split("\\|"))
            .map(String::trim)
            .filter(item -> !item.isEmpty())
            .toList();
    }

    /**
     * 把可空整数字段统一转换为非空数值。
     * 避免阅读页统计卡片和目录字数在空值场景下出现空指针或显示异常。
     */
    private int defaultInt(Integer value) {
        return value == null ? 0 : value;
    }
}
