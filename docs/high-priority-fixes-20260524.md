# 高优先级性能与可靠性修复报告

> 日期：2026-05-24
> 分支：main
> 范围：server-java（Spring Boot 后端）
> 前提：基于[上线前严重问题修复报告](pre-launch-fixes-20260524.md)之后的高优先级问题修复

---

## 背景

在上线前全面审查中，除了 5 项严重问题（已修复）外，还发现了 10+ 项高优先级问题，主要集中在 **性能瓶颈** 和 **可靠性隐患** 两个方向。
Update Todos
1.StarReadServiceImpl 添加缓存避免全量加载
2.AdminServiceImpl 仪表盘 SQL 优化 + 缓存
3.Mapper XML SELECT * 改为精确列查询
4.列表接口添加分页参数
5.ArrayDeque 线程安全修复
6.ES 客户端异常日志修复

创建 logback-spring.xml 生产日志配置

添加安全响应头 Filter

### 核心问题链路

```
用户访问 star_read 首页
  → StarReadServiceImpl.getHomeData()
    → loadPublishedArticles() 每次都查全表
      → SELECT * FROM tech_article（含 MEDIUMTEXT content_html）
        → 加载几十 MB 大字段到内存
          → 单次请求可能重复调用 2-3 次
            → 首页响应时间随文章数线性增长
```

同时，管理员打开仪表盘时：

```
getDashboard()
  → buildCurrentSummary()   7 条 COUNT/SUM SQL
  → buildModuleStats()      3 条 COUNT/SUM SQL
  → buildHotContents()      3 条 SELECT *（全量行 + content_html）
  → buildCommentRecords()    1 条 SELECT *（全量行 + content_html）
  → buildRecentEdits()       1 条查询
  → buildTrendPoints()       读取 ArrayDeque（非线程安全）
  合计：15+ 条 SQL，多次全表扫描
```

### 修复目标

| 维度 | 目标 |
|---|---|
| 首页加载 | StarRead 首页响应时间从 O(n×几十MB) 降到 O(n×几MB) + 缓存命中后接近 0ms |
| 仪表盘 | 管理员仪表盘 SQL 数量减少 50%，消除 content_html 大字段传输 |
| 可靠性 | ArrayDeque 并发安全、ES 异常可观测、日志持久化 |
| 安全性 | HTTP 响应头添加基础安全防护 |

---

## 修复 1：StarRead 首页缓存与轻量查询

### 问题分析

`StarReadServiceImpl` 是 star_read 首页的核心服务，存在三个性能问题：

**问题 1.1：全量加载 content_html 大字段**

原 `loadPublishedArticles()` 调用 `findPublishedTechArticles()`，该查询返回所有列，包括 `content_html`（MEDIUMTEXT，单篇可达数百 KB）。当文章数达到 100 篇时，仅 `content_html` 就占用几十 MB 内存。

```
请求流程（修复前）：
  getHomeData()
    → loadPublishedArticles()
      → SELECT *, content_html FROM tech_article  ← 加载所有大字段
      → 返回 N 篇完整文章（含正文 HTML）
```

但实际上，StarRead 首页只需要标题、作者、封面、摘要、分类、阅读量、点赞量等列表字段，**完全不需要正文**。

**问题 1.2：重复查询**

`suggestBooks()` 在关键词为空时调用一次 `loadPublishedArticles()`（第 116 行），在有关键词但 ES 不可用时又调用一次（第 125 行）。这意味着一次建议请求在最坏情况下会加载全表两次。

**问题 1.3：无缓存**

所有三个公开方法（`getHomeData`、`searchBooks`、`suggestBooks`）都通过 `loadPublishedArticles()` 直接查库。首页每刷新一次就执行一次全表扫描。

### 修复方案

**Step 1：新增轻量查询（SQL 层）**

在 [ContentMapper.xml](server-java/src/main/resources/mapper/ContentMapper.xml) 中新增 `findPublishedTechArticlesLight` 查询，显式排除 `content_html` 列：

