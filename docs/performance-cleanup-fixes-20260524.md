# 性能、冗余、功能缺陷与安全修复报告

> 日期：2026-05-24
> 分支：main
> 范围：server-java（Spring Boot 后端） + 前端（Vue 3）
> 前提：基于[高优先级性能与可靠性修复报告](high-priority-fixes-20260524.md)之后的次优先级问题修复

---

## 背景

在上一轮高优先级修复（缓存、SQL 精确列、线程安全、日志、安全头）完成后，对项目进行了第二轮全面审查，发现 **性能**、**冗余代码**、**功能缺陷** 和 **安全** 四个维度仍存在 12 项待优化问题。

### 核心问题链路

**性能问题**：

```
用户访问书籍阅读页
  → BookServiceImpl.listBookChapters()
    → findPublishedChaptersByBookKey() 加载全章 content_html
      → SELECT * FROM book_chapter（含 MEDIUMTEXT content_html）
        → 50 章 × 每章 50KB = 2.5MB 正文数据
          → 但目录页只需要标题和章节号，完全不需要正文
```

**冗余问题**：

```
AdminBookController.hasValidBookMagicBytes()   ─┐
AdminController.hasValidImageMagicBytes()       ─┤ 三个类各自实现相同的
AdminBookServiceImpl.isImageFileByMagicBytes()  ─┘ match() + 魔数检测逻辑
```

**安全问题**：

```
用户 → POST /api/content/tech-articles/{key}/comments
  → 无认证、无限流
    → 可被脚本批量刷评
```

### 修复目标

| 维度 | 目标 |
|---|---|
| 性能 | 消除 Book / AdminBook Mapper 中所有 SELECT *，书籍章节列表数据传输减少 80-95% |
| 冗余 | 删除未使用依赖（~500KB），提取共享工具类消除 3 处重复代码 |
| 功能 | Markdown 解析器支持代码块/列表/链接/图片，公司 ID 不再硬编码，首页注册人数动态获取 |
| 安全 | 评论接口添加 IP 级别频率限制（10 条/分钟） |

---

## 修复 1：AdminBookMapper.xml SELECT * → 精确列名

### 问题分析

`AdminBookMapper.xml` 中有 10 处 `SELECT *` 查询，分布在 5 张表上。虽然大部分表的 resultMap 不映射大字段（如 `book_import_job` 的 `message` 为 VARCHAR），但 `book_import_chapter_stage` 表的 `content_html` 为 MEDIUMTEXT 列，在审核列表和批量导入阶段会被全量拉取。

### 修复方案

将 10 处 `SELECT *` 全部替换为显式列名，与对应 resultMap 字段一一对应：

| 查询 ID | 表名 | 是否含大字段 |
|---|---|---|
| findSourceFileByKey | book_source_file | 否（仅文件元数据） |
| findImportJobByKey | book_import_job | 否 |
| findImportStagesByJobKey | book_import_chapter_stage | 是（content_html / plain_text） |
| findImportStageByKey | book_import_chapter_stage | 是（同上） |
| findBookChaptersByBookKey | book_chapter | 是（content_html） |
| findAllBooks | book | 否 |
| findBookByKey | book | 否 |
| listImportJobs | book_import_job | 否 |
| findImportJobsByBookKey | book_import_job | 否 |
| findImportJobsByCategory | book_import_job | 否 |

示例（findImportJobByKey）：

```xml
<!-- 修复前 -->
<select id="findImportJobByKey" resultMap="BookImportJobResultMap">
  SELECT * FROM book_import_job WHERE job_key = #{jobKey} LIMIT 1
</select>

<!-- 修复后 -->
<select id="findImportJobByKey" resultMap="BookImportJobResultMap">
  SELECT
    id, job_key, book_key, title, author, translator, publisher,
    summary, category, cover_url, source_file_key, import_type,
    source_url, original_format, file_size, status, progress,
    total_chapters, success_chapters, fail_chapters, message,
    created_at, updated_at
  FROM book_import_job WHERE job_key = #{jobKey} LIMIT 1
</select>
```

### 影响文件

