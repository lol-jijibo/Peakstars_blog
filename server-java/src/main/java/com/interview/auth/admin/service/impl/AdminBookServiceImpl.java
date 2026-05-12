package com.interview.auth.admin.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.interview.auth.admin.dto.request.AdminBookImportChapterUpdateRequest;
import com.interview.auth.admin.dto.request.AdminBookImportExternalRequest;
import com.interview.auth.admin.dto.request.AdminContentImportPreviewRequest;
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
import com.interview.auth.domain.dto.response.BookResponse;
import com.interview.auth.domain.entity.Book;
import com.interview.auth.domain.entity.BookChapter;
import com.interview.auth.infrastructure.storage.ContentStorageService;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.net.URI;
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
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import lombok.RequiredArgsConstructor;
import nl.siegmann.epublib.domain.Spine;
import nl.siegmann.epublib.domain.SpineReference;
import nl.siegmann.epublib.epub.EpubReader;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 集中承接后台书籍导入、章节审核和发布流程。
 * 复用现有富文本清洗与资源迁移能力，支持 ZIP/EPUB/PDF/TXT/MD/DOCX/HTML 多格式导入。
 */
@Service
@RequiredArgsConstructor
public class AdminBookServiceImpl implements AdminBookService {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final String CONTENT_TYPE_BOOK = "book";
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

    private final AdminBookMapper adminBookMapper;
    private final AdminContentImportService adminContentImportService;
    private final ContentStorageService contentStorageService;

