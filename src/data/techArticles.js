export const techArticleCategories = [
  {
    key: 'all',
    label: '全部文章',
    caption: '最新沉淀',
    description: '把近期值得反复阅读的工程实战整理成一页。'
  },
  {
    key: 'frontend',
    label: '前端',
    caption: '交互与工程',
    description: '聚焦 Vue、组件拆分、渲染性能和体验设计。'
  },
  {
    key: 'backend',
    label: '后端',
    caption: '接口与架构',
    description: '覆盖服务分层、接口治理、稳定性和系统演进。'
  },
  {
    key: 'vip',
    label: 'VIP 文章',
    caption: '深度专题',
    description: '更完整的复盘、架构推演和长线方法论。'
  },
  {
    key: 'history',
    label: '历史',
    caption: '最近浏览',
    description: '延续你刚刚看过的内容，方便继续回看。'
  },
  {
    key: 'collect',
    label: '收藏',
    caption: '重点留存',
    description: '把高价值方案和代码思路集中放回手边。'
  },
  {
    key: 'like',
    label: '喜欢',
    caption: '高共鸣内容',
    description: '偏向你已经点赞过的技术主题和作者表达。'
  }
]

export const recommendedAuthors = [
  {
    id: 'fly',
    name: 'FLy 鹏程万里',
    subtitle: '国内网络安全研究员，专注于 Web 安全与工程复盘。',
    initials: 'FL',
    accent: 'linear-gradient(135deg, #2b6cb0, #0ea5e9)'
  },
  {
    id: 'francek',
    name: 'Francek Chen',
    subtitle: '长期记录分布式系统、IoT 数据链路与基础设施实践。',
    initials: 'FC',
    accent: 'linear-gradient(135deg, #7c3aed, #ec4899)'
  },
  {
    id: 'd-life',
    name: '科技 D 人生',
    subtitle: '金融科技领域技术负责人，关注云基础设施与架构稳定性。',
    initials: 'DL',
    accent: 'linear-gradient(135deg, #f97316, #facc15)'
  },
  {
    id: 'clean-code',
    name: '微笑很纯洁',
    subtitle: '分享真实项目中的代码整洁度、系统取舍和工程感觉。',
    initials: 'WC',
    accent: 'linear-gradient(135deg, #64748b, #cbd5e1)'
  },
  {
    id: 'algo-art',
    name: '算法与编程之美',
    subtitle: '从算法思维延展到业务建模，兼顾代码与表达。',
    initials: 'AB',
    accent: 'linear-gradient(135deg, #2563eb, #8b5cf6)'
  },
  {
    id: 'free-dev',
    name: '自由程序员',
    subtitle: '记录独立开发、效率系统与长期主义的技术写作。',
    initials: 'FP',
    accent: 'linear-gradient(135deg, #fb7185, #f59e0b)'
  }
]

