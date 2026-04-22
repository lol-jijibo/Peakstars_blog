-- ============================================================
-- 面经宝典 - 初始数据导入
-- 执行前请先运行 schema.sql
-- ============================================================

USE interview_db;

-- ------------------------------------------------------------
-- 分类
-- ------------------------------------------------------------
INSERT INTO `category` (`code`, `name`, `sort_order`) VALUES
('all',      '全部',      0),
('frontend', '前端',      1),
('java',     'Java后端',  2)
ON DUPLICATE KEY UPDATE `name` = VALUES(`name`);

-- ------------------------------------------------------------
-- 企业
-- ------------------------------------------------------------
INSERT INTO `company` (`name`, `short_name`, `avatar_text`, `avatar_color`, `description`) VALUES
('百度',     'BD',  '百', '#e87722', '全球最大的中文搜索引擎，AI技术领先企业'),
('字节跳动', 'ZJ',  '字', '#1d7cfe', '抖音、今日头条母公司，高速成长的互联网巨头'),
('阿里巴巴', 'ALI', '阿', '#ff6a00', '中国最大的电商平台，云计算领域领军企业'),
('腾讯',     'TX',  '腾', '#07c160', '社交和游戏领域领导者，微信、QQ母公司'),
('美团',     'MT',  '美', '#ffc400', '生活服务平台，外卖、酒旅领域市场份额第一'),
('网易',     'WY',  '网', '#c0392b', '游戏、教育、音乐等多元化互联网公司'),
('京东',     'JD',  '京', '#e1251b', '中国第二大电商平台，自营物流体系完善'),
('滴滴',     'DD',  '滴', '#ff6000', '中国最大的网约车平台，出行科技领先企业')
ON DUPLICATE KEY UPDATE `avatar_color` = VALUES(`avatar_color`);

-- ------------------------------------------------------------
-- 标签
-- ------------------------------------------------------------
INSERT IGNORE INTO `tag` (`name`) VALUES
('前端'),('Java'),('JavaScript'),('Vue'),('React'),
('ES6'),('算法'),('CSS'),('HTTP'),('浏览器原理'),
('TypeScript'),('Canvas'),('性能优化'),('工程化'),
('JVM'),('Spring'),('MySQL'),('Redis'),('分布式'),
('微服务'),('Elasticsearch'),('Webpack'),('Vue3'),
('地图'),('微前端'),('百度'),('字节跳动'),('阿里巴巴'),
('腾讯'),('美团'),('网易'),('京东'),('滴滴');

-- ------------------------------------------------------------
-- 面经数据
-- （使用变量避免硬编码ID，兼容 MySQL 5.7+）
-- ------------------------------------------------------------
SET @cat_frontend = (SELECT id FROM category WHERE code = 'frontend');
SET @cat_java     = (SELECT id FROM category WHERE code = 'java');
SET @co_baidu     = (SELECT id FROM company WHERE name = '百度');
SET @co_bytedance = (SELECT id FROM company WHERE name = '字节跳动');
SET @co_ali       = (SELECT id FROM company WHERE name = '阿里巴巴');
SET @co_tencent   = (SELECT id FROM company WHERE name = '腾讯');
SET @co_meituan   = (SELECT id FROM company WHERE name = '美团');
SET @co_netease   = (SELECT id FROM company WHERE name = '网易');
SET @co_jd        = (SELECT id FROM company WHERE name = '京东');
SET @co_didi      = (SELECT id FROM company WHERE name = '滴滴');