    @Override
    @Transactional
    public AdminBookImportJobResponse createImportJob(String fileName, InputStream inputStream, long size, String contentType) throws Exception {
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
        String storageUrl = contentStorageService.upload(
            "book/source", normalizedName,
            new ByteArrayInputStream(zipBytes), zipBytes.length,
            defaultString(contentType, "application/zip")
        );

        BookSourceFile sourceFile = buildSourceFile(fileKey, normalizedName, storageUrl, size > 0 ? size : zipBytes.length, fileHash);
        adminBookMapper.saveSourceFile(sourceFile);

        BookImportJob job = createInitialJob(normalizedName, fileKey, "zip", "zip", zipBytes.length);
        adminBookMapper.saveImportJob(job);

        ParsedBookPackage parsedBook = parseBookPackage(zipBytes, job.getJobKey());
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
        String storageUrl = contentStorageService.upload(
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

        List<BookImportChapterStage> stages = parseSingleFile(extension, fileBytes, job.getJobKey());

        String title = extractTitleFromFileName(normalizedName);
        job.setTitle(title);
        job.setTotalChapters(stages.size());
        job.setSuccessChapters(stages.size());
        job.setFailChapters(0);
        job.setProgress(80);
        job.setStatus("await_review");
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
        job.setPublisher(defaultString(request.getPublisher(), ""));
        job.setSummary(defaultString(request.getSummary(), ""));
        job.setCategory(defaultString(request.getCategory(), "书籍"));
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
            List<BookImportChapterStage> stages = parseSingleFile(detectedFormat, content, job.getJobKey());

            job.setTitle(defaultString(request.getTitle(), extractTitleFromUrl(sourceUrl)));
            job.setAuthor(defaultString(request.getAuthor(), ""));
            job.setPublisher(defaultString(request.getPublisher(), ""));
            job.setSummary(defaultString(request.getSummary(), ""));
            job.setCategory(defaultString(request.getCategory(), "书籍"));
            job.setTotalChapters(stages.size());
            job.setSuccessChapters(stages.size());
            job.setFailChapters(0);
            job.setProgress(80);
            job.setStatus("await_review");
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
        requireImportJob(jobKey);
        return adminBookMapper.findImportStagesByJobKey(jobKey).stream().map(this::toImportChapterResponse).toList();
    }

    @Override
    @Transactional
    public AdminBookImportChapterResponse updateImportJobChapter(String jobKey, String tempChapterKey, AdminBookImportChapterUpdateRequest request) {
        requireImportJob(jobKey);
        BookImportChapterStage stage = requireImportStage(jobKey, tempChapterKey);
        stage.setTitle(request.getTitle().trim());
        stage.setSubtitle(defaultString(request.getSubtitle(), ""));
        stage.setContentHtml(request.getContentHtml());
        stage.setPlainText(stripHtml(request.getContentHtml()));
        stage.setWordCount(countWords(stage.getPlainText()));
        stage.setSortOrder(request.getSortOrder() == null ? stage.getSortOrder() : request.getSortOrder());
        stage.setReviewStatus("edited");
        adminBookMapper.updateImportStage(stage);
        return toImportChapterResponse(stage);
    }

    @Override
    @Transactional
    public BookResponse publishImportJob(String jobKey) {
        BookImportJob job = requireImportJob(jobKey);
        List<BookImportChapterStage> stages = adminBookMapper.findImportStagesByJobKey(jobKey).stream()
            .sorted(Comparator.comparing(BookImportChapterStage::getSortOrder).thenComparing(BookImportChapterStage::getChapterNo))
            .toList();

        if (stages.isEmpty()) {
            throw new BusinessException(400, "当前导入任务没有可发布章节");
        }

        int totalWords = stages.stream().map(BookImportChapterStage::getWordCount).mapToInt(this::defaultInt).sum();
        Book book = new Book();
        book.setBookKey(defaultString(job.getBookKey(), "book-" + shortId()));
        book.setTitle(requireText(job.getTitle(), "书籍标题不能为空"));
        book.setAuthor(defaultString(job.getAuthor(), "未知作者"));
        book.setPublisher(defaultString(job.getPublisher(), ""));
        book.setCategory(defaultString(job.getCategory(), "书籍"));
        book.setSummary(defaultString(job.getSummary(), ""));
        book.setCoverUrl(defaultString(job.getCoverUrl(), ""));
        book.setTagList(book.getCategory());
        book.setWordCount(totalWords);
        book.setChapterCount(stages.size());
        book.setReadCount(0);
        book.setRating(BigDecimal.valueOf(9.0));
        book.setStatus(1);
        book.setSortOrder(0);
        book.setPublishedAt(LocalDateTime.now());
        adminBookMapper.saveBook(book);

        adminBookMapper.deleteBookChaptersByBookKey(book.getBookKey());
        List<BookChapter> chapters = new ArrayList<>();
        int index = 1;
        for (BookImportChapterStage stage : stages) {
            BookChapter chapter = new BookChapter();
            chapter.setChapterKey("chapter-" + book.getBookKey() + "-" + String.format("%03d", index));
            chapter.setBookKey(book.getBookKey());
            chapter.setChapterNo(index);
            chapter.setTitle(stage.getTitle());
            chapter.setSubtitle(defaultString(stage.getSubtitle(), ""));
            chapter.setContentHtml(stage.getContentHtml());
            chapter.setWordCount(defaultInt(stage.getWordCount()));
            chapter.setIsFree(index <= 3 ? 1 : 0);
            chapter.setStatus(1);
            chapter.setSortOrder(index);
            chapters.add(chapter);
            index++;
        }
        adminBookMapper.batchInsertBookChapters(chapters);

        job.setBookKey(book.getBookKey());
        job.setStatus("published");
        job.setProgress(100);
        job.setMessage("书籍已发布到用户端");
        adminBookMapper.updateImportJob(job);
        return toBookResponse(book);
    }

    @Override
    public List<BookResponse> listBooks() {
        return adminBookMapper.findAllBooks().stream().map(this::toBookResponse).toList();
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

    private List<BookImportChapterStage> parseEpub(byte[] fileBytes, String jobKey) throws Exception {
        List<BookImportChapterStage> stages = new ArrayList<>();
        try (InputStream is = new ByteArrayInputStream(fileBytes)) {
            EpubReader epubReader = new EpubReader();
            nl.siegmann.epublib.domain.Book epubBook = epubReader.readEpub(is);

            Spine spine = epubBook.getSpine();
            List<SpineReference> spineReferences = spine.getSpineReferences();
            int sortOrder = 1;
            int chapterNo = 1;

            for (SpineReference ref : spineReferences) {
                try {
                    nl.siegmann.epublib.domain.Resource resource = ref.getResource();
                    String htmlContent = new String(resource.getData(), StandardCharsets.UTF_8);
                    String cleanedHtml = cleanImportedHtml(htmlContent);
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
                    // skip unreadable resources
                }
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

    private ParsedBookPackage parseBookPackage(byte[] zipBytes, String jobKey) throws Exception {
        Map<String, byte[]> entries = readZipEntries(zipBytes);
        if (entries.isEmpty()) {
            throw new BusinessException(400, "ZIP 压缩包内容为空");
        }

        Map<String, Object> manifest = readManifest(entries);
        String title = requireText(asText(manifest.get("title")), "manifest.json 缺少 title");
        String bookKey = defaultString(asText(manifest.get("bookKey")), buildSlugKey(title));
        String author = defaultString(asText(manifest.get("author")), "未知作者");
        String publisher = defaultString(asText(manifest.get("publisher")), "");
        String category = defaultString(asText(manifest.get("category")), "书籍");
        String summary = defaultString(asText(manifest.get("summary")), "");
        String coverUrl = resolveCoverUrl(entries, defaultString(asText(manifest.get("coverFile")), ""));

        List<Map<String, Object>> chapterDefinitions = readChapterDefinitions(manifest);
        if (chapterDefinitions.isEmpty()) {
            throw new BusinessException(400, "manifest.json 缺少 chapters 章节定义");
        }

        List<BookImportChapterStage> stages = new ArrayList<>();
        int sortOrder = 1;
        for (Map<String, Object> chapterDefinition : chapterDefinitions) {
            String sourcePath = requireText(asText(chapterDefinition.get("file")), "章节文件路径不能为空");
            byte[] chapterBytes = entries.get(sourcePath);
            if (chapterBytes == null) {
                throw new BusinessException(400, "章节文件不存在: " + sourcePath);
            }

            String sourceType = resolveSourceType(sourcePath);
            String html = toImportHtml(sourcePath, chapterBytes);

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

        return new ParsedBookPackage(bookKey, title, author, publisher, category, summary, coverUrl, stages);
    }

    private Map<String, byte[]> readZipEntries(byte[] zipBytes) throws Exception {
        for (Charset charset : List.of(StandardCharsets.UTF_8, Charset.forName("GB18030"), Charset.forName("GBK"))) {
            try {
                Map<String, byte[]> entries = readZipEntries(zipBytes, charset);
                if (!entries.isEmpty()) {
                    return entries;
                }
            } catch (IllegalArgumentException exception) {
                // 继续尝试下一个文件名编码
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

    private Map<String, Object> readManifest(Map<String, byte[]> entries) throws Exception {
        byte[] manifestBytes = entries.get("manifest.json");
        if (manifestBytes == null) {
            throw new BusinessException(400, "导入包缺少 manifest.json");
        }
        return OBJECT_MAPPER.readValue(manifestBytes, new TypeReference<Map<String, Object>>() {});
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

    private String resolveCoverUrl(Map<String, byte[]> entries, String coverFile) throws Exception {
        if (coverFile == null || coverFile.isBlank()) {
            return "";
        }
        byte[] coverBytes = entries.get(coverFile);
        if (coverBytes == null) {
            return "";
        }
        return contentStorageService.upload(
            "book/cover", coverFile.substring(coverFile.lastIndexOf('/') + 1),
            new ByteArrayInputStream(coverBytes), coverBytes.length,
            guessContentType(coverFile)
        );
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
        String[] lines = defaultString(markdown, "").replace("\r\n", "\n").split("\n");
        StringBuilder builder = new StringBuilder();
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) continue;
            if (trimmed.startsWith("### ")) {
                builder.append("<h3>").append(escapeHtml(trimmed.substring(4))).append("</h3>");
            } else if (trimmed.startsWith("## ")) {
                builder.append("<h2>").append(escapeHtml(trimmed.substring(3))).append("</h2>");
            } else if (trimmed.startsWith("# ")) {
                builder.append("<h1>").append(escapeHtml(trimmed.substring(2))).append("</h1>");
            } else {
                builder.append("<p>").append(escapeHtml(trimmed)).append("</p>");
            }
        }
        return builder.toString();
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
        job.setCategory("书籍");
        job.setMessage("等待解析");
        return job;
    }

    private void updateJobWithParsedData(BookImportJob job, ParsedBookPackage parsedBook) {
        job.setBookKey(parsedBook.bookKey());
        job.setTitle(parsedBook.title());
        job.setAuthor(parsedBook.author());
        job.setPublisher(parsedBook.publisher());
        job.setSummary(parsedBook.summary());
        job.setCategory(parsedBook.category());
        job.setCoverUrl(parsedBook.coverUrl());
        job.setTotalChapters(parsedBook.stages().size());
        job.setSuccessChapters(parsedBook.stages().size());
        job.setFailChapters(0);
        job.setProgress(80);
        job.setStatus("await_review");
        job.setMessage("章节解析完成，等待审核发布");
    }

    private String cleanImportedHtml(String html) {
        return defaultString(html, "")
            .replace("<script", "<!-- script")
            .replace("</script>", "</script -->")
            .replace("<style", "<!-- style")
            .replace("</style>", "</style -->")
            .trim();
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

    private AdminBookImportJobResponse toImportJobResponse(BookImportJob job) {
        AdminBookImportJobResponse response = new AdminBookImportJobResponse();
        response.setJobKey(job.getJobKey());
        response.setBookKey(job.getBookKey());
        response.setTitle(job.getTitle());
        response.setAuthor(job.getAuthor());
        response.setPublisher(job.getPublisher());
        response.setSummary(job.getSummary());
        response.setCategory(job.getCategory());
        response.setCoverUrl(job.getCoverUrl());
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

    private AdminBookImportChapterResponse toImportChapterResponse(BookImportChapterStage stage) {
        AdminBookImportChapterResponse response = new AdminBookImportChapterResponse();
        response.setTempChapterKey(stage.getTempChapterKey());
        response.setChapterNo(defaultInt(stage.getChapterNo()));
        response.setTitle(stage.getTitle());
        response.setSubtitle(stage.getSubtitle());
        response.setSourcePath(stage.getSourcePath());
        response.setContentHtml(stage.getContentHtml());
        response.setPlainText(stage.getPlainText());
        response.setWordCount(defaultInt(stage.getWordCount()));
        response.setReviewStatus(stage.getReviewStatus());
        response.setSortOrder(defaultInt(stage.getSortOrder()));
        response.setWarnings(parseWarnings(stage.getWarningJson()));
        return response;
    }

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
        response.setPublishedAt(formatDateTime(book.getPublishedAt()));
        return response;
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
        String bookKey, String title, String author, String publisher,
        String category, String summary, String coverUrl,
        List<BookImportChapterStage> stages
    ) {}
}
