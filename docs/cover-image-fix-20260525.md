# 封面图片显示异常修复报告

> 修复日期：2026-05-25

---

## 一、问题背景

项目中出现两个封面图片异常：

1. **前台技术文章页**：部分文章的封面显示为站长的个人头像照片
2. **管理后台上传封面**：新增/编辑文章时无论选择哪张本地图片，封面预览始终显示站长头像

---

## 二、根因分析

### 2.1 Fallback 图片是头像副本

`public/peakstars-blog-icon.jpg` 与 `public/qq.jpg`（站长头像）的 MD5 哈希值完全一致：

```
peakstars-blog-icon.jpg  41ed61098d450ee09d900ee9399f716e
qq.jpg                   41ed61098d450ee09d900ee9399f716e
```

确认 `peakstars-blog-icon.jpg` 是头像文件的直接副本。

### 2.2 全局 Fallback 引用

项目中 **8 个文件、14 处**封面图片 `<img>` 标签在加载失败时回退到 `/peakstars-blog-icon.jpg`：

| 文件 | 场景 |
|------|------|
| `HomeView.vue` | 首页精选文章封面 fallback + `resolveCoverUrl()` |
| `TechArticleList.vue` | 文章列表封面 fallback |
| `Collect.vue` | 收藏页封面 fallback |
| `Like.vue` | 点赞页封面 fallback |
| `LearningRouteDetail.vue` | 学习路线封面 fallback |
| `LoginView.vue` | 登录页品牌图标 |
| `AdminDashboardView.vue` | 后台文章编辑封面预览 fallback |
| `AdminBookImportView.vue` | 后台书籍导入封面预览 fallback |
| `AdminBookOverviewView.vue` | 后台书籍总览封面 fallback + `sampleCoverMap` |

因此，当任何封面 URL 为空或图片加载失败时，浏览器回退显示的就是站长头像。

### 2.3 后台上传预览加载失败

后台新增文章时，封面处理流程为：

```
用户选择本地文件 → 上传至服务端 → 服务端返回 URL → <img> 加载该 URL → 加载失败 → @error → 显示 fallback
```

上传接口返回的 URL（如 `/uploads/cover/2026/05/25/uuid.jpg`）在浏览器中加载时出现问题，导致 `@error` 事件触发，回退到头像图片。

### 2.4 OSS 路由缺失

`StorageRoutingService` 的 OSS 路由表中只包含 `"tech-article"`、`"tech-article-cover"` 等完整名称，但前端 `AdminDashboardView.vue` 实际传递的 `moduleType` 是简写 `"tech"`（即 `currentType.value`），导致生产环境中文章封面上传无法正确路由到 OSS。

---

## 三、修复方案

### 3.1 封面上传即时本地预览（核心修复）

**问题**：上传完成后依赖服务端 URL 渲染预览，若 URL 不可访问则回退到 fallback。

**方案**：使用 `URL.createObjectURL()` 在文件选中瞬间生成本地 blob URL，立即在 `<img>` 中显示，上传在后台异步执行。

**修改文件**：
- `src/modules/admin/views/AdminDashboardView.vue`
- `src/modules/admin/views/AdminBookImportView.vue`

**具体改动**：

1. 新增 `localCoverPreviewUrl` ref，存储本地 blob URL
2. `handleCoverFileSelect()`：选中文件后立刻调用 `URL.createObjectURL(file)` 生成本地预览，再启动异步上传；上传成功后更新 `draftForm.coverUrl` 为服务端 URL（用于最终保存）
3. 模板 `<img :src>` 优先使用 `localCoverPreviewUrl`，回退到 `draftForm.coverUrl`
4. 新增 `handleCoverImageError()`：有本地预览时不执行 fallback，保证用户始终看到选中图片
5. 新增 `clearLocalCoverPreview()`：关闭弹窗、切换记录、组件卸载时通过 `URL.revokeObjectURL()` 释放 blob URL
6. `removeCover()`：同时清除服务端 URL 和本地 blob URL

```
修复后的流程：
用户选择文件 → createObjectURL() → 立即显示本地预览 ✅
              → 异步上传至服务端 → 更新 coverUrl（用于保存）
```

### 3.2 替换 Fallback 图标

- 新建 `public/peakstars-blog-icon.svg`（"PS" 品牌标识 SVG 图标，713 bytes）
- 删除 `public/peakstars-blog-icon.jpg`（头像副本）
- 全局替换 8 个文件中的 `/peakstars-blog-icon.jpg` → `/peakstars-blog-icon.svg`

### 3.3 修复 OSS 路由

在 `StorageRoutingService.resolveTarget()` 的 switch 语句中添加 `"tech"` 和 `"ai"`：

```java
case "tech", "tech-article", "tech-article-cover", "tech-article-rich-text",
     "ai",
     ...
```

确保前端传递 `moduleType='tech'` 时能正确匹配 OSS 路由。

---

## 四、涉及文件清单

### 前端
| 文件 | 改动 |
|------|------|
| `public/peakstars-blog-icon.svg` | 新建 |
| `public/peakstars-blog-icon.jpg` | 删除 |
| `src/views/HomeView.vue` | `.jpg` → `.svg` |
| `src/views/TechArticleList.vue` | `.jpg` → `.svg` |
| `src/views/Collect.vue` | `.jpg` → `.svg` |
| `src/views/Like.vue` | `.jpg` → `.svg` |
| `src/views/LearningRouteDetail.vue` | `.jpg` → `.svg` |
| `src/views/LoginView.vue` | `.jpg` → `.svg` |
| `src/modules/admin/views/AdminDashboardView.vue` | 本地预览 + `.svg` |
| `src/modules/admin/views/AdminBookImportView.vue` | 本地预览 + `.svg` |
| `src/modules/admin/views/AdminBookOverviewView.vue` | `.jpg` → `.svg` |

### 后端
| 文件 | 改动 |
|------|------|
| `server-java/.../storage/StorageRoutingService.java` | 添加 `"tech"`、`"ai"` OSS 路由 |

---

## 五、验证要点

1. 后台新建/编辑文章时，选择封面图片后预览**立即**显示选中的图片
2. 前台文章列表中，有有效封面的文章正常显示封面，无封面的文章显示 "PS" SVG 图标
3. 登录页品牌图标显示 "PS" SVG 图标
4. 生产环境中 OSS 配置启用时，文章封面上传正确写入 OSS
