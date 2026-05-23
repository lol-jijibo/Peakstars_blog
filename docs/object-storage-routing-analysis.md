# 对象存储按业务分流改造解析

## 1. 这次改造到底在解决什么

这次需求表面上是：

1. 面经页面图片全部走 `MinIO`
2. 书籍模块图片全部走 `阿里云 OSS`

但真正要解决的问题其实是：

**系统以前只知道“上传文件”，不知道“这次上传属于哪个业务”。**

这会带来两个直接后果：

1. 图片传到哪套存储，更多取决于当前默认配置，而不是业务意图。
2. 后台上传成功，不代表前台一定能按同一套规则把图展示出来。

所以这次不是简单换存储地址，而是把“资源该进哪个对象存储”这件事，正式上升成业务规则。

## 2. 核心设计：把存储选择权交给业务模块

这次改造最核心的思路只有一句话：

**由 `moduleType` 决定对象存储，而不是由默认实现决定对象存储。**

最终落下来的规则是：

| 模块标识 | 存储目标 |
| --- | --- |
| `interview` | MinIO |
| `interview-rich-text` | MinIO |
| `interview-cover` | MinIO |
| `book` | OSS |
| `book-cover` | OSS |
| `book-import` | OSS |
| `book-source` | OSS |

这样之后，系统不再是“谁启用就用谁”，而是：

1. 面经相关资源天然属于 MinIO
2. 书籍相关资源天然属于 OSS
3. 同一业务在任何入口下都走同一套存储规则

## 3. 关键代码一：统一存储路由器

这次新增的核心类就是存储路由器，它把所有分流规则收口到了一个地方。

```java
public ContentStorageService resolveForModule(String moduleType) {
    StorageTarget target = resolveTarget(moduleType);
    return switch (target) {
        case MINIO -> requireStorage(minioStorageProvider.getIfAvailable(), "MinIO");
        case OSS -> requireStorage(ossStorageProvider.getIfAvailable(), "阿里云 OSS");
        case AUTO -> requireStorage(contentStorageProvider.getIfAvailable(), "默认对象存储");
    };
}

public StorageTarget resolveTarget(String moduleType) {
    String normalized = moduleType == null ? "" : moduleType.trim().toLowerCase(Locale.ROOT);
    return switch (normalized) {
        case "interview", "interview-rich-text", "interview-cover" -> StorageTarget.MINIO;
        case "book", "book-cover", "book-rich-text", "book-import", "book-source" -> StorageTarget.OSS;
        default -> StorageTarget.AUTO;
    };
}
```

这段代码的业务价值很高：

1. 前后端只需要约定 `moduleType`
2. 所有上传入口都不必再各自维护一套 `if-else`
3. 以后排查“为什么这张图进了 OSS”时，只需要看这一张路由表

这次改造最重要的亮点，其实不是接了 MinIO 或 OSS，而是**建立了统一、可解释的业务分流规则**。

## 4. 关键代码二：前端把业务身份显式传给后端

以前上传接口只传文件，现在上传接口会多带一个 `moduleType`。

```js
export async function uploadCoverImage(file, moduleType = '') {
  const formData = new FormData()
  formData.append('file', file)
  if (moduleType) {
    formData.append('moduleType', moduleType)
  }

  const response = await fetch(`${BASE_URL}/api/admin/upload/cover`, {
    method: 'POST',
    body: formData
  })

  const payload = await response.json()
  if (!response.ok || payload.code !== 200) {
    throw new Error(payload.message || '封面图片上传失败')
  }
  return payload.data
}
```

面经正文编辑器也会显式声明自己的资源身份：

```vue
<AdminRichEditor
  v-model="draftForm.contentHtml"
  storage-type="interview-rich-text"
  placeholder="请输入正文内容"
/>
```

面经封面上传则单独传 `interview-cover`：

```js
const result = await uploadCoverImage(
  file,
  currentType.value === 'interview' ? 'interview-cover' : currentType.value
)
```

这三段代码加在一起，完成了一个关键转变：

1. 前端不再只是“上传图片”
2. 前端开始明确告诉后端“这张图属于哪个业务模块”
3. 后端才能稳定把资源送进正确的对象存储

