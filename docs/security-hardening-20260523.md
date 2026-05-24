# PeakStars Blog 安全加固全记录

## 概述

本次安全审计发现项目存在 8 项安全漏洞，按严重程度分为关键（CRITICAL）、高危（HIGH）和中危（MEDIUM）三个等级。本文档记录每项漏洞的修复方案、核心代码、设计思路及业务逻辑。

---

## 目录

1. [管理员接口鉴权](#1-管理员接口鉴权-critical)
2. [密码哈希升级为 BCrypt](#2-密码哈希升级为-bcrypt-high)
3. [消除硬编码凭证](#3-消除硬编码凭证-high)
4. [CORS 跨域策略收窄](#4-cors-跨域策略收窄-high)
5. [文件上传安全校验](#5-文件上传安全校验-medium)
6. [StorageProxyController 路径穿越修复](#6-storageproxycontroller-路径穿越修复-medium)
7. [登录频率限制](#7-登录频率限制-medium)
8. [评论删除身份校验](#8-评论删除身份校验-medium)

---

## 1. 管理员接口鉴权 (CRITICAL)

### 漏洞描述

`AdminController.java` 和 `AdminBookController.java` 中所有接口（仪表盘、内容 CRUD、文件上传、书籍导入/发布/删除等）没有进行任何身份验证。攻击者可直接操作后台内容数据、上传恶意文件或删除书籍。

### 修复方案

采用 Spring MVC `HandlerInterceptor` 拦截器模式，对所有 `/api/admin/**` 路径请求强制校验 Bearer Token，无需引入完整的 Spring Security 框架，与项目现有的 Token 认证体系保持一致。

### 架构设计

```
前端请求 → AdminWebMvcConfig(路径匹配) → AuthInterceptor.preHandle() → TokenService.parseToken()
                                              ↓ 校验失败                    ↓ 校验成功
                                         BusinessException(401)         request.setAttribute("currentUserId")
                                                                              ↓
                                                                         Controller 执行
```

### 核心代码

#### 1.1 鉴权拦截器 `AuthInterceptor.java`（新增）

```java
@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {

    private final TokenService tokenService;

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) {
        // 从请求头提取 Bearer Token
        String authorization = request.getHeader("Authorization");
        if (authorization == null || authorization.isBlank()) {
            throw new BusinessException(401, "请先登录后再访问管理后台");
        }

        // 解析并校验 Token（含签名验证和过期检查）
        Map<String, Object> payload = tokenService.parseToken(authorization);

        // 将用户 ID 写入 request 属性，供下游 Controller 使用
        request.setAttribute("currentUserId", payload.get("userId"));
        return true;
    }
}
```

**设计要点：**

- 注入已有的 `TokenService`，复用 `parseToken()` 方法中的 HMAC-SHA256 签名校验与过期时间检查
- 校验失败直接抛出 `BusinessException(401, ...)`，由 `GlobalExceptionHandler` 统一包装为标准 JSON 错误响应
- 校验成功时将 `userId` 写入 `request.setAttribute`，Controller 可通过 `request.getAttribute("currentUserId")` 获取当前操作用户

#### 1.2 拦截器注册 `AdminWebMvcConfig.java`（新增）

```java
@Configuration
@RequiredArgsConstructor
public class AdminWebMvcConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
            .addPathPatterns("/api/admin/**")          // 匹配所有管理后台路径
            .excludePathPatterns("/api/admin/heartbeat"); // 心跳接口豁免
    }
}
```

**设计要点：**

- `addPathPatterns("/api/admin/**")` 精确绑定到管理后台路径，不影响公开 API（如 `/api/auth/login`、`/api/content/tech-articles`）
- `excludePathPatterns("/api/admin/heartbeat")` 豁免心跳接口，因为心跳仅用于仪表盘在线人数统计，不涉及敏感操作

#### 1.3 前端补齐 Authorization 请求头 `admin.js`（修改）

```javascript
// 从 localStorage 读取登录后保存的 Token
function getAuthHeaders() {
  const token = localStorage.getItem('interview_demo_access_token')
  return token ? { Authorization: `Bearer ${token}` } : {}
}

async function request(url, options = {}) {
  const response = await fetch(`${BASE_URL}${url}`, {
    headers: {
      'Content-Type': 'application/json',
      ...getAuthHeaders(),       // 自动注入 Bearer Token
      ...(options.headers || {})
    },
    ...options
  })
  // ...
}
```

**设计要点：**

- Token 存储在 `localStorage` 的 `interview_demo_access_token` 键（与 `auth.js` store 保持一致）
- `getAuthHeaders()` 返回空对象时，`...getAuthHeaders()` 不会添加 Authorization 头，对调用方无侵入
- `bookAdmin.js` 中的 `request()` 和 `uploadFile()` 同步做了一致修改

---

## 2. 密码哈希升级为 BCrypt (HIGH)

### 漏洞描述

原 `PasswordService` 使用 `sha256(salt + rawPassword)` 进行密码哈希。SHA-256 是通用快速哈希算法，GPU 可每秒计算数十亿次。结合现代 GPU 集群，8 位随机盐值的 SHA-256 哈希可在数小时内被暴力破解。

### 修复方案

引入 `spring-security-crypto` 依赖，使用 `BCryptPasswordEncoder` 替代 SHA-256。同时保留对旧版 SHA-256 格式的兼容校验，并在用户登录成功后自动将密码升级为 BCrypt。

### 新旧格式对比

| 维度 | 旧版 SHA-256 | 新版 BCrypt |
|------|------------|------------|
| 哈希函数 | SHA-256（快速） | BCrypt（慢速，work factor=10） |
| 盐值 | 手动拼接 32 位十六进制 | 内嵌在哈希结果中 |
| 存储格式 | `{salt}:{sha256hash}` | `$2a$10$...`（含盐值和 work factor） |
| 每次校验耗时 | ~2 微秒 | ~100 毫秒 |
| 暴力破解成本 | GPU 每秒数十亿次 | 每秒约 10 次 |

### 核心代码

#### 2.1 重写 `PasswordService.java`

```java
@Component
public class PasswordService {

    private static final BCryptPasswordEncoder BCRYPT = new BCryptPasswordEncoder();

    /**
     * 新密码统一走 BCrypt 编码。
     * BCrypt 自动生成随机盐值并嵌入结果字符串，无需手动管理盐值。
     */
    public String encode(String rawPassword) {
        return BCRYPT.encode(rawPassword);
    }

    /**
     * 校验时先判断存储格式，再走对应校验路径。
     * BCrypt 格式以 $ 开头，旧版格式为 salt:hash。
     */
    public boolean matches(String rawPassword, String encodedPassword) {
        if (encodedPassword == null) {
            return false;
        }
        if (isLegacyHash(encodedPassword)) {
            return matchesLegacy(rawPassword, encodedPassword);
        }
        return BCRYPT.matches(rawPassword, encodedPassword);
    }

    /**
     * 旧版哈希识别规则：
     * - 包含 ":" 分隔符（salt:hash 结构的标志）
     * - 不以 "$" 开头（排除 BCrypt 和其他新版格式）
     */
    public boolean isLegacyHash(String encodedPassword) {
        return encodedPassword != null
            && encodedPassword.contains(":")
            && !encodedPassword.startsWith("$");
    }

    /**
     * 旧版 SHA-256 校验。
     * 使用 MessageDigest.isEqual() 做常量时间比较，防止时序攻击。
     */
    private boolean matchesLegacy(String rawPassword, String encodedPassword) {
        String[] parts = encodedPassword.split(":", 2);
        if (parts.length != 2) {
            return false;
        }
        String expectedHash = sha256(parts[0] + rawPassword);
        return MessageDigest.isEqual(
            expectedHash.getBytes(StandardCharsets.UTF_8),
            parts[1].getBytes(StandardCharsets.UTF_8)
        );
    }

    private String sha256(String content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(content.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (Exception exception) {
            throw new IllegalStateException("密码加密失败", exception);
        }
    }
}
```

**设计要点：**

- `isLegacyHash()` 是密码迁移的关键方法：同时检查 `":"` 存在和 `"$"` 缺失，确保不会误判
- `matchesLegacy()` 使用 `MessageDigest.isEqual()` 而非 `String.equals()`，因为常量时间比较可以防止时序侧信道攻击
- 旧版 `sha256()` 方法保留，仅用于兼容校验，不参与新密码编码

#### 2.2 自动密码迁移 `AuthServiceImpl.java`（修改）

```java
public LoginResponse login(LoginRequest request) {
    String account = request.getAccount().trim().toLowerCase();
    AuthUser authUser = authUserMapper.findByEmailOrUsername(account);

    if (authUser == null || authUser.getStatus() == null || authUser.getStatus() != 1) {
        throw new BusinessException(400, "Account does not exist");
    }

    if (!passwordService.matches(request.getPassword(), authUser.getPasswordHash())) {
        throw new BusinessException(400, "Incorrect password");
    }

    // 登录成功后，将旧版 SHA-256 哈希静默升级为 BCrypt
    if (passwordService.isLegacyHash(authUser.getPasswordHash())) {
        authUser.setPasswordHash(passwordService.encode(request.getPassword()));
        authUserMapper.updatePassword(authUser.getId(), authUser.getPasswordHash());
    }

    // ... 生成 Token 并返回
}
```

**设计要点：**

- 密码迁移发生在登录成功后、Token 生成前。迁移失败不影响登录流程（此时密码已验证通过）
- 使用户无感知：旧用户下次登录时自动获得 BCrypt 保护，无需强制修改密码
- `updatePassword` 是新增的 Mapper 方法，更新 `password_hash` 和 `updated_at` 两列

#### 2.3 AuthUserMapper 补充

```java
// AuthUserMapper.java - 新增方法
int updatePassword(@Param("id") Long id, @Param("passwordHash") String passwordHash);
```

```xml
<!-- AuthUserMapper.xml - 新增 SQL -->
<update id="updatePassword">
    UPDATE auth_user SET password_hash = #{passwordHash}, updated_at = NOW()
    WHERE id = #{id}
</update>
```

#### 2.4 pom.xml 依赖

```xml
<dependency>
    <groupId>org.springframework.security</groupId>
    <artifactId>spring-security-crypto</artifactId>
</dependency>
```

**选择 `spring-security-crypto` 而非 `spring-boot-starter-security`：**

- `spring-security-crypto` 只包含加密工具类（`BCryptPasswordEncoder`、`PasswordEncoder` 等），体积小，无自动配置
- `spring-boot-starter-security` 会引入完整的 Spring Security 框架，包括 `SecurityFilterChain`、认证管理器等。如果直接引入而不配置，Spring Boot 会自动对所有端点要求 HTTP Basic 认证，这会导致现有基于自定义 Token 的认证体系被覆盖

---

## 3. 消除硬编码凭证 (HIGH)

### 漏洞描述

`application.yml` 和 `application-dev.yml` 中硬编码了：
- 数据库密码：`lixuF123456`
- Token 签名密钥：`interview-demo-auth-secret`
- MinIO 默认凭据：`minioadmin / minioadmin`
- QQ 邮箱：`1843599766@qq.com`

任何有源码仓库访问权限的人都可以获取这些敏感信息。Token 签名密钥泄露意味着攻击者可以伪造任意用户的登录 Token。

### 修复方案

移除所有敏感配置的默认值，强制通过环境变量注入。开发环境可使用 `.env` 文件或 IDE Run Configuration 注入。

### 修改清单

**`application.yml`（修改 6 处）：**

```yaml
# 修改前：password: ${DB_PASSWORD:lixuF123456}
# 修改后：移除默认值，部署时必须通过环境变量传入
spring:
  datasource:
    password: ${DB_PASSWORD:}

  mail:
    username: ${MAIL_USERNAME:}   # 曾硬编码 QQ 邮箱，已移除
    password: ${MAIL_PASSWORD:}

app:
  auth:
    # 修改前：token-secret: ${AUTH_TOKEN_SECRET:interview-demo-auth-secret}
    # 修改后：移除默认值，环境变量必须显式设置
    token-secret: ${AUTH_TOKEN_SECRET}

  storage:
    minio:
      access-key: ${MINIO_ACCESS_KEY:}   # 曾硬编码 minioadmin，已移除
      secret-key: ${MINIO_SECRET_KEY:}   # 曾硬编码 minioadmin，已移除
```

**`application-dev.yml`（修改 2 处）：**

```yaml
spring:
  mail:
    username: ${MAIL_USERNAME:}  # 曾硬编码 1843599766@qq.com

app:
  auth:
    verification:
      mail-from: ${MAIL_FROM:${MAIL_USERNAME:}}  # 链式回退曾包含 QQ 邮箱
```

### 官方建议的安全实践

应用 OWASP [密码存储速查表](https://cheatsheetseries.owasp.org/cheatsheets/Secrets_Management_Cheat_Sheet.html) 第 1 条规则：**永远不要在代码中存储凭证**。所有密钥、密码、Token 必须通过环境变量、Vault 或 CI/CD 密钥管理系统注入。

---

## 4. CORS 跨域策略收窄 (HIGH)

### 漏洞描述

原 CORS 配置 `allowedOriginPatterns("*")` 配合 `allowedHeaders("*")` 允许来自任意域名的任意请求头跨域访问。攻击者可以在任意网站上通过 `fetch()` 请求管理后台接口，如果攻击者知晓 Token（例如通过 XSS 从 localStorage 窃取），即可远程操控后台。

### 修复方案

将 `allowedOriginPatterns` 从 `"*"` 改为可配置的来源白名单，通过 `CORS_ALLOWED_ORIGINS` 环境变量注入逗号分隔的域名列表，开发环境默认仅允许 Vite 常用端口。

### 核心代码

#### 修改后 `CorsConfig.java`

```java
@Configuration
public class CorsConfig {

    // 允许的来源列表，通过环境变量注入
    // 开发环境默认仅开放 localhost:5173 (Vite) 和 localhost:3000
    @Value("${app.cors.allowed-origins:http://localhost:5173,http://localhost:3000}")
    private List<String> allowedOrigins;

    @Bean
    public WebMvcConfigurer corsWebMvcConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/api/**")
                    .allowedOriginPatterns(allowedOrigins.toArray(new String[0]))
                    .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                    .allowedHeaders("*");
            }
        };
    }
    // ... 本地文件资源配置保持不变
}
```

**设计要点：**

- `@Value` 的 Spring EL 表达式 `"${app.cors.allowed-origins:http://localhost:5173,http://localhost:3000}"` 提供双重保障：
  - 如果设置了 `CORS_ALLOWED_ORIGINS` 环境变量，使用显式配置
  - 如果未设置（开发环境），回退到 `localhost:5173,localhost:3000`
- Spring 自动将逗号分隔的字符串解析为 `List<String>`
- 生产环境部署时需显式设置 `CORS_ALLOWED_ORIGINS=https://your-production-domain.com`

---

## 5. 文件上传安全校验 (MEDIUM)

### 漏洞描述

原文件上传校验仅检查 HTTP 请求头中的 `Content-Type` 字段：

```java
// 修复前：仅检查 Content-Type 头，可被伪造
String contentType = file.getContentType();
if (contentType == null || !contentType.startsWith("image/")) {
    return "仅支持上传图片文件";
}
```

攻击者可以将恶意文件（如 `.jsp`、`.php`、`.exe`）的 `Content-Type` 设置为 `image/png` 从而绕过校验。如果服务器能将上传文件解析为可执行脚本，将导致远程代码执行（RCE）。

### 修复方案

建立三层校验体系：**扩展名白名单 → Content-Type 头 → 文件魔数**。其中魔数校验是核心防线，通过读取文件二进制头识别真实格式。

### 核心代码

#### 5.1 三层校验 `AdminController.validateImageFile()`

```java
// 合法图片扩展名白名单
private static final Set<String> ALLOWED_IMAGE_EXTENSIONS = Set.of(
    "png", "jpg", "jpeg", "gif", "webp", "bmp", "svg"
);

private String validateImageFile(MultipartFile file) {
    // 第一层：空文件检查
    if (file.isEmpty()) {
        return "上传文件不能为空";
    }

    // 第二层：文件扩展名白名单
    String originalName = file.getOriginalFilename();
    if (originalName == null || !hasAllowedImageExtension(originalName)) {
        return "仅支持上传 png / jpg / jpeg / gif / webp / bmp / svg 格式图片";
    }

    // 第三层：Content-Type 头（辅助校验，不可作为唯一依据）
    String contentType = file.getContentType();
    if (contentType == null || !contentType.startsWith("image/")) {
        return "仅支持上传图片文件";
    }

    // 第四层：魔数校验（核心防线）
    if (!hasValidImageMagicBytes(file)) {
        return "文件内容与图片格式不匹配";
    }

    // 大小限制
    long maxSize = 10 * 1024 * 1024;
    if (file.getSize() > maxSize) {
        return "图片大小不能超过 10MB";
    }

    return null; // 校验通过
}
```

#### 5.2 扩展名校验

```java
private boolean hasAllowedImageExtension(String filename) {
    int dot = filename.lastIndexOf('.');
    if (dot < 0) {
        return false;
    }
    return ALLOWED_IMAGE_EXTENSIONS.contains(
        filename.substring(dot + 1).toLowerCase()
    );
}
```

#### 5.3 魔数校验——核心防线

```java
/**
 * 读取文件头部的魔数字节（magic bytes）判断真实文件类型。
 *
 * 常见图片格式的魔数：
 *   PNG  → 89 50 4E 47
 *   JPEG → FF D8 FF
 *   GIF  → 47 49 46 38
 *   WebP → 52 49 46 46 xx xx xx xx 57 45 42 50
 *   BMP  → 42 4D
 *
 * SVG 是 XML 文本格式，没有固定魔数，通过扩展名白名单单独放行。
 */
private boolean hasValidImageMagicBytes(MultipartFile file) {
    String originalName = file.getOriginalFilename();
    if (originalName != null && originalName.toLowerCase().endsWith(".svg")) {
        return true; // SVG 是纯文本 XML，通过扩展名放行
    }

    try (InputStream in = file.getInputStream()) {
        byte[] header = new byte[12]; // 读取前 12 字节（WebP 需要 12 字节）
        int read = in.read(header);
        if (read < 4) {
            return false; // 文件太小，不可能是合法图片
        }

        // 逐个匹配各图片格式的魔数
        if (match(header, 0, 0x89, 0x50, 0x4E, 0x47)) return true;         // PNG
        if (match(header, 0, 0xFF, 0xD8, 0xFF)) return true;               // JPEG
        if (match(header, 0, 0x47, 0x49, 0x46, 0x38)) return true;         // GIF
        if (read >= 12
            && match(header, 0, 0x52, 0x49, 0x46, 0x46)                    // RIFF
            && match(header, 8, 0x57, 0x45, 0x42, 0x50)) return true;      // WEBP
        if (match(header, 0, 0x42, 0x4D)) return true;                     // BMP

        return false;
    } catch (IOException e) {
        return false; // 读取异常时拒绝
    }
}

// 字节匹配辅助方法：将 Java 有符号字节转为无符号整数后比较
private boolean match(byte[] header, int offset, int... expected) {
    for (int i = 0; i < expected.length; i++) {
        if ((header[offset + i] & 0xFF) != expected[i]) {
            return false;
        }
    }
    return true;
}
```

**设计要点：**

- `MultipartFile.getInputStream()` 每次调用返回新的流，读取魔数不会影响后续的上传处理（Controller 的上传方法会再次调用 `getInputStream()`）
- `(header[i] & 0xFF)` 将 Java 有符号 byte（范围 -128~127）转为无符号整数（0~255），与魔数常量做精确比较
- WebP 格式需要 12 字节：前 4 字节 `RIFF` + 4 字节文件大小 + 后 4 字节 `WEBP`

---

## 6. StorageProxyController 路径穿越修复 (MEDIUM)

### 漏洞描述

`StorageProxyController.serveUploadedFile()` 从请求 URI 中提取文件路径，拼接到本地上传目录后进行文件读取。原代码使用 `Path.normalize()` 折叠了 `..` 段，但**没有验证归一化后的路径仍然在允许的上传目录范围内**。

攻击者可以通过 `/uploads/../../etc/passwd` 或在 MinIO/OSS 对象存储中添加带 `../` 路径的文件，读取服务器上的任意文件。

### 修复方案

在两层做防御：

1. **文件系统层**：在 `tryServeFromLocal()` 中增加 `startsWith` 边界检查
2. **URL 解析层**：在 `normalizeProxyFilePath()` 中拒绝含 `..` 和 `//` 的输入

### 核心代码

#### 6.1 本地文件边界检查

```java
private ResponseEntity<?> tryServeFromLocal(String filePath) {
    // 将用户输入的相对路径拼接到上传根目录并归一化
    Path localFile = Paths.get(uploadDir).resolve(filePath).normalize();

    // 【新增】验证归一化后的路径仍然在上传根目录下
    // 例如：filePath = "../../etc/passwd"
    //       normalize 后 = "/etc/passwd"
    //       uploadDir = "/app/uploads"
    //       startsWith 检查失败 → 拒绝
    if (!localFile.startsWith(Paths.get(uploadDir).normalize())) {
        log.warn("路径穿越尝试被拒绝: {}", filePath);
        return null;
    }

    if (!Files.exists(localFile) || !Files.isRegularFile(localFile)) {
        return null;
    }
    // ... 读取文件并返回
}
```

**为什么 `normalize()` 不够？**

```
Paths.get("/app/uploads/").resolve("../../etc/passwd").normalize()
→ Paths.get("/etc/passwd")  // normalize() 正确折叠了 .. 段，但没有报错
```

`normalize()` 的语义是"规范化路径"，不是"安全校验"。它会把 `a/b/../c` 变成 `a/c`，但不会拒绝越界路径。`startsWith` 才是真正判定"目标是否在允许范围内"的方法。

#### 6.2 URL 路径字符过滤

```java
private String normalizeProxyFilePath(String filePath) {
    String normalized = filePath == null ? "" : filePath.trim();

    // 【新增】拒绝包含路径穿越字符的请求
    // ".." 可用于目录穿越
    // "//" 可能绕过某些路径匹配规则
    if (normalized.contains("..") || normalized.contains("//")) {
        log.warn("拒绝包含非法路径字符的代理请求: {}", normalized);
        return "";
    }

    // ... 去除 MinIO bucket 前缀
}
```

**设计要点：**

- URL 层的过滤在文件系统操作之前执行，尽早拦截恶意请求
- `normalizeProxyFilePath()` 返回空字符串后，下游 `Paths.get(uploadDir).resolve("")` 得到上传根目录，文件不存在则返回 null，最终走 404 响应

---

## 7. 登录频率限制 (MEDIUM)

### 漏洞描述

`POST /api/auth/login` 接口没有任何频率限制。攻击者可以对任意账号进行无限次的密码猜测（暴力破解），特别是在密码策略较弱（仅要求 6 位密码）的情况下，弱密码账号极易被攻破。

### 修复方案

引入基于 Redis 的登录频率限制服务 `LoginRateLimitService`，遵循与现有 `EmailVerificationServiceImpl` 一致的 Redis 键管理和过期策略。

**限流策略：**同一账号 + 同一 IP 在 15 分钟内连续失败 5 次后，该组合被锁定 15 分钟。成功登录自动清除失败计数。

### 核心代码

#### 7.1 限流服务 `LoginRateLimitService.java`（新增）

```java
@Slf4j
@Component
@RequiredArgsConstructor
public class LoginRateLimitService {

    private static final int MAX_FAILURES = 5;              // 最大失败次数
    private static final Duration LOCK_DURATION = Duration.ofMinutes(15);  // 锁定时长

    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 前置检查：登录是否已被锁定。
     * Redis 不可用时降级跳过限流（记录警告日志），避免缓存故障导致全员无法登录。
     */
    public void checkNotLocked(String account, String clientIp) {
        try {
            String lockKey = buildLockKey(account, clientIp);
            if (Boolean.TRUE.equals(stringRedisTemplate.hasKey(lockKey))) {
                long remainingSeconds = getRemainingSeconds(lockKey);
                throw new BusinessException(429,
                    "操作过于频繁，请在 " + formatRemaining(remainingSeconds) + " 后重试");
            }
        } catch (DataAccessException e) {
            log.warn("Redis 不可用，跳过登录限流检查: {}", e.getMessage());
        }
    }

    /**
     * 登录成功后清理计数和锁定标记。
     * 防止正常用户在锁定窗口过期后、下一次登录前被残留记录误拦。
     */
    public void onLoginSuccess(String account, String clientIp) {
        try {
            stringRedisTemplate.delete(buildFailCountKey(account, clientIp));
            stringRedisTemplate.delete(buildLockKey(account, clientIp));
        } catch (DataAccessException e) {
            log.warn("Redis 不可用，跳过登录成功计数清除: {}", e.getMessage());
        }
    }

    /**
     * 登录失败后递增计数，达到阈值时添加锁定标记。
     */
    public void onLoginFailure(String account, String clientIp) {
        try {
            String failKey = buildFailCountKey(account, clientIp);
            Long count = stringRedisTemplate.opsForValue().increment(failKey);
            // 首次失败时设置过期时间（后续递增不重置 TTL）
            if (count != null && count == 1L) {
                stringRedisTemplate.expire(failKey, LOCK_DURATION);
            }
            // 达到阈值时添加锁定标记
            if (count != null && count >= MAX_FAILURES) {
                stringRedisTemplate.opsForValue()
                    .set(buildLockKey(account, clientIp), "1", LOCK_DURATION);
            }
        } catch (DataAccessException e) {
            log.warn("Redis 不可用，跳过登录失败计数: {}", e.getMessage());
        }
    }

    // Redis Key 格式：auth:login:fail-count:{account}:{ip}
    private String buildFailCountKey(String account, String clientIp) {
        return RedisKeyConstants.Login.FAIL_COUNT_PREFIX + account + ":" + clientIp;
    }

    // Redis Key 格式：auth:login:locked:{account}:{ip}
    private String buildLockKey(String account, String clientIp) {
        return RedisKeyConstants.Login.LOCKED_PREFIX + account + ":" + clientIp;
    }

    private String formatRemaining(long seconds) {
        if (seconds >= 60) {
            return (seconds / 60) + " 分钟";
        }
        return seconds + " 秒";
    }
}
```

**设计要点：**

- **降级策略**：所有 Redis 操作包裹在 try-catch 中，`DataAccessException` 时只记录日志不阻断业务。这保证了 Redis 宕机时系统仍可正常登录（牺牲限流能力换取可用性）
- **锁定时长与计数过期一致**：`LOCK_DURATION = 15分钟`，失败计数 Key 首次设置时的 TTL 也是 15 分钟。15 分钟后，计数和锁定标记同时过期，用户可以重新尝试
- **成功即清空**：`onLoginSuccess()` 主动删除失败计数和锁定标记，不使用 `expire`。这意味着用户在第 3 次失败后如果成功了，不会因为之前的失败次数在 15 分钟内被锁定——这是一个"奖励成功"的设计

#### 7.2 Controller 集成 `AuthController.login()`（修改）

```java
@PostMapping("/login")
public ApiResponse<LoginResponse> login(
    @Valid @RequestBody LoginRequest request,
    HttpServletRequest httpServletRequest         // 新增参数，用于提取客户端 IP
) {
    String account = request.getAccount().trim().toLowerCase();
    String clientIp = extractClientIp(httpServletRequest);

    // 前置检查：是否已被锁定
    loginRateLimitService.checkNotLocked(account, clientIp);

    try {
        LoginResponse response = authService.login(request);
        loginRateLimitService.onLoginSuccess(account, clientIp);  // 成功 → 清除计数
        return ApiResponse.success("Welcome back, ...", response);
    } catch (BusinessException e) {
        loginRateLimitService.onLoginFailure(account, clientIp);  // 失败 → 递增计数
        throw e;  // 异常继续向上抛，由 GlobalExceptionHandler 处理
    }
}
```

**设计要点：**

- `extractClientIp()` 是已有的方法，优先取 `X-Forwarded-For` 头（适配反向代理），再取 `X-Real-IP`，最后回退到 `getRemoteAddr()`
- `catch (BusinessException e)` 精确捕获业务异常（密码错误、账号不存在等），不捕获 `RuntimeException`（如 NPE），避免将系统错误误计为登录失败
- 限流计数器以 `{account}:{ip}` 为维度，防止单 IP 对多账号的攻击，也防止多 IP 对单账号的攻击

#### 7.3 Redis Key 常量补充

```java
public static final class Login {
    /** auth:login:fail-count:{account}:{clientIp} */
    public static final String FAIL_COUNT_PREFIX = "auth:login:fail-count:";
    /** auth:login:locked:{account}:{clientIp} */
    public static final String LOCKED_PREFIX = "auth:login:locked:";
}
```

---

## 8. 评论删除身份校验 (MEDIUM)

### 漏洞描述

`DELETE /api/content/comments/{commentId}` 接口不需要任何身份验证。任何人可以删除任何文章下的任何评论。在浏览器控制台中执行 `fetch()` 即可批量删除评论。

### 修复方案

在 Controller 层增加 Authorization 头校验，未登录用户返回 401。

### 核心代码

```java
// 修改前：无需任何认证
@DeleteMapping("/comments/{commentId}")
public ApiResponse<Map<String, Object>> deleteArticleComment(
    @PathVariable Long commentId
) {
    boolean deleted = contentService.deleteArticleComment(commentId);
    // ...
}

// 修改后：强制要求登录
@DeleteMapping("/comments/{commentId}")
public ApiResponse<Map<String, Object>> deleteArticleComment(
    @PathVariable Long commentId,
    @RequestHeader(value = "Authorization", required = false) String authorization
) {
    Long currentUserId = resolveCurrentUserId(authorization);
    if (currentUserId == null) {
        return ApiResponse.fail(401, "请先登录后再删除评论");
    }
    boolean deleted = contentService.deleteArticleComment(commentId);
    // ...
}
```

**设计要点：**

- `resolveCurrentUserId()` 是已有的私有方法，通过解析 Authorization 头中的 Token 提取 `userId`。Token 无效或过期时返回 `null`
- `required = false` 允许 Authorization 头不存在（不会触发 Spring 的 400 错误），在方法体内显式判断 `null` 并返回 401
- 当前评论系统使用昵称（`nickname`）而非用户 ID 标识评论者，无法做"作者本人才能删除"的细粒度控制。后续如需更严格的权限控制，可在评论表中添加 `user_id` 字段并在 `deleteArticleComment` 中校验 `comment.userId == currentUserId`

---

## 环境变量配置清单

部署前必须设置以下环境变量：

| 环境变量 | 对应配置 | 生成命令 |
|---------|---------|---------|
| `AUTH_TOKEN_SECRET` | Token HMAC 签名密钥 | `openssl rand -base64 32` |
| `DB_PASSWORD` | MySQL 数据库密码 | — |
| `MAIL_PASSWORD` | QQ 邮箱 SMTP 授权码 | 在 QQ 邮箱设置中生成 |
| `MAIL_USERNAME` | 发件邮箱地址 | — |
| `CORS_ALLOWED_ORIGINS` | 生产域名 | 如 `https://blog.example.com` |
| `MINIO_ACCESS_KEY` | MinIO 访问密钥（可选） | — |
| `MINIO_SECRET_KEY` | MinIO 秘密密钥（可选） | — |
| `OSS_ACCESS_KEY_ID` | 阿里云 OSS AK（可选） | — |
| `OSS_ACCESS_KEY_SECRET` | 阿里云 OSS SK（可选） | — |

---

## 修改文件清单

### 新增文件（5 个）

| 文件 | 说明 |
|------|------|
| `server-java/.../config/AuthInterceptor.java` | 管理员接口鉴权拦截器 |
| `server-java/.../config/AdminWebMvcConfig.java` | 拦截器注册配置 |
| `server-java/.../service/LoginRateLimitService.java` | 登录频率限制服务 |

### 修改文件（17 个）

| 文件 | 变更内容 |
|------|---------|
| `pom.xml` | 新增 `spring-security-crypto` 依赖 |
| `PasswordService.java` | SHA-256 → BCrypt，保留兼容校验 |
| `AuthServiceImpl.java` | 登录时自动升级旧密码 |
| `AuthUserMapper.java` | 新增 `updatePassword` 方法 |
| `AuthUserMapper.xml` | 新增密码更新 SQL |
| `application.yml` | 移除硬编码凭证（6 处） |
| `application-dev.yml` | 移除硬编码邮箱和密钥 |
| `CorsConfig.java` | CORS 来源从 `*` 改为可配置白名单 |
| `AdminController.java` | 文件上传新增魔数校验和扩展名白名单 |
| `StorageProxyController.java` | 路径穿越双层防御 |
| `AuthController.java` | 登录接口集成频率限制 |
| `ContentController.java` | 评论删除强制登录校验 |
| `RedisKeyConstants.java` | 新增 Login 键前缀 |
| `src/modules/admin/api/admin.js` | 前端补齐 Authorization 请求头 |
| `src/modules/admin/api/bookAdmin.js` | 前端补齐 Authorization 请求头 |

---

## 参考资料

- [OWASP 密码存储速查表](https://cheatsheetseries.owasp.org/cheatsheets/Password_Storage_Cheat_Sheet.html)
- [OWASP 文件上传速查表](https://cheatsheetseries.owasp.org/cheatsheets/File_Upload_Cheat_Sheet.html)
- [OWASP 路径遍历](https://owasp.org/www-community/attacks/Path_Traversal)
- [OWASP 暴力破解防护](https://cheatsheetseries.owasp.org/cheatsheets/Authentication_Cheat_Sheet.html)
- [BCrypt 设计原理](https://en.wikipedia.org/wiki/Bcrypt)
- [Spring Security Crypto 模块](https://docs.spring.io/spring-security/reference/features/integrations/cryptography.html)