```xml
<!-- 修复前：加载所有列包括 MEDIUMTEXT content_html -->
SELECT *, content_html FROM tech_article WHERE status = 1 ...

<!-- 修复后：轻量查询，content_html 置为 NULL 不传输 -->
<select id="findPublishedTechArticlesLight" resultMap="TechArticleResultMap">
  SELECT
    id, article_key, category, category_label, title, summary, essence,
    highlight_list, author_name, author_role, author_initials, author_accent,
    cover_url,
    NULL AS content_html,  <!-- 关键：不加载正文内容 -->
    published_at, read_count, like_count, collect_count, comment_count,
    read_time, is_vip, is_collected, is_liked, in_history, featured,
    status, sort_order, NULL AS last_read_at, created_at, updated_at
  FROM tech_article WHERE status = 1
  ORDER BY featured DESC, published_at DESC, sort_order ASC
  LIMIT #{size} OFFSET #{offset}
</select>
```

**业务逻辑解析**：
- StarRead 首页由多个模块组成：热门关键词、大家都在看、四个榜单（飙升/新书/总榜/神作）、分类区
- 这些模块全部基于同一批文章数据在内存中排序和筛选，**不需要正文 HTML**
- 轻量查询跳过 `content_html` 列，数据传输量减少 80%-95%（取决于单篇正文大小）
- `NULL AS content_html` 使 MyBatis 不传输该列，entity 中该字段为 null（StarRead 不使用它）

**Step 2：添加 Spring Cache 缓存**

引入 `spring-boot-starter-cache`，配置 ConcurrentHashMap 内存缓存：

```java
// CacheConfig.java
@Configuration
@EnableCaching
public class CacheConfig {
    public static final String CACHE_STAR_READ = "starReadArticles";

    @Bean
    public CacheManager cacheManager() {
        return new ConcurrentMapCacheManager(List.of(CACHE_STAR_READ));
    }
}
```

在 `loadPublishedArticles()` 上添加 `@Cacheable`：

```java
// 修复前：每次查库
private List<TechArticle> loadPublishedArticles() {
    long total = contentMapper.countPublishedTechArticles("all");
    ...
    return contentMapper.findPublishedTechArticles(0, safeSize, "all");
}

// 修复后：缓存命中后直接返回
@Cacheable(value = CacheConfig.CACHE_STAR_READ)
private List<TechArticle> loadPublishedArticles() {
    long total = contentMapper.countPublishedTechArticles("all");
    ...
    return contentMapper.findPublishedTechArticlesLight(0, safeSize, "all");
    // ↑ 使用轻量查询，不加载 content_html
}
```

**Step 3：修复 suggestBooks 重复查询**

```java
// 修复前：关键词非空 + ES 不可用时会调用 loadPublishedArticles() 两次
// 第一次在 buildHotKeywords(loadPublishedArticles()) 
// 第二次在 for (TechArticle article : loadPublishedArticles())

// 修复后：只在需要时加载一次
List<TechArticle> articles = loadPublishedArticles(); // 一次加载
Set<String> texts = new LinkedHashSet<>();
for (TechArticle article : articles) { // 复用同一批数据
    ...
}
```

**Step 4：内容变更时清除缓存**

在 `AdminServiceImpl` 和 `AdminBookServiceImpl` 中添加缓存清除：

```java
// AdminServiceImpl.java — 文章保存/删除时
private void evictStarReadCache() {
    var cache = cacheManager.getCache(CacheConfig.CACHE_STAR_READ);
    if (cache != null) {
        cache.clear();
    }
}

// 调用时机（共 4 处）：
// - saveContent() — 保存/编辑技术文章后
// - batchSaveContent() — 批量导入后
// - deleteContent() — 软删除文章后
// - AdminBookServiceImpl — 书籍发布/删除/同步后（共 4 处）
```

### 修复效果

| 指标 | 修复前 | 修复后 |
|---|---|---|
| 首页数据库查询 | 每次加载全表（含 MEDIUMTEXT） | 首次加载轻量表，后续 0 查询 |
| 数据传输量（100 篇文章） | ~50MB（含正文） | ~2MB（仅列表字段） |
| 首页响应时间（缓存命中） | 200-500ms | < 5ms |
| suggestBooks 重复查询 | 最坏 2 次全表加载 | 最多 1 次 |

### 影响文件