-- ① 百度前端面经
INSERT INTO `interview` (`company_id`, `category_id`, `title`, `author`, `summary`, `content`, `views`, `likes`, `collects`, `publish_date`) VALUES
(@co_baidu, @cat_frontend, '百度前端面经', '青春，那么骚',
'虽然百度这几年发展势头落后于AT，甚至快被京东赶上了，毕竟瘦死的骆驼比马大，面试还是相当有难度和水准的...',
'虽然百度这几年发展势头落后于AT，甚至快被京东赶上了，毕竟瘦死的骆驼比马大，面试还是相当有难度和水准的。

**一面**（1小时）

1. 询问你的项目经验、学习经历、主修语言（照实答）

2. 解释ES6的暂时性死区（let 和 var 的区别）
   - var 存在变量提升，let/const 不存在
   - let/const 存在暂时性死区（TDZ），声明前访问会抛出 ReferenceError
   - let/const 声明的变量不会挂载到 window 对象上

3. 箭头函数、闭包、异步（老生常谈，参见上文）
   - 箭头函数没有自己的 this，继承外层作用域的 this
   - 闭包是函数和其词法环境的组合
   - Promise、async/await 的使用和原理

4. 高阶函数（emmm......我真不太清楚这是啥，听起来挺像刨包的）
   - map、filter、reduce 的使用
   - 函数柯里化、函数组合

5. 求N的阶乘末尾有多少个0，在线编码代码或讲思路（求因数，统计2、5、10的个数）

6. 算法：两数之和（LeetCode 1），要求 O(n) 时间复杂度
   - 使用 Map 存储已遍历的元素

**二面**（45分钟）

1. 介绍做过最有成就感的项目，重点讲技术难点

2. HTTP 缓存机制
   - 强缓存：Cache-Control、Expires
   - 协商缓存：ETag/If-None-Match、Last-Modified/If-Modified-Since

3. Vue 响应式原理
   - Vue2：Object.defineProperty
   - Vue3：Proxy
   - 依赖收集与派发更新

4. 跨域问题解决方案
   - CORS、JSONP、代理服务器、Nginx 反向代理

5. 性能优化手段
   - 懒加载、代码分割、Tree Shaking
   - 图片压缩、CDN加速、HTTP/2

**HR面**

薪资待遇，工作强度，发展规划（正常聊即可）',
3152, 448, 89, '2024-03-15');

SET @iid1 = LAST_INSERT_ID();

-- ② 字节跳动前端面经
INSERT INTO `interview` (`company_id`, `category_id`, `title`, `author`, `summary`, `content`, `views`, `likes`, `collects`, `publish_date`) VALUES
(@co_bytedance, @cat_frontend, '字节跳动前端面经（三轮技术面）', '不风流怎样倜傥',
'笔者读大三，前端小白一枚，正在准备春招，人生第一次面试，投了头条前端，总共经历了四轮技术面...',
'笔者读大三，前端小白一枚，正在准备春招，人生第一次面试，投了头条前端，总共经历了四轮技术面，最终拿到了实习 offer，特此记录。

**一面**（60分钟）

1. 自我介绍 + 项目介绍

2. JavaScript 基础
   - 说说 JS 的数据类型，如何判断类型（typeof、instanceof、Object.prototype.toString）
   - 解释原型链，什么是原型，如何实现继承
   - 手写 instanceof

3. CSS 基础
   - BFC 是什么，如何触发，有什么用
   - flex 布局常用属性（flex-direction、justify-content、align-items、flex-wrap）
   - CSS 选择器优先级

4. 网络基础
   - 从输入 URL 到页面展示的过程
   - TCP 三次握手、四次挥手
   - HTTP 和 HTTPS 的区别

5. 算法：反转链表（LeetCode 206）

**二面**（50分钟）

1. Vue 核心原理
   - MVVM 模式理解
   - Vue 的生命周期（详细说说每个阶段）
   - computed 和 watch 的区别
   - v-if 和 v-show 的区别和使用场景
   - 组件通信方式（props/$emit、EventBus、Vuex、provide/inject）

2. 浏览器原理
   - 重绘和回流（reflow/repaint）的区别
   - 如何减少回流？
   - 事件循环机制（Event Loop）、宏任务和微任务

3. 手写 Promise.all

4. 算法：最长递增子序列

**三面**（45分钟）

1. 项目深挖，聊做过最复杂的功能点

2. 前端工程化
   - Webpack 构建流程
   - Babel 的作用
   - 模块化规范（CommonJS、ESModule 的区别）

3. 性能优化
   - 首屏加载优化方案
   - 虚拟列表的实现原理

4. 手写深拷贝（考虑循环引用）

**四面**（HR面）

聊职业规划、团队合作、对加班的看法',
3205, 432, 76, '2024-02-20');

