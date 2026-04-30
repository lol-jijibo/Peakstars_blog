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
    summary: '',
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
  },
  {
    id: 'rust-ownership-system',
    category: 'backend',
    categoryLabel: '系统编程',
    title: '深入理解 Rust 所有权系统：内存安全的哲学',
    summary:
      '所有权不只是一个语言特性，它是一种关于资源管理的全新思维方式。文章从零开始拆解 Rust 最核心的设计决策。',
    subtitle:
      '所有权不只是一个语言特性——它是一种关于资源管理的全新思维方式。本文从零开始，逐层剥开 Rust 最核心的设计决策。',
    essence:
      '如果你总觉得 borrow checker 像在刁难人，这篇文章会把它背后的内存安全逻辑讲清楚。',
    highlights: ['所有权三条规则', '借用与可变引用', '生命周期标注'],
    tags: ['Rust', '系统编程', '内存安全', '所有权', '编译器', '并发'],
    series: 'Rust 深潜系列 · 第 3 期',
    authorIntro:
      '在分布式系统和底层性能优化领域耕耘 8 年，现专注于用 Rust 重构关键基础设施。相信清晰的思维比聪明的技巧更重要。',
    author: {
      name: '陈 Kai',
      role: '系统工程师 · Rust 布道者',
      initials: '陈K',
      accent: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)'
    },
    coverUrl: '',
    publishedAt: '2026-04-28',
    readCount: 4200,
    likeCount: 312,
    collectCount: 186,
    commentCount: 48,
    readTime: '12 min',
    followersCount: 11800,
    isVip: true,
    isCollected: false,
    isLiked: true,
    inHistory: false,
    featured: true,
    contentHtml: `
      <p class="lead">
        当你第一次遇到 Rust 的借用检查器（borrow checker）时，它看起来像一个严苛的守门人。但理解它的底层逻辑之后，你会发现这其实是编译器在帮你思考——在代码运行之前就替你排除了一整类错误。
      </p>

      <p>
        内存安全问题长期以来是系统编程的噩梦：use-after-free、双重释放、数据竞争……这些错误不仅难以复现，还往往成为安全漏洞的根源。C 和 C++ 将内存管理的责任完全交给程序员；垃圾回收语言用运行时开销换取安全；而 Rust 选择了第三条路：<strong>通过类型系统在编译期强制执行所有权规则</strong>。
      </p>

      <h2>所有权的三条规则</h2>

      <p>
        Rust 的所有权系统建立在三条简洁的规则之上。理解这三条规则，就理解了 Rust 内存管理的全部基础：
      </p>

      <div class="callout callout-info">
        <span class="callout-icon">📌</span>
        <p>
          <strong>规则一：</strong>每个值都有且仅有一个所有者（owner）。<br>
          <strong>规则二：</strong>同一时刻，一个值只能有一个所有者。<br>
          <strong>规则三：</strong>当所有者离开作用域，值被自动释放（drop）。
        </p>
      </div>

      <p>
        这三条规则听起来简单，但它们的组合效果非常强大。让我们通过代码来感受它们：
      </p>

      <div class="code-block">
        <div class="code-header">
          <div class="code-dots">
            <div class="dot dot-r"></div>
            <div class="dot dot-y"></div>
            <div class="dot dot-g"></div>
          </div>
          <span class="code-lang">Rust</span>
        </div>
        <pre><code><span class="kw">fn</span> <span class="fn">main</span>() {
    <span class="kw">let</span> s1 = <span class="ty">String</span>::<span class="fn">from</span>(<span class="st">"hello"</span>);  <span class="cm">// s1 是这个 String 的所有者</span>
    <span class="kw">let</span> s2 = s1;                      <span class="cm">// 所有权移交给 s2（move）</span>

    <span class="cm">// println!("{}", s1);          // ❌ 编译错误！s1 已经无效</span>
    <span class="fn">println!</span>(<span class="st">"{}"</span>, s2);               <span class="cm">// ✅ s2 有效</span>
}   <span class="cm">// s2 离开作用域，内存被自动释放</span></code></pre>
      </div>

      <p>
        注意第三行：<code>let s2 = s1</code> 执行的不是复制（copy），而是<strong>移动（move）</strong>。移动后，<code>s1</code> 不再有效。这正是所有权系统防止双重释放的方式——当作用域结束时，只有 <code>s2</code> 会被 drop。
      </p>

      <h2>借用：不转让所有权的访问</h2>

      <p>
        如果每次传递值都要转移所有权，代码会变得极其繁琐。Rust 提供了<strong>借用（borrowing）</strong>机制——通过引用来访问值，而不夺取所有权：
      </p>

      <div class="code-block">
        <div class="code-header">
          <div class="code-dots">
            <div class="dot dot-r"></div>
            <div class="dot dot-y"></div>
            <div class="dot dot-g"></div>
          </div>
          <span class="code-lang">Rust</span>
        </div>
        <pre><code><span class="kw">fn</span> <span class="fn">calculate_length</span>(s: <span class="lf">&</span><span class="ty">String</span>) -> <span class="ty">usize</span> {
    s.len()  <span class="cm">// 只是读取，不夺取所有权</span>
}

<span class="kw">fn</span> <span class="fn">main</span>() {
    <span class="kw">let</span> s1 = <span class="ty">String</span>::<span class="fn">from</span>(<span class="st">"hello"</span>);
    <span class="kw">let</span> len = <span class="fn">calculate_length</span>(<span class="lf">&</span>s1);   <span class="cm">// 传递引用</span>

    <span class="fn">println!</span>(<span class="st">"'{}' 的长度是 {}"</span>, s1, len);  <span class="cm">// s1 仍然有效！</span>
}</code></pre>
      </div>

      <h3>可变引用的独占性</h3>

      <p>
        借用规则中最精妙的一条是：<strong>在同一时刻，你只能有一个可变引用，或者任意多个不可变引用——两者不能共存。</strong>
      </p>

      <div class="article-image">
        <div class="image-placeholder">
          <div class="ownership-diagram">
            <div class="mem-block">
              <div class="mem-label">堆内存</div>
              <div class="mem-cell owned">ptr → "hello"</div>
              <div class="mem-cell owned">len: 5</div>
              <div class="mem-cell owned">cap: 5</div>
            </div>
            <div class="arrow-group">
              <div class="arrow-line"></div>
              <div class="arrow-tag">move</div>
            </div>
            <div class="mem-block">
              <div class="mem-label">栈: s2 (owner)</div>
              <div class="mem-cell owned">ptr</div>
              <div class="mem-cell owned">len: 5</div>
              <div class="mem-cell owned">cap: 5</div>
            </div>
            <div class="ownership-plus">+</div>
            <div class="mem-block">
              <div class="mem-label">栈: s1 (invalid)</div>
              <div class="mem-cell freed">ptr</div>
              <div class="mem-cell freed">len: 5</div>
              <div class="mem-cell freed">cap: 5</div>
            </div>
          </div>
        </div>
        <p class="image-caption">图 1 — 所有权移动后，原变量的栈数据失效，堆内存只由新所有者负责释放</p>
      </div>

      <p>
        这条规则在编译期消除了数据竞争（data race）的可能性。数据竞争需要：两个或以上指针同时访问同一数据，且至少一个在写入。Rust 的规则让这三个条件永远无法同时成立。
      </p>

      <blockquote>
        <p>"所有权系统最大的天才之处，在于它将一个运行时问题变成了一个编译期约束，而这个约束本身足够表达力强，不会成为正确程序的障碍。"</p>
        <cite>— Niko Matsakis，Rust 核心团队</cite>
      </blockquote>

      <h2>生命周期：让借用跨函数也安全</h2>

      <p>
        当引用跨越函数边界时，借用检查器需要额外的信息来验证安全性。这就是<strong>生命周期标注（lifetime annotations）</strong>的用途——它们不改变引用的实际存活时间，只是告诉编译器不同引用之间的关系：
      </p>

      <div class="code-block">
        <div class="code-header">
          <div class="code-dots">
            <div class="dot dot-r"></div>
            <div class="dot dot-y"></div>
            <div class="dot dot-g"></div>
          </div>
          <span class="code-lang">Rust</span>
        </div>
        <pre><code><span class="cm">// 'a 声明一个生命周期参数</span>
<span class="cm">// 返回的引用与两个参数中较短的那个同寿</span>
<span class="kw">fn</span> <span class="fn">longest</span><span class="lf">&lt;'a&gt;</span>(x: <span class="lf">&'a</span> <span class="ty">str</span>, y: <span class="lf">&'a</span> <span class="ty">str</span>) -> <span class="lf">&'a</span> <span class="ty">str</span> {
    <span class="kw">if</span> x.len() > y.len() { x } <span class="kw">else</span> { y }
}

<span class="kw">fn</span> <span class="fn">main</span>() {
    <span class="kw">let</span> s1 = <span class="ty">String</span>::<span class="fn">from</span>(<span class="st">"long string"</span>);
    <span class="kw">let</span> result;
    {
        <span class="kw">let</span> s2 = <span class="ty">String</span>::<span class="fn">from</span>(<span class="st">"xyz"</span>);
        result = <span class="fn">longest</span>(s1.as_str(), s2.as_str());
        <span class="fn">println!</span>(<span class="st">"最长的字符串是 {}"</span>, result);
    }   <span class="cm">// s2 在这里被释放，result 也随之无效</span>
}</code></pre>
      </div>

      <div class="callout callout-warn">
        <span class="callout-icon">⚡</span>
        <p>
          <strong>常见误解：</strong>生命周期标注不会让引用活得更长，它只是在编译器无法自行推断时，帮助它理解多个引用之间的约束关系。大多数情况下，Rust 可以通过<strong>生命周期省略规则</strong>自动推断，你不需要手动标注。
        </p>
      </div>

      <p>
        理解了所有权、借用和生命周期，你就掌握了 Rust 内存安全的核心。这三个概念互相配合，构建出一个在编译期就能捕获整类内存错误的系统。随着你写更多 Rust 代码，借用检查器会从「令人沮丧的障碍」变成「值得信赖的同伴」。
      </p>
    `
  }
]

export const techMegaHighlights = techArticles
  .filter((article) => article.featured)
  .slice(0, 4)
