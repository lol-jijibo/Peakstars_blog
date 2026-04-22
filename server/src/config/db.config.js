// 数据库连接配置
// 根据实际环境修改以下配置

module.exports = {
  host: 'localhost',       // MySQL 主机地址
  port: 3306,             // MySQL 端口
  user: 'root',           // 数据库用户名
  password: 'lixuF123456',     // 数据库密码（请修改为实际密码）
  database: 'interview_db',
  waitForConnections: true,
  connectionLimit: 10,    // 连接池最大连接数
  queueLimit: 0,
  charset: 'utf8mb4'
}
