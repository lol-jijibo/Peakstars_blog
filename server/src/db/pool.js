const mysql = require('mysql2/promise')
const dbConfig = require('../config/db.config')

// 创建连接池
const pool = mysql.createPool(dbConfig)

// 测试连接
pool.getConnection()
  .then(conn => {
    console.log('✅ MySQL 数据库连接成功')
    conn.release()
  })
  .catch(err => {
    console.error('❌ MySQL 连接失败:', err.message)
    console.error('请检查 server/src/config/db.config.js 中的数据库配置')
  })

module.exports = pool
