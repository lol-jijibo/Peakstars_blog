package com.interview.auth.service.impl;

import com.interview.auth.common.BusinessException;
import com.interview.auth.domain.dto.response.BookChapterResponse;
import com.interview.auth.domain.dto.response.BookResponse;
import com.interview.auth.domain.entity.Book;
import com.interview.auth.domain.entity.BookChapter;
import com.interview.auth.infrastructure.mapper.BookMapper;
import com.interview.auth.infrastructure.storage.ContentStorageService;
import com.interview.auth.infrastructure.storage.StorageRoutingService;
import com.interview.auth.service.BookService;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
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
    private final ContentStorageService contentStorageService;
    private final StorageRoutingService storageRoutingService;

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
     * 过滤封面占位页后重新编号，供阅读页目录面板按需加载展示。
     */
    @Override
    public List<BookChapterResponse> listBookChapters(String bookKey) {
        return bookMapper.findPublishedChaptersLightByBookKey(bookKey).stream().map(this::toBookChapterResponse).toList();
    }

    /**
     * 查询指定书籍下某一章正文。
     * 使用书籍主键和章节主键双重定位章节，并跳过误入正文流的封面占位页。
     */
    @Override
    public BookChapterResponse getBookChapterDetail(String bookKey, String chapterKey) {
        BookChapter chapter = bookMapper.findPublishedChapterByKey(bookKey, chapterKey);
        if (chapter == null) return null;
        Book book = bookMapper.findPublishedBookByKey(bookKey);
        return toBookChapterResponse(chapter, resolveCoverUrl(book == null ? "" : book.getCoverUrl()));
    }

    /**
     * 将书籍实体转换为用户端详情对象。
     * 统一拆分标签列表并规范封面地址，避免前端重复理解数据库存储格式。
     */
    private BookResponse toBookResponse(Book book) {
        BookResponse response = new BookResponse();
        response.setId(book.getBookKey());
        response.setTitle(book.getTitle());
        response.setAuthor(book.getAuthor());
        response.setTranslator(book.getTranslator());
        response.setPublisher(book.getPublisher());
        response.setCategory(book.getCategory());
        response.setSummary(book.getSummary());
        response.setCoverUrl(resolveCoverUrl(book.getCoverUrl()));
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
     * 统一补齐章节展示序号和正文图片地址，减少前端模板分支判断。
     */
    private BookChapterResponse toBookChapterResponse(BookChapter chapter) {
        return toBookChapterResponse(chapter, "");
    }

    /**
     * 将章节实体转换为用户端响应，同时对封面章图片做兜底替换。
     * 当正文里还残留 cover.jpeg 这类裸文件名时，用书籍封面地址回填保证用户端可直接展示。
     */
    private BookChapterResponse toBookChapterResponse(BookChapter chapter, String bookCoverUrl) {
        BookChapterResponse response = new BookChapterResponse();
        response.setId(chapter.getChapterKey());
        response.setChapterNo(defaultInt(chapter.getChapterNo()));
        response.setTitle(chapter.getTitle());
        response.setSubtitle(chapter.getSubtitle());
        response.setContentHtml(resolveChapterContentHtml(chapter.getContentHtml(), bookCoverUrl));
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

    /**
     * 统一整理书籍封面地址，确保用户端拿到可直接访问的路径。
     * 保留 OSS 直链和历史代理地址，避免新上传封面继续绕回后端代理。
     */
    private String resolveCoverUrl(String coverUrl) {
        if (coverUrl == null || coverUrl.isBlank()) {
            return "/peakstars-blog-icon.jpg";
        }
        String trimmed = coverUrl.trim();
        if (trimmed.startsWith("/uploads/")) {
            return trimmed;
        }
        if (trimmed.startsWith("uploads/")) {
            return "/" + trimmed;
        }
        if (isManagedStorageUrl(trimmed) && trimmed.startsWith("http")) {
            int pathIndex = trimmed.indexOf("/uploads/");
            if (pathIndex >= 0) {
                return trimmed.substring(pathIndex);
            }
        }
        return trimmed;
    }

    /**
     * 统一修正章节 HTML 里的图片地址，保证用户端详情页能直接渲染。
     * 遍历 img 和 svg image 节点后回填主地址属性，把站内资源统一改写成稳定的代理路径。
     */
    private String resolveChapterContentHtml(String contentHtml, String bookCoverUrl) {
        if (contentHtml == null || contentHtml.isBlank()) {
            return contentHtml;
        }
        Document document = Jsoup.parseBodyFragment(contentHtml);
        for (Element image : document.select("img, image")) {
            String attributeName = resolvePrimaryImageAttribute(image);
            if (attributeName.isBlank()) {
                continue;
            }
            String originalUrl = String.valueOf(image.attr(attributeName) == null ? "" : image.attr(attributeName)).trim();
            String resolvedUrl = resolveStoredAssetUrl(originalUrl);
            if (resolvedUrl.equals(originalUrl)) {
                String coverFallbackUrl = resolveCoverPlaceholderImageUrl(image, originalUrl, bookCoverUrl);
                if (!coverFallbackUrl.isBlank()) {
                    resolvedUrl = coverFallbackUrl;
                }
            }
            if (!resolvedUrl.equals(image.attr(attributeName))) {
                image.attr(attributeName, resolvedUrl);
            }
        }
        return document.body().html();
    }

    /**
     * 将封面章里常见的裸文件名图片引用替换成书籍封面地址。
     * 仅处理无路径、无协议的图片引用，避免误改正文中正常的相对资源路径。
     */
    private String resolveCoverPlaceholderImageUrl(Element image, String rawUrl, String bookCoverUrl) {
        String normalized = String.valueOf(rawUrl == null ? "" : rawUrl).trim();
        if (normalized.isBlank()) {
            return "";
        }
        String lower = normalized.toLowerCase(Locale.ROOT);
        if (lower.startsWith("http://")
            || lower.startsWith("https://")
            || lower.startsWith("data:")
            || normalized.startsWith("/")
            || normalized.startsWith("uploads/")) {
            return "";
        }
        if (!lower.matches(".*\\.(png|jpg|jpeg|webp|gif|bmp|svg)$")) {
            return "";
        }
        if (!isCoverPlaceholderImage(image, normalized)) {
            return "";
        }
        String cover = String.valueOf(bookCoverUrl == null ? "" : bookCoverUrl).trim();
        return cover.isBlank() ? "" : cover;
    }

    /**
     * 统一识别章节图片节点当前应回写的地址属性。
     * 普通图片优先使用 src，SVG 图片继续沿用 href 或 xlink:href，避免错误改写节点结构。
     */
    private String resolvePrimaryImageAttribute(Element image) {
        if (image == null) {
            return "";
        }
        if ("img".equalsIgnoreCase(image.tagName())) {
            return "src";
        }
        if (image.hasAttr("href")) {
            return "href";
        }
        if (image.hasAttr("xlink:href")) {
            return "xlink:href";
        }
        return "";
    }

    /**
     * 统一把存储资源地址收敛成前端可访问的稳定路径。
     * 保留 data URL、外部地址和 OSS 直链，同时补齐 uploads 相对路径前缀。
     */
    private String resolveStoredAssetUrl(String assetUrl) {
        if (assetUrl == null || assetUrl.isBlank()) {
            return "";
        }
        String trimmed = assetUrl.trim();
        if (trimmed.startsWith("/uploads/") || trimmed.toLowerCase(Locale.ROOT).startsWith("data:")) {
            return trimmed;
        }
        if (trimmed.startsWith("uploads/")) {
            return "/" + trimmed;
        }
        if (isManagedStorageUrl(trimmed) && trimmed.startsWith("http")) {
            int uploadsIndex = trimmed.indexOf("/uploads/");
            if (uploadsIndex >= 0) {
                return trimmed.substring(uploadsIndex);
            }
        }
        return trimmed;
    }

    /**
     * 统一识别书籍模块受管的对象存储地址。
     * 兼容书籍主链路走 OSS、历史内容或兜底资源仍来自代理的混合展示场景。
     */
    private boolean isManagedStorageUrl(String assetUrl) {
        if (assetUrl == null || assetUrl.isBlank()) {
            return false;
        }
        return isStorageUrlSafely("book", assetUrl)
            || isStorageUrlSafely("interview", assetUrl)
            || isStorageUrlSafely(contentStorageService, assetUrl);
    }

    /**
     * 安全判断指定模块的存储实现是否识别当前资源地址。
     * 存储未启用时直接返回 false，避免本地阅读页因缺少对象存储配置而中断。
     */
    private boolean isStorageUrlSafely(String moduleType, String assetUrl) {
        try {
            return isStorageUrlSafely(storageRoutingService.resolveForModule(moduleType), assetUrl);
        } catch (BusinessException exception) {
            return false;
        }
    }

    /**
     * 统一收口底层存储实现的空值判断。
     * 仅在存储服务存在时执行地址识别，保证章节图片规整逻辑始终可继续向下走。
     */
    private boolean isStorageUrlSafely(ContentStorageService storageService, String assetUrl) {
        return storageService != null && storageService.isStorageUrl(assetUrl);
    }

    /**
     * 识别章节中是否仍残留 EPUB 封面占位图。
     * 结合文件名提示词与 svg 包装结构判断，只对明显封面图执行封面回填。
     */
    private boolean isCoverPlaceholderImage(Element image, String rawUrl) {
        String normalized = String.valueOf(rawUrl == null ? "" : rawUrl).trim().replace("\\", "/");
        if (normalized.isBlank()) {
            return false;
        }
        String fileName = normalized.contains("/") ? normalized.substring(normalized.lastIndexOf('/') + 1) : normalized;
        String hint = (
            normalized + " "
                + fileName + " "
                + String.valueOf(image == null ? "" : image.attr("alt")) + " "
                + String.valueOf(image == null ? "" : image.attr("title"))
        ).toLowerCase(Locale.ROOT);
        if (hint.matches(".*(cover|front|title[-_ ]?page|calibre[_-]?cover|fm|titlepage).*")) {
            return true;
        }
        return image != null
            && "image".equalsIgnoreCase(image.tagName())
            && image.parents().stream().anyMatch(parent -> "svg".equalsIgnoreCase(parent.tagName()))
            && hint.matches(".*(cover|front|title|calibre).*");
    }
}
