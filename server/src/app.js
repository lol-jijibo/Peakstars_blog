const express = require('express')
const cors = require('cors')
const interviewRoutes = require('./routes/interview.routes')
const interviewCtrl = require('./controllers/interview.controller')

const app = express()
const PORT = process.env.PORT || 3000

// 中间件
app.use(cors())                        // 允许跨域（开发阶段）
app.use(express.json())                // 解析 JSON body
app.use(express.urlencoded({ extended: true }))

// 路由
app.use('/api/interviews', interviewRoutes)
app.get('/api/categories', interviewCtrl.getCategories)

// 健康检查
app.get('/api/health', (req, res) => {
  res.json({ code: 0, message: 'OK', timestamp: new Date().toISOString() })
})

// 404 处理
app.use((req, res) => {
  res.status(404).json({ code: 404, message: `接口不存在: ${req.method} ${req.path}` })
})

// 全局错误处理
app.use((err, req, res, next) => {
  console.error('Unhandled error:', err)
  res.status(500).json({ code: 500, message: '服务器内部错误' })
})

app.listen(PORT, () => {
  console.log(`\n🚀 面经宝典后端服务启动成功`)
  console.log(`   地址：http://localhost:${PORT}`)
  console.log(`   接口：http://localhost:${PORT}/api/interviews`)
  console.log(`   健康：http://localhost:${PORT}/api/health\n`)
})