这一步是整次改造能落地的前提。

## 5. 关键代码三：上传入口真正接入存储路由

后端控制器接住 `moduleType` 之后，会继续交给服务层。

```java
@PostMapping("/upload/cover")
public ApiResponse<Map<String, String>> uploadCoverImage(
    @RequestParam("file") MultipartFile file,
    @RequestParam(value = "moduleType", required = false) String moduleType
) {
    String url = adminService.uploadCoverImage(
        moduleType,
        file.getOriginalFilename(),
        file.getInputStream(),
        file.getSize(),
        file.getContentType()
    );
    return ApiResponse.success(Map.of("url", url));
}
```

服务层的核心实现非常直接：

```java
@Override
public String uploadCoverImage(String moduleType, String fileName, InputStream inputStream, long size, String contentType) throws Exception {
    return storageRoutingService.resolveForModule(moduleType).upload("cover", fileName, inputStream, size, contentType);
}

@Override
public String uploadRichTextImage(String moduleType, String fileName, InputStream inputStream, long size, String contentType) throws Exception {
    return storageRoutingService.resolveForModule(moduleType).upload("rich-text", fileName, inputStream, size, contentType);
}
```

这里体现了两个非常重要的业务设计：

1. **由模块决定存储**
2. **由资源类型决定目录**

也就是说：

1. `interview-cover` 和 `book-cover` 都是封面图，但落在不同存储
2. `cover` 和 `rich-text` 又会在各自存储内部继续分目录

所以这次不是简单二选一，而是同时完成了：

1. 模块级分流
2. 资源级分层

## 6. 书籍模块为什么要单独处理得更重

书籍模块是这次改造里最复杂的部分，因为它不是单次上传，而是一整条资源解析链路。

书籍链路的真实过程是：

1. 上传一本 EPUB / ZIP / PDF / TXT
2. 先保存源文件
3. 再解析封面
4. 再解析章节内容
5. 再解析章节里的图片
6. 把这些资源重新上传成可访问的受管地址

所以书籍模块不能只改一个上传 API，而是要把“源文件、封面、正文图片”拆开处理。

### 6.1 源文件先走 `book-import`

```java
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
    // 后续省略：保存源文件、解析章节与封面
}
```

这段代码表达的是：

1. 书籍原始文件不是普通图片资源
2. 它应该先进入书籍导入专属存储
3. 后面所有封面和章节图，都是在这个基础上继续拆出来的

这个设计的亮点是：**源文件和展示资源从一开始就分开治理。**

### 6.2 书籍封面固定走 `book-cover`

```java
private String uploadEpubCoverResource(Resource coverImage, String fileName) throws Exception {
    if (coverImage == null) {
        return "";
    }
    byte[] coverBytes = coverImage.getData();
    String contentType = coverImage.getMediaType() == null ? "" : coverImage.getMediaType().getName();
    if (!isValidCoverImageResource(coverImage.getHref(), contentType, coverBytes)) {
        return "";
    }
    return storageRoutingService.resolveForModule("book-cover").upload(
        "book/cover",
        coverImage.getHref(),
        new ByteArrayInputStream(coverBytes),
        coverBytes.length,
        defaultString(contentType, guessContentType(coverImage.getHref()))
    );
}
```

它解决的不是简单“上传一张封面”，而是：

1. EPUB 里的封面资源可能命名不规范
2. 甚至可能不是标准封面
3. 所以上传前要先校验
4. 校验通过后再路由到 `book-cover`

也就是说，书籍封面不是“拿到就传”，而是“识别后再传”。

### 6.3 章节图片固定走 `book`

```java
return storageRoutingService.resolveForModule("book").upload(
    "book/content/" + defaultString(jobKey, "epub"),
    fileName,
    new ByteArrayInputStream(bytes),
    bytes.length,
    contentType
);
```

这段代码是修复“导入中心能解析，但用户端首章还是破图”的关键。

因为它做的事情不是继续引用 EPUB 包内的相对路径，而是：

1. 把章节图片真正提取出来
2. 重新上传到 OSS
3. 生成新的受管资源地址
4. 再由前台按这个新地址展示

