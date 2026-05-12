package com.interview.auth.controller;

import com.interview.auth.common.ApiResponse;
import com.interview.auth.domain.dto.response.BookChapterResponse;
import com.interview.auth.domain.dto.response.BookResponse;
import com.interview.auth.service.BookService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 对外暴露书籍列表、详情与章节阅读接口。
 * 控制器只负责统一响应封装，书籍与章节字段组装全部交由 Service 层处理。
 */
@RestController
@RequestMapping("/api/content/books")
@RequiredArgsConstructor
public class BookController {

    private final BookService bookService;

    /**
     * 提供用户端书籍列表接口。
     * 返回已发布书籍概览，供书架页和阅读推荐区直接渲染卡片列表。
     */
    @GetMapping
    public ApiResponse<List<BookResponse>> listBooks() {
        return ApiResponse.success(bookService.listPublishedBooks());
    }

    /**
     * 提供用户端书籍详情接口。
     * 根据书籍主键返回书籍信息面板所需字段，供阅读详情页左侧区域使用。
     */
    @GetMapping("/{bookKey}")
    public ApiResponse<BookResponse> getBookDetail(@PathVariable String bookKey) {
        return ApiResponse.success(bookService.getBookDetail(bookKey));
    }

    /**
     * 提供用户端书籍章节目录接口。
     * 返回已发布章节目录和免费标记，供阅读页目录面板和章节切换使用。
     */
    @GetMapping("/{bookKey}/chapters")
    public ApiResponse<List<BookChapterResponse>> listBookChapters(@PathVariable String bookKey) {
        return ApiResponse.success(bookService.listBookChapters(bookKey));
    }

    /**
     * 提供用户端单章正文接口。
     * 根据书籍和章节双主键读取已发布正文，供阅读页按需加载当前章节内容。
     */
    @GetMapping("/{bookKey}/chapters/{chapterKey}")
    public ApiResponse<BookChapterResponse> getBookChapterDetail(
        @PathVariable String bookKey,
        @PathVariable String chapterKey
    ) {
        return ApiResponse.success(bookService.getBookChapterDetail(bookKey, chapterKey));
    }
}
