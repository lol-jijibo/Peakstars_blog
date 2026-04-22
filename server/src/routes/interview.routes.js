const express = require('express')
const router = express.Router()
const ctrl = require('../controllers/interview.controller')

// 面经列表
router.get('/', ctrl.getList)

// 面经详情
router.get('/:id', ctrl.getDetail)

// 点赞
router.post('/:id/like', ctrl.like)

// 收藏
router.post('/:id/collect', ctrl.collect)

module.exports = router