这样图片展示就不再依赖原始文件包内部结构，稳定性会高很多。

## 7. 导入预处理链路为什么也必须改

除了“用户主动上传”，系统里还有一大类资源来自“导入时自动迁移”。

这条链路最关键的方法是：

```java
private AssetMigrationResult migrateAsset(
    String contentType,
    String originalUrl,
    String sourceUrl,
    String assetType,
    boolean migrateAssets
) throws Exception {
    String resolvedUrl = resolveResourceUrl(originalUrl, sourceUrl);
    ContentStorageService storageService = resolveStorageService(contentType);

    if (!shouldMigrateAsset(migrateAssets, storageService, resolvedUrl)) {
        return new AssetMigrationResult(originalUrl, resolvedUrl, "skipped", "资源迁移已跳过");
    }

    DownloadedAsset asset = resolvedUrl.startsWith("data:")
        ? decodeDataUrl(resolvedUrl, assetType)
        : downloadRemoteAsset(resolvedUrl, assetType);

    validateAsset(asset, assetType, resolvedUrl);
    String targetUrl = storageService.upload(
        buildObjectPrefix(contentType, assetType),
        asset.fileName(),
        asset.inputStream(),
        asset.size(),
        asset.contentType()
    );
    return new AssetMigrationResult(originalUrl, targetUrl, "migrated", "资源已迁移到对象存储");
}
```

它真正解决的是：

1. 外链图片
2. 相对路径图片
3. base64 图片
4. 导入内容中的附件资源

统一迁移成站内受管资源。

导入阶段的存储选择则是：

```java
private ContentStorageService resolveStorageService(String contentType) {
    String normalized = defaultString(contentType, "").trim().toLowerCase(Locale.ROOT);
    if ("book".equals(normalized)) {
        return storageRoutingService.resolveForModule("book");
    }
    ContentStorageService storageService = storageServiceProvider.getIfAvailable();
    return storageService != null ? storageService : storageRoutingService.resolveForModule("interview");
}
```

这个规则很关键：

1. 书籍导入内容，明确走 OSS
2. 普通内容导入，尽量保持旧链路兼容
3. 这样书籍和面经两条链路都不会互相污染

## 8. 为什么展示层还要补兼容识别

如果只改上传，不改展示识别，仍然会出问题。

因为现在系统里会同时存在：

1. MinIO 资源
2. OSS 资源
3. 历史默认存储资源

所以展示层新增了统一判断：

```java
private boolean isManagedStorageUrl(String assetUrl) {
    if (assetUrl == null || assetUrl.isBlank()) {
        return false;
    }
    return storageRoutingService.resolveForModule("interview").isStorageUrl(assetUrl)
        || storageRoutingService.resolveForModule("book").isStorageUrl(assetUrl)
        || contentStorageService.isStorageUrl(assetUrl);
}
```

这段代码的业务意义是：

1. 新资源不会被误判成外链
2. 历史资源也不会被错误处理
3. MinIO 和 OSS 可以在过渡期共存

这一步保证的是“展示稳定性”，不是“上传稳定性”。

## 9. 这次改造最值得分享的亮点

如果只提最核心的几个亮点，我会总结成下面四条：

1. **把对象存储选择从配置问题变成业务问题**
   以前是谁启用就用谁，现在是面经天然进 MinIO，书籍天然进 OSS。

2. **建立了统一的存储路由层**
   所有分流规则统一收口，避免多个业务模块各写一套判断。

3. **把上传链路、导入链路、展示链路一起打通**
   不是只修编辑器上传，而是把书籍导入、章节图、封面图、资源迁移一起纳入治理。

4. **对复杂业务数据做了真实兜底**
   特别是 EPUB / ZIP 这类不规范资源，不再直接信任原始路径，而是重新提取、重新上传、重新生成展示地址。

## 10. 一句话总结

这次改造的本质，不是“同时支持 MinIO 和 OSS”，而是：

**让系统第一次真正知道，每一张资源图到底属于哪个业务模块。**

只有这样，面经图片走 MinIO、书籍图片走 OSS，才不是偶然成功，而是可持续、可维护、可扩展的稳定能力。