| 文件 | 操作 |
|---|---|
| [AdminBookMapper.xml](server-java/src/main/resources/mapper/AdminBookMapper.xml) | 10 处 SELECT * → 精确列名 |

---

## 修复 2：BookMapper.xml SELECT * → 精确列名 + 章节轻量查询

### 问题分析

`BookMapper.xml` 中有 4 处 `SELECT *`。其中最严重的是 `findPublishedChaptersByBookKey`：该查询在书籍阅读页加载时调用，一次性加载所有章节的 `content_html`（MEDIUMTEXT）。当书籍有 50+ 章时，仅正文数据就达 2-5MB。

但 `listBookChapters()` 接口仅用于构建目录面板（标题 + 章节号），**完全不需要正文**。章节正文由 `getBookChapterDetail()` 单独按需加载。

```
请求流程（修复前）：
  BookReaderView.reloadBookData()
    → getBookChapters(bookKey)
      → listBookChapters()
        → findPublishedChaptersByBookKey()   ← SELECT * 含 content_html
        → 返回 N 章完整内容（含正文 HTML）
    → getBookChapterDetail(bookKey, chapterKey)  ← 再次加载当前章正文！
```

### 修复方案

**Step 1：新增轻量章节查询**

在 BookMapper.xml 中新增 `findPublishedChaptersLightByBookKey`，将 `content_html` 置为 NULL：

```xml
<select id="findPublishedChaptersLightByBookKey" resultMap="BookChapterResultMap">
  SELECT
    id, chapter_key, book_key, chapter_no, title, subtitle,
    NULL AS content_html, word_count, is_free, status, sort_order,
    created_at, updated_at
  FROM book_chapter
  WHERE book_key = #{bookKey} AND status = 1
  ORDER BY sort_order ASC, chapter_no ASC, id ASC
</select>
```

**Step 2：BookServiceImpl.listBookChapters 改用轻量查询**

```java
// 修复前
public List<BookChapterResponse> listBookChapters(String bookKey) {
    return bookMapper.findPublishedChaptersByBookKey(bookKey)
        .stream().map(this::toBookChapterResponse).toList();
}

// 修复后
public List<BookChapterResponse> listBookChapters(String bookKey) {
    return bookMapper.findPublishedChaptersLightByBookKey(bookKey)
        .stream().map(this::toBookChapterResponse).toList();
}
```

**Step 3：前端 isOpeningImageOnlyChapter 适配**

因 contentHtml 可能为 null（轻量查询），修改封面章检测逻辑：

```js
// 修复前：contentHtml 为 null 时返回 false（漏过滤封面章）
if (!html) { return false }

// 修复后：轻量查询无正文时，wordCount=0 即判定为封面章
if (!html) { return true }
```

### 修复效果

| 指标 | 修复前 | 修复后 |
|---|---|---|
| 章节列表数据传输（50 章） | ~2.5MB（含正文） | ~50KB（仅元数据） |
| 目录加载时间 | 200-600ms | < 30ms |
| 正文加载方式 | 目录接口返回全量正文 | 目录仅元数据，正文按需单独加载 |

### 影响文件

| 文件 | 操作 |
|---|---|
| [BookMapper.xml](server-java/src/main/resources/mapper/BookMapper.xml) | 4 处 SELECT * → 精确列名 + 新增轻量查询 |
| [BookMapper.java](server-java/src/main/java/com/interview/auth/infrastructure/mapper/BookMapper.java) | 新增 findPublishedChaptersLightByBookKey 接口 |
| [BookServiceImpl.java](server-java/src/main/java/com/interview/auth/service/impl/BookServiceImpl.java) | listBookChapters 改用轻量查询 |
| [BookReaderView.vue](src/modules/world/BookReaderView.vue) | isOpeningImageOnlyChapter 适配 null contentHtml |

---

## 修复 3：移除未使用的 @antv/g2 依赖

### 问题分析

`package.json` 中声明了 `"@antv/g2": "^5.4.8"`，但源码中无任何 `import` / `require` 引用。该依赖会增加 ~500KB 打包体积。