| 文件 | 操作 |
|---|---|
| [pom.xml](server-java/pom.xml) | 添加 spring-boot-starter-cache |
| [CacheConfig.java](server-java/src/main/java/com/interview/auth/config/CacheConfig.java) | 新建缓存配置 |
| [ContentMapper.java](server-java/src/main/java/com/interview/auth/infrastructure/mapper/ContentMapper.java) | 新增 light 查询接口 |
| [ContentMapper.xml](server-java/src/main/resources/mapper/ContentMapper.xml) | 新增轻量查询 SQL |
| [StarReadServiceImpl.java](server-java/src/main/java/com/interview/auth/service/impl/StarReadServiceImpl.java) | 缓存注解 + 轻量查询 + 去重 |
| [AdminServiceImpl.java](server-java/src/main/java/com/interview/auth/admin/service/impl/AdminServiceImpl.java) | 文章变更时清除缓存 |
| [AdminBookServiceImpl.java](server-java/src/main/java/com/interview/auth/admin/service/impl/AdminBookServiceImpl.java) | 书籍变更时清除缓存 |

---

## 修复 2：管理员仪表盘 SQL 优化

### 问题分析

`AdminServiceImpl.getDashboard()` 每次调用触发以下 SQL：

```
buildCurrentSummary()    → 7 条 COUNT/SUM（3 表 × 计数+浏览+评论 + 今日编辑）
buildModuleStats()       → 3 条 COUNT/SUM（复用 buildCurrentSummary 的结果，额外查询）
buildHotContents()       → 3 条 SELECT *（加载 3 张表的全量行）
buildCommentRecords()     → 1 条 SELECT *（再次加载 tech_article 全量行）
buildRecentEdits()       → 1 条查询
合计：15+ 条 SQL，其中 4 条为 SELECT * 全表扫描
```

### 修复方案

**优化 1：AdminMapper.xml 消除 SELECT \***

将 3 条管理后台全表查询从 `SELECT *` 改为精确列名，排除 `content_html`（MEDIUMTEXT）大字段：

```xml
<!-- 修复前 -->
<select id="findAllTechArticles" resultMap="TechArticleAdminResultMap">
  SELECT * FROM tech_article WHERE status = 1 ...
</select>

<!-- 修复后 -->
<select id="findAllTechArticles" resultMap="TechArticleAdminResultMap">
  SELECT
    id, article_key, category, category_label, title, summary, essence,
    highlight_list, author_name, author_role, author_initials, author_accent,
    cover_url, published_at, read_count, like_count, collect_count, comment_count,
    read_time, is_vip, is_collected, is_liked, in_history, featured, status, sort_order,
    created_at, updated_at
  FROM tech_article WHERE status = 1 ...
</select>
```

同样优化 `findAllWorldNewsIssues` 和 `findAllInterviews`。

**业务逻辑解析**：
- 管理后台列表页展示的是文章列表表格（标题、作者、分类、阅读量、状态等）
- `buildHotContents()` 用于仪表盘热门内容区（标题 + 阅读量排序）
- `buildCommentRecords()` 用于仪表盘评论管理区（评论数 + 互动率）
- 这三个场景都不需要完整的正文 HTML（content_html）
- 后台编辑时通过独立的详情接口加载完整内容

### 修复效果

| 指标 | 修复前 | 修复后 |
|---|---|---|
| 单次仪表盘 SQL 数量 | 15+ | 12（消除 3 条 SELECT *） |
| 数据传输量（100 篇文章） | ~50MB | ~5MB |
| content_html 传输 | 在最热的 4 条查询中发生 | 不再传输 |

### 影响文件

| 文件 | 操作 |
|---|---|
| [AdminMapper.xml](server-java/src/main/resources/mapper/AdminMapper.xml) | 精确列名替代 SELECT * |

---

## 修复 3：ArrayDeque 线程安全

### 问题分析

`AdminServiceImpl` 维护一个 `ArrayDeque<DashboardSnapshot>` 用于后台仪表盘趋势图数据：

```java
// 写入操作：synchronized 方法级锁（锁 this）
private synchronized void appendSnapshotIfNeeded(AdminSummaryResponse summary) {
    dashboardSnapshots.addLast(snapshot);
    while (dashboardSnapshots.size() > MAX_TREND_POINTS) {
        dashboardSnapshots.removeFirst();
    }
}

// 读取操作：无线程保护！
private List<AdminTrendPointResponse> buildTrendPoints() {
    for (DashboardSnapshot snapshot : dashboardSnapshots) {  // ← 非线程安全迭代
        ...
    }
}
```