SET @iid2 = LAST_INSERT_ID();

-- ③ 阿里巴巴 Java 后端面经
INSERT INTO `interview` (`company_id`, `category_id`, `title`, `author`, `summary`, `content`, `views`, `likes`, `collects`, `publish_date`) VALUES
(@co_ali, @cat_java, '阿里巴巴 Java 后端面经', '醉卧九天',
'历经五轮面试终于拿下阿里 P6 offer，Java基础、JVM、并发、Spring、分布式样样不落...',
'历经五轮面试终于拿下阿里 P6 offer，把面试过程完整记录下来供大家参考。

**一面**（90分钟，技术面）

**Java基础**
1. HashMap 的底层实现原理
   - 数组 + 链表 + 红黑树（JDK8+）
   - 扩容机制（负载因子0.75，扩容为2倍）
   - 线程不安全的原因（并发put可能死循环/数据覆盖）
   - ConcurrentHashMap 如何保证线程安全？

2. Java 内存模型（JMM）
   - 主内存与工作内存
   - volatile 的可见性和禁止指令重排
   - happens-before 原则

3. 多线程
   - synchronized 和 ReentrantLock 的区别
   - 线程池的核心参数（corePoolSize、maximumPoolSize、workQueue、handler）
   - 线程池拒绝策略有哪些？
   - AQS 的原理

**JVM**
4. JVM 内存结构（堆、方法区、虚拟机栈、本地方法栈、程序计数器）

5. 垃圾回收算法（标记清除、标记整理、复制算法）

6. GC Roots 有哪些

7. CMS 和 G1 的区别

**二面**（60分钟）

**框架与中间件**
1. Spring IOC 和 AOP 的原理
   - IOC：控制反转，依赖注入
   - AOP：动态代理（JDK动态代理 vs CGLIB）

2. Spring Boot 自动配置原理
   - @SpringBootApplication → @EnableAutoConfiguration
   - spring.factories 文件

3. MyBatis 的工作原理
   - SqlSession、Executor、MappedStatement

4. MySQL 索引
   - B+ 树索引的优势
   - 聚簇索引 vs 非聚簇索引
   - 联合索引最左前缀原则
   - 索引失效的场景

5. MySQL 事务隔离级别和 MVCC 原理

**三面**（系统设计）

1. 设计一个秒杀系统，如何应对高并发？
   - 前端：限流（令牌桶）、页面静态化
   - 应用层：接口幂等、异步处理（MQ）
   - 缓存层：Redis 预减库存
   - 数据库：分库分表、乐观锁

2. 如何设计一个分布式锁？（Redis实现：SETNX + Lua脚本 + 看门狗机制）

3. 消息队列 Kafka 的基本原理（Topic、Partition、Consumer Group）

**四面（交叉面）+ 五面（HR面）**

项目经历深挖 + 薪资谈判',
4183, 612, 134, '2024-01-10');

SET @iid3 = LAST_INSERT_ID();

