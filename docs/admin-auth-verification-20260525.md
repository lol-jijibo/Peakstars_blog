# 管理后台权限验证与准入控制报告

> 日期：2026-05-25
> 分支：main
> 范围：server-java（Spring Boot 后端） + 前端（Vue 3）
> 前提：修复 commit `daef268` ~ `27c348e` 遗留的管理后台访问控制逻辑

---

## 背景

当前项目存在一个严重安全缺口：任何已登录的普通用户都可以通过直接输入 URL（如 `/admin/tech`）进入管理后台。虽然管理后台的 API 调用会被后端拦截，但前端路由层面没有有效的角色校验，导致普通用户可以进入管理界面并在客户端层面探测后台功能。

同时，管理员（博客作者本人）每次访问后台都需要重新验证身份，缺乏"首次验证、后续免验"的体验优化。

### 核心问题链路

```
普通用户登录
  → 浏览器地址栏输入 /admin/tech
    → Vue Router 只检查 requiresAuth（是否已登录）
      → 路由放行，用户进入管理后台界面
        → 可查看侧边栏、菜单结构、功能入口
          → 虽然 API 调用可能被后端拦截，但已暴露后台结构
```

### 修复目标

| 维度 | 目标 |
|---|---|
| 安全 | 前端路由 + 后端 API 双重拦截，只有 `role=admin` 且 `status=1` 的用户可访问管理后台 |
| 体验 | 管理员首次验证通过后缓存结果，同一 token 后续访问免验证，0 API 调用 |
| 隐晦 | 普通用户被拦截时不暴露权限相关提示，仅显示"当前页面暂时不可用" |
| 可靠 | 以数据库实时角色为准，防止前端缓存或 localStorage 篡改绕过 |

---

## 修复 1：后端 AuthInterceptor 角色校验（重建）

### 问题分析

原有 `AuthInterceptor` 仅校验 Bearer Token 是否存在和能否解析，不校验用户角色。任何人只要持有有效 token，就能通过拦截器访问 `/api/admin/**` 接口。

```
修复前拦截逻辑：
  Authorization header 存在？
    → Token 可解析？
      → 放行（不关心用户角色）
```

### 修复方案

重写 `AuthInterceptor.preHandle()`，新增数据库角色查询：

```java
// 修复前
public boolean preHandle(HttpServletRequest request, ...) {
    String authorization = request.getHeader("Authorization");
    if (authorization == null || authorization.isBlank()) {
        throw new BusinessException(401, "请先登录后再访问管理后台");
    }
    Map<String, Object> payload = tokenService.parseToken(authorization);
    request.setAttribute("currentUserId", payload.get("userId"));
    return true;
}

// 修复后
public boolean preHandle(HttpServletRequest request, ...) {
    String authorization = request.getHeader("Authorization");
    if (authorization == null || authorization.isBlank()) {
        throw new BusinessException(401, ACCESS_UNAVAILABLE_MESSAGE);
    }
    Map<String, Object> payload = tokenService.parseToken(authorization);
    Long userId = resolveUserId(payload);
    AuthUser authUser = authUserMapper.findById(userId);        // ★ 查库验证
    if (!isActiveAdmin(authUser)) {                              // ★ 角色 + 状态校验
        throw new BusinessException(403, ACCESS_UNAVAILABLE_MESSAGE);
    }
    request.setAttribute("currentUserId", userId);
    return true;
}
```

**关键设计**：
- 注入 `AuthUserMapper`，以数据库角色为准，不使用前端传递的任何角色信息
- `isActiveAdmin()` 同时校验 `role = "admin"` AND `status = 1`，防止已禁用的管理员账号被利用
- 所有失败分支统一返回 `"当前页面暂时不可用"`，不区分 401/403，避免暴露权限结构
- `resolveUserId()` 中 try-catch 包裹类型转换，异常时同样返回隐晦提示

### 影响文件

| 文件 | 操作 |
|---|---|
| [AuthInterceptor.java](server-java/src/main/java/com/interview/auth/config/AuthInterceptor.java) | 重写 preHandle，新增角色校验 + 隐私错误提示 |

---

## 修复 2：移除 heartbeat 白名单

