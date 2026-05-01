/**
 * 技术文章配置数据 —— 分类定义与作者推荐信息
 * 注意：仅包含页面结构所需的配置常量，不含文章正文内容。
 * 所有文章数据改由后端 MySQL 提供，此文件只作辅助展示用途。
 */

export const techArticleCategories = [
  { key: 'all', label: '全部文章', description: '全部分类内容混排展示，优先展示精选与最近发布的文章。' },
  { key: 'frontend', label: '前端工程', description: '围绕 Vue 3、React、TypeScript 等前端技术栈的深度实践。' },
  { key: 'backend', label: '后端架构', description: 'Node.js、Java、数据库、系统设计等后端领域的技术沉淀。' },
  { key: 'vip', label: 'VIP 专题', description: '深度长文与体系化专题内容，需要 VIP 权限查看。' },
  { key: 'history', label: '浏览记录', description: '你最近看过的文章会出现在这里，方便继续阅读。' },
  { key: 'collect', label: '收藏', description: '你收藏过的文章，方便快速回顾。' },
  { key: 'like', label: '喜欢', description: '你点赞过的文章，值得再次品读。' }
]

export const recommendedAuthors = [
  {
    name: 'PeakDepth',
    subtitle: '技术作者',
    initials: 'PD',
    accent: 'linear-gradient(135deg, #c84b2f, #f59e0b)'
  },
  {
    name: '码客星云',
    subtitle: '全栈架构师',
    initials: 'MK',
    accent: 'linear-gradient(135deg, #1d4ed8, #0891b2)'
  },
  {
    name: 'TechLead',
    subtitle: '技术负责人',
    initials: 'TL',
    accent: 'linear-gradient(135deg, #7c3aed, #a855f7)'
  }
]