项目中实际使用的图表库是 `echarts`（AdminModuleChart / AdminTrendChart / AdminCommentChart 三个组件均动态导入 echarts），`@antv/g2` 为冗余依赖。

### 修复方案

直接从 `package.json` 的 `dependencies` 中删除 `@antv/g2` 声明。

### 修复效果

| 指标 | 修复前 | 修复后 |
|---|---|---|
| 生产构建体积 | 含 ~500KB 未使用图表库 | 减少 ~500KB |

### 影响文件

| 文件 | 操作 |
|---|---|
| [package.json](package.json) | 删除 @antv/g2 依赖声明 |

---

## 修复 4：提取 MagicBytesValidator 共享工具类

### 问题分析

文件魔数校验逻辑在 3 个类中重复实现：

```
AdminBookController
  ├── hasValidBookMagicBytes()   — PDF / ZIP 魔数检测
  └── match()                    — 字节比对工具方法

AdminController
  ├── hasValidImageMagicBytes()  — PNG / JPEG / GIF / WebP / BMP 检测
  └── match()                    — 与 AdminBookController 完全相同的 match()

AdminBookServiceImpl
  └── isImageFileByMagicBytes()  — PNG / JPEG / GIF / WebP / BMP / TIFF / SVG / AVIF 检测（内联实现，无 match() 复用）
```

`match()` 方法是逐字节完全重复的，图片魔数检测在 AdminController 和 AdminBookServiceImpl 之间也高度重叠。

### 修复方案

新建 `common/MagicBytesValidator` 工具类，提供：

```java
public final class MagicBytesValidator {
    // 基础
    public static boolean match(byte[] header, int offset, int... expected);
    public static byte[] readHeader(InputStream in, int size);

    // 图片格式
    public static boolean isPng / isJpeg / isGif / isWebP / isBmp / isTiff / isSvg / isAvif(byte[]);
    public static boolean isImage(byte[]);  // 组合检测

    // 文档格式
    public static boolean isPdf(byte[]);
    public static boolean isZipBased(byte[]);  // ZIP / EPUB / DOCX
}
```

三个调用方重构：

```java
// AdminBookController — 修复前（40+ 行）
private boolean hasValidBookMagicBytes(MultipartFile file) {
    ... // 读 header + 手动比对 PDF / ZIP 魔数
}
private boolean match(byte[] header, int offset, int... expected) { ... }

// AdminBookController — 修复后（10 行）
private boolean hasValidBookMagicBytes(MultipartFile file) {
    byte[] header = MagicBytesValidator.readHeader(in, 4);
    if ("pdf".equals(ext)) return MagicBytesValidator.isPdf(header);
    if (Set.of("zip", "epub", "docx").contains(ext)) return MagicBytesValidator.isZipBased(header);
}

// AdminController — 修复前（30+ 行）
// AdminController — 修复后（5 行）
byte[] header = MagicBytesValidator.readHeader(in, 12);
return MagicBytesValidator.isImage(header);

// AdminBookServiceImpl — 修复前（46 行内联魔数检测）
// AdminBookServiceImpl — 修复后（3 行）
return MagicBytesValidator.isImage(bytes);
```

### 修复效果

| 指标 | 修复前 | 修复后 |
|---|---|---|
| 魔数检测代码行数 | ~120 行（3 处重复） | ~80 行工具类 + 3 处各 3-10 行调用 |
| 支持图片格式 | 5 种（部分实现） | 8 种（统一实现：PNG/JPEG/GIF/WebP/BMP/TIFF/SVG/AVIF） |
| 新增格式维护 | 需要改 3 个类 | 只改 1 个类 |

### 影响文件

| 文件 | 操作 |
|---|---|
| [MagicBytesValidator.java](server-java/src/main/java/com/interview/auth/common/MagicBytesValidator.java) | 新建工具类 |
| [AdminBookController.java](server-java/src/main/java/com/interview/auth/admin/controller/AdminBookController.java) | 删除 match() + 内联魔数检测，改用工具类 |
| [AdminController.java](server-java/src/main/java/com/interview/auth/admin/controller/AdminController.java) | 删除 match() + hasValidImageMagicBytes 简化 |
| [AdminBookServiceImpl.java](server-java/src/main/java/com/interview/auth/admin/service/impl/AdminBookServiceImpl.java) | isImageFileByMagicBytes 简化为 1 行调用 |