-- ④ 腾讯前端面经
INSERT INTO `interview` (`company_id`, `category_id`, `title`, `author`, `summary`, `content`, `views`, `likes`, `collects`, `publish_date`) VALUES
(@co_tencent, @cat_frontend, '腾讯前端面经（社招）', '五行缺钱',
'腾讯社招前端，岗位是互动娱乐事业群（IEG），整体难度偏高，更注重工程化和性能优化...',
'腾讯社招前端，岗位是互动娱乐事业群（IEG），整体难度偏高，更注重工程化和性能优化。

**一面**（70分钟）

**手写代码**
1. 实现一个 EventEmitter（发布订阅模式）

2. 手写 apply、call、bind

3. 实现数组去重的多种方法（Set、indexOf、filter、reduce）

4. 实现一个 LRU 缓存（Map + 双向链表）

**框架原理**
5. React Fiber 架构是什么，解决了什么问题？
   - 解决同步渲染不可中断导致的卡顿
   - 将渲染工作分片，利用浏览器空闲时间执行

6. React Hooks 的使用规则和原理
   - 不能在条件/循环中使用 Hook（链表结构原因）
   - useState、useEffect、useCallback、useMemo 的使用场景

7. Virtual DOM 的作用和 Diff 算法
   - key 的作用（帮助 Diff 识别节点）
   - React Diff vs Vue Diff 的策略差异

**二面**（60分钟）

**工程化深度**
1. Webpack 5 的 Module Federation 是什么？
2. Vite 为什么比 Webpack 快？（ESBuild + 按需编译）
3. 前端监控体系如何搭建？（错误监控、性能监控、用户行为）
4. Monorepo 方案对比（Lerna vs Turborepo vs pnpm workspace）

**性能优化实战**
5. 长列表渲染优化（虚拟列表原理和实现）
6. 动画性能优化（使用 transform/opacity，开启 GPU 加速）
7. 首屏时间优化指标（FCP、LCP、TTI）

**三面**（系统设计）

设计一个前端埋点 SDK，要求：
- 支持 PV/UV 统计
- 支持用户行为轨迹
- 不阻塞主线程（Web Worker / requestIdleCallback）
- 数据批量上报（beacon API）

**HR面**：聊期望薪资和职业规划',
2876, 387, 67, '2024-01-20');

SET @iid4 = LAST_INSERT_ID();

-- ⑤ 美团 Java 后端面经
INSERT INTO `interview` (`company_id`, `category_id`, `title`, `author`, `summary`, `content`, `views`, `likes`, `collects`, `publish_date`) VALUES
(@co_meituan, @cat_java, '美团 Java 后端面经（校招）', '骑手逆袭',
'美团校招Java后端，公司风格务实，更注重基础扎实和项目实战经验...',
'美团校招 Java 后端，公司风格务实，更注重基础扎实和项目实战经验，三轮技术面 + HR 面。

**一面**（60分钟）

**计算机基础**
1. 进程和线程的区别？协程是什么？
2. 操作系统的内存管理（分页、分段、虚拟内存）
3. TCP 和 UDP 的区别，各自适用场景
4. 讲讲 TCP 的可靠传输机制（确认应答、超时重传、滑动窗口、拥塞控制）

**Java 核心**
5. String、StringBuilder、StringBuffer 的区别
   - String 不可变（final），每次操作创建新对象
   - StringBuilder 非线程安全，性能高
   - StringBuffer 线程安全（synchronized）

6. List、Set、Map 的区别和使用场景
   - ArrayList vs LinkedList（随机访问 vs 频繁插入删除）
   - HashSet 底层是 HashMap
   - TreeMap 基于红黑树，有序

7. Java 的异常体系
   - Throwable → Error（不可恢复）/ Exception
   - Checked Exception vs Unchecked Exception
   - try-with-resources

**二面**（75分钟）

**框架与数据库**
1. Spring 的 Bean 生命周期（实例化→填充属性→初始化→使用→销毁）
2. Spring 循环依赖如何解决？（三级缓存机制）
3. Redis 数据类型及应用场景
   - String：缓存、计数器、分布式锁
   - Hash：用户信息、购物车
   - List：消息队列、最新消息
   - Set：好友关系、共同关注
   - ZSet：排行榜、延迟队列

4. Redis 持久化方式（RDB vs AOF）的区别和选择
5. MySQL 慢查询优化流程（EXPLAIN 分析、索引优化、查询重写）

**三面**（系统设计）

设计美团外卖的订单系统：
- 订单状态机设计（下单→待付款→待接单→配送中→已完成）
- 如何保证下单接口的幂等性（唯一索引 + 防重Token）
- 大促期间的限流方案（令牌桶 + Sentinel）
- 订单数据的冷热分离',
2943, 356, 58, '2024-02-05');

SET @iid5 = LAST_INSERT_ID();

