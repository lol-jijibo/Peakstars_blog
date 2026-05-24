# 上线前严重问题修复报告

> 日期：2026-05-24
> 分支：main
> 范围：server-java（Spring Boot 后端）

---

## 背景

在上线前进行全面审查，覆盖安全、性能、代码质量和架构四个维度。审查共发现 20+ 项问题，其中 **5 项严重问题** 在上线前必须修复。本文档记录这 5 项的修复内容。

---

## 修复 1：MySQL 数据库 SSL 加固

### 原始问题

[application.yml](server-java/src/main/resources/application.yml) 中 JDBC 连接硬编码了 `useSSL=false&allowPublicKeyRetrieval=true`，导致数据库密码和所有数据在网络上明文传输。`allowPublicKeyRetrieval=true` 还可能被中间人攻击利用。

### 修复内容

**基座配置（application.yml）**：将 SSL 相关参数改为通过环境变量注入，保留 dev 默认值：

```yaml
# 修改前
url: jdbc:mysql://localhost:3306/interview_db?...&useSSL=false&allowPublicKeyRetrieval=true

# 修改后
url: jdbc:mysql://${DB_HOST:localhost}:${DB_PORT:3306}/${DB_NAME:interview_db}?...&useSSL=${DB_USE_SSL:false}&allowPublicKeyRetrieval=${DB_ALLOW_PUBLIC_KEY_RETRIEVAL:true}
```

同时新增 HikariCP 连接池配置：最大连接数、空闲连接、连接超时、泄露检测阈值均可通过环境变量覆盖。

**新建生产配置（application-prod.yml）**：

```yaml
spring:
  datasource:
    url: ...&useSSL=true&requireSSL=true&allowPublicKeyRetrieval=false
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      leak-detection-threshold: 30000

logging:
  level:
    root: WARN
    com.interview: INFO

app:
  auth:
    token-expire-hours: 24  # 生产环境缩短到 24 小时
```

**生产部署时必须设置** `SPRING_PROFILES_ACTIVE=prod`

### 影响文件

| 文件 | 操作 |
|---|---|
| [application.yml](server-java/src/main/resources/application.yml) | 修改 |
| [application-prod.yml](server-java/src/main/resources/application-prod.yml) | 新建 |

---

## 修复 2：书籍上传文件校验

### 原始问题

[AdminBookController.java](server-java/src/main/java/com/interview/auth/admin/controller/AdminBookController.java) 的 ZIP 导入和单文件导入接口只检查了文件是否为空和 200MB 大小限制，没有扩展名白名单、Content-Type 和魔数校验。攻击者可上传任意文件（.jsp、.war 等可执行文件），是严重安全隐患。

同时，Service 层的校验在 `inputStream.readAllBytes()` 之后才执行，恶意大文件会先撑满内存再被拒绝。

### 修复内容

在 Controller 层新增 `validateBookFile()` 方法，覆盖两个上传入口，实现 4 层校验：

| 校验层 | ZIP 入口 | 单文件入口 |
|---|---|---|
| 空文件 | 拦截 | 拦截 |
| 扩展名白名单 | 仅 `.zip` | `epub/pdf/txt/md/docx/html` |
| 文件大小 | ≤200MB | ≤200MB |
| 魔数校验 | PK\x03\x04 (ZIP) | PDF: %PDF, EPUB/DOCX: PK\x03\x04, TXT/MD/HTML: 跳过 |

魔数校验逻辑：
- PDF 文件：检测文件头 `25 50 44 46`（`%PDF`）
- ZIP/EPUB/DOCX：检测文件头 `50 4B 03 04`（`PK..`）
- TXT/MD/HTML：纯文本无固定魔数，通过扩展名白名单控制

### 影响文件

| 文件 | 操作 |
|---|---|
| [AdminBookController.java](server-java/src/main/java/com/interview/auth/admin/controller/AdminBookController.java) | 修改 |

---

## 修复 3：健康检查端点

### 原始问题

项目未引入 Spring Boot Actuator，没有 `/actuator/health` 端点。负载均衡器和容器编排平台（K8s、Docker Compose 等）无法判断服务是否存活。数据库、Redis 或对象存储挂了也无法被监控系统发现。

### 修复内容

1. [pom.xml](server-java/pom.xml) 添加依赖：

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

2. [application.yml](server-java/src/main/resources/application.yml) 暴露 health 端点：

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health
  endpoint:
    health:
      show-details: when-authorized