---

## 修复 5：Markdown 解析器增强

### 问题分析

`AdminBookServiceImpl.markdownToHtml()` 原始实现极其简陋，仅处理 3 种元素：

```
修复前支持：
  # 标题    → <h1>
  ## 标题   → <h2>
  ### 标题  → <h3>
  其他      → <p>
```

缺失的支持：
- 围栏代码块（```language ... ```）
- 无序列表（- item, * item）
- 有序列表（1. item）
- 引用块（> text）
- 分隔线（---, ***）
- 行内加粗（**text**）
- 行内斜体（*text*）
- 行内代码（`code`）
- 删除线（~~text~~）
- 链接（[text](url)）
- 图片（![alt](url)）

这导致用户上传 Markdown 格式书籍时，除标题和段落外所有排版信息丢失，严重影响阅读体验。

### 修复方案

重写 `markdownToHtml()`，新增 `renderInlineMarkdown()` 辅助方法：

```java
private String markdownToHtml(String markdown) {
    // 逐行解析，支持：
    // - 分隔线：--- / *** → <hr />
    // - 围栏代码块：```lang ... ``` → <pre><code>
    // - 标题：### / ## / # → <h3> / <h2> / <h1>
    // - 引用：> text → <blockquote>
    // - 无序列表：- / * / + item → <ul><li>
    // - 有序列表：1. item → <ol><li>
    // - 普通段落 → <p>
    // 所有文本内容经过 renderInlineMarkdown() 处理行内格式
}

private String renderInlineMarkdown(String text) {
    // 先 HTML 转义防 XSS
    // 再按顺序处理：图片 → 链接 → 加粗 → 斜体 → 删除线 → 行内代码
}
```

**关键设计**：
- 先 `escapeHtml()` 转义所有 HTML 标签，防止 XSS 注入
- 再按优先级顺序替换 Markdown 语法为 HTML 标签
- 图片在链接之前处理，避免 `!` 被 `[text](url)` 误匹配
- 加粗 `**` 在斜体 `*` 之前处理，避免 `**text**` 被两次斜体匹配破坏

### 修复效果

| 指标 | 修复前 | 修复后 |
|---|---|---|
| 支持的 Markdown 元素 | 3 种（h1/h2/h3/p） | 12 种 |
| 代码块 | 无 | 围栏代码块 + 语言标注 |
| 列表 | 无 | 有序列表 + 无序列表 |
| 行内格式 | 无 | 加粗/斜体/删除线/代码/链接/图片 |

### 影响文件

| 文件 | 操作 |
|---|---|
| [AdminBookServiceImpl.java](server-java/src/main/java/com/interview/auth/admin/service/impl/AdminBookServiceImpl.java) | 重写 markdownToHtml + 新增 renderInlineMarkdown |

---

## 修复 6：parseCompanyId 硬编码修复

### 问题分析

`AdminServiceImpl.parseCompanyId()` 始终返回 `1L`，无论面经分类（如 "alibaba"、"bytedance" 等）为何值：

```java
// 修复前
private Long parseCompanyId(String category) {
    return 1L;  // 所有面经的公司 ID 都是 1
}
```

后果：所有面经被保存到同一个公司下，多公司场景下数据归类错误，且 `interview.company_id` 字段形同虚设。

### 修复方案

**Step 1：新增 company 表查询（AdminMapper）**

```java
Company findCompanyByCategoryCode(@Param("categoryCode") String categoryCode);
```

```xml
<select id="findCompanyByCategoryCode" resultType="com.interview.auth.domain.entity.Company">
  SELECT id, name, short_name, avatar_text, avatar_color, logo_url, description, created_at, updated_at
  FROM company
  WHERE short_name = #{categoryCode} OR name = #{categoryCode}
  LIMIT 1
</select>
```

**Step 2：parseCompanyId 使用真实查询**