-- ⑥ 网易前端面经
INSERT INTO `interview` (`company_id`, `category_id`, `title`, `author`, `summary`, `content`, `views`, `likes`, `collects`, `publish_date`) VALUES
(@co_netease, @cat_frontend, '网易前端面经（游戏部门）', '网易吃鸡少年',
'网易互娱前端，重点考察 Canvas、WebGL 和游戏相关的前端技术，和普通业务前端方向有所不同...',
'网易互娱前端，重点考察 Canvas、WebGL 和游戏相关的前端技术，和普通业务前端方向有所不同。

**一面**

1. Canvas vs SVG 的区别及适用场景
   - Canvas：像素操作，适合游戏、图像处理
   - SVG：矢量图形，适合图标、地图、可交互图表

2. requestAnimationFrame 的原理和用法
   - 与定时器的区别（跟随屏幕刷新率，节能）
   - 实现动画循环

3. Web Worker 的使用场景
   - 将耗时计算（物理模拟、数据处理）移入 Worker 线程
   - 主线程通过 postMessage 通信

4. 浏览器渲染流水线
   - 解析HTML/CSS → 构建DOM/CSSOM → Layout → Paint → Composite

5. JavaScript 引擎的工作原理
   - V8 的 JIT 编译
   - 隐藏类（Hidden Class）优化
   - 内联缓存（Inline Cache）

**二面**

1. TypeScript 的优势和高级特性
   - 泛型、条件类型、映射类型
   - 装饰器（Decorator）
   - 类型体操实战

2. 前端安全
   - XSS 攻击原理与防御（输入过滤、CSP）
   - CSRF 攻击原理与防御（Token、SameSite Cookie）
   - 点击劫持（iframe + opacity）

3. 手写题：实现 Promise 的 then 方法（完整 Promise/A+ 规范）

4. 设计模式：观察者模式 vs 发布订阅模式的区别

**三面**（负责人面）

聊项目亮点，对游戏前端的理解，PixiJS/Three.js 的使用经验',
1987, 267, 41, '2024-03-01');

SET @iid6 = LAST_INSERT_ID();

-- ⑦ 京东 Java 后端面经
INSERT INTO `interview` (`company_id`, `category_id`, `title`, `author`, `summary`, `content`, `views`, `likes`, `collects`, `publish_date`) VALUES
(@co_jd, @cat_java, '京东 Java 后端面经（物流系统）', '搬砖工程师',
'京东物流技术部Java后端，偏向高并发、分布式系统，面试注重实际工程经验...',
'京东物流技术部 Java 后端，偏向高并发、分布式系统，面试注重实际工程经验，问的都是实际场景题。

**一面**

1. 讲讲你做过的最有挑战的项目（重点：技术选型原因、遇到的问题、如何解决）

2. Dubbo 的工作原理
   - 服务注册与发现（ZooKeeper/Nacos）
   - 负载均衡策略（轮询、随机、最少活跃调用）
   - 容错机制（Failover、Failfast、Failsafe）

3. 分布式事务解决方案
   - 2PC、3PC 的问题
   - TCC（Try-Confirm-Cancel）
   - Saga 模式
   - 消息最终一致性（本地消息表 + MQ）

4. MySQL 主从复制原理（binlog）和读写分离

5. ES（Elasticsearch）的原理
   - 倒排索引
   - 分片与副本
   - 常见查询类型（Term、Match、Bool）

**二面**

1. 设计一个物流轨迹推送系统
   - 数据来源：司机APP上报位置
   - 推送方式：WebSocket + 服务端推送
   - 高并发处理：消息队列削峰
   - 数据存储：Redis 存实时位置，MySQL 存历史轨迹

2. 如何排查线上 OOM（内存溢出）问题？
   - jmap 导出堆快照
   - MAT/JVisualVM 分析对象引用
   - 常见原因：内存泄漏、数据量过大

3. 线上接口响应时间从100ms升到5000ms，怎么排查？
   - APM 工具查看调用链
   - 慢SQL、慢Redis、慢第三方接口
   - 线程池是否满了

**三面**（架构面）

微服务拆分原则，服务网格（Service Mesh）了解多少，Istio 的功能',
2341, 298, 52, '2024-02-28');

SET @iid7 = LAST_INSERT_ID();