### 问题分析

`AdminWebMvcConfig` 中注册拦截器时排除了 `/api/admin/heartbeat` 路径：

```java
registry.addInterceptor(authInterceptor)
    .addPathPatterns("/api/admin/**")
    .excludePathPatterns("/api/admin/heartbeat");  // ← 可被探测
```

heartbeat 端点用于管理台"在线状态"心跳上报，需要携带 `clientId`。排除白名单意味着无需任何认证就可以调用该端点，可能被用于探测管理后台是否存在。

### 修复方案

移除排除路径，心跳端点由 `AuthInterceptor` 统一保护。心跳请求通过 `getAuthHeaders()` 携带 Bearer Token，拦截器正常校验。

### 影响文件

| 文件 | 操作 |
|---|---|
| [AdminWebMvcConfig.java](server-java/src/main/java/com/interview/auth/config/AdminWebMvcConfig.java) | 移除 excludePathPatterns |

---

## 修复 3：前端路由双层管理员守卫

### 问题分析

原有 `router.beforeEach` 仅做 `requiresAuth` 校验。管理后台路由虽然定义了 `requiresAdmin: true`，但没有对应的守卫逻辑：

```js
// 修复前
router.beforeEach((to) => {
  if (to.meta.requiresAuth && !isAuthenticated()) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }
  // requiresAdmin 被忽略！
})
```

### 修复方案

在 `beforeEach` 中新增两层管理守卫：

```js
// 第一层：客户端快速拦截（缓存角色判断）
if (to.meta.requiresAdmin && !isAdminUser()) {
    stopEntryTransitionLoader()
    return redirectUnavailable()  // → /home?notice=unavailable
}

// 第二层：服务端二次验证（防止 localStorage 篡改）
if (to.meta.requiresAdmin) {
    const verified = await verifyAdminAccess()
    if (!verified) {
      stopEntryTransitionLoader()
      return redirectUnavailable()
    }
}
```

**两层设计的意图**：

```
请求 /admin/tech
  │
  ├─ 第一层（isAdminUser）
  │   ├─ state.user.role === 'admin' → 继续
  │   └─ state.user.role !== 'admin' → 立即重定向（0ms，无网络调用）
  │
  └─ 第二层（verifyAdminAccess）
      ├─ 缓存命中 → 直接放行（0 API 调用）
      └─ 缓存未命中 → GET /api/auth/me → 验证角色 → 缓存结果
```

- **第一层**：基于内存缓存的对象引用判断，性能极高，对普通用户即时阻断
- **第二层**：服务端实时验证，防止恶意篡改 localStorage 绕过

### 影响文件

| 文件 | 操作 |
|---|---|
| [index.js](src/router/index.js) | 新增两层管理守卫 + redirectUnavailable 重定向 |

---

## 修复 4：Auth Store 管理员验证与缓存

### 问题分析

原有 auth store 没有管理员专用的验证逻辑。`isAdminUser()` 仅读取 `state.user.role`，无服务端验证，无缓存机制。管理员每次进入后台都需要等待接口响应。

### 修复方案

在 `auth.js` 中新增三个模块级函数：

```js
// ① 服务端刷新用户信息
async function refreshCurrentUser() {
    // GET /api/auth/me → 更新 state.user → 持久化
    // 失败时自动 logout() + 清空 adminVerifiedToken
}

// ② 管理员会话验证 + 缓存
async function ensureAdminSession() {
    if (!isAuthenticated() || state.user?.role !== 'admin') return false

    // ★ 缓存命中：同一 token 已验证过 → 跳过 API 调用
    if (localStorage.getItem('adminVerifiedToken') === state.accessToken) return true

    // ★ 缓存未命中：调用服务端验证
    const result = await refreshCurrentUser()
    if (result.success && state.user?.role === 'admin') {
        localStorage.setItem('adminVerifiedToken', state.accessToken)  // 缓存
        return true
    }
    localStorage.removeItem('adminVerifiedToken')
    return false
}

// ③ 对外导出
export function isAdminUser() { return state.user?.role === 'admin' }
export async function verifyAdminAccess() { return ensureAdminSession() }
```

**缓存生命周期**：