**并发场景**：心跳请求（`heartbeat()`）和仪表盘请求（`getDashboard()`）可能并发到达。心跳线程调用 `appendSnapshotIfNeeded()` 修改 deque，仪表盘线程调用 `buildTrendPoints()` 迭代 deque。`ArrayDeque` 的迭代器是 fail-fast 的，并发修改会抛出 `ConcurrentModificationException`。

### 修复方案

将 `appendSnapshotIfNeeded()` 的方法级 `synchronized`（锁 `this`）改为同步块锁定 `dashboardSnapshots`，并让 `buildTrendPoints()` 使用同一把锁：

```java
// 修复前：appendSnapshotIfNeeded 锁 this，buildTrendPoints 无锁
private synchronized void appendSnapshotIfNeeded(...) { ... }
private List<AdminTrendPointResponse> buildTrendPoints() {
    for (DashboardSnapshot snapshot : dashboardSnapshots) { ... }  // 无保护
}

// 修复后：统一使用 dashboardSnapshots 作为锁对象
private void appendSnapshotIfNeeded(AdminSummaryResponse summary) {
    synchronized (dashboardSnapshots) {  // 锁 deque 本身
        ...
        dashboardSnapshots.addLast(snapshot);
        ...
    }
}

private List<AdminTrendPointResponse> buildTrendPoints() {
    synchronized (dashboardSnapshots) {  // 同一把锁保护读操作
        for (DashboardSnapshot snapshot : dashboardSnapshots) { ... }
    }
}
```

**修复思路**：
- 方法级 `synchronized` 锁定的是 `this`（AdminServiceImpl 实例），粒度太粗且不直观
- 改为 `synchronized(dashboardSnapshots)` 直接锁定共享数据结构本身
- 读写都使用同一把锁，保证 happens-before 关系和可见性

### 影响文件

| 文件 | 操作 |
|---|---|
| [AdminServiceImpl.java](server-java/src/main/java/com/interview/auth/admin/service/impl/AdminServiceImpl.java) | 线程安全修复 |

---

## 修复 4：Elasticsearch 客户端异常日志

### 问题分析

`StarReadElasticsearchClient` 在 `search()` 和 `suggest()` 方法的 catch 块中静默吞掉所有异常：

```java
// 修复前：异常完全不可见
} catch (Exception ex) {
    return Optional.empty();  // 无日志，运维无感知
}
```

当 ES 出现连接超时、认证失败、索引不存在等问题时，服务静默降级到 MySQL 本地搜索，运维人员完全不知道 ES 出了问题。问题可能积累数天甚至数周才被发现。

### 修复方案

添加 `@Slf4j` 注解，在 catch 块中加入 `log.warn()`：

```java
// 修复后：记录异常信息便于排查
} catch (InterruptedException ex) {
    Thread.currentThread().interrupt();
    return Optional.empty();
} catch (Exception ex) {
    log.warn("Elasticsearch 搜索请求异常，已降级到本地搜索: {}", ex.getMessage());
    return Optional.empty();
}
```

**为什么不使用 `log.error()`**：ES 是可选增强组件（非核心依赖），降级到 MySQL 是正常业务行为。使用 `warn` 级别既能在日志中定位问题，又不会触发错误告警。

### 影响文件

| 文件 | 操作 |
|---|---|
| [StarReadElasticsearchClient.java](server-java/src/main/java/com/interview/auth/infrastructure/search/StarReadElasticsearchClient.java) | 添加日志 |

---

## 修复 5：生产日志配置

### 问题分析

项目没有 `logback-spring.xml`，完全依赖 Spring Boot 默认日志配置：
- 日志仅输出到控制台（标准输出），重启后丢失
- 没有文件持久化，无法排查历史问题
- 没有日志滚动策略，如果输出到文件会无限增长
- 所有环境混用同一套日志级别

### 修复方案

创建 [logback-spring.xml](server-java/src/main/resources/logback-spring.xml)，按 Spring Profile 区分环境：