-- ⑧ 滴滴前端面经
INSERT INTO `interview` (`company_id`, `category_id`, `title`, `author`, `summary`, `content`, `views`, `likes`, `collects`, `publish_date`) VALUES
(@co_didi, @cat_frontend, '滴滴前端面经（出行平台）', '代码骑士',
'滴滴出行前端，业务涉及地图、实时定位，面试既考基础也考工程实践，节奏较快...',
'滴滴出行前端，业务涉及地图、实时定位，面试既考基础也考工程实践，节奏较快。

**一面**（55分钟）

1. CSS 题目
   - 实现一个自适应的正方形（padding-bottom 技巧 / aspect-ratio）
   - 水平垂直居中的5种方法
   - CSS 动画 vs JS 动画的性能对比

2. JavaScript 进阶
   - 描述 Generator 函数的使用（迭代器协议）
   - WeakMap 和 Map 的区别（弱引用，防止内存泄漏）
   - Proxy 和 Reflect 的配合使用

3. 手写题
   - 实现 throttle（节流）和 debounce（防抖）
   - 实现一个带缓存的 memoize 函数
   - 实现 compose 函数（函数组合）

4. 地图相关
   - 瓦片地图的原理（XYZ坐标系）
   - 如何在地图上渲染10万个点？（WebGL / Canvas / 聚合策略）

**二面**（50分钟）

1. Vue3 和 Vue2 的核心区别
   - Composition API vs Options API
   - Proxy vs Object.defineProperty
   - 性能提升（更小的 bundle、更快的 Diff）
   - Fragment、Teleport、Suspense 等新特性

2. 微前端架构
   - qiankun 的原理（single-spa + 沙箱隔离）
   - JS 沙箱实现方式（快照沙箱、Proxy沙箱）
   - CSS 隔离方案（Shadow DOM、CSS Module、Scoped）

3. Node.js 相关
   - Event Loop 与浏览器的区别（libuv）
   - Stream 流的应用场景
   - 如何做接口性能压测？

**三面**（系统设计）

设计一个实时打车地图展示方案：如何高效展示附近司机，如何处理频繁的位置更新，如何优化渲染性能',
1756, 231, 38, '2024-03-10');

SET @iid8 = LAST_INSERT_ID();

-- ------------------------------------------------------------
-- 标签关联（interview_tag）
-- ------------------------------------------------------------
-- 百度
INSERT IGNORE INTO `interview_tag` (`interview_id`, `tag_id`) SELECT @iid1, id FROM tag WHERE name IN ('前端','百度','ES6','Vue','算法','HTTP');
-- 字节
INSERT IGNORE INTO `interview_tag` (`interview_id`, `tag_id`) SELECT @iid2, id FROM tag WHERE name IN ('前端','字节跳动','JavaScript','Vue','算法','CSS');
-- 阿里
INSERT IGNORE INTO `interview_tag` (`interview_id`, `tag_id`) SELECT @iid3, id FROM tag WHERE name IN ('Java','阿里巴巴','JVM','Spring','MySQL','分布式');
-- 腾讯
INSERT IGNORE INTO `interview_tag` (`interview_id`, `tag_id`) SELECT @iid4, id FROM tag WHERE name IN ('前端','腾讯','React','Webpack','性能优化','工程化');
-- 美团
INSERT IGNORE INTO `interview_tag` (`interview_id`, `tag_id`) SELECT @iid5, id FROM tag WHERE name IN ('Java','美团','Redis','MySQL','Spring');
-- 网易
INSERT IGNORE INTO `interview_tag` (`interview_id`, `tag_id`) SELECT @iid6, id FROM tag WHERE name IN ('前端','网易','Canvas','TypeScript','浏览器原理');
-- 京东
INSERT IGNORE INTO `interview_tag` (`interview_id`, `tag_id`) SELECT @iid7, id FROM tag WHERE name IN ('Java','京东','分布式','微服务','Elasticsearch');
-- 滴滴
INSERT IGNORE INTO `interview_tag` (`interview_id`, `tag_id`) SELECT @iid8, id FROM tag WHERE name IN ('前端','滴滴','Vue3','地图','微前端');