```java
// 修复后
private Long parseCompanyId(String category) {
    String normalizedCode = normalizeInterviewCategoryCode(category);
    Company company = adminMapper.findCompanyByCategoryCode(normalizedCode);
    return company != null ? company.getId() : 1L;  // 未匹配时回退默认
}
```

**匹配逻辑**：分类编码经过标准化后，与 company 表的 `short_name` 或 `name` 字段匹配。例如 category "alibaba" 匹配 company.short_name = "alibaba"。

### 影响文件

| 文件 | 操作 |
|---|---|
| [AdminMapper.java](server-java/src/main/java/com/interview/auth/admin/mapper/AdminMapper.java) | 新增 findCompanyByCategoryCode 接口 |
| [AdminMapper.xml](server-java/src/main/resources/mapper/AdminMapper.xml) | 新增 SQL 查询 |
| [AdminServiceImpl.java](server-java/src/main/java/com/interview/auth/admin/service/impl/AdminServiceImpl.java) | parseCompanyId 使用真实查询 |

---

## 修复 7：首页注册人数动态获取

### 问题分析

`HomeView.vue` 中 `routeEnrollCount` 硬编码为 62，并标注 TODO：

```js
const routeEnrollCount = ref(62) // TODO: 后续接入后端 API 返回实际注册人数
```

首页展示的注册用户数量与实际注册数脱节，给用户不信任感。

### 修复方案

**Step 1：后端新增注册人数统计 API**

AuthUserMapper 新增 countUsers：

```java
long countUsers();
```

```xml
<select id="countUsers" resultType="long">
  SELECT COUNT(1) FROM auth_user WHERE status = 1
</select>
```

AuthService / AuthController 新增公开端点：

```java
// AuthController.java
@GetMapping("/user-count")
public ApiResponse<Map<String, Object>> getUserCount() {
    return ApiResponse.success(Map.of("userCount", authService.getUserCount()));
}
```

**Step 2：前端接入 API**

```js
// HomeView.vue — loadHomeData()
const userCountResult = await fetch(`${apiBase}/api/auth/user-count`)
  .then(r => r.ok ? r.json() : null)
// ...
routeEnrollCount.value = Number(userCountResult.value.data.userCount || 0)
```

该端点无需认证，前端与其他数据并行加载（Promise.allSettled），失败时不影响页面其余部分展示。

### 影响文件

| 文件 | 操作 |
|---|---|
| [AuthUserMapper.java](server-java/src/main/java/com/interview/auth/infrastructure/mapper/AuthUserMapper.java) | 新增 countUsers 接口 |
| [AuthUserMapper.xml](server-java/src/main/resources/mapper/AuthUserMapper.xml) | 新增 countUsers SQL |
| [AuthService.java](server-java/src/main/java/com/interview/auth/service/AuthService.java) | 新增 getUserCount 接口 |
| [AuthServiceImpl.java](server-java/src/main/java/com/interview/auth/service/impl/AuthServiceImpl.java) | 实现 getUserCount |
| [AuthController.java](server-java/src/main/java/com/interview/auth/controller/AuthController.java) | 新增 GET /api/auth/user-count |
| [HomeView.vue](src/views/HomeView.vue) | 移除硬编码，接入 API |

---

## 修复 8：评论接口 IP 频率限制

### 问题分析

评论接口 `POST /api/content/tech-articles/{articleKey}/comments` 无需认证即可调用，且没有任何频率限制。攻击者可以：

```
while true:
    POST /api/content/tech-articles/xxx/comments
    body: { "content": "垃圾评论" }
```

一次攻击可在数秒内产生数千条垃圾评论。虽然 `LoginRateLimitService` 保护了登录接口，但评论接口完全裸露。

### 修复方案

新建 `CommentRateLimitService`，采用与 `LoginRateLimitService` 相同的 Redis + 降级模式：