export const techArticles = [
  {
    id: 'fastapi-middleware-advanced',
    category: 'backend',
    title: '开源模型应用落地-FastAPI-助力模型交互-进阶篇-中间件（四）',
    summary:
      '把复杂路由、鉴权、日志链路和参数校验收拢进中间件层，让模型调用链从“能跑”进入“可持续演进”。',
    essence:
      '重点拆开了中间件在推理请求里的位置，读完能直接把鉴权、请求追踪和异常处理接进现有服务。',
    highlights: ['模型接口统一鉴权', '请求追踪与日志落盘', '异常链路的统一兜底'],
    author: {
      name: 'fTiN CAPA',
      role: '后端工程 / 模型服务',
      initials: 'FC',
      accent: 'linear-gradient(135deg, #f59e0b, #fb7185)'
    },
    coverUrl: '/ChatGPT Image 2026年4月23日 18_37_46.png',
    publishedAt: '2026-04-24',
    readCount: 363,
    likeCount: 8,
    collectCount: 8,
    commentCount: 18,
    readTime: '8 min',
    isVip: true,
    isCollected: true,
    isLiked: true,
    inHistory: true,
    featured: true
  },
  {
    id: 'iotdb-advantage',
    category: 'backend',
    title: '【IoTDB】工业物联网时序数据库优选：Apache IoTDB 的显著优势',
    summary:
      '围绕高并发写入、冷热数据组织、查询吞吐和边缘设备接入，把时序数据库选型从概念对比推进到工程判断。',
    essence:
      '适合做 IoT 平台选型和 PoC 前的速读，能帮你快速抓住 IoTDB 真正拉开差距的几个点。',
    highlights: ['时序写入性能拆解', '冷热分层策略', '边缘数据接入路径'],
    author: {
      name: 'Francek Chen',
      role: 'IoT 基础设施',
      initials: 'FC',
      accent: 'linear-gradient(135deg, #7c3aed, #22d3ee)'
    },
    coverUrl: '/【哲风壁纸】公路-后视镜-城镇.png',
    publishedAt: '2026-04-22',
    readCount: 486,
    likeCount: 8,
    collectCount: 11,
    commentCount: 22,
    readTime: '10 min',
    isVip: false,
    isCollected: true,
    isLiked: false,
    inHistory: true,
    featured: true
  },
  {
    id: 'iot-protocol-engineering-tradeoff',
    category: 'backend',
    title: '物联网应用开发的协议选型与数据架构：工程落地中的真实取舍',
    summary:
      '把 MQTT、HTTP、私有协议与数据分层放到同一条交付链里看，直面系统复杂度是如何一步步堆出来的。',
    essence:
      '不是简单比较协议优缺点，而是从设备采集、链路抖动、存储分级和业务成本一起做取舍。',
    highlights: ['协议选型的业务边界', '数据分层与成本平衡', '设备侧到平台侧的链路设计'],
    author: {
      name: '互联网推荐官',
      role: '工业互联网架构',
      initials: 'IR',
      accent: 'linear-gradient(135deg, #0891b2, #34d399)'
    },
    coverUrl: '/【哲风壁纸】女孩-户外-旷野.png',
    publishedAt: '2026-04-21',
    readCount: 542,
    likeCount: 14,
    collectCount: 5,
    commentCount: 31,
    readTime: '9 min',
    isVip: false,
    isCollected: false,
    isLiked: true,
    inHistory: true,
    featured: true
  },
  {
    id: 'oiiotool-batch-image',
    category: 'frontend',
    title: 'C++ openimageio 工具：如何使用 oiiotool 进行图像批量处理',
    summary:
      '从命令行参数、批量 glob、跨平台路径差异到输出规范，整理一套图像批处理的可复用工作流。',
    essence:
      '如果你要做图像工具链或前端素材预处理，这篇非常实用，尤其是 Windows 和 macOS 的路径坑位总结。',
    highlights: ['批量文件处理技巧', '跨平台命令差异', '输出规范与脚本封装'],
    author: {
      name: '2301_81666021',
      role: '图像处理 / 工具链',
      initials: 'OI',
      accent: 'linear-gradient(135deg, #2563eb, #38bdf8)'
    },
    coverUrl: '/【哲风壁纸】夏日-晴天-氛围感.png',
    publishedAt: '2026-04-20',
    readCount: 417,
    likeCount: 10,
    collectCount: 12,
    commentCount: 16,
    readTime: '7 min',
    isVip: false,
    isCollected: true,
    isLiked: true,
    inHistory: false,
    featured: false
  },
  {
    id: 'vue-dashboard-experience',
    category: 'frontend',
    title: 'Vue 后台系统如何把列表页做得更顺手：从信息密度到交互反馈的细改',
    summary:
      '不只讨论组件封装，而是从真实业务页面的扫描效率、按钮层级和状态反馈入手，重做列表页体验。',
    essence:
      '很适合前端同学拿去对照自己的管理后台，看哪些地方总让人觉得“能用但不顺手”。',
    highlights: ['高频动作前置', '列表信息密度重排', '反馈状态统一'],
    author: {
      name: '自由程序员',
      role: '前端体验设计',
      initials: 'FP',
      accent: 'linear-gradient(135deg, #fb7185, #f97316)'
    },
    coverUrl: '/【哲风壁纸】侧脸-树木-欧阳娜娜.png',
    publishedAt: '2026-04-19',
    readCount: 628,
    likeCount: 22,
    collectCount: 18,
    commentCount: 46,
    readTime: '11 min',
    isVip: true,
    isCollected: true,
    isLiked: true,
    inHistory: false,
    featured: true
  },
  {
    id: 'springboot-boundary',
    category: 'backend',
    title: 'Spring Boot 接口边界治理：服务拆分之后，真正难的是责任归属',
    summary:
      '用一次接口风暴复盘说明服务拆分为什么常常越拆越乱，以及如何从边界命名、错误码和契约文档重新收束。',
    essence:
      '这篇写的是拆分之后的治理问题，尤其适合已经有多个服务、却总感觉职责交叉的团队。',
    highlights: ['服务边界收束', '错误码体系重建', '契约文档的长期维护'],
    author: {
      name: '科技 D 人生',
      role: '服务治理 / 金融科技',
      initials: 'DL',
      accent: 'linear-gradient(135deg, #f97316, #facc15)'
    },
    coverUrl: '/【哲风壁纸】xiaomiyu7-小米suv.png',
    publishedAt: '2026-04-18',
    readCount: 782,
    likeCount: 31,
    collectCount: 25,
    commentCount: 58,
    readTime: '13 min',
    isVip: true,
    isCollected: false,
    isLiked: true,
    inHistory: true,
    featured: false
  },
  {
    id: 'frontend-observability',
    category: 'frontend',
    title: '前端可观测性不是埋点越多越好：错误、性能与行为日志如何真正协同',
    summary:
      '从日志设计开始，把错误采集、性能指标和行为路径串起来，避免监控堆很多却仍然无法定位问题。',
    essence:
      '适合正在搭建前端监控体系的团队，文章把“该采什么”和“采了怎么用”讲得比较透。',
    highlights: ['错误与性能联动', '行为日志降噪', '排障路径设计'],
    author: {
      name: '微笑很纯洁',
      role: '前端监控 / 工程化',
      initials: 'WC',
      accent: 'linear-gradient(135deg, #64748b, #cbd5e1)'
    },
    coverUrl: '/【哲风壁纸】公路-后视镜-城镇.png',
    publishedAt: '2026-04-17',
    readCount: 519,
    likeCount: 19,
    collectCount: 20,
    commentCount: 27,
    readTime: '9 min',
    isVip: false,
    isCollected: true,
    isLiked: false,
    inHistory: false,
    featured: false
  },
  {
    id: 'architecture-retrospective',
    category: 'backend',
    title: '一次失败改造之后的架构复盘：为什么技术方案说服了大家，却没有说服系统',
    summary:
      '从目标错位、数据迁移策略和灰度发布节奏拆开一次失败改造，讲清楚方案正确不等于系统就能承受。',
    essence:
      '比较少见地把“失败的架构改造”讲透了，适合技术负责人和准备做大改的同学提前避坑。',
    highlights: ['改造目标校准', '数据迁移窗口设计', '灰度发布节奏控制'],
    author: {
      name: '算法与编程之美',
      role: '架构复盘 / 方法论',
      initials: 'AB',
      accent: 'linear-gradient(135deg, #2563eb, #8b5cf6)'
    },
    coverUrl: '/【哲风壁纸】女孩-户外-旷野.png',
    publishedAt: '2026-04-15',
    readCount: 901,
    likeCount: 43,
    collectCount: 37,
    commentCount: 64,
    readTime: '14 min',
    isVip: true,
    isCollected: false,
    isLiked: true,
    inHistory: true,
    featured: false
  }
]

export const techMegaHighlights = techArticles
  .filter((article) => article.featured)
  .slice(0, 4)
