package com.interview.auth.admin.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.interview.auth.admin.dto.request.AdminBookCategoryUpdateRequest;
import com.interview.auth.admin.dto.request.AdminBookImportBatchRequest;
import com.interview.auth.admin.dto.request.AdminBookImportChapterBatchUpdateRequest;
import com.interview.auth.admin.dto.request.AdminBookImportChapterBatchUpdateItem;
import com.interview.auth.admin.dto.request.AdminBookImportChapterUpdateRequest;
import com.interview.auth.admin.dto.request.AdminBookImportExternalRequest;
import com.interview.auth.admin.dto.request.AdminBookImportMetadataUpdateRequest;
import com.interview.auth.admin.dto.request.AdminContentImportPreviewRequest;
import com.interview.auth.admin.dto.response.AdminBookImportBatchResponse;
import com.interview.auth.admin.dto.response.AdminBookImportChapterResponse;
import com.interview.auth.admin.dto.response.AdminBookImportJobResponse;
import com.interview.auth.admin.dto.response.AdminContentImportPreviewResponse;
import com.interview.auth.admin.entity.BookImportChapterStage;
import com.interview.auth.admin.entity.BookImportJob;
import com.interview.auth.admin.entity.BookSourceFile;
import com.interview.auth.admin.mapper.AdminBookMapper;
import com.interview.auth.admin.service.AdminBookService;
import com.interview.auth.admin.service.AdminContentImportService;
import com.interview.auth.common.BusinessException;
import com.interview.auth.common.MagicBytesValidator;
import com.interview.auth.config.CacheConfig;
import com.interview.auth.domain.dto.response.BookResponse;
import com.interview.auth.domain.entity.Book;
import com.interview.auth.domain.entity.BookChapter;
import com.interview.auth.infrastructure.storage.ContentStorageService;
import com.interview.auth.infrastructure.storage.StorageRoutingService;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.siegmann.epublib.domain.Author;
import nl.siegmann.epublib.domain.Metadata;
import nl.siegmann.epublib.domain.Relator;
import nl.siegmann.epublib.domain.Resource;
import nl.siegmann.epublib.domain.Spine;
import nl.siegmann.epublib.domain.SpineReference;
import nl.siegmann.epublib.epub.EpubReader;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 集中承接后台书籍导入、章节审核和发布流程。
 * 复用现有富文本清洗与资源迁移能力，支持 ZIP/EPUB/PDF/TXT/MD/DOCX/HTML 多格式导入。
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AdminBookServiceImpl implements AdminBookService {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final String CONTENT_TYPE_BOOK = "book";
    private static final String STATUS_AWAIT_REVIEW = "await_review";
    private static final String STATUS_APPROVED = "approved";
    private static final String STATUS_REJECTED = "rejected";
    private static final String STATUS_PUBLISHED = "published";
    private static final String STATUS_DELETED = "deleted";
    private static final long MAX_FILE_SIZE = 200 * 1024 * 1024; // 200MB

    private static final Map<String, String> FORMAT_LABELS = Map.of(
        "epub", "EPUB",
        "pdf", "PDF",
        "txt", "TXT",
        "md", "Markdown",
        "docx", "Word",
        "html", "HTML",
        "htm", "HTML",
        "zip", "ZIP"
    );

    private static final List<String> BOOK_CATEGORY_OPTIONS = List.of(
        "精品书籍",
        "历史",
        "文学",
        "悬疑",
        "人物传记",
        "名家代表"
    );

    private final AdminBookMapper adminBookMapper;
    private final AdminContentImportService adminContentImportService;
    private final ContentStorageService contentStorageService;
    private final StorageRoutingService storageRoutingService;
    private final CacheManager cacheManager;

    @Override
    @Transactional
    public AdminBookImportJobResponse createImportJob(String fileName, InputStream inputStream, long size, String contentType) throws Exception {
        ContentStorageService bookStorageService = storageRoutingService.resolveForModule("book-import");
        String normalizedName = defaultString(fileName, "book-import.zip");
        if (!normalizedName.toLowerCase(Locale.ROOT).endsWith(".zip")) {
            throw new BusinessException(400, "ZIP 导入仅支持 .zip 压缩包");
        }

        byte[] zipBytes = inputStream.readAllBytes();
        if (zipBytes.length == 0) {
            throw new BusinessException(400, "上传文件不能为空");
        }

        String fileHash = sha256(zipBytes);
        String fileKey = "book-source-" + shortId();
        String storageUrl = bookStorageService.upload(
            "book/source", normalizedName,
            new ByteArrayInputStream(zipBytes), zipBytes.length,
            defaultString(contentType, "application/zip")
        );

        BookSourceFile sourceFile = buildSourceFile(fileKey, normalizedName, storageUrl, size > 0 ? size : zipBytes.length, fileHash);
        adminBookMapper.saveSourceFile(sourceFile);

        BookImportJob job = createInitialJob(normalizedName, fileKey, "zip", "zip", zipBytes.length);
        adminBookMapper.saveImportJob(job);

        ParsedBookPackage parsedBook = parseBookPackage(zipBytes, job.getJobKey(), normalizedName);
        updateJobWithParsedData(job, parsedBook);
        adminBookMapper.updateImportJob(job);

        adminBookMapper.deleteImportStageByJobKey(job.getJobKey());
        if (!parsedBook.stages().isEmpty()) {
            adminBookMapper.batchInsertImportStages(parsedBook.stages());
        }

        job.setProgress(100);
        adminBookMapper.updateImportJob(job);
        return toImportJobResponse(job);
    }

    @Override
    @Transactional
    public AdminBookImportJobResponse createImportJobFromFile(String fileName, InputStream inputStream, long size, String contentType) throws Exception {
        ContentStorageService bookStorageService = storageRoutingService.resolveForModule("book-import");
        String normalizedName = defaultString(fileName, "import-file");
        String extension = resolveExtension(normalizedName);
        if (extension.isEmpty() || !isValidSingleFileFormat(extension)) {
            throw new BusinessException(400, "不支持的文件格式，仅支持：epub、pdf、txt、md、docx、html");
        }

        byte[] fileBytes = inputStream.readAllBytes();
        if (fileBytes.length == 0) {
            throw new BusinessException(400, "上传文件不能为空");
        }
        if (fileBytes.length > MAX_FILE_SIZE) {
            throw new BusinessException(400, "文件大小超过 200MB 限制");
        }

        String fileHash = sha256(fileBytes);
        String fileKey = "book-source-" + shortId();
        String storageUrl = bookStorageService.upload(
            "book/source", normalizedName,
            new ByteArrayInputStream(fileBytes), fileBytes.length,
            defaultString(contentType, "application/octet-stream")
        );

        BookSourceFile sourceFile = buildSourceFile(fileKey, normalizedName, storageUrl, (long) fileBytes.length, fileHash);
        adminBookMapper.saveSourceFile(sourceFile);

        BookImportJob job = createInitialJob(normalizedName, fileKey, extension, extension, fileBytes.length);
        job.setStatus("parsing");
        job.setMessage("正在解析 " + getFormatLabel(extension) + " 文件");
        job.setProgress(10);
        adminBookMapper.saveImportJob(job);

        ParsedBookPackage parsedBook = parseSingleFilePackage(extension, normalizedName, fileBytes, job.getJobKey());
        List<BookImportChapterStage> stages = parsedBook.stages();

        job.setBookKey(parsedBook.bookKey());
        job.setTitle(parsedBook.title());
        job.setAuthor(parsedBook.author());
        job.setTranslator(parsedBook.translator());
        job.setPublisher(parsedBook.publisher());
        job.setSummary(parsedBook.summary());
        job.setCategory(parsedBook.category());
        job.setCoverUrl(parsedBook.coverUrl());
        job.setTotalChapters(stages.size());
        job.setSuccessChapters(stages.size());
        job.setFailChapters(0);
        job.setProgress(80);
        job.setStatus(STATUS_AWAIT_REVIEW);
        job.setMessage("解析完成，共 " + stages.size() + " 章，等待审核");
        adminBookMapper.updateImportJob(job);

        adminBookMapper.deleteImportStageByJobKey(job.getJobKey());
        if (!stages.isEmpty()) {
            adminBookMapper.batchInsertImportStages(stages);
        }

        job.setProgress(100);
        adminBookMapper.updateImportJob(job);
        return toImportJobResponse(job);
    }

    @Override
    @Transactional
    public AdminBookImportJobResponse createImportJobFromExternal(AdminBookImportExternalRequest request) throws Exception {
        String sourceUrl = request.getSourceUrl().trim();
        String importType = defaultString(request.getImportType(), "api");

        BookImportJob job = new BookImportJob();
        job.setJobKey("book-job-" + shortId());
        job.setBookKey("");
        job.setTitle(defaultString(request.getTitle(), "外部资源导入"));
        job.setAuthor(defaultString(request.getAuthor(), ""));
        job.setTranslator("");
        job.setPublisher(defaultString(request.getPublisher(), ""));
        job.setSummary(defaultString(request.getSummary(), ""));
        job.setCategory(normalizeBookCategory(request.getCategory(), null, null, List.of()));
        job.setCoverUrl("");
        job.setSourceFileKey("");
        job.setImportType(importType);
        job.setSourceUrl(sourceUrl);
        job.setOriginalFormat("external");
        job.setFileSize(0L);
        job.setStatus("parsing");
        job.setProgress(5);
        job.setMessage("正在拉取外部资源");
        adminBookMapper.saveImportJob(job);

        try {
            byte[] content = fetchExternalContent(sourceUrl);
            String detectedFormat = detectFormatFromUrl(sourceUrl);
            ParsedBookPackage parsedBook = parseSingleFilePackage(detectedFormat, extractTitleFromUrl(sourceUrl) + "." + detectedFormat, content, job.getJobKey());
            List<BookImportChapterStage> stages = parsedBook.stages();

            job.setBookKey(parsedBook.bookKey());
            job.setTitle(defaultString(request.getTitle(), parsedBook.title()));
            job.setAuthor(defaultString(request.getAuthor(), parsedBook.author()));
            job.setTranslator(parsedBook.translator());
            job.setPublisher(defaultString(request.getPublisher(), ""));
            job.setSummary(defaultString(request.getSummary(), parsedBook.summary()));
            job.setCategory(normalizeBookCategory(request.getCategory(), parsedBook.title(), parsedBook.summary(), parsedBook.stages()));
            job.setTotalChapters(stages.size());
            job.setSuccessChapters(stages.size());
            job.setFailChapters(0);
            job.setProgress(80);
            job.setStatus(STATUS_AWAIT_REVIEW);
            job.setMessage("外部资源解析完成，共 " + stages.size() + " 章");
            adminBookMapper.updateImportJob(job);

            adminBookMapper.deleteImportStageByJobKey(job.getJobKey());
            if (!stages.isEmpty()) {
                adminBookMapper.batchInsertImportStages(stages);
            }

            job.setProgress(100);
            adminBookMapper.updateImportJob(job);
        } catch (Exception e) {
            job.setStatus("failed");
            job.setMessage("外部资源导入失败: " + e.getMessage());
            adminBookMapper.updateImportJob(job);
        }

        return toImportJobResponse(job);
    }

    @Override
    public AdminBookImportJobResponse getImportJob(String jobKey) {
        BookImportJob job = requireImportJob(jobKey);
        return toImportJobResponse(job);
    }

    @Override
    public List<AdminBookImportJobResponse> listRecentImportJobs(int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 100));
        return adminBookMapper.listImportJobs(safeLimit).stream().map(this::toImportJobResponse).toList();
    }

    @Override
    public List<AdminBookImportChapterResponse> listImportJobChapters(String jobKey) {
        BookImportJob job = requireImportJob(jobKey);
        String coverUrlFallback = normalizeCoverUrlForDisplay(job.getCoverUrl());
        return adminBookMapper.findImportStagesByJobKey(jobKey).stream()
            .map(stage -> toImportChapterResponse(stage, coverUrlFallback))
            .toList();
    }

    /**
     * 更新导入任务的书名、作者、译者与封面字段。
     * 校验必填字段后写回任务表，发布时沿用最新审核结果。
     */
    @Override
    @Transactional
    public AdminBookImportJobResponse updateImportJobMetadata(String jobKey, AdminBookImportMetadataUpdateRequest request) {
        BookImportJob job = requireImportJob(jobKey);
        job.setTitle(requireText(request.getTitle(), "书籍标题不能为空"));
        job.setAuthor(requireText(request.getAuthor(), "作者不能为空"));
        job.setTranslator(defaultString(request.getTranslator(), "无"));
        job.setCategory(normalizeBookCategory(request.getCategory(), request.getTitle(), job.getSummary(), adminBookMapper.findImportStagesByJobKey(jobKey)));
        job.setCoverUrl(defaultString(request.getCoverUrl(), ""));
        adminBookMapper.updateImportJob(job);
        syncPublishedBookIfNeeded(job);
        return toImportJobResponse(job);
    }

    @Override
    @Transactional
    public AdminBookImportChapterResponse updateImportJobChapter(String jobKey, String tempChapterKey, AdminBookImportChapterUpdateRequest request) {
        BookImportJob job = requireImportJob(jobKey);
        BookImportChapterStage stage = requireImportStage(jobKey, tempChapterKey);
        applyChapterUpdate(stage, request);
        adminBookMapper.updateImportStage(stage);
        syncPublishedBookIfNeeded(job);
        return toImportChapterResponse(stage, normalizeCoverUrlForDisplay(job.getCoverUrl()));
    }

    /**
     * 批量更新导入任务下的章节暂存内容。
     * 逐个定位章节并复用单章字段更新规则，最后返回本次更新后的章节结果。
     */
    @Override
    @Transactional
    public List<AdminBookImportChapterResponse> updateImportJobChapters(String jobKey, AdminBookImportChapterBatchUpdateRequest request) {
        BookImportJob job = requireImportJob(jobKey);
        String coverUrlFallback = normalizeCoverUrlForDisplay(job.getCoverUrl());
        List<AdminBookImportChapterResponse> responses = new ArrayList<>();
        for (AdminBookImportChapterBatchUpdateItem item : request.getChapters()) {
            BookImportChapterStage stage = requireImportStage(jobKey, item.getTempChapterKey());
            applyChapterUpdate(stage, item);
            adminBookMapper.updateImportStage(stage);
            responses.add(toImportChapterResponse(stage, coverUrlFallback));
        }
        syncPublishedBookIfNeeded(job);
        return responses;
    }

    /**
     * 应用章节审核修改字段。
     * 统一重算纯文本和字数并标记已编辑，保证单章和批量保存结果一致。
     */
    private void applyChapterUpdate(BookImportChapterStage stage, AdminBookImportChapterUpdateRequest request) {
        stage.setTitle(request.getTitle().trim());
        stage.setSubtitle(defaultString(request.getSubtitle(), ""));
        stage.setContentHtml(request.getContentHtml());
        stage.setPlainText(stripHtml(request.getContentHtml()));
        stage.setWordCount(countWords(stage.getPlainText()));
        stage.setSortOrder(request.getSortOrder() == null ? stage.getSortOrder() : request.getSortOrder());
        stage.setReviewStatus("edited");
    }

    /**
     * 将审核通过的导入任务发布为正式书籍。
     * 读取最新任务元数据和章节暂存内容，写入书籍表与章节表后标记任务已发布。
     */
    @Override
    @Transactional
    public BookResponse publishImportJob(String jobKey) {
        BookImportJob job = requireImportJob(jobKey);
        assertPublishableJob(job);
        List<BookImportChapterStage> stages = adminBookMapper.findImportStagesByJobKey(jobKey).stream()
            .sorted(Comparator.comparing(BookImportChapterStage::getSortOrder).thenComparing(BookImportChapterStage::getChapterNo))
            .toList();

        if (stages.isEmpty()) {
            throw new BusinessException(400, "当前导入任务没有可发布章节");
        }

        Book book = buildBookFromImportJob(job, stages);
        book.setRating(BigDecimal.valueOf(9.0));
        book.setSortOrder(0);
        book.setPublishedAt(LocalDateTime.now());
        adminBookMapper.saveBook(book);

        replacePublishedBookChapters(book.getBookKey(), stages, normalizeCoverUrlForDisplay(book.getCoverUrl()));

        job.setBookKey(book.getBookKey());
        job.setStatus(STATUS_PUBLISHED);
        job.setProgress(100);
        job.setMessage("书籍已发布到用户端");
        adminBookMapper.updateImportJob(job);
        evictStarReadCache();
        return toBookResponse(book);
    }

    /**
     * 同步已发布书籍的正式数据。
     * 仅在任务已发布且存在书籍键时执行，将审核页改动即时反映到用户端。
     */
    private void syncPublishedBookIfNeeded(BookImportJob job) {
        if (!STATUS_PUBLISHED.equals(job.getStatus()) || defaultString(job.getBookKey(), "").isBlank()) {
            return;
        }
        List<BookImportChapterStage> stages = adminBookMapper.findImportStagesByJobKey(job.getJobKey()).stream()
            .sorted(Comparator.comparing(BookImportChapterStage::getSortOrder).thenComparing(BookImportChapterStage::getChapterNo))
            .toList();
        if (stages.isEmpty()) {
            return;
        }
        Book book = buildBookFromImportJob(job, stages);
        adminBookMapper.updatePublishedBookFromImportJob(book);
        replacePublishedBookChapters(book.getBookKey(), stages, normalizeCoverUrlForDisplay(book.getCoverUrl()));
        evictStarReadCache();
    }

    /**
     * 从导入任务构造正式书籍主记录。
     * 统一计算字数、章节数和展示字段，保证发布与发布后修改使用同一套映射。
     */
    private Book buildBookFromImportJob(BookImportJob job, List<BookImportChapterStage> stages) {
        int totalWords = stages.stream().map(BookImportChapterStage::getWordCount).mapToInt(this::defaultInt).sum();
        Book book = new Book();
        book.setBookKey(defaultString(job.getBookKey(), "book-" + shortId()));
        book.setTitle(requireText(job.getTitle(), "书籍标题不能为空"));
        book.setAuthor(defaultString(job.getAuthor(), "未知作者"));
        book.setTranslator(defaultString(job.getTranslator(), "无"));
        book.setPublisher(defaultString(job.getPublisher(), ""));
        book.setCategory(normalizeBookCategory(job.getCategory(), job.getTitle(), job.getSummary(), stages));
        book.setSummary(defaultString(job.getSummary(), ""));
        book.setCoverUrl(normalizeCoverUrlForDisplay(job.getCoverUrl()));
        book.setTagList(book.getCategory());
        book.setWordCount(totalWords);
        book.setChapterCount(stages.size());
        book.setReadCount(0);
        book.setStatus(1);
        return book;
    }

    /**
     * 替换正式书籍的章节快照。
     * 根据暂存章节顺序重新生成章节键和目录，保持阅读页内容与审核页一致。
     */
    private void replacePublishedBookChapters(String bookKey, List<BookImportChapterStage> stages) {
        replacePublishedBookChapters(bookKey, stages, "");
    }

    private void replacePublishedBookChapters(String bookKey, List<BookImportChapterStage> stages, String coverUrlFallback) {
        adminBookMapper.deleteBookChaptersByBookKey(bookKey);
        List<BookChapter> chapters = new ArrayList<>();
        int index = 1;
        for (BookImportChapterStage stage : stages) {
            BookChapter chapter = new BookChapter();
            chapter.setChapterKey("chapter-" + bookKey + "-" + String.format("%03d", index));
            chapter.setBookKey(bookKey);
            chapter.setChapterNo(index);
            chapter.setTitle(stage.getTitle());
            chapter.setSubtitle(defaultString(stage.getSubtitle(), ""));
            chapter.setContentHtml(normalizeImportedChapterHtmlForDisplay(stage.getContentHtml(), coverUrlFallback));
            chapter.setWordCount(defaultInt(stage.getWordCount()));
            chapter.setIsFree(index <= 3 ? 1 : 0);
            chapter.setStatus(1);
            chapter.setSortOrder(index);
            chapters.add(chapter);
            index++;
        }
        adminBookMapper.batchInsertBookChapters(chapters);
    }

    /**
     * 批量通过待审核书籍导入任务。
     * 逐个校验任务状态并写入通过标记，失败任务记录主键后继续处理剩余任务。
     */
    @Override
    @Transactional
    public AdminBookImportBatchResponse approveImportJobs(AdminBookImportBatchRequest request) {
        return updateImportJobReviewStatus(
            request,
            STATUS_APPROVED,
            "书籍已通过审核，等待发布",
            List.of(STATUS_AWAIT_REVIEW)
        );
    }

    /**
     * 批量拒绝待审核或已通过书籍导入任务。
     * 将任务状态改为拒绝并保留拒绝说明，后续发布动作会自动跳过这些任务。
     */
    @Override
    @Transactional
    public AdminBookImportBatchResponse rejectImportJobs(AdminBookImportBatchRequest request) {
        String reason = getBatchReason(request);
        return updateImportJobReviewStatus(
            request,
            STATUS_REJECTED,
            "书籍已拒绝：" + reason,
            List.of(STATUS_AWAIT_REVIEW, STATUS_APPROVED)
        );
    }

    /**
     * 批量发布审核通过的书籍导入任务。
     * 复用单本发布流程逐本写入书籍和章节，汇总成功任务与失败主键。
     */
    @Override
    public AdminBookImportBatchResponse publishImportJobs(AdminBookImportBatchRequest request) {
        AdminBookImportBatchResponse response = new AdminBookImportBatchResponse();
        for (String jobKey : normalizeJobKeys(request)) {
            try {
                publishImportJob(jobKey);
                response.getJobs().add(toImportJobResponse(requireImportJob(jobKey)));
                response.setSuccessCount(response.getSuccessCount() + 1);
            } catch (BusinessException exception) {
                response.getFailedKeys().add(jobKey);
            }
        }
        response.setFailedCount(response.getFailedKeys().size());
        return response;
    }

    /**
     * 读取批量请求中的拒绝原因。
     * 空原因使用默认文案，避免任务消息出现空白审核说明。
     */
    private String getBatchReason(AdminBookImportBatchRequest request) {
        return defaultString(request.getReason(), "书籍导入审核未通过");
    }

    @Override
    public List<BookResponse> listBooks() {
        return adminBookMapper.findAllBooks().stream().map(this::toBookResponse).toList();
    }

    /**
     * 更新已发布书籍的分类标签。
     * 标准化分类后写入书籍表与导入任务表，返回最新书籍展示数据。
     */
    @Override
    @Transactional
    public BookResponse updateBookCategory(String bookKey, AdminBookCategoryUpdateRequest request) {
        Book book = requireBook(bookKey);
        String category = normalizeBookCategory(request.getCategory(), book.getTitle(), book.getSummary(), List.of());
        adminBookMapper.updateBookCategory(book.getBookKey(), category);
        adminBookMapper.updateImportJobCategoryByBookKey(book.getBookKey(), category);
        book.setCategory(category);
        book.setTagList(category);
        return toBookResponse(book);
    }

    /**
     * 将正式书籍移入已删除列表。
     * 下线前台书籍并把关联导入任务标记为已删除，保留章节与存储资源以便恢复。
     */
    @Override
    @Transactional
    public void softDeleteBook(String bookKey) {
        Book book = requireBook(bookKey);
        adminBookMapper.updateBookStatusByKey(book.getBookKey(), 0);
        for (BookImportJob job : adminBookMapper.findImportJobsByBookKey(book.getBookKey())) {
            if (!STATUS_DELETED.equals(job.getStatus())) {
                adminBookMapper.updateImportJobDeletedByKey(job.getJobKey(), buildDeleteMessage(job.getStatus()));
            }
        }
        evictStarReadCache();
    }

    /**
     * 彻底删除已发布书籍和关联导入数据。
     * 按章节、导入暂存、导入任务、书籍主表顺序清理，并同步回收对象存储中的封面与正文资源。
     */
    @Override
    @Transactional
    public void hardDeleteBook(String bookKey) {
        Book book = requireBook(bookKey);
        List<BookImportJob> importJobs = adminBookMapper.findImportJobsByBookKey(book.getBookKey());
        List<BookChapter> chapters = adminBookMapper.findBookChaptersByBookKey(book.getBookKey());
        Set<String> storageUrls = collectBookStorageUrls(book, importJobs, chapters);

        adminBookMapper.deleteBookChaptersByBookKey(book.getBookKey());
        adminBookMapper.deleteImportStageByBookKey(book.getBookKey());
        adminBookMapper.deleteImportJobByBookKey(book.getBookKey());
        adminBookMapper.deleteBookByKey(book.getBookKey());

        deleteStorageResources(storageUrls, book.getBookKey());
        evictStarReadCache();
    }

    /**
     * 将指定导入任务移入已删除列表。
     * 保留任务和章节数据，记录删除前状态并下线关联的正式书籍。
     */
    @Override
    @Transactional
    public void deleteImportJob(String jobKey) {
        BookImportJob job = requireImportJob(jobKey);
        if (STATUS_DELETED.equals(job.getStatus())) {
            return;
        }
        if (!defaultString(job.getBookKey(), "").isBlank()) {
            adminBookMapper.updateBookStatusByKey(job.getBookKey(), 0);
        }
        adminBookMapper.updateImportJobDeletedByKey(jobKey, buildDeleteMessage(job.getStatus()));
    }

    /**
     * 彻底删除指定导入任务及其关联的正式书籍、章节与存储资源。
     * 先汇总封面、正文图片和源文件地址，再按正式书籍、暂存章节、导入任务顺序清理数据库记录。
     */
    @Override
    @Transactional
    public void hardDeleteImportJob(String jobKey) {
        BookImportJob job = requireImportJob(jobKey);
        Set<String> storageUrls = new LinkedHashSet<>();
        addStorageUrl(storageUrls, job.getCoverUrl());

        BookSourceFile sourceFile = adminBookMapper.findSourceFileByKey(job.getSourceFileKey());
        if (sourceFile != null) {
            addStorageUrl(storageUrls, sourceFile.getStorageUrl());
        }

        List<BookImportChapterStage> importStages = adminBookMapper.findImportStagesByJobKey(jobKey);
        for (BookImportChapterStage stage : importStages) {
            storageUrls.addAll(extractStorageUrlsFromHtml(stage.getContentHtml()));
        }

        String bookKey = defaultString(job.getBookKey(), "");
        if (!bookKey.isBlank()) {
            Book book = adminBookMapper.findBookByKey(bookKey);
            if (book != null) {
                storageUrls.addAll(collectBookStorageUrls(
                    book,
                    List.of(job),
                    adminBookMapper.findBookChaptersByBookKey(bookKey)
                ));
                adminBookMapper.deleteBookChaptersByBookKey(bookKey);
            }
        }

        adminBookMapper.deleteImportStageByJobKey(jobKey);
        adminBookMapper.deleteImportJobByKey(jobKey);
        if (!bookKey.isBlank()) {
            adminBookMapper.deleteBookByKey(bookKey);
        }
        if (sourceFile != null && !defaultString(sourceFile.getFileKey(), "").isBlank()) {
            adminBookMapper.deleteSourceFileByKey(sourceFile.getFileKey());
        }

        deleteStorageResources(storageUrls, bookKey.isBlank() ? jobKey : bookKey);
    }

    /**
     * 恢复已删除导入任务到删除前状态。
     * 读取删除标记还原任务状态，已发布书籍同步恢复为前台可见。
     */
    @Override
    @Transactional
    public AdminBookImportJobResponse restoreImportJob(String jobKey) {
        BookImportJob job = requireImportJob(jobKey);
        if (!STATUS_DELETED.equals(job.getStatus())) {
            return toImportJobResponse(job);
        }
        String restoredStatus = resolveRestoredStatus(job.getMessage());
        adminBookMapper.restoreImportJobStatusByKey(jobKey, "书籍已恢复到删除前状态");
        if (STATUS_PUBLISHED.equals(restoredStatus) && !defaultString(job.getBookKey(), "").isBlank()) {
            adminBookMapper.updateBookStatusByKey(job.getBookKey(), 1);
        }
        return toImportJobResponse(requireImportJob(jobKey));
    }
    /**
     * 批量删除导入任务。
     * 逐个删除导入记录及关联数据，单条异常不影响其他任务。
     */
    @Override
    public AdminBookImportBatchResponse batchDeleteImportJobs(AdminBookImportBatchRequest request) {
        AdminBookImportBatchResponse response = new AdminBookImportBatchResponse();
        for (String jobKey : normalizeJobKeys(request)) {
            try {
                deleteImportJob(jobKey);
                response.setSuccessCount(response.getSuccessCount() + 1);
            } catch (BusinessException exception) {
                response.getFailedKeys().add(jobKey);
            }
        }
        response.setFailedCount(response.getFailedKeys().size());
        return response;
    }

    /**
     * 按分类删除导入任务。
     * 清理指定分类下所有导入暂存章节和导入主记录，已发布的正式书籍不受影响。
     */
    @Override
    @Transactional
    public int deleteImportJobsByCategory(String category) {
        int count = 0;
        for (BookImportJob job : adminBookMapper.findImportJobsByCategory(category)) {
            if (!STATUS_DELETED.equals(job.getStatus())) {
                deleteImportJob(job.getJobKey());
                count++;
            }
        }
        return count;
    }

    /**
     * 重新扫描导入任务可用的封面来源并回填封面地址。
     * 仅在封面资源可实际回读时才判定修复成功，避免失效旧地址被误认为仍可继续使用。
     */
    @Override
    @Transactional
    public AdminBookImportJobResponse repairImportJobCover(String jobKey) throws Exception {
        BookImportJob job = requireImportJob(jobKey);
        String previousCoverUrl = normalizeCoverUrlForDisplay(job.getCoverUrl());
        boolean previousCoverUsable = isUsableCoverUrl(previousCoverUrl);

        String newCoverUrl = tryRepairCoverFromSourceFile(job);
        if (shouldContinueCoverRepair(newCoverUrl, previousCoverUrl, previousCoverUsable)) {
            newCoverUrl = tryRepairCoverFromChapterContent(jobKey, previousCoverUrl, previousCoverUsable);
        }
        if (shouldContinueCoverRepair(newCoverUrl, previousCoverUrl, previousCoverUsable)) {
            newCoverUrl = tryRepairCoverFromPublishedBook(job, previousCoverUrl, previousCoverUsable);
        }

        String normalizedCoverUrl = normalizeCoverUrlForDisplay(newCoverUrl);
        boolean normalizedCoverUsable = isUsableCoverUrl(normalizedCoverUrl);

        if (normalizedCoverUsable) {
            job.setCoverUrl(normalizedCoverUrl);
            if (normalizedCoverUrl.equals(previousCoverUrl)) {
                job.setMessage("封面已校验，当前封面可继续使用");
            } else {
                job.setMessage("封面已恢复");
            }
        } else if (previousCoverUsable) {
            job.setCoverUrl(previousCoverUrl);
            job.setMessage("封面已校验，当前封面可继续使用");
        } else if (normalizedCoverUrl.isBlank()) {
            job.setMessage("封面修复完成：源文件和章节内容中均未找到可识别封面图片");
        } else {
            job.setMessage("封面修复完成：找到了候选封面，但资源仍不可访问，请重新上传封面");
        }
        adminBookMapper.updateImportJob(job);
        syncPublishedBookIfNeeded(job);
        return toImportJobResponse(job);
    }

    /**
     * 批量修复导入任务封面。
     * 逐个调用单任务修复流程，单条异常不影响其他任务。
     */
    @Override
    public AdminBookImportBatchResponse batchRepairImportJobCovers(AdminBookImportBatchRequest request) {
        AdminBookImportBatchResponse response = new AdminBookImportBatchResponse();
        for (String jobKey : normalizeJobKeys(request)) {
            try {
                AdminBookImportJobResponse repaired = repairImportJobCover(jobKey);
                response.getJobs().add(repaired);
                response.setSuccessCount(response.getSuccessCount() + 1);
            } catch (Exception exception) {
                log.warn("批量修复封面失败(jobKey={}): {}", jobKey, exception.getMessage());
                response.getFailedKeys().add(jobKey);
            }
        }
        response.setFailedCount(response.getFailedKeys().size());
        return response;
    }

    /**
     * 尝试从源文件中解析封面。
     * 源文件在 OSS 中不存在时返回空字符串（不抛异常），以便回退到其他策略。
     */
    private String tryRepairCoverFromSourceFile(BookImportJob job) {
        String sourceFileKey = defaultString(job.getSourceFileKey(), "");
        if (sourceFileKey.isBlank()) {
            return "";
        }
        BookSourceFile sourceFile = adminBookMapper.findSourceFileByKey(sourceFileKey);
        if (sourceFile == null) {
            return "";
        }
        String storageUrl = defaultString(sourceFile.getStorageUrl(), "");
        if (storageUrl.isBlank()) {
            return "";
        }
        try {
            String originalName = defaultString(sourceFile.getOriginalName(), "import-file");
            String extension = resolveExtension(originalName);
            byte[] fileBytes;
            try (InputStream inputStream = contentStorageService.download(storageUrl)) {
                fileBytes = inputStream.readAllBytes();
            }
            if (fileBytes.length == 0) {
                return "";
            }
            return resolveCoverFromSourceFile(fileBytes, extension, originalName, job.getJobKey());
        } catch (Exception e) {
            log.warn("从源文件恢复封面失败(jobKey={}, url={}): {}", job.getJobKey(), storageUrl, e.getMessage());
            return "";
        }
    }

    /**
     * 从导入暂存章节正文里提取可直接展示的首张图片。
     * 遇到与当前失效封面相同的旧地址时继续向后扫描，优先选择真正还能访问的候选图。
     */
    private String tryRepairCoverFromChapterContent(String jobKey, String excludedCoverUrl, boolean excludedCoverUsable) {
        List<BookImportChapterStage> stages = adminBookMapper.findImportStagesByJobKey(jobKey);
        if (stages == null || stages.isEmpty()) {
            return "";
        }
        for (BookImportChapterStage stage : stages) {
            String html = defaultString(stage.getContentHtml(), "");
            if (html.isBlank()) {
                continue;
            }
            String imageUrl = extractFirstImageUrlFromHtml(html, excludedCoverUrl, excludedCoverUsable);
            if (!imageUrl.isBlank()) {
                return imageUrl;
            }
        }
        return "";
    }

    /**
     * 从已发布书籍里回补导入任务缺失的封面地址。
     * 若正式书籍仍挂着同一条失效旧封面，则继续回退到正式章节正文里寻找可用图片。
     */
    private String tryRepairCoverFromPublishedBook(BookImportJob job, String excludedCoverUrl, boolean excludedCoverUsable) {
        String bookKey = defaultString(job.getBookKey(), "");
        if (bookKey.isBlank()) {
            return "";
        }
        Book book = adminBookMapper.findBookByKey(bookKey);
        if (book != null) {
            String bookCoverUrl = normalizeCoverUrlForDisplay(book.getCoverUrl());
            if (!bookCoverUrl.isBlank()
                && (!bookCoverUrl.equals(excludedCoverUrl) || excludedCoverUsable)
                && isUsableCoverUrl(bookCoverUrl)) {
                return bookCoverUrl;
            }
        }
        List<BookChapter> chapters = adminBookMapper.findBookChaptersByBookKey(bookKey);
        if (chapters == null || chapters.isEmpty()) {
            return "";
        }
        for (BookChapter chapter : chapters) {
            String imageUrl = extractFirstImageUrlFromHtml(chapter.getContentHtml(), excludedCoverUrl, excludedCoverUsable);
            if (!imageUrl.isBlank()) {
                return imageUrl;
            }
        }
        return "";
    }

    /**
     * 从章节 HTML 中提取第一张能直接回显的图片地址。
     * 同时跳过已确认失效的旧封面地址，避免预览正文里的同一张坏图再次被选回封面。
     */
    private String extractFirstImageUrlFromHtml(String html, String excludedCoverUrl, boolean excludedCoverUsable) {
        String normalizedExcludedCoverUrl = normalizeCoverUrlForDisplay(excludedCoverUrl);
        Document document = Jsoup.parseBodyFragment(defaultString(html, ""));
        for (Element image : document.select("img[src], image[href], image[xlink\\:href]")) {
            String attributeName = image.hasAttr("src") ? "src" : image.hasAttr("href") ? "href" : "xlink:href";
            String src = defaultString(image.attr(attributeName), "").trim();
            if (src.isBlank() || src.toLowerCase(Locale.ROOT).startsWith("data:")) {
                continue;
            }
            String normalizedUrl = normalizeCoverUrlForDisplay(src);
            if (!normalizedUrl.isBlank()) {
                if (normalizedUrl.equals(normalizedExcludedCoverUrl) && !excludedCoverUsable) {
                    continue;
                }
                if (isUsableCoverUrl(normalizedUrl)) {
                    return normalizedUrl;
                }
                continue;
            }
            String normalizedStoredUrl = normalizeStoredAssetUrlForDisplay(src);
            String normalizedDisplayUrl = normalizeCoverUrlForDisplay(normalizedStoredUrl);
            if (normalizedDisplayUrl.isBlank()) {
                continue;
            }
            if (normalizedDisplayUrl.equals(normalizedExcludedCoverUrl) && !excludedCoverUsable) {
                continue;
            }
            if (isUsableCoverUrl(normalizedDisplayUrl)) {
                return normalizedDisplayUrl;
            }
        }
        return "";
    }

    /**
     * 判断当前候选封面是否还需要继续走下一层修复策略。
     * 只要候选为空、资源不可读，或仍指向那条已失效旧地址，就继续尝试后续兜底来源。
     */
    private boolean shouldContinueCoverRepair(String candidateCoverUrl, String previousCoverUrl, boolean previousCoverUsable) {
        String normalizedCandidateCoverUrl = normalizeCoverUrlForDisplay(candidateCoverUrl);
        if (normalizedCandidateCoverUrl.isBlank()) {
            return true;
        }
        if (normalizedCandidateCoverUrl.equals(previousCoverUrl) && !previousCoverUsable) {
            return true;
        }
        return !isUsableCoverUrl(normalizedCandidateCoverUrl);
    }

    /**
     * 校验封面地址对应的资源当前是否还能正常读取。
     * 对本站存储资源会回读文件流做一次轻量探测，避免数据库里残留坏链时仍被当成有效封面。
     */
    private boolean isUsableCoverUrl(String coverUrl) {
        String normalizedCoverUrl = normalizeCoverUrlForDisplay(coverUrl);
        if (normalizedCoverUrl.isBlank()) {
            return false;
        }
        String lowerCoverUrl = normalizedCoverUrl.toLowerCase(Locale.ROOT);
        if (lowerCoverUrl.startsWith("data:image/")) {
            return true;
        }
        if (isManagedStorageUrl(normalizedCoverUrl)) {
            return probeStorageResource(normalizedCoverUrl);
        }
        if (normalizedCoverUrl.startsWith("/")) {
            return probeStorageResource(normalizedCoverUrl);
        }
        if (lowerCoverUrl.startsWith("http://") || lowerCoverUrl.startsWith("https://")) {
            return probeHttpResource(normalizedCoverUrl);
        }
        return false;
    }

    private boolean probeStorageResource(String normalizedCoverUrl) {
        try (InputStream inputStream = contentStorageService.download(normalizedCoverUrl)) {
            return inputStream.read() >= 0;
        } catch (Exception exception) {
            log.warn("封面资源校验失败(url={}): {}", normalizedCoverUrl, exception.getMessage());
            return false;
        }
    }

    private boolean probeHttpResource(String url) {
        try {
            java.net.HttpURLConnection connection = (java.net.HttpURLConnection) new java.net.URL(url).openConnection();
            connection.setRequestMethod("HEAD");
            connection.setConnectTimeout(3000);
            connection.setReadTimeout(3000);
            connection.setInstanceFollowRedirects(true);
            int status = connection.getResponseCode();
            connection.disconnect();
            return status >= 200 && status < 400;
        } catch (Exception exception) {
            log.warn("封面资源 HTTP 探测失败(url={}): {}", url, exception.getMessage());
            return false;
        }
    }

    /**
     * 从源文件字节中解析封面图片。
     * 基于文件格式分支处理：ZIP 解包扫描、EPUB 读取内置封面、其他单文件尝试提取首图。
     */
    private String resolveCoverFromSourceFile(byte[] fileBytes, String extension, String fileName, String jobKey) throws Exception {
        return switch (extension) {
            case "zip" -> {
                Map<String, byte[]> entries = readZipEntries(fileBytes);
                String coverUrl = resolveAutoCoverUrl(entries);
                yield coverUrl;
            }
            case "epub" -> {
                try (InputStream is = new ByteArrayInputStream(fileBytes)) {
                    EpubReader epubReader = new EpubReader();
                    nl.siegmann.epublib.domain.Book epubBook = epubReader.readEpub(is);
                    yield resolveEpubCoverUrl(epubBook, fileName);
                }
            }
            case "pdf" -> "";
            case "docx" -> "";
            case "html", "htm" -> extractFirstImageAsCover(new String(fileBytes, StandardCharsets.UTF_8), fileName, jobKey);
            case "txt", "md", "markdown" -> "";
            default -> "";
        };
    }

    // ========== 单文件解析方法 ==========

    private List<BookImportChapterStage> parseSingleFile(String format, byte[] fileBytes, String jobKey) throws Exception {
        return switch (format) {
            case "epub" -> parseEpub(fileBytes, jobKey);
            case "pdf" -> parsePdf(fileBytes, jobKey);
            case "docx" -> parseDocx(fileBytes, jobKey);
            case "md", "markdown" -> parseMarkdown(fileBytes, jobKey);
            case "html", "htm" -> parseHtml(fileBytes, jobKey);
            default -> parseText(fileBytes, jobKey);
        };
    }

    /**
     * 解析单文件并同时补齐书籍元信息。
     * EPUB 优先读取内置元数据，其他格式从文件名和正文片段识别作者与译者。
     * 对于 HTML 类格式尝试从正文中提取第一张图片作为封面；其他纯文本格式暂时留空。
     */
    private ParsedBookPackage parseSingleFilePackage(String format, String fileName, byte[] fileBytes, String jobKey) throws Exception {
        if ("epub".equals(format)) {
            return parseEpubPackage(fileName, fileBytes, jobKey);
        }
        List<BookImportChapterStage> stages = parseSingleFile(format, fileBytes, jobKey);
        BookCreditInfo creditInfo = inferBookCreditInfo(fileName, stages, "", "");
        String coverUrl = "";
        if ("html".equals(format) || "htm".equals(format)) {
            coverUrl = extractFirstImageAsCover(new String(fileBytes, StandardCharsets.UTF_8), fileName, jobKey);
        }
        return new ParsedBookPackage(buildSlugKey(creditInfo.title()), creditInfo.title(), creditInfo.author(), creditInfo.translator(), "", "书籍", "", coverUrl, stages);
    }

    /**
     * 从 HTML 正文中提取第一张插图并将其导入封面存储。
     * 仅在 HTML 单文件上传时触发，无法定位图片或外链失败时返回空字符串。
     */
    private String extractFirstImageAsCover(String html, String fileName, String jobKey) throws Exception {
        Document document = Jsoup.parse(defaultString(html, ""));
        Element firstImage = document.selectFirst("img[src]");
        if (firstImage == null) {
            return "";
        }
        String src = defaultString(firstImage.attr("src"), "").trim();
        if (src.isBlank()) {
            return "";
        }
        String lowerSrc = src.toLowerCase(Locale.ROOT);
        if (lowerSrc.startsWith("http://") || lowerSrc.startsWith("https://")
            || lowerSrc.startsWith("data:") || isManagedStorageUrl(src)) {
            return src;
        }
        // 非外链图片无法从单文件中提取二进制内容，留空
        return "";
    }

    /**
     * 解析 EPUB 文件并读取标题、作者、译者等元信息。
     * 先读取 EPUB metadata，再结合文件名和章节正文兜底修正缺失字段。
     */
    private ParsedBookPackage parseEpubPackage(String fileName, byte[] fileBytes, String jobKey) throws Exception {
        try (InputStream is = new ByteArrayInputStream(fileBytes)) {
            EpubReader epubReader = new EpubReader();
            nl.siegmann.epublib.domain.Book epubBook = epubReader.readEpub(is);
            List<BookImportChapterStage> stages = parseEpubBook(epubBook, jobKey);
            Metadata metadata = epubBook.getMetadata();
            String title = defaultString(metadata == null ? "" : metadata.getFirstTitle(), extractTitleFromFileName(fileName));
            String author = extractAuthors(metadata);
            String translator = extractTranslators(metadata);
            BookCreditInfo authorInfo = parseCreditInfoFromText(author);
            author = authorInfo.author().isBlank() ? author : authorInfo.author();
            translator = defaultString(translator, authorInfo.translator());
            String publisher = firstString(metadata == null ? List.of() : metadata.getPublishers());
            String summary = firstString(metadata == null ? List.of() : metadata.getDescriptions());
            String coverUrl = resolveEpubCoverUrl(epubBook, fileName);
            BookCreditInfo creditInfo = inferBookCreditInfo(title, stages, author, translator);
            return new ParsedBookPackage(buildSlugKey(creditInfo.title()), creditInfo.title(), creditInfo.author(), creditInfo.translator(), publisher, "书籍", summary, coverUrl, stages);
        }
    }

    private List<BookImportChapterStage> parseEpub(byte[] fileBytes, String jobKey) throws Exception {
        try (InputStream is = new ByteArrayInputStream(fileBytes)) {
            EpubReader epubReader = new EpubReader();
            nl.siegmann.epublib.domain.Book epubBook = epubReader.readEpub(is);
            return parseEpubBook(epubBook, jobKey);
        }
    }

    /**
     * 从已读取的 EPUB 对象中提取章节列表。
     * 遍历 spine 顺序生成暂存章节，跳过无法读取或空白的资源。
     */
    private List<BookImportChapterStage> parseEpubBook(nl.siegmann.epublib.domain.Book epubBook, String jobKey) throws Exception {
        List<BookImportChapterStage> stages = new ArrayList<>();
        Spine spine = epubBook.getSpine();
        List<SpineReference> spineReferences = spine.getSpineReferences();
        int sortOrder = 1;
        int chapterNo = 1;

        for (SpineReference ref : spineReferences) {
            try {
                nl.siegmann.epublib.domain.Resource resource = ref.getResource();
                String htmlContent = new String(resource.getData(), StandardCharsets.UTF_8);
                String normalizedHtml = normalizeEpubChapterHtml(epubBook, resource, htmlContent, jobKey);
                String cleanedHtml = cleanImportedHtml(normalizedHtml);
                if (cleanedHtml.isBlank()) {
                    continue;
                }

                BookImportChapterStage stage = new BookImportChapterStage();
                stage.setJobKey(jobKey);
                stage.setTempChapterKey("temp-chapter-" + shortId());
                stage.setSourcePath(resource.getHref() != null ? resource.getHref() : "chapter-" + chapterNo);
                stage.setChapterNo(chapterNo);
                stage.setTitle(extractTitleFromHtml(cleanedHtml, "第" + chapterNo + "章"));
                stage.setSubtitle("");
                stage.setContentHtml(cleanedHtml);
                stage.setPlainText(stripHtml(cleanedHtml));
                stage.setWordCount(countWords(stage.getPlainText()));
                stage.setWarningJson(OBJECT_MAPPER.writeValueAsString(List.of()));
                stage.setReviewStatus("parsed");
                stage.setSortOrder(sortOrder);
                stages.add(stage);
                sortOrder++;
                chapterNo++;
            } catch (Exception ignored) {
                continue;
            }
        }
        if (stages.isEmpty()) {
            throw new BusinessException(400, "EPUB 解析失败：未找到有效章节内容");
        }
        return stages;
    }

    private List<BookImportChapterStage> parsePdf(byte[] fileBytes, String jobKey) throws Exception {
        List<BookImportChapterStage> stages = new ArrayList<>();
        try (PDDocument document = Loader.loadPDF(fileBytes)) {
            PDFTextStripper stripper = new PDFTextStripper();
            int totalPages = document.getNumberOfPages();
            int chapterNo = 1;
            int sortOrder = 1;

            // 按每 10 页一个章节拆分，或按 PDF 章节标题模式拆分
            int pagesPerChapter = Math.max(1, totalPages / Math.max(1, totalPages / 10));
            int startPage = 1;

            while (startPage <= totalPages) {
                int endPage = Math.min(startPage + pagesPerChapter - 1, totalPages);
                stripper.setStartPage(startPage);
                stripper.setEndPage(endPage);
                String text = stripper.getText(document).trim();

                if (!text.isBlank()) {
                    String html = textToParagraphHtml(text);
                    BookImportChapterStage stage = new BookImportChapterStage();
                    stage.setJobKey(jobKey);
                    stage.setTempChapterKey("temp-chapter-" + shortId());
                    stage.setSourcePath("pages-" + startPage + "-" + endPage);
                    stage.setChapterNo(chapterNo);
                    stage.setTitle("第" + chapterNo + "章 (页" + startPage + "-" + endPage + ")");
                    stage.setSubtitle("");
                    stage.setContentHtml(html);
                    stage.setPlainText(text);
                    stage.setWordCount(countWords(text));
                    stage.setWarningJson(OBJECT_MAPPER.writeValueAsString(List.of("PDF 内容为纯文本提取，原排版信息可能丢失")));
                    stage.setReviewStatus("parsed");
                    stage.setSortOrder(sortOrder);
                    stages.add(stage);
                    sortOrder++;
                    chapterNo++;
                }
                startPage = endPage + 1;
            }
        }
        if (stages.isEmpty()) {
            throw new BusinessException(400, "PDF 解析失败：未提取到有效文本内容");
        }
        return stages;
    }

    private List<BookImportChapterStage> parseDocx(byte[] fileBytes, String jobKey) throws Exception {
        StringBuilder builder = new StringBuilder();
        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(fileBytes))) {
            for (XWPFParagraph paragraph : document.getParagraphs()) {
                String text = defaultString(paragraph.getText(), "").trim();
                if (text.isEmpty()) {
                    continue;
                }
                String style = defaultString(paragraph.getStyle(), "").toLowerCase(Locale.ROOT);
                if (style.contains("heading1") || style.contains("title1")) {
                    builder.append("<h1>").append(escapeHtml(text)).append("</h1>");
                } else if (style.contains("heading2") || style.contains("title2")) {
                    builder.append("<h2>").append(escapeHtml(text)).append("</h2>");
                } else if (style.contains("heading3") || style.contains("title3")) {
                    builder.append("<h3>").append(escapeHtml(text)).append("</h3>");
                } else {
                    builder.append("<p>").append(escapeHtml(text)).append("</p>");
                }
            }
        }

        String html = builder.toString();
        if (html.isBlank()) {
            throw new BusinessException(400, "DOCX 解析失败：未提取到有效文本内容");
        }

        BookImportChapterStage stage = new BookImportChapterStage();
        stage.setJobKey(jobKey);
        stage.setTempChapterKey("temp-chapter-" + shortId());
        stage.setSourcePath("docx-content");
        stage.setChapterNo(1);
        stage.setTitle(extractTitleFromHtml(html, "导入文档"));
        stage.setSubtitle("");
        stage.setContentHtml(cleanImportedHtml(html));
        stage.setPlainText(stripHtml(html));
        stage.setWordCount(countWords(stripHtml(html)));
        stage.setWarningJson(OBJECT_MAPPER.writeValueAsString(List.of()));
        stage.setReviewStatus("parsed");
        stage.setSortOrder(1);
        return List.of(stage);
    }

    private List<BookImportChapterStage> parseMarkdown(byte[] fileBytes, String jobKey) throws Exception {
        String markdown = new String(fileBytes, StandardCharsets.UTF_8);
        String html = markdownToHtml(markdown);
        if (html.isBlank()) {
            throw new BusinessException(400, "Markdown 文件内容为空");
        }

        // 按 ## 标题拆分章节
        String[] sections = markdown.split("(?=^## )", -1);
        List<BookImportChapterStage> stages = new ArrayList<>();

        if (sections.length <= 1) {
            BookImportChapterStage stage = new BookImportChapterStage();
            stage.setJobKey(jobKey);
            stage.setTempChapterKey("temp-chapter-" + shortId());
            stage.setSourcePath("markdown-content");
            stage.setChapterNo(1);
            stage.setTitle(extractTitleFromHtml(html, "导入文档"));
            stage.setSubtitle("");
            stage.setContentHtml(cleanImportedHtml(html));
            stage.setPlainText(stripHtml(html));
            stage.setWordCount(countWords(stripHtml(html)));
            stage.setWarningJson(OBJECT_MAPPER.writeValueAsString(List.of()));
            stage.setReviewStatus("parsed");
            stage.setSortOrder(1);
            stages.add(stage);
        } else {
            int sortOrder = 1;
            for (String section : sections) {
                String sectionHtml = markdownToHtml(section.trim());
                if (sectionHtml.isBlank()) continue;

                BookImportChapterStage stage = new BookImportChapterStage();
                stage.setJobKey(jobKey);
                stage.setTempChapterKey("temp-chapter-" + shortId());
                stage.setSourcePath("markdown-section-" + sortOrder);
                stage.setChapterNo(sortOrder);
                stage.setTitle(extractTitleFromHtml(sectionHtml, "第" + sortOrder + "章"));
                stage.setSubtitle("");
                stage.setContentHtml(cleanImportedHtml(sectionHtml));
                stage.setPlainText(stripHtml(sectionHtml));
                stage.setWordCount(countWords(stripHtml(sectionHtml)));
                stage.setWarningJson(OBJECT_MAPPER.writeValueAsString(List.of()));
                stage.setReviewStatus("parsed");
                stage.setSortOrder(sortOrder);
                stages.add(stage);
                sortOrder++;
            }
        }

        if (stages.isEmpty()) {
            throw new BusinessException(400, "Markdown 解析结果为空");
        }
        return stages;
    }

    private List<BookImportChapterStage> parseHtml(byte[] fileBytes, String jobKey) throws Exception {
        String html = new String(fileBytes, StandardCharsets.UTF_8);
        String cleanedHtml = cleanImportedHtml(html);
        if (cleanedHtml.isBlank()) {
            throw new BusinessException(400, "HTML 文件内容为空");
        }

        BookImportChapterStage stage = new BookImportChapterStage();
        stage.setJobKey(jobKey);
        stage.setTempChapterKey("temp-chapter-" + shortId());
        stage.setSourcePath("html-content");
        stage.setChapterNo(1);
        stage.setTitle(extractTitleFromHtml(cleanedHtml, "导入页面"));
        stage.setSubtitle("");
        stage.setContentHtml(cleanedHtml);
        stage.setPlainText(stripHtml(cleanedHtml));
        stage.setWordCount(countWords(stripHtml(cleanedHtml)));
        stage.setWarningJson(OBJECT_MAPPER.writeValueAsString(List.of()));
        stage.setReviewStatus("parsed");
        stage.setSortOrder(1);
        return List.of(stage);
    }

    private List<BookImportChapterStage> parseText(byte[] fileBytes, String jobKey) throws Exception {
        String text = new String(fileBytes, StandardCharsets.UTF_8);
        if (text.isBlank()) {
            throw new BusinessException(400, "文本文件内容为空");
        }

        String html = textToParagraphHtml(text);
        BookImportChapterStage stage = new BookImportChapterStage();
        stage.setJobKey(jobKey);
        stage.setTempChapterKey("temp-chapter-" + shortId());
        stage.setSourcePath("text-content");
        stage.setChapterNo(1);
        stage.setTitle(extractTitleFromHtml(html, "导入文本"));
        stage.setSubtitle("");
        stage.setContentHtml(html);
        stage.setPlainText(text.trim());
        stage.setWordCount(countWords(text.trim()));
        stage.setWarningJson(OBJECT_MAPPER.writeValueAsString(List.of()));
        stage.setReviewStatus("parsed");
        stage.setSortOrder(1);
        return List.of(stage);
    }

    // ========== ZIP 包解析方法（保留原逻辑） ==========

    /**
     * 解析 ZIP 导入包并生成书籍元信息与章节暂存数据。
     * 优先读取 manifest.json 精准导入，缺少清单时自动扫描章节文件生成默认书籍。
     */
    private ParsedBookPackage parseBookPackage(byte[] zipBytes, String jobKey, String zipFileName) throws Exception {
        Map<String, byte[]> entries = readZipEntries(zipBytes);
        if (entries.isEmpty()) {
            throw new BusinessException(400, "ZIP 压缩包内容为空");
        }

        String manifestPath = findManifestPath(entries);
        if (manifestPath.isBlank()) {
            Map.Entry<String, byte[]> standaloneBookEntry = findPreferredStandaloneBookEntry(entries);
            if (standaloneBookEntry != null) {
                return parseStandaloneBookArchive(standaloneBookEntry.getKey(), standaloneBookEntry.getValue(), jobKey);
            }
            return parseBookPackageWithoutManifest(entries, jobKey, zipFileName);
        }

        Map<String, Object> manifest = readManifest(entries, manifestPath);
        String manifestBasePath = extractParentPath(manifestPath);
        String title = requireText(asText(manifest.get("title")), "manifest.json 缺少 title");
        String bookKey = defaultString(asText(manifest.get("bookKey")), buildSlugKey(title));
        String author = defaultString(asText(manifest.get("author")), "未知作者");
        String translator = defaultString(asText(manifest.get("translator")), "");
        String publisher = defaultString(asText(manifest.get("publisher")), "");
        String summary = defaultString(asText(manifest.get("summary")), "");
        String category = normalizeBookCategory(asText(manifest.get("category")), title, summary, List.of());
        String coverUrl = resolveCoverUrl(entries, defaultString(asText(manifest.get("coverFile")), ""), manifestBasePath);
        if (coverUrl.isBlank()) {
            coverUrl = resolveAutoCoverUrl(entries);
        }

        List<Map<String, Object>> chapterDefinitions = readChapterDefinitions(manifest);
        if (chapterDefinitions.isEmpty()) {
            throw new BusinessException(400, "manifest.json 缺少 chapters 章节定义");
        }

        List<BookImportChapterStage> stages = new ArrayList<>();
        int sortOrder = 1;
        for (Map<String, Object> chapterDefinition : chapterDefinitions) {
            String sourcePath = requireText(asText(chapterDefinition.get("file")), "章节文件路径不能为空");
            Map.Entry<String, byte[]> chapterEntry = resolveZipEntry(entries, sourcePath, manifestBasePath);
            if (chapterEntry == null || chapterEntry.getValue() == null) {
                throw new BusinessException(400, "章节文件不存在: " + sourcePath);
            }
            sourcePath = chapterEntry.getKey();
            byte[] chapterBytes = chapterEntry.getValue();

            String sourceType = resolveSourceType(sourcePath);
            String html = toImportHtml(sourcePath, chapterBytes);
            html = migrateZipEmbeddedImages(html, entries, sourcePath, jobKey);

            AdminContentImportPreviewRequest previewRequest = new AdminContentImportPreviewRequest();
            previewRequest.setContentHtml(html);
            previewRequest.setSourceType(sourceType);
            previewRequest.setSourceUrl("");
            previewRequest.setMigrateAssets(Boolean.FALSE);
            AdminContentImportPreviewResponse previewResponse = adminContentImportService.preview(CONTENT_TYPE_BOOK, previewRequest);

            BookImportChapterStage stage = new BookImportChapterStage();
            stage.setJobKey(jobKey);
            stage.setTempChapterKey("temp-chapter-" + shortId());
            stage.setSourcePath(sourcePath);
            stage.setChapterNo(defaultInt(asInteger(chapterDefinition.get("sortOrder"))) > 0 ? asInteger(chapterDefinition.get("sortOrder")) : sortOrder);
            stage.setTitle(defaultString(asText(chapterDefinition.get("title")), extractTitle(previewResponse.getPlainText(), sourcePath)));
            stage.setSubtitle(defaultString(asText(chapterDefinition.get("subtitle")), ""));
            stage.setContentHtml(previewResponse.getNormalizedHtml());
            stage.setPlainText(defaultString(previewResponse.getPlainText(), ""));
            stage.setWordCount(countWords(stage.getPlainText()));
            stage.setWarningJson(OBJECT_MAPPER.writeValueAsString(defaultWarnings(previewResponse.getWarnings())));
            stage.setReviewStatus("parsed");
            stage.setSortOrder(sortOrder);
            stages.add(stage);
            sortOrder++;
        }

        BookCreditInfo creditInfo = inferBookCreditInfo(title, stages, author, translator);
        return new ParsedBookPackage(bookKey, creditInfo.title(), creditInfo.author(), creditInfo.translator(), publisher, category, summary, coverUrl, stages);
    }

    /**
     * 自动扫描无清单 ZIP 中的章节文件，降低普通压缩包上传门槛。
     * 按文件路径排序后逐个转换为富文本章节，并用压缩包文件名生成默认书籍信息。
     */
    private ParsedBookPackage parseBookPackageWithoutManifest(Map<String, byte[]> entries, String jobKey, String zipFileName) throws Exception {
        List<String> chapterPaths = entries.keySet().stream()
            .filter(this::isImportableChapterPath)
            .sorted(this::compareChapterPath)
            .toList();

        if (chapterPaths.isEmpty()) {
            throw new BusinessException(400, "ZIP 中未找到可导入章节文件，请放入 txt、md、html 或 docx 文件");
        }

        String title = extractTitleFromFileName(zipFileName);
        List<BookImportChapterStage> stages = new ArrayList<>();
        int sortOrder = 1;
        for (String sourcePath : chapterPaths) {
            String sourceType = resolveSourceType(sourcePath);
            String html = toImportHtml(sourcePath, entries.get(sourcePath));
            html = migrateZipEmbeddedImages(html, entries, sourcePath, jobKey);

            AdminContentImportPreviewRequest previewRequest = new AdminContentImportPreviewRequest();
            previewRequest.setContentHtml(html);
            previewRequest.setSourceType(sourceType);
            previewRequest.setSourceUrl("");
            previewRequest.setMigrateAssets(Boolean.FALSE);
            AdminContentImportPreviewResponse previewResponse = adminContentImportService.preview(CONTENT_TYPE_BOOK, previewRequest);

            BookImportChapterStage stage = new BookImportChapterStage();
            stage.setJobKey(jobKey);
            stage.setTempChapterKey("temp-chapter-" + shortId());
            stage.setSourcePath(sourcePath);
            stage.setChapterNo(sortOrder);
            stage.setTitle(extractTitle(previewResponse.getPlainText(), sourcePath));
            stage.setSubtitle("");
            stage.setContentHtml(previewResponse.getNormalizedHtml());
            stage.setPlainText(defaultString(previewResponse.getPlainText(), ""));
            stage.setWordCount(countWords(stage.getPlainText()));
            stage.setWarningJson(OBJECT_MAPPER.writeValueAsString(defaultWarnings(previewResponse.getWarnings())));
            stage.setReviewStatus("parsed");
            stage.setSortOrder(sortOrder);
            stages.add(stage);
            sortOrder++;
        }

        BookCreditInfo creditInfo = inferBookCreditInfo(title, stages, "", "");
        String coverUrl = resolveAutoCoverUrl(entries);
        return new ParsedBookPackage(buildSlugKey(creditInfo.title()), creditInfo.title(), creditInfo.author(), creditInfo.translator(), "", "书籍", "", coverUrl, stages);
    }

    /**
     * 解析 ZIP 中直接包含的整本电子书文件。
     * 优先使用 epub、pdf、docx 这类可直接还原内容的格式，再交给单文件解析器处理。
     */
    private ParsedBookPackage parseStandaloneBookArchive(String entryName, byte[] fileBytes, String jobKey) throws Exception {
        String extension = resolveExtension(entryName);
        return parseSingleFilePackage(extension, entryName, fileBytes, jobKey);
    }

    /**
     * 从 ZIP 条目中挑出最适合直接解析的整本电子书文件。
     * 先选 epub、pdf、docx，避免把这些成品电子书误当成章节压缩包来扫。
     */
    private Map.Entry<String, byte[]> findPreferredStandaloneBookEntry(Map<String, byte[]> entries) {
        List<String> preferredExtensions = List.of("epub", "pdf", "docx");
        for (String extension : preferredExtensions) {
            Map.Entry<String, byte[]> candidate = entries.entrySet().stream()
                .filter(entry -> extension.equals(resolveExtension(entry.getKey())))
                .sorted(Map.Entry.comparingByKey(String.CASE_INSENSITIVE_ORDER))
                .findFirst()
                .orElse(null);
            if (candidate != null) {
                return candidate;
            }
        }
        return null;
    }

    /**
     * 判断 ZIP 内文件是否可作为章节导入。
     * 跳过隐藏文件、系统目录和封面资源，仅保留文本类章节格式。
     */
    private boolean isImportableChapterPath(String sourcePath) {
        String normalizedPath = defaultString(sourcePath, "").replace("\\", "/");
        String fileName = normalizedPath.contains("/") ? normalizedPath.substring(normalizedPath.lastIndexOf('/') + 1) : normalizedPath;
        if (fileName.isBlank() || fileName.startsWith(".") || normalizedPath.startsWith("__MACOSX/")) {
            return false;
        }
        return List.of("txt", "md", "markdown", "html", "htm", "docx").contains(resolveExtension(fileName));
    }

    /**
     * 比较章节路径的自然顺序，保证带数字的文件名按阅读顺序排列。
     * 优先比较文件名中的首个数字，数字相同或缺失时回退到路径字典序。
     */
    private int compareChapterPath(String leftPath, String rightPath) {
        int leftNumber = extractFirstNumber(leftPath);
        int rightNumber = extractFirstNumber(rightPath);
        if (leftNumber != rightNumber) {
            if (leftNumber < 0) return 1;
            if (rightNumber < 0) return -1;
            return Integer.compare(leftNumber, rightNumber);
        }
        return leftPath.compareToIgnoreCase(rightPath);
    }

    /**
     * 提取文件路径中的首个数字片段用于章节排序。
     * 未找到数字时返回 -1，让调用方回退到普通字符串排序。
     */
    private int extractFirstNumber(String sourcePath) {
        var matcher = java.util.regex.Pattern.compile("\\d+").matcher(defaultString(sourcePath, ""));
        if (!matcher.find()) {
            return -1;
        }
        try {
            return Integer.parseInt(matcher.group());
        } catch (Exception exception) {
            return -1;
        }
    }

    private Map<String, byte[]> readZipEntries(byte[] zipBytes) throws Exception {
        for (Charset charset : List.of(StandardCharsets.UTF_8, Charset.forName("GB18030"), Charset.forName("GBK"))) {
            try {
                Map<String, byte[]> entries = readZipEntries(zipBytes, charset);
                if (!entries.isEmpty()) {
                    return entries;
                }
            } catch (IllegalArgumentException exception) {
                continue;
            }
        }

        throw new BusinessException(400, "ZIP 文件名编码无法识别，请重新打包后再上传");
    }

    /**
     * 用指定编码读取 ZIP 条目，兼容不同系统生成的压缩包文件名。
     * 先按当前主流编码解包，再由调用方决定是否继续回退到其他中文编码。
     */
    private Map<String, byte[]> readZipEntries(byte[] zipBytes, Charset charset) throws Exception {
        Map<String, byte[]> entries = new HashMap<>();
        try (ZipInputStream zipInputStream = new ZipInputStream(new ByteArrayInputStream(zipBytes), charset)) {
            ZipEntry entry;
            while ((entry = zipInputStream.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    continue;
                }
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                zipInputStream.transferTo(outputStream);
                entries.put(entry.getName(), outputStream.toByteArray());
            }
        }
        return entries;
    }

    /**
     * 定位压缩包内的 manifest.json 文件。
     * 兼容文件位于顶层或书籍目录内的场景，优先返回顶层清单。
     */
    private String findManifestPath(Map<String, byte[]> entries) {
        Map.Entry<String, byte[]> rootManifest = findZipEntryByNormalizedPath(entries, "manifest.json");
        if (rootManifest != null) {
            return rootManifest.getKey();
        }
        return entries.keySet().stream()
            .filter(path -> "manifest.json".equalsIgnoreCase(extractFileNameFromPath(path)))
            .sorted(String.CASE_INSENSITIVE_ORDER)
            .findFirst()
            .orElse("");
    }

    /**
     * 读取指定路径下的导入清单。
     * 通过标准化路径匹配真实条目，避免顶层目录或编码差异导致清单丢失。
     */
    private Map<String, Object> readManifest(Map<String, byte[]> entries, String manifestPath) throws Exception {
        Map.Entry<String, byte[]> manifestEntry = findZipEntryByNormalizedPath(entries, manifestPath);
        byte[] manifestBytes = manifestEntry == null ? null : manifestEntry.getValue();
        if (manifestBytes == null) {
            throw new BusinessException(400, "导入包缺少 manifest.json");
        }
        return OBJECT_MAPPER.readValue(manifestBytes, new TypeReference<Map<String, Object>>() {});
    }

    /**
     * 迁移 ZIP 章节 HTML 中引用的包内图片资源。
     * 按章节相对路径解析图片并上传到对象存储，再把正文引用替换为可访问地址。
     */
    private String migrateZipEmbeddedImages(String html, Map<String, byte[]> entries, String chapterPath, String jobKey) throws Exception {
        if (html == null || html.isBlank() || entries == null || entries.isEmpty()) {
            return html;
        }
        Document document = Jsoup.parseBodyFragment(html);
        document.outputSettings().prettyPrint(false);
        for (Element image : document.select("img, image")) {
            String uploadedUrl = "";
            for (String rawPath : extractImageSourceCandidates(image)) {
                uploadedUrl = uploadZipEmbeddedImage(entries, chapterPath, rawPath, jobKey);
                if (!uploadedUrl.isBlank()) {
                    break;
                }
            }
            if (!uploadedUrl.isBlank()) {
                String attributeName = resolvePrimaryImageAttribute(image);
                image.attr(attributeName, uploadedUrl);
                if ("img".equalsIgnoreCase(image.tagName())) {
                    image.attr("loading", "lazy");
                    image.removeAttr("data-src");
                    image.removeAttr("data-original");
                    image.removeAttr("data-lazy-src");
                    image.removeAttr("srcset");
                }
            }
        }
        return document.body().html();
    }

    /**
     * 收集图片节点中可能承载真实地址的属性。
     * 按常规 src 到懒加载字段的顺序尝试，兼容常见网页打包后的图片写法。
     */
    private List<String> extractImageSourceCandidates(Element image) {
        List<String> candidates = new ArrayList<>();
        for (String attributeName : List.of("src", "href", "xlink:href", "data-src", "data-original", "data-lazy-src")) {
            String value = defaultString(image.attr(attributeName), "").trim();
            if (!value.isBlank()) {
                candidates.add(value);
            }
        }
        String srcsetFirstUrl = extractFirstSrcsetUrl(image.attr("srcset"));
        if (!srcsetFirstUrl.isBlank()) {
            candidates.add(srcsetFirstUrl);
        }
        return candidates.stream().distinct().toList();
    }

    /**
     * 确定图片节点回填真实地址时使用的主属性。
     * 普通 img 统一写回 src，SVG image 则沿用 href 或 xlink:href。
     */
    private String resolvePrimaryImageAttribute(Element image) {
        if ("img".equalsIgnoreCase(image.tagName())) {
            return "src";
        }
        if (image.hasAttr("href")) {
            return "href";
        }
        return "xlink:href";
    }

    /**
     * 从 srcset 中取出首个图片地址。
     * 只解析逗号分隔项的第一段 URL，交由后续路径解析流程继续处理。
     */
    private String extractFirstSrcsetUrl(String srcset) {
        String normalizedSrcset = defaultString(srcset, "").trim();
        if (normalizedSrcset.isBlank()) {
            return "";
        }
        String firstItem = normalizedSrcset.split(",")[0].trim();
        int spaceIndex = firstItem.indexOf(' ');
        return spaceIndex >= 0 ? firstItem.substring(0, spaceIndex).trim() : firstItem;
    }

    /**
     * 上传 ZIP 包内单个图片资源。
     * 仅处理包内相对路径图片，外链和 data URL 继续沿用原始值。
     */
    private String uploadZipEmbeddedImage(Map<String, byte[]> entries, String chapterPath, String rawPath, String jobKey) throws Exception {
        String sourcePath = defaultString(rawPath, "").trim();
        if (sourcePath.isBlank()) {
            return "";
        }
        String lowerPath = sourcePath.toLowerCase(Locale.ROOT);
        if (lowerPath.startsWith("http://") || lowerPath.startsWith("https://") || lowerPath.startsWith("data:")
            || isManagedStorageUrl(sourcePath)) {
            return sourcePath;
        }
        Map.Entry<String, byte[]> imageEntry = resolveZipEntry(entries, sourcePath, chapterPath);
        if (imageEntry == null) {
            return "";
        }
        byte[] imageBytes = imageEntry.getValue();
        if (imageBytes == null || imageBytes.length == 0) {
            return "";
        }
        String fileName = extractFileNameFromPath(imageEntry.getKey());
        // 仅处理图片格式的资源，忽略 CSS/JS 等非图像引用
        if (!isImageFilePath(imageEntry.getKey())) {
            // 尝试按 MIME 类型兜底：若字节头部符合图片特征也允许上传
            if (!isImageFileByMagicBytes(imageBytes)) {
                return "";
            }
        }
        return storageRoutingService.resolveForModule("book").upload(
            "book/content/" + defaultString(jobKey, "zip"),
            fileName,
            new ByteArrayInputStream(imageBytes),
            imageBytes.length,
            guessContentType(fileName)
        );
    }

    /**
     * 在 ZIP 条目中解析章节相对资源路径。
     * 同时尝试原路径、章节同级路径、根路径和文件名兜底匹配。
     */
    private Map.Entry<String, byte[]> resolveZipEntry(Map<String, byte[]> entries, String rawPath, String chapterPath) {
        String normalizedPath = normalizeArchivePath(rawPath);
        if (normalizedPath.isBlank()) {
            return null;
        }
        List<String> candidates = new ArrayList<>();
        candidates.add(normalizedPath);
        if (rawPath.startsWith("/")) {
            candidates.add(normalizeArchivePath(rawPath.substring(1)));
        }
        String normalizedChapterPath = normalizeArchivePath(chapterPath);
        int lastSlash = normalizedChapterPath.lastIndexOf('/');
        if (lastSlash >= 0 && !rawPath.startsWith("/")) {
            candidates.add(normalizeArchivePath(normalizedChapterPath.substring(0, lastSlash + 1) + normalizedPath));
        }
        for (String candidate : candidates) {
            Map.Entry<String, byte[]> matchedEntry = findZipEntryByNormalizedPath(entries, candidate);
            if (matchedEntry != null) {
                return matchedEntry;
            }
        }
        String fileName = extractFileNameFromPath(normalizedPath);
        if (fileName.isBlank()) {
            return null;
        }
        return entries.entrySet().stream()
            .filter(entry -> extractFileNameFromPath(entry.getKey()).equalsIgnoreCase(fileName))
            .findFirst()
            .orElse(null);
    }

    /**
     * 按标准化路径在 ZIP 条目中查找资源。
     * 消除路径分隔符、编码和相对目录差异，提升不同打包工具的兼容性。
     */
    private Map.Entry<String, byte[]> findZipEntryByNormalizedPath(Map<String, byte[]> entries, String targetPath) {
        String normalizedTarget = normalizeArchivePath(targetPath);
        if (normalizedTarget.isBlank()) {
            return null;
        }
        return entries.entrySet().stream()
            .filter(entry -> normalizeArchivePath(entry.getKey()).equalsIgnoreCase(normalizedTarget))
            .findFirst()
            .orElse(null);
    }

    /**
     * 标准化压缩包内部资源路径。
     * 去除查询串、锚点、URL 编码和相对目录片段，统一用于资源查找。
     */
    private String normalizeArchivePath(String path) {
        String normalized = defaultString(path, "").trim().replace("\\", "/");
        if (normalized.isBlank()) {
            return "";
        }
        int queryIndex = normalized.indexOf('?');
        if (queryIndex >= 0) {
            normalized = normalized.substring(0, queryIndex);
        }
        int fragmentIndex = normalized.indexOf('#');
        if (fragmentIndex >= 0) {
            normalized = normalized.substring(0, fragmentIndex);
        }
        try {
            normalized = URLDecoder.decode(normalized, StandardCharsets.UTF_8);
        } catch (Exception ignored) {
            normalized = normalized.replace("%20", " ");
        }
        while (normalized.startsWith("./")) {
            normalized = normalized.substring(2);
        }
        while (normalized.contains("/./")) {
            normalized = normalized.replace("/./", "/");
        }
        while (normalized.contains("//")) {
            normalized = normalized.replace("//", "/");
        }
        List<String> segments = new ArrayList<>();
        for (String segment : normalized.split("/")) {
            if (segment.isBlank() || ".".equals(segment)) {
                continue;
            }
            if ("..".equals(segment)) {
                if (!segments.isEmpty()) {
                    segments.remove(segments.size() - 1);
                }
                continue;
            }
            segments.add(segment);
        }
        return String.join("/", segments);
    }

    /**
     * 从资源路径中提取文件名。
     * 统一兼容正反斜杠和查询串，供上传命名与兜底匹配复用。
     */
    private String extractFileNameFromPath(String sourcePath) {
        String normalizedPath = normalizeArchivePath(sourcePath);
        int lastSlash = normalizedPath.lastIndexOf('/');
        return lastSlash >= 0 ? normalizedPath.substring(lastSlash + 1) : normalizedPath;
    }

    /**
     * 从压缩包条目路径中提取父目录。
     * 为 manifest 内相对章节和封面路径提供同级目录基准。
     */
    private String extractParentPath(String sourcePath) {
        String normalizedPath = normalizeArchivePath(sourcePath);
        int lastSlash = normalizedPath.lastIndexOf('/');
        return lastSlash >= 0 ? normalizedPath.substring(0, lastSlash + 1) : "";
    }

    private List<Map<String, Object>> readChapterDefinitions(Map<String, Object> manifest) {
        Object rawValue = manifest.get("chapters");
        if (!(rawValue instanceof List<?> listValue)) {
            return List.of();
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object item : listValue) {
            if (item instanceof Map<?, ?> mapItem) {
                Map<String, Object> normalizedMap = new HashMap<>();
                mapItem.forEach((key, value) -> normalizedMap.put(String.valueOf(key), value));
                result.add(normalizedMap);
            }
        }
        return result;
    }

    private String resolveCoverUrl(Map<String, byte[]> entries, String coverFile, String basePath) throws Exception {
        if (coverFile == null || coverFile.isBlank()) {
            return "";
        }
        Map.Entry<String, byte[]> coverEntry = resolveZipEntry(entries, coverFile, basePath);
        byte[] coverBytes = coverEntry == null ? null : coverEntry.getValue();
        if (coverBytes == null || !isValidCoverImageResource(coverEntry.getKey(), guessContentType(coverEntry.getKey()), coverBytes)) {
            return "";
        }
        String coverName = coverEntry == null ? coverFile : coverEntry.getKey();
        return storageRoutingService.resolveForModule("book-cover").upload(
            "book/cover", extractFileNameFromPath(coverName),
            new ByteArrayInputStream(coverBytes), coverBytes.length,
            guessContentType(coverName)
        );
    }

    /**
     * 上传已确认有效的 EPUB 封面图片资源。
     * 复用统一图片校验过滤封面页文本、OPF 清单等非图片内容后再写入对象存储。
     */
    private String uploadEpubCoverResource(Resource coverImage, String fileName) throws Exception {
        if (coverImage == null) {
            return "";
        }
        byte[] coverBytes = coverImage.getData();
        String contentType = coverImage.getMediaType() == null ? "" : coverImage.getMediaType().getName();
        if (!isValidCoverImageResource(coverImage.getHref(), contentType, coverBytes)) {
            return "";
        }
        String coverName = defaultString(coverImage.getHref(), extractTitleFromFileName(fileName) + "-cover");
        if (!coverName.contains(".") && coverImage.getMediaType() != null && coverImage.getMediaType().getDefaultExtension() != null) {
            coverName = coverName + "." + coverImage.getMediaType().getDefaultExtension();
        }
        return storageRoutingService.resolveForModule("book-cover").upload(
            "book/cover",
            coverName.substring(coverName.lastIndexOf('/') + 1),
            new ByteArrayInputStream(coverBytes),
            coverBytes.length,
            defaultString(contentType, guessContentType(coverName))
        );
    }

    /**
     * 自动识别并上传 EPUB 内置封面图片。
     * 依次读取标准封面、开篇图片页和资源清单候选，兼容封面被放进第一章的文件。
     */
    private String resolveEpubCoverUrl(nl.siegmann.epublib.domain.Book epubBook, String fileName) throws Exception {
        String coverUrl = uploadEpubCoverResource(epubBook.getCoverImage(), fileName);
        if (!coverUrl.isBlank() || epubBook.getResources() == null) {
            return coverUrl;
        }

        coverUrl = resolveEpubCoverFromOpeningChapters(epubBook, fileName);
        if (!coverUrl.isBlank()) {
            return coverUrl;
        }

        Resource bestResource = null;
        byte[] bestBytes = null;
        for (Resource resource : epubBook.getResources().getAll()) {
            if (resource == null) {
                continue;
            }
            byte[] bytes = resource.getData();
            String contentType = resource.getMediaType() == null ? "" : resource.getMediaType().getName();
            if (!isValidCoverImageResource(resource.getHref(), contentType, bytes)) {
                continue;
            }
            if (bestResource == null || scoreCoverPath(resource.getHref(), bytes) > scoreCoverPath(bestResource.getHref(), bestBytes)) {
                bestResource = resource;
                bestBytes = bytes;
            }
        }
        return uploadEpubCoverResource(bestResource, fileName);
    }

    /**
     * 从 EPUB 开篇章节中提取疑似封面图片。
     * 仅处理前几页的少文本图片页，兜底支持未在 metadata 标记 cover 的电子书。
     */
    private String resolveEpubCoverFromOpeningChapters(nl.siegmann.epublib.domain.Book epubBook, String fileName) throws Exception {
        if (epubBook == null || epubBook.getSpine() == null) {
            return "";
        }
        List<SpineReference> spineReferences = epubBook.getSpine().getSpineReferences();
        int maxScanCount = Math.min(3, spineReferences.size());
        for (int index = 0; index < maxScanCount; index++) {
            SpineReference reference = spineReferences.get(index);
            Resource chapterResource = reference == null ? null : reference.getResource();
            if (chapterResource == null) {
                continue;
            }
            String html = new String(chapterResource.getData(), StandardCharsets.UTF_8);
            if (!isOpeningCoverCandidateChapter(chapterResource, html, index + 1)) {
                continue;
            }
            Resource coverResource = resolveFirstValidEpubImageResource(epubBook, chapterResource, html);
            String coverUrl = uploadEpubCoverResource(coverResource, fileName);
            if (!coverUrl.isBlank()) {
                return coverUrl;
            }
        }
        return "";
    }

    /**
     * 判断开篇章节是否适合作为封面兜底来源。
     * 前两页若路径或标题含封面特征，或第一页只有图片和极少文本，就允许尝试提取。
     */
    private boolean isOpeningCoverCandidateChapter(Resource chapterResource, String html, int chapterNo) {
        if (chapterNo > 2 || html == null || html.isBlank()) {
            return false;
        }
        Document document = Jsoup.parse(defaultString(html, ""));
        String text = document.text().trim();
        int imageCount = document.select("img[src], image[href], image[xlink\\:href]").size();
        if (imageCount == 0 || text.length() > 80) {
            return false;
        }
        String href = normalizeEpubHref(chapterResource == null ? "" : chapterResource.getHref()).toLowerCase(Locale.ROOT);
        String title = extractTitleFromHtml(html, "").toLowerCase(Locale.ROOT);
        boolean coverHint = href.matches(".*(cover|封面|title|front|fmatter).*") || title.matches(".*(cover|封面|title|front).*");
        return coverHint || (chapterNo == 1 && imageCount <= 2 && text.length() <= 20);
    }

    /**
     * 从章节 HTML 中解析第一张有效 EPUB 图片资源。
     * 按图片节点顺序回溯资源清单，过滤空文件和非图片内容后返回可上传资源。
     */
    private Resource resolveFirstValidEpubImageResource(nl.siegmann.epublib.domain.Book epubBook, Resource chapterResource, String html) throws IOException {
        Document document = Jsoup.parse(defaultString(html, ""));
        for (Element image : document.select("img[src], image[href], image[xlink\\:href]")) {
            String attributeName = image.hasAttr("src") ? "src" : image.hasAttr("href") ? "href" : "xlink:href";
            Resource embeddedResource = resolveEpubResource(epubBook, chapterResource, image.attr(attributeName));
            if (embeddedResource == null) {
                continue;
            }
            byte[] bytes = embeddedResource.getData();
            String contentType = embeddedResource.getMediaType() == null ? "" : embeddedResource.getMediaType().getName();
            if (isValidCoverImageResource(embeddedResource.getHref(), contentType, bytes)) {
                return embeddedResource;
            }
        }
        return null;
    }

    /**
     * 自动识别并上传压缩包中的封面图片。
     * 优先选择文件名包含封面特征的图片，缺失时按图片大小选择最可能的候选。
     */
    private String resolveAutoCoverUrl(Map<String, byte[]> entries) throws Exception {
        Map.Entry<String, byte[]> candidate = entries.entrySet().stream()
            .filter(entry -> isCoverImagePath(entry.getKey()))
            .filter(entry -> isValidCoverImageResource(entry.getKey(), guessContentType(entry.getKey()), entry.getValue()))
            .sorted((left, right) -> {
                int scoreCompare = Integer.compare(
                    scoreCoverPath(right.getKey(), right.getValue()),
                    scoreCoverPath(left.getKey(), left.getValue())
                );
                if (scoreCompare != 0) {
                    return scoreCompare;
                }
                int sizeCompare = Integer.compare(right.getValue().length, left.getValue().length);
                return sizeCompare != 0 ? sizeCompare : String.CASE_INSENSITIVE_ORDER.compare(left.getKey(), right.getKey());
            })
            .findFirst()
            .orElse(null);
        if (candidate == null) {
            return "";
        }
        String coverFile = candidate.getKey();
        byte[] coverBytes = candidate.getValue();
        return storageRoutingService.resolveForModule("book-cover").upload(
            "book/cover",
            extractFileNameFromPath(coverFile),
            new ByteArrayInputStream(coverBytes),
            coverBytes.length,
            guessContentType(coverFile)
        );
    }

    /**
     * 判断文件路径是否属于可作为封面或内嵌展示的图片资源。
     * 覆盖主流图片格式，避免章节内容中的插图因扩展名识别失败而丢失。
     */
    private boolean isImageFilePath(String sourcePath) {
        String normalizedPath = normalizeArchivePath(sourcePath);
        String fileName = extractFileNameFromPath(normalizedPath);
        if (fileName.isBlank() || fileName.startsWith(".") || normalizedPath.startsWith("__MACOSX/")) {
            return false;
        }
        return List.of("jpg", "jpeg", "png", "webp", "gif", "bmp", "svg",
            "tiff", "tif", "avif", "jfif", "jpe", "jp2", "ico").contains(resolveExtension(fileName));
    }

    /**
     * 别名兼容：仅用于外部封面资源路径过滤（保持原名以便语义阅读）。
     */
    private boolean isCoverImagePath(String sourcePath) {
        return isImageFilePath(sourcePath);
    }

    /**
     * 判断候选封面资源是否是真实图片内容。
     * 同时检查路径后缀、MIME 类型和文件魔数，避免 XHTML 或 OPF 文本被保存成封面地址。
     */
    private boolean isValidCoverImageResource(String sourcePath, String contentType, byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return false;
        }
        String lowerContentType = defaultString(contentType, "").toLowerCase(Locale.ROOT);
        boolean hasImageSuffix = isImageFilePath(sourcePath);
        boolean hasImageMime = lowerContentType.startsWith("image/");
        boolean hasImageMagic = isImageFileByMagicBytes(bytes);
        if (!hasImageMagic && !(hasImageSuffix && hasImageMime)) {
            return false;
        }
        return !looksLikeTextMarkup(bytes);
    }

    private boolean isImageFileByMagicBytes(byte[] bytes) {
        if (bytes == null || bytes.length < 4) {
            return false;
        }
        return MagicBytesValidator.isImage(bytes);
    }

    /**
     * 判断字节内容是否更像 XHTML、OPF 或普通文本。
     * 在扩展名和 MIME 被误标时兜底拦截非图片封面，避免破图地址写入封面字段。
     */
    private boolean looksLikeTextMarkup(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return false;
        }
        String header = new String(bytes, 0, Math.min(bytes.length, 512), StandardCharsets.UTF_8)
            .trim()
            .toLowerCase(Locale.ROOT);
        if (header.isEmpty()) {
            return false;
        }
        return header.startsWith("<?xml")
            || header.startsWith("<html")
            || header.startsWith("<!doctype html")
            || header.startsWith("<package")
            || header.startsWith("<opf")
            || header.startsWith("<ncx")
            || header.startsWith("<body")
            || header.startsWith("<metadata")
            || header.startsWith("<manifest")
            || header.startsWith("<spine")
            || header.contains("<html")
            || header.contains("<package")
            || header.contains("<manifest")
            || header.contains("<spine")
            || header.contains("<metadata")
            || header.contains("<dc:title")
            || header.contains("<itemref");
    }

    /**
     * 计算压缩包图片成为封面的优先级。
     * 文件名命中封面词会显著加权，缩略图、图标等资源会降低排序。
     */
    private int scoreCoverPath(String sourcePath, byte[] bytes) {
        String lowerPath = defaultString(sourcePath, "").replace("\\", "/").toLowerCase(Locale.ROOT);
        String fileName = lowerPath.contains("/") ? lowerPath.substring(lowerPath.lastIndexOf('/') + 1) : lowerPath;
        int score = bytes != null && bytes.length >= 50 * 1024 ? 10 : 0;
        if (fileName.matches(".*(cover|封面|front|folder|title|poster).*")) {
            score += 100;
        }
        if (lowerPath.matches(".*(/cover/|/covers/|/image/|/images/|/img/).*")) {
            score += 15;
        }
        if (fileName.matches(".*(chapter|section|page|illustration|insert|inline|figure|fig|body|text).*")) {
            score -= 35;
        }
        if (fileName.matches(".*(logo|icon|sprite|avatar|thumb|thumbnail).*")) {
            score -= 50;
        }
        if (bytes != null) {
            if (bytes.length < 5 * 1024) {
                score -= 80;
            } else if (bytes.length < 20 * 1024) {
                score -= 25;
            } else if (bytes.length >= 200 * 1024) {
                score += 20;
            }
        }
        return score;
    }

    private String toImportHtml(String sourcePath, byte[] chapterBytes) {
        String extension = resolveExtension(sourcePath);
        if ("html".equals(extension) || "htm".equals(extension)) {
            return new String(chapterBytes, StandardCharsets.UTF_8);
        }
        if ("docx".equals(extension)) {
            return docxToHtml(chapterBytes);
        }
        if ("md".equals(extension) || "markdown".equals(extension)) {
            return markdownToHtml(new String(chapterBytes, StandardCharsets.UTF_8));
        }
        return textToParagraphHtml(new String(chapterBytes, StandardCharsets.UTF_8));
    }

    private String docxToHtml(byte[] chapterBytes) {
        StringBuilder builder = new StringBuilder();
        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(chapterBytes))) {
            for (XWPFParagraph paragraph : document.getParagraphs()) {
                String text = defaultString(paragraph.getText(), "").trim();
                if (text.isEmpty()) continue;
                String style = defaultString(paragraph.getStyle(), "").toLowerCase(Locale.ROOT);
                if (style.contains("heading1") || style.contains("title1")) {
                    builder.append("<h1>").append(escapeHtml(text)).append("</h1>");
                } else if (style.contains("heading2") || style.contains("title2")) {
                    builder.append("<h2>").append(escapeHtml(text)).append("</h2>");
                } else if (style.contains("heading3") || style.contains("title3")) {
                    builder.append("<h3>").append(escapeHtml(text)).append("</h3>");
                } else {
                    builder.append("<p>").append(escapeHtml(text)).append("</p>");
                }
            }
        } catch (Exception exception) {
            throw new BusinessException(400, "DOCX 章节解析失败: " + exception.getMessage());
        }
        return builder.toString();
    }

    private String markdownToHtml(String markdown) {
        String text = defaultString(markdown, "").replace("\r\n", "\n");
        StringBuilder builder = new StringBuilder();
        int i = 0;
        String[] lines = text.split("\n", -1);
        while (i < lines.length) {
            String line = lines[i];
            String trimmed = line.trim();

            if (trimmed.isEmpty()) {
                i++;
                continue;
            }

            // 分隔线
            if (trimmed.matches("^[-*_]{3,}$")) {
                builder.append("<hr />");
                i++;
                continue;
            }

            // 围栏代码块
            if (trimmed.startsWith("```")) {
                String lang = trimmed.substring(3).trim();
                builder.append("<pre><code");
                if (!lang.isEmpty()) {
                    builder.append(" class=\"language-").append(escapeHtml(lang)).append("\"");
                }
                builder.append(">");
                i++;
                while (i < lines.length && !lines[i].trim().startsWith("```")) {
                    builder.append(escapeHtml(lines[i])).append("\n");
                    i++;
                }
                builder.append("</code></pre>");
                i++; // skip closing ```
                continue;
            }

            // 标题
            if (trimmed.startsWith("### ")) {
                builder.append("<h3>").append(renderInlineMarkdown(trimmed.substring(4))).append("</h3>");
                i++;
                continue;
            }
            if (trimmed.startsWith("## ")) {
                builder.append("<h2>").append(renderInlineMarkdown(trimmed.substring(3))).append("</h2>");
                i++;
                continue;
            }
            if (trimmed.startsWith("# ")) {
                builder.append("<h1>").append(renderInlineMarkdown(trimmed.substring(2))).append("</h1>");
                i++;
                continue;
            }

            // 引用
            if (trimmed.startsWith("> ")) {
                builder.append("<blockquote><p>").append(renderInlineMarkdown(trimmed.substring(2))).append("</p></blockquote>");
                i++;
                continue;
            }

            // 无序列表
            if (trimmed.matches("^[-*+]\\s.+")) {
                builder.append("<ul>");
                while (i < lines.length && lines[i].trim().matches("^[-*+]\\s.+")) {
                    String itemText = lines[i].trim().replaceFirst("^[-*+]\\s+", "");
                    builder.append("<li>").append(renderInlineMarkdown(itemText)).append("</li>");
                    i++;
                }
                builder.append("</ul>");
                continue;
            }

            // 有序列表
            if (trimmed.matches("^\\d+\\.\\s.+")) {
                builder.append("<ol>");
                while (i < lines.length && lines[i].trim().matches("^\\d+\\.\\s.+")) {
                    String itemText = lines[i].trim().replaceFirst("^\\d+\\.\\s+", "");
                    builder.append("<li>").append(renderInlineMarkdown(itemText)).append("</li>");
                    i++;
                }
                builder.append("</ol>");
                continue;
            }

            // 普通段落
            builder.append("<p>").append(renderInlineMarkdown(trimmed)).append("</p>");
            i++;
        }
        return builder.toString();
    }

    /**
     * 渲染行内 Markdown：加粗、斜体、行内代码、删除线、链接、图片。
     */
    private String renderInlineMarkdown(String text) {
        String escaped = escapeHtml(text);
        // 图片 ![alt](url)
        escaped = escaped.replaceAll("!\\[([^]]*)]\\(([^)]+)\\)", "<img src=\"$2\" alt=\"$1\" />");
        // 链接 [text](url)
        escaped = escaped.replaceAll("\\[([^]]*)]\\(([^)]+)\\)", "<a href=\"$2\" target=\"_blank\" rel=\"noopener\">$1</a>");
        // 加粗 **text**
        escaped = escaped.replaceAll("\\*\\*(.+?)\\*\\*", "<strong>$1</strong>");
        // 斜体 *text* (不冲突已处理的加粗)
        escaped = escaped.replaceAll("(?<!<strong[^>]*>)\\*(.+?)\\*(?!</strong>)", "<em>$1</em>");
        // 删除线 ~~text~~
        escaped = escaped.replaceAll("~~(.+?)~~", "<del>$1</del>");
        // 行内代码 `code`
        escaped = escaped.replaceAll("`([^`]+)`", "<code>$1</code>");
        return escaped;
    }

    private String textToParagraphHtml(String text) {
        return defaultString(text, "")
            .replace("\r\n", "\n")
            .trim()
            .replaceAll("\\n{2,}", "</p><p>")
            .replace("\n", "<br />")
            .transform(content -> content.isBlank() ? "" : "<p>" + escapeHtml(content).replace("&lt;br /&gt;", "<br />") + "</p>");
    }

    // ========== 外部资源获取 ==========

    private byte[] fetchExternalContent(String url) throws Exception {
        var client = java.net.http.HttpClient.newBuilder()
            .followRedirects(java.net.http.HttpClient.Redirect.NORMAL)
            .connectTimeout(java.time.Duration.ofSeconds(15))
            .build();
        var request = java.net.http.HttpRequest.newBuilder()
            .uri(URI.create(url))
            .timeout(java.time.Duration.ofSeconds(30))
            .GET()
            .build();
        var response = client.send(request, java.net.http.HttpResponse.BodyHandlers.ofByteArray());
        if (response.statusCode() >= 400) {
            throw new BusinessException(400, "外部资源请求失败，HTTP 状态码: " + response.statusCode());
        }
        return response.body();
    }

    private String detectFormatFromUrl(String url) {
        String path = url.split("[?#]")[0].toLowerCase(Locale.ROOT);
        if (path.endsWith(".epub")) return "epub";
        if (path.endsWith(".pdf")) return "pdf";
        if (path.endsWith(".md") || path.endsWith(".markdown")) return "md";
        if (path.endsWith(".docx")) return "docx";
        if (path.endsWith(".html") || path.endsWith(".htm")) return "html";
        if (path.endsWith(".txt")) return "txt";
        if (path.endsWith(".zip")) return "zip";
        return "html";
    }

    private String extractTitleFromUrl(String url) {
        String path = url.split("[?#]")[0];
        int lastSlash = path.lastIndexOf('/');
        if (lastSlash >= 0) {
            String fileName = path.substring(lastSlash + 1);
            int dotIndex = fileName.lastIndexOf('.');
            return dotIndex > 0 ? fileName.substring(0, dotIndex) : fileName;
        }
        return "外部资源导入";
    }

    // ========== 通用工具方法 ==========

    private boolean isValidSingleFileFormat(String extension) {
        return List.of("epub", "pdf", "txt", "md", "markdown", "docx", "html", "htm").contains(extension);
    }

    private void evictStarReadCache() {
        var cache = cacheManager.getCache(CacheConfig.CACHE_STAR_READ);
        if (cache != null) {
            cache.clear();
        }
    }

    private String getFormatLabel(String format) {
        return FORMAT_LABELS.getOrDefault(format, format.toUpperCase());
    }

    private BookSourceFile buildSourceFile(String fileKey, String originalName, String storageUrl, long fileSize, String fileHash) {
        BookSourceFile sourceFile = new BookSourceFile();
        sourceFile.setFileKey(fileKey);
        sourceFile.setOriginalName(originalName);
        sourceFile.setStorageUrl(storageUrl);
        sourceFile.setFileSize(fileSize);
        sourceFile.setFileHash(fileHash);
        return sourceFile;
    }

    /**
     * 创建书籍导入任务的初始状态，补齐文件来源和章节统计默认值。
     * 先写入待解析状态供后台展示，后续解析完成后再覆盖书籍信息与章节数量。
     */
    private BookImportJob createInitialJob(String title, String sourceFileKey, String importType, String originalFormat, long fileSize) {
        BookImportJob job = new BookImportJob();
        job.setJobKey("book-job-" + shortId());
        job.setBookKey("");
        job.setTitle(title);
        job.setAuthor("");
        job.setTranslator("");
        job.setPublisher("");
        job.setSummary("");
        job.setCoverUrl("");
        job.setSourceFileKey(sourceFileKey);
        job.setImportType(importType);
        job.setSourceUrl("");
        job.setOriginalFormat(originalFormat);
        job.setFileSize(fileSize);
        job.setStatus("uploaded");
        job.setProgress(0);
        job.setTotalChapters(0);
        job.setSuccessChapters(0);
        job.setFailChapters(0);
        job.setCategory("精品书籍");
        job.setMessage("等待解析");
        return job;
    }

    private void updateJobWithParsedData(BookImportJob job, ParsedBookPackage parsedBook) {
        job.setBookKey(parsedBook.bookKey());
        job.setTitle(parsedBook.title());
        job.setAuthor(parsedBook.author());
        job.setTranslator(parsedBook.translator());
        job.setPublisher(parsedBook.publisher());
        job.setSummary(parsedBook.summary());
        job.setCategory(normalizeBookCategory(parsedBook.category(), parsedBook.title(), parsedBook.summary(), parsedBook.stages()));
        job.setCoverUrl(parsedBook.coverUrl());
        job.setTotalChapters(parsedBook.stages().size());
        job.setSuccessChapters(parsedBook.stages().size());
        job.setFailChapters(0);
        job.setProgress(80);
        job.setStatus(STATUS_AWAIT_REVIEW);
        job.setMessage("章节解析完成，等待审核发布");
    }

    /**
     * 综合文件名、元数据和章节正文推断书籍署名信息。
     * 优先保留已有元数据，缺失时从标题括号和前置版权页文本识别作者与译者。
     */
    private BookCreditInfo inferBookCreditInfo(String rawTitle, List<BookImportChapterStage> stages, String knownAuthor, String knownTranslator) {
        String title = extractTitleFromFileName(defaultString(rawTitle, "导入书籍"));
        String author = defaultString(knownAuthor, "");
        String translator = defaultString(knownTranslator, "");

        BookCreditInfo titleInfo = parseCreditInfoFromText(title);
        if (author.isBlank()) {
            author = titleInfo.author();
        }
        if (translator.isBlank()) {
            translator = titleInfo.translator();
        }
        title = cleanupCreditText(titleInfo.title());

        if (author.isBlank() || translator.isBlank()) {
            String sampleText = stages.stream()
                .limit(3)
                .map(BookImportChapterStage::getPlainText)
                .filter(text -> text != null && !text.isBlank())
                .reduce("", (left, right) -> left + "\n" + right);
            BookCreditInfo textInfo = parseCreditInfoFromText(sampleText);
            if (author.isBlank()) {
                author = textInfo.author();
            }
            if (translator.isBlank()) {
                translator = textInfo.translator();
            }
        }

        return new BookCreditInfo(defaultString(title, "导入书籍"), defaultString(author, "未知作者"), defaultString(translator, ""));
    }

    /**
     * 统一标准化书籍分类，只保留运营约定的六类标签。
     * 优先采用人工选择，缺失时再结合标题、摘要与前几章文本自动识别最接近的分类。
     */
    private String normalizeBookCategory(String rawCategory, String title, String summary, List<BookImportChapterStage> stages) {
        String text = defaultString(rawCategory, "").trim();
        if (!text.isBlank()) {
            String normalized = switch (text.toLowerCase(Locale.ROOT)) {
                case "精品书籍", "精品", "best", "featured" -> "精品书籍";
                case "历史", "history" -> "历史";
                case "文学", "literature" -> "文学";
                case "悬疑", "suspense", "mystery", "detective" -> "悬疑";
                case "人物传记", "传记", "biography", "memoir" -> "人物传记";
                case "名家代表", "名家", "classic", "masterpiece" -> "名家代表";
                default -> "";
            };
            if (!normalized.isBlank()) {
                return normalized;
            }
        }

        String sampleText = defaultString(title, "") + "\n" + defaultString(summary, "") + "\n" +
            stages.stream()
                .limit(3)
                .map(BookImportChapterStage::getPlainText)
                .filter(item -> item != null && !item.isBlank())
                .reduce("", (left, right) -> left + "\n" + right);
        String lowerText = sampleText.toLowerCase(Locale.ROOT);

        if (matchesBookCategory(lowerText, "拿破仑", "王朝", "帝国", "朝代", "战争", "史", "编年", "历史")) {
            return "历史";
        }
        if (matchesBookCategory(lowerText, "侦探", "探案", "谋杀", "疑案", "凶手", "悬疑", "推理", "谜团")) {
            return "悬疑";
        }
        if (matchesBookCategory(lowerText, "传记", "回忆录", "人物", "先生", "女士", "自述", "生平", " biography ")) {
            return "人物传记";
        }
        if (matchesBookCategory(lowerText, "名家", "经典", "诺贝尔", "代表作", "文集", "选集", "大师")) {
            return "名家代表";
        }
        if (matchesBookCategory(lowerText, "小说", "散文", "诗", "文学", "长篇", "短篇", "故事")) {
            return "文学";
        }
        return "精品书籍";
    }

    /**
     * 在候选文本中检测分类关键词，尽量用轻量规则给导入书籍落一个可用分类。
     * 命中任意关键词就返回真，供分类识别按优先级逐层判断。
     */
    private boolean matchesBookCategory(String sourceText, String... keywords) {
        String text = defaultString(sourceText, "");
        for (String keyword : keywords) {
            if (!defaultString(keyword, "").isBlank() && text.contains(keyword.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    /**
     * 从自然文本中识别标题、作者和译者。
     * 兼容“作者著译者译”“作者 著 / 译者 译”和常见中英标点分隔写法。
     */
    private BookCreditInfo parseCreditInfoFromText(String rawText) {
        String text = defaultString(rawText, "").replaceAll("\\s+", " ").trim();
        String author = matchFirst(text, List.of(
            "(?:作者|原著|著者)[:： ]+([^\\n,，;；/|]+)",
            "(?:[\\[【(（][^\\]】)）]*[\\]】)）]\\s*)?([\\p{IsHan}A-Za-z·.\\- ]{2,50})\\s*(?:著|著作|作)"
        ));
        String translator = matchFirst(text, List.of(
            "(?:译者|翻译)[:： ]+([^\\n,，;；/|]+)",
            "(?:著|著作|作)\\s*([^\\n,，;；/|\\[\\]【】()（）]{2,40})\\s*(?:译|翻译)",
            "(?:^|[,，;；/|])\\s*([\\p{IsHan}A-Za-z·.\\- ]{2,40})\\s*(?:译|翻译)"
        ));
        String title = cleanupCreditText(text);
        return new BookCreditInfo(title, normalizeCreditName(author), normalizeCreditName(translator));
    }

    /**
     * 清理标题中的作者译者尾缀。
     * 去掉括号内国别和“著/译”等署名信息，保留真正书名供后台展示。
     */
    private String cleanupCreditText(String rawTitle) {
        String title = defaultString(rawTitle, "").trim();
        title = title.replaceAll("\\[[^\\]]*\\]", " ");
        title = title.replaceAll("【[^】]*】", " ");
        title = title.replaceAll("（[^）]*）", " ");
        title = title.replaceAll("\\([^)]*\\)", " ");
        title = title.replaceAll("[\\p{IsHan}A-Za-z·.\\- ]{2,50}\\s*(?:著|著作|作)", " ");
        title = title.replaceAll("[\\p{IsHan}A-Za-z·.\\- ]{2,40}\\s*(?:译|翻译)", " ");
        title = title.replaceAll("\\s+", " ").trim();
        return title.isBlank() ? rawTitle : title;
    }

    /**
     * 按多个正则模式提取第一个有效分组。
     * 每个候选值都会经过署名清洗，避免把角色词和国别标记带入字段。
     */
    private String matchFirst(String text, List<String> patterns) {
        for (String pattern : patterns) {
            var matcher = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE).matcher(defaultString(text, ""));
            if (matcher.find()) {
                return normalizeCreditName(matcher.group(1));
            }
        }
        return "";
    }

    /**
     * 规范化作者或译者名称。
     * 移除国别、角色词和多余标点，只保留可展示的人名文本。
     */
    private String normalizeCreditName(String rawName) {
        String name = defaultString(rawName, "").trim();
        name = name.replaceAll("\\[[^\\]]*\\]", "");
        name = name.replaceAll("【[^】]*】", "");
        name = name.replaceAll("（[^）]*）", "");
        name = name.replaceAll("\\([^)]*\\)", "");
        name = name.replaceAll("(作者|原著|著者|译者|翻译|著作|著|作|译)", "");
        name = name.replaceAll("^[：:、,，;；/|\\s]+|[：:、,，;；/|\\s]+$", "");
        return name.trim();
    }

    /**
     * 从 EPUB 元数据读取作者列表。
     * 优先使用 authors 字段，避免 contributor 中译者等其他角色污染作者。
     */
    private String extractAuthors(Metadata metadata) {
        if (metadata == null || metadata.getAuthors() == null) {
            return "";
        }
        return joinAuthors(metadata.getAuthors());
    }

    /**
     * 从 EPUB 元数据读取译者列表。
     * 优先识别 contributor 的 TRANSLATOR 角色，兼容少量把译者写入作者文本的文件。
     */
    private String extractTranslators(Metadata metadata) {
        if (metadata == null || metadata.getContributors() == null) {
            return "";
        }
        List<Author> translators = metadata.getContributors().stream()
            .filter(author -> author.getRelator() == Relator.TRANSLATOR)
            .toList();
        return joinAuthors(translators);
    }

    /**
     * 拼接 EPUB Author 对象列表。
     * 统一调用 toString 并过滤空值，保证中英文姓名都能原样展示。
     */
    private String joinAuthors(List<Author> authors) {
        return authors.stream()
            .map(author -> normalizeCreditName(author == null ? "" : author.toString()))
            .filter(name -> !name.isBlank())
            .distinct()
            .reduce((left, right) -> left + "、" + right)
            .orElse("");
    }

    /**
     * 读取字符串列表中的第一个非空值。
     * 用于 EPUB 出版社和简介等可选元数据的兜底转换。
     */
    private String firstString(List<String> values) {
        if (values == null) {
            return "";
        }
        return values.stream().filter(value -> value != null && !value.isBlank()).findFirst().orElse("");
    }

    private String cleanImportedHtml(String html) {
        return defaultString(html, "")
            .replace("<script", "<!-- script")
            .replace("</script>", "</script -->")
            .replace("<style", "<!-- style")
            .replace("</style>", "</style -->")
            .trim();
    }

    /**
     * 统一补齐 EPUB 章节里的图片资源地址，确保封面页和插图页发布后可直接显示。
     * 遍历 img 与 svg image 节点回填上传地址，并清理空容器与脚本样式残留。
     */
    private String normalizeEpubChapterHtml(
        nl.siegmann.epublib.domain.Book epubBook,
        Resource chapterResource,
        String htmlContent,
        String jobKey
    ) throws Exception {
        Document document = Jsoup.parse(defaultString(htmlContent, ""));
        document.outputSettings().prettyPrint(false);
        document.select("script, style, link[rel=stylesheet], meta, noscript").remove();

        for (Element image : document.select("img, image")) {
            String uploadedUrl = "";
            for (String rawPath : extractImageSourceCandidates(image)) {
                uploadedUrl = uploadEpubEmbeddedResource(epubBook, chapterResource, rawPath, jobKey);
                if (!uploadedUrl.isBlank()) {
                    break;
                }
            }
            if (!uploadedUrl.isBlank()) {
                if ("img".equalsIgnoreCase(image.tagName())) {
                    image.attr(resolvePrimaryImageAttribute(image), uploadedUrl);
                    image.attr("loading", "lazy");
                    image.removeAttr("data-src");
                    image.removeAttr("data-original");
                    image.removeAttr("data-lazy-src");
                    image.removeAttr("srcset");
                } else {
                    replaceEpubSvgImageWithHtmlImage(image, uploadedUrl);
                }
            }
        }

        document.select("body, section, div, article, main").forEach(element -> {
            if (!element.select("img, svg, figure, p, h1, h2, h3, h4, h5, h6, blockquote, ul, ol, table, pre").isEmpty()) {
                return;
            }
            if (defaultString(element.text(), "").isBlank()) {
                element.remove();
            }
        });

        String bodyHtml = document.body().html().trim();
        if (!bodyHtml.isBlank()) {
            return bodyHtml;
        }
        return document.html().trim();
    }

    /**
     * 将 EPUB 封面页里的 SVG image 节点转成普通 img。
     * 保留图片内容并移除 SVG 画布包装，避免审核页和阅读页出现空白大画布。
     */
    private void replaceEpubSvgImageWithHtmlImage(Element image, String uploadedUrl) {
        Element htmlImage = new Element("img");
        htmlImage.attr("src", uploadedUrl);
        htmlImage.attr("loading", "lazy");
        htmlImage.attr("alt", defaultString(image.attr("alt"), "book image"));

        Element svg = image.parents().stream()
            .filter(parent -> "svg".equalsIgnoreCase(parent.tagName()))
            .findFirst()
            .orElse(null);
        if (svg != null) {
            svg.replaceWith(htmlImage);
            return;
        }
        image.replaceWith(htmlImage);
    }

    /**
     * 将 EPUB 章节内引用的相对资源路径解析为对象存储地址，避免读者端出现封面或插图破图。
     * 优先跳过外链和 data URL，仅处理书内资源，并按章节路径推导相对图片的真实位置。
     */
    private String uploadEpubEmbeddedResource(
        nl.siegmann.epublib.domain.Book epubBook,
        Resource chapterResource,
        String rawPath,
        String jobKey
    ) throws Exception {
        String sourcePath = defaultString(rawPath, "").trim();
        if (sourcePath.isBlank()) {
            return "";
        }
        String lowerPath = sourcePath.toLowerCase(Locale.ROOT);
        if (lowerPath.startsWith("http://") || lowerPath.startsWith("https://") || lowerPath.startsWith("data:")) {
            return sourcePath;
        }

        Resource embeddedResource = resolveEpubResource(epubBook, chapterResource, sourcePath);
        if (embeddedResource == null) {
            return "";
        }
        byte[] bytes = embeddedResource.getData();
        if (bytes == null || bytes.length == 0) {
            return "";
        }

        String resourceName = defaultString(embeddedResource.getHref(), sourcePath);
        String fileName = resourceName.contains("/") ? resourceName.substring(resourceName.lastIndexOf('/') + 1) : resourceName;
        if (fileName.isBlank()) {
            fileName = "epub-image-" + shortId();
        }
        String contentType = embeddedResource.getMediaType() == null
            ? guessContentType(fileName)
            : defaultString(embeddedResource.getMediaType().getName(), guessContentType(fileName));

        return storageRoutingService.resolveForModule("book").upload(
            "book/content/" + defaultString(jobKey, "epub"),
            fileName,
            new ByteArrayInputStream(bytes),
            bytes.length,
            contentType
        );
    }

    /**
     * 解析 EPUB 章节中的资源引用路径，兼容相对路径、根路径和常见 URL 编码写法。
     * 先按章节所在目录回溯真实资源，再逐步回退到 href 原值和去查询串路径。
     */
    private Resource resolveEpubResource(
        nl.siegmann.epublib.domain.Book epubBook,
        Resource chapterResource,
        String rawPath
    ) {
        if (epubBook == null || epubBook.getResources() == null) {
            return null;
        }
        String normalizedPath = normalizeEpubHref(rawPath);
        List<String> candidates = new ArrayList<>();
        candidates.add(normalizedPath);

        String chapterHref = chapterResource == null ? "" : normalizeEpubHref(chapterResource.getHref());
        String rawRelativePath = defaultString(rawPath, "").trim().replace("\\", "/");
        if (!chapterHref.isBlank() && !rawRelativePath.startsWith("/") && !rawRelativePath.isBlank()) {
            int lastSlash = chapterHref.lastIndexOf('/');
            String baseDir = lastSlash >= 0 ? chapterHref.substring(0, lastSlash + 1) : "";
            candidates.add(normalizeEpubHref(baseDir + rawRelativePath));
        }
        if (!chapterHref.isBlank() && !normalizedPath.startsWith("/")) {
            int lastSlash = chapterHref.lastIndexOf('/');
            String baseDir = lastSlash >= 0 ? chapterHref.substring(0, lastSlash + 1) : "";
            candidates.add(normalizeEpubHref(baseDir + normalizedPath));
        }
        if (normalizedPath.startsWith("/")) {
            candidates.add(normalizeEpubHref(normalizedPath.substring(1)));
        }

        for (String candidate : candidates) {
            Resource resource = findEpubResourceByHref(epubBook, candidate);
            if (resource != null) {
                return resource;
            }
        }
        Resource fallback = findEpubResourceByFileName(epubBook, normalizedPath);
        if (fallback != null) {
            return fallback;
        }
        return null;
    }

    /**
     * 在 EPUB 资源清单中按 href 查找目标资源，兼容前导斜杠与 URL 编码差异。
     * 遍历资源列表做宽松比对，避免不同打包器输出的 href 形式不一致导致图片漏传。
     */
    private Resource findEpubResourceByHref(nl.siegmann.epublib.domain.Book epubBook, String href) {
        String normalizedHref = normalizeEpubHref(href);
        if (normalizedHref.isBlank()) {
            return null;
        }
        for (Resource resource : epubBook.getResources().getAll()) {
            String resourceHref = normalizeEpubHref(resource.getHref());
            if (normalizedHref.equals(resourceHref)
                || resourceHref.endsWith("/" + normalizedHref)
                || normalizedHref.endsWith("/" + resourceHref)) {
                return resource;
            }
        }
        return null;
    }

    /**
     * 在 EPUB 资源清单中按文件名兜底匹配目标图片资源。
     * 当章节只引用 cover.jpeg 这类无路径文件名时，按评分选出最像封面或插图的候选资源。
     */
    private Resource findEpubResourceByFileName(nl.siegmann.epublib.domain.Book epubBook, String rawPath) {
        if (epubBook == null || epubBook.getResources() == null) {
            return null;
        }
        String normalizedPath = normalizeEpubHref(rawPath);
        if (normalizedPath.isBlank()) {
            return null;
        }
        String fileName = normalizedPath.contains("/") ? normalizedPath.substring(normalizedPath.lastIndexOf('/') + 1) : normalizedPath;
        if (fileName.isBlank() || fileName.contains("..")) {
            return null;
        }
        String targetName = fileName.toLowerCase(Locale.ROOT);

        Resource best = null;
        byte[] bestBytes = null;
        int bestScore = Integer.MIN_VALUE;

        for (Resource resource : epubBook.getResources().getAll()) {
            if (resource == null) {
                continue;
            }
            String href = normalizeEpubHref(resource.getHref());
            if (href.isBlank()) {
                continue;
            }
            String candidateName = href.contains("/") ? href.substring(href.lastIndexOf('/') + 1) : href;
            if (!candidateName.toLowerCase(Locale.ROOT).equals(targetName)) {
                continue;
            }
            byte[] bytes;
            try {
                bytes = resource.getData();
            } catch (Exception ignored) {
                continue;
            }
            if (bytes == null || bytes.length == 0) {
                continue;
            }
            int score = scoreCoverPath(href, bytes);
            if (best == null || score > bestScore) {
                best = resource;
                bestBytes = bytes;
                bestScore = score;
            }
        }
        return best;
    }

    /**
     * 标准化 EPUB 资源路径，消除 ../、./、查询串和 URL 编码带来的路径差异。
     * 统一转为正斜杠路径，方便章节图片与封面资源做稳定匹配。
     */
    private String normalizeEpubHref(String href) {
        String normalized = defaultString(href, "").trim().replace("\\", "/");
        if (normalized.isBlank()) {
            return "";
        }
        int queryIndex = normalized.indexOf('?');
        if (queryIndex >= 0) {
            normalized = normalized.substring(0, queryIndex);
        }
        int fragmentIndex = normalized.indexOf('#');
        if (fragmentIndex >= 0) {
            normalized = normalized.substring(0, fragmentIndex);
        }
        normalized = normalized.replace("%20", " ");
        while (normalized.startsWith("./")) {
            normalized = normalized.substring(2);
        }
        while (normalized.contains("/./")) {
            normalized = normalized.replace("/./", "/");
        }
        while (normalized.contains("//")) {
            normalized = normalized.replace("//", "/");
        }
        List<String> segments = new ArrayList<>();
        for (String segment : normalized.split("/")) {
            if (segment.isBlank() || ".".equals(segment)) {
                continue;
            }
            if ("..".equals(segment)) {
                if (!segments.isEmpty()) {
                    segments.remove(segments.size() - 1);
                }
                continue;
            }
            segments.add(segment);
        }
        return String.join("/", segments);
    }

    private String extractTitleFromHtml(String html, String fallback) {
        var matcher = java.util.regex.Pattern.compile("<h[1-3][^>]*>([^<]+)</h[1-3]>", java.util.regex.Pattern.CASE_INSENSITIVE).matcher(html);
        if (matcher.find()) {
            String title = matcher.group(1).trim();
            return title.length() > 100 ? title.substring(0, 100) : title;
        }
        return fallback;
    }

    private String extractTitleFromFileName(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        String baseName = dotIndex > 0 ? fileName.substring(0, dotIndex) : fileName;
        int lastSlash = Math.max(baseName.lastIndexOf('/'), baseName.lastIndexOf('\\'));
        if (lastSlash >= 0) {
            baseName = baseName.substring(lastSlash + 1);
        }
        return baseName.isBlank() ? "导入书籍" : baseName;
    }

    /**
     * 汇总书籍主记录、章节正文和导入源文件中的对象存储地址。
     * 删除正式书籍前统一抽取可回收资源，避免 OSS、MinIO 与本地上传目录残留孤儿文件。
     */
    private Set<String> collectBookStorageUrls(Book book, List<BookImportJob> importJobs, List<BookChapter> chapters) {
        Set<String> storageUrls = new LinkedHashSet<>();
        addStorageUrl(storageUrls, book == null ? "" : book.getCoverUrl());

        for (BookImportJob job : importJobs) {
            addStorageUrl(storageUrls, job.getCoverUrl());
            BookSourceFile sourceFile = adminBookMapper.findSourceFileByKey(job.getSourceFileKey());
            if (sourceFile != null) {
                addStorageUrl(storageUrls, sourceFile.getStorageUrl());
            }
        }

        for (BookChapter chapter : chapters) {
            storageUrls.addAll(extractStorageUrlsFromHtml(chapter.getContentHtml()));
        }
        return storageUrls;
    }

    /**
     * 删除书籍关联的对象存储资源。
     * 单个文件删除失败仅记录日志，避免已完成的数据库删除因某个对象异常而整体回滚。
     */
    private void deleteStorageResources(Set<String> storageUrls, String bookKey) {
        for (String storageUrl : storageUrls) {
            try {
                contentStorageService.delete(storageUrl);
            } catch (Exception exception) {
                log.warn("删除书籍资源失败(bookKey={}, url={}): {}", bookKey, storageUrl, exception.getMessage());
            }
        }
    }

    /**
     * 从章节 HTML 中提取本站对象存储图片资源地址。
     * 仅回收由本站存储服务托管的 img/image 链接，避免误删外部图片或 data URL。
     */
    private Set<String> extractStorageUrlsFromHtml(String contentHtml) {
        Set<String> storageUrls = new LinkedHashSet<>();
        if (contentHtml == null || contentHtml.isBlank()) {
            return storageUrls;
        }
        Document document = Jsoup.parseBodyFragment(contentHtml);
        for (Element image : document.select("img[src], image[href], image[xlink\\:href]")) {
            String attributeName = image.hasAttr("src") ? "src" : image.hasAttr("href") ? "href" : "xlink:href";
            addStorageUrl(storageUrls, image.attr(attributeName));
        }
        return storageUrls;
    }

    /**
     * 向待删除集合中追加本站对象存储地址。
     * 统一过滤空值、data URL 与外部资源，只保留当前存储实现可识别的对象链接。
     */
    private void addStorageUrl(Set<String> storageUrls, String candidateUrl) {
        String normalizedUrl = defaultString(candidateUrl, "").trim();
        if (normalizedUrl.isBlank() || normalizedUrl.toLowerCase(Locale.ROOT).startsWith("data:")) {
            return;
        }
        if (isManagedStorageUrl(normalizedUrl)) {
            storageUrls.add(normalizedUrl);
        }
    }

    private BookImportJob requireImportJob(String jobKey) {
        BookImportJob job = adminBookMapper.findImportJobByKey(jobKey);
        if (job == null) {
            throw new BusinessException(404, "书籍导入任务不存在");
        }
        return job;
    }

    private BookImportChapterStage requireImportStage(String jobKey, String tempChapterKey) {
        BookImportChapterStage stage = adminBookMapper.findImportStageByKey(jobKey, tempChapterKey);
        if (stage == null) {
            throw new BusinessException(404, "导入章节不存在");
        }
        return stage;
    }

    /**
     * 读取指定业务主键对应的正式书籍。
     * 找不到记录时抛出统一业务异常，供总览页编辑和删除操作复用。
     */
    private Book requireBook(String bookKey) {
        Book book = adminBookMapper.findBookByKey(bookKey);
        if (book == null) {
            throw new BusinessException(404, "书籍不存在");
        }
        return book;
    }

    /**
     * 统一处理导入任务审核状态批量变更。
     * 仅允许指定来源状态进入目标状态，异常任务记录为失败并继续处理。
     */
    private AdminBookImportBatchResponse updateImportJobReviewStatus(
        AdminBookImportBatchRequest request,
        String targetStatus,
        String message,
        List<String> allowedStatuses
    ) {
        AdminBookImportBatchResponse response = new AdminBookImportBatchResponse();
        for (String jobKey : normalizeJobKeys(request)) {
            try {
                BookImportJob job = requireImportJob(jobKey);
                if (!allowedStatuses.contains(job.getStatus())) {
                    throw new BusinessException(400, "当前任务状态不可批量处理");
                }
                job.setStatus(targetStatus);
                job.setMessage(message);
                adminBookMapper.updateImportJob(job);
                response.getJobs().add(toImportJobResponse(job));
                response.setSuccessCount(response.getSuccessCount() + 1);
            } catch (BusinessException exception) {
                response.getFailedKeys().add(jobKey);
            }
        }
        response.setFailedCount(response.getFailedKeys().size());
        return response;
    }

    /**
     * 清洗批量请求中的任务主键。
     * 去除空值和重复值，避免同一导入任务在一次请求中被重复处理。
     */
    private List<String> normalizeJobKeys(AdminBookImportBatchRequest request) {
        return request.getJobKeys().stream()
            .map(key -> defaultString(key, ""))
            .filter(key -> !key.isBlank())
            .distinct()
            .toList();
    }

    /**
     * 校验导入任务是否允许发布。
     * 只有待审核或已通过任务可进入发布流程，已拒绝和已发布任务直接阻断。
     */
    private void assertPublishableJob(BookImportJob job) {
        if (!List.of(STATUS_AWAIT_REVIEW, STATUS_APPROVED).contains(job.getStatus())) {
            throw new BusinessException(400, "当前任务状态不可发布");
        }
    }

    /**
     * 解析已删除任务应恢复到的原始状态。
     * 删除标记缺失或异常时回退为待审核，避免恢复后仍停留在删除态。
     */
    private String resolveRestoredStatus(String deleteMessage) {
        String prefix = "DELETE_PREVIOUS_STATUS=";
        String normalizedStatus = STATUS_AWAIT_REVIEW;
        String message = defaultString(deleteMessage, "");
        if (message.startsWith(prefix) && message.contains(";")) {
            normalizedStatus = message.substring(prefix.length(), message.indexOf(';')).trim();
        }
        return STATUS_DELETED.equals(normalizedStatus) ? STATUS_AWAIT_REVIEW : normalizedStatus;
    }

    /**
     * 生成已删除任务的状态保留说明。
     * 复用现有 message 字段暂存删除前状态，避免旧数据库缺少新增列时写入失败。
     */
    private String buildDeleteMessage(String previousStatus) {
        return "DELETE_PREVIOUS_STATUS=" + defaultString(previousStatus, STATUS_AWAIT_REVIEW) + ";书籍已移入已删除列表，可从导入中心恢复原始状态";
    }

    /**
     * 解析导入任务返回给前端的删除前状态字段。
     * 已删除任务从 message 中提取原状态，其余任务直接返回当前状态以便页面统一展示。
     */
    private String resolvePreviousStatusForResponse(BookImportJob job) {
        if (job == null) {
            return STATUS_AWAIT_REVIEW;
        }
        if (STATUS_DELETED.equals(job.getStatus())) {
            return resolveRestoredStatus(job.getMessage());
        }
        return defaultString(job.getStatus(), STATUS_AWAIT_REVIEW);
    }

    private AdminBookImportJobResponse toImportJobResponse(BookImportJob job) {
        AdminBookImportJobResponse response = new AdminBookImportJobResponse();
        response.setJobKey(job.getJobKey());
        response.setPreviousStatus(resolvePreviousStatusForResponse(job));
        response.setBookKey(job.getBookKey());
        response.setTitle(job.getTitle());
        response.setAuthor(job.getAuthor());
        response.setTranslator(job.getTranslator());
        response.setPublisher(job.getPublisher());
        response.setSummary(job.getSummary());
        response.setCategory(job.getCategory());
        response.setCoverUrl(normalizeCoverUrlForDisplay(job.getCoverUrl()));
        response.setImportType(job.getImportType());
        response.setSourceUrl(job.getSourceUrl());
        response.setOriginalFormat(job.getOriginalFormat());
        response.setFileSize(job.getFileSize());
        response.setStatus(job.getStatus());
        response.setProgress(defaultInt(job.getProgress()));
        response.setTotalChapters(defaultInt(job.getTotalChapters()));
        response.setSuccessChapters(defaultInt(job.getSuccessChapters()));
        response.setFailChapters(defaultInt(job.getFailChapters()));
        response.setMessage(job.getMessage());
        response.setCreatedAt(formatDateTime(job.getCreatedAt()));
        response.setUpdatedAt(formatDateTime(job.getUpdatedAt()));
        return response;
    }

    private AdminBookImportChapterResponse toImportChapterResponse(BookImportChapterStage stage, String coverUrlFallback) {
        AdminBookImportChapterResponse response = new AdminBookImportChapterResponse();
        response.setTempChapterKey(stage.getTempChapterKey());
        response.setChapterNo(defaultInt(stage.getChapterNo()));
        response.setTitle(stage.getTitle());
        response.setSubtitle(stage.getSubtitle());
        response.setSourcePath(stage.getSourcePath());
        response.setContentHtml(normalizeImportedChapterHtmlForDisplay(stage.getContentHtml(), coverUrlFallback));
        response.setPlainText(stage.getPlainText());
        response.setWordCount(defaultInt(stage.getWordCount()));
        response.setReviewStatus(stage.getReviewStatus());
        response.setSortOrder(defaultInt(stage.getSortOrder()));
        response.setWarnings(parseWarnings(stage.getWarningJson()));
        return response;
    }

    private AdminBookImportChapterResponse toImportChapterResponse(BookImportChapterStage stage) {
        return toImportChapterResponse(stage, "");
    }

    private BookResponse toBookResponse(Book book) {
        BookResponse response = new BookResponse();
        response.setId(book.getBookKey());
        response.setTitle(book.getTitle());
        response.setAuthor(book.getAuthor());
        response.setTranslator(book.getTranslator());
        response.setPublisher(book.getPublisher());
        response.setCategory(book.getCategory());
        response.setSummary(book.getSummary());
        response.setCoverUrl(resolveAdminCoverUrl(book.getCoverUrl()));
        response.setTags(splitPipeValues(book.getTagList()));
        response.setWordCount(defaultInt(book.getWordCount()));
        response.setChapterCount(defaultInt(book.getChapterCount()));
        response.setReadCount(defaultInt(book.getReadCount()));
        response.setRating(book.getRating());
        response.setPublishedAt(formatDateTime(book.getPublishedAt()));
        return response;
    }

    private String resolveAdminCoverUrl(String coverUrl) {
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
        return trimmed;
    }

    private List<String> parseWarnings(String warningJson) {
        if (warningJson == null || warningJson.isBlank()) return List.of();
        try {
            return OBJECT_MAPPER.readValue(warningJson, new TypeReference<List<String>>() {});
        } catch (Exception exception) {
            return List.of();
        }
    }

    private List<String> defaultWarnings(List<String> warnings) {
        return warnings == null ? List.of() : warnings;
    }

    private String resolveSourceType(String sourcePath) {
        String extension = resolveExtension(sourcePath);
        return extension.isBlank() ? "txt" : extension;
    }

    private String resolveExtension(String fileName) {
        String normalized = defaultString(fileName, "").toLowerCase(Locale.ROOT);
        int index = normalized.lastIndexOf('.');
        return index < 0 ? "" : normalized.substring(index + 1);
    }

    private String extractTitle(String plainText, String sourcePath) {
        String[] lines = defaultString(plainText, "").split("\\R");
        for (String line : lines) {
            String trimmed = line.trim();
            if (!trimmed.isEmpty()) {
                return trimmed.length() > 80 ? trimmed.substring(0, 80) : trimmed;
            }
        }
        String fileName = sourcePath.contains("/") ? sourcePath.substring(sourcePath.lastIndexOf('/') + 1) : sourcePath;
        int dotIndex = fileName.lastIndexOf('.');
        return dotIndex > 0 ? fileName.substring(0, dotIndex) : fileName;
    }

    private String buildSlugKey(String title) {
        String slug = defaultString(title, "book")
            .trim()
            .toLowerCase(Locale.ROOT)
            .replaceAll("[^a-z0-9\\u4e00-\\u9fa5]+", "-")
            .replaceAll("(^-|-$)", "");
        return "book-" + (slug.isBlank() ? shortId() : slug);
    }

    private int countWords(String text) {
        return defaultString(text, "").replaceAll("\\s+", "").length();
    }

    private String stripHtml(String html) {
        return defaultString(html, "").replaceAll("<[^>]+>", " ").replaceAll("\\s+", " ").trim();
    }

    private String guessContentType(String fileName) {
        String extension = resolveExtension(fileName);
        return switch (extension) {
            case "png" -> "image/png";
            case "jpg", "jpeg" -> "image/jpeg";
            case "webp" -> "image/webp";
            case "gif" -> "image/gif";
            case "bmp" -> "image/bmp";
            case "svg" -> "image/svg+xml";
            default -> "application/octet-stream";
        };
    }

    private String sha256(byte[] bytes) throws Exception {
        MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
        byte[] digest = messageDigest.digest(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
    }

    private String shortId() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }

    private String asText(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    private Integer asInteger(Object value) {
        if (value == null) return 0;
        try {
            return Integer.parseInt(String.valueOf(value).trim());
        } catch (Exception exception) {
            return 0;
        }
    }

    private String formatDateTime(LocalDateTime dateTime) {
        return dateTime == null ? null : dateTime.format(DATE_TIME_FORMATTER);
    }

    private String defaultString(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }

    private List<String> splitPipeValues(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) return List.of();
        return Arrays.stream(rawValue.split("\\|")).map(String::trim).filter(item -> !item.isEmpty()).toList();
    }

    private String requireText(String value, String errorMessage) {
        if (value == null || value.isBlank()) throw new BusinessException(400, errorMessage);
        return value.trim();
    }

    /**
     * 规范化返回前端和发布入库的封面地址。
     * 仅保留可渲染的图片链接，过滤误写入封面字段的 HTML、OPF 或普通文本。
     */
    private String normalizeCoverUrlForDisplay(String coverUrl) {
        String normalizedUrl = defaultString(coverUrl, "").trim();
        if (normalizedUrl.isBlank() || Pattern.compile("[\\r\\n<>\"']").matcher(normalizedUrl).find()) {
            return "";
        }
        if (normalizedUrl.startsWith("/uploads/")) {
            return normalizedUrl;
        }
        String lowerUrl = normalizedUrl.toLowerCase(Locale.ROOT);
        if (lowerUrl.startsWith("data:image/") || normalizedUrl.startsWith("/")) {
            return normalizedUrl;
        }
        if (lowerUrl.startsWith("http://") || lowerUrl.startsWith("https://")) {
            if (isManagedStorageUrl(normalizedUrl)) {
                int uploadsIndex = normalizedUrl.indexOf("/uploads/");
                if (uploadsIndex >= 0) {
                    return normalizedUrl.substring(uploadsIndex);
                }
            }
            return normalizedUrl;
        }
        return "";
    }

    /**
     * 统一修正导入审核页章节 HTML 中的图片资源地址，确保审核端预览与发布结果一致。
     * 逐个改写本站存储链接，并把历史遗留的封面占位图回填为书籍封面地址。
     */
    private String normalizeImportedChapterHtmlForDisplay(String contentHtml, String coverUrlFallback) {
        if (contentHtml == null || contentHtml.isBlank()) {
            return contentHtml;
        }
        Document document = Jsoup.parseBodyFragment(contentHtml);
        for (Element image : document.select("img[src], image[href], image[xlink\\:href]")) {
            String attributeName = image.hasAttr("src") ? "src" : image.hasAttr("href") ? "href" : "xlink:href";
            String rawUrl = defaultString(image.attr(attributeName), "").trim();
            String normalizedUrl = normalizeStoredAssetUrlForDisplay(rawUrl);
            if (normalizedUrl.equals(rawUrl)) {
                String coverFallback = resolveCoverUrlFallbackForImportPreview(image, rawUrl, coverUrlFallback);
                if (!coverFallback.isBlank()) {
                    normalizedUrl = coverFallback;
                }
            }
            if (!normalizedUrl.equals(image.attr(attributeName))) {
                image.attr(attributeName, normalizedUrl);
            }
        }
        return document.body().html();
    }

    /**
     * 将导入章节里遗留的封面占位图引用替换成任务封面地址，避免审核与发布时出现破图。
     * 识别相对路径或裸文件名中的封面提示词，仅对明显的封面占位图执行回填。
     */
    private String resolveCoverUrlFallbackForImportPreview(Element image, String rawUrl, String coverUrlFallback) {
        String normalized = defaultString(rawUrl, "").trim();
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
        String cover = defaultString(coverUrlFallback, "").trim();
        return cover.isBlank() ? "" : cover;
    }

    /**
     * 识别章节里仍指向封面占位资源的图片节点，避免把正文插图误替换成书籍封面。
     * 结合文件名、提示词和 svg 封面结构综合判断，只匹配明显的 EPUB 封面引用。
     */
    private boolean isCoverPlaceholderImage(Element image, String rawUrl) {
        String normalized = defaultString(rawUrl, "").trim().replace("\\", "/");
        if (normalized.isBlank()) {
            return false;
        }
        String fileName = normalized.contains("/") ? normalized.substring(normalized.lastIndexOf('/') + 1) : normalized;
        String hint = (
            normalized + " "
                + fileName + " "
                + defaultString(image == null ? "" : image.attr("alt"), "") + " "
                + defaultString(image == null ? "" : image.attr("title"), "")
        ).toLowerCase(Locale.ROOT);
        if (hint.matches(".*(cover|front|title[-_ ]?page|calibre[_-]?cover|fm|titlepage).*")) {
            return true;
        }
        return image != null
            && "image".equalsIgnoreCase(image.tagName())
            && image.parents().stream().anyMatch(parent -> "svg".equalsIgnoreCase(parent.tagName()))
            && hint.matches(".*(cover|front|title|calibre).*");
    }

    /**
     * 统一把本站对象存储直链折叠成代理地址，避免后台审核页直接访问私有桶导致图片破损。
     * 保留 data URL、外部链接和 OSS 直链，仅规范历史 /uploads 代理地址。
     */
    private String normalizeStoredAssetUrlForDisplay(String assetUrl) {
        String normalizedUrl = defaultString(assetUrl, "").trim();
        if (normalizedUrl.isBlank() || normalizedUrl.startsWith("/uploads/")) {
            return normalizedUrl;
        }
        String lowerUrl = normalizedUrl.toLowerCase(Locale.ROOT);
        if (lowerUrl.startsWith("data:")) {
            return normalizedUrl;
        }
        if (isManagedStorageUrl(normalizedUrl) && (lowerUrl.startsWith("http://") || lowerUrl.startsWith("https://"))) {
            int uploadsIndex = normalizedUrl.indexOf("/uploads/");
            if (uploadsIndex >= 0) {
                return normalizedUrl.substring(uploadsIndex);
            }
        }
        return normalizedUrl;
    }

    /**
     * 统一判断书籍模块资源链接是否属于本站托管的对象存储。
     * 同时识别书籍走 OSS 与历史代理资源的场景，避免导入审核页和发布链路误判资源来源。
     */
    private boolean isManagedStorageUrl(String assetUrl) {
        if (assetUrl == null || assetUrl.isBlank()) {
            return false;
        }
        return storageRoutingService.resolveForModule("book").isStorageUrl(assetUrl)
            || storageRoutingService.resolveForModule("interview").isStorageUrl(assetUrl)
            || contentStorageService.isStorageUrl(assetUrl);
    }

    private int defaultInt(Integer value) {
        return value == null ? 0 : value;
    }

    private String escapeHtml(String value) {
        return defaultString(value, "")
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;");
    }

    private record ParsedBookPackage(
        String bookKey, String title, String author, String translator, String publisher,
        String category, String summary, String coverUrl,
        List<BookImportChapterStage> stages
    ) {}

    /**
     * 保存自动识别出的书名、作者和译者署名。
     * 在导入流程中作为元数据兜底结果传递，避免各格式解析分支重复拆分文本。
     */
    private record BookCreditInfo(String title, String author, String translator) {}
}

