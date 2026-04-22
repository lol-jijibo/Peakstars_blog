# 🌟 Peakstars Blog - 面经宝典

<div align="center">

![Vue](https://img.shields.io/badge/Vue-3.4-4FC08D?logo=vue.js&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5-6DB33F?logo=spring&logoColor=white)
![Java](https://img.shields.io/badge/Java-17-007396?logo=java&logoColor=white)
![MySQL](https://img.shields.io/badge/MySQL-8.0-4479A1?logo=mysql&logoColor=white)
![Redis](https://img.shields.io/badge/Redis-6.0-DC382D?logo=redis&logoColor=white)
![License](https://img.shields.io/badge/License-MIT-blue.svg)

**一个基于 Vue3 + Spring Boot 的面经分享平台，帮助求职者快速获取一线互联网公司的面试经验**

[在线演示](#) | [快速开始](#-快速开始) | [功能特性](#-功能特性) | [技术架构](#-技术栈)

</div>

---

## 📖 项目简介

Peakstars Blog 是一个面向求职者的**面经分享社区**，收录了百度、字节跳动、阿里巴巴、腾讯、美团、网易、京东、滴滴等一线互联网公司的真实面试经验。

### 🎯 核心价值
- 📚 **真实面经**：收录一线大厂真实面试题目和经验分享
- 🔐 **安全认证**：基于邮箱验证码的注册登录系统
- 💾 **数据持久化**：MySQL + Redis 双层存储架构
- 🎨 **现代化 UI**：响应式设计，支持多端访问
- ⚡ **高性能**：Vite 构建 + Spring Boot 后端，毫秒级响应

## ✨ 功能特性

### ✅ 已完成功能

#### 前端模块
- **面经展示系统**
  - 面经列表页（支持前端/Java 分类筛选）
  - 面经详情页（Markdown 格式渲染）
  - 响应式卡片布局设计
  - 标签系统（技术栈标签展示）
  
- **用户认证系统**
  - 邮箱注册（QQ 邮箱 SMTP 验证码）
  - 用户登录/登出
  - "记住我"功能
  - 路由守卫（登录态校验）
  - 忘记密码入口
  
- **个人中心**
  - 我的收藏页面
  - 我的点赞页面
  - 个人信息展示

#### 后端模块
- **认证服务 (Spring Boot)**
  - RESTful API 设计（统一响应体）
  - 邮箱验证码发送（QQ SMTP）
  - 用户注册/登录接口
  - 密码加密存储（BCrypt）
  - Token 生成与验证
  - 全局异常处理
  - CORS 跨域配置
  
- **验证码系统**
  - Redis 缓存验证码（5分钟有效期）
  - 发送冷却机制（60秒）
  - 每日发送限制（邮箱/IP 维度）
  - 验证码一次性消费
  - 防刷保护

#### 数据库设计
- 用户认证表（auth_user）
- 面经主表（interview）
- 企业信息表（company）
- 分类表（category）
- 标签表（tag）
- 面经-标签关联表（interview_tag）
- 用户行为表（user_action）

### 🚧 进行中

- 前后端联调优化
- 用户会话管理完善
- 收藏/点赞数据持久化
- 面经列表 API 对接

### 📋 待开发功能

#### 核心功能
- ⏳ 面经发布功能（用户投稿）
- ⏳ 评论系统（支持回复）
- ⏳ 搜索功能（关键词/标签/企业搜索）
- ⏳ 用户个人主页
- ⏳ 面经编辑/删除（作者权限）

#### 优化方向
- ⏳ 数据统计（浏览量/点赞数实时更新）
- ⏳ 分页加载优化（虚拟滚动）
- ⏳ 图片上传功能（OSS 集成）
- ⏳ 第三方登录（微信/GitHub OAuth）
- ⏳ 面经推荐算法
- ⏳ 用户等级系统
- ⏳ 消息通知中心

## 🛠️ 技术栈

### 前端技术栈

| 技术 | 版本 | 说明 |
|------|------|------|
| **Vue.js** | 3.4.0 | 渐进式 JavaScript 框架，使用 Composition API |
| **Vite** | 5.0.0 | 下一代前端构建工具，开发体验极佳 |
| **Vue Router** | 4.3.0 | 官方路由管理器，支持路由守卫 |
| **JavaScript** | ES6+ | 现代 JavaScript 语法 |
| **CSS3** | - | 原生样式，Flexbox/Grid 布局 |
| **Fetch API** | - | 原生 HTTP 请求，统一错误处理 |

**前端特色**
- 🎨 **组件化开发**：InterviewCard、路由懒加载
- 🔐 **状态管理**：Reactive API 实现轻量级 auth store
- 🛡️ **路由守卫**：登录态校验、重定向逻辑
- ⚡ **性能优化**：按需加载、代码分割
- 📱 **响应式设计**：适配多端设备

### 后端技术栈

| 技术 | 版本 | 说明 |
|------|------|------|
| **Spring Boot** | 3.5.0 | 企业级 Java 应用框架 |
| **JDK** | 17 | LTS 长期支持版本 |
| **MyBatis** | 3.0.4 | 持久层框架，SQL 映射 |
| **MySQL** | 8.0+ | 关系型数据库 |
| **Redis** | 6.0+ | 内存数据库，验证码缓存 |
| **Spring Boot Mail** | - | 邮件发送服务（QQ SMTP） |
| **Maven** | 3.6+ | 项目构建管理工具 |
| **Lombok** | 1.18.38 | 简化 Java 代码 |

**后端特色**
- 🏗️ **分层架构**：Controller → Service → Mapper
- 📦 **统一响应体**：ApiResponse (code/message/data)
- 🛡️ **全局异常处理**：GlobalExceptionHandler
- ✅ **参数校验**：Jakarta Validation (@Valid)
- 🔒 **密码加密**：BCrypt 哈希算法
- 🚀 **RESTful API**：符合 REST 规范
- 📊 **数据库设计**：规范化设计，外键约束

## 📁 项目结构

```bash
Peakstars_blog/
├── src/                                    # 前端源码目录
│   ├── views/                              # 页面组件
│   │   ├── InterviewList.vue               # 面经列表页（分类筛选）
│   │   ├── InterviewDetail.vue             # 面经详情页（Markdown 渲染）
│   │   ├── LoginView.vue                   # 登录页（记住我功能）
│   │   ├── AuthView.vue                    # 注册页（邮箱验证码）
│   │   ├── Collect.vue                     # 我的收藏页
│   │   ├── Like.vue                        # 我的点赞页
│   │   └── Mine.vue                        # 个人中心页
│   ├── components/                         # 公共组件
│   │   └── InterviewCard.vue               # 面经卡片组件
│   ├── router/                             # 路由配置
│   │   └── index.js                        # 路由定义 + 守卫
│   ├── api/                                # API 接口封装
│   │   └── interview.js                    # 面经相关接口
│   ├── stores/                             # 状态管理
│   │   └── auth.js                         # 认证状态管理（266行）
│   ├── data/                               # 静态数据
│   │   └── interviews.js                   # 面经初始数据（8篇）
│   ├── styles/                             # 全局样式
│   ├── App.vue                             # 根组件
│   └── main.js                             # 应用入口
│
├── server-java/                            # Spring Boot 后端服务
│   ├── src/main/java/com/interview/auth/  # Java 源码
│   │   ├── controller/                     # 控制层
│   │   │   └── AuthController.java         # 认证接口（注册/登录/验证码）
│   │   ├── service/                        # 业务层
│   │   │   ├── AuthService.java            # 认证服务接口
│   │   │   ├── EmailVerificationService.java # 邮箱验证服务
│   │   │   ├── PasswordService.java        # 密码加密服务
│   │   │   └── TokenService.java           # Token 生成服务
│   │   ├── domain/                         # 领域模型
│   │   │   ├── entity/                     # 实体类
│   │   │   │   └── AuthUser.java           # 用户实体
│   │   │   ├── dto/request/                # 请求 DTO
│   │   │   └── dto/response/               # 响应 DTO
│   │   ├── infrastructure/                 # 基础设施层
│   │   │   └── mapper/                     # MyBatis Mapper
│   │   │       └── AuthUserMapper.java     # 用户数据访问
│   │   ├── common/                         # 公共模块
│   │   │   ├── ApiResponse.java            # 统一响应体
│   │   │   ├── BusinessException.java      # 业务异常
│   │   │   ├── GlobalExceptionHandler.java # 全局异常处理
│   │   │   └── RedisKeyConstants.java      # Redis Key 常量
│   │   ├── config/                         # 配置类
│   │   │   ├── CorsConfig.java             # CORS 跨域配置
│   │   │   └── AuthVerificationProperties.java # 验证码配置
│   │   └── AuthApplication.java            # 应用启动类
│   ├── src/main/resources/                 # 资源文件
│   │   ├── application.yml                 # 应用配置
│   │   └── mapper/                         # MyBatis XML 映射
│   ├── pom.xml                             # Maven 依赖配置
│   └── README.md                           # 后端服务说明
│
├── database/                               # 数据库脚本
│   ├── schema.sql                          # 表结构定义（7张表）
│   ├── seed.sql                            # 初始数据（8篇面经）
│   ├── auth_user.sql                       # 用户表结构
│   └── README.md                           # 数据库说明文档
│
├── public/                                 # 静态资源目录
├── .env.development                        # 开发环境变量
├── vite.config.js                          # Vite 构建配置
├── package.json                            # 前端依赖管理
├── index.html                              # HTML 入口
└── README.md                               # 项目说明文档
```

### 核心文件说明

| 文件路径 | 代码量 | 核心功能 |
|---------|--------|---------|
| `src/stores/auth.js` | 266 行 | 认证状态管理、登录注册逻辑、Token 管理 |
| `src/data/interviews.js` | 510 行 | 8 篇面经初始数据（百度/字节/阿里/腾讯等） |
| `server-java/controller/AuthController.java` | 80 行 | 5 个认证接口（注册/登录/验证码/忘记密码） |
| `database/schema.sql` | 105 行 | 7 张表结构定义（用户/面经/企业/标签等） |
| `database/seed.sql` | 600+ 行 | 初始数据（8 家企业、8 篇面经、30+ 标签） |

## 🚀 快速开始

### 环境要求
- Node.js >= 16.0
- JDK 17+
- MySQL 8.0+
- Redis 6.0+
- Maven 3.6+

### 前端启动

```bash
# 安装依赖
npm install

# 启动开发服务器
npm run dev

# 构建生产版本
npm run build
```

访问 `http://localhost:5173`

### 后端启动

1. **配置环境变量**（Windows PowerShell）

```powershell
# 邮件服务配置（QQ 邮箱）
$env:MAIL_HOST="smtp.qq.com"
$env:MAIL_PORT="465"
$env:MAIL_PROTOCOL="smtps"
$env:MAIL_USERNAME="your_qq_mail@qq.com"
$env:MAIL_PASSWORD="your_qq_smtp_authorization_code"
$env:MAIL_FROM="your_qq_mail@qq.com"

# Redis 配置
$env:REDIS_HOST="127.0.0.1"
$env:REDIS_PORT="6379"
$env:REDIS_PASSWORD=""
$env:REDIS_DATABASE="0"
```

2. **启动 MySQL 和 Redis**

```bash
# 导入数据库脚本
mysql -u root -p < database/init.sql

# 启动 Redis
redis-server
```

3. **运行 Spring Boot 应用**

```bash
cd server-java
mvn spring-boot:run
```

后端服务运行在 `http://localhost:8080`

## 📡 API 接口文档

### 认证接口（已实现）

#### 1. 发送注册验证码

```http
POST /api/auth/register/email-code/send
Content-Type: application/json

{
  "email": "user@qq.com"
}
```

**响应示例**

```json
{
  "code": 0,
  "message": "Verification code sent. Please check your QQ mailbox.",
  "data": {
    "expiresInSeconds": 300,
    "cooldownSeconds": 60
  }
}
```

**限流规则**
- 同一邮箱：60 秒冷却时间
- 同一 IP：每日最多 10 次
- 同一邮箱：每日最多 5 次

#### 2. 用户注册

```http
POST /api/auth/register
Content-Type: application/json

{
  "email": "user@qq.com",
  "username": "张三",
  "password": "Password123",
  "passwordConfirm": "Password123",
  "emailCode": "123456"
}
```

**响应示例**

```json
{
  "code": 0,
  "message": "Registration completed. Please sign in.",
  "data": {
    "user": {
      "id": 1,
      "username": "张三",
      "email": "user@qq.com",
      "joinedAt": "2024-04-21T10:30:00"
    }
  }
}
```

#### 3. 用户登录

```http
POST /api/auth/login
Content-Type: application/json

{
  "account": "user@qq.com",
  "password": "Password123",
  "remember": true
}
```

**响应示例**

```json
{
  "code": 0,
  "message": "Welcome back, 张三",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "user": {
      "id": 1,
      "username": "张三",
      "email": "user@qq.com"
    }
  }
}
```

#### 4. 获取当前用户信息

```http
GET /api/auth/me
Authorization: Bearer <token>
```

#### 5. 忘记密码

```http
POST /api/auth/forgot-password
Content-Type: application/json

{
  "email": "user@qq.com"
}
```

### 面经接口（规划中）

| 接口 | 方法 | 说明 | 状态 |
|------|------|------|------|
| `/api/interviews` | GET | 获取面经列表（支持分页、分类筛选） | 📋 待开发 |
| `/api/interviews/:id` | GET | 获取面经详情 | 📋 待开发 |
| `/api/interviews` | POST | 发布面经（需登录） | 📋 待开发 |
| `/api/interviews/:id` | PUT | 编辑面经（作者权限） | 📋 待开发 |
| `/api/interviews/:id` | DELETE | 删除面经（作者权限） | 📋 待开发 |
| `/api/interviews/:id/like` | POST | 点赞/取消点赞 | 📋 待开发 |
| `/api/interviews/:id/collect` | POST | 收藏/取消收藏 | 📋 待开发 |
| `/api/categories` | GET | 获取分类列表 | 📋 待开发 |
| `/api/companies` | GET | 获取企业列表 | 📋 待开发 |
| `/api/tags` | GET | 获取标签列表 | 📋 待开发 |

## 🗄️ 数据库设计

### 数据库表结构（7 张表）

#### 1. 用户认证表 (auth_user)

```sql
CREATE TABLE auth_user (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  username VARCHAR(50) NOT NULL,
  email VARCHAR(100) NOT NULL UNIQUE,
  password_hash VARCHAR(255) NOT NULL,
  status TINYINT DEFAULT 1,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
```

#### 2. 企业信息表 (company)

存储面试企业信息（百度、字节、阿里等 8 家企业）

- `avatar_text`: 头像显示文字（如"百"）
- `avatar_color`: 头像背景色（HEX 颜色）

#### 3. 分类表 (category)

面经分类（全部/前端/Java 后端）

#### 4. 标签表 (tag)

技术栈标签（Vue、React、Spring、MySQL 等 30+ 标签）

#### 5. 面经主表 (interview)

```sql
CREATE TABLE interview (
  id INT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  company_id INT UNSIGNED NOT NULL,
  category_id INT UNSIGNED NOT NULL,
  title VARCHAR(128) NOT NULL,
  author VARCHAR(64) NOT NULL,
  summary VARCHAR(512) NOT NULL,
  content MEDIUMTEXT NOT NULL,
  views INT UNSIGNED DEFAULT 0,
  likes INT UNSIGNED DEFAULT 0,
  collects INT UNSIGNED DEFAULT 0,
  publish_date DATE NOT NULL,
  status TINYINT DEFAULT 1,
  FOREIGN KEY (company_id) REFERENCES company(id),
  FOREIGN KEY (category_id) REFERENCES category(id)
);
```

#### 6. 面经-标签关联表 (interview_tag)

多对多关系，一篇面经可以有多个标签

#### 7. 用户行为表 (user_action)

记录用户的点赞和收藏行为

```sql
CREATE TABLE user_action (
  id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  user_id INT UNSIGNED NOT NULL,
  interview_id INT UNSIGNED NOT NULL,
  action_type TINYINT NOT NULL,  -- 1=点赞 2=收藏
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY (user_id, interview_id, action_type)
);
```

### 初始数据

- **8 家企业**：百度、字节跳动、阿里巴巴、腾讯、美团、网易、京东、滴滴
- **8 篇面经**：涵盖前端和 Java 后端岗位
- **30+ 标签**：JavaScript、Vue、React、Java、Spring、MySQL、Redis 等
- **3 个分类**：全部、前端、Java 后端

## 🎨 核心功能特性

### 邮箱验证码系统

- ✅ QQ 邮箱 SMTP 发送验证码
- ✅ Redis 存储验证码（5 分钟有效期）
- ✅ 发送冷却时间（60 秒）
- ✅ 每日发送限制（邮箱/IP 维度）
- ✅ 验证码一次性消费机制
- ✅ 防刷保护（IP + 邮箱双重限流）

### 面经展示系统

- ✅ 支持前端/Java 分类筛选
- ✅ Markdown 格式内容渲染
- ✅ 标签系统（技术栈标签）
- ✅ 浏览量/点赞数统计
- ✅ 作者头像与昵称展示
- ✅ 企业 Logo 颜色标识

### 用户认证系统

- ✅ 邮箱注册/登录
- ✅ 密码加密存储（BCrypt）
- ✅ Token 认证机制
- ✅ "记住我"功能
- ✅ 路由守卫（登录态校验）
- ✅ 个人收藏/点赞记录

## 💡 项目亮点

### 技术亮点

1. **前端架构设计**
   - 使用 Vue 3 Composition API，代码更简洁
   - 自研轻量级状态管理（auth.js 266 行）
   - 路由守卫实现登录态校验和重定向
   - 统一的 API 请求封装（超时控制、错误处理）

2. **后端架构设计**
   - 分层架构：Controller → Service → Mapper
   - 统一响应体（ApiResponse）
   - 全局异常处理（GlobalExceptionHandler）
   - 参数校验（Jakarta Validation）
   - 密码加密（BCrypt）

3. **验证码系统**
   - Redis 缓存验证码（5 分钟有效期）
   - 多维度限流（IP + 邮箱）
   - 防刷保护（冷却时间 + 每日限制）
   - 一次性消费机制

4. **数据库设计**
   - 规范化设计（7 张表）
   - 外键约束保证数据一致性
   - 索引优化（company_id、category_id、publish_date）
   - 支持软删除（status 字段）

5. **开发体验**
   - Vite 构建，热更新速度快
   - 代码分割，按需加载
   - 环境变量配置
   - 代理配置简化开发

### 业务亮点

1. **真实面经数据**：8 篇一线大厂面经，涵盖前端和 Java 后端
2. **完整认证流程**：注册 → 验证码 → 登录 → Token 认证
3. **用户体验优化**："记住我"功能、登录后跳转原页面
4. **安全性考虑**：密码加密、验证码限流、Token 认证

## 🔧 开发配置

### Vite 代理配置

前端开发服务器已配置代理，自动转发 `/auth-api` 请求到后端：

```javascript
// vite.config.js
export default defineConfig({
  plugins: [vue()],
  server: {
    proxy: {
      '/auth-api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/auth-api/, '')
      }
    }
  },
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  }
})
```

### 环境变量

前端环境变量配置在 `.env.development`：

```bash
VITE_API_BASE_URL=/auth-api
VITE_AUTH_API_BASE_URL=/auth-api
```

### CORS 配置

后端已配置 CORS 跨域支持：

```java
@Configuration
public class CorsConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins("http://localhost:5173")
                .allowedMethods("GET", "POST", "PUT", "DELETE")
                .allowCredentials(true);
    }
}
```

## 📝 开发日志

### 2024-04-21
- ✅ 完成登录页面 UI 设计
- ✅ 完成注册页面邮箱验证码功能
- ✅ 完成面经基础数据结构
- ✅ 开始前后端联调

### 2024-04-20
- ✅ 搭建 Spring Boot 后端服务
- ✅ 集成 MyBatis + MySQL
- ✅ 实现邮箱验证码发送功能
- ✅ 配置 Redis 缓存

### 2024-04-19
- ✅ 初始化 Vue3 + Vite 项目
- ✅ 完成面经列表页面
- ✅ 完成面经详情页面
- ✅ 配置 Vue Router

## 🤝 贡献指南

欢迎提交 Issue 和 Pull Request！

1. Fork 本仓库
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 提交 Pull Request

## 📄 许可证

本项目采用 MIT 许可证

## 👨‍💻 作者

**lol-jijibo**

- GitHub: [@lol-jijibo](https://github.com/lol-jijibo)

## 🙏 致谢

感谢所有贡献面经内容的求职者，帮助更多人顺利通过面试！

---

⭐ 如果这个项目对你有帮助，欢迎 Star 支持！

## 🚀 快速开始

### 环境要求

| 软件 | 版本要求 | 说明 |
|------|---------|------|
| Node.js | >= 16.0 | 前端运行环境 |
| JDK | 17+ | Java 开发工具包 |
| MySQL | 8.0+ | 关系型数据库 |
| Redis | 6.0+ | 内存数据库 |
| Maven | 3.6+ | Java 项目构建工具 |

### 前端启动

```bash
# 1. 克隆项目
git clone https://github.com/lol-jijibo/Peakstars_blog.git
cd Peakstars_blog

# 2. 安装依赖
npm install

# 3. 启动开发服务器
npm run dev

# 4. 构建生产版本
npm run build

# 5. 预览生产构建
npm run preview
```

访问 `http://localhost:5173`

### 后端启动

#### 1. 配置环境变量

**Windows PowerShell**

```powershell
# 邮件服务配置（QQ 邮箱）
$env:MAIL_HOST="smtp.qq.com"
$env:MAIL_PORT="465"
$env:MAIL_PROTOCOL="smtps"
$env:MAIL_USERNAME="your_qq_mail@qq.com"
$env:MAIL_PASSWORD="your_qq_smtp_authorization_code"
$env:MAIL_FROM="your_qq_mail@qq.com"

# Redis 配置
$env:REDIS_HOST="127.0.0.1"
$env:REDIS_PORT="6379"
$env:REDIS_PASSWORD=""
$env:REDIS_DATABASE="0"
```

**Linux / macOS**

```bash
export MAIL_HOST="smtp.qq.com"
export MAIL_PORT="465"
export MAIL_PROTOCOL="smtps"
export MAIL_USERNAME="your_qq_mail@qq.com"
export MAIL_PASSWORD="your_qq_smtp_authorization_code"
export MAIL_FROM="your_qq_mail@qq.com"

export REDIS_HOST="127.0.0.1"
export REDIS_PORT="6379"
export REDIS_PASSWORD=""
export REDIS_DATABASE="0"
```

#### 2. 配置 QQ 邮箱 SMTP

1. 登录 [QQ 邮箱](https://mail.qq.com/)
2. 进入 **设置** → **账户** → **POP3/IMAP/SMTP/Exchange/CardDAV/CalDAV服务**
3. 开启 **POP3/SMTP 服务** 或 **IMAP/SMTP 服务**
4. 生成 **授权码**（16 位字符）
5. 将授权码填入 `MAIL_PASSWORD` 环境变量

**注意**：`MAIL_PASSWORD` 是授权码，不是 QQ 邮箱登录密码！

#### 3. 启动 MySQL 和 Redis

```bash
# 启动 MySQL（确保 MySQL 服务已安装）
# Windows: 在服务中启动 MySQL
# Linux/macOS: sudo systemctl start mysql

# 导入数据库脚本
mysql -u root -p < database/schema.sql
mysql -u root -p < database/auth_user.sql
mysql -u root -p < database/seed.sql

# 启动 Redis
redis-server
```

#### 4. 运行 Spring Boot 应用

```bash
cd server-java

# 方式 1：使用 Maven 运行
mvn spring-boot:run

# 方式 2：打包后运行
mvn clean package
java -jar target/interview-auth-service-1.0.0.jar
```

后端服务运行在 `http://localhost:8080`

### 验证安装

1. **前端**：访问 `http://localhost:5173`，应该看到登录页面
2. **后端**：访问 `http://localhost:8080/api/auth/register/email-code/send`，应该返回 JSON 响应
3. **数据库**：执行 `SELECT * FROM interview_db.company;`，应该看到 8 家企业数据
4. **Redis**：执行 `redis-cli ping`，应该返回 `PONG`

## 🎯 使用指南

### 注册新用户

1. 访问注册页面
2. 输入 QQ 邮箱（必须是 @qq.com 结尾）
3. 点击"发送验证码"，等待邮件
4. 输入验证码、用户名和密码
5. 点击"注册"完成注册

### 登录系统

1. 输入邮箱或用户名
2. 输入密码
3. 勾选"记住我"（可选）
4. 点击"登录"

### 浏览面经

1. 登录后自动跳转到面经列表
2. 点击分类标签筛选（全部/前端/Java）
3. 点击面经卡片查看详情
4. 点击点赞/收藏按钮（功能开发中）

## 🐛 常见问题

### 1. 前端启动失败

**问题**：`npm install` 报错

**解决**：
```bash
# 清除缓存
npm cache clean --force

# 删除 node_modules 和 package-lock.json
rm -rf node_modules package-lock.json

# 重新安装
npm install
```

### 2. 后端启动失败

**问题**：`Failed to connect to Redis`

**解决**：
- 确保 Redis 服务已启动：`redis-server`
- 检查 Redis 端口：`redis-cli ping`
- 检查环境变量：`echo $REDIS_HOST`

**问题**：`Access denied for user 'root'@'localhost'`

**解决**：
- 检查 MySQL 用户名和密码
- 确保数据库 `interview_db` 已创建
- 检查 `application.yml` 中的数据库配置

### 3. 验证码收不到

**问题**：点击"发送验证码"后没有收到邮件

**解决**：
- 检查 QQ 邮箱是否开启 SMTP 服务
- 确认 `MAIL_PASSWORD` 是授权码，不是登录密码
- 检查邮箱地址是否正确（必须是 @qq.com）
- 查看后端日志是否有错误信息
- 检查 Redis 是否正常运行

### 4. 跨域问题

**问题**：前端请求后端接口报 CORS 错误

**解决**：
- 确保后端 `CorsConfig.java` 配置正确
- 检查前端请求地址是否正确（应该是 `/auth-api/...`）
- 确保 Vite 代理配置正确

## 📚 更多文档

- [数据库设计文档](database/README.md)
- [后端服务说明](server-java/README.md)
- [API 接口文档](#-api-接口文档)

## 🔮 未来规划

### 短期目标（1-2 个月）

- [ ] 完成面经列表 API 对接
- [ ] 实现面经发布功能
- [ ] 添加评论系统
- [ ] 实现搜索功能
- [ ] 完善用户个人主页

### 中期目标（3-6 个月）

- [ ] 添加第三方登录（微信/GitHub）
- [ ] 实现面经推荐算法
- [ ] 添加用户等级系统
- [ ] 实现消息通知中心
- [ ] 优化性能（虚拟滚动、懒加载）

### 长期目标（6 个月以上）

- [ ] 移动端适配优化
- [ ] 开发小程序版本
- [ ] 添加 AI 面试助手
- [ ] 实现面经智能分类
- [ ] 构建面试题库系统

## 📊 项目统计

- **前端代码**：约 1,500+ 行（Vue 组件 + 路由 + 状态管理）
- **后端代码**：约 2,000+ 行（Java + Spring Boot）
- **数据库表**：7 张表
- **API 接口**：5 个已实现，10+ 个规划中
- **初始数据**：8 家企业、8 篇面经、30+ 标签

## 🏆 项目成就

- ✅ 完整的前后端分离架构
- ✅ 规范的 RESTful API 设计
- ✅ 完善的邮箱验证码系统
- ✅ 安全的用户认证机制
- ✅ 清晰的代码结构和注释

---

<div align="center">

**Built with ❤️ by lol-jijibo**

如果这个项目对你有帮助，欢迎 Star ⭐ 支持！

[⬆ 回到顶部](#-peakstars-blog---面经宝典)

</div>