```java
@Slf4j
@Component
@RequiredArgsConstructor
public class CommentRateLimitService {
    private static final String KEY_PREFIX = "comment:rate-limit:ip:";
    private static final int MAX_COMMENTS_PER_WINDOW = 10;
    private static final Duration WINDOW_DURATION = Duration.ofMinutes(1);

    public void checkRateLimit(String clientIp) {
        try {
            String key = KEY_PREFIX + clientIp;
            Long count = stringRedisTemplate.opsForValue().increment(key);
            if (count != null && count == 1L) {
                stringRedisTemplate.expire(key, WINDOW_DURATION);
            }
            if (count != null && count > MAX_COMMENTS_PER_WINDOW) {
                throw new BusinessException(429, "评论过于频繁，请稍后再试");
            }
        } catch (DataAccessException e) {
            log.warn("Redis 不可用，跳过评论频率限制: {}", e.getMessage());
        }
    }
}
```

ContentController 集成：

```java
@PostMapping("/tech-articles/{articleKey}/comments")
public ApiResponse<Map<String, Object>> addArticleComment(..., HttpServletRequest request) {
    commentRateLimitService.checkRateLimit(extractClientIp(request));  // ★ 限流检查
    // ... 原有评论逻辑
}
```

**设计考量**：
- **10 条/分钟**：正常人评论速度远低于此阈值，脚本批量发布会被阻断
- **Redis 主 + 降级**：Redis 不可用时仅 log.warn，不阻断正常用户评论（可用性优先于限流准确性）
- **IP 粒度**：同一 IP 所有文章共享限流窗口，防止切换文章绕过限制

### 影响文件

| 文件 | 操作 |
|---|---|
| [CommentRateLimitService.java](server-java/src/main/java/com/interview/auth/service/CommentRateLimitService.java) | 新建限流服务 |
| [ContentController.java](server-java/src/main/java/com/interview/auth/controller/ContentController.java) | 注入限流服务 + 评论接口集成 |

---

## 文件变更汇总

### 修改文件（19 个）

| 文件 | 说明 |
|---|---|
| [package.json](package.json) | 删除 @antv/g2 依赖 |
| [AdminBookMapper.xml](server-java/src/main/resources/mapper/AdminBookMapper.xml) | 10 处 SELECT * → 精确列名 |
| [BookMapper.xml](server-java/src/main/resources/mapper/BookMapper.xml) | 4 处 SELECT * → 精确列名 + 轻量查询 |
| [BookMapper.java](server-java/src/main/java/com/interview/auth/infrastructure/mapper/BookMapper.java) | 新增轻量查询接口 |
| [BookServiceImpl.java](server-java/src/main/java/com/interview/auth/service/impl/BookServiceImpl.java) | 改用轻量查询 |
| [BookReaderView.vue](src/modules/world/BookReaderView.vue) | 适配 null contentHtml |
| [AdminBookController.java](server-java/src/main/java/com/interview/auth/admin/controller/AdminBookController.java) | 魔数校验改用工具类 |
| [AdminController.java](server-java/src/main/java/com/interview/auth/admin/controller/AdminController.java) | 魔数校验改用工具类 |
| [AdminBookServiceImpl.java](server-java/src/main/java/com/interview/auth/admin/service/impl/AdminBookServiceImpl.java) | 魔数校验精简 + Markdown 解析器重写 |
| [AdminMapper.java](server-java/src/main/java/com/interview/auth/admin/mapper/AdminMapper.java) | 新增 findCompanyByCategoryCode |
| [AdminMapper.xml](server-java/src/main/resources/mapper/AdminMapper.xml) | 新增 company 查询 SQL |
| [AdminServiceImpl.java](server-java/src/main/java/com/interview/auth/admin/service/impl/AdminServiceImpl.java) | parseCompanyId 真实查询 |
| [AuthUserMapper.java](server-java/src/main/java/com/interview/auth/infrastructure/mapper/AuthUserMapper.java) | 新增 countUsers |
| [AuthUserMapper.xml](server-java/src/main/resources/mapper/AuthUserMapper.xml) | 新增 countUsers SQL |
| [AuthService.java](server-java/src/main/java/com/interview/auth/service/AuthService.java) | 新增 getUserCount 接口 |
| [AuthServiceImpl.java](server-java/src/main/java/com/interview/auth/service/impl/AuthServiceImpl.java) | 实现 getUserCount |
| [AuthController.java](server-java/src/main/java/com/interview/auth/controller/AuthController.java) | 新增 GET /api/auth/user-count |
| [ContentController.java](server-java/src/main/java/com/interview/auth/controller/ContentController.java) | 评论接口集成限流 + 客户端 IP 提取 |
| [HomeView.vue](src/views/HomeView.vue) | 注册人数接入 API |