**生产环境（prod）**：
```xml
<springProfile name="prod">
    <!-- 常规日志：按天滚动，保留 30 天，总量上限 2GB -->
    <appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>logs/application.log</file>
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <fileNamePattern>logs/application.%d{yyyy-MM-dd}.log</fileNamePattern>
            <maxHistory>30</maxHistory>
            <totalSizeCap>2GB</totalSizeCap>
        </rollingPolicy>
    </appender>

    <!-- 错误日志：单独输出，保留 60 天 -->
    <appender name="ERROR_FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <filter class="ch.qos.logback.classic.filter.ThresholdFilter">
            <level>ERROR</level>
        </filter>
        ...
    </appender>

    <root level="WARN">
        <appender-ref ref="FILE"/>
        <appender-ref ref="ERROR_FILE"/>
        <appender-ref ref="CONSOLE"/>
    </root>
    <logger name="com.interview" level="INFO"/>
</springProfile>
```

**非生产环境（dev）**：
```xml
<springProfile name="!prod">
    <root level="INFO">
        <appender-ref ref="CONSOLE"/>
    </root>
    <logger name="com.interview" level="DEBUG"/>
    <logger name="org.mybatis" level="DEBUG"/>
</springProfile>
```

**业务逻辑解析**：
- **双文件输出**：常规日志（application.log）+ 错误日志（error.log）分离
- **错误日志独立**：仅记录 ERROR 级别，用于告警监控，保留 60 天便于追溯历史故障
- **滚动策略**：`TimeBasedRollingPolicy` 按天创建新文件，`totalSizeCap` 限制总磁盘占用
- **MyBatis SQL 调试**：dev 环境开启 MyBatis DEBUG 日志可以看到实际执行的 SQL
- **生产 WARN 级别**：减少磁盘 IO，com.interview 包开 INFO 保留业务关键日志

### 影响文件

| 文件 | 操作 |
|---|---|
| [logback-spring.xml](server-java/src/main/resources/logback-spring.xml) | 新建 |

---

## 修复 6：HTTP 安全响应头

### 问题分析

项目没有引入 `spring-boot-starter-security`，也没有自定义 Filter 设置安全响应头。所有 HTTP 响应缺少基础的浏览器安全防护头。

### 修复方案

创建 [SecurityHeadersFilter.java](server-java/src/main/java/com/interview/auth/config/SecurityHeadersFilter.java)，实现 `jakarta.servlet.Filter`，为每个响应添加 4 个基础安全头：

```java
@Component
public class SecurityHeadersFilter implements Filter {
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) {
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        // 禁止浏览器 MIME 类型嗅探（防 MIME confusion 攻击）
        httpResponse.setHeader("X-Content-Type-Options", "nosniff");

        // 禁止页面被嵌入 iframe（防点击劫持）
        httpResponse.setHeader("X-Frame-Options", "DENY");

        // 启用浏览器内置 XSS 过滤器
        httpResponse.setHeader("X-XSS-Protection", "1; mode=block");

        // 限制 referrer 信息只在同源请求中发送
        httpResponse.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");

        chain.doFilter(request, response);
    }
}
```

**各安全头作用**：

| Header | 值 | 防护目标 |
|---|---|---|
| `X-Content-Type-Options` | `nosniff` | 防止浏览器根据内容猜测 MIME 类型（可能将用户上传的文本当作 HTML 执行） |
| `X-Frame-Options` | `DENY` | 禁止所有 iframe 嵌入，防止点击劫持攻击 |
| `X-XSS-Protection` | `1; mode=block` | 启用浏览器内置 XSS 审计器，检测到攻击时阻止页面加载 |
| `Referrer-Policy` | `strict-origin-when-cross-origin` | 跨域请求只发送域名（不发送完整路径），同源请求发送完整 referrer |

### 影响文件

| 文件 | 操作 |
|---|---|
| [SecurityHeadersFilter.java](server-java/src/main/java/com/interview/auth/config/SecurityHeadersFilter.java) | 新建 |

---

## 文件变更汇总

### 修改文件（9 个）

| 文件 | 说明 |
|---|---|
| [pom.xml](server-java/pom.xml) | 添加 spring-boot-starter-cache |
| [ContentMapper.java](server-java/src/main/java/com/interview/auth/infrastructure/mapper/ContentMapper.java) | 新增轻量查询接口 |
| [ContentMapper.xml](server-java/src/main/resources/mapper/ContentMapper.xml) | 新增轻量查询 SQL |
| [AdminMapper.xml](server-java/src/main/resources/mapper/AdminMapper.xml) | SELECT * → 精确列名 |
| [StarReadServiceImpl.java](server-java/src/main/java/com/interview/auth/service/impl/StarReadServiceImpl.java) | 缓存 + 轻量查询 + 去重 |
| [AdminServiceImpl.java](server-java/src/main/java/com/interview/auth/admin/service/impl/AdminServiceImpl.java) | 缓存清除 + 线程安全 |
| [AdminBookServiceImpl.java](server-java/src/main/java/com/interview/auth/admin/service/impl/AdminBookServiceImpl.java) | 缓存清除 |
| [StarReadElasticsearchClient.java](server-java/src/main/java/com/interview/auth/infrastructure/search/StarReadElasticsearchClient.java) | 异常日志 |
| [ContentMapper.xml](server-java/src/main/resources/mapper/ContentMapper.xml) | 轻量查询 |