```

健康检查地址：`GET /actuator/health`

### 影响文件

| 文件 | 操作 |
|---|---|
| [pom.xml](server-java/pom.xml) | 修改 |
| [application.yml](server-java/src/main/resources/application.yml) | 修改 |

---

## 修复 4：Token 密钥启动校验

### 原始问题

[application-dev.yml](server-java/src/main/resources/application-dev.yml) 中 `token-secret` 有 fallback 值 `dev-secret-key-for-local-development-only`。如果生产环境忘记设置 `AUTH_TOKEN_SECRET` 环境变量，Spring 会使用这个弱密钥，导致任何人可以伪造 token 登录任意账号。

### 修复内容

新建 [TokenSecretStartupChecker.java](server-java/src/main/java/com/interview/auth/config/TokenSecretStartupChecker.java)，监听 `ApplicationReadyEvent`，在应用就绪后自动校验：

| 检测场景 | 行为 |
|---|---|
| `AUTH_TOKEN_SECRET` 为空 | ERROR 日志告警 |
| 使用已知 dev 弱密钥 `dev-secret-key-for-local-development-only` | WARN 日志提醒 |
| 密钥长度 < 32 字节（HMAC-SHA256 推荐最低长度） | WARN 日志提醒 |
| 密钥正常 | INFO 日志打印密钥长度 |

注意：该组件不会阻止启动（避免因配置问题导致服务不可用），而是通过显眼的日志告警提醒运维人员。建议生产环境配合日志监控使用。

### 影响文件

| 文件 | 操作 |
|---|---|
| [TokenSecretStartupChecker.java](server-java/src/main/java/com/interview/auth/config/TokenSecretStartupChecker.java) | 新建 |

---

## 修复 5：关键单元测试

### 原始问题

项目后端 `src/test/` 目录存在但为空，没有任何测试用例。`pom.xml` 中的 `spring-boot-starter-test` 依赖完全未使用。生产上线零测试覆盖是高风险隐患。

### 修复内容

新增 2 个测试类，覆盖最核心的安全组件：

**PasswordServiceTest（13 个用例）**：

| 测试场景 | 覆盖点 |
|---|---|
| BCrypt 加密后能匹配原文 | 正常流程 |
| 错误密码被拒绝 | 正常流程 |
| null 密码被拒绝 | 边界 |
| 旧版 SHA-256 格式识别 | `isLegacyHash()` |
| BCrypt 不会被误判为旧版 | `isLegacyHash()` |
| 旧版密码正确匹配 | `matches()` 兼容逻辑 |
| 旧版密码错误拒绝 | `matches()` 兼容逻辑 |
| 畸形旧版哈希拒绝 | 边界 |
| 多冒号旧版格式拒绝 | 边界 |
| 每次生成不重复的 salt | `generateSalt()` |
| 相同明文生成不同 BCrypt 哈希 | salt 随机性 |
| 空密码处理 | 边界 |
| 特殊字符（中文/emoji/日文） | 国际化 |

**TokenServiceTest（12 个用例）**：

| 测试场景 | 覆盖点 |
|---|---|
| 签发 token 后能正确解析 | 正常流程 |
| 无 Bearer 前缀解析 | 正常流程 |
| null / 空 token 拒绝 | 边界 |
| 无分隔符 token 拒绝 | 格式校验 |
| 篡改 token 拒绝 | 签名防篡改 |
| 不同密钥签发的 token 拒绝 | 签名隔离 |
| 过期 token 拒绝 | 过期校验 |
| 同一用户多次签发 token 不同 | 时间戳随机性 |
| 用户名含特殊字符 | 国际化 |
| 仅 Bearer 前缀无内容拒绝 | 边界 |

### 影响文件

| 文件 | 操作 |
|---|---|
| [PasswordServiceTest.java](server-java/src/test/java/com/interview/auth/service/PasswordServiceTest.java) | 新建 |
| [TokenServiceTest.java](server-java/src/test/java/com/interview/auth/service/TokenServiceTest.java) | 新建 |

---

## 文件变更汇总

| 文件 | 操作 | 说明 |
|---|---|---|
| `server-java/pom.xml` | 修改 | 添加 actuator 依赖 |
| `server-java/src/main/resources/application.yml` | 修改 | SSL 参数化、连接池配置、actuator 端点、token 过期可配 |
| `server-java/src/main/resources/application-prod.yml` | **新建** | 生产环境安全配置 |
| `server-java/src/main/java/.../admin/controller/AdminBookController.java` | 修改 | 书籍上传 4 层文件校验 |
| `server-java/src/main/java/.../config/TokenSecretStartupChecker.java` | **新建** | Token 密钥启动校验 |
| `server-java/src/test/java/.../service/PasswordServiceTest.java` | **新建** | 密码服务测试（13 用例） |
| `server-java/src/test/java/.../service/TokenServiceTest.java` | **新建** | Token 服务测试（12 用例） |

---

## 部署检查清单

生产环境上线前，请逐项确认：

- [ ] `SPRING_PROFILES_ACTIVE=prod` 已设置
- [ ] `AUTH_TOKEN_SECRET` 已设置为强随机值（≥32 字符，建议 `openssl rand -base64 48` 生成）
- [ ] `DB_USE_SSL=true`（由 application-prod.yml 强制，无需额外设置）
- [ ] `DB_HOST` / `DB_PORT` / `DB_NAME` / `DB_USERNAME` / `DB_PASSWORD` 已配置
- [ ] `REDIS_HOST` / `REDIS_PORT` / `REDIS_PASSWORD` 已配置
- [ ] `CORS_ALLOWED_ORIGINS` 已设置为生产前端域名
- [ ] 启动日志中无 Token 密钥告警
- [ ] `GET /actuator/health` 返回 `{"status":"UP"}`

---

## 后续建议（中优先级）

审查中发现的以下问题未在本次修复，建议在上线后尽快处理：

1. **App 首页添加缓存** — `StarReadServiceImpl` 每次都全量加载文章，加 `@Cacheable` 可显著降低数据库压力
2. **管理员仪表盘 SQL 优化** — 当前每次请求 10+ 条全表扫描，应加缓存或改用聚合查询
3. **评论接口加频率限制** — 当前无需认证即可无限发评论
4. **ArrayDeque 线程安全** — `AdminServiceImpl.buildTrendPoints()` 读取未加同步
5. **ES 客户端异常日志** — 搜索失败时静默吞异常，应加 `log.warn()`
6. **添加安全响应头** — 缺少 X-Content-Type-Options、X-Frame-Options、CSP 等
7. **add logback-spring.xml** — 生产环境日志需要文件持久化和滚动策略