```
管理员登录 → accessToken = "abc123"
  │
  ├─ 首次访问 /admin/tech
  │   → ensureAdminSession()
  │     → adminVerifiedToken 为空 → 调用 /api/auth/me
  │       → 验证通过 → setItem('adminVerifiedToken', 'abc123')
  │
  ├─ 再次访问 /admin/stats
  │   → ensureAdminSession()
  │     → 'abc123' === 'abc123' → 命中缓存 → return true（0 API 调用）
  │
  ├─ 页面刷新
  │   → state 从 localStorage 恢复
  │     → ensureAdminSession()
  │       → 'abc123' === 'abc123' → 仍命中 → return true
  │
  └─ Token 过期，重新登录 → accessToken = "xyz789"
      → ensureAdminSession()
        → 'abc123' !== 'xyz789' → 缓存未命中 → 重新调用 /api/auth/me
```

### 影响文件

| 文件 | 操作 |
|---|---|
| [auth.js](src/stores/auth.js) | 新增 refreshCurrentUser / ensureAdminSession / verifyAdminAccess + adminVerifiedToken 存储 |

---

## 修复 5：HomeView 隐晦阻断 Toast

### 问题分析

普通用户被拦截后，原有逻辑直接重定向到 `/home`，没有任何提示。用户无法理解为何无法访问（也无从猜测 — 这是安全优势），但体验上略显粗糙。

### 修复方案

**Step 1：路由重定向携带隐晦参数**

```js
function redirectUnavailable() {
    return {
        path: '/home',
        query: { notice: 'unavailable' }  // 不携带任何权限相关信息
    }
}
```

**Step 2：HomeView 监听参数并展示 toast**

```vue
<transition name="home-access-toast">
  <div v-if="accessNoticeVisible" class="home-access-toast" role="status">
    页面暂时不可用，请稍后再试
  </div>
</transition>
```

```js
watch(() => route.query.notice, (notice) => {
    if (notice === 'unavailable') showAccessNotice()
}, { immediate: true })

function showAccessNotice() {
    accessNoticeVisible.value = true
    accessNoticeTimer = setTimeout(() => { accessNoticeVisible.value = false }, 2600)
    router.replace({ path: '/home' })  // 清除 URL 中的参数
}
```

**Step 3：CSS 样式**

- 固定定位在页面顶部，半透明毛玻璃效果
- 2.6 秒自动淡出消失
- 明暗主题均适配

```css
.home-access-toast {
    position: fixed;
    top: 76px;
    left: 50%;
    transform: translateX(-50%);
    padding: 0.5rem 1.25rem;
    font-size: 0.8125rem;
    color: #64748b;                    /* 灰色文字，非警告色 */
    background: rgba(241,245,249,0.92);
    backdrop-filter: blur(8px);
    border: 1px solid #e2e8f0;
    border-radius: 20px;              /* 药丸形状 */
    pointer-events: none;             /* 不阻碍交互 */
}
```

**隐晦设计要点**：
- 文字："页面暂时不可用，请稍后再试" — 像系统维护通知，不像权限拒绝
- 颜色：`#64748b` (slate-500)，非红色/橙色警告色
- 形状：圆角药丸，像普通系统 toast
- 时长：2.6 秒后自动消失，不强制用户交互
- 交互：`pointer-events: none`，不阻挡任何点击

### 影响文件

| 文件 | 操作 |
|---|---|
| [HomeView.vue](src/views/HomeView.vue) | 新增 toast 模板 + watch + showAccessNotice |
| [HomeView.css](src/styles/views/HomeView.css) | 新增 .home-access-toast 样式 + 过渡动画 + 暗色主题 |

---

## 文件变更汇总

### 修改文件（6 个）

