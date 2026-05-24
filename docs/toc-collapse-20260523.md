# PeakStars Blog 文章目录多层级折叠

## 概述

技术文章详情页右侧目录在章节较多时，层次结构会超出侧边栏可视范围，低层级的标题直接被裁剪不可见。本次改造为目录引入了**树形多层级折叠系统**：h1 和 h2 标题各自拥有独立的折叠箭头，可根据实际层级深度灵活收起/展开子章节。目录溢出时自动折叠非活跃分支，保持当前阅读位置所在层级链完整可见。

---

## 目录

1. [数据模型：从平铺到树形](#1-数据模型从平铺到树形)
2. [模板结构：平铺渲染 + 祖先链判断](#2-模板结构平铺渲染--祖先链判断)
3. [折叠动画：max-height 过渡](#3-折叠动画max-height-过渡)
4. [智能自动折叠](#4-智能自动折叠)
5. [交互设计](#5-交互设计)

---

## 1. 数据模型：从平铺到树形

### 旧模型

```
outlineItems = [
  { id, text, level: 'h1' },
  { id, text, level: 'h2' },
  { id, text, level: 'h3' },
  ...
]
```

平铺数组，只有 level 字段区分层级，**没有父子关系信息**。

### 新模型

采用**带祖先链的平铺节点**（flat tree with parentKeys）。每个节点记录自己的所有祖先 key，同时保留 children 引用用于折叠逻辑：

```
outlineTree = [
  { key, item, children: [...], parentKeys: [] },        // h1，无祖先
  { key, item, children: [...], parentKeys: [h1.key] },  // h2，祖先为 h1
  { key, item, children: [], parentKeys: [h2.key, h1.key] }, // h3，祖先为 h2 + h1
  ...
]
```

### 核心代码

**`TechArticleDetail.vue` — `outlineTree` 计算属性**

```javascript
const outlineTree = computed(() => {
  const items = outlineItems.value
  if (!items.length) return []

  const flat = []
  let currentH1 = null
  let currentH2 = null

  for (const item of items) {
    const level = item.level
    if (level === 'h1') {
      currentH1 = { key: item.id, item, children: [], parentKeys: [] }
      currentH2 = null
      flat.push(currentH1)
    } else if (level === 'h2') {
      currentH2 = { key: item.id, item, children: [], parentKeys: [] }
      if (currentH1) {
        currentH2.parentKeys = [currentH1.key]
        currentH1.children.push(currentH2)
      }
      flat.push(currentH2)
    } else {
      const parentKeys = []
      if (currentH2) parentKeys.push(currentH2.key)
      if (currentH1) parentKeys.push(currentH1.key)
      const node = { key: item.id, item, children: [], parentKeys }
      if (currentH2) {
        currentH2.children.push(node)
      } else if (currentH1) {
        currentH1.children.push(node)
      }
      flat.push(node)
    }
  }

  return flat
})
```

**设计要点：**

- `parentKeys` 是一个从近到远的祖先 key 列表。例如 h3 标题的 `parentKeys = [h2.key, h1.key]`。这用于判断「我的哪个祖先被折叠了，我是否应该隐藏」
- `children` 保留，用于判断「这个节点是否有子级需要折叠箭头」以及「收起全部时哪些节点是可折叠目标」
- 平铺输出（`flat` 数组）而非嵌套结构，让模板可以用单个 `v-for` 渲染，避免递归组件
- `currentH1` 和 `currentH2` 是游标变量，在遍历过程中维护当前的父子挂载点

### 层级关系图

```
outlineItems 输入                    outlineTree 输出（平铺 + 祖先链）
─────────────────                   ──────────────────────────────
h1 "一、架构设计"        →    { key: K1, parentKeys: [],      children: [K2] }
  h2 "1.1 整体架构"     →    { key: K2, parentKeys: [K1],    children: [K3, K4] }
    h3 "请求链路"       →    { key: K3, parentKeys: [K2,K1], children: [] }
    h3 "响应链路"       →    { key: K4, parentKeys: [K2,K1], children: [] }
  h2 "1.2 技术选型"     →    { key: K5, parentKeys: [K1],    children: [] }
h1 "二、核心模块"        →    { key: K6, parentKeys: [],      children: [K7] }
  h2 "2.1 认证模块"     →    { key: K7, parentKeys: [K6],    children: [K8] }
    h3 "Token 生成"     →    { key: K8, parentKeys: [K7,K6], children: [] }
```

---

## 2. 模板结构：平铺渲染 + 祖先链判断

模板使用单个 `v-for` 遍历 `outlineTree` 平铺数组，每个节点的显隐由 `isNodeCollapsed(node)` 判断：

```html
<ul class="article-toc-list">
  <li
    v-for="node in outlineTree"
    :key="node.key"
    class="article-toc-item toc-tree-node"
    :class="[
      node.item.level,
      `toc-depth-${node.parentKeys.length}`,
      {
        active: activeOutlineId === node.item.id,
        collapsed: isNodeCollapsed(node),
        'toc-has-children': node.children.length
      }
    ]"
  >
    <button
      type="button"
      class="article-toc-node-btn"
      :class="{ 'toc-parent': node.children.length }"
      @click="scrollToHeading(node.item.id)"
    >
      <span
        v-if="node.children.length"
        class="article-toc-chevron"
        :class="{ collapsed: collapsedNodes.has(node.key) }"
        @click.stop="toggleNode(node.key)"
      >
        <svg><!-- 箭头图标 --></svg>
      </span>
      <span class="article-toc-text">{{ node.item.text }}</span>
    </button>
  </li>
</ul>
```

**设计要点：**

- `node.children.length > 0` 时渲染折叠箭头，h1 和 h2 只要有自己的子级都会出现箭头
- `isNodeCollapsed(node)` 检查 `node.parentKeys` 中是否有任意祖先的 key 在 `collapsedNodes` 集合中。这是一个传递性判断：h1 被收起时，其下所有 h2 和 h3 的 `parentKeys` 都包含该 h1 的 key，全部返回 `true`
- `toc-depth-N` 的 N 等于 `parentKeys.length`，表示节点在树中的深度。模板中没有使用它做缩进（缩进由原有的 `.h1/.h2/.h3` 控制），预留用于未来差异化样式

**`isNodeCollapsed` 实现：**

```javascript
function isNodeCollapsed(node) {
  return node.parentKeys.some(k => collapsedNodes.value.has(k))
}
```

这段逻辑是整个折叠系统的核心：**只要任意一级祖先被折叠，后代节点就不可见**。因为 `parentKeys` 包含所有祖先，所以无论是 h1 折叠（h2 和 h3 都不可见）还是 h2 折叠（仅 h3 不可见），都通过这一条规则正确表达。

---

## 3. 折叠动画：max-height 过渡

### 为什么用 max-height 而非 v-if

`v-if` 会直接移除 DOM 元素，无法添加过渡动画。`max-height` 过渡是纯 CSS 方案，元素始终存在于 DOM 中，通过 `max-height: 0` + `overflow: hidden` 实现视觉上的消失。

### CSS 实现

```css
/* 所有树节点都有过渡属性 */
.toc-tree-node {
  max-height: 6rem;
  overflow: hidden;
  transition:
    max-height 0.35s cubic-bezier(0.4, 0, 0.2, 1),
    opacity 0.25s ease,
    padding-top 0.3s ease,
    padding-bottom 0.3s ease;
}

/* 折叠态：高度归零 + 透明 + 不可点击 */
.toc-tree-node.collapsed {
  max-height: 0;
  opacity: 0;
  padding-top: 0;
  padding-bottom: 0;
  border-left-color: transparent;
  pointer-events: none;
}
```

**设计要点：**

- `max-height: 6rem` 是展开态的上限。6rem 足以容纳 3 行中文标题（侧边栏宽 280px，0.8rem 字体约 21 字/行），正常 1-2 行的标题远小于此值，不会触发裁剪
- 过渡使用 `cubic-bezier(0.4, 0, 0.2, 1)`（Material Design 标准缓出曲线），收起时先快后慢，展开时先慢后快
- `padding-top / padding-bottom` 也参与过渡，避免文本消失后空白区域残留
- `pointer-events: none` 防止折叠状态下的元素拦截鼠标事件
- `border-left-color: transparent` 隐藏左侧高亮条，与 padding 同步过渡

### 箭头旋转动画

```css
.article-toc-chevron {
  transition: transform 0.25s cubic-bezier(0.4, 0, 0.2, 1);
}

.article-toc-chevron.collapsed {
  transform: rotate(-90deg);  /* 向右旋转 90°，由 ▼ 变为 ▶ */
}
```

收起的 h1/h2 显示为右箭头 `▶`，展开显示为下箭头 `▼`。

---

## 4. 智能自动折叠

### 触发条件

文章加载完成、目录 DOM 渲染后，计算 TOC 列表的实际高度与侧边栏可用空间：

```javascript
async function autoCollapseOverflow() {
  await nextTick()
  const tocList = document.querySelector('.article-toc-list')
  const sidebar = document.querySelector('.article-sidebar')
  if (!tocList || !sidebar) return

  const nodesWithChildren = collapsibleNodes.value
  if (!nodesWithChildren.length) return

  // 为标题、系列相关区块等预留约 260px
  const reserved = 260
  const available = sidebar.clientHeight - reserved
  if (tocList.scrollHeight <= available) return   // 未溢出，不操作

  // 找出活跃节点及其全部祖先
  const activeKeys = new Set()
  for (const node of outlineTree.value) {
    if (node.item.id === activeOutlineId.value) {
      activeKeys.add(node.key)
      for (const pk of node.parentKeys) activeKeys.add(pk)
      break
    }
  }

  // 折叠所有非活跃分支
  for (const node of nodesWithChildren) {
    if (!activeKeys.has(node.key)) {
      collapsedNodes.value.add(node.key)
    }
  }
  collapsedNodes.value = new Set(collapsedNodes.value)
}
```

**设计要点：**

- 预留 260px 给目录标题和系列相关区块，剩余空间用于 TOC 列表
- `activeKeys` 收集当前阅读位置标题及其所有祖先。例如用户正在读 h3 "请求链路"，则 `activeKeys = {h3.key, h2.key, h1.key}`——整条祖先链都不会被折叠
- 只折叠非活跃的**有子级节点**。叶子节点（没有 children 的标题）不是折叠目标，它们通过祖先折叠间接隐藏
- 调用时机：`syncOutline()` → `updateScrollState()` → `autoCollapseOverflow()`，确保 `activeOutlineId` 已被设置

### 文章切换时的重置

每次重建目录时清空折叠状态，确保新文章以全展开状态呈现，然后由 `autoCollapseOverflow` 根据需要自动折叠：

```javascript
function syncOutline() {
  // ...
  collapsedNodes.value = new Set()    // 重置所有折叠状态
  // ...
}
```

---

## 5. 交互设计

### 分层级的独立折叠

每一级标题的折叠作用域不同：

```
▼ h1 "一、架构设计"          ← 收起：隐藏其下所有 h2 + h3
  ▼ h2 "1.1 整体架构"        ← 收起：只隐藏自己的 h3
    h3 "请求链路"
    h3 "响应链路"
  ▶ h2 "1.2 技术选型"        ← 已收起，h3 不可见
    h3 "缓存策略"            (隐藏)
▶ h1 "二、核心模块"           ← 已收起，h2 + h3 全部不可见
  h2 "2.1 认证模块"          (隐藏)
```

### 全局操控

目录标题旁的「收起全部」按钮遍历所有有子级的节点并折叠。当全部已折叠时，按钮文案切换为「展开全部」：

```javascript
const collapsibleNodes = computed(() =>
  outlineTree.value.filter(n => n.children.length)
)

const allNodesCollapsed = computed(() => {
  const list = collapsibleNodes.value
  if (!list.length) return false
  return list.every(n => collapsedNodes.value.has(n.key))
})

function toggleAllNodes() {
  if (allNodesCollapsed.value) {
    collapsedNodes.value = new Set()             // 全部展开
  } else {
    collapsedNodes.value = new Set(collapsibleNodes.value.map(n => n.key))  // 全部折叠
  }
}
```

### 边界情况处理

| 场景 | 行为 |
|------|------|
| 文章无 h1，只有 h2 + h3 | `parentKeys` 正确计算（h2 的 parentKeys 为空），h2 作为顶层节点仍可折叠自己的 h3 |
| 文章只有 h1，无 h2/h3 | 所有节点的 `children` 为空数组，不显示箭头和「收起全部」按钮 |
| h2 出现在 h1 之前 | 该 h2 的 `parentKeys` 为空，作为独立顶层节点渲染 |
| h3 出现在 h2 之前（直属 h1） | `parentKeys = [h1.key]`，h1 折叠时该 h3 也会隐藏 |
| 目录未溢出 | `autoCollapseOverflow` 不做任何操作，保持全展开 |

---

## 修改文件清单

| 文件 | 变更内容 |
|------|---------|
| `src/views/TechArticleDetail.vue` | 新增 `outlineTree`、`collapsibleNodes`、`allNodesCollapsed` 计算属性；新增 `collapsedNodes` ref、`isNodeCollapsed()`、`toggleNode()`、`toggleAllNodes()`、`autoCollapseOverflow()` 函数；模板改为树形平铺渲染 |
| `src/styles/views/TechArticleDetail.css` | 新增 `.toc-tree-node` 折叠动画、`.article-toc-chevron` 箭头旋转、`.article-toc-collapse-all` 全局按钮、`.article-toc-node-btn` flex 布局样式 |

### 删除的旧代码

- `outlineGroups` 计算属性（h1 单层分组模型）
- `allGroupsCollapsed` 计算属性
- `toggleGroup()` / `toggleAllGroups()` 函数
- 模板中的 `<template v-for="group in outlineGroups">` 双层渲染结构
