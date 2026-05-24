# 技术文章分页展示

## 概述

为技术文章列表页添加服务端分页能力，默认每页展示 10 条，用户可自行切换每页条数（5 / 10 / 20 / 50）。同时将原有的客户端分类筛选（模式 + 分类）迁移至服务端，按需加载数据，降低首屏传输量。

---

## 涉及文件

### 后端（Java / MyBatis）

| 文件 | 改动说明 |
|---|---|
| `server-java/.../controller/ContentController.java` | `/api/content/tech-articles` 新增 `page`、`pageSize`、`category` 三个可选查询参数，返回值由 `List` 改为包含 `list`、`total`、`page`、`pageSize`、`categoryCounts` 的 Map 结构 |
| `server-java/.../service/ContentService.java` | `listTechArticles` 签名扩展为 `(Long, int, int, String)`，返回 `PageResult<TechArticleResponse>`；新增 `getTechArticleCategoryCounts` 方法 |
| `server-java/.../service/impl/ContentServiceImpl.java` | 实现分页 offset 计算、count + 分页查询、分类统计聚合；新增 `normalizeCategory` 归一化方法 |
| `server-java/.../mapper/ContentMapper.java` | 查询方法增加 `offset`、`size`、`category` 参数；新增 `countPublishedTechArticles`、`countPublishedTechArticlesByUser`、`countTechArticlesByCategory` |
| `server-java/.../resources/mapper/ContentMapper.xml` | 原有两条 SELECT 加上 `LIMIT #{size} OFFSET #{offset}`；新增两段 `<sql>` 动态分类过滤片段；新增 COUNT 查询；新增按分类统计的 UNION ALL 查询 |

### 前端（Vue 3 / JS）

| 文件 | 改动说明 |
|---|---|
| `src/api/content.js` | `getTechArticles()` 参数结构化为 `{ page, pageSize, category, forceRefresh }`；缓存 key 按参数组合区分，避免翻页时读到旧缓存；`loadWithCache` 支持动态 key 初始化 |
| `src/views/TechArticleList.vue` | 新增分页状态（`currentPage`、`pageSize`、`totalArticles`、`categoryCounts`）；新增每页条数下拉选择器与页码导航组件；分类/模式切换改为服务端查询并重置至第 0 页；移除客户端过滤逻辑 |
| `src/styles/views/TechArticleList.css` | 新增 `.article-stream-header`、`.article-pagination`、`.page-btn`、`.page-size-select` 样式及深色模式适配 |
| `src/views/TechArticleDetail.vue` | 适配新响应格式（解 `.list`），传 `pageSize: 999` 确保能找到任意文章 |
| `src/views/HomeView.vue` | 适配新响应格式（解 `.list`） |
| `src/views/Collect.vue` | 改用服务端 `category: 'collect'` 过滤，传 `pageSize: 999` 一次拿全 |
| `src/views/Like.vue` | 改用服务端 `category: 'like'` 过滤，传 `pageSize: 999` 一次拿全 |
| `src/views/Mine.vue` | 适配新响应格式（解 `.list`），传 `pageSize: 999` 一次拿全供客户端统计 |

---

## API 变更

### `GET /api/content/tech-articles`

**请求参数：**

| 参数 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `page` | int | `0` | 页码（0-based） |
| `pageSize` | int | `10` | 每页条数 |
| `category` | string | `"all"` | 分类筛选：`all` / `frontend` / `backend` / `project` / `vip` / `history` / `collect` / `like` |

**响应结构：**

```json
{
  "code": 0,
  "data": {
    "list": [ /* TechArticleResponse[] */ ],
    "total": 42,
    "page": 0,
    "pageSize": 10,
    "categoryCounts": {
      "all": 42,
      "frontend": 18,
      "backend": 12,
      "project": 8,
      "vip": 4,
      "history": 15,
      "collect": 6,
      "like": 9
    }
  }
}
```

---

## 前端交互

- **每页条数选择器**：文章列表顶部右侧下拉框，可选 5 / 10 / 20 / 50，切换后回到第 0 页
- **页码导航**：文章列表底部居中，最多展示 5 个页码按钮 + 上一页 / 下一页
- **分类筛选**：顶栏胶囊 + 模式胶囊按钮，点击后重置至第 0 页并重新请求，各胶囊上的计数来自服务端返回的 `categoryCounts`
- **深色模式**：所有新增 UI 均适配深色主题