| 文件 | 说明 |
|---|---|
| [AuthInterceptor.java](server-java/src/main/java/com/interview/auth/config/AuthInterceptor.java) | 重写拦截逻辑，新增 DB 角色查询 + 隐晦错误提示 |
| [AdminWebMvcConfig.java](server-java/src/main/java/com/interview/auth/config/AdminWebMvcConfig.java) | 移除 heartbeat 白名单 |
| [index.js](src/router/index.js) | 新增两层管理守卫（客户端 + 服务端）+ redirectUnavailable |
| [auth.js](src/stores/auth.js) | 新增 refreshCurrentUser / ensureAdminSession / verifyAdminAccess |
| [HomeView.vue](src/views/HomeView.vue) | 新增隐晦阻断 toast 逻辑 |
| [HomeView.css](src/styles/views/HomeView.css) | 新增 toast 样式 + 过渡动画 + 暗色主题适配 |

---

## 架构设计思路

### 权限验证数据流

```
前端路由守卫                          后端拦截器
═══════════                         ══════════

用户访问 /admin/tech
  │
  ├─ ① isAuthenticated()
  │   └─ state.accessToken && state.user
  │
  ├─ ② isAdminUser()                      API: /api/admin/**
  │   └─ state.user.role === 'admin'         │
  │                                           ├─ 解析 Bearer Token
  ├─ ③ verifyAdminAccess()                   ├─ 提取 userId
  │   └─ ensureAdminSession()                ├─ authUserMapper.findById(userId)
  │       ├─ 缓存命中 → true                 ├─ isActiveAdmin(authUser)
  │       └─ 缓存未命中                      │   ├─ role === "admin" ✓
  │           └─ GET /api/auth/me            │   └─ status === 1     ✓
  │               └─ 后端返回 role           └─ 放行/拒绝
  │                   ├─ admin → 缓存 + 放行
  │                   └─ user  → logout + 拒绝
  │
  └─ 拒绝 → /home?notice=unavailable → Toast "页面暂时不可用"
```

### 多层防御层次

| 层级 | 位置 | 机制 | 作用 |
|---|---|---|---|
| L1 | 前端路由 | `isAdminUser()` 内存缓存判断 | 即时阻断，0ms 响应 |
| L2 | 前端路由 | `verifyAdminAccess()` 服务端验证 | 防 localStorage 篡改 |
| L3 | 后端拦截器 | `AuthInterceptor` DB 角色查询 | 最终防线，数据库为准 |
| L4 | 业务层 | `AuthServiceImpl.getCurrentUser()` 状态校验 | 用户状态变更实时生效 |

### 管理员验证缓存策略

```
Token 生命周期 = 信任区间
  │
  ├─ adminVerifiedToken === accessToken
  │   → 已信任此 token，跳过验证
  │
  └─ adminVerifiedToken !== accessToken
      → token 已换或首次使用
        → 调用 /api/auth/me 重新验证
          ├─ role === 'admin' → 缓存 token，信任
          └─ role !== 'admin' → 清空缓存，拒绝
```

---

## 安全考量

1. **数据库为准**：`isActiveAdmin()` 每次都查 `auth_user` 表，不信任前端传递的任何角色声明
2. **双重校验**：`isActiveAdmin()` 同时检查 `role = "admin"` AND `status = 1`，已禁用账号即使 role 为 admin 也无法访问
3. **缓存仅用于免验**：`adminVerifiedToken` 缓存在 localStorage，仅在 L2 层用于跳过重复 API 调用。L3 层（后端拦截器）每次请求都查库，不受前端缓存影响
4. **信息不泄露**：所有失败路径返回相同提示 "当前页面暂时不可用"，不区分 401/403，防止攻击者通过响应差异探测后台路径
5. **XSS 风险提示**：token 和 adminVerifiedToken 均存储在 localStorage 中，存在 XSS 泄露风险。建议后续迁移至 httpOnly Cookie

---

## 后续建议（低优先级）

1. **httpOnly Cookie**：将 accessToken 从 localStorage 迁移至 httpOnly Cookie，进一步降低 XSS 泄露风险
2. **管理操作审计日志**：记录管理员的关键操作（内容发布、删除、书籍导入），便于安全审计
3. **IP 白名单**：可考虑为管理后台增加 IP 白名单（如仅允许特定 IP 段访问），作为额外安全层
4. **会话超时提醒**：管理员长时间停留在后台时，token 过期前弹窗提醒续期，避免编辑内容丢失
5. **二次验证**：敏感操作（批量删除、全量导入）可增加确认密码或二次验证步骤
