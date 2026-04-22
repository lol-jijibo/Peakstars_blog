const pool = require('../db/pool')

/**
 * 获取面经列表
 * GET /api/interviews
 * Query: category(all/frontend/java), keyword, page, pageSize
 */
exports.getList = async (req, res) => {
  try {
    const { category = 'all', keyword = '', page = 1, pageSize = 20 } = req.query
    const offset = (parseInt(page) - 1) * parseInt(pageSize)

    let where = 'WHERE i.status = 1'
    const params = []

    if (category && category !== 'all') {
      where += ' AND c.code = ?'
      params.push(category)
    }

    if (keyword) {
      where += ' AND (i.title LIKE ? OR i.author LIKE ? OR i.summary LIKE ?)'
      const kw = `%${keyword}%`
      params.push(kw, kw, kw)
    }

    // 查询总数
    const [countRows] = await pool.query(
      `SELECT COUNT(*) AS total
       FROM interview i
       JOIN category c ON i.category_id = c.id
       ${where}`,
      params
    )
    const total = countRows[0].total

    // 查询列表
    const [rows] = await pool.query(
      `SELECT
         i.id,
         i.title,
         i.author,
         i.summary,
         i.views,
         i.likes,
         i.collects,
         i.publish_date AS date,
         c.code         AS category,
         c.name         AS categoryName,
         co.name        AS companyName,
         co.avatar_text AS avatar,
         co.avatar_color AS avatarColor
       FROM interview i
       JOIN category c  ON i.category_id = c.id
       JOIN company  co ON i.company_id  = co.id
       ${where}
       ORDER BY i.publish_date DESC, i.likes DESC
       LIMIT ? OFFSET ?`,
      [...params, parseInt(pageSize), offset]
    )

    // 为每篇面经查询标签
    for (const row of rows) {
      const [tags] = await pool.query(
        `SELECT t.name FROM tag t
         JOIN interview_tag it ON t.id = it.tag_id
         WHERE it.interview_id = ?`,
        [row.id]
      )
      row.tags = tags.map(t => t.name)
    }

    res.json({
      code: 0,
      data: {
        list: rows,
        total,
        page: parseInt(page),
        pageSize: parseInt(pageSize)
      }
    })
  } catch (err) {
    console.error('getList error:', err)
    res.status(500).json({ code: 500, message: '服务器错误' })
  }
}

/**
 * 获取面经详情
 * GET /api/interviews/:id
 */
exports.getDetail = async (req, res) => {
  try {
    const { id } = req.params

    const [rows] = await pool.query(
      `SELECT
         i.id,
         i.title,
         i.author,
         i.summary,
         i.content,
         i.views,
         i.likes,
         i.collects,
         i.publish_date AS date,
         c.code         AS category,
         c.name         AS categoryName,
         co.name        AS companyName,
         co.avatar_text AS avatar,
         co.avatar_color AS avatarColor,
         co.description AS companyDesc
       FROM interview i
       JOIN category c  ON i.category_id = c.id
       JOIN company  co ON i.company_id  = co.id
       WHERE i.id = ? AND i.status = 1`,
      [id]
    )

    if (rows.length === 0) {
      return res.status(404).json({ code: 404, message: '面经不存在' })
    }

    const interview = rows[0]

    // 查询标签
    const [tags] = await pool.query(
      `SELECT t.name FROM tag t
       JOIN interview_tag it ON t.id = it.tag_id
       WHERE it.interview_id = ?`,
      [id]
    )
    interview.tags = tags.map(t => t.name)

    // 增加浏览量（异步，不等待）
    pool.query('UPDATE interview SET views = views + 1 WHERE id = ?', [id])

    res.json({ code: 0, data: interview })
  } catch (err) {
    console.error('getDetail error:', err)
    res.status(500).json({ code: 500, message: '服务器错误' })
  }
}

/**
 * 点赞
 * POST /api/interviews/:id/like
 */
exports.like = async (req, res) => {
  try {
    const { id } = req.params
    await pool.query('UPDATE interview SET likes = likes + 1 WHERE id = ?', [id])
    const [rows] = await pool.query('SELECT likes FROM interview WHERE id = ?', [id])
    res.json({ code: 0, data: { likes: rows[0]?.likes ?? 0 } })
  } catch (err) {
    res.status(500).json({ code: 500, message: '服务器错误' })
  }
}

/**
 * 收藏
 * POST /api/interviews/:id/collect
 */
exports.collect = async (req, res) => {
  try {
    const { id } = req.params
    await pool.query('UPDATE interview SET collects = collects + 1 WHERE id = ?', [id])
    const [rows] = await pool.query('SELECT collects FROM interview WHERE id = ?', [id])
    res.json({ code: 0, data: { collects: rows[0]?.collects ?? 0 } })
  } catch (err) {
    res.status(500).json({ code: 500, message: '服务器错误' })
  }
}

/**
 * 获取分类列表
 * GET /api/categories
 */
exports.getCategories = async (req, res) => {
  try {
    const [rows] = await pool.query(
      'SELECT id, code, name FROM category ORDER BY sort_order'
    )
    res.json({ code: 0, data: rows })
  } catch (err) {
    res.status(500).json({ code: 500, message: '服务器错误' })
  }
}
