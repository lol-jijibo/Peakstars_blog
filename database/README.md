# Stars_面经宝典 - 数据库接入说明

## 完整架构

```
前端 Vue3 (localhost:5173)
    ↓ HTTP API
后端 Node.js Express (localhost:3000)
    ↓ mysql2
MySQL 数据库 (localhost:3306 / interview_db)
```

---

## 第一步：创建数据库

打开 MySQL 客户端（Navicat / MySQL Workbench / 命令行），依次执行：

```sql
-- 1. 创建表结构
source database/schema.sql

-- 2. 导入初始数据
source database/seed.sql
```

或在命令行中：
```bash
mysql -u root -p < database/schema.sql
mysql -u root -p < database/seed.sql
```

---

## 第二步：配置数据库连接

编辑 `server/src/config/db.config.js`，修改为你的实际配置：

```js
module.exports = {
  host: 'localhost',
  port: 3306,
  user: 'root',        // ← 你的用户名
  password: '123456',  // ← 你的密码
  database: 'interview_db',
  // ...
}
```

---

## 第三步：启动后端服务

```bash
cd server
npm install
npm run dev    # 开发模式（需要 nodemon）
# 或
npm start      # 生产模式
```

服务启动后访问：http://localhost:3000/api/health

---

## 第四步：启动前端

```bash
# 项目根目录
npm run dev
```

访问：http://localhost:5173

---

## API 接口文档

| 方法 | 路径 | 说明 |
|------|------|------|
| GET  | /api/interviews | 获取面经列表（支持 category/keyword/page 参数）|
| GET  | /api/interviews/:id | 获取面经详情 |
| POST | /api/interviews/:id/like | 点赞 |
| POST | /api/interviews/:id/collect | 收藏 |
| GET  | /api/categories | 获取分类列表 |
| GET  | /api/health | 健康检查 |

### 列表接口参数

```
GET /api/interviews?category=frontend&keyword=Vue&page=1&pageSize=20
```

| 参数 | 说明 | 默认值 |
|------|------|--------|
| category | all / frontend / java | all |
| keyword | 搜索关键词 | 空 |
| page | 页码 | 1 |
| pageSize | 每页条数 | 20 |

---

## 数据库表说明

| 表名 | 说明 |
|------|------|
| company | 企业信息（8家企业） |
| category | 分类（全部/前端/Java后端） |
| tag | 标签 |
| interview | 面经主表（含 content 正文） |
| interview_tag | 面经-标签 多对多关联 |
| user_action | 用户点赞/收藏行为（可选扩展） |

---

## 离线降级方案

如果暂时不想连接 MySQL，前端仍然可以使用 `src/data/interviews.js` 中的静态数据。
只需将 `InterviewList.vue` 和 `InterviewDetail.vue` 中的 import 改回：

```js
// 改为静态数据模式
import { interviews } from '@/data/interviews.js'
```