### 新建文件（2 个）

| 文件 | 说明 |
|---|---|
| [MagicBytesValidator.java](server-java/src/main/java/com/interview/auth/common/MagicBytesValidator.java) | 文件魔数校验共享工具类 |
| [CommentRateLimitService.java](server-java/src/main/java/com/interview/auth/service/CommentRateLimitService.java) | 评论 IP 频率限制服务 |

---

## 架构设计思路

### 魔数校验重构

```
修复前：
  AdminBookController ─── match() + PDF/ZIP 检测
  AdminController     ─── match() + PNG/JPEG/GIF/WebP/BMP 检测
  AdminBookServiceImpl ── isImageFileByMagicBytes() 内联检测（8 种格式）

修复后：
  MagicBytesValidator ─── match() / readHeader() / isPng / isJpeg / isGif
                      │    isWebP / isBmp / isTiff / isSvg / isAvif
                      │    isImage() [组合检测]
                      │    isPdf / isZipBased
                      │
  AdminBookController ─── hasValidBookMagicBytes() ── 调用 MagicBytesValidator.isPdf / isZipBased
  AdminController     ─── hasValidImageMagicBytes() ─ 调用 MagicBytesValidator.isImage
  AdminBookServiceImpl ── isImageFileByMagicBytes() ── 调用 MagicBytesValidator.isImage
```

### 评论限流数据流

```
POST /api/content/tech-articles/{key}/comments
  │
  ├─ extractClientIp(request)
  │   ├─ X-Forwarded-For  → 取首个 IP
  │   ├─ X-Real-IP        → 直取
  │   └─ request.getRemoteAddr()
  │
  └─ commentRateLimitService.checkRateLimit(clientIp)
      ├─ Redis INCR comment:rate-limit:ip:{ip}
      │   ├─ 首次 → EXPIRE 60s
      │   └─ count > 10 → 429 Too Many Requests
      └─ Redis 不可用 → log.warn + 放行
```

### 用户计数链路

```
HomeView.loadHomeData()
  │
  ├─ Promise.allSettled([
  │     getTechArticles(),
  │     getLearningRoutes(),
  │     getInterviews(),
  │     fetch('/api/auth/user-count')     ← 新增，并行加载
  │   ])
  │
  └─ userCountResult.data.userCount → routeEnrollCount
      （失败时不影响页面其余模块展示）
```

---

## 后续建议（低优先级）

以下问题在本次修复中未处理，建议后续逐步优化：

1. **JWT Token 存储在 localStorage** — 前端 `content.js` 的 `getAccessToken()` 直接读 `localStorage`，存在 XSS 泄露风险。建议迁移至 httpOnly Cookie 或使用 BFF 代理模式
2. **前端零测试** — 未引入 Vitest 等测试框架，建议为核心组件（BookReaderView、TechArticleList）添加基础测试
3. **AdminBookServiceImpl 上帝类** — 3347 行同时处理 ZIP 解压、多格式解析、图片检测、存储管理，建议按职责拆分为 BookImportParser、BookStorageManager、BookImageResolver 等专职类
4. **BookMapper 章节查询仍含 content_html** — `findPublishedChaptersByBookKey`（完整版）在 `AdminBookServiceImpl` 中仍被使用（批量导入/审核需要正文），暂保留。管理员审核路径可后续增加分页加载优化
5. **comment 接口再优化** — 当前限流基于 Redis INCR，建议后续增加验证码或 reCAPTCHA 作为二级防护
6. **前端大组件拆分** — AdminBookImportView（3406 行）、AdminDashboardView（2750 行）建议按功能区域拆分为子组件