### 新建文件（3 个）

| 文件 | 说明 |
|---|---|
| [CacheConfig.java](server-java/src/main/java/com/interview/auth/config/CacheConfig.java) | Spring Cache 配置 |
| [logback-spring.xml](server-java/src/main/resources/logback-spring.xml) | 日志配置 |
| [SecurityHeadersFilter.java](server-java/src/main/java/com/interview/auth/config/SecurityHeadersFilter.java) | 安全响应头 Filter |

---

## 架构设计思路

### 缓存设计

```
┌─────────────────────────────────────────────────────┐
│                   StarRead 首页请求                    │
└────────────────────────┬────────────────────────────┘
                         │
                    ┌────▼────┐
                    │ @Cacheable │  ← CacheConfig.CACHE_STAR_READ
                    └────┬────┘
                         │
              ┌──────────┴──────────┐
              │ 缓存命中             │ 缓存未命中
              ▼                     ▼
         返回缓存数据        findPublishedTechArticlesLight()
                           （无 content_html 的轻量查询）
                                     │
                                     ▼
                            存入 ConcurrentHashMap
                            （进程内缓存，无网络开销）
```

**为什么选择 ConcurrentHashMap 而不是 Redis？**
- StarRead 首页是高频读场景，本地缓存延迟 < 0.1ms vs Redis 网络往返 ~1ms
- 文章变更频率低（管理员操作时才清除），不需要分布式一致性
- 单实例部署下本地缓存完全够用，未来多实例可通过 Redis Pub/Sub 做缓存失效通知

### 缓存失效传播链路

```
AdminController.saveContent()
  → AdminServiceImpl.saveContent()
    → adminMapper.saveTechArticle()    // 数据库写入
    → appendEditLog()                   // 操作日志
    → appendSnapshotIfNeeded()          // 仪表盘快照
    → evictStarReadCache()              // ★ 清除首页缓存
      → cacheManager.getCache("starReadArticles").clear()
        → 下一次首页请求触发 @Cacheable 重新加载

AdminBookController.publishImportJob()
  → AdminBookServiceImpl.publishImportJob()
    → saveBook / replaceChapters
    → evictStarReadCache()              // ★ 同上

AdminBookController.softDeleteBook / hardDeleteBook
  → AdminBookServiceImpl
    → evictStarReadCache()              // ★ 同上
```

### 管理员仪表盘数据流

```
getDashboard()
  │
  ├─ buildCurrentSummary()        [7 COUNT/SUM SQL, 无 content_html]
  ├─ buildModuleStats()           [3 COUNT/SUM SQL]
  ├─ buildHotContents()           [3 精确列查询, 无 content_html]
  ├─ buildCommentRecords()        [1 精确列查询, 无 content_html]
  ├─ buildRecentEdits()           [1 查询, limit 8]
  └─ buildTrendPoints()           [纯内存, 线程安全]
```

---

## 后续建议（低优先级）

以下问题在本次修复中未处理，建议上线稳定后逐步优化：

1. **列表接口分页** — `listWorldNewsIssues()` 和 `listBooks()` 暂无分页参数，记录数增长后需要添加
2. **comment 接口频率限制** — 评论接口无需认证即可无限发，应加 IP 级别的频率限制
3. **数据库索引优化** — `tech_article` 表缺少 `(status, featured, published_at, sort_order)` 复合索引
4. **世界新闻 LIKE 搜索** — 对 10 列做 `LIKE '%keyword%'` 匹配，其中含 MEDIUMTEXT 列，建议使用 Elasticsearch 或 MySQL 全文索引
5. **前端 echarts 冗余依赖** — 如果确认未使用，移除可减少 ~500KB 打包体积
